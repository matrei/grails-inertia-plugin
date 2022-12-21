package grails.plugin.inertia

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import grails.util.Environment
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

import static grails.web.http.HttpHeaders.VARY
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT
import static javax.servlet.http.HttpServletResponse.SC_MOVED_TEMPORARILY
import static javax.servlet.http.HttpServletResponse.SC_SEE_OTHER
import static org.grails.web.util.WebUtils.retrieveGrailsWebRequest as webRequest
import static Inertia.INERTIA_ATTRIBUTE_VERSION
import static Inertia.INERTIA_ATTRIBUTE_MANIFEST
import static Inertia.isAssetsOutOfDate
import static Inertia.isInertiaRequest
import static Inertia.isInertiaView
import static Inertia.staleAssetsResponse

@CompileStatic
class InertiaInterceptor implements GrailsConfigurationAware {

    String manifestLocation
    String manifestHash = 'not yet calculated'
    private volatile Object manifestObject

    static final String INERTIA_HEADER_VERSION = 'X-Inertia-Version'
    static final String INERTIA_HEADER_NAME = 'X-Inertia'
    static final String INERTIA_HEADER_VALUE = 'true'

    private static final String CONTENT_TYPE_JSON = 'application/json;charset=utf-8'
    private static final String CONTENT_TYPE_HTML = 'text/html;charset=utf-8'
    private static final String GET = 'GET'

    InertiaInterceptor() { matchAll() }

    boolean before() {

        request.setAttribute INERTIA_ATTRIBUTE_VERSION, manifestHash

        setContentType()
        setHeaders()

        // Check for asset version changes on GET requests
        if(inertiaRequest && getRequest && manifestShouldBeUsed && assetsOutOfDate) {

            log.debug 'Inertia asset version has changed, notifying Inertia client and aborting request processing to force full page reload!'

            render staleAssetsResponse()
            return false
        }

        true
    }

    boolean after() {

        // Changes the status code during redirects, ensuring they are made as
        // GET requests, preventing "MethodNotAllowedHttpException" errors.
        if (methodNotAllowedShouldBePrevented) {
            response.status = SC_SEE_OTHER
        }

        // Add the Javascript Manifest when in Production and Test Environments
        // In Development Environment a node server should be started to serve the javascript files (npm run serve)
        if(inertiaView && manifestShouldBeUsed) {
            model.put INERTIA_ATTRIBUTE_MANIFEST, manifest
        }

        true
    }


    private void setContentType() { response.contentType = isInertiaRequest ? CONTENT_TYPE_JSON : CONTENT_TYPE_HTML }
    private void setHeaders() {
        if(isInertiaRequest) header INERTIA_HEADER_NAME, INERTIA_HEADER_VALUE
        response.addHeader VARY, INERTIA_HEADER_NAME
    }

    private Object loadManifest() {
        // TODO: should the manifest file be checked for modification and reloaded?
        if(manifestObject == null) {
            synchronized(this) {
                if(manifestObject == null) {
                    def manifestResource= Holders.grailsApplication.mainContext.getResource manifestLocation
                    manifestObject = new JsonSlurper().parse manifestResource.inputStream
                    manifestHash = Objects.hashCode(manifestObject) as String
                }
            }
        }
        manifestObject
    }

    private Object getManifest() {
        loadManifest()
    }

    private static boolean getManifestShouldBeUsed() {
        Environment.current != Environment.DEVELOPMENT
    }

    private static boolean isGetRequest() {
        'GET' == webRequest().currentRequest.method
    }

    private static boolean getMethodNotAllowedShouldBePrevented() {
        def webRequest = webRequest()
        inertiaRequest && webRequest.currentResponse.status == SC_MOVED_TEMPORARILY && webRequest.currentRequest.method in ['PUT', 'PATCH', 'DELETE']
    }

    @Override
    void setConfiguration(Config co) {
        // Load the Javascript Manifest when in Production and Test Environments
        // In Development Environment a node server should be started to serve the javascript files (npm run serve)
        if(manifestShouldBeUsed) {
            manifestLocation = co.getRequiredProperty('inertia.manifest.location')
            loadManifest()
        }
    }
}

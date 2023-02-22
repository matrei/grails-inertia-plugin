package grails.plugin.inertia

import grails.config.Config
import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import grails.core.support.GrailsConfigurationAware
import grails.plugin.inertia.annotation.AnnotationExcluder
import grails.plugin.inertia.annotation.SkipInertia
import grails.util.Environment
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus

import static Inertia.INERTIA_ATTRIBUTE_MANIFEST
import static Inertia.INERTIA_ATTRIBUTE_VERSION
import static grails.web.http.HttpHeaders.VARY

@CompileStatic
class InertiaInterceptor implements GrailsConfigurationAware, GrailsApplicationAware {

    String manifestLocation
    String manifestHash = 'not yet calculated'
    private volatile Object manifestObject

    static final String INERTIA_HEADER_VERSION = 'X-Inertia-Version'
    static final String INERTIA_HEADER_NAME = 'X-Inertia'
    static final String INERTIA_HEADER_VALUE = 'true'

    private static final String CONTENT_TYPE_JSON = 'application/json;charset=utf-8'
    private static final String CONTENT_TYPE_HTML = 'text/html;charset=utf-8'

    InertiaInterceptor() {
        match controller: '*'
    }

    @Override
    void setGrailsApplication(GrailsApplication grailsApplication) {
        AnnotationExcluder.excludeAnnotations(this, grailsApplication, SkipInertia)
    }

    boolean before() {

        // Set the assets version so the client can check if it has an old version loaded
        request.setAttribute INERTIA_ATTRIBUTE_VERSION, manifestHash

        setContentType()
        setHeaders()

        // Check for asset version changes on GET requests
        if(isInertiaRequest && isGetRequest && manifestShouldBeUsed && isAssetsOutOfDate) {
            log.debug 'Inertia asset version has changed, notifying Inertia client and aborting request processing to force full inertiaPage reload!'
            header Inertia.INERTIA_HEADER_LOCATION, webRequest.currentRequest.forwardURI
            render status: HttpStatus.CONFLICT.code
            return false
        }

        true
    }

    boolean after() {

        // Changes the status code during redirects, ensuring they are made as
        // GET requests, preventing "MethodNotAllowedHttpException" errors.
        if (methodNotAllowedShouldBePrevented) response.status = HttpStatus.SEE_OTHER.code

        // Add the Javascript Manifest when not in Development Environment
        // In Development Environment a node server should be started to serve the javascript files (npm run serve)
        if(isInertiaHtmlView && manifestShouldBeUsed) model.put INERTIA_ATTRIBUTE_MANIFEST, manifest

        true
    }


    private void setContentType() { response.contentType = isInertiaRequest ? CONTENT_TYPE_JSON : CONTENT_TYPE_HTML }
    private void setHeaders() {
        if(isInertiaRequest) header INERTIA_HEADER_NAME, INERTIA_HEADER_VALUE
        response.addHeader VARY, INERTIA_HEADER_NAME
    }

    private Object loadManifest() {
        // TODO: should the manifest file be checked for modification and reloaded (live reloading of assets in production)?
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
    Object getManifest() { loadManifest() }

    boolean getManifestShouldBeUsed() { Environment.current != Environment.DEVELOPMENT }
    boolean getIsGetRequest() { 'GET' == request.method }
    boolean getMethodNotAllowedShouldBePrevented() { isInertiaRequest && response.status == HttpStatus.FOUND.code && request.method in ['PUT', 'PATCH', 'DELETE'] }
    boolean getIsInertiaHtmlView() { modelAndView?.viewName == Inertia.INERTIA_VIEW_HTML }
    boolean getIsInertiaRequest() { request.getHeader(INERTIA_HEADER_NAME) == INERTIA_HEADER_VALUE }
    boolean getIsAssetsCurrent() {
        def currentVersion = request.getAttribute(INERTIA_ATTRIBUTE_VERSION) as String
        def requestedVersion = request.getHeader(INERTIA_HEADER_VERSION) as String
        requestedVersion == currentVersion
    }
    boolean getIsAssetsOutOfDate() { !isAssetsCurrent }

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

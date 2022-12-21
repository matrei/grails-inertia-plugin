package grails.plugin.inertia

import grails.converters.JSON
import groovy.transform.CompileStatic
import io.micronaut.http.MediaType
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.ModelAndView

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT
import static org.grails.web.util.WebUtils.retrieveGrailsWebRequest as webRequest

@CompileStatic
class Inertia {

    static final String INERTIA_SHARED_DATA = 'grails.plugin.inertia.InertiaSharedData'
    static final String INERTIA_ATTRIBUTE_NAME = 'grails.plugin.inertia.InertiaRequest'
    static final String INERTIA_ATTRIBUTE_VERSION = 'grails.plugin.inertia.InertiaManifestVersion'
    static final String INERTIA_ATTRIBUTE_MANIFEST = 'inertiaManifest'
    static final String INERTIA_HEADER_NAME = 'X-Inertia'
    static final String INERTIA_HEADER_VALUE = 'true'
    static final String INERTIA_HEADER_LOCATION = 'X-Inertia-Location'
    static final String INERTIA_HEADER_VERSION = 'X-Inertia-Version'
    static final short INERTIA_RESPONSE_TYPE_HTML = 1
    static final short INERTIA_RESPONSE_TYPE_JSON = 2

    protected static final String INERTIA_VIEW_NAME = '/inertia'

    static ModelAndView render(String component) {
        render component, [:], [:]
    }

    static ModelAndView render(String component, Map props) {
        render component, props, [:]
    }

    static ModelAndView render(String component, Map props, Map viewData) {
        def request = webRequest().currentRequest
        request.setAttribute(INERTIA_ATTRIBUTE_NAME, true)
        renderInternal getResponseType(), component, chainModel + sharedData + props, viewData
    }

    static void redirect(String uri) {
        def response = webRequest().currentResponse
        response.sendRedirect(uri)
    }

    static void location(String url) {
        def response = webRequest().currentResponse
        response.setHeader(INERTIA_HEADER_LOCATION, url)
        response.status = SC_CONFLICT
    }

    private static ModelAndView renderInternal(short responseType, String component, Map props, Map viewData) {
        responseType == INERTIA_RESPONSE_TYPE_HTML ?
                doHtml(component, props, viewData) :
                doJson(component, props)
    }

    private static ModelAndView doJson(String component, Map model) {
        def response = webRequest().currentResponse
        response.addHeader Inertia.INERTIA_HEADER_NAME, Inertia.INERTIA_HEADER_VALUE
        (createInertiaPageModel(component, model)).render response
        null

    }
    private static ModelAndView doHtml(String component, Map props, Map viewData) {
        webRequest().currentResponse.contentType = MediaType.TEXT_HTML
        [INERTIA_VIEW_NAME, [inertiaPage: createInertiaPageModel(component, props).toString()] + viewData] as ModelAndView
    }

    private static JSON createInertiaPageModel(String component, Map model) {
        def grailsWebRequest = webRequest()
        if(!model.errors) model.errors = []
        if(!model.flash) model.flash = grailsWebRequest.flashScope
        def pageObject =
        [
            component: component,
            props: model,
            url: grailsWebRequest.currentRequest.requestURI,
            version: grailsWebRequest.currentRequest.getAttribute(INERTIA_ATTRIBUTE_VERSION)
        ]
        pageObject as JSON
    }

    static Map getSharedData() {
        (webRequest().currentRequest.getAttribute(Inertia.INERTIA_SHARED_DATA) ?: [:]) as Map
    }

    static Map getChainModel() {
        (webRequest().flashScope['chainModel'] ?: [:]) as Map
    }

    static void setSharedData(Map data) {
        webRequest().currentRequest.setAttribute(Inertia.INERTIA_SHARED_DATA, data)
    }

    static boolean isInertiaRequest() {
        webRequest().currentRequest.getHeader(INERTIA_HEADER_NAME) == INERTIA_HEADER_VALUE
    }

    static boolean isInertiaResponse() {
        def webRequest = webRequest()
        webRequest.currentResponse.getHeader(INERTIA_HEADER_NAME) == INERTIA_HEADER_VALUE ||
        webRequest.currentRequest.getAttribute(INERTIA_ATTRIBUTE_NAME)
    }

    static boolean isInertiaView() {
        def modelAndView = (ModelAndView) RequestContextHolder.currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        return modelAndView?.viewName == INERTIA_VIEW_NAME
    }

    private static short getResponseType() {
        isInertiaRequest() ? INERTIA_RESPONSE_TYPE_JSON : INERTIA_RESPONSE_TYPE_HTML
    }

    static boolean isAssetsCurrent() {
        def webRequest = webRequest()
        def currentVersion = webRequest.currentRequest.getAttribute(INERTIA_ATTRIBUTE_VERSION) as String
        def requestedVersion = webRequest.currentRequest.getHeader(INERTIA_HEADER_VERSION) as String
        requestedVersion == currentVersion
    }

    static boolean isAssetsOutOfDate() {
        !assetsCurrent
    }

    static Map staleAssetsResponse() {
        def webRequest = webRequest()
        webRequest.currentResponse.setHeader Inertia.INERTIA_HEADER_LOCATION, webRequest.currentRequest.forwardURI
        [status: SC_CONFLICT]
    }
}

package grails.plugin.inertia

import grails.plugin.json.view.JsonViewTemplateEngine
import grails.util.Holders
import groovy.transform.CompileStatic
import org.springframework.web.servlet.ModelAndView

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT
import static org.grails.web.util.WebUtils.retrieveGrailsWebRequest as webRequest

@CompileStatic
class Inertia {

    static final String INERTIA_SHARED_DATA = 'grails.plugin.inertia.InertiaSharedData'
    static final String INERTIA_ATTRIBUTE_NAME = 'grails.plugin.inertia.InertiaRequest'
    static final String INERTIA_ATTRIBUTE_VERSION = 'grails.plugin.inertia.InertiaManifestVersion'
    static final String INERTIA_ATTRIBUTE_MANIFEST = 'inertiaManifest'
    static final String INERTIA_HEADER_LOCATION = 'X-Inertia-Location'

    protected static final String INERTIA_VIEW_HTML = '/inertia/html'
    protected static final String INERTIA_VIEW_JSON = '/inertia/json'

    private static final String JSON_VIEW_TEMPLATE_ENGINE_BEAN_NAME = 'jsonTemplateEngine'
    private static final String INERTIA_PAGE_MODEL_KEY = 'inertiaPage'


    @SuppressWarnings('unused')
    static ModelAndView render(String component) { render component, [:], [:] }
    @SuppressWarnings('unused')
    static ModelAndView render(String component, Map props) { render component, props, [:] }
    static ModelAndView render(String component, Map props, Map viewData) {
        webRequest().currentRequest.setAttribute INERTIA_ATTRIBUTE_NAME, true
        renderInternal component, chainModel + sharedData + props, viewData
    }

/*
    static void redirect(String uri) { webRequest().currentResponse.sendRedirect uri }
*/

    static void location(String url) {
        def response = webRequest().currentResponse
        response.setHeader INERTIA_HEADER_LOCATION, url
        response.status = SC_CONFLICT
    }

    private static ModelAndView renderInternal(String component, Map props, Map viewData) {
        webRequest().currentResponse.getHeader('X-Inertia') ?
                doJson(component, props) :
                doHtml(component, props, viewData)
    }

    private static ModelAndView doJson(String component, Map model) {
        new ModelAndView(
            INERTIA_VIEW_JSON,
            [(INERTIA_PAGE_MODEL_KEY): createInertiaPageModel(component, model)]
        )
    }

    private static ModelAndView doHtml(String component, Map props, Map viewData) {
        def jsonViewTemplateEngine = Holders.getApplicationContext().getBean JSON_VIEW_TEMPLATE_ENGINE_BEAN_NAME, JsonViewTemplateEngine
        def jsonTemplate = jsonViewTemplateEngine.resolveTemplate(INERTIA_VIEW_JSON)
        def jsonModel = [(INERTIA_PAGE_MODEL_KEY): createInertiaPageModel(component, props)]
        def json = jsonTemplate.make(jsonModel).writeTo(new StringWriter()).toString()
        new ModelAndView(
            INERTIA_VIEW_HTML,
            [(INERTIA_PAGE_MODEL_KEY): json] + (viewData ?: [:])
        )
    }

    private static InertiaPage createInertiaPageModel(String component, Map model) {
        def grailsWebRequest = webRequest()
        if(!model.errors) model.errors = []
        if(!model.flash) model.flash = grailsWebRequest.flashScope
        def pageObject = new InertiaPage(
            component: component,
            props: model,
            url: grailsWebRequest.currentRequest.requestURI,
            version: grailsWebRequest.currentRequest.getAttribute(INERTIA_ATTRIBUTE_VERSION)
        )
        pageObject
    }

    static Map getSharedData() {
        (webRequest().currentRequest.getAttribute(INERTIA_SHARED_DATA) ?: [:]) as Map
    }

    static void setSharedData(Map data) {
        webRequest().currentRequest.setAttribute INERTIA_SHARED_DATA, data
    }

    static Map getChainModel() {
        (webRequest().flashScope['chainModel'] ?: [:]) as Map
    }
}

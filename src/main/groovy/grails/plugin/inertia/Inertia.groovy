/*
 * Copyright 2022-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.inertia

import grails.plugin.inertia.ssr.ServerSideRenderConfig
import grails.plugin.inertia.ssr.ServerSideRenderer
import grails.plugin.json.view.JsonViewTemplateEngine
import grails.util.Holders
import grails.web.mvc.FlashScope
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT
import static org.grails.web.util.WebUtils.retrieveGrailsWebRequest as webRequest

/**
 * Class for handling Inertia requests and responses.
 *
 * @author Mattias Reichel
 * @since 1.0.0
 */
@CompileStatic
class Inertia {

    static final String INERTIA_SHARED_DATA = 'grails.plugin.inertia.InertiaSharedData'
    static final String INERTIA_ATTRIBUTE_NAME = 'grails.plugin.inertia.InertiaRequest'
    static final String INERTIA_ATTRIBUTE_VERSION = 'grails.plugin.inertia.InertiaManifestVersion'
    static final String INERTIA_ATTRIBUTE_PAGE = 'grails.plugin.inertia.InertiaPage'
    static final String INERTIA_ATTRIBUTE_SSR_RESPONSE = 'grails.plugin.inertia.InertiaSsrResponse'
    static final String INERTIA_ATTRIBUTE_CANCEL_INERTIA = 'grails.plugin.inertia.CancelInertia'
    static final String INERTIA_ATTRIBUTE_MANIFEST = 'inertiaManifest'
    static final String INERTIA_HEADER = 'X-Inertia'
    static final String INERTIA_HEADER_VERSION = 'X-Inertia-Version'
    static final String INERTIA_HEADER_LOCATION = 'X-Inertia-Location'

    protected static final String INERTIA_VIEW_HTML = '/inertia/html'
    protected static final String INERTIA_VIEW_JSON = '/inertia/json'

    private static final String JSON_VIEW_TEMPLATE_ENGINE_BEAN_NAME = 'jsonTemplateEngine'
    private static final String SSR_RENDERER_BEAN_NAME = 'serverSideRenderer'
    private static final String INERTIA_PAGE_MODEL_KEY = 'inertiaPage'


    @SuppressWarnings('unused')
    static ModelAndView render(String component) { render component, [:], [:] }
    @SuppressWarnings('unused')
    static ModelAndView render(String component, Map props) { render component, props, [:] }
    static ModelAndView render(String component, Map props, Map viewData) {
        request.setAttribute INERTIA_ATTRIBUTE_NAME, true
        renderInternal component, chainModel + sharedData + props, viewData
    }

/*
    static void redirect(String uri) { webRequest().currentResponse.sendRedirect uri }
*/

    static void location(String url) {
        response.setHeader INERTIA_HEADER_LOCATION, url
        response.status = SC_CONFLICT
    }

    @SuppressWarnings('unused')
    static void cancel() {
        request.setAttribute INERTIA_ATTRIBUTE_CANCEL_INERTIA, true
    }

    static boolean getIsCanceled() {
        request.getAttribute INERTIA_ATTRIBUTE_CANCEL_INERTIA
    }

    private static ModelAndView renderInternal(String component, Map props, Map viewData) {
        isInertiaRequest ?
            renderJson(component, props) :
            renderHtml(component, props, viewData)
    }

    private static ModelAndView renderJson(String component, Map model) {
        def jsonModel = createJsonModel component, model
        new ModelAndView(INERTIA_VIEW_JSON, jsonModel)
    }

    private static ModelAndView renderHtml(String component, Map props, Map viewData) {

        if (ssrEnabled) {
            def page = createInertiaPageModel component, props
            def ssrResult = ssrRenderer.render page
            if (ssrResult) {
                def ssrResultJson = new JsonSlurper().parseText ssrResult
                request.setAttribute INERTIA_ATTRIBUTE_SSR_RESPONSE, ssrResultJson
                return new ModelAndView(INERTIA_VIEW_HTML, (viewData ?: [:]))
            }
        }

        def jsonTemplate = jsonViewTemplateEngine.resolveTemplate INERTIA_VIEW_JSON
        def jsonModel = createJsonModel component, props
        def jsonString = jsonTemplate.make(jsonModel).writeTo(new StringWriter()).toString()
        request.setAttribute INERTIA_ATTRIBUTE_PAGE, jsonString
        new ModelAndView(INERTIA_VIEW_HTML, (viewData ?: [:]))
    }

    private static InertiaPage createInertiaPageModel(String component, Map model) {
        if (!model.errors) model.errors = []
        if (!model.flash) model.flash = flash
        new InertiaPage(
            component: component,
            props: model,
            url: forwardURI ?: requestURI,
            version: inertiaAssetVersion
        )
    }

    static Map getSharedData() {
        (request.getAttribute(INERTIA_SHARED_DATA) ?: [:]) as Map
    }

    static void setSharedData(Map data) {
        request.setAttribute INERTIA_SHARED_DATA, data
    }

    static Map getChainModel() {
        (flash['chainModel'] ?: [:]) as Map
    }

    static Map<String,InertiaPage> createJsonModel(String component, Map model) {
        [(INERTIA_PAGE_MODEL_KEY): createInertiaPageModel(component, model)]
    }

    static JsonViewTemplateEngine getJsonViewTemplateEngine() {
        Holders.getApplicationContext().getBean JSON_VIEW_TEMPLATE_ENGINE_BEAN_NAME, JsonViewTemplateEngine
    }

    static ServerSideRenderer getSsrRenderer() {
        Holders.getApplicationContext().getBean SSR_RENDERER_BEAN_NAME, ServerSideRenderer
    }

    static boolean isSsrEnabled() {
        Holders.getConfig().getProperty("${ServerSideRenderConfig.PREFIX}.enabled", Boolean, false)
    }

    static FlashScope getFlash() {
        webRequest().flashScope
    }

    static String getForwardURI() {
        request.forwardURI
    }

    static String getRequestURI() {
        request.requestURI
    }

    static String getInertiaAssetVersion() {
        request.getAttribute INERTIA_ATTRIBUTE_VERSION
    }

    static boolean getIsInertiaRequest() {
        request.getHeader INERTIA_HEADER
    }

    static HttpServletRequest getRequest() {
        webRequest().currentRequest
    }

    static HttpServletResponse getResponse() {
        webRequest().currentResponse
    }
}

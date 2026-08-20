/*
 * Copyright 2022-present original authors
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

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import org.springframework.web.servlet.ModelAndView

import grails.plugin.json.view.JsonViewTemplateEngine
import grails.util.Holders
import grails.web.mvc.FlashScope

import grails.plugin.inertia.ssr.ServerSideRenderer

import static jakarta.servlet.http.HttpServletResponse.SC_CONFLICT
import static org.grails.web.util.WebUtils.retrieveGrailsWebRequest as webRequest

/**
 * Class for handling Inertia requests and responses.
 *
 * @author Mattias Reichel
 * @since 1.0.0
 */
@CompileStatic
class Inertia {

    public static final String INERTIA_SHARED_DATA = 'grails.plugin.inertia.InertiaSharedData'
    public static final String INERTIA_ATTRIBUTE_NAME = 'grails.plugin.inertia.InertiaRequest'
    public static final String INERTIA_ATTRIBUTE_VERSION = 'grails.plugin.inertia.InertiaManifestVersion'
    public static final String INERTIA_ATTRIBUTE_PAGE = 'grails.plugin.inertia.InertiaPage'
    public static final String INERTIA_ATTRIBUTE_SSR_RESPONSE = 'grails.plugin.inertia.InertiaSsrResponse'
    public static final String INERTIA_ATTRIBUTE_CANCEL_INERTIA = 'grails.plugin.inertia.CancelInertia'
    public static final String INERTIA_ATTRIBUTE_MANIFEST = 'inertiaManifest'
    public static final String INERTIA_HEADER = 'X-Inertia'
    public static final String INERTIA_HEADER_VERSION = 'X-Inertia-Version'
    public static final String INERTIA_HEADER_LOCATION = 'X-Inertia-Location'
    public static final String INERTIA_HEADER_REDIRECT = 'X-Inertia-Redirect'
    public static final String INERTIA_HEADER_PARTIAL_COMPONENT = 'X-Inertia-Partial-Component'
    public static final String INERTIA_HEADER_PARTIAL_DATA = 'X-Inertia-Partial-Data'
    public static final String INERTIA_HEADER_PARTIAL_EXCEPT = 'X-Inertia-Partial-Except'
    public static final String INERTIA_HEADER_RESET = 'X-Inertia-Reset'
    public static final String INERTIA_HEADER_ERROR_BAG = 'X-Inertia-Error-Bag'
    public static final String INERTIA_HEADER_INFINITE_SCROLL_MERGE_INTENT =
            'X-Inertia-Infinite-Scroll-Merge-Intent'
    public static final String INERTIA_HEADER_EXCEPT_ONCE_PROPS = 'X-Inertia-Except-Once-Props'
    public static final String INERTIA_HEADER_PURPOSE = 'Purpose'

    protected static final String INERTIA_VIEW_HTML = '/inertia/html'
    protected static final String INERTIA_VIEW_JSON = '/inertia/json'

    private static final String JSON_VIEW_TEMPLATE_ENGINE_BEAN_NAME = 'jsonTemplateEngine'
    private static final String SSR_RENDERER_BEAN_NAME = 'serverSideRenderer'
    static final String INERTIA_PAGE_MODEL_KEY = 'inertiaPage'


    @SuppressWarnings('unused')
    static ModelAndView render(String component) {
        render(component, [:], [:])
    }
    @SuppressWarnings('unused')
    static ModelAndView render(String component, Map props) {
        render(component, props, [:])
    }
    static ModelAndView render(String component, Map props, Map viewData) {
        request.setAttribute(INERTIA_ATTRIBUTE_NAME, true)
        Map shared = sharedData
        renderInternal(component, chainModel + shared + props, viewData, shared.keySet() as List<String>)
    }

/*
    static void redirect(String uri) { webRequest().currentResponse.sendRedirect uri }
*/

    static void location(String url) {
        response.setHeader(INERTIA_HEADER_LOCATION, url)
        response.status = SC_CONFLICT
    }

    static InertiaProp always(Object value) {
        InertiaProp.always(value)
    }

    static InertiaProp optional(Object value) {
        InertiaProp.optional(value)
    }

    static InertiaProp defer(Object value, String group = 'default') {
        InertiaProp.deferred(value, group)
    }

    static InertiaProp merge(Object value) {
        InertiaProp.merge(value)
    }

    static InertiaProp prepend(Object value) {
        InertiaProp.prepend(value)
    }

    static InertiaProp deepMerge(Object value) {
        InertiaProp.deepMerge(value)
    }

    static InertiaProp once(Object value, String key = null, Long expiresAt = null) {
        InertiaProp.once(value, key, expiresAt)
    }

    static InertiaProp scroll(
            Object value,
            String mergePath,
            Map scrollProps,
            String matchOn = null
    ) {
        InertiaProp.scroll(value, mergePath, scrollProps, matchOn)
    }

    @SuppressWarnings('unused')
    static void cancel() {
        request.setAttribute(INERTIA_ATTRIBUTE_CANCEL_INERTIA, true)
    }

    static boolean getIsCanceled() {
        request.getAttribute(INERTIA_ATTRIBUTE_CANCEL_INERTIA)
    }

    private static ModelAndView renderInternal(
            String component,
            Map props,
            Map viewData,
            List<String> sharedProps
    ) {
        isInertiaRequest ?
            renderJson(component, props, sharedProps) :
            renderHtml(component, props, viewData, sharedProps)
    }

    private static ModelAndView renderJson(String component, Map model, List<String> sharedProps) {
        def jsonModel = createJsonModel(component, model, sharedProps)
        new ModelAndView(INERTIA_VIEW_JSON, jsonModel)
    }

    private static ModelAndView renderHtml(
            String component,
            Map props,
            Map viewData,
            List<String> sharedProps
    ) {

        if (ssrEnabled) {
            def page = createInertiaPageModel(component, props, sharedProps)
            def ssrResult = ssrRenderer.render(page)
            if (ssrResult) {
                def ssrResultJson = new JsonSlurper().parseText(ssrResult)
                request.setAttribute(INERTIA_ATTRIBUTE_SSR_RESPONSE, ssrResultJson)
                return new ModelAndView(INERTIA_VIEW_HTML, (viewData ?: [:]))
            }
        }

        def jsonTemplate = jsonViewTemplateEngine.resolveTemplate(INERTIA_VIEW_JSON)
        def jsonModel = createJsonModel(component, props, sharedProps)
        def jsonString = jsonTemplate.make(jsonModel).writeTo(new StringWriter()).toString()
        request.setAttribute(INERTIA_ATTRIBUTE_PAGE, jsonString)
        new ModelAndView(INERTIA_VIEW_HTML, (viewData ?: [:]))
    }

    private static InertiaPage createInertiaPageModel(
            String component,
            Map model,
            List<String> sharedProps
    ) {
        InertiaResponseFactory.createPage(component, model, sharedProps)
    }

    static Map getSharedData() {
        (request.getAttribute(INERTIA_SHARED_DATA) ?: [:]) as Map
    }

    static void setSharedData(Map data) {
        request.setAttribute(INERTIA_SHARED_DATA, data)
    }

    static Map getChainModel() {
        (flash['chainModel'] ?: [:]) as Map
    }

    static Map<String,InertiaPage> createJsonModel(String component, Map model) {
        InertiaResponseFactory.createJsonModel(component, model)
    }

    static Map<String,InertiaPage> createJsonModel(
            String component,
            Map model,
            List<String> sharedProps
    ) {
        InertiaResponseFactory.createJsonModel(component, model, sharedProps)
    }

    static JsonViewTemplateEngine getJsonViewTemplateEngine() {
        Holders.getApplicationContext().getBean(
                JSON_VIEW_TEMPLATE_ENGINE_BEAN_NAME,
                JsonViewTemplateEngine
        )
    }

    static ServerSideRenderer getSsrRenderer() {
        Holders.getApplicationContext().getBean(
                SSR_RENDERER_BEAN_NAME,
                ServerSideRenderer
        )
    }

    static boolean isSsrEnabled() {
        Holders.getConfig().getProperty(
                'inertia.ssr.enabled',
                Boolean,
                false
        )
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
        request.getAttribute(INERTIA_ATTRIBUTE_VERSION)
    }

    static boolean getIsInertiaRequest() {
        request.getHeader(INERTIA_HEADER)
    }

    static InertiaRequestContext getRequestContext() {
        InertiaRequestContext.from(request)
    }

    static HttpServletRequest getRequest() {
        webRequest().currentRequest
    }

    static HttpServletResponse getResponse() {
        webRequest().currentResponse
    }
}

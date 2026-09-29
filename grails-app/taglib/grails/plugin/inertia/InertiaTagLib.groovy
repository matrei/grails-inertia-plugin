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

import groovy.transform.CompileStatic

import org.springframework.web.util.HtmlUtils

import grails.gsp.Tag

/**
 * Taglib for including Inertia.js in a page.
 *
 * @author Mattias Reichel
 * @since 1.0.0
 */
@CompileStatic
class InertiaTagLib {

    final static String namespace = 'inertia'

    /**
     * Renders the element the Inertia app is mounted on, together with the initial page data.
     * When the page was rendered server-side, the rendered body is output instead.
     *
     * @attr id The id of the element, defaults to 'app'
     * @attr tagName The tag name of the element, defaults to 'div'
     */
    @Tag
    void app(Map<String, Object> attrs) {
        def ssr = ssrResponse
        if (ssr) {
            out << ssr.body
        } else {
            def tagName = attrs.tagName ?: 'div'
            def id = attrs.id ?: 'app'
            out << "<script data-page=\"$id\" type=\"application/json\">${pageForScriptTag}</script>"
            out << "<$tagName id=\"$id\"></$tagName>"
        }
    }

    /**
     * Renders the head elements of a server-side rendered page, preceded by the title
     * configured with {@code inertia.title}, or else the application name from {@code info.app.name},
     * unless the page rendered a title of its own.
     */
    @Tag
    void head() {
        def headElements = (ssrResponse?.head ?: []) as List<String>
        def config = grailsApplication.config
        def title = config.getProperty('inertia.title', String) ?: config.getProperty('info.app.name', String)
        if (title && !headElements.any { it.startsWith('<title') }) {
            out << "<title>${HtmlUtils.htmlEscape(title)}</title>"
        }
        headElements.each { headElement ->
            out << headElement
        }
    }

    private String getPage() {
        request.getAttribute(Inertia.INERTIA_ATTRIBUTE_PAGE) as String
    }

    private String getPageForScriptTag() {
        InertiaScriptJsonEncoder.encode(page)
    }

    private Map<String,Object> getSsrResponse() {
        request.getAttribute(Inertia.INERTIA_ATTRIBUTE_SSR_RESPONSE) as Map<String,Object>
    }

/*
    def routes = { attrs, body ->
        //grailsApplication.controllerClasses
        out << '''
        <script type="text/javascript">
            window.route = function() var routes = [] {}
       </script>
        '''
    }
*/
}

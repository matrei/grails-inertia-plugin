/*
 * Copyright 2022-2024 original authors
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
import org.grails.encoder.CodecLookup

/**
 * Taglib for including Inertia.js in a page.
 *
 * @author Mattias Reichel
 * @since 1.0.0
 */
@CompileStatic
class InertiaTagLib {

    CodecLookup codecLookup

    final static String namespace = 'inertia'

    Closure app = { Map<String,Object> attrs, Closure body ->
        if(ssrResponse) {
            out << ssrResponse.body
        } else {
            String tagName = attrs.tagName ?: 'div'
            String id = attrs.id ?: 'app'
            def htmlEncoder = codecLookup.lookupEncoder('HTML')
            out << "<$tagName id=\"$id\" data-page=\"${htmlEncoder.encode(page)}\"></$tagName>"
        }
    }

    Closure head = { Map<String,Object> attrs, Closure body ->
        if(ssrResponse) {
            ssrResponse.head.each { headElement ->
                out << headElement
            }
        }
    }

    private String getPage() {
        request.getAttribute(Inertia.INERTIA_ATTRIBUTE_PAGE) as String
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

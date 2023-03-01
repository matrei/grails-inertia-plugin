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

//import grails.web.mapping.UrlMapping

/**
 * Taglib for including Inertia.js in a page.
 *
 * @author Mattias Reichel
 * @since 1.0.0
 */
class InertiaTagLib {


//    static defaultEncodeAs = [taglib:'text']
    static namespace = 'inertia'

    def app = { attrs, body ->
        String tagName = attrs.tagName ?: 'div'
        String id = attrs.id ?: 'app'
        out << "<$tagName id=\"$id\" data-page=\"${body()}\"></$tagName>"
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

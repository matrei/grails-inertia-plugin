/*
 * Copyright 2026-present original authors
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
package grails.plugin.inertia.routes

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * A controller action and the URL it is mapped to, as seen from the browser.
 *
 * <p>The template names the URL parameters in braces: {@code {id}} for a required one, {@code {id?}} for an optional
 * one, and {@code {path*}} for one that may span several path segments.</p>
 *
 * <p>A route of a named URL mapping carries its name, and has a controller and action only when the mapping names
 * them.</p>
 *
 * @since 4.1
 */
@CompileStatic
@ToString(includeNames = true)
@EqualsAndHashCode
class Route {

    /** The name of the URL mapping, such as {@code showBook}, or null for a route of a controller action. */
    final String name

    /** The logical name of the controller, such as {@code organizations}. */
    final String controller

    /** The name of the action. */
    final String action

    /** The URL template, such as {@code /organizations/{id}/edit}. */
    final String template

    /** The HTTP methods the action accepts at this URL, in lower case, the first one being the default. */
    final List<String> methods

    Route(String controller, String action, String template, List<String> methods) {
        this(null, controller, action, template, methods)
    }

    Route(String name, String controller, String action, String template, List<String> methods) {
        this.name = name
        this.controller = controller
        this.action = action
        this.template = template
        this.methods = methods.asImmutable()
    }

    /**
     * The name the route is selected by: the name of its URL mapping, or else {@code controller.action}.
     */
    String getFilterName() {
        name ?: "${controller}.${action}" as String
    }
}

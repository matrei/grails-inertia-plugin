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

import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileStatic

import grails.gorm.validation.ConstrainedProperty
import grails.web.mapping.UrlMapping
import grails.web.mapping.UrlMappingsHolder
import org.grails.web.mapping.ResponseCodeUrlMapping

/**
 * Collects the routes of the controller actions from the URL mappings of an application.
 *
 * <p>A mapping that names its controller and action, such as {@code "/organizations/$id"(controller: 'organizations',
 * action: [PUT: 'update', DELETE: 'delete'])}, gives a route for each of its actions. A mapping that takes the
 * controller and action from the URL, such as {@code "/$controller/$action?/$id?"}, gives a route for each action of
 * each controller that no other mapping maps.</p>
 *
 * <p>A named mapping, such as {@code name showBook: "/books/$id"(controller: 'book', action: 'show')}, also gives a
 * route under its name, whatever it maps to.</p>
 *
 * <p>The methods of a route are those the mapping names. A mapping that accepts any method gives the methods of the
 * action in the {@code allowedMethods} of its controller, or else {@code get}.</p>
 *
 * @since 4.1
 */
@CompileStatic
class RouteCollector {

    private static final Pattern VARIABLE = ~/\(\*\*?\)\??/
    private static final Pattern FORMAT_EXTENSION = ~/\(\.\(\*\)\)\??$/
    private static final String DEFAULT_METHOD = 'get'

    /**
     * @param holder the URL mappings of the application
     * @param controllerActions the actions of each controller, by the logical name of the controller
     * @param allowedMethods the {@code allowedMethods} of each controller, by the logical name of the controller, with
     *        the method or methods each restricted action allows
     * @return the routes of the controller actions, ordered by controller and in the order the mappings define them,
     *         followed by the routes of the named mappings, ordered by name
     */
    List<Route> collect(UrlMappingsHolder holder, Map<String, Collection<String>> controllerActions, Map<String, Map> allowedMethods) {
        final routes = new LinkedHashMap<String, Route>()
        final namedRoutes = new TreeMap<String, Route>()
        final genericMappings = [] as List<UrlMapping>
        for (mapping in holder.urlMappings) {
            if (mapping instanceof ResponseCodeUrlMapping) {
                continue
            }
            if (mapping.mappingName) {
                namedRoutes.putIfAbsent(mapping.mappingName, namedRoute(mapping, allowedMethods))
            }
            if (mapping.controllerName instanceof String) {
                explicitRoutes(mapping, allowedMethods).each { routes.putIfAbsent(key(it), it) }
            } else if (variableNames(mapping).containsAll(['controller', 'action'])) {
                genericMappings << mapping
            }
        }
        for (mapping in genericMappings) {
            controllerActions.each { String controller, Collection<String> actions ->
                for (action in actions) {
                    final template = template(mapping, [controller: controller, action: action])
                    final route = new Route(controller, action, template, methods(mapping, controller, action, allowedMethods))
                    routes.putIfAbsent(key(route), route)
                }
            }
        }
        routes.values().sort(false) { Route it -> it.controller } + namedRoutes.values()
    }

    private static Route namedRoute(UrlMapping mapping, Map<String, Map> allowedMethods) {
        final controller = mapping.controllerName instanceof String ? mapping.controllerName as String : null
        final actionName = mapping.actionName
        final action = actionName instanceof String ? actionName as String : null
        final methods = actionName instanceof Map ?
                (actionName as Map<String, String>).keySet().collect { it.toLowerCase(Locale.ROOT) } :
                methods(mapping, controller, action, allowedMethods)
        new Route(mapping.mappingName, controller, action, template(mapping, [:]), methods)
    }

    /**
     * The methods of a mapping to a single action: the method the mapping is restricted to, or else the methods the
     * controller allows the action, or else the default method.
     */
    private static List<String> methods(UrlMapping mapping, String controller, String action, Map<String, Map> allowedMethods) {
        final method = mapping.httpMethod
        if (method && method != UrlMapping.ANY_HTTP_METHOD) {
            return [method.toLowerCase(Locale.ROOT)]
        }
        final allowed = controller && action ? allowedMethods[controller]?.get(action) : null
        final methods = (allowed instanceof Collection ? allowed as Collection : [allowed]).findAll().collect {
            it.toString().toLowerCase(Locale.ROOT)
        }
        methods ?: [DEFAULT_METHOD]
    }

    private List<Route> explicitRoutes(UrlMapping mapping, Map<String, Map> allowedMethods) {
        final controller = mapping.controllerName as String
        final template = template(mapping, [:])
        final actionName = mapping.actionName
        final methodsByAction = new LinkedHashMap<String, List<String>>()
        if (actionName instanceof Map) {
            (actionName as Map<String, String>).each { String method, String action ->
                methodsByAction.computeIfAbsent(action) { [] as List<String> } << method.toLowerCase(Locale.ROOT)
            }
        } else if (actionName instanceof String) {
            methodsByAction[actionName as String] = methods(mapping, controller, actionName as String, allowedMethods)
        }
        methodsByAction.collect { String action, List<String> methods -> new Route(controller, action, template, methods) }
    }

    /**
     * Builds the URL template of a mapping, filling in the given variables and leaving out the format extension.
     */
    private static String template(UrlMapping mapping, Map<String, String> values) {
        final names = variableNames(mapping).iterator()
        final segments = mapping.urlData.tokens.collect { String token ->
            final withoutFormat = FORMAT_EXTENSION.matcher(token).replaceAll('')
            final matcher = VARIABLE.matcher(withoutFormat)
            final segment = new StringBuilder()
            while (matcher.find()) {
                final variable = matcher.group()
                final name = names.next()
                String replacement
                if (values.containsKey(name)) {
                    replacement = values[name]
                } else {
                    replacement = "{${name}${variable.startsWith('(**)') ? '*' : ''}${variable.endsWith('?') ? '?' : ''}}"
                }
                matcher.appendReplacement(segment, Matcher.quoteReplacement(replacement))
            }
            matcher.appendTail(segment)
            segment.toString()
        }
        '/' + segments.findAll().join('/')
    }

    private static List<String> variableNames(UrlMapping mapping) {
        mapping.constraints.toList().collect { (it as ConstrainedProperty).propertyName }.findAll { it != 'format' }
    }

    private static String key(Route route) {
        "${route.controller}.${route.action}"
    }
}

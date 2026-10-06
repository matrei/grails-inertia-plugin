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
     * @return the routes, ordered by controller and in the order the mappings define them
     */
    List<Route> collect(UrlMappingsHolder holder, Map<String, Collection<String>> controllerActions) {
        final routes = new LinkedHashMap<String, Route>()
        final genericMappings = [] as List<UrlMapping>
        for (mapping in holder.urlMappings) {
            if (mapping instanceof ResponseCodeUrlMapping) {
                continue
            }
            if (mapping.controllerName instanceof String) {
                explicitRoutes(mapping).each { routes.putIfAbsent(key(it), it) }
            } else if (variableNames(mapping).containsAll(['controller', 'action'])) {
                genericMappings << mapping
            }
        }
        for (mapping in genericMappings) {
            controllerActions.each { String controller, Collection<String> actions ->
                for (action in actions) {
                    final route = new Route(controller, action, template(mapping, [controller: controller, action: action]), [DEFAULT_METHOD])
                    routes.putIfAbsent(key(route), route)
                }
            }
        }
        routes.values().sort(false) { Route it -> it.controller }
    }

    private List<Route> explicitRoutes(UrlMapping mapping) {
        final controller = mapping.controllerName as String
        final template = template(mapping, [:])
        final actionName = mapping.actionName
        final methodsByAction = new LinkedHashMap<String, List<String>>()
        if (actionName instanceof Map) {
            (actionName as Map<String, String>).each { String method, String action ->
                methodsByAction.computeIfAbsent(action) { [] as List<String> } << method.toLowerCase(Locale.ROOT)
            }
        } else if (actionName instanceof String) {
            final method = mapping.httpMethod
            methodsByAction[actionName as String] = [method && method != UrlMapping.ANY_HTTP_METHOD ? method.toLowerCase(Locale.ROOT) : DEFAULT_METHOD]
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

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

import java.util.regex.Pattern

import groovy.transform.CompileStatic

/**
 * Selects the routes to write by the name {@code controller.action}, with patterns in which {@code *} matches any
 * characters, such as {@code organizations.*} or {@code users.update}.
 *
 * <p>When patterns to include are given, only the routes matching one of them are kept. The routes matching one of
 * the patterns to exclude are then left out.</p>
 *
 * @since 4.1
 */
@CompileStatic
class RouteFilter {

    private final List<Pattern> only
    private final List<Pattern> except

    RouteFilter(List<String> only, List<String> except) {
        this.only = toPatterns(only)
        this.except = toPatterns(except)
    }

    List<Route> filter(List<Route> routes) {
        routes.findAll { Route route ->
            final name = "${route.controller}.${route.action}" as String
            (only.empty || only.any { it.matcher(name).matches() }) && !except.any { it.matcher(name).matches() }
        }
    }

    private static List<Pattern> toPatterns(List<String> patterns) {
        (patterns ?: []).findAll().collect { String pattern ->
            Pattern.compile(pattern.trim().split(/\*/, -1).collect { Pattern.quote(it) }.join('.*'))
        }
    }
}

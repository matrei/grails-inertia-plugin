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
package grails.plugin.inertia

import groovy.transform.CompileStatic
import groovy.transform.Immutable

import jakarta.servlet.http.HttpServletRequest

/**
 * Request options sent by an Inertia client.
 *
 * This is deliberately independent from prop resolution. It provides one
 * normalized representation of the v3 request headers for the response
 * pipeline and later resolver implementations.
 *
 * @since 4.0
 */
@Immutable
@CompileStatic
class InertiaRequestContext {

    String partialComponent
    List<String> partialData = []
    List<String> partialExcept = []
    List<String> reset = []
    String errorBag
    String infiniteScrollMergeIntent
    List<String> exceptOnceProps = []
    boolean prefetch

    boolean isPartialReload() {
        partialComponent != null
    }

    static InertiaRequestContext from(HttpServletRequest request) {
        new InertiaRequestContext(
                partialComponent: value(request, Inertia.INERTIA_HEADER_PARTIAL_COMPONENT),
                partialData: csv(request.getHeader(Inertia.INERTIA_HEADER_PARTIAL_DATA)),
                partialExcept: csv(request.getHeader(Inertia.INERTIA_HEADER_PARTIAL_EXCEPT)),
                reset: csv(request.getHeader(Inertia.INERTIA_HEADER_RESET)),
                errorBag: value(request, Inertia.INERTIA_HEADER_ERROR_BAG),
                infiniteScrollMergeIntent: value(
                        request,
                        Inertia.INERTIA_HEADER_INFINITE_SCROLL_MERGE_INTENT
                ),
                exceptOnceProps: csv(request.getHeader(Inertia.INERTIA_HEADER_EXCEPT_ONCE_PROPS)),
                prefetch: 'prefetch'.equalsIgnoreCase(request.getHeader(Inertia.INERTIA_HEADER_PURPOSE))
        )
    }

    private static String value(HttpServletRequest request, String header) {
        String value = request.getHeader(header)
        value?.trim() ?: null
    }

    private static List<String> csv(String value) {
        value ? value.split(',').collect { it.trim() }.findAll { it } : []
    }
}

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

/**
 * Creates the server-side response models used by Inertia rendering.
 *
 * This factory currently preserves the original page construction behavior.
 * Request-aware prop resolution and page metadata can be added here without
 * changing the public Inertia facade.
 *
 * @since 4.0
 */
@CompileStatic
class InertiaResponseFactory {

    static InertiaPage createPage(String component, Map model) {
        InertiaResolvedProps resolved = InertiaPropResolver.resolve(
                component,
                model,
                Inertia.requestContext
        )
        Map resolvedModel = resolved.props
        if (!resolvedModel.errors) resolvedModel.errors = []
        if (!resolvedModel.flash) resolvedModel.flash = Inertia.flash

        new InertiaPage(
                component: component,
                props: resolvedModel,
                url: Inertia.forwardURI ?: Inertia.requestURI,
                version: Inertia.inertiaAssetVersion,
                deferredProps: resolved.deferredProps,
                mergeProps: resolved.mergeProps,
                prependProps: resolved.prependProps,
                deepMergeProps: resolved.deepMergeProps,
                onceProps: resolved.onceProps
        )
    }

    static Map<String, InertiaPage> createJsonModel(String component, Map model) {
        [(Inertia.INERTIA_PAGE_MODEL_KEY): createPage(component, model)]
    }
}

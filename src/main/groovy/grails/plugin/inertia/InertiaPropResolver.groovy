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
 * Resolves page props according to the current Inertia request.
 *
 * @since 4.0
 */
@CompileStatic
class InertiaPropResolver {

    static InertiaResolvedProps resolve(
            String component,
            Map model,
            InertiaRequestContext context
    ) {
        boolean partial = context.partialReload && context.partialComponent == component
        Map resolved = [:]
        Map deferred = [:]
        List<String> merge = []
        List<String> prepend = []
        List<String> deepMerge = []
        Map once = [:]

        model.each { key, rawValue ->
            def propName = key as String
            def prop = rawValue instanceof InertiaProp ?
                    rawValue as InertiaProp :
                    InertiaProp.regular(rawValue)

            if (prop.type == InertiaProp.Type.DEFERRED && !partial) {
                addDeferred(deferred, prop.group, propName)
                return
            }

            String onceKey = prop.key ?: propName
            if (prop.type == InertiaProp.Type.ONCE && !partial &&
                    context.exceptOnceProps.contains(onceKey)) {
                addOnce(once, onceKey, propName, prop.expiresAt)
                return
            }

            if (!include(propName, prop, context, partial)) return
            resolved[propName] = resolveValue(prop.value)

            addMetadata(prop, propName, merge, prepend, deepMerge)
            if (prop.type == InertiaProp.Type.ONCE) {
                addOnce(once, onceKey, propName, prop.expiresAt)
            }
        }

        new InertiaResolvedProps(
                props: resolved,
                deferredProps: deferred,
                mergeProps: merge,
                prependProps: prepend,
                deepMergeProps: deepMerge,
                onceProps: once
        )
    }

    private static boolean include(
            String propName,
            InertiaProp prop,
            InertiaRequestContext context,
            boolean partial
    ) {
        if (!partial) {
            return prop.type != InertiaProp.Type.OPTIONAL &&
                    prop.type != InertiaProp.Type.DEFERRED
        }
        if (prop.type == InertiaProp.Type.ALWAYS) {
            return true
        }
        selected(propName, prop, context)
    }

    private static boolean selected(
            String propName,
            InertiaProp prop,
            InertiaRequestContext context
    ) {
        boolean included = context.partialData.empty || context.partialData.contains(propName)
        boolean excluded = context.partialExcept.contains(propName)
        included && !excluded
    }

    private static Object resolveValue(Object value) {
        value instanceof Closure ? (value as Closure).call() : value
    }

    private static void addDeferred(Map deferred, String group, String propName) {
        def props = (deferred[group] ?: []) as List<String>
        props << propName
        deferred[group] = props
    }

    private static void addMetadata(
            InertiaProp prop,
            String propName,
            List<String> merge,
            List<String> prepend,
            List<String> deepMerge
    ) {
        switch (prop.type) {
            case InertiaProp.Type.MERGE:
                merge << propName
                break
            case InertiaProp.Type.PREPEND:
                prepend << propName
                break
            case InertiaProp.Type.DEEP_MERGE:
                deepMerge << propName
                break
        }
    }

    private static void addOnce(
            Map once,
            String key,
            String propName,
            Long expiresAt
    ) {
        once[key] = [prop: propName, expiresAt: expiresAt]
    }
}

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
        boolean partial = isPartialReloadForComponent(component, context)
        Map resolved = [:]
        Map deferred = [:]
        List<String> merge = []
        List<String> prepend = []
        List<String> deepMerge = []
        Map once = [:]

        model.each { key, rawValue ->
            resolveProp(
                    key as String,
                    key as String,
                    rawValue,
                    resolved,
                    deferred,
                    merge,
                    prepend,
                    deepMerge,
                    once,
                    context,
                    partial
            )
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

    private static void resolveProp(
            String path,
            String outputPath,
            Object rawValue,
            Map output,
            Map deferred,
            List<String> merge,
            List<String> prepend,
            List<String> deepMerge,
            Map once,
            InertiaRequestContext context,
            boolean partial
    ) {
        def prop = rawValue instanceof InertiaProp ?
                rawValue as InertiaProp : InertiaProp.regular(rawValue)

        if (shouldDeferProp(prop, partial)) {
            addDeferred(deferred, prop.group, path)
            return
        }
        if (isOptionalPropExcluded(prop, partial)) {
            return
        }

        def onceKey = prop.key ?: path
        if (isPreviouslyLoadedOnceProp(prop, onceKey, partial, context)) {
            addOnce(once, onceKey, path, prop.expiresAt)
            return
        }

        def selection = determineSelection(path, prop, context, partial)
        if (isExcludedSelection(selection)) {
            return
        }

        def value = prop.value
        if (value instanceof Closure) {
            value = (value as Closure).call()
        }
        def resolvedValue = resolveNested(path, value, selection, output, context, partial,
                deferred, merge, prepend, deepMerge, once)
        if (shouldWriteResolvedValue(resolvedValue, value)) {
            put(output, outputPath, resolvedValue)
        }

        addMetadata(prop, path, merge, prepend, deepMerge, isResetProp(path, context))
        if (isOnceProp(prop)) {
            addOnce(once, onceKey, path, prop.expiresAt)
        }
    }

    private static Object resolveNested(
            String path,
            Object value,
            Selection selection,
            Map output,
            InertiaRequestContext context,
            boolean partial,
            Map deferred,
            List<String> merge,
            List<String> prepend,
            List<String> deepMerge,
            Map once
    ) {
        if (isFullyResolvedValue(value, selection)) {
            return value
        }
        def nested = [:]
        (value as Map).each { key, child ->
            def childPath = "$path.$key"
            resolveProp(childPath, key as String, child, nested, deferred, merge, prepend, deepMerge, once,
                    context, partial)
        }
        nested
    }

    private static Selection determineSelection(
            String path,
            InertiaProp prop,
            InertiaRequestContext context,
            boolean partial
    ) {
        if (isFullSelectionRequired(prop, partial)) {
            return Selection.FULL
        }
        if (isExcludedByPartialExcept(path, context)) {
            return Selection.NONE
        }
        if (isIncludedByPartialData(path, context)) {
            return hasNestedPartialSelection(path, context) ? Selection.PARTIAL : Selection.FULL
        }
        Selection.NONE
    }

    private static boolean isPartialReloadForComponent(
            String component,
            InertiaRequestContext context
    ) {
        context.partialReload && context.partialComponent == component
    }

    private static boolean shouldDeferProp(InertiaProp prop, boolean partial) {
        prop.type == InertiaProp.Type.DEFERRED && !partial
    }

    private static boolean isOptionalPropExcluded(InertiaProp prop, boolean partial) {
        prop.type == InertiaProp.Type.OPTIONAL && !partial
    }

    private static boolean isPreviouslyLoadedOnceProp(
            InertiaProp prop,
            String onceKey,
            boolean partial,
            InertiaRequestContext context
    ) {
        isOnceProp(prop) && !partial && context.exceptOnceProps.contains(onceKey)
    }

    private static boolean isExcludedSelection(Selection selection) {
        selection == Selection.NONE
    }

    private static boolean shouldWriteResolvedValue(Object resolvedValue, Object originalValue) {
        resolvedValue != null || !(originalValue instanceof Map)
    }

    private static boolean isResetProp(String path, InertiaRequestContext context) {
        context.reset.contains(path)
    }

    private static boolean isOnceProp(InertiaProp prop) {
        prop.type == InertiaProp.Type.ONCE
    }

    private static boolean isFullyResolvedValue(Object value, Selection selection) {
        !(value instanceof Map) || selection == Selection.FULL
    }

    private static boolean isFullSelectionRequired(InertiaProp prop, boolean partial) {
        !partial || prop.type == InertiaProp.Type.ALWAYS
    }

    private static boolean isExcludedByPartialExcept(
            String path,
            InertiaRequestContext context
    ) {
        context.partialExcept.any { path == it || path.startsWith("$it.") }
    }

    private static boolean isIncludedByPartialData(
            String path,
            InertiaRequestContext context
    ) {
        context.partialData.empty || context.partialData.any {
            it == path || it.startsWith("$path.") || path.startsWith("$it.")
        }
    }

    private static boolean hasNestedPartialSelection(
            String path,
            InertiaRequestContext context
    ) {
        context.partialData.any { it == path || it.startsWith("$path.") }
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
            List<String> deepMerge,
            boolean reset
    ) {
        if (reset) {
            return
        }
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

    private static void put(Map output, String path, Object value) {
        def pathParts = path.split('\\.')
        def targetMap = output
        if (pathParts.length > 1) {
            pathParts[0..-2].each { part ->
                def child = targetMap[part] instanceof Map ? targetMap[part] as Map : [:]
                targetMap[part] = child
                targetMap = child
            }
        }
        targetMap[pathParts[-1]] = value
    }

    private enum Selection {
        NONE,
        PARTIAL,
        FULL
    }
}

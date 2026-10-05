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
 * Describes how a page prop should be resolved.
 *
 * @since 4.0
 */
@CompileStatic
class InertiaProp {

    enum Type {
        REGULAR,
        ALWAYS,
        OPTIONAL,
        DEFERRED,
        MERGE,
        PREPEND,
        DEEP_MERGE,
        ONCE,
        SCROLL
    }

    final Object value
    final Type type
    final String group
    final String key
    final Long expiresAt
    final Map scroll
    final boolean rescue

    InertiaProp(
            Object value,
            Type type = Type.REGULAR,
            String group = 'default',
            String key = null,
            Long expiresAt = null,
            Map scroll = [:],
            boolean rescue = false
    ) {
        this.value = value
        this.type = type
        this.group = group ?: 'default'
        this.key = key
        this.expiresAt = expiresAt
        this.scroll = scroll ?: [:]
        this.rescue = rescue
    }

    static InertiaProp regular(Object value) {
        new InertiaProp(value)
    }

    static InertiaProp always(Object value) {
        new InertiaProp(value, Type.ALWAYS)
    }

    static InertiaProp optional(Object value) {
        new InertiaProp(value, Type.OPTIONAL)
    }

    static InertiaProp deferred(Object value, String group = 'default', boolean rescue = false) {
        new InertiaProp(value, Type.DEFERRED, group ?: 'default', null, null, [:], rescue)
    }

    static InertiaProp merge(Object value) {
        merge(value, null)
    }

    static InertiaProp merge(Object value, String matchOn) {
        new InertiaProp(value, Type.MERGE, 'default', null, null, [matchOn: matchOn])
    }

    static InertiaProp prepend(Object value) {
        prepend(value, null)
    }

    static InertiaProp prepend(Object value, String matchOn) {
        new InertiaProp(value, Type.PREPEND, 'default', null, null, [matchOn: matchOn])
    }

    static InertiaProp deepMerge(Object value) {
        deepMerge(value, null)
    }

    static InertiaProp deepMerge(Object value, String matchOn) {
        new InertiaProp(value, Type.DEEP_MERGE, 'default', null, null, [matchOn: matchOn])
    }

    static InertiaProp once(Object value, String key = null, Long expiresAt = null) {
        new InertiaProp(value, Type.ONCE, 'default', key, expiresAt)
    }

    static InertiaProp scroll(
            Object value,
            String mergePath,
            Map scrollProps,
            String matchOn = null
    ) {
        new InertiaProp(
                value,
                Type.SCROLL,
                'default',
                mergePath,
                null,
                [scrollProps: scrollProps ?: [:], matchOn: matchOn]
        )
    }
}

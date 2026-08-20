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
        DEFERRED
    }

    final Object value
    final Type type
    final String group

    InertiaProp(Object value, Type type = Type.REGULAR, String group = 'default') {
        this.value = value
        this.type = type
        this.group = group ?: 'default'
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

    static InertiaProp deferred(Object value, String group = 'default') {
        new InertiaProp(value, Type.DEFERRED, group ?: 'default')
    }
}

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
 * Encodes JSON for safe embedding in an HTML script element.
 *
 * @since 4.0
 */
@CompileStatic
class InertiaScriptJsonEncoder {

    static String encode(String json) {
        json
                ?.replace('/', '\\/')
                ?.replace('\u2028', '\\u2028')
                ?.replace('\u2029', '\\u2029')
    }
}

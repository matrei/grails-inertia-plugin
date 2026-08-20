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

/**
 * Resolved props and protocol metadata for one Inertia response.
 *
 * @since 4.0
 */
@Immutable
@CompileStatic
class InertiaResolvedProps {

    Map props = [:]
    Map deferredProps = [:]
    List<String> mergeProps = []
    List<String> prependProps = []
    List<String> deepMergeProps = []
    List<String> matchPropsOn = []
    Map scrollProps = [:]
    Map onceProps = [:]
}

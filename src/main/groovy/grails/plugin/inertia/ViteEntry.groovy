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
 * The files a page needs to load a Vite entry point, resolved from the Vite manifest
 * as described in https://vite.dev/guide/backend-integration.
 *
 * @since 4.0
 */
@CompileStatic
class ViteEntry {

    /** The JavaScript file of the entry point. */
    final String file

    /** The stylesheets of the entry point and of all chunks it imports. */
    final List<String> css

    /** The JavaScript files of all chunks the entry point imports, to be preloaded. */
    final List<String> preloads

    private ViteEntry(String file, List<String> css, List<String> preloads) {
        this.file = file
        this.css = css
        this.preloads = preloads
    }

    static ViteEntry from(Map<String, Map<String, Object>> manifest, String name) {
        def entry = manifest?.get(name)
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Entry point [$name] not found in the Vite manifest. Build the assets, for example with " +
                    "'vite build', or use the Vite dev server by setting ${ViteConfig.DEV_SERVER_ENABLED} to true."
            )
        }
        def importedChunks = importedChunks(manifest, entry, [] as Set<String>)
        def css = [] as LinkedHashSet<String>
        css.addAll(filesOf(entry, 'css'))
        importedChunks.each { css.addAll(filesOf(it, 'css')) }
        new ViteEntry(
                entry.file as String,
                css.toList(),
                importedChunks.collect { it.file as String }
        )
    }

    // Follows the imports depth first, so that a chunk is listed after the chunks it imports.
    private static List<Map<String, Object>> importedChunks(Map<String, Map<String, Object>> manifest, Map<String, Object> chunk, Set<String> seen) {
        def chunks = [] as List<Map<String, Object>>
        for (String name in filesOf(chunk, 'imports')) {
            if (seen.add(name)) {
                def importee = manifest[name]
                chunks.addAll(importedChunks(manifest, importee, seen))
                chunks.add(importee)
            }
        }
        chunks
    }

    private static List<String> filesOf(Map<String, Object> chunk, String key) {
        (chunk[key] ?: []) as List<String>
    }
}

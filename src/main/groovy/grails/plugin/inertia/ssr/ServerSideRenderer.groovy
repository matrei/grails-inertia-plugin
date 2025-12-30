/*
 * Copyright 2023-present original authors
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
package grails.plugin.inertia.ssr

import groovy.json.JsonOutput
import groovy.transform.CompileStatic

import grails.plugin.inertia.InertiaPage

/**
 * Renderer for server-side rendering via an Inertia SSR Node Server.
 *
 * @author Mattias Reichel
 * @since 2.0.0
 */
@CompileStatic
class ServerSideRenderer {

    private final ServerSideRenderConfig ssr

    ServerSideRenderer(ServerSideRenderConfig config) {
        ssr = config
    }

    String render(InertiaPage page) {
        if (!ssr.enabled) return null
        // Pass the page to the Inertia SSR Node Server
        String ssrResult = ((HttpURLConnection) new URL(ssr.url).openConnection()).with {
            requestMethod = 'POST'
            doOutput = true
            outputStream.withWriter {
                it << JsonOutput.toJson(page)
            }
            inputStream.text
        }
        return ssrResult
    }
}

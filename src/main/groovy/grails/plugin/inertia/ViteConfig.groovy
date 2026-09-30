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

import grails.config.Config
import grails.util.Environment

/**
 * How the Inertia page loads its JavaScript: from the Vite dev server, or from the assets built by Vite.
 *
 * @since 4.0
 */
@CompileStatic
class ViteConfig {

    static final String DEV_SERVER_ENABLED = 'inertia.vite.devServer.enabled'
    static final String DEV_SERVER_URL = 'inertia.vite.devServer.url'
    static final String ENTRY = 'inertia.vite.entry'

    static final String DEFAULT_DEV_SERVER_URL = 'http://localhost:3000'
    static final String DEFAULT_ENTRY = 'src/main/javascript/main.js'

    /** Whether the JavaScript is loaded from the Vite dev server instead of the built assets. */
    final boolean devServerEnabled

    /** The URL of the Vite dev server, without a trailing slash. */
    final String devServerUrl

    /** The entry point of the JavaScript application, as known to Vite. */
    final String entry

    ViteConfig(boolean devServerEnabled, String devServerUrl, String entry) {
        this.devServerEnabled = devServerEnabled
        this.devServerUrl = devServerUrl.replaceAll('/+$', '')
        this.entry = entry
    }

    /**
     * Resolves the configuration. The dev server is used in the development environment
     * unless {@code inertia.vite.devServer.enabled} says otherwise.
     *
     * @param config the application configuration
     * @return the resolved configuration
     */
    static ViteConfig from(Config config) {
        new ViteConfig(
                config.getProperty(DEV_SERVER_ENABLED, Boolean, Environment.current == Environment.DEVELOPMENT),
                config.getProperty(DEV_SERVER_URL, String, DEFAULT_DEV_SERVER_URL),
                config.getProperty(ENTRY, String, DEFAULT_ENTRY)
        )
    }
}

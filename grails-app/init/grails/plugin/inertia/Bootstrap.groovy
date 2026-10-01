/*
 * Copyright 2022-present original authors
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

import java.util.concurrent.TimeUnit

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import grails.core.GrailsApplication

import grails.plugin.inertia.ssr.BundleDetector
import grails.plugin.inertia.ssr.ServerSideRenderConfig

/**
 * A class that handles startup and shutdown tasks.
 *
 * @author Mattias Reichel
 * @since 1.0.0
 */
@Slf4j
@CompileStatic
class BootStrap {

    private static final long STOP_TIMEOUT_SECONDS = 5

    GrailsApplication grailsApplication

    private Process ssrProcess

    def init = {
        var ssr = ssrConfig
        if (ssr.enabled) {
            // Registered before the process is started, so that it is stopped whatever happens after
            addShutdownHook {
                stopSSR()
            }
            startSSR(ssr)
        }
    }

    def destroy = {
        stopSSR()
    }

    ServerSideRenderConfig getSsrConfig() {
        grailsApplication.mainContext.getBean(ServerSideRenderConfig)
    }

    void startSSR(ServerSideRenderConfig ssr) {

        log.debug('Trying to start SSR process...')

        // Parsed before the process is started, so that an invalid URL does not leave a process behind
        var url = parseUrl(ssr.url)
        var bundle = BundleDetector.detect(ssr.bundle)
        var bundleConfigured = ssr.bundle && ssr.bundle != ServerSideRenderConfig.DEFAULT_BUNDLE

        if (!bundle) {
            log.error(
                    (bundleConfigured ?
                            /Inertia SSR bundle not found at configured path: "${ssr.bundle}"/ :
                            'Inertia SSR bundle not found. Set the correct Inertia SSR bundle path ' +
                            'in your inertia.ssr.bundle config.'
                    ) as String
            )
            return
        }
        if (bundleConfigured && bundle != ssr.bundle) {
            log.warn(
                    /Inertia SSR bundle not found at configured path: "{}", / +
                    /using a default bundle instead: "{}"/,
                    ssr.bundle, bundle
            )
        }

        ssrProcess = startProcess(bundle)
        try {
            waitForSsrServer(url.host, url.port != -1 ? url.port : url.defaultPort)
        } catch (Throwable e) {
            stopSSR()
            throw e
        }
        log.debug(
                'SSR process started with pid: {}',
                ssrProcess.pid()
        )
    }

    Process startProcess(String bundle) {
        new ProcessBuilder()
                .inheritIO()
                .command('node', bundle)
                .start()
    }

    void stopSSR() {
        var process = ssrProcess
        ssrProcess = null
        if (process) {
            log.debug(
                    'Stopping SSR process with pid: {}',
                    process.pid()
            )
            process.destroy()
            if (!process.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.debug('SSR process did not stop, stopping it forcibly')
                process.destroyForcibly()
            }
        }
    }

    private static URL parseUrl(String url) {
        try {
            URI.create(url).toURL()
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Inertia SSR URL in inertia.ssr.url: \"$url\" (${e.message})", e)
        }
    }

    void waitForSsrServer(String host, int port, int timeoutMs = 1000, int maxRetries = 10) {
        boolean portOpen = false
        int tryNumber = 0
        while (!portOpen && ++tryNumber <= maxRetries) {
            log.debug(
                    'Checking if SSR server is up on {}:{} ({}/{})...',
                    host, port, tryNumber, maxRetries
            )
            try (Socket ignore = new Socket(host, port)) {
                // If the socket is successfully created, the port is open
                log.debug('SSR server is up!')
                portOpen = true
            } catch (IOException ignore) {
                // Port is not open yet, wait for some time before retrying
                log.debug(
                        'SSR server is not responding yet, retrying in {} ms...',
                        timeoutMs
                )
                try {
                    Thread.sleep(timeoutMs)
                } catch (InterruptedException ex) {
                    ex.printStackTrace()
                }
            }
        }
        if (!portOpen) {
            throw new IllegalStateException("SSR server is not responding on $host:$port")
        }
    }
}

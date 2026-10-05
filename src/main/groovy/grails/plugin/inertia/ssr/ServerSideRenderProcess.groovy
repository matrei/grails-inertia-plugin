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
package grails.plugin.inertia.ssr

import java.util.concurrent.TimeUnit

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.web.server.context.WebServerApplicationContext
import org.springframework.context.SmartLifecycle

/**
 * Runs the Inertia SSR server as a {@code node} process for as long as the application context runs.
 *
 * <p>It is started before the web server and stopped after it, so that no request is rendered while the SSR server
 * is unavailable.</p>
 *
 * @since 4.0
 */
@Slf4j
@CompileStatic
class ServerSideRenderProcess implements SmartLifecycle, DisposableBean {

    /** Just before the web server, which is started later and stopped earlier. */
    public static final int PHASE = WebServerApplicationContext.START_STOP_LIFECYCLE_PHASE - 1

    private static final long STOP_TIMEOUT_SECONDS = 5

    private final ServerSideRenderConfig ssr

    private volatile Process process

    ServerSideRenderProcess(ServerSideRenderConfig ssr) {
        this.ssr = ssr
    }

    @Override
    void start() {

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

        process = startProcess(bundle)
        try {
            waitForSsrServer(url.host, url.port != -1 ? url.port : url.defaultPort)
        } catch (Throwable e) {
            // The context fails to start, and does not stop a lifecycle that failed to start
            stop()
            throw e
        }
        log.debug(
                'SSR process started with pid: {}',
                process.pid()
        )
    }

    @Override
    void stop() {
        var stopping = process
        process = null
        if (stopping) {
            log.debug(
                    'Stopping SSR process with pid: {}',
                    stopping.pid()
            )
            stopping.destroy()
            if (!stopping.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.debug('SSR process did not stop, stopping it forcibly')
                stopping.destroyForcibly()
            }
        }
    }

    @Override
    boolean isRunning() {
        process != null
    }

    @Override
    int getPhase() {
        PHASE
    }

    /**
     * Stops the process when the context is destroyed without being stopped, which is the case when it fails to
     * start after this lifecycle was started.
     */
    @Override
    void destroy() {
        stop()
    }

    @SuppressWarnings('GrMethodMayBeStatic') // Not static, so that tests and subclasses can replace it
    Process startProcess(String bundle) {
        new ProcessBuilder()
                .inheritIO()
                .command('node', bundle)
                .start()
    }

    private static URL parseUrl(String url) {
        try {
            URI.create(url).toURL()
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Inertia SSR URL in inertia.ssr.url: \"$url\" (${e.message})", e)
        }
    }

    @SuppressWarnings('GrMethodMayBeStatic') // Not static, so that tests and subclasses can replace it
    void waitForSsrServer(String host, int port, int timeoutMs = 1000, int maxRetries = 10) {
        boolean portOpen = false
        int tryNumber = 0
        while (!portOpen && ++tryNumber <= maxRetries) {
            log.debug(
                    'Checking if SSR server is up on {}:{} ({}/{})...',
                    host, port, tryNumber, maxRetries
            )
            try {
                new Socket(host, port).close()
                // If the socket could be opened, the server is up
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

package grails.plugin.inertia

import java.util.concurrent.TimeUnit

import spock.lang.Specification
import spock.lang.TempDir

import grails.plugin.inertia.ssr.ServerSideRenderConfig

class BootStrapSpec extends Specification {

    @TempDir
    File tempDir

    BootStrap bootStrap = Spy()

    Process process = Mock()

    String bundle

    void setup() {
        bundle = new File(tempDir, 'ssr.mjs').tap { text = '' }.path
    }

    void 'the default url is used when none is configured'() {

        when: 'starting the SSR server without a configured url'
            bootStrap.startSSR(new ServerSideRenderConfig(enabled: true, bundle: bundle))

        then: 'the process is started and the default url is waited for'
            1 * bootStrap.startProcess(bundle) >> process
            1 * bootStrap.waitForSsrServer('localhost', 13714) >> {}
    }

    void 'the configured url #url is used'(String url, String host, int port) {

        when: 'starting the SSR server with a configured url'
            bootStrap.startSSR(new ServerSideRenderConfig(enabled: true, bundle: bundle, url: url))

        then: 'its host and port are waited for'
            1 * bootStrap.startProcess(bundle) >> process
            1 * bootStrap.waitForSsrServer(host, port) >> {}

        where:
            url                            || host         | port
            'http://127.0.0.1:9999/render' || '127.0.0.1'  | 9999
            'http://ssr.internal/render'   || 'ssr.internal' | 80
    }

    void 'an invalid url #url fails before a process is started'(String url) {

        when: 'starting the SSR server with an invalid url'
            bootStrap.startSSR(new ServerSideRenderConfig(enabled: true, bundle: bundle, url: url))

        then: 'the failure names the setting'
            var e = thrown(IllegalArgumentException)
            e.message.contains('inertia.ssr.url')
            e.message.contains(url)

        and: 'no process is started'
            0 * bootStrap.startProcess(_)

        where:
            url << ['not a url', 'localhost:13714/render']
    }

    void 'the process is stopped when the SSR server does not come up'() {

        when: 'starting the SSR server'
            bootStrap.startSSR(new ServerSideRenderConfig(enabled: true, bundle: bundle))

        then: 'the server does not respond'
            1 * bootStrap.startProcess(bundle) >> process
            1 * bootStrap.waitForSsrServer('localhost', 13714) >> {
                throw new IllegalStateException('SSR server is not responding on localhost:13714')
            }

        and: 'the started process is stopped and the failure is passed on'
            1 * process.destroy()
            1 * process.waitFor(5, TimeUnit.SECONDS) >> true
            0 * process.destroyForcibly()
            thrown(IllegalStateException)
    }

    void 'a process that does not stop is stopped forcibly'() {

        given: 'a started SSR server'
            bootStrap.startProcess(bundle) >> process
            bootStrap.waitForSsrServer(_, _) >> {}
            bootStrap.startSSR(new ServerSideRenderConfig(enabled: true, bundle: bundle))

        when: 'stopping it'
            bootStrap.stopSSR()

        then: 'it is destroyed forcibly when it does not end in time'
            1 * process.destroy()
            1 * process.waitFor(5, TimeUnit.SECONDS) >> false
            1 * process.destroyForcibly()
    }

    void 'the process is stopped when the application shuts down'() {

        given: 'SSR is enabled and the SSR server started'
            bootStrap.ssrConfig >> new ServerSideRenderConfig(enabled: true, bundle: bundle)
            bootStrap.startProcess(bundle) >> process
            bootStrap.waitForSsrServer(_, _) >> {}
            bootStrap.init.call()

        when: 'the application shuts down'
            bootStrap.destroy.call()

        then: 'the process is stopped'
            1 * process.destroy()
            1 * process.waitFor(5, TimeUnit.SECONDS) >> true
    }

    void 'no process is started when SSR is disabled'() {

        given: 'SSR is not enabled'
            bootStrap.ssrConfig >> new ServerSideRenderConfig(bundle: bundle)

        when: 'the application starts'
            bootStrap.init.call()

        then: 'no process is started'
            0 * bootStrap.startProcess(_)
    }

    void 'no process is started when the bundle is missing'() {

        when: 'starting the SSR server with a bundle that does not exist'
            bootStrap.startSSR(new ServerSideRenderConfig(enabled: true, bundle: new File(tempDir, 'missing.mjs').path))

        then: 'SSR is skipped without failing'
            0 * bootStrap.startProcess(_)
            noExceptionThrown()
    }
}

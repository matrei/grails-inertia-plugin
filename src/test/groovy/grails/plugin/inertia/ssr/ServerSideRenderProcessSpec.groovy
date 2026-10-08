package grails.plugin.inertia.ssr

import java.util.concurrent.TimeUnit

import org.springframework.boot.web.server.context.WebServerApplicationContext
import spock.lang.Specification
import spock.lang.TempDir

class ServerSideRenderProcessSpec extends Specification {

    @TempDir
    File tempDir

    Process process = Mock()

    String bundle

    void setup() {
        bundle = new File(tempDir, 'ssr.mjs').tap { text = '' }.path
    }

    void 'the default url is used when none is configured'() {

        given: 'no configured url'
            final ssr = ssrProcess(bundle: bundle)

        when: 'starting'
            ssr.start()

        then: 'the process is started and the default url is waited for'
            1 * ssr.startProcess(bundle) >> process
            1 * ssr.waitForSsrServer('localhost', 13714) >> {}
            ssr.running
    }

    void 'the configured url #url is used'(String url, String host, int port) {

        given: 'a configured url'
            final ssr = ssrProcess(bundle: bundle, url: url)

        when: 'starting'
            ssr.start()

        then: 'its host and port are waited for'
            1 * ssr.startProcess(bundle) >> process
            1 * ssr.waitForSsrServer(host, port) >> {}

        where:
            url                            || host           | port
            'http://127.0.0.1:9999/render' || '127.0.0.1'    | 9999
            'http://ssr.internal/render'   || 'ssr.internal' | 80
    }

    void 'an invalid url #url fails before a process is started'(String url) {

        given: 'an invalid url'
            final ssr = ssrProcess(bundle: bundle, url: url)

        when: 'starting'
            ssr.start()

        then: 'the failure names the setting'
            final e = thrown(IllegalArgumentException)
            e.message.contains('inertia.ssr.url')
            e.message.contains(url)

        and: 'no process is started'
            0 * ssr.startProcess(_)
            !ssr.running

        where:
            url << ['not a url', 'localhost:13714/render']
    }

    void 'the process is stopped when the SSR server does not come up'() {

        given: 'an SSR server that does not respond'
            final ssr = ssrProcess(bundle: bundle)

        when: 'starting'
            ssr.start()

        then: 'the server does not respond'
            1 * ssr.startProcess(bundle) >> process
            1 * ssr.waitForSsrServer('localhost', 13714) >> {
                throw new IllegalStateException('SSR server is not responding on localhost:13714')
            }

        and: 'the started process is stopped and the failure is passed on'
            1 * process.destroy()
            1 * process.waitFor(5, TimeUnit.SECONDS) >> true
            0 * process.destroyForcibly()
            thrown(IllegalStateException)
            !ssr.running
    }

    void 'the process is stopped by #method'(String method) {

        given: 'a started SSR server'
            final ssr = started(ssrProcess(bundle: bundle))

        when: 'the context stops or destroys it'
            ssr."$method"()

        then: 'the process is stopped'
            1 * process.destroy()
            1 * process.waitFor(5, TimeUnit.SECONDS) >> true
            !ssr.running

        where:
            method << ['stop', 'destroy']
    }

    void 'a process that does not stop is stopped forcibly'() {

        given: 'a started SSR server'
            final ssr = started(ssrProcess(bundle: bundle))

        when: 'stopping it'
            ssr.stop()

        then: 'it is destroyed forcibly when it does not end in time'
            1 * process.destroy()
            1 * process.waitFor(5, TimeUnit.SECONDS) >> false
            1 * process.destroyForcibly()
    }

    void 'no process is started when the bundle is missing'() {

        given: 'a bundle that does not exist'
            final ssr = ssrProcess(bundle: new File(tempDir, 'missing.mjs').path)

        when: 'starting'
            ssr.start()

        then: 'SSR is skipped without failing'
            0 * ssr.startProcess(_)
            !ssr.running
            noExceptionThrown()
    }

    void 'waiting for the SSR server ends as soon as it accepts connections'() {

        given: 'a server listening on a free port'
            final server = new ServerSocket(0)

        when: 'waiting for it'
            new ServerSideRenderProcess(new ServerSideRenderConfig()).waitForSsrServer('localhost', server.localPort, 10, 2)

        then: 'it is found'
            noExceptionThrown()

        cleanup:
            server?.close()
    }

    void 'waiting for an SSR server that does not respond fails after the retries'() {

        given: 'a port nothing listens on'
            final port = new ServerSocket(0).withCloseable { it.localPort }

        when: 'waiting for it'
            new ServerSideRenderProcess(new ServerSideRenderConfig()).waitForSsrServer('localhost', port, 10, 2)

        then: 'the failure names the host and port'
            final e = thrown(IllegalStateException)
            e.message == "SSR server is not responding on localhost:$port"
    }

    void 'it is started before and stopped after the web server'() {

        expect: 'a lower phase than the web server'
            new ServerSideRenderProcess(new ServerSideRenderConfig()).phase < WebServerApplicationContext.START_STOP_LIFECYCLE_PHASE
    }

    private ServerSideRenderProcess ssrProcess(Map<String, Object> config) {
        Spy(ServerSideRenderProcess, constructorArgs: [new ServerSideRenderConfig(config)]) as ServerSideRenderProcess
    }

    private ServerSideRenderProcess started(ServerSideRenderProcess ssr) {
        ssr.startProcess(bundle) >> process
        ssr.waitForSsrServer(_, _) >> {}
        ssr.start()
        ssr
    }
}

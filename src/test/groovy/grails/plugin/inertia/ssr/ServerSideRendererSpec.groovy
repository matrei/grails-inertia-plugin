package grails.plugin.inertia.ssr

import com.sun.net.httpserver.HttpServer

import grails.plugin.inertia.InertiaPage
import spock.lang.Specification

class ServerSideRendererSpec extends Specification {

    private HttpServer server

    def cleanup() {
        server?.stop(0)
    }

    def 'renders a successful JSON response'() {
        given:
            server = HttpServer.create(new InetSocketAddress('localhost', 0), 0)
            server.createContext('/render') { exchange ->
                assert exchange.requestMethod == 'POST'
                assert exchange.requestHeaders.getFirst('Content-Type') == 'application/json'
                byte[] body = '{"body":"rendered"}'.bytes
                exchange.sendResponseHeaders(200, body.length)
                exchange.responseBody.withCloseable { it.write(body) }
            }
            server.start()
            final config = new ServerSideRenderConfig(
                    enabled: true,
                    url: "http://localhost:${server.address.port}/render"
            )

        when:
            final result = new ServerSideRenderer(config).render(page())

        then:
            result == '{"body":"rendered"}'
    }

    def 'falls back when the SSR service returns an error'() {
        given:
            server = HttpServer.create(new InetSocketAddress('localhost', 0), 0)
            server.createContext('/render') { exchange ->
                exchange.sendResponseHeaders(500, -1)
                exchange.close()
            }
            server.start()
            final config = new ServerSideRenderConfig(
                    enabled: true,
                    url: "http://localhost:${server.address.port}/render"
            )

        expect:
            new ServerSideRenderer(config).render(page()) == null
    }

    def 'falls back when the SSR service is unavailable'() {
        given:
            final config = new ServerSideRenderConfig(
                    enabled: true,
                    url: 'http://localhost:1/render',
                    connectTimeout: 50,
                    readTimeout: 50
            )

        expect:
            new ServerSideRenderer(config).render(page()) == null
    }

    private static InertiaPage page() {
        new InertiaPage(
                component: 'Dashboard',
                props: [title: 'Dashboard'],
                url: '/dashboard',
                version: '1'
        )
    }
}

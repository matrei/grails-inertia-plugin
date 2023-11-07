package grails.plugin.inertia

import grails.testing.web.interceptor.InterceptorUnitTest
import grails.web.Controller
import spock.lang.Specification

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT
import static javax.servlet.http.HttpServletResponse.SC_OK

class InertiaInterceptorSpec extends Specification implements InterceptorUnitTest<InertiaInterceptor> {

    Closure doWithConfig() {
        { it.inertia.manifest.location = 'classpath:location/of/the/manifest.json' } as Closure
    }

    void 'the inertia interceptor matches all controller requests'() {

        when: 'a request comes in for any controller action'
        withRequest controller: 'any'

        then: 'the interceptor matches'
        interceptor.doesMatch()
    }

    void 'the inertia interceptor does not match requests for assets'() {

        when: 'a request comes in for an asset'
        withRequest uri: '/static/dist/main.js'

        then: 'the interceptor does not match'
        !interceptor.doesMatch()
    }

    void 'inertia #method requests from stale assets returns appropriately'(String action, String method, String location, int status) {

        given: 'a controller'
        def controller = (TestController) mockController(TestController)

        when: 'an inertia request with an outdated asset version is handled'
        request.addHeader 'X-Inertia', true
        request.addHeader 'X-Inertia-Version', 'a value that is certain to be deemed as stale'
        request.setForwardURI location
        request.method = method
        withInterceptors(controller: 'test', action: action, httpMethod: method) { controller.index() }
        interceptor.after()

        then: 'the interceptor handles the request appropriately'
        response.getHeader('X-Inertia-Location') == location
        response.status == status

        where:
        action    | method    | location   | status
        'index'   | 'GET'     | ""         | SC_CONFLICT
        'testing' | 'GET'     | "/testing" | SC_CONFLICT
        'index'   | 'HEAD'    | null       | SC_OK
        'index'   | 'POST'    | null       | SC_OK
        'index'   | 'PUT'     | null       | SC_OK
        'index'   | 'DELETE'  | null       | SC_OK
        'index'   | 'CONNECT' | null       | SC_OK
        'index'   | 'OPTIONS' | null       | SC_OK
        'index'   | 'TRACE'   | null       | SC_OK
        'index'   | 'PATCH'   | null       | SC_OK
    }

    void 'the http headers are correct for html responses'() {

        given: 'a controller'
        def controller = (TestController) mockController(TestController)

        when: 'a request for html is processed'
        withInterceptors(controller: 'test', httpMethod: 'GET') { controller.index() }
        interceptor.after()

        then: 'X-Inertia is one of the Vary header values'
        'text/html;charset=UTF-8'.equalsIgnoreCase response.contentType
        'X-Inertia' in response.getHeaders('Vary')
    }

    def 'the http headers are correct for json responses'() {
        given: 'a controller'
        def controller = (TestController) mockController(TestController)

        when: 'a request for json is processed'
        request.addHeader 'X-Inertia', true
        request.addHeader 'X-Inertia-Version', '0'
        withInterceptors(controller: 'test', httpMethod: 'GET') { controller.index() }
        interceptor.after()

        then: 'X-Inertia is one of the Vary header values'
        'application/json;charset=UTF-8'.equalsIgnoreCase response.contentType
        'X-Inertia' in response.getHeaders('Vary')
    }

    def 'canceling Inertia request works'() {
        given: 'a controller'
        def controller = (TestController) mockController(TestController)

        when: 'a request for json is processed'
        request.addHeader 'X-Inertia', true
        request.addHeader 'X-Inertia-Version', '0'
        withInterceptors(controller: 'test', httpMethod: 'GET') { controller.index() }
        request.setAttribute Inertia.INERTIA_ATTRIBUTE_CANCEL_INERTIA, true
        interceptor.after()

        then: 'no inertia response headers are set'
        ! response.containsHeader('X-Inertia')
        ! ('X-Inertia' in response.getHeaders('Vary'))
    }

    def 'canceling Inertia request with Inertia.cancel() works'() {
        given: 'a controller'
        def controller = (TestController) mockController(TestController)

        when: 'a request for json is processed'
        request.addHeader 'X-Inertia', true
        request.addHeader 'X-Inertia-Version', '0'
        withInterceptors(controller: 'test', httpMethod: 'GET') { controller.cancelInertiaAction() }
        interceptor.after()

        then: 'no inertia response headers are set'
        ! response.containsHeader('X-Inertia')
        ! ('X-Inertia' in response.getHeaders('Vary'))
    }
}

@Controller
@SuppressWarnings('unused')
class TestController {

    def index() {
        renderInertia 'index', [hello: 'world']
    }

    def cancelInertiaAction() {
        Inertia.cancel()
        render 'cancelInertiaAction'
    }

    def testing() {
        renderInertia 'testing'
    }
}

package grails.plugin.inertia

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification
import grails.plugin.inertia.controllers.TestController

import static grails.plugin.inertia.Inertia.INERTIA_HEADER_NAME
import static grails.plugin.inertia.Inertia.INERTIA_HEADER_LOCATION
import static grails.plugin.inertia.Inertia.INERTIA_HEADER_VALUE
import static grails.plugin.inertia.Inertia.INERTIA_HEADER_VERSION

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT
import static javax.servlet.http.HttpServletResponse.SC_OK


class InertiaInterceptorSpec extends Specification implements InterceptorUnitTest<InertiaInterceptor> {

    Closure doWithConfig() {
        { it.inertia.manifest.location = 'classpath:location/of/the/manifest.json' } as Closure
    }

    void 'the inertia interceptor matches all requests'() {

        when: 'a request comes in for any controller action'
        withRequest controller: 'any'

        then: 'the interceptor does match'
        interceptor.doesMatch()
    }

    void 'inertia #method requests from stale assets returns appropriately'(String action, String method, String location, int status) {

        given: 'a controller'
        def controller = mockController(TestController) as TestController

        when: 'an inertia request with an outdated asset version is handled'
        request.method = method
        request.addHeader INERTIA_HEADER_NAME, INERTIA_HEADER_VALUE
        request.addHeader INERTIA_HEADER_VERSION, 'a value that is certain to be deemed as stale'
        request.setForwardURI location
        withInterceptors(controller: 'test') { controller[action]() }

        then: 'the interceptor handles the request appropriately'
        response.getHeader(INERTIA_HEADER_LOCATION) == location
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
}

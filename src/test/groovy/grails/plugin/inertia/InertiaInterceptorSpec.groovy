package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.interceptor.InterceptorUnitTest
import grails.web.Controller

import static jakarta.servlet.http.HttpServletResponse.SC_CONFLICT
import static jakarta.servlet.http.HttpServletResponse.SC_FOUND
import static jakarta.servlet.http.HttpServletResponse.SC_OK

class InertiaInterceptorSpec extends Specification implements InterceptorUnitTest<InertiaInterceptor> {

    Closure doWithConfig() {
        { it.inertia.manifest.location = 'classpath:location/of/the/manifest.json' } as Closure
    }

    void 'the inertia interceptor matches all controller requests'() {

        when: 'a request comes in for any controller action'
            withRequest(controller: 'any')

        then: 'the interceptor matches'
            interceptor.doesMatch()
    }

    void 'the inertia interceptor does not match requests for assets'() {

        when: 'a request comes in for an asset'
            withRequest(uri: '/static/dist/main.js')

        then: 'the interceptor does not match'
            !interceptor.doesMatch()
    }

    void 'inertia #method requests from stale assets returns appropriately'(String action, String method, String location, int status) {

        given: 'a controller'
            def controller = mockController(TestController) as TestController

        when: 'an inertia request with an outdated asset version is handled'
            request.addHeader('X-Inertia', true)
            request.addHeader('X-Inertia-Version', 'a value that is certain to be deemed as stale')
            request.setForwardURI(location)
            request.method = method
            withInterceptors(controller: 'test', action: action, httpMethod: method) {
                controller.index()
            }
            interceptor.after()

        then: 'the interceptor handles the request appropriately'
            response.getHeader('X-Inertia-Location') == location
            response.status == status

        where:
            action    | method    | location   || status
            'index'   | 'GET'     | ''         || SC_CONFLICT
            'testing' | 'GET'     | '/testing' || SC_CONFLICT
            'index'   | 'HEAD'    | null       || SC_OK
            'index'   | 'POST'    | null       || SC_OK
            'index'   | 'PUT'     | null       || SC_OK
            'index'   | 'DELETE'  | null       || SC_OK
            'index'   | 'CONNECT' | null       || SC_OK
            'index'   | 'OPTIONS' | null       || SC_OK
            'index'   | 'TRACE'   | null       || SC_OK
            'index'   | 'PATCH'   | null       || SC_OK
    }

    def 'version mismatch preserves the request query string and current version'() {
        given:
            def controller = mockController(TestController) as TestController
            request.addHeader('X-Inertia', true)
            request.addHeader('X-Inertia-Version', 'stale')
            request.setRequestURI('/users')
            request.setQueryString('page=2&active=true')
            request.method = 'GET'
            interceptor.before()
            withInterceptors(controller: 'test', action: 'testing', httpMethod: 'GET') {
                controller.testing()
            }

        when:
            interceptor.after()

        then:
            response.status == SC_CONFLICT
            response.getHeader('X-Inertia-Location') == '/users?page=2&active=true'
            response.getHeader('X-Inertia-Version') == request.getAttribute(Inertia.INERTIA_ATTRIBUTE_VERSION)
            response.getHeader('X-Inertia') == null
    }

    def 'version mismatch reflashes data for the follow-up request'() {
        given:
            def controller = mockController(TestController) as TestController
            Inertia.flash.put('notice', 'Saved')
            request.addHeader('X-Inertia', true)
            request.addHeader('X-Inertia-Version', 'stale')
            request.method = 'GET'
            interceptor.before()
            withInterceptors(controller: 'test', action: 'testing', httpMethod: 'GET') {
                controller.testing()
            }

        when:
            interceptor.after()

        then:
            response.status == SC_CONFLICT
            Inertia.flash.get('notice') == 'Saved'
    }

    def 'Inertia request header matching ignores case and surrounding whitespace'() {
        given:
            request.addHeader('X-Inertia', ' TRUE ')

        expect:
            interceptor.isInertiaRequest
    }

    void 'the http headers are correct for html responses'() {

        given: 'a controller'
            def controller = mockController(TestController) as TestController

        when: 'a request for html is processed'
            withInterceptors(controller: 'test', httpMethod: 'GET') {
                controller.index()
            }
            interceptor.after()

        then: 'X-Inertia is one of the Vary header values'
            response.contentType.equalsIgnoreCase('text/html;charset=UTF-8')
            'X-Inertia' in response.getHeaders('Vary')
    }

    def 'the http headers are correct for json responses'() {
        given: 'a controller'
            def controller = mockController(TestController) as TestController

        when: 'a request for json is processed'
            request.addHeader('X-Inertia', true)
            request.addHeader('X-Inertia-Version', '0')
            withInterceptors(controller: 'test', httpMethod: 'GET') {
                controller.index()
            }
            interceptor.after()

        then: 'X-Inertia is one of the Vary header values'
            response.contentType.equalsIgnoreCase('application/json;charset=UTF-8')
            'X-Inertia' in response.getHeaders('Vary')
    }

    def 'an Inertia partial request produces a filtered page response'() {
        given:
            def controller = mockController(TestController) as TestController
            request.addHeader('X-Inertia', true)
            request.addHeader('X-Inertia-Version', '0')
            request.addHeader('X-Inertia-Partial-Component', 'partial')
            request.addHeader('X-Inertia-Partial-Data', 'users')

        when:
            withInterceptors(controller: 'test', action: 'partial', httpMethod: 'GET') {
                controller.partial()
            }
            interceptor.after()

        then:
            response.contentType.equalsIgnoreCase('application/json;charset=UTF-8')
            response.getHeader('X-Inertia') == 'true'
            'X-Inertia' in response.getHeaders('Vary')
    }

    def 'page factory accepts immutable resolved props when adding defaults'() {
        given:
            def controller = mockController(TestController) as TestController
            def holder = [:]

        when:
            withInterceptors(controller: 'test', action: 'testing', httpMethod: 'GET') {
                holder.page = InertiaResponseFactory.createPage(
                        'Dashboard',
                        [title: 'Dashboard']
                )
            }

        then:
            holder.page.props.title == 'Dashboard'
            holder.page.props.errors == []
    }

    def 'canceling Inertia request works'() {
        given: 'a controller'
            def controller = mockController(TestController) as TestController

        when: 'a request for json is processed'
            request.addHeader('X-Inertia', true)
            request.addHeader('X-Inertia-Version', '0')
            withInterceptors(controller: 'test', httpMethod: 'GET') {
                controller.index()
            }
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_CANCEL_INERTIA, true)
            interceptor.after()

        then: 'no inertia response headers are set'
            ! response.containsHeader('X-Inertia')
            ! ('X-Inertia' in response.getHeaders('Vary'))
    }

    def 'canceling Inertia request with Inertia.cancel() works'() {
        given: 'a controller'
            def controller = (TestController) mockController(TestController)

        when: 'a request for json is processed'
            request.addHeader('X-Inertia', true)
            request.addHeader('X-Inertia-Version', '0')
            withInterceptors(controller: 'test', httpMethod: 'GET') {
                controller.cancelInertiaAction()
            }
            interceptor.after()

        then: 'no inertia response headers are set'
            ! response.containsHeader('X-Inertia')
            ! ('X-Inertia' in response.getHeaders('Vary'))
    }

    def 'page history controls are exposed through the Inertia facade'() {
        given:
            def controller = (TestController) mockController(TestController)
            request.addHeader('X-Inertia', true)
            request.addHeader('X-Inertia-Version', '0')

        when:
            withInterceptors(controller: 'test', httpMethod: 'GET') {
                controller.pageControlsAction()
            }

        then:
            request.getAttribute(Inertia.INERTIA_ATTRIBUTE_CLEAR_HISTORY)
            request.getAttribute(Inertia.INERTIA_ATTRIBUTE_ENCRYPT_HISTORY)
            request.getAttribute(Inertia.INERTIA_ATTRIBUTE_PRESERVE_FRAGMENT)
    }

    def 'redirects with fragments use the Inertia redirect header'() {
        given:
            def controller = mockController(TestController) as TestController
            request.addHeader('X-Inertia', true)
            request.method = 'GET'
            interceptor.before()
            request.addHeader('X-Inertia-Version', request.getAttribute(Inertia.INERTIA_ATTRIBUTE_VERSION))
            withInterceptors(controller: 'test', httpMethod: 'GET') {
                controller.index()
            }
            response.setHeader('Location', '/users#details')
            response.status = 302

        when:
            interceptor.after()

        then:
            response.status == SC_CONFLICT
            response.getHeader('X-Inertia-Redirect') == '/users#details'
    }

    def 'prefetch redirects with fragments are not converted'() {
        given:
            def controller = mockController(TestController) as TestController
            request.addHeader('X-Inertia', true)
            request.addHeader('Purpose', 'prefetch')
            request.method = 'GET'
            interceptor.before()
            request.addHeader('X-Inertia-Version', request.getAttribute(Inertia.INERTIA_ATTRIBUTE_VERSION))
            withInterceptors(controller: 'test', httpMethod: 'GET') {
                controller.index()
            }
            response.setHeader('Location', '/users#details')
            response.status = 302

        when:
            interceptor.after()

        then:
            response.status == SC_FOUND
            response.getHeader('Location') == '/users#details'
            response.getHeader('X-Inertia-Redirect') == null
    }
}

@Controller
@SuppressWarnings('unused')
class TestController {

    def index() {
        renderInertia('index', [hello: 'world'])
    }

    def cancelInertiaAction() {
        Inertia.cancel()
        render('cancelInertiaAction')
    }

    def pageControlsAction() {
        Inertia.clearHistory()
        Inertia.encryptHistory()
        Inertia.preserveFragment()
        renderInertia('controls')
    }

    def partial() {
        renderInertia('partial', [
                users: ['Mattias'],
                companies: ['Acme']
        ])
    }

    def testing() {
        renderInertia('testing')
    }
}

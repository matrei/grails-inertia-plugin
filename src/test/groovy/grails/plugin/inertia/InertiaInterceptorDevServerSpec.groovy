package grails.plugin.inertia

import org.springframework.web.servlet.ModelAndView
import spock.lang.Specification

import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.web.util.GrailsApplicationAttributes

import static jakarta.servlet.http.HttpServletResponse.SC_OK

class InertiaInterceptorDevServerSpec extends Specification implements InterceptorUnitTest<InertiaInterceptor> {

    Closure doWithConfig() {
        { config ->
            config.inertia.vite.devServer.enabled = true
            config.inertia.manifest.location = 'classpath:there/is/no/manifest.json'
        } as Closure
    }

    void 'the page gets the dev server configuration and no manifest'() {

        given: 'the html view is rendered'
            def modelAndView = new ModelAndView(Inertia.INERTIA_VIEW_HTML, [:])
            request.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView)

        when: 'the interceptor handles the response'
            interceptor.after()

        then: 'the dev server is used, without a manifest, which was not loaded either'
            (modelAndView.model[Inertia.INERTIA_ATTRIBUTE_VITE] as ViteConfig).devServerEnabled
            !modelAndView.model.containsKey(Inertia.INERTIA_ATTRIBUTE_MANIFEST)
    }

    void 'the asset version is not checked'() {

        given: 'an inertia request with a stale asset version'
            def controller = mockController(TestController) as TestController
            request.addHeader('X-Inertia', true)
            request.addHeader('X-Inertia-Version', 'stale')
            request.method = 'GET'

        when: 'the request is handled'
            withInterceptors(controller: 'test', action: 'index', httpMethod: 'GET') {
                controller.index()
            }
            interceptor.after()

        then: 'the dev server serves the current assets, so there is no conflict'
            response.status == SC_OK
            !response.getHeader('X-Inertia-Location')
    }
}

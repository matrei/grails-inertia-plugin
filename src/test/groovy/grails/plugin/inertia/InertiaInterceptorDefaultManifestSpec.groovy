package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.interceptor.InterceptorUnitTest

class InertiaInterceptorDefaultManifestSpec extends Specification implements InterceptorUnitTest<InertiaInterceptor> {

    void 'the manifest is loaded from the default location when none is configured'() {

        expect: 'the manifest written by Vite to the default output directory is loaded'
            interceptor.manifest == ['src/main/javascript/main.js': [file: 'js/main-default.js']]
    }
}

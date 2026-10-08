package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.interceptor.InterceptorUnitTest

class InertiaInterceptorDefaultManifestSpec extends Specification implements InterceptorUnitTest<InertiaInterceptor> {

    void 'the manifest is loaded from the default location when none is configured'() {

        expect: 'the entry is resolved from the manifest written by Vite to the default output directory'
            interceptor.viteEntry.file == 'js/main-default.js'
    }
}

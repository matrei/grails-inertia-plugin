package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.taglib.TagLibUnitTest

class InertiaTagLibTitleFallbackSpec extends Specification implements TagLibUnitTest<InertiaTagLib> {

    Closure doWithConfig() {
        { it.info.app.name = 'my-app' } as Closure
    }

    void 'the application name is the title when none is configured'() {

        expect: 'the application name as title'
            applyTemplate('<inertia:head/>') == '<title>my-app</title>'
    }
}

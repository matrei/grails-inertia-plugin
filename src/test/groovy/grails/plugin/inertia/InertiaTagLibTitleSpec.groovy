package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.taglib.TagLibUnitTest

class InertiaTagLibTitleSpec extends Specification implements TagLibUnitTest<InertiaTagLib> {

    Closure doWithConfig() {
        { config ->
            config.inertia.title = 'Tom & Jerry <3'
            config.info.app.name = 'the application name, which the configured title takes precedence over'
        } as Closure
    }

    void 'the configured title is output, html encoded'() {

        expect: 'the title element'
            applyTemplate('<inertia:head/>') == '<title>Tom &amp; Jerry &lt;3</title>'
    }

    void 'the configured title is output before server-side rendered head elements without a title'() {

        given: 'a server-side rendered response without a title'
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_SSR_RESPONSE, [head: ['<meta name="a" content="b">'], body: ''])

        expect: 'the configured title followed by the head elements'
            applyTemplate('<inertia:head/>') == '<title>Tom &amp; Jerry &lt;3</title><meta name="a" content="b">'
    }

    void 'a server-side rendered title replaces the configured title'() {

        given: 'a server-side rendered response with a title'
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_SSR_RESPONSE, [head: ['<title data-inertia="">Books</title>'], body: ''])

        expect: 'only the server-side rendered title'
            applyTemplate('<inertia:head/>') == '<title data-inertia="">Books</title>'
    }
}

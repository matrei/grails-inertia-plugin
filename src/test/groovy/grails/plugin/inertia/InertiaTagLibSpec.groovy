package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.taglib.TagLibUnitTest
class InertiaTagLibSpec extends Specification implements TagLibUnitTest<InertiaTagLib> {

    void 'inertia markup is created'() {

        given: 'some JSON content in the proper request attribute'
            def json = '{"msg":"hello"}'
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_PAGE, json)

        when: 'using it with the taglib'
            def output = applyTemplate('<inertia:app/>')

        then: 'the output is correct'
            output == $/<script data-page="app" type="application/json">{"msg":"hello"}</script><div id="app"></div>/$
    }

    void 'changing the id works'() {

        given: 'some JSON content in the proper request attribute and an id'
            def json = '{"msg":"hello"}'
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_PAGE, json)
            def id = 'myId'

        when: 'using them with the taglib'
            def output = applyTemplate($/<inertia:app id="$id"/>/$)

        then: 'the id is used for both the element and the page data, which the client looks up by id'
            output == $/<script data-page="$id" type="application/json">{"msg":"hello"}</script><div id="$id"></div>/$
    }

    void 'changing the tagName works'() {

        given: 'some JSON content in the proper request attribute and a tag name'
            def json = '{"msg":"hello"}'
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_PAGE, json)
            def tagName = 'span'

        when: 'using them with the taglib'
            def output = applyTemplate($/<inertia:app tagName="$tagName"/>/$)

        then: 'the output is correct'
            output == "<script data-page=\"app\" type=\"application/json\">{\"msg\":\"hello\"}</script><$tagName id=\"app\"></$tagName>"
    }

    void 'slashes in the page JSON are escaped for script safety'() {

        given:
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_PAGE, '{"url":"/users"}')

        expect:
            applyTemplate('<inertia:app/>') ==
                    $/<script data-page="app" type="application/json">{"url":"\/users"}</script><div id="app"></div>/$
    }

    void 'script closing sequences are escaped without HTML entity encoding'() {
        given:
            request.setAttribute(
                    Inertia.INERTIA_ATTRIBUTE_PAGE,
                    '{"content":"</script><script>alert(1)</script>"}'
            )

        when:
            def output = applyTemplate('<inertia:app/>')

        then:
            output ==
                    $/<script data-page="app" type="application/json">{"content":"<\/script><script>alert(1)<\/script>"}</script><div id="app"></div>/$
            !output.contains('&lt;')
            !output.contains('</script><script>')
    }

    void 'the server-side rendered body replaces the client-side markup'() {

        given: 'a server-side rendered response'
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_PAGE, '{"msg":"hello"}')
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_SSR_RESPONSE, [head: [], body: '<div id="app" data-server-rendered="true">hello</div>'])

        when: 'using it with the taglib'
            def output = applyTemplate('<inertia:app/>')

        then: 'the server-side rendered body is output as is'
            output == '<div id="app" data-server-rendered="true">hello</div>'
    }

    void 'the server-side rendered head elements are output'() {

        given: 'a server-side rendered response with head elements'
            request.setAttribute(Inertia.INERTIA_ATTRIBUTE_SSR_RESPONSE, [head: ['<title>Hello</title>', '<meta name="a" content="b">'], body: ''])

        when: 'using it with the taglib'
            def output = applyTemplate('<inertia:head/>')

        then: 'all head elements are output in order'
            output == '<title>Hello</title><meta name="a" content="b">'
    }

    void 'the head only has the title without server-side rendering'() {

        expect: 'the application name, which the Grails build provides, as title'
            applyTemplate('<inertia:head/>') == "<title>${config.getProperty('info.app.name')}</title>"
    }
}

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

        then: 'the output is correct'
            output == $/<script data-page="app" type="application/json">{"msg":"hello"}</script><div id="$id"></div>/$
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
}

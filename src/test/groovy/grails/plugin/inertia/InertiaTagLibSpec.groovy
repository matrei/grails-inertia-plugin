package grails.plugin.inertia

import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class InertiaTagLibSpec extends Specification implements TagLibUnitTest<InertiaTagLib> {

    void "inertia markup is created"() {

        given: "some JSON content"
        def json = '{&quot;msg&quot;:&quot;hello&quot;}'

        when: "using it with the taglib"
        def output = applyTemplate("<inertia:app>$json</inertia:app>")

        then: "the output is correct"
        output == $/<div id="app" data-page="$json"></div>/$
    }

    void "changing the id works"() {

        given: "some JSON content and an id"
        def json = '{&quot;msg&quot;:&quot;hello&quot;}'
        def id = 'myId'

        when: "using them with the taglib"
        def output = applyTemplate("<inertia:app id='$id'>$json</inertia:app>")

        then: "the output is correct"
        output == $/<div id="$id" data-page="$json"></div>/$
    }

    void "changing the tagName works"() {

        given: "some JSON content and a tag name"
        def json = '{&quot;msg&quot;:&quot;hello&quot;}'
        def tagName = 'span'

        when: "using them with the taglib"
        def output = applyTemplate("<inertia:app tagName='$tagName'>$json</inertia:app>")

        then: "the output is correct"
        output == "<$tagName id=\"app\" data-page=\"$json\"></$tagName>"
    }
}

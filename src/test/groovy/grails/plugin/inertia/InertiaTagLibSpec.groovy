package grails.plugin.inertia

import grails.testing.web.taglib.TagLibUnitTest
import org.grails.plugins.codecs.HTMLCodec
import spock.lang.Specification

class InertiaTagLibSpec extends Specification implements TagLibUnitTest<InertiaTagLib> {

    void setup() {
        mockCodec HTMLCodec
    }

    void "inertia markup is created"() {

        given: "some JSON content in the proper request attribute"
        def json = '{"msg":"hello"}'
        request.setAttribute Inertia.INERTIA_ATTRIBUTE_PAGE, json

        when: "using it with the taglib"
        def output = applyTemplate("<inertia:app/>")

        then: "the output is correct"
        output == $/<div id="app" data-page="${json.encodeAsHTML()}"></div>/$
    }

    void "changing the id works"() {

        given: "some JSON content in the proper request attribute and an id"
        def json = '{"msg":"hello"}'
        request.setAttribute Inertia.INERTIA_ATTRIBUTE_PAGE, json
        def id = 'myId'

        when: "using them with the taglib"
        def output = applyTemplate("<inertia:app id='$id'/>")

        then: "the output is correct"
        output == $/<div id="$id" data-page="${json.encodeAsHTML()}"></div>/$
    }

    void "changing the tagName works"() {

        given: "some JSON content in the proper request attribute and a tag name"
        def json = '{"msg":"hello"}'
        request.setAttribute Inertia.INERTIA_ATTRIBUTE_PAGE, json
        def tagName = 'span'

        when: "using them with the taglib"
        def output = applyTemplate("<inertia:app tagName='$tagName'/>")

        then: "the output is correct"
        output == "<$tagName id=\"app\" data-page=\"${json.encodeAsHTML()}\"></$tagName>"
    }
}

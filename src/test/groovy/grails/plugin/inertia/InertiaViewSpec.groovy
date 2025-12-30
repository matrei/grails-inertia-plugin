package grails.plugin.inertia

import spock.lang.Specification

import grails.plugin.json.view.test.JsonViewTest

class InertiaViewSpec extends Specification implements JsonViewTest {

    void 'Test render a raw JSON view'() {

        given: 'An Inertia page'
            def inertiaPage = new InertiaPage(
                    component: 'HelloWorld',
                    props: [name: 'Mattias'],
                    url: '/helloworld',
                    version: '1'
            )

        when: 'A json view is rendered'
            def result = render(
                    view: '/inertia/json',
                    model: [inertiaPage: inertiaPage]
            )

        then: 'The json is correct'
            result.json.component == 'HelloWorld'
            result.json.props.name == 'Mattias'
            result.json.url == '/helloworld'
            result.json.version == '1'
    }
}

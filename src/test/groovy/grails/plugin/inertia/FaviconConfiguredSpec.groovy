package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.GrailsWebUnitTest

class FaviconConfiguredSpec extends Specification implements GrailsWebUnitTest {

    def 'configured favicon is rendered in the html view'() {
        given: 'a configured favicon'
            config.inertia.favicon = '/static/favicon.svg'

        when: 'rendering the html view with a configured favicon'
            def output = render(
                    view: '/inertia/html',
                    model: [inertiaManifest: [:]]
            )

        then: 'the output contains the favicon link'
            output.contains('<link rel="icon" href="/static/favicon.svg"/>')
    }
}

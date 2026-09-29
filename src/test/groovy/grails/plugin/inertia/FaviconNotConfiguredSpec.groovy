package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.GrailsWebUnitTest

class FaviconNotConfiguredSpec extends Specification implements GrailsWebUnitTest {

    def 'favicon is omitted when it is not configured'() {
        when: 'rendering the html view without a configured favicon'
            def output = render(
                    view: '/inertia/html',
                    model: [inertiaManifest: [:]]
            )

        then: 'the output does not contain a favicon link'
            !output.contains('rel="icon"')
    }
}

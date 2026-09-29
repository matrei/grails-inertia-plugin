package grails.plugin.inertia

import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import grails.testing.web.GrailsWebUnitTest
import grails.util.Environment

class ContextPathSpec extends Specification implements GrailsWebUnitTest {

    private static final Map MANIFEST = [
            'src/main/javascript/main.js': [
                    file: 'js/main-abc123.js',
                    imports: ['_shared-ghi789.js'],
                    css: ['js/main-def456.css']
            ],
            '_shared-ghi789.js': [
                    file: 'js/shared-ghi789.js',
                    css: ['js/shared-jkl012.css']
            ]
    ]

    def setup() {
        request.contextPath = '/myapp'
    }

    @RestoreSystemProperties
    def 'production assets are prefixed with the context path'() {
        given: 'the production environment'
            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)

        when: 'rendering the html view'
            def output = render(
                    view: '/inertia/html',
                    model: [inertiaManifest: MANIFEST]
            )

        then: 'the stylesheets, the script and the preloaded modules are output in order, with the context path'
            output.readLines()*.trim().findAll { it.startsWith('<link rel="stylesheet"') || it.startsWith('<script type="module"') || it.startsWith('<link rel="modulepreload"') } == [
                    '<link rel="stylesheet" href="/myapp/static/dist/js/main-def456.css">',
                    '<link rel="stylesheet" href="/myapp/static/dist/js/shared-jkl012.css">',
                    '<script type="module" src="/myapp/static/dist/js/main-abc123.js"></script>',
                    '<link rel="modulepreload" href="/myapp/static/dist/js/shared-ghi789.js">'
            ]
    }

    def 'favicon #favicon is rendered as #expected'() {
        given: 'a configured favicon'
            config.inertia.favicon = favicon

        when: 'rendering the html view'
            def output = render(
                    view: '/inertia/html',
                    model: [inertiaManifest: [:]]
            )

        then: 'the favicon url is resolved against the context path only when root-relative'
            output.contains("<link rel=\"icon\" href=\"${expected}\">")

        where:
            favicon                               | expected
            '/static/favicon.svg'                 | '/myapp/static/favicon.svg'
            'static/favicon.svg'                  | 'static/favicon.svg'
            '//cdn.example.com/favicon.svg'       | '//cdn.example.com/favicon.svg'
            'https://cdn.example.com/favicon.svg' | 'https://cdn.example.com/favicon.svg'
    }
}

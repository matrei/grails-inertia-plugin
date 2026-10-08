package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.GrailsWebUnitTest

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

    def 'built assets are prefixed with the context path'() {
        when: 'rendering the html view with the built assets'
            final output = render(
                    view: '/inertia/html',
                    model: [
                            inertiaVite: new ViteConfig(false, ViteConfig.DEFAULT_DEV_SERVER_URL, ViteConfig.DEFAULT_ENTRY),
                            inertiaViteEntry: ViteEntry.from(MANIFEST, ViteConfig.DEFAULT_ENTRY)
                    ]
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
            final output = render(
                    view: '/inertia/html',
                    model: [inertiaVite: new ViteConfig(true, ViteConfig.DEFAULT_DEV_SERVER_URL, ViteConfig.DEFAULT_ENTRY)]
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

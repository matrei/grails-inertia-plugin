package grails.plugin.inertia

import spock.lang.Specification

import grails.testing.web.GrailsWebUnitTest

class ViteDevServerViewSpec extends Specification implements GrailsWebUnitTest {

    private static final Map MANIFEST = [
            'src/main/javascript/main.js': [
                    file: 'js/main-abc123.js',
                    imports: ['_shared-ghi789.js'],
                    css: ['js/main-def456.css']
            ],
            '_shared-ghi789.js': [file: 'js/shared-ghi789.js']
    ]

    void 'with the dev server enabled, the page loads the vite client and the entry from it'() {

        when: 'rendering the html view with the dev server'
            def output = render(
                    view: '/inertia/html',
                    model: [inertiaVite: new ViteConfig(true, ViteConfig.DEFAULT_DEV_SERVER_URL, ViteConfig.DEFAULT_ENTRY)]
            )

        then: 'the scripts come from the dev server'
            scriptsAndLinks(output) == [
                    '<script type="module" src="http://localhost:3000/@vite/client"></script>',
                    '<script type="module" src="http://localhost:3000/src/main/javascript/main.js"></script>'
            ]
    }

    void 'the configured dev server url #url and entry are used'(String url) {

        when: 'rendering the html view with a custom dev server and entry'
            def output = render(
                    view: '/inertia/html',
                    model: [inertiaVite: new ViteConfig(true, url, 'src/main/js/app.ts')]
            )

        then: 'the scripts use them'
            scriptsAndLinks(output) == [
                    '<script type="module" src="http://localhost:5173/@vite/client"></script>',
                    '<script type="module" src="http://localhost:5173/src/main/js/app.ts"></script>'
            ]

        where:
            url << ['http://localhost:5173', 'http://localhost:5173/']
    }

    void 'with the dev server disabled, the page loads the built assets'() {

        given: 'a context path'
            request.contextPath = '/myapp'

        when: 'rendering the html view with the built assets'
            def output = render(
                    view: '/inertia/html',
                    model: [
                            inertiaVite: new ViteConfig(false, ViteConfig.DEFAULT_DEV_SERVER_URL, ViteConfig.DEFAULT_ENTRY),
                            inertiaManifest: MANIFEST
                    ]
            )

        then: 'the stylesheets, the module script and the preloaded modules from the manifest, and nothing from the dev server'
            scriptsAndLinks(output) == [
                    '<link rel="stylesheet" href="/myapp/static/dist/js/main-def456.css">',
                    '<script type="module" src="/myapp/static/dist/js/main-abc123.js"></script>',
                    '<link rel="modulepreload" href="/myapp/static/dist/js/shared-ghi789.js">'
            ]
    }

    void 'the configured entry is looked up in the manifest'() {

        when: 'rendering the html view with a custom entry'
            def output = render(
                    view: '/inertia/html',
                    model: [
                            inertiaVite: new ViteConfig(false, ViteConfig.DEFAULT_DEV_SERVER_URL, 'src/main/js/app.ts'),
                            inertiaManifest: ['src/main/js/app.ts': [file: 'js/app-123.js']]
                    ]
            )

        then: 'the module script of that entry'
            scriptsAndLinks(output) == ['<script type="module" src="/static/dist/js/app-123.js"></script>']
    }

    void 'a missing entry explains how to provide the JavaScript'() {

        when: 'rendering the html view with built assets that do not contain the entry'
            render(
                    view: '/inertia/html',
                    model: [
                            inertiaVite: new ViteConfig(false, ViteConfig.DEFAULT_DEV_SERVER_URL, ViteConfig.DEFAULT_ENTRY),
                            inertiaManifest: [:]
                    ]
            )

        then: 'the failure says to build the assets or to use the dev server'
            def e = thrown(Exception)
            def message = causes(e)*.message.find { it?.contains('not found in the Vite manifest') }
            message.contains("Build the assets, for example with 'vite build'")
            message.contains('inertia.vite.devServer.enabled')
    }

    private static List<String> scriptsAndLinks(String output) {
        output.readLines()*.trim().findAll {
            it.startsWith('<script type="module"') || it.startsWith('<link rel="stylesheet"') || it.startsWith('<link rel="modulepreload"')
        }
    }

    private static List<Throwable> causes(Throwable throwable) {
        def causes = [] as List<Throwable>
        for (def cause = throwable; cause != null && !causes.contains(cause); cause = cause.cause) {
            causes << cause
        }
        causes
    }
}

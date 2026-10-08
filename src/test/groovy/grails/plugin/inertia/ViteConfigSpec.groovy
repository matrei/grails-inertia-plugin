package grails.plugin.inertia

import org.grails.config.PropertySourcesConfig
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import grails.util.Environment

class ViteConfigSpec extends Specification {

    @RestoreSystemProperties
    void 'the dev server is used by default only in the #environment environment'(String environment, boolean enabled) {

        given: 'the environment'
            System.setProperty(Environment.KEY, environment)

        expect: 'the dev server is only enabled in development'
            ViteConfig.from(new PropertySourcesConfig()).devServerEnabled == enabled

        where:
            environment   || enabled
            'development' || true
            'test'        || false
            'production'  || false
            'staging'     || false
    }

    @RestoreSystemProperties
    void 'an explicit setting overrides the default of the #environment environment'(String environment, boolean enabled) {

        given: 'the environment'
            System.setProperty(Environment.KEY, environment)

        expect: 'the configured value is used'
            ViteConfig.from(config(inertia: [vite: [devServer: [enabled: enabled]]])).devServerEnabled == enabled

        where:
            environment   || enabled
            'test'        || true
            'staging'     || true
            'development' || false
    }

    void 'the dev server url and entry have defaults'() {

        when: 'nothing is configured'
            final vite = ViteConfig.from(new PropertySourcesConfig())

        then: 'the defaults are used'
            vite.devServerUrl == 'http://localhost:3000'
            vite.entry == 'src/main/javascript/main.js'
    }

    void 'the configured dev server url #url is used without a trailing slash'(String url) {

        expect: 'the url without trailing slash'
            ViteConfig.from(config(inertia: [vite: [devServer: [url: url]]])).devServerUrl == 'http://localhost:5173'

        where:
            url << ['http://localhost:5173', 'http://localhost:5173/']
    }

    void 'the configured entry is used'() {

        expect: 'the configured entry'
            ViteConfig.from(config(inertia: [vite: [entry: 'src/main/js/app.ts']])).entry == 'src/main/js/app.ts'
    }

    private static PropertySourcesConfig config(Map<String, Object> values) {
        new PropertySourcesConfig(values)
    }
}

package grails.plugin.inertia

import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.i18n.FixedLocaleResolver
import spock.lang.Specification

import grails.testing.web.GrailsWebUnitTest

class HtmlLangSpec extends Specification implements GrailsWebUnitTest {

    void 'the lang attribute is the locale of the request as a language tag'() {

        given: 'a request for a locale with a region'
            request.addPreferredLocale(Locale.forLanguageTag('de-CH'))

        when: 'rendering the html view'
            final output = render(
                    view: '/inertia/html',
                    model: [inertiaVite: new ViteConfig(true, ViteConfig.DEFAULT_DEV_SERVER_URL, ViteConfig.DEFAULT_ENTRY)]
            )

        then: 'the language tag uses a hyphen'
            output.contains('<html lang="de-CH">')
    }

    void 'the lang attribute follows the locale resolver of the application'() {

        given: 'a request for one locale, and a locale resolver that decides on another'
            request.addPreferredLocale(Locale.GERMAN)
            request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new FixedLocaleResolver(Locale.forLanguageTag('sv')))

        when: 'rendering the html view'
            final output = render(
                    view: '/inertia/html',
                    model: [inertiaVite: new ViteConfig(true, ViteConfig.DEFAULT_DEV_SERVER_URL, ViteConfig.DEFAULT_ENTRY)]
            )

        then: 'the locale resolved by the application is used'
            output.contains('<html lang="sv">')
    }
}

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

    void 'optional page metadata is rendered only when present'() {

        given:
            def inertiaPage = new InertiaPage(
                    component: 'Users/Index',
                    props: [users: []],
                    url: '/users',
                    version: '1',
                    flash: [success: 'Saved'],
                    sharedProps: ['auth'],
                    clearHistory: true,
                    mergeProps: ['users'],
                    deferredProps: [default: ['permissions']],
                    encryptHistory: true,
                    preserveFragment: true,
                    matchPropsOn: ['posts.data.id'],
                    scrollProps: [posts: [pageName: 'page', nextPage: 2]],
                    onceProps: [profile: [prop: 'profile', expiresAt: null]]
            )

        when:
            def result = render(
                    view: '/inertia/json',
                    model: [inertiaPage: inertiaPage]
            )

        then:
            result.json.mergeProps == ['users']
            result.json.deferredProps.default == ['permissions']
            result.json.encryptHistory
            result.json.clearHistory
            result.json.preserveFragment
            result.json.flash.success == 'Saved'
            result.json.sharedProps == ['auth']
            result.json.matchPropsOn == ['posts.data.id']
            result.json.scrollProps.posts.nextPage == 2
            result.json.onceProps.profile.prop == 'profile'
    }

    void 'shared prop names are preserved independently from page props'() {
        given:
            def page = new InertiaPage(
                    component: 'Dashboard',
                    props: [auth: [name: 'controller'], notifications: []],
                    url: '/dashboard',
                    version: '1',
                    sharedProps: ['auth', 'notifications']
            )

        when:
            def result = render(
                    view: '/inertia/json',
                    model: [inertiaPage: page]
            )

        then:
            result.json.props.auth.name == 'controller'
            result.json.sharedProps == ['auth', 'notifications']
    }
}

package grails.plugin.inertia

import spock.lang.Specification

class InertiaPropResolverSpec extends Specification {

    def 'full visits resolve regular and always props and announce deferred props'() {
        given:
            def evaluated = false
            def model = [
                    title: 'Users',
                    users: { evaluated = true; ['Mattias'] },
                    optional: InertiaProp.optional { throw new AssertionError((Object) 'must not evaluate') },
                    errors: InertiaProp.always([:]),
                    permissions: InertiaProp.deferred { ['read'] },
                    teams: InertiaProp.deferred({ ['admin'] }, 'sidebar')
            ]

        when:
            def result = InertiaPropResolver.resolve(
                    'Users/Index',
                    model,
                    new InertiaRequestContext()
            )

        then:
            result.props.title == 'Users'
            result.props.users == ['Mattias']
            evaluated
            result.props.errors == [:]
            result.props.optional == null
            result.props.permissions == null
            result.deferredProps == [default: ['permissions'], sidebar: ['teams']]
    }

    def 'partial reload evaluates only selected props and always props'() {
        given:
            def excludedEvaluated = false
            def context = new InertiaRequestContext(
                    partialComponent: 'Users/Index',
                    partialData: ['users']
            )
            def model = [
                    users: { ['Mattias'] },
                    companies: { excludedEvaluated = true; ['Acme'] },
                    errors: InertiaProp.always([invalid: 'value']),
                    optional: InertiaProp.optional { ['optional'] }
            ]

        when:
            def result = InertiaPropResolver.resolve('Users/Index', model, context)

        then:
            result.props.users == ['Mattias']
            result.props.errors == [invalid: 'value']
            result.props.optional == null
            result.props.companies == null
            !excludedEvaluated
    }

    def 'partial except removes selected props'() {
        given:
            def context = new InertiaRequestContext(
                    partialComponent: 'Users/Index',
                    partialExcept: ['companies']
            )

        when:
            def result = InertiaPropResolver.resolve(
                    'Users/Index',
                    [users: ['Mattias'], companies: ['Acme']],
                    context
            )

        then:
            result.props == [users: ['Mattias']]
    }

    def 'partial headers for another component do not filter props'() {
        given:
            def context = new InertiaRequestContext(
                    partialComponent: 'Dashboard',
                    partialData: ['users']
            )

        when:
            def result = InertiaPropResolver.resolve(
                    'Users/Index',
                    [users: ['Mattias'], companies: ['Acme']],
                    context
            )

        then:
            result.props == [users: ['Mattias'], companies: ['Acme']]
    }

    def 'merge and once props produce protocol metadata'() {
        given:
            def context = new InertiaRequestContext(
                    partialComponent: 'Users/Index',
                    partialData: ['users', 'notifications', 'settings', 'profile']
            )

        when:
            def result = InertiaPropResolver.resolve(
                    'Users/Index',
                    [
                            users: InertiaProp.merge(['one']),
                            notifications: InertiaProp.prepend(['new']),
                            settings: InertiaProp.deepMerge([theme: 'dark']),
                            profile: InertiaProp.once([name: 'Mattias'], 'profile', 123L)
                    ],
                    context
            )

        then:
            result.props.users == ['one']
            result.props.notifications == ['new']
            result.props.settings == [theme: 'dark']
            result.props.profile == [name: 'Mattias']
            result.mergeProps == ['users']
            result.prependProps == ['notifications']
            result.deepMergeProps == ['settings']
            result.onceProps == [profile: [prop: 'profile', expiresAt: 123L]]
    }

    def 'once props already loaded by the client are skipped'() {
        given:
            def context = new InertiaRequestContext(
                    exceptOnceProps: ['profile']
            )

        when:
            def result = InertiaPropResolver.resolve(
                    'Users/Index',
                    [profile: InertiaProp.once { throw new AssertionError((Object) 'must not evaluate') }],
                    context
            )

        then:
            result.props == [:]
            result.onceProps == [profile: [prop: 'profile', expiresAt: null]]
    }
}

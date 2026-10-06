package grails.plugin.inertia.routes

import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import spock.lang.Specification

class RouteFilterSpec extends Specification {

    List<Route> routes = [
            route('organizations', 'index'),
            route('organizations', 'update'),
            route('users', 'update'),
            route('users', 'updateUser'),
            route('errorHandling', 'notFound')
    ]

    void 'without patterns every route is kept'() {

        expect: 'all routes'
            names(new RouteFilter([], []).filter(routes)) == names(routes)
    }

    void 'only keeps the routes matching a pattern'() {

        expect: 'the routes of the organizations controller and the users updateUser action'
            names(new RouteFilter(['organizations.*', 'users.updateUser'], []).filter(routes)) ==
                    ['organizations.index', 'organizations.update', 'users.updateUser']
    }

    void 'except leaves out the routes matching a pattern'() {

        expect: 'all routes but the error handling ones and the users update action'
            names(new RouteFilter([], ['errorHandling.*', 'users.update']).filter(routes)) ==
                    ['organizations.index', 'organizations.update', 'users.updateUser']
    }

    void 'except leaves out routes that only includes'() {

        expect: 'the users routes without the update action'
            names(new RouteFilter(['users.*'], ['users.update']).filter(routes)) == ['users.updateUser']
    }

    void 'a wildcard matches any characters, and a pattern without one matches exactly'() {

        expect: 'every action starting with update, and only the update action itself'
            names(new RouteFilter(['*.update*'], []).filter(routes)) == ['organizations.update', 'users.update', 'users.updateUser']
            names(new RouteFilter(['users.update'], []).filter(routes)) == ['users.update']
    }

    void 'the patterns are read from #description'(String description, Map<String, Object> properties) {

        given: 'the settings'
            final environment = new StandardEnvironment()
            environment.propertySources.addFirst(new MapPropertySource('test', properties))

        expect: 'the filter applies them'
            names(RoutesGenerator.routeFilter(environment).filter(routes)) == ['organizations.index', 'organizations.update']

        where:
            description                 | properties
            'lists'                     | ['inertia.routes.only[0]': 'organizations.*', 'inertia.routes.only[1]': 'users.*', 'inertia.routes.except[0]': 'users.*']
            'comma-separated strings'   | ['inertia.routes.only': 'organizations.*, users.*', 'inertia.routes.except': 'users.*']
    }

    private static Route route(String controller, String action) {
        new Route(controller, action, "/$controller/$action" as String, ['get'])
    }

    private static List<String> names(List<Route> routes) {
        routes.collect { "${it.controller}.${it.action}" as String }
    }
}

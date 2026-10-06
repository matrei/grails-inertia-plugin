package grails.plugin.inertia.routes

import spock.lang.Specification

import grails.testing.web.UrlMappingsUnitTest

class RouteCollectorSpec extends Specification implements UrlMappingsUnitTest<RoutesTestUrlMappings> {

    List<Route> routes

    void setup() {
        routes = new RouteCollector().collect(urlMappingsHolder, [
                organizations: ['index', 'store', 'edit', 'update', 'delete', 'export'],
                dashboard: ['index'],
                images: ['thumbnail'],
                reports: ['index', 'summary']
        ])
    }

    void 'a mapping with a single action gives a route with its template'() {

        expect: 'the variables of the url pattern named in the template'
            route('organizations', 'edit').template == '/organizations/{id}/edit'
            route('organizations', 'edit').methods == ['get']
    }

    void 'a mapping with an action for each method gives a route for each action'() {

        expect: 'each action with its methods, the first one being the default'
            route('organizations', 'index').methods == ['get']
            route('organizations', 'store').methods == ['post']
            route('organizations', 'update').template == '/organizations/{id}'
            route('organizations', 'update').methods == ['put', 'post']
            route('organizations', 'delete').methods == ['delete']
    }

    void 'a mapping restricted to a method uses it'() {

        expect: 'the method of the mapping'
            route('reports', 'summary').methods == ['post']
    }

    void 'a variable spanning segments is marked'() {

        expect: 'the variable with an asterisk'
            route('images', 'thumbnail').template == '/img/{path*}'
    }

    void 'the format extension is left out'() {

        expect: 'no format variable'
            route('reports', 'index').template == '/reports/{year}'
    }

    void 'actions without a mapping of their own get a route from the generic mapping'() {

        expect: 'the controller and action filled in, and the optional id kept'
            route('organizations', 'export').template == '/organizations/export/{id?}'
            route('organizations', 'export').methods == ['get']
    }

    void 'mappings without a controller action are left out'() {

        expect: 'no routes for views and response codes'
            routes.every { it.controller in ['organizations', 'dashboard', 'images', 'reports'] }
            routes.size() == 10
    }

    private Route route(String controller, String action) {
        routes.find { it.controller == controller && it.action == action }
    }
}

class RoutesTestUrlMappings {

    static mappings = {
        "/$controller/$action?/$id?" {}
        '/' (controller: 'dashboard', action: [GET: 'index'])
        '/organizations' (controller: 'organizations', action: [GET: 'index', POST: 'store'])
        "/organizations/$id/edit" (controller: 'organizations', action: [GET: 'edit'])
        "/organizations/$id" (controller: 'organizations', action: [PUT: 'update', POST: 'update', DELETE: 'delete'])
        "/img/$path**" (controller: 'images', action: 'thumbnail')
        "/reports/$year(.$format)?" (controller: 'reports', action: 'index')
        '/reports/summary' (controller: 'reports', action: 'summary', method: 'POST')
        '/error' (view: '/error')
        '404' (controller: 'dashboard', action: 'index')
    }
}

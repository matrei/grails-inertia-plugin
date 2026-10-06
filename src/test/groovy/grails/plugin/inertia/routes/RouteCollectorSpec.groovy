package grails.plugin.inertia.routes

import spock.lang.Specification

import grails.testing.web.UrlMappingsUnitTest

class RouteCollectorSpec extends Specification implements UrlMappingsUnitTest<RoutesTestUrlMappings> {

    List<Route> routes

    void setup() {
        routes = new RouteCollector().collect(urlMappingsHolder, [
                organizations: ['index', 'store', 'edit', 'update', 'delete', 'restore', 'archive', 'export'],
                dashboard: ['index'],
                images: ['thumbnail'],
                reports: ['index', 'summary']
        ], [
                organizations: [update: 'PATCH', restore: 'PUT', archive: 'POST', export: ['POST', 'GET']],
                reports: [summary: ['GET', 'POST']]
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

        expect: 'the method of the mapping, whatever the controller allows'
            route('reports', 'summary').methods == ['post']
    }

    void 'a mapping that accepts any method uses the allowed methods of the action'() {

        expect: 'the methods the controller allows the action, the first one being the default'
            route('organizations', 'restore').methods == ['put']
            route('organizations', 'export').methods == ['post', 'get']
    }

    void 'a mapping with an action for each method ignores the allowed methods'() {

        expect: 'the methods of the mapping'
            route('organizations', 'update').methods == ['put', 'post']
    }

    void 'an action without allowed methods gets the default method'() {

        expect: 'get'
            route('images', 'thumbnail').methods == ['get']
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
    }

    void 'mappings without a controller action are left out'() {

        expect: 'no controller routes for views and response codes'
            controllerRoutes.every { it.controller in ['organizations', 'dashboard', 'images', 'reports', 'book', 'contact'] }
            controllerRoutes.size() == 15
    }

    void 'a named mapping also gives a route under its name'() {

        expect: 'the named route with its controller, action and template'
            with(namedRoute('showBook')) {
                controller == 'book'
                action == 'show'
                template == '/books/{id}'
                methods == ['get']
            }
    }

    void 'a named mapping can be reached by name when another mapping maps the same action'() {

        expect: 'the controller route from the first mapping, and the named one from its own'
            route('book', 'show').template == '/api/books/{id}'
            namedRoute('showBook').template == '/books/{id}'
    }

    void 'a named mapping that accepts any method uses the allowed methods of its action'() {

        expect: 'the methods the controller allows the action'
            namedRoute('archiveOrganization').methods == ['post']
    }

    void 'a named mapping of a view gives a route without a controller'() {

        expect: 'the named route of the view'
            with(namedRoute('about')) {
                controller == null
                action == null
                template == '/about'
                methods == ['get']
            }
    }

    void 'a named mapping with an action for each method accepts each method'() {

        expect: 'the methods of the mapping, without a single action'
            with(namedRoute('contact')) {
                action == null
                methods == ['get', 'post']
            }
    }

    void 'the named routes follow the controller routes, ordered by name'() {

        expect: 'the names in order after the controller routes'
            routes.findAll { it.name }*.name == ['about', 'archiveOrganization', 'contact', 'showBook']
            routes.findIndexOf { it.name } == controllerRoutes.size()
    }

    private List<Route> getControllerRoutes() {
        routes.findAll { !it.name }
    }

    private Route namedRoute(String name) {
        routes.find { it.name == name }
    }

    private Route route(String controller, String action) {
        controllerRoutes.find { it.controller == controller && it.action == action }
    }
}

class RoutesTestUrlMappings {

    static mappings = {
        "/$controller/$action?/$id?" {}
        '/' (controller: 'dashboard', action: [GET: 'index'])
        '/organizations' (controller: 'organizations', action: [GET: 'index', POST: 'store'])
        "/organizations/$id/edit" (controller: 'organizations', action: [GET: 'edit'])
        "/organizations/$id" (controller: 'organizations', action: [PUT: 'update', POST: 'update', DELETE: 'delete'])
        "/organizations/$id/restore" (controller: 'organizations', action: 'restore')
        name archiveOrganization: "/organizations/$id/archive" (controller: 'organizations', action: 'archive')
        "/img/$path**" (controller: 'images', action: 'thumbnail')
        "/reports/$year(.$format)?" (controller: 'reports', action: 'index')
        '/reports/summary' (controller: 'reports', action: 'summary', method: 'POST')
        name showBook: "/books/$id" (controller: 'book', action: 'show')
        "/api/books/$id" (controller: 'book', action: 'show', method: 'GET')
        name about: '/about' (view: '/about')
        name contact: '/contact' (controller: 'contact', action: [GET: 'show', POST: 'send'])
        '/error' (view: '/error')
        '404' (controller: 'dashboard', action: 'index')
    }
}

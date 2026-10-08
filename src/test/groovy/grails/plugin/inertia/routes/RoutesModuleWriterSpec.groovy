package grails.plugin.inertia.routes

import spock.lang.Specification

class RoutesModuleWriterSpec extends Specification {

    void 'each controller is exported by its class name with a route for each action'() {

        when: 'writing two routes of a controller'
            final source = new RoutesModuleWriter().write([
                    new Route('organizations', 'update', '/organizations/{id}', ['put', 'post']),
                    new Route('organizations', 'delete', '/organizations/{id}', ['delete'])
            ], '/myapp')

        then: 'the controller object with the template and methods of each action, and the exported context path'
            source.contains('export const contextPath = "/myapp"')
            source.contains('''export const OrganizationsController = {
    "update": route("/organizations/{id}", ["put","post"]),
    "delete": route("/organizations/{id}", ["delete"]),
}''')
    }

    void 'a controller name of several words becomes its class name'() {

        expect: 'the class name of the errorTest controller'
            new RoutesModuleWriter().write([new Route('errorTest', 'show', '/test-500-error', ['get'])], '')
                    .contains('export const ErrorTestController = {')
    }

    void 'the routes of named mappings are exported by name'() {

        when: 'writing a controller route and a named route'
            final source = new RoutesModuleWriter().write([
                    new Route('book', 'show', '/api/books/{id}', ['get']),
                    new Route('showBook', 'book', 'show', '/books/{id}', ['get'])
            ], '')

        then: 'the named route only in the named export'
            source.contains('''export const BookController = {
    "show": route("/api/books/{id}", ["get"]),
}''')
            source.contains('''export const named = {
    "showBook": route("/books/{id}", ["get"]),
}''')
    }

    void 'without named routes there is no named export'() {

        expect: 'no named export'
            !new RoutesModuleWriter().write([new Route('book', 'show', '/books/{id}', ['get'])], '').contains('export const named')
    }

    void 'without a context path the urls start at the root'() {

        expect: 'an empty context path'
            new RoutesModuleWriter().write([], null).contains('export const contextPath = ""')
    }
}

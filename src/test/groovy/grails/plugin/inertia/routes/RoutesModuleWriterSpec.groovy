package grails.plugin.inertia.routes

import spock.lang.Specification

class RoutesModuleWriterSpec extends Specification {

    void 'each controller is exported with a route for each action'() {

        when: 'writing two routes of a controller'
            final source = new RoutesModuleWriter().write([
                    new Route('organizations', 'update', '/organizations/{id}', ['put', 'post']),
                    new Route('organizations', 'delete', '/organizations/{id}', ['delete'])
            ], '/myapp')

        then: 'the controller object with the template and methods of each action, and the context path'
            source.contains('const contextPath = "/myapp"')
            source.contains('''export const organizations = {
    "update": route("/organizations/{id}", ["put","post"]),
    "delete": route("/organizations/{id}", ["delete"]),
}''')
    }

    void 'without a context path the urls start at the root'() {

        expect: 'an empty context path'
            new RoutesModuleWriter().write([], null).contains('const contextPath = ""')
    }
}

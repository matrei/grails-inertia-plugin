package grails.plugin.inertia

import grails.plugins.Plugin
import groovy.transform.CompileStatic

@SuppressWarnings("unused")
@CompileStatic
class InertiaGrailsPlugin extends Plugin {

    def grailsVersion = "5.0.1 > *"
    def pluginExcludes = []
    def title = "Grails Adapter for Inertia.js"
    def author = "Mattias Reichel"
    def authorEmail = "mattias.reichel@gmail.com"
    def description = "Inertia server-side adapter for Grails"
    def profiles = ['web']

    def documentation = "https://github.com/matrei/inertia-grails-plugin#readme"
    def license = "APACHE"

    // Any additional developers beyond the author specified above.
    // def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    def issueManagement = [ system: "GitHub Issues", url: "https://github.com/matrei/inertia-grails-plugin/issues" ]
    def scm = [ url: "https://github.com/matrei/inertia-grails-plugin" ]

}

/*
 * Copyright 2022-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.inertia

import grails.plugins.Plugin
import groovy.transform.CompileStatic

/**
 * Grails plugin descriptor class.
 *
 * @author Mattias Reichel
 * @since 1.0.0
 */
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

package build.config

import groovy.transform.CompileStatic

import org.gradle.api.Project

@CompileStatic
trait GrailsProject {

    void configureProjectVersion(Project project) {
        def version = project.findProperty('projectVersion') as String
        if (!version) {
            throw new IllegalStateException('projectVersion property must be set for Grails plugins')
        }
        project.version = version
    }

    void configureGrailsVersion(Project project) {
        def grailsVersion = project.findProperty('grailsVersion') as String
        if (!grailsVersion) {
            throw new IllegalStateException('grailsVersion property must be set for Grails projects')
        }
        project.pluginManager.withPlugin('java') {
            project.dependencies.add(
                'implementation',
                project.dependencies.platform("org.apache.grails:grails-bom:$grailsVersion")
            )
        }
    }
}

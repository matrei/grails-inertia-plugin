package build.config

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.tasks.GenerateModuleMetadata

import org.apache.grails.gradle.publish.GrailsPublishExtension

@CompileStatic
class Publish implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.pluginManager.apply('org.apache.grails.gradle.grails-publish')
        project.extensions.configure(GrailsPublishExtension) {
            it.title.set('Grails Adapter for Inertia.js')
            it.desc.set('Grails plugin with server-side adapter for Inertia.js')
            it.license.name = 'Apache-2.0'
            it.githubSlug.set('matrei/grails-inertia-plugin')
            it.developers = [
                    matrei: 'Mattias Reichel',
            ]
        }

        // grails-publish uses the project while generating the POM and the module metadata,
        // so builds that publish run without the configuration cache
        project.tasks.withType(GenerateMavenPom).configureEach {
            it.notCompatibleWithConfigurationCache('grails-publish uses the project while generating the POM')
        }
        project.tasks.withType(GenerateModuleMetadata).configureEach {
            it.notCompatibleWithConfigurationCache('grails-publish uses the project while generating the module metadata')
        }
    }
}

package build.config

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Groovydoc

import org.grails.gradle.plugin.core.GrailsExtension
import org.grails.gradle.plugin.views.json.GsonViewCompilerTask

@CompileStatic
class GrailsPlugin implements Plugin<Project>, GrailsProject {

    private static final List<String> PLUGINS = [
        'org.apache.grails.gradle.grails-plugin',
        'org.apache.grails.gradle.grails-gson',
        'org.apache.grails.gradle.grails-gsp',
        'build.config.java',
        'build.config.publish',
        'build.config.reproducible',
        'build.config.test'
    ]

    @Override
    void apply(Project project) {

        configureProjectVersion(project)
        configureGrailsVersion(project)

        // This prevents the Grails Gradle Plugin from unnecessarily
        // excluding slf4j-simple in the generated POMs:
        // https://github.com/grails/grails-gradle-plugin/issues/222
        project.extensions.extraProperties.set(
                'slf4jPreventExclusion',
                'true'
        )

        PLUGINS.each {
            project.pluginManager.apply(it)
        }

        project.extensions.configure(GrailsExtension) {
            it.springDependencyManagement = false
        }

        project.tasks.named('compileGsonViews', GsonViewCompilerTask) {
            it.packageName.set('inertia') // This is needed for the gson view to be resolved from plugin
        }

        project.tasks.withType(Groovydoc).configureEach {
            it.dependsOn('compileGsonViews')
        }
    }
}

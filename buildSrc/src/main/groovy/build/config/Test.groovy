package build.config

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class Test implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.withPlugin('java') {
            configureTests(project)
            project.dependencies.add(
                    'testRuntimeOnly',
                    'org.junit.jupiter:junit-jupiter-api'
            )
        }
    }

    private static void configureTests(Project project) {
        project.tasks.withType(org.gradle.api.tasks.testing.Test).configureEach { testTask ->
            testTask.useJUnitPlatform()
            testTask.testLogging {
                events('passed', 'skipped', 'failed', 'standardOut', 'standardError')
            }
            testTask.reports.html.required.set(false)
            testTask.reports.junitXml.required.set(false)
        }
    }
}

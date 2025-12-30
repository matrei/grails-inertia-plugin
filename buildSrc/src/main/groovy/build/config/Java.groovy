package build.config

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

@CompileStatic
class Java implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.withPlugin('java') {
            def releaseVersion = resolveJavaVersion(project)
            project.tasks.withType(JavaCompile).configureEach {
                it.options.release.set(releaseVersion)
            }
            project.extensions.getByType(JavaPluginExtension).toolchain {
                it.languageVersion.set(JavaLanguageVersion.of(releaseVersion))
            }
        }
    }

    private static Integer resolveJavaVersion(Project project) {
        def raw = (project.findProperty('javaVersion') as String ?: '').trim()
        def releaseVersion = raw.isInteger() ? raw.toInteger() : null
        if (releaseVersion == null) {
            throw new IllegalStateException('javaVersion project property must be set to a valid integer value')
        }
        releaseVersion
    }
}

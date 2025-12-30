package build.config

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class Reproducible implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def buildInstant = Optional.ofNullable(System.getenv('SOURCE_DATE_EPOCH'))
                .filter(s -> !s.empty)
                .map(Long::parseLong)
                .map(Instant::ofEpochSecond)
                .orElseGet(Instant::now) as Instant
        project.extensions.extraProperties.set(
                'formattedBuildDate',
                DateTimeFormatter.ISO_INSTANT.format(buildInstant)
        )
        project.extensions.extraProperties.set(
                'buildDate',
                buildInstant.atZone(ZoneOffset.UTC)
        )
    }
}

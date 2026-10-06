/*
 * Copyright 2026-present original authors
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
package grails.plugin.inertia.routes

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.core.env.Environment

import grails.core.GrailsApplication
import grails.core.GrailsControllerClass
import grails.web.mapping.UrlMappingsHolder

/**
 * Writes the routes of the application as a JavaScript module when the application has started, so that the page
 * components can use them instead of hard-coded URLs.
 *
 * @since 4.1
 */
@Slf4j
@CompileStatic
class RoutesGenerator implements ApplicationListener<ApplicationReadyEvent> {

    static final String OUTPUT = 'inertia.routes.output'
    static final String ONLY = 'inertia.routes.only'
    static final String EXCEPT = 'inertia.routes.except'
    static final String DEFAULT_OUTPUT = 'src/main/javascript/routes.js'

    private static final String ALLOWED_METHODS = 'allowedMethods'

    private final UrlMappingsHolder urlMappingsHolder
    private final GrailsApplication grailsApplication
    private final Environment environment

    RoutesGenerator(UrlMappingsHolder urlMappingsHolder, GrailsApplication grailsApplication, Environment environment) {
        this.urlMappingsHolder = urlMappingsHolder
        this.grailsApplication = grailsApplication
        this.environment = environment
    }

    /**
     * Creates a generator for the application of the given context.
     */
    static RoutesGenerator forApplicationContext(ApplicationContext context) {
        new RoutesGenerator(
                context.getBean('grailsUrlMappingsHolder', UrlMappingsHolder),
                context.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication),
                context.environment
        )
    }

    @Override
    void onApplicationEvent(ApplicationReadyEvent event) {
        generate()
    }

    /**
     * Writes the module to {@code inertia.routes.output}, relative to the working directory.
     *
     * @return whether the module was written, which it is not when it is already up to date
     */
    boolean generate() {
        generate(Path.of(environment.getProperty(OUTPUT, DEFAULT_OUTPUT)))
    }

    /**
     * @param output the file to write the module to
     * @return whether the module was written, which it is not when it is already up to date
     */
    boolean generate(Path output) {
        final controllers = grailsApplication.getArtefacts('Controller').toList().collect { it as GrailsControllerClass }
        final controllerActions = controllers.collectEntries {
            [(it.logicalPropertyName): it.actions]
        } as Map<String, Collection<String>>
        final allowedMethods = controllers.collectEntries {
            [(it.logicalPropertyName): it.getPropertyValue(ALLOWED_METHODS, Map) ?: [:]]
        } as Map<String, Map>
        final routes = routeFilter(environment).filter(new RouteCollector().collect(urlMappingsHolder, controllerActions, allowedMethods))
        final source = new RoutesModuleWriter().write(routes, environment.getProperty('server.servlet.context-path', ''))
        if (Files.exists(output) && Files.readString(output, StandardCharsets.UTF_8) == source) {
            log.debug('Inertia routes in {} are up to date', output)
            return false
        }
        Files.createDirectories(output.toAbsolutePath().parent)
        Files.writeString(output, source, StandardCharsets.UTF_8)
        log.info('Wrote {} Inertia routes to {}', routes.size(), output)
        return true
    }

    /**
     * Creates the filter from {@code inertia.routes.only} and {@code inertia.routes.except}, which may each be a list
     * or a comma-separated string.
     */
    static RouteFilter routeFilter(Environment environment) {
        final binder = Binder.get(environment)
        new RouteFilter(
                binder.bind(ONLY, Bindable.listOf(String)).orElse([]),
                binder.bind(EXCEPT, Bindable.listOf(String)).orElse([])
        )
    }
}

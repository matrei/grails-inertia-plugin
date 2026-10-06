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
package grails.plugin.inertia.cli

import groovy.transform.CompileStatic

import org.apache.grails.core.cli.ApplicationCommand
import org.apache.grails.core.cli.ExecutionContext

import grails.plugin.inertia.routes.RoutesGenerator

/**
 * Writes the routes of the controller actions as a JavaScript module, as {@link RoutesGenerator} does when the
 * application starts, for builds that need the module without running the application.
 *
 * <p>Run it with {@code ./gradlew inertiaRoutes}. The module is written to {@code inertia.routes.output}, relative to
 * the project directory, or to the file given with {@code --output}.</p>
 *
 * @since 4.1
 */
@CompileStatic
class InertiaRoutesCommand implements ApplicationCommand {

    final String description = 'Writes the routes of the controller actions as a JavaScript module'

    @Override
    boolean handle(ExecutionContext executionContext) {
        final output = executionContext.commandLine?.optionValue('output') ?:
                applicationContext.environment.getProperty(RoutesGenerator.OUTPUT, RoutesGenerator.DEFAULT_OUTPUT)
        RoutesGenerator.forApplicationContext(applicationContext)
                .generate(executionContext.baseDir.toPath().resolve(output as String))
        true
    }
}

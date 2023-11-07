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

import grails.artefact.Enhances
import groovy.transform.CompileStatic
import org.springframework.web.servlet.ModelAndView

/**
 * A trait that is used to add Inertia.js support to controllers and interceptors.
 *
 * @author Mattias Reichel
 * @since 1.0.0
 */
@SuppressWarnings('unused')
@CompileStatic
@Enhances(['Controller','Interceptor'])
trait InertiaTrait {

    ModelAndView renderInertia(String component, Map props = [:], Map viewData = [:]) {
        Inertia.render component, props, viewData
    }

    /**
     * External redirects
     *
     * Sometimes it's necessary to redirect to an external website, or even
     * another non-Inertia endpoint in your app, within an Inertia request.
     * This is possible using a server-side initiated window.location visit.
     *
     * This will generate a 409 Conflict response, which includes the destination
     * URL in the X-Inertia-Location header. Client-side, Inertia will detect this
     * response and automatically do a "window.location = url" visit.
     *
     * @param url The url to redirect to
     */
    void locationInertia(String url) {
        Inertia.location url
    }

    Map getInertiaSharedData() {
        Inertia.sharedData
    }

    void setInertiaSharedData(Map sharedData) {
        Inertia.sharedData = sharedData
    }

    void cancelInertia() {
        Inertia.cancel()
    }

    boolean isInertiaCanceled() {
        Inertia.isCanceled
    }
}
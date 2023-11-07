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

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import grails.util.Environment
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus

import static Inertia.INERTIA_ATTRIBUTE_MANIFEST
import static Inertia.INERTIA_ATTRIBUTE_VERSION
import static Inertia.INERTIA_HEADER
import static Inertia.INERTIA_HEADER_LOCATION
import static Inertia.INERTIA_HEADER_VERSION
import static Inertia.INERTIA_VIEW_HTML
import static grails.web.http.HttpHeaders.VARY

/**
 * Interceptor that implements Inertia functionality.
 *
 * @author Mattias Reichel
 * @since 1.0.0
 */
@CompileStatic
class InertiaInterceptor implements GrailsConfigurationAware {

    private String manifestLocation
    private String manifestHash = 'not yet calculated'
    private volatile Object manifestObject

    private boolean ssrEnabled

    private static final String CONTENT_TYPE_JSON = 'application/json;charset=utf-8'
    private static final String CONTENT_TYPE_HTML = 'text/html;charset=utf-8'

    InertiaInterceptor() {
        match controller: '*'
    }

    boolean before() {

        // Set the assets version so the client can check if it has an old version loaded
        request.setAttribute INERTIA_ATTRIBUTE_VERSION, manifestHash

        true // Continue to process the request
    }

    boolean after() {

        if (Inertia.isCanceled) return true

        setContentType()
        setHeaders()

        if (isInertiaRequest) {
            // Check for asset version changes on GET requests
            if (isGetRequest && manifestShouldBeUsed && isAssetsOutOfDate) {
                log.debug 'Inertia asset version has changed, notifying Inertia client and aborting request processing to force full page reload!'
                header INERTIA_HEADER_LOCATION, webRequest.currentRequest.forwardURI
                render status: HttpStatus.CONFLICT.code
                return false // Stop processing the request here and return the response
            }

            // Changes the status code during redirects, ensuring they are made as
            // GET requests, preventing "MethodNotAllowedHttpException" errors.
            if (methodNotAllowedShouldBePrevented) response.status = HttpStatus.SEE_OTHER.code
        }

        // Add the Javascript Manifest when not in Development Environment
        // In Development Environment a node server should be started to serve the javascript files (npm run serve)
        if (isInertiaHtmlView && manifestShouldBeUsed) model.put INERTIA_ATTRIBUTE_MANIFEST, manifest

        true // Continue to process the request
    }


    private void setContentType() { response.contentType = isInertiaRequest ? CONTENT_TYPE_JSON : CONTENT_TYPE_HTML }
    private void setHeaders() {
        if (isInertiaRequest) header INERTIA_HEADER, 'true'
        response.addHeader VARY, INERTIA_HEADER
    }

    private Object loadManifest() {
        // TODO: should the manifest file be checked for modification and reloaded (live reloading of assets in production)?
        if (manifestObject == null) {
            synchronized(this) {
                if (manifestObject == null) {
                    def manifestResource= Holders.grailsApplication.mainContext.getResource manifestLocation
                    manifestObject = new JsonSlurper().parse manifestResource.inputStream
                    manifestHash = Objects.hashCode(manifestObject) as String
                }
            }
        }
        manifestObject
    }
    Object getManifest() { loadManifest() }

    boolean getManifestShouldBeUsed() { Environment.current != Environment.DEVELOPMENT }
    boolean getIsGetRequest() { 'GET' == request.method }
    boolean getMethodNotAllowedShouldBePrevented() { isInertiaRequest && response.status == HttpStatus.FOUND.code && request.method in ['PUT', 'PATCH', 'DELETE'] }
    boolean getIsInertiaHtmlView() { modelAndView?.viewName == INERTIA_VIEW_HTML }
    boolean getIsInertiaRequest() { request.getHeader(INERTIA_HEADER) == 'true' }
    boolean getIsAssetsCurrent() {
        def currentVersion = request.getAttribute(INERTIA_ATTRIBUTE_VERSION) as String
        def requestedVersion = request.getHeader(INERTIA_HEADER_VERSION) as String
        requestedVersion == currentVersion
    }
    boolean getIsAssetsOutOfDate() { !isAssetsCurrent }

    @Override
    void setConfiguration(Config co) {
        // Load the Javascript Manifest when in Production and Test Environments
        // In Development Environment a node server should be started to serve the javascript files (npm run serve)
        if (manifestShouldBeUsed) {
            manifestLocation = co.getRequiredProperty'inertia.manifest.location'
            loadManifest()
        }
        ssrEnabled = co.getProperty'inertia.ssr.enabled', Boolean, false
    }
}

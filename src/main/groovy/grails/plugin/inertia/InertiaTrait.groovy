package grails.plugin.inertia

import grails.artefact.Enhances
import org.springframework.web.servlet.ModelAndView

@SuppressWarnings('unused')
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
     * response and automatically dow a window.location = url visit.
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
}
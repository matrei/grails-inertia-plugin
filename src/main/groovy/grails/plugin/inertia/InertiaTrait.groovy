package grails.plugin.inertia

import grails.artefact.Enhances
import org.springframework.web.servlet.ModelAndView

@Enhances(['Controller','Interceptor'])
trait InertiaTrait {

    ModelAndView renderInertia(String component, Map props = [:], Map viewData = [:]) {
        Inertia.render component, props, viewData
    }

    Map getInertiaSharedData() {
        Inertia.sharedData
    }

    void setInertiaSharedData(Map sharedData) {
        Inertia.sharedData = sharedData
    }
}
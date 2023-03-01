package grails.plugin.inertia

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@Immutable
@CompileStatic
class InertiaPage {

    String component
    Map props
    String url
    String version

}

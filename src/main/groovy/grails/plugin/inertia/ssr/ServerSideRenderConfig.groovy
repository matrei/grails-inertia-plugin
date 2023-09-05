package grails.plugin.inertia.ssr

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties

@CompileStatic
@ConfigurationProperties(PREFIX)
class ServerSideRenderConfig {

    static final String PREFIX = 'inertia.ssr'

    boolean enabled
    String url
    String bundle
}
package grails.plugin.inertia

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import spock.lang.AutoCleanup
import spock.lang.Specification

import grails.plugin.inertia.ssr.ServerSideRenderProcess
import grails.plugin.inertia.ssr.ServerSideRenderer

class InertiaAutoConfigurationSpec extends Specification {

    @AutoCleanup
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()

    void 'without SSR there is no SSR process or renderer'() {

        when: 'the auto-configuration is applied without settings'
            start([:])

        then: 'neither bean'
            context.getBeansOfType(ServerSideRenderProcess).isEmpty()
            context.getBeansOfType(ServerSideRenderer).isEmpty()
    }

    void 'with SSR enabled the SSR process is managed by the context'() {

        when: 'the auto-configuration is applied with SSR enabled'
            start('inertia.ssr.enabled': 'true', 'inertia.ssr.bundle': 'there/is/no/ssr.mjs')

        then: 'the SSR process and renderer beans'
            context.getBean(ServerSideRenderProcess)
            context.getBean(ServerSideRenderer)
    }

    private void start(Map<String, Object> properties) {
        context.environment.propertySources.addFirst(new MapPropertySource('test', properties))
        context.register(InertiaAutoConfiguration)
        context.refresh()
    }
}

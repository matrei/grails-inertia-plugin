package grails.plugin.inertia

import groovy.transform.CompileStatic

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import grails.plugin.inertia.ssr.ServerSideRenderConfig
import grails.plugin.inertia.ssr.ServerSideRenderer

@CompileStatic
@AutoConfiguration
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServerSideRenderConfig)
class InertiaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = 'inertia.ssr.enabled', havingValue = 'true')
    ServerSideRenderer serverSideRenderer(ServerSideRenderConfig ssrConfig) {
        new ServerSideRenderer(ssrConfig)
    }
}

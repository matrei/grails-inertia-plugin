package grails.plugin.inertia

import groovy.json.JsonSlurper
import org.springframework.util.ClassUtils
import spock.lang.Shared
import spock.lang.Specification

import grails.plugin.inertia.ssr.ServerSideRenderConfig

class ConfigurationMetadataSpec extends Specification {

    @Shared
    Map metadata = loadMetadata()

    @Shared
    Map<String, Map> documentedProperties = (metadata.get('properties') as List<Map>).collectEntries { [(it.name): it] }

    void 'every property of the server-side rendering configuration is documented with its type and default'() {

        given: 'the defaults of the configuration class'
            def defaults = new ServerSideRenderConfig()
            def configProperties = ServerSideRenderConfig.metaClass.properties.findAll { it.name != 'class' }

        expect: 'each property is documented under its kebab-case name'
            configProperties.each { property ->
                def documented = documentedProperties["inertia.ssr.${toKebabCase(property.name)}"]
                assert documented
                assert documented.sourceType == ServerSideRenderConfig.name
                assert documented.type == ClassUtils.resolvePrimitiveIfNecessary(property.type).name
                assert documented.defaultValue == defaults[property.name]
            }

        and: 'nothing else is documented for it'
            documentedProperties.values().findAll { it.sourceType == ServerSideRenderConfig.name }.size() == configProperties.size()
    }

    void 'the server-side rendering group refers to its configuration class'() {

        expect: 'the group has the type and source type of the configuration class'
            def group = (metadata.get('groups') as List<Map>).find { it.name == 'inertia.ssr' }
            group.type == ServerSideRenderConfig.name
            group.sourceType == ServerSideRenderConfig.name
    }

    void 'the Vite properties are documented with the defaults of ViteConfig'() {

        expect: 'the dev server url and entry defaults'
            documentedProperties[ViteConfig.DEV_SERVER_URL].defaultValue == ViteConfig.DEFAULT_DEV_SERVER_URL
            documentedProperties[ViteConfig.ENTRY].defaultValue == ViteConfig.DEFAULT_ENTRY

        and: 'no fixed default for the dev server switch, since it depends on the environment'
            documentedProperties[ViteConfig.DEV_SERVER_ENABLED].type == 'java.lang.Boolean'
            !documentedProperties[ViteConfig.DEV_SERVER_ENABLED].containsKey('defaultValue')
    }

    void 'every property and group is described'() {

        expect: 'a description for each'
            (metadata.get('properties') as List<Map>).every { it.description && it.type }
            (metadata.get('groups') as List<Map>).every { it.description }
    }

    private static String toKebabCase(String name) {
        name.replaceAll(/([A-Z])/, '-$1').toLowerCase()
    }

    // Grails modules publish metadata under the same resource name, so find the one with the Inertia group.
    private static Map loadMetadata() {
        def resources = ConfigurationMetadataSpec.classLoader
                .getResources('META-INF/spring-configuration-metadata.json')
                .toList()
                .collect { new JsonSlurper().parse(it) as Map }
                .findAll { (it.groups as List<Map>)?.any { it.name == 'inertia' } }
        assert resources.size() == 1
        resources.first()
    }
}

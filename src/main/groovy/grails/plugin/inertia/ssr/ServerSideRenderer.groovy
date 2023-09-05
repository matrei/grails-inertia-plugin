package grails.plugin.inertia.ssr

import grails.plugin.inertia.InertiaPage
import groovy.json.JsonOutput
import jakarta.inject.Inject
import org.springframework.stereotype.Service

@Service
class ServerSideRenderer {

    private final ServerSideRenderConfig ssr

    @Inject
    ServerSideRenderer(ServerSideRenderConfig config) {
        this.ssr = config
    }

    String render(InertiaPage page) {

        if (!ssr.enabled) return null

        // Pass the page to the Inertia SSR Node Server
        String ssrResult = ((HttpURLConnection)new URL(ssr.url).openConnection()).with {
            requestMethod = 'POST'
            doOutput = true
            outputStream.withWriter { writer ->
                writer << JsonOutput.toJson(page)
            }
            inputStream.text
        }

        ssrResult
    }
}

package grails.plugin.inertia.ssr

import grails.config.Config

class BundleDetector {

    static String detect(Config config) {

        String bundle = [
            config.getProperty("${ServerSideRenderConfig.PREFIX}.bundle", String),
            './src/main/resources/ssr/ssr.mjs',
            './src/main/resources/ssr/ssr.js'
        ].find {
            it && new File(it).exists()
        }

        bundle
    }
}

package grails.plugin.inertia.controllers

import grails.artefact.Artefact

@Artefact('Controller')
class TestController {

    def index() {
        renderInertia 'index', [hello: 'world']
    }

    def testing() {
        renderInertia 'testing'
    }
}

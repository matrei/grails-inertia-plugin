package grails.plugin.inertia.controllers

import grails.artefact.Artefact

@Artefact('Controller')
class TestController {

    def index() {
        render 'index'
    }

    def testing() {
        render 'testing'
    }
}

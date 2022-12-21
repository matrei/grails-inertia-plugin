package grails.plugin.inertia

//import grails.web.mapping.UrlMapping

class InertiaTagLib {


//    static defaultEncodeAs = [taglib:'text']
    static namespace = 'inertia'

    def app = { attrs, body ->
        String tagName = attrs.tagName ?: 'div'
        String id = attrs.id ?: 'app'
        out << "<$tagName id=\"$id\" data-page=\"${body()}\"></$tagName>"
    }

/*
    def routes = { attrs, body ->
        //grailsApplication.controllerClasses
        out << '''
        <script type="text/javascript">
            window.route = function() var routes = [] {}
       </script>
        '''
    }
*/
}

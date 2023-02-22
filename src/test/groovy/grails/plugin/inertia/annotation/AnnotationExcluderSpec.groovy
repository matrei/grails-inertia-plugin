package grails.plugin.inertia.annotation

import grails.artefact.Interceptor
import grails.testing.web.interceptor.InterceptorUnitTest
import grails.web.Controller
import spock.lang.Specification

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

class AnnotationExcluderSpec extends Specification implements InterceptorUnitTest<AllMatchingInterceptor> {

    def 'it excludes controller classes with annotation'() {
        given:
        grailsApplication.addArtefact('Controller', SkipClassController)

        when:
        withRequest(controller: 'skipClass', action: 'action')

        then:
        interceptor.doesMatch()

        when:
        AnnotationExcluder.excludeAnnotations(interceptor, grailsApplication, SkipAnnotation)

        then:
        !interceptor.doesMatch()
    }

    def 'it excludes controller method actions with annotation'() {
        given:
        grailsApplication.addArtefact('Controller', SkipMethodActionController)

        when:
        withRequest(controller: 'skipMethodAction', action: 'action')

        then:
        interceptor.doesMatch()

        when:
        AnnotationExcluder.excludeAnnotations(interceptor, grailsApplication, SkipAnnotation)

        then:
        !interceptor.doesMatch()
    }

    def 'it excludes controller closure actions with annotation'() {
        given:
        grailsApplication.addArtefact('Controller', SkipClosureActionController)

        when:
        withRequest(controller: 'skipClosureAction', action: 'action')

        then:
        interceptor.doesMatch()

        when:
        AnnotationExcluder.excludeAnnotations(interceptor, grailsApplication, SkipAnnotation)

        then:
        !interceptor.doesMatch()
    }
}

@Controller
@SkipAnnotation
class SkipClassController {

    @SuppressWarnings('unused')
    def action() {}
}

@Controller
class SkipMethodActionController {

    @SkipAnnotation
    @SuppressWarnings('unused')
    def action() {}
}

@Controller
class SkipClosureActionController {
    @SkipAnnotation
    @SuppressWarnings('unused')
    def action = {}
}

class AllMatchingInterceptor implements Interceptor {
    AllMatchingInterceptor() { matchAll() }
}

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD, ElementType.TYPE, ElementType.FIELD])
@interface SkipAnnotation {
    String value() default "";
}

package grails.plugin.inertia

import spock.lang.Specification

import jakarta.servlet.http.HttpServletRequest

class InertiaRequestContextSpec extends Specification {

    def 'v3 request headers are normalized into request context'() {
        given:
            final request = Mock(HttpServletRequest)
            request.getHeader(Inertia.INERTIA_HEADER_PARTIAL_COMPONENT) >> 'Users/Index'
            request.getHeader(Inertia.INERTIA_HEADER_PARTIAL_DATA) >> ' users, profile.name '
            request.getHeader(Inertia.INERTIA_HEADER_PARTIAL_EXCEPT) >> ' companies, '
            request.getHeader(Inertia.INERTIA_HEADER_RESET) >> 'users'
            request.getHeader(Inertia.INERTIA_HEADER_ERROR_BAG) >> 'create'
            request.getHeader(Inertia.INERTIA_HEADER_INFINITE_SCROLL_MERGE_INTENT) >> 'append'
            request.getHeader(Inertia.INERTIA_HEADER_EXCEPT_ONCE_PROPS) >> 'settings, permissions'
            request.getHeader(Inertia.INERTIA_HEADER_PURPOSE) >> 'PREFETCH'

        when:
            final context = InertiaRequestContext.from(request)

        then:
            context.partialComponent == 'Users/Index'
            context.partialData == ['users', 'profile.name']
            context.partialExcept == ['companies']
            context.reset == ['users']
            context.errorBag == 'create'
            context.infiniteScrollMergeIntent == 'append'
            context.exceptOnceProps == ['settings', 'permissions']
            context.prefetch
            context.partialReload
    }

    def 'missing request headers produce an initial visit context'() {
        given:
            final request = Mock(HttpServletRequest)

        when:
            final context = InertiaRequestContext.from(request)

        then:
            !context.partialReload
            !context.prefetch
            context.partialData.empty
            context.partialExcept.empty
            context.reset.empty
            context.exceptOnceProps.empty
            context.errorBag == null
    }
}

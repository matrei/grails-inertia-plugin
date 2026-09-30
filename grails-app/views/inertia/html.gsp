<%@ page compileStatic="true" grailsLayoutPreprocess="false" model="Map<String, Map<String, Object>> inertiaManifest; grails.plugin.inertia.ViteConfig inertiaVite; grails.core.GrailsApplication grailsApplication" %>
<%@ page import="org.springframework.web.servlet.support.RequestContextUtils; grails.plugin.inertia.ViteEntry" %>
<!DOCTYPE html>
<html lang="${RequestContextUtils.getLocale(request).toLanguageTag()}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <g:set var="favicon" value="${grailsApplication.config.getProperty('inertia.favicon', String)}"/>
    <g:if test="${favicon}">
        <link rel="icon" href="${favicon.startsWith('/') && !favicon.startsWith('//') ? request.contextPath + favicon : favicon}">
    </g:if>
    <g:if test="${inertiaVite.devServerEnabled}">
        <script type="module" src="${inertiaVite.devServerUrl}/@vite/client"></script>
        <script type="module" src="${inertiaVite.devServerUrl}/${inertiaVite.entry}"></script>
    </g:if>
    <g:else>
        <g:set var="viteEntry" value="${ViteEntry.from(inertiaManifest, inertiaVite.entry)}"/>
        <g:each in="${viteEntry.css}" var="inertiaCss">
            <link rel="stylesheet" href="${request.contextPath}/static/dist/${inertiaCss}">
        </g:each>
        <script type="module" src="${request.contextPath}/static/dist/${viteEntry.file}"></script>
        <g:each in="${viteEntry.preloads}" var="inertiaPreload">
            <link rel="modulepreload" href="${request.contextPath}/static/dist/${inertiaPreload}">
        </g:each>
    </g:else>
    <inertia:head/>
</head>
<body>
<inertia:app/>
</body>
</html>

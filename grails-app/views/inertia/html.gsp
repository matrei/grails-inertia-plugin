<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0"/>
    <g:set var="contextPath" value="${request.contextPath}"/>
    <g:set var="favicon" value="${grailsApplication.config.getProperty('inertia.favicon', String)}"/>
    <g:if test="${favicon}">
        <link rel="icon" href="${favicon.startsWith('/') && !favicon.startsWith('//') ? contextPath + favicon : favicon}"/>
    </g:if>
    <g:if env="production">
        <script type="module" src="${contextPath}/static/dist/${inertiaManifest['src/main/javascript/main.js']['file']}"></script>
        <g:each in="${inertiaManifest['src/main/javascript/main.js']['css']}" var="inertiaCss">
            <link rel="stylesheet" href="${contextPath}/static/dist/${inertiaCss}">
        </g:each>
    </g:if>
    <g:else>
        <script type="module" src="http://localhost:3000/@vite/client"></script>
        <script type="module" src="http://localhost:3000/src/main/javascript/main.js"></script>
    </g:else>
    <inertia:head/>
</head>
<body>
<inertia:app/>
</body>
</html>

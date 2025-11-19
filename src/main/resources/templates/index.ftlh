<!DOCTYPE html>
<html>
    <head>
        <link rel="icon" href="data:;base64,iVBORw0KGgo=">
        <@Style />
    </head>
    <body>

    <h4>Page loading</h4>
    <ul>
        <li><a href="/load/traditional">/load/traditional</a></li>
        <li><a href="/load/streaming">/load/streaming</a></li>
        <li><a href="/load/head-first">/load/head-first</a></li>
    </ul>

    <h4>Execution types</h4>
    <ul>
        <li><a href="/blocking-futures/basic">/blocking-futures/basic</a></li>
        <li><a href="/blocking-futures/dependencies">/blocking-futures/dependencies</a></li>
        <li><a href="/futures/basic">/futures/basic</a></li>
        <li><a href="/futures/dependencies">/futures/dependencies</a></li>
    </ul>

    <h4>Extras</h4>
    <ul>
        <li><a href="/atoms">/atoms</a></li>
        <li><a href="/error-boundaries">/error-boundaries</a></li>
        <li><a href="/suspend">/suspend</a></li>
        <li><a href="/defer">/defer</a></li>
    </ul>

    </body>
</html>

<#macro Style>
    <style>
        body {
            margin: 3em;
        }
        li {
            padding-bottom: 0.25em;
        }
        ul:nth-of-type(2) li:nth-child(2) {
           padding-bottom: 0.4em;
        }
    </style>
</#macro>
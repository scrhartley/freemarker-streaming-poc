<!DOCTYPE html>
<html>
	<head>
	    <style> li + li { padding-top: 0.5rem; } </style>
	</head>
	<body>

	    <ul>
            <li> <div>My page using suspend macro (Declarative Shadow DOM)!</div> </li>

            <li>
                <@suspend> <div>${myData1}</div> </@suspend>
            </li>
            <li>
                <@suspend fallback=myLoadingMacro> <div>${myData2}</div> </@suspend>
            </li>
            <li>
                <@suspend fallback="Loading (with HTML string fallback) ..."> <div>${myData3}</div> </@suspend>
            </li>
        </ul>

        <div>Page finished!</div>

	</body>
</html>


<#macro myLoadingMacro>
    Loading (with macro fallback) ...
</#macro>

<#macro sharedSuspendFallback>
    Loading (with default fallback) ...
</#macro>


<#-- Inspiration https://react.dev/reference/react/Suspense -->
<#macro suspend fallback={}>
    <div style="display:contents"><#t>
        <template shadowrootmode="open"><slot><#t>
            <#if fallback?is_macro>
                <@fallback /><#t>
            <#elseif fallback?is_string>
                ${fallback?no_esc}<#t>
            <#elseif fallback?is_markup_output>
                ${fallback}<#t>
            <#else>
                <@sharedSuspendFallback /><#t>
            </#if><#t>
        </slot></template><#t>
    <@atom><#nested></@atom>
</#macro>
<!DOCTYPE html>
<html>
	<head>
	    <style> li + li { padding-top: 0.5rem; } </style>
	</head>
	<body>

	    <ul>
            <li> <div>My page using suspend macro with callables!</div> </li>

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
    <#local counter = (.globals.__suspendIdCounter__!0) + 1>
    <#global __suspendIdCounter__ = counter>
    <#local id = "FMS_fb:" + counter?c>

    <#if fallback?is_macro>
        <template id="${id}"><@fallback /></template>
    <#elseif fallback?is_string>
        <template id="${id}">${fallback?no_esc}</template>
    <#elseif fallback?is_markup_output>
        <template id="${id}">${fallback}</template>
    <#else>
        <template id="${id}"><@sharedSuspendFallback /></template>
    </#if>
    <#-- Show fallback using JS so won't show if no JS. -->
    <script>(t => t.after(t.content))(document.getElementById("${id}"));</script>

    <@atom>
        <#-- Remove fallback -->
        <script>(() => {
            const self = document.currentScript;
            let el = document.getElementById("${id}");
            do {
                let next = el.nextSibling;
                el.parentNode.removeChild(el);
                el = next;
            } while (el && el !== self);
            self.remove();
        })();</script>

        <#nested>
    </@atom>

</#macro>

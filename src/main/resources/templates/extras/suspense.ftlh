<!DOCTYPE html>
<html>
	<head>
	    <style> li + li { padding-top: 0.5rem; } </style>
	</head>
	<body>

	    <ul>
            <li> <div>My page using suspense macro with callables!</div> </li>

            <li>
                <@suspense> <div>${myData1}</div> </@suspense>
            </li>
            <li>
                <@suspense fallback=myLoadingMacro> <div>${myData2}</div> </@suspense>
            </li>
            <li>
                <@suspense fallback="Loading (with HTML string fallback) ..."> <div>${myData3}</div> </@suspense>
            </li>
        </ul>

        <div>Page finished!</div>

	</body>
</html>



<#macro myLoadingMacro>
    Loading (with macro fallback) ...
</#macro>

<#macro sharedSuspenseFallback>
    Loading (with default fallback) ...
</#macro>



<#-- Inspiration https://react.dev/reference/react/Suspense -->
<#macro suspense fallback={}>
    <#local counter = .globals.__suspenseIdCounter__!0>
    <#global __suspenseIdCounter__ = counter + 1>
    <#local id = "FMS_fallback:" + .globals.__suspenseIdCounter__?c>

    <#if fallback?is_macro>
        <template id="${id}"><@fallback /></template>
    <#elseif fallback?is_string>
        <template id="${id}">${fallback?no_esc}</template>
    <#elseif fallback?is_markup_output>
        <template id="${id}">${fallback}</template>
    <#else>
        <template id="${id}"><@sharedSuspenseFallback /></template>
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

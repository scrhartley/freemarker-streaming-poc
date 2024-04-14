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
<#if .data_model._$_MODERN_BROWSER___SUSPEND_MACRO_$_>
    <#switch true>
        <#case fallback?is_macro        ><template id="${id}"><@fallback              /></template><#break>
        <#case fallback?is_string       ><template id="${id}">${fallback?no_esc        }</template><#break>
        <#case fallback?is_markup_output><template id="${id}">${fallback               }</template><#break>
        <#default                       ><template id="${id}"><@sharedSuspendFallback /></template><#break>
    </#switch>
    <#-- Show fallback using JS so won't show if no JS. -->
    <script>(t => t.after(t.content))(document.getElementById("${id}"))</script>
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
<#else><#-- Also supporting IE11 -->
    <#switch true>
        <#case fallback?is_macro        ><template id="${id}" hidden><@fallback              /></template><#break>
        <#case fallback?is_string       ><template id="${id}" hidden>${fallback?no_esc        }</template><#break>
        <#case fallback?is_markup_output><template id="${id}" hidden>${fallback               }</template><#break>
        <#default                       ><template id="${id}" hidden><@sharedSuspendFallback /></template><#break>
    </#switch>
    <#-- Show fallback using JS so won't show if no JS. -->
    <script>(function(t) {
        const nodes = t.content ? [t.content] : Array.prototype.slice.call(t.childNodes);
        const parent = t.parentNode, next = t.nextSibling;
        nodes.forEach(function(node) {
            next ? parent.insertBefore(node, next) : parent.appendChild(node);
        });
    })(document.getElementById("${id}"))</script>
    <@atom>
        <#-- Remove fallback -->
        <script>(function() {
            const self = document.currentScript || document.scripts[document.scripts.length-1];
            let el = document.getElementById("${id}");
            do {
                let next = el.nextSibling;
                el.parentNode.removeChild(el);
                el = next;
            } while (el && el !== self);
            self.parentNode.removeChild(self);
        })();</script>

        <#nested>
    </@atom>
</#if>
</#macro>

<!DOCTYPE html>
<html>
	<head></head>
	<body>

	    <div>My page!</div>
	    <br><br>


        <@errorBoundary>
            <div>${throwsException}</div>
        </@errorBoundary>

        <br><br>

        <@errorBoundary fallback=myErrorFallback>
            <div>${throwsException}</div>
        </@errorBoundary>

        <br><br>

        <@errorBoundary fallback='<div style="border: 1px solid red">HTML string fallback</div>'>
            <div>${throwsException}</div>
        </@errorBoundary>


        <br><br>
        <div>Page finished!</div>

	</body>
</html>



<#macro myErrorFallback>
    <div style="border: 1px solid red">
        This is my custom error fallback macro. <br>
        Much better than the generic one.
    </div>
</#macro>

<#macro sharedErrorFallback>
    <div style="border: 1px solid DarkMagenta">
        This is my general error fallback macro. <br>
        This should be picked up automatically when another fallback is not specified.
    </div>
</#macro>



<#-- Inspiration: https://reactjs.org/docs/error-boundaries.html -->
<#macro errorBoundary fallback={}>
    <#flush> <#-- Doesn't do anything inside an attempt block -->
    <#attempt>
        <#nested>
    <#recover>
        <#if fallback?is_macro>
            <@fallback />
        <#elseif fallback?is_string>
            ${fallback?no_esc}
        <#elseif fallback?is_markup_output>
            ${fallback}
        <#elseif sharedErrorFallback?? && sharedErrorFallback?is_macro>
            <@sharedErrorFallback />
        <#else>
            <div style="border: 1px solid red">
                Generic error fallback
            </div>
        </#if>
    </#attempt>
</#macro>
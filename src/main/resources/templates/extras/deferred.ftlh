<!DOCTYPE html>
<html>
	<head>
	    <style>
	        li + li { padding-top: 0.5rem; }
	        .box { width: 25%; border: 1px solid black; padding: 0.5em; }
	        .box .box { width: 80%; border-color: grey; margin-top: 0.5em; }
	        .box .box .box { border-color: blue; }
	     </style>
	</head>
	<body>

	    <ul>
            <li> <div>My page using deferred macro with callables!</div> </li>

            <li>
                <@deferred> <div>${myData1}</div> </@deferred>
            </li>
            <li>
                <@deferred fallback=myLoadingMacro> <div>${myData2}</div> </@deferred>
            </li>
            <li>
                <@deferred fallback="Loading (with HTML string fallback) ...">
                    <div class="box">

                        <div>${myData3}</div>

                        <div class="box">
                            <@deferred fallback="Loading (nested) ...">
                                <div>${myData4}</div>
                            </@deferred>

                            <div class="box">
                                <@deferred fallback="Loading (nested 2) ...">
                                    ${myData5}
                                </@deferred>
                            </div>
                        </div>

                    </div>
                </@deferred>
            </li>
        </ul>

        <div>Page finished!</div>
        <@triggerDeferred />
	</body>
</html>

<#macro myLoadingMacro>Loading (with macro fallback) ...</#macro>
<#macro sharedDeferredFallback>Loading (with default fallback) ...</#macro>
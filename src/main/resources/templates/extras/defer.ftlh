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
            <li> <div>My page using defer macro!</div> </li>

            <li>
                <@defer> <div>${myData1}</div> </@defer>
            </li>
            <li>
                <@defer fallback=myLoadingMacro> <div>${myData2}</div> </@defer>
            </li>
            <li>
                <@defer fallback="Loading (with HTML string fallback) ...">
                    <div class="box">

                        <div>${myData3}</div>

                        <div class="box">
                            <@defer fallback="Loading (nested) ...">
                                <div>${myData4}</div>
                            </@defer>

                            <div class="box">
                                <@defer fallback="Loading (nested 2) ...">
                                    ${myData5}
                                </@defer>
                            </div>
                        </div>

                    </div>
                </@defer>
            </li>
        </ul>

        <div>Page finished!</div>
        <@renderDeferred />
	</body>
</html>

<#macro myLoadingMacro>Loading (with macro fallback) ...</#macro>
<#macro sharedDeferFallback>Loading (with default fallback) ...</#macro>
<!DOCTYPE html>
<html>
    <head>
        <style>
            li + li, ol { padding-top: 0.5rem; }
            .box { width: 25%; border: 1px solid black; padding: 0.5em; }
            .box .box { width: 80%; border-color: grey; margin-top: 0.5em; }
            .box .box .box { border-color: blue; }
        </style>
    </head>
    <body>

        <ul>
            <li> <div>My page using asyncDefer macro!</div> </li>

            <li>
                <@asyncDefer dependencies="myData1"> <div>${myData1}</div> </@asyncDefer>
            </li>
            <li>
                <@asyncDefer dependencies="myData2" fallback=myLoadingMacro> <div>${myData2}</div> </@asyncDefer>
            </li>
            <li>
                <@asyncDefer dependencies="myData3" fallback="Loading (with HTML string fallback) ...">
                    <div class="box">

                        <div>${myData3}</div>

                        <div class="box">
                            <@asyncDefer dependencies="myData4" fallback="Loading (nested) ...">
                                <div>${myData4}</div>
                            </@asyncDefer>

                            <div class="box">
                                <@asyncDefer dependencies=["myData5", "myData6"] fallback="Loading (nested 2) ...">
                                    <div> Multiple futures waited for together: </div>
                                    <ol>
                                        <li> ${myData5} </li>
                                        <li> ${myData6} </li>
                                    </ol>
                                </@asyncDefer>
                            </div>
                        </div>

                    </div>
                </@asyncDefer>
            </li>
        </ul>

        <div>Page finished!</div>
        <@renderAsyncDeferred />
    </body>
</html>

<#macro myLoadingMacro>Loading (with macro fallback) ...</#macro>
<#macro sharedDeferFallback>Loading (with default fallback) ...</#macro>
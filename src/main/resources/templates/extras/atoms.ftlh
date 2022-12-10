<!DOCTYPE html>
<html>
	<head>
	    <style> li + li { padding-top: 0.5rem; } </style>
	</head>
	<body>

        <#-- Compare the behaviour of this page to /callables/basic -->

	    <ul>
            <li> <div>My page using atom macro with callables!</div> </li>
            <#-- (It would work equally well with futures.) -->

            <@atom>
                <li> <div>${myData}</div> </li>
            </@atom>
            <@atom>
                <li> <div>${myData2}</div> </li>
            </@atom>
        </ul>

        <div>Page finished!</div>


	</body>
</html>

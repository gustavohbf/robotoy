<%@ page contentType="text/html;charset=iso-8859-1" 
	language="java"%><%@ taglib uri="robotoy.tld" prefix="app" %><!DOCTYPE html>
<html>
<head>
	<title>RoboToy Admin Options</title>
	<link rel="manifest" href="manifest.json">
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width,initial-scale=1.0">
	<meta name="copyright" content="© 2016 https://github.com/gustavohbf/robotoy" />
	<link rel="stylesheet" type="text/css" href="buttons.css" />
</head>
<body>
<img src="logo.png" alt="RoboToy" width="200"/> 
<p><h1>Admin Options Page</h1></p>
<input type="button" class="blue button" value="Play" onclick="playGame()">
<br>
<input type="button" class="blue button" value="Options" onclick="showOptions()">
<br>
<script>
function playGame() {
	window.location='<app:currentpage/>'
}
function showOptions() {
	window.location='admin.jsp'
}
</script>
</body>
</html>
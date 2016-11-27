<%@ page contentType="text/html;charset=iso-8859-1" 
	language="java"
	import="org.guga.robotoy.rasp.game.*"%><%@ taglib uri="robotoy.tld" prefix="app" %><!DOCTYPE html>
<html>
<head>
	<title>RoboToy</title>
	<link rel="manifest" href="manifest.json">
	<meta charset="UTF-8">
	<meta http-equiv="refresh" content="3;url=<app:currentpage/>" />
	<meta name="viewport" content="width=device-width,initial-scale=1.0">
	<meta name="copyright" content="© 2016 https://github.com/gustavohbf/robotoy" />
	<link rel="stylesheet" type="text/css" href="buttons.css" />
	<script src="jquery-1.12.0.min.js"></script>
	<style>
.rcorners {
    border-radius: 25px;
    border: 2px solid #2173AD;
    padding: 20px; 
    width: 60%;
    height: 60%; 
    text-align: center;
    margin: 0 auto;
    background: rgba(255,255,255,0.8);
}
.copyright {
	text-align: center;
	text-shadow: 0px 2px 2px #4d4d4d; 
}
html {
    background: url(background.jpg) no-repeat center fixed; 
    background-size: cover;
}
	</style>
</head>
<body>
<form>
<div id="logo">
<table class="rcorners" border="0">
<tr><td>
<img src="logo.png" alt="RoboToy" />
</td></tr>
</table>
<div class="copyright">&copy; 2016 https://github.com/gustavohbf/robotoy</div>
</div>
</form>
<script>
$(document).ready(function(){
	$( "#logo" ).hide(0).delay(100).fadeIn(1000);
});
</script>
<!-- Background image Copyright: http://www.pptgrounds.com/abstract/5438-abstract-blue-waves-2-backgrounds -->
</body>
</html>
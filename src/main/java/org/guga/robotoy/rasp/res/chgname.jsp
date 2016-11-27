<%@ page contentType="text/html;charset=iso-8859-1" 
	language="java"%><%@ taglib uri="robotoy.tld" prefix="app" %><app:chgname userName="${param['username']}" change="${param['changeit']}" alertProperty="alert"/><!DOCTYPE html>
<html>
<head>
	<title>RoboToy</title>
	<link rel="manifest" href="manifest.json">
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width,initial-scale=1.0">
	<meta name="copyright" content="© 2016 https://github.com/gustavohbf/robotoy" />
	<link rel="stylesheet" type="text/css" href="buttons.css" />
	<style>
.rcorners {
    border-radius: 25px;
    border: 2px solid #2173AD;
    padding: 20px; 
    width: 220;
    height: 150px; 
}
p.alert {
	font-family: Arial, Helvetica, sans-serif;
	font-size: 14px;
	text-align:center;
	color: #ff1111;
}
	</style>
   <script src="jquery-1.12.0.min.js"></script>
</head>
<body>
<form method="post">
<table class="rcorners" border="0">
<tr><td colspan="2">
<img src="logo.png" alt="RoboToy" width="180"/>
</td></tr>
<tr><td colspan="2"><app:alert property="alert"/></td></tr>
<tr><td>Your Name:</td>
<td>
<input type='text' name='username' value='${param["username"]}' maxlength='10' size='10' autofocus autocorrect='off' autocapitalize='on'>
</td></tr>
<tr><td colspan="2">&nbsp;</td></tr>
<tr><td>
<input type="submit" class="blue button" value="Change Name">
</td><td>
<input type="button" class="blue button" value="Cancel" onclick="window.location='<app:currentpage/>'">
</td></tr>
</table>
<input type="hidden" name="changeit" value="true">
</form>
<script>
var username="<app:username/>";
var connection = new WebSocket('ws://<%=request.getLocalAddr()%>:<%=request.getLocalPort()%>/ws/player/'+encodeURI(username));
connection.onerror = function(error) {
	console.log('WebSocket Error ' + error)
};
</script>
</body>
</html>
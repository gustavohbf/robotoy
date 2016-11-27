<%@ page contentType="text/html;charset=iso-8859-1" 
	language="java"%><%@ taglib uri="robotoy.tld" prefix="app" %><app:setup alertProperty="alert"/><!DOCTYPE html>
<html>
<head>
	<title>RoboToy Setup Page</title>
	<link rel="manifest" href="manifest.json">
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width,initial-scale=1.0">
	<meta name="copyright" content="© 2016 https://github.com/gustavohbf/robotoy" />
	<link rel="stylesheet" type="text/css" href="buttons.css" />
	<style>
p.alert {
	font-family: Arial, Helvetica, sans-serif;
	font-size: 14px;
	text-align:center;
	color: #ff1111;
}
.dialogpanel {
    border-radius: 25px;
    border: 2px solid #2173AD;
    padding: 20px; 
	top: 15%;
    left: 15%;
    width: 60%;
    height: 60%;
    text-align: center;
	margin: 0 auto;
	vertical-align: middle;	
	background: white;
}
.dialogpanel table {
	border: 0px;
	display: table;
	width: 100%;	
	height: 100%;
}
	</style>
</head>
<body>
<form id="setup" method="post">
<img src="logo.png" alt="RoboToy" width="200"/> 
<p><h1>Setup Page</h1></p>
<app:alert property="alert" defaultValue="RoboToy is currently running as STANDALONE. Please choose one option below:"/>
<input type="button" class="blue button" value="Test Drive (Single Player)" onclick="startSinglePlayer()">
<br>
<input type="button" class="blue button" value="Configure Network" onclick="configNetwork()">
<br>
<input type="button" class="blue button" value="Reboot RoboToy" onclick="rebootRobotoy()">
<input type="hidden" name="cmd" id="cmd" value="">
<div id="setupdialog" style="position:absolute;display:none" class="dialogpanel">
<table border="0"><thead><tr><th colspan="2">
<h2>Wireless Network Configuration:</h2></th></tr>
<tbody><tr><td>
Network Name:</td><td>
<input type='text' name='ssid' value='${param["ssid"]}' autofocus autocorrect='off'>
</td></tr>
<tr><td>
Authentication:</td><td>
<select name="auth">
	<option value="none" ${param["auth"]=="none"?'selected="selected"':''}>None</option>
	<option value="wep" ${param["auth"]=="wep"?'selected="selected"':''}>WEP</option>
	<option value="wpa" ${param["auth"]=="wpa"?'selected="selected"':''}>WPA</option>
	<option value="wpa2" ${param["auth"]=="wpa2"?'selected="selected"':''}>WPA2</option>
</select>
</td></tr>
<tr><td>
Password:</td><td>
<input type='password' name='p' value='${param["p"]}' autocorrect='off'>
</td></tr>
<tr><td>
<input type="button" class="blue button" value="Submit" onclick="configNetworkSubmit()">
</td><td>
<input type="button" class="blue button" value="Cancel" onclick="configNetworkDismiss()">
</td></tr></tbody></table>
</div>
<div id="confirmdialog" style="position:absolute;display:none" class="dialogpanel">
<table border="0"><thead><tr><th colspan="2">
<h2>Are you sure?</h2></th></tr>
<tbody><tr><td>
<input type="button" class="blue button" value="Yes" onclick="rebootRobotoySubmit()">
</td><td>
<input type="button" class="blue button" value="No" onclick="rebootRobotoyDismiss()">
</td></tr></tbody></table>
</div>
</form>
<script>
function startSinglePlayer() {
	document.getElementById("cmd").value = "testdrive";
	document.getElementById("setup").submit();
}
function configNetwork() {
	var div = document.getElementById("setupdialog");	
	div.style.display = "flex"
}
function configNetworkSubmit() {
	var f = document.getElementById("setup");
	var ssid = f["ssid"];
	var auth = f["auth"];
	var p = f["p"];
	if (!ssid || !ssid.value || ssid.value=="") {
		window.alert("Missing network name!");
		return;
	}
	if (!auth || !auth.options || !auth.options[auth.selectedIndex]) {
		window.alert("Missing authentication method!");
		return;
	}
	f["cmd"].value = "config";
	f.submit();
}
function configNetworkDismiss() {
	var div = document.getElementById("setupdialog");	
	div.style.display = "none"
}
function rebootRobotoy() {
	var div = document.getElementById("confirmdialog");	
	div.style.display = "flex"
}
function rebootRobotoySubmit() {
	document.getElementById("cmd").value = "reboot";
	document.getElementById("setup").submit();
}
function rebootRobotoyDismiss() {
	var div = document.getElementById("confirmdialog");	
	div.style.display = "none"
}
</script>
</body>
</html>
<%@ page contentType="text/html;charset=iso-8859-1" 
	language="java"
	import="org.guga.robotoy.rasp.network.*" %><%

	InetUtils.NetAdapter adapters[] = InetUtils.getNetAdapters();
	pageContext.setAttribute("adapters", adapters);

%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@ taglib uri="robotoy.tld" prefix="app" %><app:playmode/><!DOCTYPE html>
<html>
<head>
	<title>RoboToy Admin Page</title>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width,initial-scale=1.0">
	<meta name="copyright" content="© 2016 https://github.com/gustavohbf/robotoy" />
	<link rel="stylesheet" type="text/css" href="buttons.css" />
	<script src="jquery-1.12.0.min.js"></script>
</head>
<body>
<img src="logo.png" alt="RoboToy" width="200"/> 
<p><h1>Admin Page</h1></p>
<br>
<h3>Current game mode is: <c:out value="${playmode}" /></h3>
<form method="post">
<h3>Query Information:</h3>
<ul>
	<li><a href="/log">Log Files (full file)</a></li>
	<li><a href="/log?last=100">Log Files (last 100 lines)</a></li>
	<li><a href="/tableview.jsp?title=Robots&url=robots">Robots</a></li>
	<li><a href="/tableview.jsp?title=Players&url=players">Players</a></li>
	<li><a href="/tableview.jsp?title=Statistics&url=stats">Statistics</a></li>
	<li><a href="/tableview.jsp?title=Network+Interfaces&url=net">Network Interfaces</a></li>
	<li><a href="/tableview.jsp?title=Active+WebSockets&url=sockets">Active WebSockets</a></li>
	<li><a href="" onclick="this.href='/wifi?net='+document.getElementById('net').value">WiFi Scan (JSON)</a> 
	<select id="net" name="net">
<c:forEach items="${adapters}" var="adapter">
		<option value="${adapter.name}" ${param.net == adapter.name ? 'selected' : ''}>${adapter.name}</option>
</c:forEach>
	</select>
	</li>
<hr>
<h3>Take Action:</h3>
	<li><a onClick="restartGame();return false;" href="#">Restart Game</a></li>
	<li><a onClick="becomeAP();return false;" href="#">WiFi: Become Access Point</a>
	<select id="net_ap" name="net_ap">
		<option value="VIRTUAL_AP">Internal WiFi + virtual (wlan0 + uap0)</option>
		<option value="INTERNAL_AP">Internal WiFi only (wlan0)</option>
		<option value="EXTERNAL_2G5">External WiFi (2.5GHz)</option>
		<option value="EXTERNAL_5G">External WiFi (5GHz)</option>
	</select>
	</li>
<c:if test="${playmode eq 'MULTIPLAYER'}">
	<li><a onClick="changePlayMode('STANDALONE');return false;" href="#">Change game mode to: STANDALONE</a></li>
</c:if>
<c:if test="${playmode eq 'STANDALONE'}">
	<li><a onClick="changePlayMode('MULTIPLAYER');return false;" href="#">Change game mode to: MULTIPLAYER</a></li>
</c:if>
	<li><a href="/setup.jsp?netdialog=true">Configure Network</a>
	<li><a onClick="reboot();return false;" href="#">Reboot</a></li>
	<li><a onClick="shutdown();return false;" href="#">Shutdown</a></li>
</ul>
<div style="position:relative;float:left"><input type="button" class="blue button" value="Back" onclick="window.history.back();"></div>
<div style="position:relative;float:right"><input type="button" class="blue button" value="Enter Game" onclick="window.location='/index.jsp'"></div>
</form>
<script>
function restartGame() {
	$.ajax({
	    url: '/restart',
	    type: 'POST',
	    async: false,
	    success: function(text) { alert(text); }
	});
}
function becomeAP() {
	if (!confirm("Are you sure?"))
		return;
	var data_contents = {
		mode: document.getElementById('net_ap').value 
	};	
	$.ajax({
	    url: '/wifimode',
	    type: 'PUT',
	    data: JSON.stringify(data_contents),
	    async: false,
	    success: function(text) { alert(text); }
	});
}
function changePlayMode(m) {
	if (!confirm("Are you sure?"))
		return;
	var data_contents = {
		mode: m 
	};	
	$.ajax({
	    url: '/playmode',
	    type: 'PUT',
	    data: JSON.stringify(data_contents),
	    async: false,
	    success: function(text) { alert(text); }
	});
}
function reboot() {
	if (!confirm("Are you sure?"))
		return;
	$.ajax({
	    url: '/reboot',
	    type: 'POST',
	    async: false,
	    success: function(text) { alert(text); }
	});
}
function shutdown() {
	if (!confirm("Are you sure?"))
		return;
	$.ajax({
	    url: '/shutdown',
	    type: 'POST',
	    async: false,
	    success: function(text) { alert(text); }
	});
}
function configNetwork() {
	var div = document.getElementById("setupdialog");	
	div.style.display = "flex"	
}
</script>
</body>
</html>
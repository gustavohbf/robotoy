<%@ page contentType="text/html;charset=iso-8859-1" 
	language="java"%><%@ taglib uri="robotoy.tld" prefix="app" %><app:access/><!DOCTYPE html>
<html>
<head>
	<title>RoboToy Lobby</title>
	<link rel="manifest" href="manifest.json">
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width,initial-scale=1.0">
	<meta name="copyright" content="© 2016 https://github.com/gustavohbf/robotoy" />
	<link rel="stylesheet" type="text/css" href="buttons.css" />
	<link rel="stylesheet" type="text/css" href="tables.css" />
	<link rel="stylesheet" type="text/css" href="waiting.css" />
	<link rel="stylesheet" type="text/css" href="disconnected.css" />
	<link rel="stylesheet" type="text/css" href="lobby_view.css" />
	<script src="jquery-1.12.0.min.js"></script>
	<app:errors/>
</head>
<body>
<form id="lobby" method="post">

<div class="container">
    <div class="fixed">
       	<img src="logo.png" alt="RoboToy" width="200"/> 
       	<p><h1>Lobby Room</h1></p>
		<p><h2>Choose a Robot:</h2></p>
    </div>
    <div class="flex-item">
		<table id="players" class="rcorners tableSection">
		<thead>
		<tr><th>Name</th><th>IP Address</th><th>Ping Time</th></tr>
		</thead>
		<tbody>
		<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
		<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
		<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
		<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
		<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
		<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
		<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
		<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
		</tbody>
		</table>
    </div>
</div>
<div style="width: 100%; height: 250px; overflow: scroll; overflow-y: hidden; padding: 15px 15px;">
   <table class="rcorners">
   <tr id="robots"></tr>
   </table>
</div>
<div>
<div style="position:relative;float:left"><input type="button" class="blue button" value="Start Game" onclick="ROBOTOY.startGame()"></div>
<div style="position:relative;float:right"><input type="button" class="blue button" value="Name" onclick="ROBOTOY.changeName()"></div>
<div style="position:relative;float:right"><input type="button" class="blue button" value="Color" onclick="ROBOTOY.changeColor()"></div>
<div id="robot" style="position:absolute;display:none"><span class="text" id="robot_owner"></span><br>
   <img src="hi_def.png" alt="RoboToy" width="120" height="120"><br>
   <input type="button" name="robot_choose" class="small blue button" value="Choose">
   <input type="hidden" name="robot_address" value="" onclick="">
   <div id="robot_color" style="position:absolute;width:80px;bottom:-40px">&nbsp;</div>
</div>
<div id="waiting" style="position:absolute;display:none" class="waiting"><h1>Waiting other players...</h1></div>
<div id="disconnected" style="position:absolute;display:none" class="disconnected"><h1>Disconnected! Trying to reconnect...</h1></div>
<div id="changecolor" style="position:absolute;display:none" class="dialogpanel">
<table border="0"><thead><tr><th>
<h2>Please choose a color for your robot:</h2></th></tr>
<tbody><tr><td>
<table id="changecolortable"><tbody>
<tr><td style="background-color:#00FF00">&nbsp;</td><td style="background-color:#FF0000">&nbsp;</td><td style="background-color:#0000FF">&nbsp;</td><td style="background-color:#FF00FF">&nbsp;</td></tr>
<tr><td style="background-color:#00FFFF">&nbsp;</td><td style="background-color:#FFFF00">&nbsp;</td><td style="background-color:#FFFFFF">&nbsp;</td><td style="background-color:#FF8C00">&nbsp;</td></tr>
<tr><td style="background-color:#FF008C">&nbsp;</td><td style="background-color:#8C8C8C">&nbsp;</td><td style="background-color:#008C00">&nbsp;</td><td style="background-color:#8C0000">&nbsp;</td></tr>
<tr><td style="background-color:#00008C">&nbsp;</td><td style="background-color:#004C00">&nbsp;</td><td style="background-color:#4C0000">&nbsp;</td><td style="background-color:#00004C">&nbsp;</td></tr>
</tbody></table>
</td></tr></tbody></table>
</div>
<div id="choosecontrols" style="position:absolute;display:none" class="dialogpanel">
<table border="0"><thead><tr><th colspan="2">
<h2>Please choose how to control your robot:</h2></th></tr>
<tbody><tr><td>
<input type="button" class="blue button" value="Buttons" onclick="ROBOTOY.chooseControls('buttons')">
</td><td>
<input type="button" class="blue button" value="Tilt" onclick="ROBOTOY.chooseControls('tilt')">
</td></tr></tbody></table>
</div>
<input type="hidden" name="username" id="username" value="<app:username/>">
</form>
<script src="lobby_comm.js"></script>
<script src="lobby_robots.js"></script>
<script src="device_motion.js"></script>
<script src="utils.js"></script>
<script>

var ROBOTOY = ROBOTOY || {}

ROBOTOY.game_ready = false;
ROBOTOY.game_start_options = null;
ROBOTOY.username = document.getElementById("username").value;
ROBOTOY.session_id = "<app:sessionid/>";

ROBOTOY.ROBOTS.robots_row = document.getElementById("robots");

ROBOTOY.PLAYERS = {
	table_players : null
}

ROBOTOY.PLAYERS.table_players = document.getElementById("players");

ROBOTOY.PLAYERS.setPlayer = function(i,name,addr,ping) {
	var table = ROBOTOY.PLAYERS.table_players;
	table.rows[i].cells[0].innerHTML = name;
	table.rows[i].cells[1].innerHTML = addr;
	table.rows[i].cells[2].innerHTML = ping;
}
ROBOTOY.PLAYERS.clearPlayer = function(i) {
	var table = ROBOTOY.PLAYERS.table_players;
	table.rows[i].cells[0].innerHTML = "";
	table.rows[i].cells[1].innerHTML = "";
	table.rows[i].cells[2].innerHTML = "";
}
ROBOTOY.changeName = function() {
	if (ROBOTOY.game_ready)
		return;
	var lobby = document.getElementById("lobby");
	lobby.action = "chgname.jsp";
	lobby.submit();
}
ROBOTOY.changeColor = function() {
	if (ROBOTOY.game_ready)
		return;
	var robot_owned = ROBOTOY.ROBOTS.getRobotOwned();
	if (!robot_owned) {
		window.alert("First you must choose an available robot!");
		return;
	}
	var table = document.getElementById("changecolortable")
	for (var i=0;i<table.rows.length;i++) {
		for (var j=0;j<table.rows[i].cells.length;j++) {
			table.rows[i].cells[j].onclick = function() {
				var div = document.getElementById("changecolor");
				div.style.display = "none";	
				if (!ROBOTOY.COMM.has_connection) {
					window.alert('not connected!');
					return;
				}
				ROBOTOY.COMM.connection.send('{"setcolor":{"color":"'+rgb2hex(this.style.backgroundColor)+'"}}');
			};
		}
	}
	var div = document.getElementById("changecolor");	
	div.style.display = "flex"
}
ROBOTOY.startGame = function() {
	if (ROBOTOY.game_ready)
		return;
	var robot_owned = ROBOTOY.ROBOTS.getRobotOwned();
	if (!robot_owned) {
		window.alert("First you must choose an available robot!");
		return;
	}
	if (ROBOTOY.MOTION.isOrientationSupported()) {
		// interrupt start process to query control mode
		var div = document.getElementById("choosecontrols");
		div.style.display = "flex";
		return;
	}	
	ROBOTOY.startGame2();
}
ROBOTOY.startGame2 = function() {
	var c = ROBOTOY.COMM;
	if (!c.has_connection) {
		window.alert('not connected!');
		return;
	}
	try {
		if (ROBOTOY.game_start_options)
			c.connection.send('S'+ROBOTOY.game_start_options);
		else
			c.connection.send('S');
	}
	catch (error) {
		window.alert(error);
	}	
}
ROBOTOY.chooseControls = function(mode) {
	ROBOTOY.game_start_options = '{"mode":"' + mode + '"}';
	var div = document.getElementById("choosecontrols");
	div.style.display = "none";
	ROBOTOY.startGame2();
}
ROBOTOY.displayReadyMessage = function() {
	var div = document.getElementById("waiting");
	div.style.display = "flex"
	ROBOTOY.game_ready = true;		
}
ROBOTOY.preloadGameResources = function() {
	// load and keep resources in browser cache
	if (ROBOTOY && ROBOTOY.EXPLOSIONS) {
		ROBOTOY.EXPLOSIONS.loadExplosionSprites();
	}
}

ROBOTOY.init = function() {
	ROBOTOY.COMM.connect_ws('ws://<%=request.getLocalAddr()%>:<%=request.getLocalPort()%>/ws/player/'+encodeURI(ROBOTOY.username), 
			'disconnected');
	ROBOTOY.MOTION.init();
	setTimeout(ROBOTOY.preloadGameResources,100);
}

ROBOTOY.init();
</script>
<script src="sprites.js"></script>
<script src="explosion.js"></script>
</body>
</html>
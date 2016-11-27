<%@ page contentType="text/html;charset=iso-8859-1" 
	language="java"%><%@ taglib uri="robotoy.tld" prefix="app" %><app:access/><!DOCTYPE html>
<html>
<head>
	<title>RoboToy Summary</title>
	<link rel="manifest" href="manifest.json">
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta name="copyright" content="© 2016 https://github.com/gustavohbf/robotoy" />
	<link rel="stylesheet" type="text/css" href="buttons.css" />
	<link rel="stylesheet" type="text/css" href="tables.css" />
	<style>
table.tableSection tbody {
	height: 100px;
}
.rcorners {
    border-radius: 25px;
    border: 2px solid #2173AD;
    padding: 10px; 
}
.container{
    display: flex;
}
.fixed{
    width: 200px;
    padding-top: 20px; 
    padding-right: 10px; 
}
.flex-item{
    flex-grow: 1;
    border-radius: 25px;
    border: 2px solid #2173AD;
    padding: 20px; 
    opacity: 0.9;
    filter: alpha(opacity=90); 
	margin: 0 auto;
	vertical-align: middle;
	background: white;
	background: -webkit-linear-gradient(blue, white);
	background: -o-linear-gradient(blue, white);
	background: -moz-linear-gradient(blue, white);
	background: linear-gradient(blue, white);
}
.flex-item h1 {
	color: #fff;
	color: rgba(255, 255, 255, 0.75);
	font: 36px "Helvetica Neue", Helvetica, Arial, sans-serif;
	text-align: center;
	text-shadow: 0 1px 0 #ccc,
               0 2px 0 #c9c9c9,
               0 3px 0 #bbb,
               0 4px 0 #b9b9b9,
               0 5px 0 #aaa,
               0 6px 1px rgba(0,0,0,.1),
               0 0 5px rgba(0,0,0,.1),
               0 1px 3px rgba(0,0,0,.3),
               0 3px 5px rgba(0,0,0,.2),
               0 5px 10px rgba(0,0,0,.25),
               0 10px 10px rgba(0,0,0,.2),
               0 20px 20px rgba(0,0,0,.15);
}
.waiting {
    border-radius: 25px;
    border: 2px solid #AD7321;
    padding: 20px; 
	top: 15%;
    left: 15%;
    width: 60%;
    height: 60%;
    opacity: 0.9;
    filter: alpha(opacity=90); 
    text-align: center;
	margin: 0 auto;
	vertical-align: middle;
	background: white;
	background: -webkit-linear-gradient(blue, red);
	background: -o-linear-gradient(blue, red);
	background: -moz-linear-gradient(blue, red);
	background: linear-gradient(blue, red);
}
.waiting h1 {
	color: white;
    text-shadow: 2px 2px 4px #000000;
    text-align: center;
	vertical-align: middle;
}
.disconnected {
    border-radius: 25px;
    border: 2px solid #AD7321;
    padding: 20px; 
	top: 15%;
    left: 15%;
    width: 60%;
    height: 60%;
    opacity: 0.9;
    filter: alpha(opacity=90); 
    text-align: center;
	margin: 0 auto;
	vertical-align: middle;
	background: white;
	background: -webkit-linear-gradient(yellow, red);
	background: -o-linear-gradient(yellow, red);
	background: -moz-linear-gradient(yellow, red);
	background: linear-gradient(yellow, red);
}
.disconnected h1 {
	color: white;
    text-shadow: 2px 2px 4px #000000;
    text-align: center;
	vertical-align: middle;
}
	</style>
	<script src="jquery-1.12.0.min.js"></script>
</head>
<body>
<div class="container">
    <div class="fixed">
       	<img src="logo.png" alt="RoboToy" width="200"/> 
    </div>
    <div class="flex-item">
       	<p><h1>Game Over</h1></p>
	</div>
</div>
&nbsp;
<table id="ranking" class="rcorners tableSection">
<thead>
<tr><th>Name</th><th>Color</th><th>Life</th><th>Kills</th></tr>
</thead>
<tbody>
<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
</tbody>
</table>
<div style="position:relative;float:left"><input type="button" class="blue button" value="Play Again" onclick="playAgain()"></div>
<script>
var game_ready = false;
var waiting_reconnect = false;
var has_connection = false;
var connection = null;
var username="<app:username/>";
function connect_ws() {
	connection = new WebSocket('ws://<%=request.getLocalAddr()%>:<%=request.getLocalPort()%>/ws/player/'+encodeURI(username));
	connection.onerror = function(error) {
		console.log('WebSocket Error ' + error)
		if (waiting_reconnect) {
			setTimeout(connect_ws,1000);
		}
	};
	connection.onopen = function() {
		if (waiting_reconnect) {
			var div = document.getElementById("disconnected");
			div.style.display = "none"
			waiting_reconnect = false;
		}
		has_connection = true;
		connection.send('K');
	};
	connection.onclose = function() {
		console.log('WebSocket Closed Connection')
		has_connection = false;
		if (game_ready)
			return;
		var div = document.getElementById("disconnected");
		div.style.display = "flex"
		waiting_reconnect = true;
		setTimeout(connect_ws,1000);
	};
	connection.onmessage = function(e) {
		var msg = e.data;		
		if (msg.charAt(0)=='{' && msg.slice(-1)=='}') {
			var obj = JSON.parse(msg);
			if (obj.ranking) {
				gotRankingFromServer(obj);
			}
			else if (obj.ready) {
				displayReadyMessage();
			}
			else if (obj.restartgame) {
				gotGameRestarted();
			}
		}
		else {
			console.log('Server: '+msg);
		}
	};
}
connect_ws();
function playAgain() {
	if (game_ready)
		return;
	if (!has_connection) {
		window.alert('not connected!');
		return;
	}
	try {
		connection.send('A');
	}
	catch (error) {
		window.alert(error);
	}	
}
function setRanking(i,name,color,life,kills) {
	var table = document.getElementById("ranking");
	table.rows[i].cells[0].innerHTML = name;
	table.rows[i].cells[2].innerHTML = life;
	table.rows[i].cells[3].innerHTML = kills;
	table.rows[i].cells[1].style.background = color;
}
function clearRanking(i) {
	var table = document.getElementById("ranking");
	table.rows[i].cells[0].innerHTML = "";
	table.rows[i].cells[1].innerHTML = "";
	table.rows[i].cells[2].innerHTML = "";
	table.rows[i].cells[3].innerHTML = "";
	table.rows[i].cells[1].style.background = "#ffffff";
}
function gotRankingFromServer(info) {
	var ranking_size = info.ranking.length;
	for (var i=0;i<ranking_size;i++) {
		var r = info.ranking[i];
		setRanking(i+1,r.player,r.color,r.life,r.kills);
	}
	var table = document.getElementById("ranking");
	for (var i=ranking_size+1;i<table.rows.length;i++) {
		clearRanking(i);
	}
}
function displayReadyMessage() {
	var div = document.getElementById("waiting");
	div.style.display = "flex"
	game_ready = true;		
}
function gotGameRestarted() {
	window.location = "lobby.jsp"; 
}
</script>
<div id="waiting" style="position:absolute;display:none" class="waiting"><h1>Waiting other players...</h1></div>
<div id="disconnected" style="position:absolute;display:none" class="disconnected"><h1>Disconnected! Trying to reconnect...</h1></div>
</body>
</html>
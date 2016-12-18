/*!
 RoboToy Lobby Room Communication
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used in websocket communication during lobby room section.
 *
 */

var ROBOTOY = ROBOTOY || {}

ROBOTOY.PLAYERS = ROBOTOY.PLAYERS || {}

ROBOTOY.PLAYERS.setPlayer =  ROBOTOY.PLAYERS.setPlayer || (function(){})
ROBOTOY.PLAYERS.clearPlayer =  ROBOTOY.PLAYERS.clearPlayer || (function(){})

ROBOTOY.ROBOTS = ROBOTOY.ROBOTS || {}

ROBOTOY.ROBOTS.addRobot = ROBOTOY.ROBOTS.addRobot || (function(){})

ROBOTOY.COMM = {

	has_connection : false,
	
	waiting_reconnect : false,
	
	connection : null
};

ROBOTOY.COMM.connect_ws = function(url,div_disconnected_id) {

	var c = ROBOTOY.COMM;

	c.connection = new WebSocket(url);
	
	var reconnect = function() {
		ROBOTOY.COMM.connect_ws(url,div_disconnected_id);
	}
	
	c.connection.onerror = function(error) {
		console.log('WebSocket Error ' + error)
		if (c.waiting_reconnect) {
			setTimeout(reconnect,1000);
		}
	};
	
	c.connection.onopen = function() {
		if (c.waiting_reconnect) {
			var div = document.getElementById(div_disconnected_id);
			div.style.display = "none"
			c.waiting_reconnect = false;
		}
		c.has_connection = true;
		
		// send greetings
		c.sendGreetings();

		c.connection.send('P');
		c.connection.send('R');
	};
	
	c.connection.onclose = function() {
		console.log('WebSocket Closed Connection')
		c.has_connection = false;
		if (ROBOTOY.game_ready)
			return;
		var div = document.getElementById(div_disconnected_id);
		div.style.display = "flex"
		c.waiting_reconnect = true;
		setTimeout(reconnect,1000);
	};
	
	c.connection.onmessage = function(e) {
		var msg = e.data;		
		if (msg.charAt(0)=='{' && msg.slice(-1)=='}') {
			var obj = JSON.parse(msg);
			if (obj.players) {
				c.gotPlayersFromServer(obj);
			}
			else if (obj.robots) {
				c.gotRobotsFromServer(obj);
			}
			else if (obj.changename) {
				c.gotPlayerChangeName(obj);
			}
			else if (obj.newplayer) {
				c.gotNewPlayer(obj);
			}
			else if (obj.newrobot) {
				c.gotNewRobot(obj);
			}
			else if (obj.newid) {
				c.gotChangeIdentification(obj);
			}
			else if (obj.changeowner) {
				c.gotChangeOwner(obj);
			}
			else if (obj.removeplayer) {
				c.gotRemovePlayer(obj);
			}
			else if (obj.removerobot) {
				c.gotRemoveRobot(obj);
			}
			else if (obj.setready) {
				c.gotReadyState(obj);
			}
			else if (obj.ready) {
				ROBOTOY.displayReadyMessage();
			}
			else if (obj.startgame) {
				c.gotGameStarted();
			}
			else if (obj.setcolor) {
				c.gotChangeColor(obj);
			}
			else if (obj.ping) {
				c.gotPing(obj);
			}
			else if (obj.updateping) {
				c.gotUpdatePing(obj);
			}
			else {
				console.log('Server: '+msg);
			}
		}
		else {
			console.log('Server: '+msg);
		}
	};
}

ROBOTOY.COMM.gotPlayersFromServer = function(info) {
	var num_players = info.players.length;
	var player_missing = true;
	for (var i=0;i<num_players;i++) {
		var player = info.players[i];
		ROBOTOY.PLAYERS.setPlayer(i+1,player.name,player.address,player.ping?player.ping:"?");
		if (ROBOTOY.username==player.name)
			player_missing = false;
	}
	var table = ROBOTOY.PLAYERS.table_players;
	if (table) {
		for (var i=num_players+1;i<table.rows.length;i++) {
			ROBOTOY.PLAYERS.clearPlayer(i);
		}
	}
	if (player_missing) {
		// Player was not found. Maybe RoboToy got restarted while our session was still on
		// Let's redirect to login page
		window.location = "login.jsp";
	}
}

ROBOTOY.COMM.gotPlayerChangeName = function(info) {
	var c = ROBOTOY.COMM;
	if (c.has_connection) {
		c.connection.send('P');
		c.connection.send('R');
	}
}

ROBOTOY.COMM.gotNewPlayer = function(info) {
	var c = ROBOTOY.COMM;
	if (c.has_connection)
		c.connection.send('P');
}

ROBOTOY.COMM.gotNewRobot = function(info) {
	var c = ROBOTOY.COMM;
	if (c.has_connection)
		c.connection.send('R');
}

ROBOTOY.COMM.gotChangeOwner = function(info) {
	var id = info.changeowner.id;
	var owner = info.changeowner.owner;
	var robots = ROBOTOY.ROBOTS.robots;
	if (robots) {
		for (var i=0;i<robots.length;i++) {
			var robot = robots[i];
			if (robot.id==id) {
				robot.setOwner(owner);
				break;
			}
		}
	}
}

ROBOTOY.COMM.gotChangeColor = function(info) {
	var id = info.setcolor.id;
	var color = info.setcolor.color;
	var robots = ROBOTOY.ROBOTS.robots;
	if (robots) {
		for (var i=0;i<robots.length;i++) {
			var robot = robots[i];
			if (robot.id==id) {
				robot.setColor(color);
				break;
			}
		}
	}	
}

ROBOTOY.COMM.gotChangeIdentification = function(info) {
	var id = info.newid.id;
	var addr = info.newid.address;
	var robots = ROBOTOY.ROBOTS.robots;
	if (robots) {
		for (var i=0;i<robots.length;i++) {
			var robot = robots[i];
			if (robot.addr==addr) {
				robot.id = id;
				break;
			}
		}
	}
}

ROBOTOY.COMM.gotRobotsFromServer = function(info) {
	var r = ROBOTOY.ROBOTS;
	var row = r.robots_row;
	if (!row)
		return;
	for (var i=row.cells.length-1;i>=0;i--) {
		row.deleteCell(i);
	}
	r.robots = [];
	var num_robots = info.robots.length;
	for (var i=0;i<num_robots;i++) {
		var robot = info.robots[i];
		r.addRobot(robot.id,robot.address,robot.owner,robot.color);
	}
}

ROBOTOY.COMM.gotRemovePlayer = function(info) {
	// remove ownership to removed player if any
	var robots = ROBOTOY.ROBOTS.robots;
	if (robots) {
		for (var i=0;i<robots.length;i++) {
			var robot = robots[i];
			if (robot.owner==info.removeplayer.name) {
				robot.setOwner(null);
				break;
			}
		}
	}
	// update player list
	ROBOTOY.COMM.connection.send('P');
}

ROBOTOY.COMM.gotRemoveRobot = function(info) {
	var id = info.removerobot.id;
	var row = ROBOTOY.ROBOTS.robots_row;
	if (!row)
		return;
	var robots = ROBOTOY.ROBOTS.robots;
	if (robots) {
		for (var i=0;i<robots.length;i++) {
			var robot = robots[i];
			if (robot.id==id) {
				robots.splice(i,1);
				row.deleteCell(i);
				break;
			}
		}
	}
}

ROBOTOY.COMM.gotReadyState = function(info) {
	console.log("Robot "+info.setready.id+" is ready!");
	if (info.startgame) {
		ROBOTOY.COMM.gotGameStarted();
	}
}

ROBOTOY.COMM.gotGameStarted = function() {
	window.location = "driving.jsp"; 
}

ROBOTOY.COMM.gotPing = function(info) {
	info.count++;
	try {
		var msg = JSON.stringify(info);
		ROBOTOY.COMM.connection.send(msg);
	}
	catch (error) { }
}

ROBOTOY.COMM.gotUpdatePing = function(info) {
	var table = ROBOTOY.PLAYERS.table_players;
	var name = escapeHtml(info.player.name);
	if (!name)
		return;
	if (table) {
		for (var i=0;i<table.rows.length;i++) {
			var row = table.rows[i];
			var name_in_row = row.cells[0].innerHTML;
			if (name_in_row==name) {
				row.cells[2].innerHTML = info.updateping;
				break;
			} 
		}
	}	
}

ROBOTOY.COMM.sendGreetings = function() {
	var c = ROBOTOY.COMM;
	c.connection.send('{"greetings":"'+ROBOTOY.session_id+'"}');
}
/*!
 RoboToy Driving Communication
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used in websocket communication.
 *
 * Depends on:
 * explosion.js
 * driving_audio.js
 * driving_view.js
 */
 
var ROBOTOY = ROBOTOY || {}

ROBOTOY.finishedWaiting = ROBOTOY.finishedWaiting || (function(){});

ROBOTOY.VIEW = ROBOTOY.VIEW || {}

ROBOTOY.COMM = {

	wifi : 0,
	
	wifi_level : 0,
	
	ping : false,

	waiting_reconnect : false,
	
	connection : null,

	has_connection : false,
	
	timed_info : null
};

ROBOTOY.COMM.connect_ws = function(url,div_disconnected_id,div_playfield_id,div_changeOrientation_id) {

	var c = ROBOTOY.COMM;
	
	c.connection = new WebSocket(url);
	
	var reconnect = function() {
		ROBOTOY.COMM.connect_ws(url,div_disconnected_id,div_playfield_id,div_changeOrientation_id);
	}
	
	c.connection.onopen = function() {
		if (c.waiting_reconnect) {
			var div = document.getElementById(div_disconnected_id);
			div.style.display = "none"
			var divp = document.getElementById(div_playfield_id);
			divp.style.opacity = 1
			var divo = document.getElementById(div_changeOrientation_id);
			divo.style.opacity = 1
			c.waiting_reconnect = false;
		}
		c.has_connection = true;
		
		// send greetings
		c.sendGreetings();
		
		if (!c.timed_info)
			c.timed_info = setInterval(c.getServerInfo,1000);
	};
	
	c.connection.onerror = function(error) {
		console.log('WebSocket Error ' + error)
		if (c.waiting_reconnect) {
			setTimeout(reconnect,1000);
		}
	};
	
	c.connection.onclose = function() {
		console.log('WebSocket Closed Connection')
		c.has_connection = false;
		var divp = document.getElementById(div_playfield_id);
		divp.style.opacity = 0
		var divo = document.getElementById(div_changeOrientation_id);
		divo.style.opacity = 0
		var div = document.getElementById(div_disconnected_id);
		div.style.display = "flex"
		c.waiting_reconnect = true;
		setTimeout(reconnect,1000);
	};
	
	c.connection.onmessage = function(e) {
		var msg = e.data;
		if (msg.charAt(0)=='{' && msg.slice(-1)=='}') {
			var obj = JSON.parse(msg);
			if (obj.speed) {
				c.gotInfoFromServer(obj);
			}
			else if (obj.hit) {
				c.gotHitFromServer(obj);
			}
			else if (obj.stopgame) {
				c.gotGameOverFromServer(obj);
			}
			else if (obj.loaded) {
				c.gotLoadedFromServer(obj);
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

ROBOTOY.COMM.getServerInfo = function() {
	var c = ROBOTOY.COMM;
	if (!c.waiting_reconnect && c.has_connection)
		c.connection.send('?')
}

ROBOTOY.COMM.stopServerInfo = function() {
	var c = ROBOTOY.COMM;
	if (c.timed_info)
		window.clearInterval(c.timed_info);
}

ROBOTOY.COMM.gotInfoFromServer = function(info) {
	if (info.stage=='INIT') {
		window.location = 'lobby.jsp';
	}
	else if (info.stage=='SUMMARY') {
		ROBOTOY.goToSummary();
	}
	else {
		ROBOTOY.COMM.wifi_level = Math.min(4,parseInt(info.wifi/20)) // [0-100] => [0-4]
		ROBOTOY.COMM.wifi = info.wifi;
		ROBOTOY.life = info.life;
		var container = ROBOTOY.VIEW.container;
		if (container) {
			container.markRectsDamaged();
			container.redraw();
		}
	}
}

ROBOTOY.COMM.gotHitFromServer = function(info) {
	if (info.hit.id==ROBOTOY.robotid) {
		// We got hit
		// Flick damage
		ROBOTOY.EXPLOSIONS.flickDamage();
		ROBOTOY.AUDIO.playDamageSound();
	}
	if (info.source.id==ROBOTOY.robotid) {
		// We did hit
		// Render explosion
		if (info.fatal) {
			ROBOTOY.EXPLOSIONS.animateBigExplosion();
			ROBOTOY.AUDIO.playBigExplosionSound();
		}
		else {
			ROBOTOY.EXPLOSIONS.animateExplosion();
			ROBOTOY.AUDIO.playExplosionSound();
		}
	}
}

ROBOTOY.COMM.gotGameOverFromServer = function(info) {
	ROBOTOY.goToSummary();
}

ROBOTOY.COMM.gotLoadedFromServer = function(info) {
	if (info.pending==0 && ROBOTOY.waiting) {
		ROBOTOY.finishedWaiting();
	}
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
	if (ROBOTOY.username==info.player.name) {
		var c = ROBOTOY.COMM;
		var prev_ping = c.ping;
		c.ping = info.updateping;
		if (!prev_ping || Math.abs(prev_ping-info.updateping)>10) {
			// For relevant PING change, refresh foreground layer
			var container = ROBOTOY.VIEW.container;
			if (container) {
				container.markRectsDamaged();
				container.redraw();
			}
		}		
	}
}

ROBOTOY.COMM.sendGreetings = function() {
	var c = ROBOTOY.COMM;
	c.connection.send('{"greetings":"'+ROBOTOY.session_id+'"}');
}
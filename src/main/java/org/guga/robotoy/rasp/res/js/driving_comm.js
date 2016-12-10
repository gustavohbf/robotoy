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
	
	timed_info : null,
	
	charging_display : null,
	
	charging_display_timestamp : null,
	
	charging_div_prev : null
};

ROBOTOY.COMM.connect_ws = function(url) {

	var c = ROBOTOY.COMM;
	
	c.connection = new WebSocket(url);
	
	var reconnect = function() {
		ROBOTOY.COMM.connect_ws(url);
	}
	
	c.connection.onopen = function() {
		if (c.waiting_reconnect) {
			var div = ROBOTOY.div_disconnected;
			if (div)
				div.style.display = "none"
			var divp = ROBOTOY.div_playfield;
			if (divp)
				divp.style.opacity = 1
			var divo = ROBOTOY.div_changeOrientation;
			if (divo)
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
		var divp = ROBOTOY.div_playfield;
		if (divp)
			divp.style.opacity = 0
		var divo = ROBOTOY.div_changeOrientation;
		if (divo)
			divo.style.opacity = 0
		var div = ROBOTOY.div_disconnected;
		if (div)
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
			else if (obj.charging) {
				c.gotCharging(obj);
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

ROBOTOY.COMM.gotCharging = function(info) {
	if (ROBOTOY.robotid==info.charging.id) {
		var div_hide = ROBOTOY.COMM.charging_div_prev;
		var div_show;
		if (info.depleted) {
			div_show = ROBOTOY.div_depleted;
		}
		else if (info.full) {
			div_show = ROBOTOY.div_charged;
		}
		else {
			div_show = ROBOTOY.div_charging;	
			if (ROBOTOY.span_charges_remaining)
				ROBOTOY.span_charges_remaining.text(info.remaining);
		}
		if (div_hide && !!(div_hide.offsetWidth || div_hide.offsetHeight || div_hide.getClientRects().length)) {
			div_hide.style.display = "none";
		}
		if (div_show) {
			if (!(div_show.offsetWidth || div_show.offsetHeight || div_show.getClientRects().length)) {
				div_show.style.display = "flex";
				div_show.style.zIndex = 1
			}
		}
		ROBOTOY.life = info.charging.life;
		var container = ROBOTOY.VIEW.container;
		if (container) {
			container.markRectsDamaged();
			container.redraw();
		}
		ROBOTOY.COMM.charging_div_prev = div_show;
		ROBOTOY.COMM.charging_display_timestamp = new Date().getTime();
		if (!ROBOTOY.COMM.charging_display) {
			ROBOTOY.COMM.charging_display = setInterval((function(){
				var timestamp = new Date().getTime();
				var delay = (timestamp - ROBOTOY.COMM.charging_display_timestamp);
				if (delay>1200) {
					var div_hide = ROBOTOY.COMM.charging_div_prev;
					if (div_hide && !!(div_hide.offsetWidth || div_hide.offsetHeight || div_hide.getClientRects().length))
						div_hide.style.display = "none";
					clearInterval(ROBOTOY.COMM.charging_display);
					ROBOTOY.COMM.charging_display = null;
				}				
			}),500);
		}
	}
}

ROBOTOY.COMM.sendGreetings = function() {
	var c = ROBOTOY.COMM;
	c.connection.send('{"greetings":"'+ROBOTOY.session_id+'"}');
}
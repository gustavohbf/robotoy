/*!
 RoboToy Keyboard Control
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used for keyboard events.
 *
 * Depends on:
 * driving_view.js
 * driving_comm.js
 * driving_audio.js
 * driving_control.js
 * laserbeam_XXXXX.js
 */

var ROBOTOY = ROBOTOY || {}

ROBOTOY.VIEW.afterBuild.push((function(){
	window.addEventListener( "keydown", ROBOTOY.doKeyDown, true )
	window.addEventListener( "keyup", ROBOTOY.doKeyUp, true )
}));	

ROBOTOY.doKeyDown = function(e) {
	var buttons_disabled = ROBOTOY.CONTROL.buttons_disabled;
	var c = ROBOTOY.COMM;
	var has_connection = c.has_connection;
	var loading = ROBOTOY.loading;
	var waiting = ROBOTOY.waiting;
	switch( e.keyCode ) {
		case 38: // up
			if (!buttons_disabled && has_connection && !loading && !waiting)
			c.connection.send('f')
			break;
		case 40: // down
			if (!buttons_disabled && has_connection && !loading && !waiting)
			c.connection.send('b')
			break;
		case 37: // left
			if (!buttons_disabled && has_connection && !loading && !waiting)
			c.connection.send('l')
			break;
		case 39: // right
			if (!buttons_disabled && has_connection && !loading && !waiting)
			c.connection.send('r')
			break;
		case 112: // F1
			if (!buttons_disabled && has_connection && !loading && !waiting)
			c.connection.send('1')
			break;
		case 113: // F2
			if (!buttons_disabled && has_connection && !loading && !waiting)
			c.connection.send('2')
			break;
		case 114: // F3
			if (!buttons_disabled && has_connection && !loading && !waiting)
			c.connection.send('3')
			break;
		case 27: // ESC
			break;
		case 32: // Space
			if (!buttons_disabled && has_connection && !loading && !waiting) {
				c.connection.send(' ') 
				ROBOTOY.LASER.animateBeam();
				ROBOTOY.AUDIO.playLaserSound();
			}
			break;
	}
}

ROBOTOY.doKeyUp = function(e) {
	var c = ROBOTOY.COMM;
	switch( e.keyCode ) {
		case 38: // up
		case 40: // down
		case 37: // left
		case 39: // right
			if (c.has_connection)
			c.connection.send('s')
			break;
	}
}

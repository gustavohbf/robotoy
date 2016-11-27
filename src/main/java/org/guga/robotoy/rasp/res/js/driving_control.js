/*!
 RoboToy Driving Control
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used for controlling the robot
 *
 * Depends on:
 * driving_comm.js
 * driving_view.js
 * driving_audio.js
 * laserbeam_XXXXX.js
 */

var ROBOTOY = ROBOTOY || {}

ROBOTOY.COMM = ROBOTOY.COMM || {} 

ROBOTOY.VIEW = ROBOTOY.VIEW || {}

ROBOTOY.AUDIO = ROBOTOY.AUDIO || {}

ROBOTOY.EXPLOSIONS = ROBOTOY.EXPLOSIONS || {}

ROBOTOY.LASER = ROBOTOY.LASER || {} 

ROBOTOY.CONTROL = {

	btnSize : 80,
	
	btn_clicked : null,
	
	buttons_disabled : true,
	
	engines_on : false, // only used with tilt control mode
	
	ControlButton : function(x, y, w, h, imgsrc) {
	    this.x = x;
	    this.y = y;
	    this.w = w;
	    this.h = h;
	    this.img = new Image();
	    this.img.src = imgsrc;
	    this.draw = function(ctx){
	    	ctx.drawImage(this.img,this.x,this.y,this.w,this.h);
	    };
		this.isInside = function(x,y) {
			return x>=this.x && x<=this.x+this.w && y>=this.y && y<=this.y+this.h;
		}
	},
	
	btnUp : null,
	btnDown : null,
	btnLeft : null,
	btnRight : null,
	btnFire : null,
	btnEngine : null // only used with tilt control mode
};

(function() {
	var c = ROBOTOY.CONTROL;
	c.btnUp = new c.ControlButton(0,0,c.btnSize,c.btnSize,'up.png');
	c.btnDown = new c.ControlButton(0,0,c.btnSize,c.btnSize,'down.png');
	c.btnLeft = new c.ControlButton(0,0,c.btnSize,c.btnSize,'left.png');
	c.btnRight = new c.ControlButton(0,0,c.btnSize,c.btnSize,'right.png');
	c.btnFire = new c.ControlButton(0,0,c.btnSize,c.btnSize,'fire.png');
	c.btnEngine = new c.ControlButton(0,0,c.btnSize,c.btnSize,'engine.png');
})();


ROBOTOY.CONTROL.checkButtons = function(clickedX,clickedY) {
	var c = ROBOTOY.CONTROL;
	var has_connection = ROBOTOY.COMM.has_connection;
	var loading = ROBOTOY.loading;
	var waiting = ROBOTOY.waiting;
	if (c.buttons_disabled || !has_connection || loading || waiting)
		return;
	if (ROBOTOY.tiltcontrols) {
		if (c.btnEngine.isInside(clickedX,clickedY)) {
			c.engines_on = true;
			c.btn_clicked = c.btnEngine;
		}
	}
	else {
	    if (c.btnUp.isInside(clickedX,clickedY)) {
			ROBOTOY.COMM.connection.send('f');
			c.btn_clicked = c.btnUp;
		}
	    else if (c.btnDown.isInside(clickedX,clickedY)) {
			ROBOTOY.COMM.connection.send('b');
			c.btn_clicked = c.btnDown;
		}
	    else if (c.btnLeft.isInside(clickedX,clickedY)) {
			ROBOTOY.COMM.connection.send('l');
			c.btn_clicked = c.btnLeft;
		}
	    else if (c.btnRight.isInside(clickedX,clickedY)) {
			ROBOTOY.COMM.connection.send('r');
			c.btn_clicked = c.btnRight;
		}
	}
	if (c.btnFire.isInside(clickedX,clickedY)) {
		ROBOTOY.COMM.connection.send(' ');
		ROBOTOY.LASER.animateBeam();
		ROBOTOY.AUDIO.playLaserSound();
		c.btn_clicked = c.btnFire;
	}
};

ROBOTOY.CONTROL.checkRelease = function() {
	var c = ROBOTOY.CONTROL;
	if (c.btn_clicked==c.btnUp 
	|| c.btn_clicked==c.btnDown 
	|| c.btn_clicked==c.btnLeft 
	|| c.btn_clicked==c.btnRight) {
		if (ROBOTOY.COMM.has_connection)
			ROBOTOY.COMM.connection.send('s');
	}
	c.btn_clicked = null;
	c.engines_on = false;
}

ROBOTOY.CONTROL.repositionButtons = function() {
	var c = ROBOTOY.CONTROL;
	var canvas = ROBOTOY.VIEW.canvas;
	if (!canvas)
		return;
	var width = Math.min(window.innerWidth, canvas.width);
	var height = Math.min(window.innerHeight, canvas.height);
	if (ROBOTOY.tiltcontrols) {
		c.btnEngine.y = height - c.btnSize - 5;
		c.btnEngine.x = width - c.btnSize - 5;
	}
	else {
		c.btnDown.x = c.btnUp.x = width - c.btnSize*2 - 5;
		c.btnDown.y = height - c.btnSize - 5;
		c.btnUp.y = height - c.btnSize * 2 - 5;
		c.btnLeft.y = c.btnRight.y = height - c.btnSize * 3 / 2 - 5;
		c.btnLeft.x = width - c.btnSize*17/6 - 5;
		c.btnRight.x = width - c.btnSize * 7 / 6 - 5;
	}
	c.btnFire.y = height - c.btnSize - 5;
	c.btnFire.x = 5;
}


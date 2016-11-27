/*!
 RoboToy View Rendering
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used in rendering play view
 * Depends on:
 * driving_control.js
 */
 
var ROBOTOY = ROBOTOY || {}

ROBOTOY.COMM = ROBOTOY.COMM || {}

ROBOTOY.VIEW = {
	canvas : {},
	
	playfield_div : null,
	
	container : null,
	
	ctx : {},
	
	layer_controls : null,
	
	life_img : new Image(),
	
	wifiLevels : new Array(),
	
	afterBuild : new Array(),
	
	// Optional rotation of foreground layer in radians
	rotate_angle : 0
};

ROBOTOY.VIEW.init = function(canvas_id,div_id) {
	var v = ROBOTOY.VIEW;
	v.canvas = document.getElementById(canvas_id);
	v.playfield_div = document.getElementById(div_id);
	v.container = new CanvasLayers.Container(v.canvas, true);
	v.ctx = v.canvas.getContext("2d");
	v.ctx.fillStyle = "#FF0000";
	v.life_img.src = 'heart.png';

	for (var i=0;i<5;i++) {
		v.wifiLevels[i] = new Image();
		v.wifiLevels[i].src = 'wifi'+i+'.png'
	}

	// Background layer
	v.container.onRender = function(layer, rect, context) {
		context.clearRect(0,0,layer.getWidth(),layer.getHeight());
	}

	// Foreground layer (with buttons and mini-displays)
	v.layer_controls = new CanvasLayers.Layer(0, 0, v.container.getWidth(), v.container.getHeight());
	v.container.getChildren().add(v.layer_controls);
	v.layer_controls.onRender = function(layer,rect,ctx) {
		if (v.rotate_angle) {
			ctx.save();
			ctx.translate(v.container.getWidth()/2,v.container.getHeight()/2);
			ctx.rotate(v.rotate_angle);
			ctx.translate(-v.container.getWidth()/2,-v.container.getHeight()/2);
		}
		v.drawButtons(ctx)
		v.drawWifiLevel(ctx)
		v.drawPingLevel(ctx)
		v.drawLife(ctx);
		v.drawColor(ctx);
		if (v.rotate_angle) {
			ctx.restore();
		}
	}
	
	if (v.afterBuild) {
		v.afterBuild.forEach(function(callback){callback.call()});
	}
}

ROBOTOY.VIEW.clearCanvas = function(){
	var v = ROBOTOY.VIEW;
	v.ctx.clearRect(0,0,v.canvas.width,v.canvas.height)
}

ROBOTOY.VIEW.resizeCanvas = function() {
	var v = ROBOTOY.VIEW;
	if(window.innerWidth > window.innerHeight){
		// landscape mode
		var div = v.playfield_div;
		var canvas = v.canvas;
		var container = v.container;
		if (div) {
			if (div.style) {
				div.style.width = (Math.min(640,window.innerWidth-20))+"px";
				div.style.height = (Math.min(480,window.innerHeight-20))+"px";
			}
			canvas.width = div.offsetWidth-10
			canvas.height = div.offsetHeight-10
		}
		ROBOTOY.CONTROL.repositionButtons();
		if (container) {
			container.resize(canvas.width,canvas.height)
		}
		if (v.layer_controls)
			v.layer_controls.show();
		if (container) {
			container.markRectsDamaged(container.getRect())
			container.redraw();
		}
	}
	else {
		// portrait mode
		if (v.layer_controls)
			v.layer_controls.hide();
		v.clearCanvas();
		ROBOTOY.CONTROL.buttons_disabled = true;
	}
}

ROBOTOY.VIEW.drawWifiLevel = function(ctx) {
	var wifi_level = ROBOTOY.COMM.wifi_level;
	if (!wifi_level)
		return;
	ctx.drawImage(ROBOTOY.VIEW.wifiLevels[wifi_level],2,2,32,32);
	ctx.fillStyle = "#FFFFFF";
	ctx.fillRect(34,2,50,32);
	ctx.font = "24px Arial";
	ctx.strokeText(ROBOTOY.COMM.wifi+'%',36,25);
}

ROBOTOY.VIEW.drawPingLevel = function(ctx) {
	var ping_level = ROBOTOY.COMM.ping;
	if (!ping_level)
		return;
	var txt = ping_level.toString();
	if (!txt)
		return;
	var v = ROBOTOY.VIEW;
	var canvas = v.canvas;
	var width = Math.min(window.innerWidth, canvas.width);
	var last_life_x = 100+ROBOTOY.max_life*36+32;
	var offset_color_indicator = 40;
	ctx.font = "24px Arial";
	var text_x = last_life_x + offset_color_indicator;
	var min_text_length = ctx.measureText('0000').width;
	if (text_x + min_text_length > width)
		return; // clipping		
	ctx.fillStyle = "#FFFFFF";
	ctx.fillRect(text_x,2,min_text_length,32);
	ctx.strokeText(txt,text_x,25);
}

ROBOTOY.VIEW.drawButtons = function(ctx) {
	var c = ROBOTOY.CONTROL;
	if (ROBOTOY.tiltcontrols) {
		c.btnEngine.draw(ctx);
	}
	else {
		c.btnUp.draw(ctx);
		c.btnDown.draw(ctx);
		c.btnLeft.draw(ctx);
		c.btnRight.draw(ctx);
	}
	c.btnFire.draw(ctx);
	c.buttons_disabled = false; // if we draw it, we can respond to it
};

ROBOTOY.VIEW.drawLife = function(ctx) {
	var life = ROBOTOY.life;
	if (!life || life<=0)
		return;
	for (var i=0;i<life;i++) {
		ctx.drawImage(ROBOTOY.VIEW.life_img,100+i*36,2,32,32);
	}
}

ROBOTOY.VIEW.drawColor = function(ctx) {
	var robot_color = ROBOTOY.robot_color;
	if (!robot_color || robot_color=="")
		return;
	var v = ROBOTOY.VIEW;
	var canvas = v.canvas;
	var width = Math.min(window.innerWidth, canvas.width);
	var last_life_x = 100+ROBOTOY.max_life*36+32;
	var radius = 16;
	if (last_life_x + radius*2 > width)
		return; // avoid image overlapping
	var center_x = last_life_x + radius;
	ctx.beginPath();
	ctx.arc(center_x , radius+2, radius, 0, 2*Math.PI, false);
	ctx.fillStyle = robot_color;
	ctx.fill();
	ctx.lineWidth = 2;
	ctx.strokeStyle = '#e0e0e0';
	ctx.stroke();
}

<%@ page contentType="text/html;charset=iso-8859-1" 
	language="java"%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@ taglib uri="robotoy.tld" prefix="app" %><app:access/><app:controlmode/><!DOCTYPE html>
<html>
<head>
	<title>RoboToy Driving</title>
	<link rel="manifest" href="manifest.json">
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta name="copyright" content="© 2016 https://github.com/gustavohbf/robotoy" />
	<link rel="stylesheet" type="text/css" href="waiting.css" />
	<link rel="stylesheet" type="text/css" href="disconnected.css" />
	<link rel="stylesheet" type="text/css" href="loading.css" />
	<link rel="stylesheet" type="text/css" href="charging.css" />
	<link rel="stylesheet" type="text/css" href="depleted.css" />
	<link rel="stylesheet" type="text/css" href="driving_view.css" />
	<link rel="stylesheet" type="text/css" href="animateflicker.css" />
	<script src="jquery-1.12.0.min.js"></script>
	<app:errors/>
<script src="canvaslayers.js"></script>
</head>
<body onresize="ROBOTOY.VIEW.resizeCanvas()" unselectable="on">

<div id="playfield">
<img id="background" class='img' src="http://<%=request.getLocalAddr()%>:<app:cameraport/>/stream/video.mjpeg" alt="" />
<canvas id="myCanvas" width="640" height="480">
Your browser does not support the HTML5 canvas tag.
</canvas>
<div id="changeOrientation" class="overlay">
<h1>Please rotate your device!</h1>
</div>
</div>

<div id="waiting" style="position:absolute;display:none" class="waiting"><h1>Waiting other players...</h1></div>
<div id="disconnected" style="position:absolute;display:none" class="disconnected"><h1>Disconnected! Trying to reconnect...</h1></div>
<div id="loading" style="position:absolute;display:flex" class="loading"><h1>Starting engine... Please wait.</h1></div>
<div id="charging" style="position:absolute;display:none" class="charging animate-flicker"><h1>Recharging...<BR>
<small>Charges remaining: <span id="chargesremaining"></span></small></h1></div>
<div id="charged" style="position:absolute;display:none" class="charging animate-flicker"><h1>Fully charged!</h1></div>
<div id="depleted" style="position:absolute;display:none" class="depleted animate-flicker"><h1>Charge depleted!</h1></div>

<form>
<input type="hidden" name="username" id="username" value="<app:username/>">
<input type="hidden" name="robotid" id="robotid" value="<app:robotid/>">
</form>

<script src="driving_comm.js"></script>
<script src="driving_control.js"></script>
<script src="driving_view.js"></script>
<script src="driving_audio.js"></script>
<script src="vector.js"></script>
<script src="laserbeam_straight.js"></script>
<script src="driving_keyboard.js"></script>
<script src="driving_mouse.js"></script>
<script src="driving_touch.js"></script>
<script src="explosion.js"></script>
<script src="sprites.js"></script>
<script src="howler.js"></script>
<script src="device_motion.js"></script>
<script>

var ROBOTOY = ROBOTOY || {}

ROBOTOY.robotid = document.getElementById("robotid").value;
ROBOTOY.life = -1;
ROBOTOY.pending_on_start = <app:playerscount pending="true"/>;

ROBOTOY.loading = true;
ROBOTOY.waiting = true;
ROBOTOY.entering_summary = false;
<c:if test="${controlmode eq 'tilt'}">
ROBOTOY.tiltcontrols = true;
</c:if>
<c:if test="${not empty param.debug}">
ROBOTOY.debug = true;
</c:if>

ROBOTOY.robot_color = "<app:color/>";
ROBOTOY.session_id = "<app:sessionid/>";
ROBOTOY.max_life = <app:maxlife/>;
ROBOTOY.username = document.getElementById("username").value;

ROBOTOY.div_playfield = $( "#playfield" ).get(0);
ROBOTOY.div_disconnected = $( "#disconnected" ).get(0);
ROBOTOY.div_changeOrientation = $( "#changeOrientation" ).get(0);
ROBOTOY.div_charging = $( "#charging" ).get(0);
ROBOTOY.div_charged = $( "#charged" ).get(0);
ROBOTOY.div_depleted = $( "#depleted" ).get(0);
ROBOTOY.span_charges_remaining = $("#chargesremaining");

window.addEventListener("orientationchange", function() {
if (window.orientation == 90 || window.orientation == -90) {
// landscape mode
	var v = ROBOTOY.VIEW;
	var div = v.playfield_div;
	var container = v.container;
	var canvas = v.canvas;
	div.style.width = (Math.min(640,window.innerWidth-20))+"px";
	div.style.height = (Math.min(480,window.innerHeight-20))+"px";
	canvas.width = div.offsetWidth-10
	canvas.height = div.offsetHeight-10
	ROBOTOY.CONTROL.repositionButtons();
	container.resize(canvas.width,canvas.height)
	v.layer_controls.show();
	container.markRectsDamaged(container.getRect())
	container.redraw();
} else {
// portrait mode
	var v = ROBOTOY.VIEW;
	v.layer_controls.hide();
	v.clearCanvas();
	ROBOTOY.CONTROL.buttons_disabled = true;
}
});

ROBOTOY.goToSummary = function() {
	if (ROBOTOY.entering_summary)
		return;
	ROBOTOY.entering_summary = true;
	// Give some time for hearing final explosion (if any)
	setTimeout((function(){window.location = 'summary.jsp';}),2000);
}

ROBOTOY.checkFinishedLoading = function() {
	var e = ROBOTOY.EXPLOSIONS;
	if (e.sprites_explosion_done 
			&& e.sprites_bigexplosion_done 
			&& ROBOTOY.AUDIO.audio_done)
		ROBOTOY.finishedLoading();
}

ROBOTOY.finishedLoading = function() {
	ROBOTOY.loading = false;
	var div = document.getElementById("loading")
	if (div) {
		div.style.display = "none"
		if (ROBOTOY.pending_on_start<=1 || !ROBOTOY.COMM.has_connection) {
			ROBOTOY.finishedWaiting();
		}
		else {
			div = document.getElementById("waiting")
			if (div) {
				div.style.display = "flex"
			}			
		}
	}
	var send_loaded_msg = function() {
    	var username = document.getElementById("username").value;
		ROBOTOY.COMM.connection.send('{"loaded":{"name":"'+username+'"}}');
	}
	if (ROBOTOY.COMM.has_connection) {
		send_loaded_msg();
	}
	else {
		setTimeout(send_loaded_msg,1000);
	}
}

ROBOTOY.finishedWaiting = function() {
	ROBOTOY.waiting = false;
	var div = document.getElementById("waiting")
	if (div) {
		div.style.display = "none"
	}
	div = document.getElementById("playfield")
	if (div) {
		div.style.display = "inline-block"
	}
	ROBOTOY.VIEW.resizeCanvas();
}

<c:if test="${controlmode eq 'tilt'}">
ROBOTOY.motionControl = function() {
	
	var loading = ROBOTOY.loading;
	var waiting = ROBOTOY.waiting;
	if (loading || waiting)
		return;

	var tilt_angle; // degrees
	var pitch_angle; // degrees
	var m = ROBOTOY.MOTION;

	// 0 = normal
	// 90 = counter-clockwise
	// -90 = clockwise
	var currentScreenOrientation = window.orientation || 0; 
	if (currentScreenOrientation==90) {
		tilt_angle = m.beta;
		pitch_angle = m.gamma;
	}
	else if (currentScreenOrientation==-90) {
		tilt_angle = -m.beta;
		pitch_angle = m.gamma;
	}
	else {
		tilt_angle = m.gamma;
		pitch_angle = m.beta;
	}
	if (tilt_angle===null)
		return;

	// Correct image (level)
	var img = $("#background");
	if (img) {		
		var param = "scale(1.4,1.4) rotate("+(-tilt_angle)+"deg)";
		img.css("-ms-transform",param) // IE 9
		img.css("-webkit-transform",param) // Chrome, Safari, Opera
		img.css("-moz-transform",param) // browsers gecko
		img.css("-o-transform",param) // Opera
		img.css("transform",param) // Others	
	}
	
	var c = ROBOTOY.CONTROL;
	
	var engines_on = !c.buttons_disabled && c.btnEngine && c.btn_clicked === c.btnEngine;

	if (engines_on) {
		// Calculate controls deltas
		if (!ROBOTOY.level_tilt)
			ROBOTOY.level_tilt = tilt_angle;
		if (!ROBOTOY.level_pitch)
			ROBOTOY.level_pitch = pitch_angle;
		var tilt_delta = tilt_angle - ROBOTOY.level_tilt;
		var pitch_delta = pitch_angle - ROBOTOY.level_pitch;
		var epsilon = 1.0; // degrees
		var stopped = (Math.abs(tilt_delta) < epsilon) && (Math.abs(pitch_delta) < epsilon);
		var left_factor = 0.0;
		var right_factor = 0.0;
		if (!stopped) {
			var max = 15.0; // degrees
			var scaled_tilt_delta = Math.min(1.0,Math.max(-1.0,tilt_delta/max))		// -1.0 (left) ... 1.0 (right)
			var scaled_pitch_delta = Math.min(1.0,Math.max(-1.0,pitch_delta/max))	// -1.0 (backward) ... 1.0 (forward)
			left_factor = scaled_pitch_delta + scaled_tilt_delta;
			right_factor = scaled_pitch_delta - scaled_tilt_delta;
			var g = Math.max(Math.abs(left_factor),Math.abs(right_factor));
			if (g>1.0) {
				left_factor /= g;
				right_factor /= g;
			}
		}
		// Send controls
		if (ROBOTOY.COMM.has_connection) {
			if (stopped || (Math.abs(left_factor)<0.1 && Math.abs(right_factor)<0.1)) {
				ROBOTOY.COMM.connection.send('s'); // stop motors
			}
			else {
				ROBOTOY.COMM.connection.send('{"movement":{"left":'+left_factor+',"right":'+right_factor+'}}');
<c:if test="${not empty param.debug}">
				ROBOTOY.left_factor = left_factor;
				ROBOTOY.right_factor = right_factor;
				ROBOTOY.tilt_delta = tilt_delta;
				ROBOTOY.pitch_delta = pitch_delta;
				ROBOTOY.scaled_tilt_delta = scaled_tilt_delta;
				ROBOTOY.scaled_pitch_delta = scaled_pitch_delta;
</c:if>
			}
		}
		ROBOTOY.engines_was_on = true;
	}
	else {
		// while engines are OFF, keep track of device orientation in order to provide a new inertial reference
		ROBOTOY.level_tilt = tilt_angle;
		ROBOTOY.level_pitch = pitch_angle;
		if (ROBOTOY.engines_was_on) {
			// Make sure motors are stopped
			ROBOTOY.engines_was_on = false;
			if (ROBOTOY.COMM.has_connection) {
				ROBOTOY.COMM.connection.send('s'); // stop motors
			}
<c:if test="${not empty param.debug}">
			ROBOTOY.left_factor = 0;
			ROBOTOY.right_factor = 0;
</c:if>
		}
	}
}
</c:if>

<c:if test="${not empty param.debug}">
ROBOTOY.fillDebugInfo = function() {
	var container = ROBOTOY.VIEW.container;
	var debug_msg = "";
	if (ROBOTOY.MOTION) {
		var m = ROBOTOY.MOTION;
		debug_msg += "alpha: " + parseFloat(Math.round(m.alpha * 10000) / 10000).toFixed(4)+"\n";
		debug_msg += "beta: " + parseFloat(Math.round(m.beta * 10000) / 10000).toFixed(4)+"\n";
		debug_msg += "gamma: " + parseFloat(Math.round(m.gamma * 10000) / 10000).toFixed(4)+"\n";
	}
	debug_msg += "left factor: " + parseFloat(Math.round(ROBOTOY.left_factor * 10000) / 10000).toFixed(4)+"\n";
	debug_msg += "right factor: " + parseFloat(Math.round(ROBOTOY.right_factor * 10000) / 10000).toFixed(4)+"\n";
	debug_msg += "tilt delta: " + parseFloat(Math.round(ROBOTOY.tilt_delta * 10000) / 10000).toFixed(4);
	debug_msg += " scaled: " + parseFloat(Math.round(ROBOTOY.scaled_tilt_delta * 10000) / 10000).toFixed(4)+"\n";
	debug_msg += "pitch delta: " + parseFloat(Math.round(ROBOTOY.pitch_delta * 10000) / 10000).toFixed(4);
	debug_msg += " scaled: " + parseFloat(Math.round(ROBOTOY.scaled_pitch_delta * 10000) / 10000).toFixed(4)+"\n";
	ROBOTOY.debug_msg = debug_msg;
	container.markRectsDamaged();
	container.redraw();		
};
</c:if>

ROBOTOY.init = function() {
	ROBOTOY.COMM.connect_ws('ws://<%=request.getLocalAddr()%>:<%=request.getLocalPort()%>/ws/player/'+encodeURI(ROBOTOY.username),
			'disconnected','playfield','changeOrientation');
	ROBOTOY.VIEW.init("myCanvas","playfield");
	var v = ROBOTOY.VIEW;
	var ctx = v.ctx;
	var div = v.playfield_div;
	var container = v.container;
	if(div.offsetWidth > div.offsetHeight){
		ctx.canvas.width = div.offsetWidth-10;
		ctx.canvas.height = div.offsetHeight-10;
		ROBOTOY.CONTROL.repositionButtons();
		container.markRectsDamaged();
		container.redraw();
	}
	setTimeout(ROBOTOY.EXPLOSIONS.loadExplosionSprites,0);
	setTimeout(ROBOTOY.AUDIO.loadAudioFiles,0);
<c:if test="${controlmode eq 'tilt'}">
	ROBOTOY.MOTION.init(ROBOTOY.motionControl);	
</c:if>
	if (!ROBOTOY.waiting) {
		// if got message while it was still starting, dismiss 'waiting' screen
		finishedWaiting();
	}
<c:if test="${not empty param.debug}">
	var debug_layer = new CanvasLayers.Layer(0, 0, container.getWidth(), container.getHeight());
	container.getChildren().add(debug_layer);
	debug_layer.onRender = function(layer,rect,ctx) {
		try {
			if (ROBOTOY.debug_msg) {
				ctx.font = "14px Arial";
				ctx.fillStyle = "#FFFFFF";
				var lines = ROBOTOY.debug_msg.split('\n');
				var lineheight = 15;
				for (var i = 0; i<lines.length; i++)
					ctx.fillText(lines[i],5,100 + (i*lineheight));
			}
		}
		catch (error) { 
			console.log(error)
		}
	};
	setInterval(ROBOTOY.fillDebugInfo,200);
</c:if>
};

ROBOTOY.init();

</script>
<script src="manup.js"></script>
</body>
</html>
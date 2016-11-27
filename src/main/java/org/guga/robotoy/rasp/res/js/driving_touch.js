/*!
 RoboToy Touch Control
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used for touch events.
 *
 * Depends on:
 * driving_view.js
 * driving_control.js
 */

var ROBOTOY = ROBOTOY || {}

ROBOTOY.VIEW.afterBuild.push((function(){
	var canvas = ROBOTOY.VIEW.canvas; 
	canvas.addEventListener( "touchstart", ROBOTOY.doTouchDown, true )
	canvas.addEventListener( "touchend", ROBOTOY.CONTROL.checkRelease, true )
}));
	

ROBOTOY.doTouchDown = function(e) {
	e.preventDefault();
	var touches = e.changedTouches;
	for (var i=0;i<touches.length;i++) {
		var x = touches[i].pageX
		var y = touches[i].pageY;
	    ROBOTOY.CONTROL.checkButtons(x,y);
	}
}

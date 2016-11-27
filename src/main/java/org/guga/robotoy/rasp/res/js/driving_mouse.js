/*!
 RoboToy Mouse Control
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used for mouse events.
 *
 * Depends on:
 * driving_view.js
 * driving_control.js
 */
 
ROBOTOY.VIEW.afterBuild.push((function(){
	var canvas = ROBOTOY.VIEW.canvas; 
	canvas.addEventListener( "mousedown", ROBOTOY.doMouseDown, true )
	canvas.addEventListener( "mouseup", ROBOTOY.CONTROL.checkRelease, true )
}));

ROBOTOY.doMouseDown = function(e) {
    var clickedX = e.offsetX;
    var clickedY = e.offsetY;
    ROBOTOY.CONTROL.checkButtons(clickedX,clickedY);
}


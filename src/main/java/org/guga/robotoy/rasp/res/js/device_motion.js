/*!
 RoboToy Device Motion Control
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used with motion detection devices such as: 
 * gyroscope, accelerometer and compass.
 */

var ROBOTOY = ROBOTOY || {}
 
ROBOTOY.MOTION = {

	// alpha angle: the rotation of the device around the Z axis (perpendicular to the ground).
	alpha : null,
	
	// beta angle: the rotation of the device around the X axis (West to East). In degree in the range [-180,180]
	beta : null,
	
	// gamma angle: the rotation of the device around the Y axis (South to North). In degree in the range [-90,90]
	gamma : null,
	
	// alpha angle change rate (in degrees per seconds)
	alpha_rate : null,
	
	// beta angle change rate (in degrees per seconds)
	beta_rate : null,
	
	// gamma angle change rate (in degrees per seconds)
	gamma_rate : null,
	
	// acceleration in X axis (in m/s2)
	accel_x : null,
	
	// acceleration in Y axis (in m/s2)
	accel_y : null,
	
	// acceleration in Z axis (in m/s2)
	accel_z : null,

	// acceleration in X axis without gravity (in m/s2)
	accel_x_ng : null,
	
	// acceleration in Y axis without gravity (in m/s2)
	accel_y_ng : null,
	
	// acceleration in Z axis without gravity (in m/s2)
	accel_z_ng : null,
	
	onChange : function() { },
	
	initialized : false
};

ROBOTOY.MOTION.init = function(onChangeCallback) {
	if (ROBOTOY.MOTION.initialized)
		return;
	ROBOTOY.MOTION.initialized = true;
	ROBOTOY.MOTION.onChange = onChangeCallback || (function(){});
	if (window.DeviceOrientationEvent) {
		var handleOrientation = function(e) {
			var m = ROBOTOY.MOTION;
			m.alpha = e.alpha;
			m.beta = e.beta;
			m.gamma = e.gamma;
			m.onChange();
		}
		window.addEventListener('deviceorientation', handleOrientation, true);
	}
	if (window.DeviceMotionEvent) {
		var handleMotion = function(e) {
			var m = ROBOTOY.MOTION;
			if (e.rotationRate) {
				m.alpha_rate = e.rotationRate.alpha;
				m.beta_rate = e.rotationRate.beta;
				m.gamma_rate = e.rotationRate.gamma;
			}
			if (e.accelerationIncludingGravity) {
				m.accel_x = e.accelerationIncludingGravity.x;
				m.accel_y = e.accelerationIncludingGravity.y;
				m.accel_z = e.accelerationIncludingGravity.z;
			}
			if (e.acceleration) {
				m.accel_x_ng = e.acceleration.x;
				m.accel_y_ng = e.acceleration.y;
				m.accel_z_ng = e.acceleration.z;
			}
			m.onChange();
		}
		window.addEventListener("devicemotion", handleMotion, true);
	}
}

/**
 * Check if orientation is currently implemented.
 * Warning: this method may return 'true' even though you
 * lack such a device (e.g.: an gyroscope).
 * See 'isOrientationSupported'.
 */
ROBOTOY.MOTION.isOrientationImplemented = function() {
	return (window.DeviceOrientationEvent) ? true : false;
}

/**
 * Check if orientation is currently supported by this device.
 * It needs some time since last previous call to 'init' in
 * order to give accurate result.
 */
ROBOTOY.MOTION.isOrientationSupported = function() {
	var m = ROBOTOY.MOTION;
	return (m.alpha===null && m.beta===null && m.gamma===null) ? false : true;
}

/**
 * Check if motion detection is currently implemented.
 * Warning: this method may return 'true' even though you
 * lack such a device (e.g.: an accelerometer).
 * See 'isMotionSupported'.
 */
ROBOTOY.MOTION.isMotionImplemented = function() {
	return (window.DeviceMotionEvent) ? true : false;
}

/**
 * Check if motion detection is currently supported by this device.
 * It needs some time since last previous call to 'init' in
 * order to give accurate result.
 */
ROBOTOY.MOTION.isMotionSupported = function() {
	var m = ROBOTOY.MOTION;
	return (m.accel_x===null && m.accel_y===null && m.accel_z===null
		&& m.accel_x_ng===null && m.accel_y_ng===null && m.accel_z_ng===null) ? false : true;
}
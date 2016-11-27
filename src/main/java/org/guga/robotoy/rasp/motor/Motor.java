/*******************************************************************************
 * Copyright 2016 See https://github.com/gustavohbf/robotoy/blob/master/AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.guga.robotoy.rasp.motor;

/**
 * Abstract interface for controlling robot's motors.<BR>
 * <BR>
 * There are currently two implementations:<BR>
 * {@link ArduinoController ArduinoController} - using serial communication
 * to Arduino, which in turn will use some other means of control (e.g. Adafruit Motor Shield)<BR>
 * {@link MotorShield MotorShield} - using Adafruit Motor Shield
 * directly wired to Raspberry's GPIO<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface Motor {

	/**
	 * Robot should move forward with a given speed factor
	 * @param speedFactor Factor between 0.0 (no speed) and 1.0 (full speed)
	 */
	public void moveForward(double speedFactor);
	
	/**
	 * Robot should move backwards with a given speed factor
	 * @param speedFactor Factor between 0.0 (no speed) and 1.0 (full speed)
	 */
	public void moveBackward(double speedFactor);
	
	/**
	 * Robot should turn left with a given speed factor
	 * @param speedFactor Factor between 0.0 (no speed) and 1.0 (full speed)
	 */
	public void turnLeft(double speedFactor);

	/**
	 * Robot should turn right with a given speed factor
	 * @param speedFactor Factor between 0.0 (no speed) and 1.0 (full speed)
	 */
	public void turnRight(double speedFactor);
	
	/**
	 * Robot should stop any movement.
	 */
	public void stop();

	/**
	 * Change the current robot speed for all running motors. Has no effect if
	 * the robot is not moving.
	 * @param speedFactor Factor between 0.0 (no speed) and 1.0 (full speed)
	 */
	public void setSpeed(double speedFactor);
	
	/**
	 * Tells if robot is supposed to be moving.
	 */
	public boolean isMoving();
	
	/**
	 * Tells the robot to spin left motors and right motors according to
	 * the given factors.
	 * @param leftFactor Value between -1.0 and +1.0 for the left side motors. Negative values tells to spin backwards.
	 * Positive values tells to spin forwards. The absolute value tells the speed (0.0 = no speed,
	 * 1.0 = full speed).
	 * @param rightFactor Value between -1.0 and +1.0 for the right side motors. Negative values tells to spin backwards.
	 * Positive values tells to spin forwards. The absolute value tells the speed (0.0 = no speed,
	 * 1.0 = full speed).
	 */
	public void setMovement(double leftFactor,double rightFactor);
}

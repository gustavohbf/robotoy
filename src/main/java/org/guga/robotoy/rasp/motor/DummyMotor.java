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
 * Dummy implementation of Motor that does nothing. For testing purposes.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DummyMotor implements Motor {

	@Override
	public void moveForward(double speedFactor) {
	}

	@Override
	public void moveBackward(double speedFactor) {
	}

	@Override
	public void turnLeft(double speedFactor) {
	}

	@Override
	public void turnRight(double speedFactor) {
	}

	@Override
	public void stop() {
	}

	@Override
	public void setSpeed(double speedFactor) {
	}

	@Override
	public boolean isMoving() {
		return false;
	}

	@Override
	public void setMovement(double leftFactor, double rightFactor) {
	}

}

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
package org.guga.robotoy.rasp.game;

/**
 * Enumerates different game control modes.
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum GameControlMode {
	
	/**
	 * Move robot through buttons on screen
	 */
	BUTTONS("buttons"),
	
	/**
	 * Move robot tilting device
	 */
	TILT("tilt");
	
	private final String mode;
	
	GameControlMode(String mode) {
		this.mode = mode;
	}
	
	public String getMode() {
		return mode;
	}
	
	public static GameControlMode parse(String mode) {
		if (mode==null)
			return BUTTONS;
		for (GameControlMode opt:values()) {
			if (mode.equalsIgnoreCase(opt.getMode()))
				return opt;
		}
		return BUTTONS;		
	}
}

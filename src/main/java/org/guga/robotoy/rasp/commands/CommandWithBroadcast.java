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
package org.guga.robotoy.rasp.commands;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;

/**
 * Extension of a command that might be further broadcast to all currently connected
 * players and optionally to all current connected robots.
 * @author Gustavo Figueiredo
 */
public interface CommandWithBroadcast<T> extends Command {

	/**
	 * Tells if this command should be broadcast to other robots once
	 * its action is complete locally
	 */
	default public boolean hasBroadcastToRobots() { return true; };
	
	/**
	 * Session path identifier that should be avoided during broadcast
	 */
	default public String getBroadcastExcludePath(T object) { return null; }

	/**
	 * Message to be used in broadcast to other players or robots
	 */
	public String getBroadcastMessage(RoboToyServerContext context,T object);

}

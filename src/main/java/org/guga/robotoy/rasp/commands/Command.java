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
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * General interface for different types of commands.
 * 
 * @author Gustavo Figueiredo
 */
public interface Command {

	/**
	 * Check if some message supposed to be transmitted by another robot or some directly player is acknownledged by this command.
	 */
	public boolean isParseable(CommandIssuer issuer,String message);
	
	/**
	 * Translates an incoming message and takes action.<BR>
	 * Returns a non-null value if there is anything to broadcast.<BR>
	 * The value returned must be of the same type informed in generics parameter
	 * or must be a collection of such types.<BR> 
	 * If it's a collection, there will be multiple broadcasts, one for each object.<BR>
	 * If it's an object (different from collection or String), there will be a broadcast with this object.<BR>
	 * Throws exception in case of error.
	 */
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception;

	/**
	 * After a call to 'parseMessage', this method will be called to get some custom reply to the caller.
	 * @param parsedMessage Returned value from previous parseMessage call 
	 */
	default public String getReply(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session,Object parsedMessage) { return null; }
}

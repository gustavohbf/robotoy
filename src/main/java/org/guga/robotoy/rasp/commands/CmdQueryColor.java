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
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * Queries for robot's current color.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdQueryColor implements Command {

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.length()>0 && message.charAt(0)==RoboToyServerController.QUERY_COLOR;
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		return null; // nothing to execute locally
	}

	@Override
	public String getReply(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session,Object parsedMessage) {
		GameRobot local_robot = context.getGame().findLocalRobot();
		if (local_robot==null)
			return null;
		return new CmdSetColor().getBroadcastMessage(context, local_robot);
	}

}

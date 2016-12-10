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

import java.net.InetAddress;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * Queries for all known robots in game.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdQueryRobots implements Command {

	@Override
	public String getHelp() {
		return RoboToyServerController.QUERY_ROBOTS + " - Query for current robots in game.";
	}

	public static String run(RoboToyServerContext context,InetAddress localAddr) {
		StringBuilder response = new StringBuilder();
		response.append("{\"robots\":[");
		boolean first = true;
		String localAddress = localAddr.getHostAddress();
		for (GameRobot robot:context.getGame().getRobots()) {
			if (first)
				first = false;
			else
				response.append(",");
			response.append(RobotSummary.getRobotInfo(robot,localAddress));
		}
		response.append("]}");
		return response.toString();
	}
	
	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.length()>0 && message.charAt(0)==RoboToyServerController.QUERY_ROBOTS;
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		return null; // nothing to execute locally
	}
	
	@Override
	public String getReply(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session,Object parsedMessage) {
		return CmdQueryRobots.run(context,session.getLocalAddress());
	}
}

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
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Message broadcast from robot to directly connected players notifying about another robot
 * that just lost connection.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdRemoveRobot implements CommandWithBroadcast<GameRobot> {

	@Override
	public String getHelp() {
		return "{\"removerobot\":<robot summary>} - Notifies that a robot should be removed from game. This command must be issued by a robot.";
	}

	@Override
	public GameRobot parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		// robot disconnected
		RobotSummary robot_to_remove = CmdRemoveRobot.parse(message);
		if (robot_to_remove==null)
			return null;
		String address = robot_to_remove.getAddress();
		final InetAddress localAddr = session.getLocalAddress();
		if (address!=null && address.equals(localAddr.getHostAddress()))
			address = null; // local robot = null address
		GameRobot robot = context.getGame().findRobotWithAddress(address);
		if (robot==null)
			return null;
		context.getGame().removeRobot(robot);
		return robot;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GameRobot robot) {
		StringBuilder message = new StringBuilder();
		message.append("{\"removerobot\":");
		message.append(RobotSummary.getRobotInfo(robot));
		message.append("}");
		return message.toString();
	}
	
	@Override
	public boolean hasBroadcastToRobots() { 
		return false; // no need to notify other robots (they will know due to connection lost) 
	};

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"removerobot\":");
		default:
			return false; // never directly called by a player
		}
	}

	public static RobotSummary parse(String json) {
		RemoveRobot chg = JSONUtils.fromJSON(json, RemoveRobot.class);
		return (chg==null) ? null : chg.removerobot;
	}

	public static class RemoveRobot {
		private RobotSummary removerobot;
		public RobotSummary getRemoverobot() {
			return removerobot;
		}
		public void setRemoverobot(RobotSummary removeRobot) {
			this.removerobot = removeRobot;
		}		
	}
}

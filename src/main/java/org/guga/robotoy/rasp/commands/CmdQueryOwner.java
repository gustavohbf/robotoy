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
import org.guga.robotoy.rasp.game.GamePersistentData;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Queries the robot for current player controlling it.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdQueryOwner implements Command {

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.length()>0 
				&& (message.charAt(0)==RoboToyServerController.QUERY_OWNER
				|| message.startsWith("{\"owner\""));
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		if (CommandIssuer.ROBOT.equals(issuer)
			&& message.startsWith("{\"owner\"")) {
			
			Ownership info = JSONUtils.fromJSON(message, Ownership.class);
			if (info!=null) {
				String robot_id = RoboToyServerContext.getRobotIdentifier(session);
				if (robot_id==null)
					throw new Exception("Session '"+session.getSessionId()+"' is not related to an identified robot!");
				GameRobot robot = context.getGame().findRobotWithIdentifier(robot_id);
				if (robot!=null) {
					if (info.getOwner()!=null) {
						GamePlayer player = context.getGame().findPlayerWithName(info.getOwner());
						if (player!=null) {
							robot.setOwner(player);
						}
					}
					if (info.getPrevious()!=null) {
						robot.setPreviousOwner(info.getPrevious());
					}
				}
			}
		}
		return null;
	}

	@Override
	public String getReply(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session,Object parsedMessage) {
		
		if (CommandIssuer.ROBOT.equals(issuer)
				&& message.startsWith("{\"owner\"")) 
			return null;

		GameRobot local = context.getGame().findLocalRobot();
		
		String currentOwner;
		if (local!=null && local.getOwner()!=null && local.getOwner().getName()!=null)
			currentOwner = local.getOwner().getName();
		else
			currentOwner = null;
		
		String previousOwner = null;
		if (context.getLocalStorage()!=null) {
			try {
				GamePersistentData data = GamePersistentData.load(context, false);
				if (data!=null) {
					previousOwner = data.getPreviousOwner();
				}
			}
			catch (Throwable e) {
				previousOwner = null;
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("{\"owner\":");
		sb.append(JSONUtils.quote(currentOwner));
		sb.append(",\"previous\":");
		sb.append(JSONUtils.quote(previousOwner));
		sb.append("}");

		return sb.toString();
	}

	public static class Ownership {
		private String owner;
		private String previous;
		public String getOwner() {
			return owner;
		}
		public void setOwner(String owner) {
			this.owner = owner;
		}
		public String getPrevious() {
			return previous;
		}
		public void setPrevious(String previousOwner) {
			this.previous = previousOwner;
		}		
	}
}

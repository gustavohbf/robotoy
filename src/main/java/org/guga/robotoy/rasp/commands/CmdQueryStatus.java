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
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.network.InetUtils;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.network.WiFiUtils;
import org.guga.robotoy.rasp.network.WiFiUtils.WiFiInfo;

/**
 * Queries for robot's internal status.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdQueryStatus implements Command {
	
	public static final String ID_CURRENT_SPEED = "speed";
	public static final String ID_WIFI_QUALITY = "wifi";
	public static final String ID_GAME_STAGE = "stage";
	public static final String ID_ROBOT_LIFE = "life";

	@Override
	public String getHelp() {
		return RoboToyServerController.QUERY_STATUS + " - Query for current robot status.";
	}

	public static String run(RoboToyServerContext context) {
		StringBuilder response = new StringBuilder();
		response.append("{\"");
		response.append(ID_CURRENT_SPEED);
		response.append("\":");
		response.append((int)(context.getSpeed()*100.0));

		try {
			WiFiInfo info = WiFiUtils.getWirelessInfo(InetUtils.DEFAULT_WIFI_INTERFACE);
			if (info!=null) {
				response.append(",\"");
				response.append(ID_WIFI_QUALITY);
				response.append("\":");
				response.append((int)info.getQuality());
			}
		}
		catch (Throwable e) {
			// no WiFi information available
		}
		
		GameStage stage = context.getGame().getStage();
		response.append(",\"");
		response.append(ID_GAME_STAGE);
		response.append("\":\"");
		response.append(stage.name());
		response.append("\"");

		GameRobot robot = context.getGame().findLocalRobot();
		if (robot!=null) {
			response.append(",\"");
			response.append(ID_ROBOT_LIFE);
			response.append("\":");
			response.append(robot.getLife());
		}

		response.append("}");
		return response.toString();
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.length()>0 && message.charAt(0)==RoboToyServerController.QUERY_STATUS;
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		return null; // nothing to execute locally
	}

	@Override
	public String getReply(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session,Object parsedMessage) {
		return CmdQueryStatus.run(context);
	}
}

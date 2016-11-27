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
import org.guga.robotoy.rasp.game.GameRules;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.statistics.RoboToyStatistics;

/**
 * Command sent by a player to its controlled robot telling it to fire its weapon.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdFire implements Command {

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.length()>0 && message.charAt(0)==RoboToyServerController.FIRE
				&& CommandIssuer.PLAYER.equals(issuer);
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		if (!GameRules.canFireRobot(context.getGame(), context.getGame().findLocalRobot()))
			return null;
		if (context.getBeamDevice()==null)
			throw new Exception("NO BEAM DEVICE IS AVAILABLE!");
		if (!CommandIssuer.PLAYER.equals(issuer))
			throw new Exception("MUST BE FIRED BY A PLAYER COMMAND!");
		
		String player_name = RoboToyServerContext.getPlayerName(session);
		if (player_name==null)
			throw new Exception("Session '"+session.getSessionId()+"' is not related to an identified player!");

		GameRobot robot = context.getGame().findRobotWithOwnerName(player_name);
		if (robot==null)
			throw new Exception("NO ROBOT IS CONTROLLED BY PLAYER "+player_name+"!");
		byte id = robot.getShortId();
		context.getBeamDevice().sendBeam(new byte[]{id});
		if (context.isTakeStatistics()) {
			RoboToyStatistics.incIRStatCountFires();
		}
		return null;
	}

}

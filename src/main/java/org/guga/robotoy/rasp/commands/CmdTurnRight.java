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
import org.guga.robotoy.rasp.game.GameRules;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * Command sent by a player to make the robot rotate clockwise
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdTurnRight implements Command {

	@Override
	public String getHelp() {
		return RoboToyServerController.TURN_RIGHT+" - Turn right. This command must be issued by a player.";
	}

	public static void run(RoboToyServerContext context) {
		context.getMotor().turnRight(context.getSpeed());
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.length()>0 && message.charAt(0)==RoboToyServerController.TURN_RIGHT;
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		if (!GameRules.canMoveRobot(context.getGame(), context.getGame().findLocalRobot()))
			return null;
		CmdTurnRight.run(context);
		return null;
	}

}

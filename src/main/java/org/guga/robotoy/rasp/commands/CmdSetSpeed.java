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
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * Command sent by a player for setting the robot's current speed.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdSetSpeed implements Command {

	@Override
	public String getHelp() {
		return RoboToyServerController.SET_MIN_SPEED + " - Set robot speed to minimum. Must be issued by a player." +
				RoboToyServerController.SET_MED_SPEED + " - Set robot speed to medium. Must be issued by a player." +
				RoboToyServerController.SET_MAX_SPEED + " - Set robot speed to maximum. Must be issued by a player.";
	}

	public static void run(RoboToyServerContext context) {
		context.getMotor().setSpeed(context.getSpeed());
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		if (message.isEmpty())
			return false;
		char c = message.charAt(0);
		return c==RoboToyServerController.SET_MIN_SPEED
			|| c==RoboToyServerController.SET_MED_SPEED
			|| c==RoboToyServerController.SET_MAX_SPEED;
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		
		char c = message.charAt(0);
		switch (c) {
		case RoboToyServerController.SET_MIN_SPEED:
			context.setSpeed(RoboToyServerContext.MIN_SPEED);
			CmdSetSpeed.run(context);
			break;
		case RoboToyServerController.SET_MED_SPEED:
			context.setSpeed(RoboToyServerContext.MED_SPEED);
			CmdSetSpeed.run(context);
			break;
		case RoboToyServerController.SET_MAX_SPEED:
			context.setSpeed(RoboToyServerContext.MAX_SPEED);
			CmdSetSpeed.run(context);
			break;
		}

		return null;
	}
}

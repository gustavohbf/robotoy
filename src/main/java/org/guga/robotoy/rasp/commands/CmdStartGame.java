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
import org.guga.robotoy.rasp.game.GamePersistentData;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStart;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * Message broadcast by one robot to other robots and players notifying that
 * the game should start (probably it was in INIT stage and should enter into PLAY stage).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdStartGame implements CommandWithBroadcast<Boolean> {
	
	public static void run(RoboToyServerContext context) {
		GameStart.startGame(context.getGame());
		// Save the name of current owner of the local robot
		GameRobot local = context.getGame().findLocalRobot();
		if (local!=null 
				&& local.getOwner()!=null 
				&& local.getOwner().getName()!=null 
				&& context.getLocalStorage()!=null) {
			try {
				GamePersistentData data = GamePersistentData.load(context, true);
				data.setPreviousOwner(local.getOwner().getName());
				GamePersistentData.save(context, data);
			}
			catch (Exception e) {
				// nothing to save...
			}
		}
	}

	@Override
	public boolean isParseable(CommandIssuer issuer, String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"startgame\":");
		default:
			return false;	// never received by a player
		}
	}

	@Override
	public Object parseMessage(CommandIssuer issuer, RoboToyServerContext context, String message, WebSocketActiveSession session) throws Exception {
		CmdStartGame.run(context);
		return Boolean.TRUE;
	}

	@Override
	public String getBroadcastMessage(RoboToyServerContext context,Boolean object) {
		return "{\"startgame\":true}";
	}

}

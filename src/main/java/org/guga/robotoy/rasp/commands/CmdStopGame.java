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
import org.guga.robotoy.rasp.game.GameOver;
import org.guga.robotoy.rasp.game.GamePlayMode;
import org.guga.robotoy.rasp.game.GameStart;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * Message broadcast by one robot to other robots and players notifying that
 * a running game is now over (probably it was in PLAY stage and should enter
 * into SUMMARY stage).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdStopGame implements CommandWithBroadcast<Boolean> {

	public static void run(RoboToyServerContext context) {
		
		// In standalone test-drive we just go back to the first page
		if (GamePlayMode.STANDALONE.equals(context.getGamePlayMode())) {
			GameStart.resetGame(context.getGame());
			return;
		}

		// In normal gameplay we go to summary page

		GameOver.stopGame(context.getGame());
	}

	@Override
	public boolean isParseable(CommandIssuer issuer, String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"stopgame\":");
		default:
			return false;	// never received by a player
		}
	}

	@Override
	public Object parseMessage(CommandIssuer issuer, RoboToyServerContext context, String message, WebSocketActiveSession session) throws Exception {
		CmdStopGame.run(context);
		return Boolean.TRUE;
	}

	@Override
	public String getBroadcastMessage(RoboToyServerContext context,Boolean object) {
		return "{\"stopgame\":true}";
	}

}

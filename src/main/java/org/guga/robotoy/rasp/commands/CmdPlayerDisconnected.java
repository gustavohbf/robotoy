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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Message broadcast by one robot to the others notifying about a connected player that just
 * got disconnected from the broadcasting robot.<BR>
 * Note: the player is not removed from the game. He's just declared offline.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdPlayerDisconnected implements CommandWithBroadcast<GamePlayer> {

	private static final Logger log = Logger.getLogger(CmdPlayerDisconnected.class.getName());

	@Override
	public String getHelp() {
		return "{\"playeroffline\":<player summary>} - Notifies that a player just got disconnected. This command must be issued by a robot.";
	}

	@Override
	public GamePlayer parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		PlayerSummary player = CmdPlayerDisconnected.parse(message);
		if (player==null) {
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE,"Got 'playeroffline' event but the message was not properly translated: "+message);
			return null;
		}
		GamePlayer player_offline = context.getGame().findPlayerWithName(player.getName());
		if (player_offline!=null) {
			player_offline.setOnline(false);
		}
		return player_offline;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GamePlayer player) {
		StringBuilder message = new StringBuilder();
		message.append("{\"playeroffline\":");
		message.append(PlayerSummary.getPlayerInfo(player));
		message.append("}");
		return message.toString();
	}
	
	@Override
	public String getBroadcastExcludePath(GamePlayer player) {
		// avoid sending message back to the same player that just got in
		return RoboToyServerContext.getWSPathWithPlayerName(player.getName());
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"playeroffline\":");
		default:
			return false; // never directly called by a player
		}
	}

	public static PlayerSummary parse(String json) {
		PlayerDisconnected chg = JSONUtils.fromJSON(json, PlayerDisconnected.class);
		return (chg==null) ? null : chg.playeroffline;
	}

	public static class PlayerDisconnected {
		private PlayerSummary playeroffline;

		public PlayerSummary getPlayeroffline() {
			return playeroffline;
		}

		public void setPlayeroffline(PlayerSummary playeroffline) {
			this.playeroffline = playeroffline;
		}
	}

}

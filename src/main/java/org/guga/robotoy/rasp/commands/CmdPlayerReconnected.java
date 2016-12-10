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
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Message broadcast by one robot to the others notifying about a player that was
 * disconnected for a while but just got reconnected again.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdPlayerReconnected implements CommandWithBroadcast<GamePlayer> {

	@Override
	public String getHelp() {
		return "{\"playeronline\":<player summary>} - Notifies that a player just got reconnected. This command must be issued by a robot.";
	}

	@Override
	public GamePlayer parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		PlayerSummary player = CmdPlayerReconnected.parse(message);
		if (player==null)
			return null;
		GamePlayer player_online = context.getGame().findPlayerWithName(player.getName());
		if (player_online==null) {
			player_online = CmdAddNewPlayer.run(context, player.getName(), player.getAddress(), player.getPort());
		}
		if (player_online!=null) {
			player_online.setOnline(true);
		}
		return player_online;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GamePlayer player) {
		StringBuilder message = new StringBuilder();
		message.append("{\"playeronline\":");
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
			return message.startsWith("{\"playeronline\":");
		default:
			return false; // never directly called by a player
		}
	}

	public static PlayerSummary parse(String json) {
		PlayerReconnected chg = JSONUtils.fromJSON(json, PlayerReconnected.class);
		return (chg==null) ? null : chg.playeronline;
	}

	public static class PlayerReconnected {
		private PlayerSummary playeronline;

		public PlayerSummary getPlayeronline() {
			return playeronline;
		}

		public void setPlayeronline(PlayerSummary playeronline) {
			this.playeronline = playeronline;
		}
	}

}

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
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Message broadcast by one robot to other robots or players for notifying about a new player in
 * the game (probably a player that connected directly to the broadcasting robot).<BR>
 * <BR>
 * This message is not delivered by the player itself.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdAddNewPlayer implements CommandWithBroadcast<GamePlayer> {
	
	@Override
	public String getHelp() {
		return "{\"newplayer\":<player summary>} - Defines a new player. This command must be issued by a robot.";
	}

	public static GamePlayer run(RoboToyServerContext context,String playerName,String playerAddress,int port) throws Exception {
		
		// Add player if not already included
		GamePlayer existent = context.getGame().findPlayerWithName(playerName);
		if (null!=existent) {
			if (!playerAddress.equals(existent.getAddressString())
				|| (port!=0 && port!=existent.getPort())) {
				existent.setAddress(InetAddress.getByName(playerAddress));
				if (port!=0)
					existent.setPort(port);
			}
			return null; // already existed
		}
		existent = context.getGame().findPlayerWithAddress(playerAddress,port);
		if (null!=existent) {
			if (!playerName.equals(existent.getName())) {
				String previousName = existent.getName();
				existent.setName(playerName);
				context.updateWebSocketReferencesAfterNameChanged(previousName, playerName);
			}
			return null; // already existed
		}
		GamePlayer new_player = new GamePlayer(playerName,playerAddress,port);
		new_player.setOnline(true);
		context.getGame().addPlayer(new_player);
		return new_player;
	}
	
	@Override
	public GamePlayer parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		PlayerSummary player = CmdAddNewPlayer.parse(message);
		if (player==null)
			return null;
		GamePlayer new_player = CmdAddNewPlayer.run(context, player.getName(), player.getAddress(), player.getPort());
		return new_player;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GamePlayer player) {
		StringBuilder message = new StringBuilder();
		message.append("{\"newplayer\":");
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
			return message.startsWith("{\"newplayer\":");
		default:
			return false; // never directly called by a player
		}
	}

	public static PlayerSummary parse(String json) {
		NewPlayer chg = JSONUtils.fromJSON(json, NewPlayer.class);
		return (chg==null) ? null : chg.newplayer;
	}

	public static class NewPlayer {
		private PlayerSummary newplayer;
		public PlayerSummary getNewplayer() {
			return newplayer;
		}
		public void setNewplayer(PlayerSummary newplayer) {
			this.newplayer = newplayer;
		}
	}
}

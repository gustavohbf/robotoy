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
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Message broadcast from one robot to other robots and players notifying about a player
 * that should be removed from the game.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdRemovePlayer implements CommandWithBroadcast<GamePlayer> {

	@Override
	public String getHelp() {
		return "{\"removeplayer\":<player summary>} - Notifies that a player should be removed from game. This command must be issued by a robot.";
	}

	public static GamePlayer run(RoboToyServerContext context,String player_name) {
		if (player_name==null)
			return null;
		GamePlayer player = context.getGame().findPlayerWithName(player_name);
		if (player!=null) {
			context.getGame().removePlayer(player);
		}
		synchronized (CmdTakeRobot.SYNCHRONIZATION_OBJECT) {
			GameRobot robot_owned_by_player = context.getGame().findRobotWithOwnerName(player_name);
			if (robot_owned_by_player!=null) {
				robot_owned_by_player.setOwner(null);
			}
		}
		return player;
	}
	
	@Override
	public GamePlayer parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		PlayerSummary player = CmdRemovePlayer.parse(message);
		GamePlayer removed_player = CmdRemovePlayer.run(context, player.getName());
		return removed_player;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GamePlayer player) {
		StringBuilder message = new StringBuilder();
		message.append("{\"removeplayer\":");
		message.append(PlayerSummary.getPlayerInfo(player));
		message.append("}");
		return message.toString();
	}
	
	@Override
	public String getBroadcastExcludePath(GamePlayer player) {
		// avoid sending message back to the same player that just left
		return RoboToyServerContext.getWSPathWithPlayerName(player.getName());
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"removeplayer\":");
		default:
			return false; // never directly called by a player
		}
	}

	public static PlayerSummary parse(String json) {
		RemovePlayer chg = JSONUtils.fromJSON(json, RemovePlayer.class);
		return (chg==null) ? null : chg.removeplayer;
	}

	public static class RemovePlayer {
		private PlayerSummary removeplayer;
		public PlayerSummary getRemoveplayer() {
			return removeplayer;
		}
		public void setRemoveplayer(PlayerSummary removeplayer) {
			this.removeplayer = removeplayer;
		}		
	}
}

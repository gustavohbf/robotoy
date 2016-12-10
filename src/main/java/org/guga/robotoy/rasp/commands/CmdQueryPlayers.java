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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Queries for all current players in game.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdQueryPlayers implements CommandWithBroadcast<GamePlayer> {

	private static final Logger log = Logger.getLogger(CmdQueryPlayers.class.getName());

	@Override
	public String getHelp() {
		return RoboToyServerController.QUERY_PLAYERS + " - Query for current players in game. Must be issued by a player.\n"
			+"{\"players\":[<players summaries>]} - Notify a list of current players in game. Must be issued by a robot.";
	}

	public static String run(RoboToyServerContext context) {
		StringBuilder response = new StringBuilder();
		response.append("{\"players\":[");
		boolean first = true;
		for (GamePlayer player:context.getGame().getPlayers()) {
			if (first)
				first = false;
			else
				response.append(",");
			response.append(PlayerSummary.getPlayerInfo(player));
		}
		response.append("]}");
		return response.toString();
	}
	
	@Override
	public List<GamePlayer> parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		
		if (CommandIssuer.ROBOT.equals(issuer)) {
			// Got players from another BOT
			// Will merge BOT's players list with ours
			CmdQueryPlayers.Players players = CmdQueryPlayers.parse(message);
			if (log.isLoggable(Level.FINEST)) {
				StringBuilder msg = new StringBuilder();
				final InetAddress remoteAddr = session.getRemoteAddress();
				final int remotePort = session.getRemotePort(); 
				msg.append("Got "+players.getNumPlayers()+" players from RoboToy at "+remoteAddr.getHostAddress()+":"+remotePort);
				for (int i=0;i<players.getNumPlayers();i++) {
					msg.append("\n\tPlayer["+i+"]: "+players.getPlayer(i));
				}
				log.log(Level.FINEST,msg.toString());
			}
			List<GamePlayer> new_players = context.getGame().mergePlayers(players);
			return new_players;
		}
		
		return null;
	}
	
	@Override
	public String getReply(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session,Object parsedMessage) {
		
		if (CommandIssuer.PLAYER.equals(issuer)) {
			return CmdQueryPlayers.run(context);
		}
		
		return null;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GamePlayer player) {
		// If we are broadcasting from this, we are probably informing our players about
		// other new players.
		StringBuilder message = new StringBuilder();
		message.append("{\"newplayer\":");
		message.append(PlayerSummary.getPlayerInfo(player));
		message.append("}");
		return message.toString();
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"players\":");
		case PLAYER:
			return message.length()>0 && message.charAt(0)==RoboToyServerController.QUERY_PLAYERS;
		default:
			return false;
		}
	}
	
	public static Players parse(String json) {
		return JSONUtils.fromJSON(json, Players.class);
	}
	
	public static class Players {
		private PlayerSummary[] players;		
		public PlayerSummary[] getPlayers() {
			return players;
		}
		public PlayerSummary getPlayer(int i) {
			return players[i];
		}
		public void setPlayers(PlayerSummary[] players) {
			this.players = players;
		}
		public int getNumPlayers() {
			return (players==null) ? 0 : players.length;
		}
	}
}

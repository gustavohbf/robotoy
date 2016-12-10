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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Command sent by a player at summary page telling he wants to play again. May be broadcast
 * from robot to the others to pass away the same message. 
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdPlayAgain implements CommandWithBroadcast<GamePlayer> {

	private static final Logger log = Logger.getLogger(CmdPlayAgain.class.getName());

	@Override
	public String getHelp() {
		return RoboToyServerController.PLAY_AGAIN + " - Player wants to start a new game. Game is in SUMMARY stage. This command must be issued by a player.\n"
				+ "{\"playagain\":<player summary>,\"restartgame\":<boolean>} - Notifies that a player wants to start a new game. Game is in SUMMARY stage. This command must be issued by a robot.";
	}

	public static GamePlayer run(RoboToyServerContext context,WebSocketActiveSession player_session) throws Exception {
		if (!GameStage.SUMMARY.equals(context.getGame().getStage()))
			throw new Exception("Game has started already!");
		String player_name = RoboToyServerContext.getPlayerName(player_session);
		if (player_name==null)
			throw new Exception("Session '"+player_session.getSessionId()+"' is not related to an identified player!");
		GamePlayer player = context.getGame().findPlayerWithName(player_name);
		if (player==null) {
			throw new Exception("Could not find player in this game!");
		}
		player.setDismissedSummary(true);
		List<GamePlayer> pendingPlayers = new LinkedList<>();
		if (context.getGame().hasAllPlayersDismissedSummary(pendingPlayers)) {
			log.log(Level.FINE, "RESTARTING GAME...");
			CmdRestartGame.run(context);
		}
		else {
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				sb.append("Pending players: ");
				for (int i=0;i<pendingPlayers.size();i++) {
					if (i>0)
						sb.append(", ");
					GamePlayer p = pendingPlayers.get(i);
					sb.append(p.getName()+" ("+p.getAddressString()+")");
				}
				log.log(Level.FINE,sb.toString());
			}			
		}
		return player;
	}
	
	@Override
	public GamePlayer parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		
		final InetAddress remoteAddr = session.getRemoteAddress();
		final int remotePort = session.getRemotePort(); 

		if (!GameStage.SUMMARY.equals(context.getGame().getStage())) {
			log.log(Level.WARNING,"Got a '"+message+"' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' but the game is in "+context.getGame().getStage()+" stage!");
			return null; // invalid commmand for this stage
		}
		
		if (CommandIssuer.PLAYER.equals(issuer)) {

			GamePlayer ok_player = CmdPlayAgain.run(context, session);
			return ok_player;

		}
		else if (CommandIssuer.ROBOT.equals(issuer)) {
		
			PlayerSummary player_started = CmdPlayAgain.parse(message);
			GamePlayer player = context.getGame().findPlayerWithName(player_started.getName());
			if (player==null) {
				// Could not find it, will ignore incoming message
				log.log(Level.WARNING,"Got a 'playagain' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' referring to player with name '"+player_started.getName()+"', but we don't know anything about it!");
				return null;
			}
			else {
				player.setDismissedSummary(true);
				List<GamePlayer> pendingPlayers = new LinkedList<>();
				if (context.getGame().hasAllPlayersDismissedSummary(pendingPlayers)) {
					log.log(Level.FINE, "RESTARTING GAME...");
					CmdRestartGame.run(context);
				}
				else {
					if (log.isLoggable(Level.FINE)) {
						StringBuilder sb = new StringBuilder();
						sb.append("Pending players: ");
						for (int i=0;i<pendingPlayers.size();i++) {
							if (i>0)
								sb.append(", ");
							GamePlayer p = pendingPlayers.get(i);
							sb.append(p.getName()+" ("+p.getAddressString()+")");
						}
						log.log(Level.FINE,sb.toString());
					}			
				}
				return player;
			}
		}
		
		return null;
	}
	
	@Override
	public String getReply(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session,Object parsedMessage) {
		
		if (CommandIssuer.PLAYER.equals(issuer)) {
			if (GameStage.INIT.equals(context.getGame().getStage()))
				return "{\"restartgame\":true}"; // same response from 'CmdRestartGame'
			else
				return "{\"ready\":true}";
		}
		
		return null;
	}

	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GamePlayer player) {
		StringBuilder message = new StringBuilder();
		message.append("{\"playagain\":");
		message.append(PlayerSummary.getPlayerInfo(player));
		if (GameStage.INIT.equals(context.getGame().getStage())) {
			message.append(",\"restartgame\":true");
		}
		message.append("}");
		return message.toString();
	}
	
	@Override
	public String getBroadcastExcludePath(GamePlayer player) {
		// avoid sending message back to the same player that just started
		return RoboToyServerContext.getWSPathWithPlayerName(player.getName());
	}
	
	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"playagain\":");
		case PLAYER:
			return message.length()>0 && message.charAt(0)==RoboToyServerController.PLAY_AGAIN;
		default:
			return false;
		}
	}

	public static PlayerSummary parse(String json) {
		PlayAgain chg = JSONUtils.fromJSON(json, PlayAgain.class);
		return (chg==null) ? null : chg.playagain;
	}

	public static class PlayAgain {
		private PlayerSummary playagain;
		boolean restartgame;
		public PlayerSummary getPlayagain() {
			return playagain;
		}
		public void setPlayagain(PlayerSummary playagain) {
			this.playagain = playagain;
		}
		public boolean isRestartgame() {
			return restartgame;
		}
		public void setRestartgame(boolean restartgame) {
			this.restartgame = restartgame;
		}
	}

}

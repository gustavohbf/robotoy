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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GameControlMode;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Message sent by a player or broadcast by a robot telling other players and robots
 * that he is ready to start the game.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdSetReady implements CommandWithBroadcast<GameRobot> {

	private static final Logger log = Logger.getLogger(CmdSetReady.class.getName());

	@Override
	public String getHelp() {
		return RoboToyServerController.START_GAME + " - Player wants to start the game. Game is in INIT stage. This command must be issued by a player.\n"
				+ "{\"setready\":<player summary>,\"startgame\":<boolean>,\"mode\":<buttons|tilt>} - Notifies that a player wants to start the game. Game is in INIT stage. This command must be issued by a robot.";
	}

	public static GameRobot run(RoboToyServerContext context,WebSocketActiveSession player_session,GameControlMode controlMode) throws Exception {
		if (!GameStage.INIT.equals(context.getGame().getStage()))
			throw new Exception("Game has started already!");
		
		String player_name = RoboToyServerContext.getPlayerName(player_session);
		if (player_name==null)
			throw new Exception("Session '"+player_session.getSessionId()+"' is not related to an identified player!");

		GamePlayer player = context.getGame().findPlayerWithName(player_name);
		if (player==null) {
			throw new Exception("Could not find player in this game!");
		}
		GameRobot robot_owned_by_player = context.getGame().findRobotWithOwnerName(player_name);
		if (robot_owned_by_player==null) {
			throw new Exception("Player does not own a robot yet!");
		}
		robot_owned_by_player.setReady(true);
		robot_owned_by_player.setControlMode(controlMode);
		if (context.getGame().hasAllRobotsReady()) {
			CmdStartGame.run(context);
		}
		return robot_owned_by_player;
	}
	
	@Override
	public GameRobot parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		
		final InetAddress remoteAddr = session.getRemoteAddress();
		final int remotePort = session.getRemotePort(); 

		if (!GameStage.INIT.equals(context.getGame().getStage())) {
			log.log(Level.WARNING,"Got a '"+message+"' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' but the game is in "+context.getGame().getStage()+" stage!");
			return null; // invalid commmand for this stage
		}
		
		if (CommandIssuer.PLAYER.equals(issuer)) {

			GameControlMode game_mode = parseGameMode(message);
			if (game_mode==null)
				game_mode = GameControlMode.BUTTONS;

			GameRobot started_robot = CmdSetReady.run(context, session, game_mode);
			if (started_robot!=null) {
				started_robot = started_robot.cloneIfLocalAddress(session.getLocalAddress()); // for broadcasting purposes				
			}
			return started_robot;

		}
		else if (CommandIssuer.ROBOT.equals(issuer)) {
		
			SetReady parsed = CmdSetReady.parse(message);
			RobotSummary robot_started = (parsed==null)?null:parsed.setready;

			GameRobot robot = context.getGame().findRobotWithIdentifier(robot_started.getId());
			if (robot==null) {
				// Could not find it, will ignore incoming message
				log.log(Level.WARNING,"Got a 'setready' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' referring to robot with id '"+robot_started.getId()+"', but we don't know anything about it!");
				return null;
			}
			else {
				GameControlMode control_mode = GameControlMode.parse(parsed.mode);
				robot.setReady(true);
				robot.setControlMode(control_mode);
				if (context.getGame().hasAllRobotsReady()) {
					CmdStartGame.run(context);
				}
				if (robot!=null)
					robot = robot.cloneIfLocalAddress(session.getLocalAddress()); // for broadcasting purposes
				return robot;
			}
		}
		
		return null;
	}
	
	@Override
	public String getReply(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session,Object parsedMessage) {
		
		if (CommandIssuer.PLAYER.equals(issuer)) {
			if (GameStage.PLAY.equals(context.getGame().getStage()))
				return "{\"startgame\":true}"; // same response from 'CmdStartGame'
			else
				return "{\"ready\":true}";
		}
		
		return null;
	}

	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GameRobot robot) {
		StringBuilder message = new StringBuilder();
		message.append("{\"setready\":");
		message.append(RobotSummary.getRobotInfo(robot));
		if (GameStage.PLAY.equals(context.getGame().getStage())) {
			message.append(",\"startgame\":true");
		}
		if (robot.getControlMode()!=null) {
			message.append(",\"mode\":"+JSONUtils.quote(robot.getControlMode().getMode()));
		}
		message.append("}");
		return message.toString();
	}
	
	@Override
	public String getBroadcastExcludePath(GameRobot robot) {
		// avoid sending message back to the same player that just started
		if (robot==null)
			return null;
		GamePlayer owner = robot.getOwner();
		if (owner==null)
			return null;
		return RoboToyServerContext.getWSPathWithPlayerName(owner.getName());
	}
	
	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"setready\":");
		case PLAYER:
			return message.length()>0 && message.charAt(0)==RoboToyServerController.START_GAME;
		default:
			return false;
		}
	}

	public static SetReady parse(String json) {
		SetReady chg = JSONUtils.fromJSON(json, SetReady.class);
		return chg;
	}

	public static GameControlMode parseGameMode(String message) {
		if (message==null || message.isEmpty())
			return null;
		if (message.charAt(0)==RoboToyServerController.START_GAME)
			message = message.substring(1);
		StartGame cmd = JSONUtils.fromJSON(message, StartGame.class);
		if (cmd==null || cmd.mode==null || cmd.mode.length()==0)
			return null;
		return GameControlMode.parse(cmd.mode);
	}

	public static class SetReady {
		private RobotSummary setready;
		private boolean startgame;
		private String mode;
		public RobotSummary getSetready() {
			return setready;
		}
		public void setSetready(RobotSummary setready) {
			this.setready = setready;
		}
		public boolean isStartgame() {
			return startgame;
		}
		public void setStartgame(boolean startgame) {
			this.startgame = startgame;
		}
		public String getMode() {
			return mode;
		}
		public void setMode(String mode) {
			this.mode = mode;
		}	
	}

	public static class StartGame {
		private String mode;
		public String getMode() {
			return mode;
		}
		public void setMode(String mode) {
			this.mode = mode;
		}
	}
}

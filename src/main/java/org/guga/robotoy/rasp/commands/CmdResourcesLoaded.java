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
 * Parse message sent by a connected player when 'PLAY' stage just got started
 * and all needed resources (such as audio and image) have been loaded.<BR>
 * <BR>
 * This message is broadcast to other players and robots in order to synchronize
 * the start of game.<BR>
 * <BR>
 * @author Gustavo Figueiredo
 */
public class CmdResourcesLoaded implements CommandWithBroadcast<CmdResourcesLoaded.MessageToBroadcast> {

	@Override
	public String getHelp() {
		return "{\"loaded\":<player summary>,\"pending\":<number>} - Notifies that a player controlling a robot just got all the page resources loaded in his interface. This command must be issued by a player or robot.";
	}

	@Override
	public CmdResourcesLoaded.MessageToBroadcast parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {

		PlayerSummary parsed = parse(message);
		if (parsed==null)
			return null;
		
		String robot_id = null;
		if (CommandIssuer.ROBOT.equals(issuer)) {
			robot_id = RoboToyServerContext.getRobotIdentifier(session);
			if (robot_id==null)
				return null;	// issuer is not a known robot
		}
				
		GamePlayer player = context.getGame().findPlayerWithName(parsed.getName());
		
		if (player==null) {
			return null;	// player identification is invalid
		}
		
		if (CommandIssuer.PLAYER.equals(issuer) ) {
			String player_name = RoboToyServerContext.getPlayerName(session);
			if (player_name==null || !player_name.equalsIgnoreCase(player.getName()))
				return null;	// issuer is not the player identified in message
		}

		player.setResourcesLoaded(true);
		
		CmdResourcesLoaded.MessageToBroadcast obj = new CmdResourcesLoaded.MessageToBroadcast();
		obj.setPlayer(player);
		if (robot_id!=null) {
			// avoid loopback
			obj.setExcludeReference(RoboToyServerContext.getWSPathWithRobotIdentifier(robot_id));
		}
		return obj;
	}
	
	@Override
	public String getBroadcastExcludePath(CmdResourcesLoaded.MessageToBroadcast object) { 
		return object.getExcludeReference();
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,CmdResourcesLoaded.MessageToBroadcast object) {
		StringBuilder message = new StringBuilder();
		message.append("{\"loaded\":");
		message.append(PlayerSummary.getPlayerInfo(object.getPlayer()));
		
		int pending_count = 0;
		for (GameRobot robot:context.getGame().getRobots()) {
			if (robot.getOwner()==null)
				continue;
			if (!robot.getOwner().isResourcesLoaded())
				pending_count++;
		}
		
		message.append(",\"pending\":");
		message.append(pending_count);
		
		message.append("}");
		return message.toString();
	}
	
	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.startsWith("{\"loaded\":");
	}

	public static PlayerSummary parse(String json) {
		Loaded l = JSONUtils.fromJSON(json, Loaded.class);
		return (l==null) ? null : l.loaded;
	}

	public static class Loaded {
		private PlayerSummary loaded;
		private int pending;
		public PlayerSummary getLoaded() {
			return loaded;
		}
		public void setLoaded(PlayerSummary loaded) {
			this.loaded = loaded;
		}
		public int getPending() {
			return pending;
		}
		public void setPending(int pending) {
			this.pending = pending;
		}
	}

	public static class MessageToBroadcast {
		private GamePlayer player;
		private String excludeReference;

		public GamePlayer getPlayer() {
			return player;
		}

		public void setPlayer(GamePlayer player) {
			this.player = player;
		}

		public String getExcludeReference() {
			return excludeReference;
		}

		public void setExcludeReference(String excludeReference) {
			this.excludeReference = excludeReference;
		}
		
	}
}

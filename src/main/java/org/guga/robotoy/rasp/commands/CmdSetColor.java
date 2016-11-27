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
import org.guga.robotoy.rasp.game.GamePersistentData;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.optics.LedColor;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Message sent by a player or by a robot to other robots and players telling which color the
 * robot should have.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdSetColor implements CommandWithBroadcast<GameRobot> {

	private static final Logger log = Logger.getLogger(CmdSetColor.class.getName());

	public static GameRobot run(RoboToyServerContext context,WebSocketActiveSession player_session,LedColor color) throws Exception {
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
		robot_owned_by_player.setColor(color);
		if (robot_owned_by_player.getAddress()==null) {
			// If this is the local robot, change the LED color
			if (context.getRGBLed()!=null) {
				context.getRGBLed().setColor(color);
			}
		}
		// also persist this information locally so that we can remember this option after shutdown
		if (context.getLocalStorage()!=null) {
			GamePersistentData data = GamePersistentData.load(context, true);
			data.setColor(color.getName());
			GamePersistentData.save(context, data);
		}
		return robot_owned_by_player;
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.startsWith("{\"setcolor\":");
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GameRobot robot) {
		StringBuilder message = new StringBuilder();
		message.append("{\"setcolor\":");
		message.append(RobotSummary.getRobotInfo(robot));
		message.append("}");
		return message.toString();
	}

	@Override
	public GameRobot parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		
		final InetAddress remoteAddr = session.getRemoteAddress();
		final int remotePort = session.getRemotePort(); 

		if (!GameStage.INIT.equals(context.getGame().getStage())) {
			log.log(Level.WARNING,"Got a '"+message+"' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' but the game is in "+context.getGame().getStage()+" stage!");
			return null; // invalid commmand for this stage
		}
		
		RobotSummary summary = parse(message);
		if (summary==null) {
			throw new Exception("Invalid contents!");
		}
		if (summary.getColor()==null) {
			throw new Exception("Missing color name!");
		}
		LedColor color = LedColor.get(summary.getColor());
		if (color==null) {
			throw new Exception("Unknown color: "+summary.getColor());
		}

		if (CommandIssuer.PLAYER.equals(issuer)) {
			
			String player_name = RoboToyServerContext.getPlayerName(session);
			if (player_name==null) {
				log.log(Level.WARNING,"Got a 'setcolor' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' supposedly a player, but there is no player with such IP and port number!");
				return null; // not a player				
			}
			if (context.getGame().findPlayerWithName(player_name)==null) {
				log.log(Level.WARNING,"Got a 'setcolor' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' supposedly a player, but there is no player with name '"+player_name+"'!");
				return null; // not a player
			}
			
			GameRobot target_robot = CmdSetColor.run(context, session, color);
			if (target_robot!=null)
				target_robot = target_robot.cloneIfLocalAddress(session.getLocalAddress()); // for broadcasting purposes
			return target_robot;

		}
		else if (CommandIssuer.ROBOT.equals(issuer)) {

			String robot_id = RoboToyServerContext.getRobotIdentifier(session);
			if (robot_id==null) {
				log.log(Level.WARNING,"Got a 'setcolor' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' supposedly a robot, but there is no robot with such IP and port number!");
				return null; // not a robot
			}
			if (context.getGame().findRobotWithIdentifier(robot_id)==null) {
				log.log(Level.WARNING,"Got a 'setcolor' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' supposedly a robot, but there is no robot with id '"+robot_id+"'!");
				return null; // not a robot
			}

			if (summary.getId()==null) {
				// Probably got this message from a player
				return null;
			}
			GameRobot robot = context.getGame().findRobotWithIdentifier(summary.getId());
			if (robot==null) {
				// Could not find it, will ignore incoming message
				log.log(Level.WARNING,"Got a 'setcolor' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' referring to robot with id '"+summary.getId()+"', but we don't know anything about it!");
				return null;
			}
			else {
				robot.setColor(color);
				if (robot.getAddress()==null) {
					// If this is the local robot, change LED color
					if (context.getRGBLed()!=null) {
						context.getRGBLed().setColor(color);
					}
				}
				if (robot!=null)
					robot = robot.cloneIfLocalAddress(session.getLocalAddress()); // for broadcasting purposes
				return robot;
			}
		}
		
		return null;
	}
	
	public static RobotSummary parse(String json) {
		SetColor chg = JSONUtils.fromJSON(json, SetColor.class);
		return (chg==null) ? null : chg.setcolor;
	}

	public static class SetColor {
		private RobotSummary setcolor;
		public RobotSummary getSetcolor() {
			return setcolor;
		}
		public void setSetcolor(RobotSummary setcolor) {
			this.setcolor = setcolor;
		}		
	}

}

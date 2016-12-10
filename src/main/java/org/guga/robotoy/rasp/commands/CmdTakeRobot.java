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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.game.GameState;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Command sent by a player and broadcast by a robot to other robots and players notifying
 * that the robot should be controlled by that player.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdTakeRobot implements CommandWithBroadcast<GameRobot> {
	
	@Override
	public String getHelp() {
		return RoboToyServerController.TAKE_ROBOT + " - Take control of a robot. This command must be issued by a player.\n"
				+ "{\"changeowner\":<robot summary>} - Notifies that a player took control a robot or is not controlling it anymore. This command must be issued by a robot.";
	}

	/**
	 * Synchronization object used in different parts of this application that
	 * need to assert unique control of a robot.<BR>
	 * WARNING: synchronization may fail if different users connected to different
	 * robots are trying to take ownership of the same robot at the same time.
	 */
	public static final Object SYNCHRONIZATION_OBJECT = new Object();

	private static final Logger log = Logger.getLogger(CmdTakeRobot.class.getName());

	public static GameRobot[] run(RoboToyServerContext context,WebSocketActiveSession player_session,String robot_id,InetAddress robot_address) throws Exception {
		
		synchronized (SYNCHRONIZATION_OBJECT) {
			
			String player_name = RoboToyServerContext.getPlayerName(player_session);
			if (player_name==null)
				throw new Exception("Session '"+player_session.getSessionId()+"' is not related to an identified player!");

			// Check if same user alread took another robot
			GameRobot owned = context.getGame().findRobotWithOwnerName(player_name);
			if (owned!=null) {
				if (robot_id.equals(owned.getIdentifier())) {
					// Already owns the desired robot, but will enforce anyway
					return new GameRobot[]{owned.cloneIfLocalAddress(robot_address)};
				}
				else {
					// Owns another robot, so leave it as long as he takes another robot
					GameRobot[] result = new GameRobot[2];
					GameRobot new_owned = takeOwnership(context.getGame(),player_session,robot_id);
					owned.setOwner(null);
					result[0] = owned.cloneIfLocalAddress(robot_address);
					result[1] = new_owned.cloneIfLocalAddress(robot_address);
					
					return result;
				}
			}
			else {
				// Check if another user took the desired robot
				owned = takeOwnership(context.getGame(),player_session,robot_id).cloneIfLocalAddress(robot_address);
				return new GameRobot[]{owned};
			}
		}
	}
	
	public static GameRobot[] run(RoboToyServerContext context,String player_name,String robot_id,InetAddress robot_address) throws Exception {
		
		synchronized (SYNCHRONIZATION_OBJECT) {
			
			// Check if same user alread took another robot
			GameRobot owned = context.getGame().findRobotWithOwnerName(player_name);
			if (owned!=null) {
				if (robot_id.equals(owned.getIdentifier())) {
					// Already owns the desired robot, but will enforce anyway
					return new GameRobot[]{owned.cloneIfLocalAddress(robot_address)};
				}
				else {
					// Owns another robot, so leave it as long as he takes another robot
					GameRobot[] result = new GameRobot[2];
					GameRobot new_owned = takeOwnership(context.getGame(),player_name,robot_id);
					owned.setOwner(null);
					result[0] = owned.cloneIfLocalAddress(robot_address);
					result[1] = new_owned.cloneIfLocalAddress(robot_address);
					
					return result;
				}
			}
			else {
				// Check if another user took the desired robot
				owned = takeOwnership(context.getGame(),player_name,robot_id).cloneIfLocalAddress(robot_address);
				return new GameRobot[]{owned};
			}
		}
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		
		final InetAddress remoteAddr = session.getRemoteAddress();
		final int remotePort = session.getRemotePort(); 

		if (!GameStage.INIT.equals(context.getGame().getStage())) {
			log.log(Level.WARNING,"Got a '"+message+"' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' but the game is in "+context.getGame().getStage()+" stage!");
			return null; // invalid commmand for this stage
		}

		if (CommandIssuer.PLAYER.equals(issuer)) {
			String robot_id = message.substring(1);
			GameRobot[] target_robots = CmdTakeRobot.run(context,session,robot_id,session.getLocalAddress());
			if (target_robots!=null && target_robots.length>0) {
				if (target_robots.length==1)
					return target_robots[0];
				else
					return Arrays.asList(target_robots);
			}
		}
		else if (CommandIssuer.ROBOT.equals(issuer)) {
			// Somebody took or left ownership of some robot (maybe us)
			// Will just update our status
			RobotSummary changed_robot = CmdTakeRobot.parse(message);
			if (changed_robot!=null) {
				
				synchronized (SYNCHRONIZATION_OBJECT) {
				
					// Locate robot in our current game status
					String robot_id = changed_robot.getId();
					GameRobot robot = context.getGame().findRobotWithIdentifier(robot_id);
					if (robot==null) {
						// Could not find it, will ignore incoming message
						log.log(Level.WARNING,"Got a 'changeowner' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' referring to robot with id '"+robot_id+"', but we don't know anything about it!");
						return null;
					}
					else {
						// Found it. Let's update internal state about this robot
						if (changed_robot.getOwner()==null) {
							robot.setOwner(null);
						}
						else {
							GamePlayer player = context.getGame().findPlayerWithName(changed_robot.getOwner());
							if (player==null) {
								log.log(Level.SEVERE,"Got a 'changeowner' from '"+remoteAddr.getHostAddress()+":"+remotePort+"' referring to player with name '"+changed_robot.getOwner()+"', but we don't know anything about it!");
								return null;
							}
							robot.setOwner(player);
						}
						// If it's ourselves, broadcasts a clone instance with indication of our address
						return robot.cloneIfLocalAddress(session.getLocalAddress());
					}
					
				}
			}
		}
		return null;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GameRobot robot) {
		StringBuilder message = new StringBuilder();
		message.append("{\"changeowner\":");
		message.append(RobotSummary.getRobotInfo(robot));
		message.append("}");
		return message.toString();
	}

	private static GameRobot takeOwnership(GameState game,WebSocketActiveSession player_session,String robot_id) throws Exception {
		GameRobot found = game.findRobotWithIdentifier(robot_id);
		if (found==null)
			throw new Exception("Could not find robot with identifier "+robot_id+"!");
		if (found.getOwner()!=null)
			throw new Exception("This robot is alread owned by \""+found.getOwner().getName()+"\"!");
		
		String player_name = RoboToyServerContext.getPlayerName(player_session);
		if (player_name==null)
			throw new Exception("Session '"+player_session.getSessionId()+"' is not related to an identified player!");

		GamePlayer player = game.findPlayerWithName(player_name);
		if (player==null)
			throw new Exception("Could not find player with name "+player_name+"!");
		found.setOwner(player);
		return found;
	}
	
	private static GameRobot takeOwnership(GameState game,String player_name,String robot_id) throws Exception {
		GameRobot found = game.findRobotWithIdentifier(robot_id);
		if (found==null)
			throw new Exception("Could not find robot with identifier "+robot_id+"!");
		if (found.getOwner()!=null)
			throw new Exception("This robot is alread owned by \""+found.getOwner().getName()+"\"!");
		
		GamePlayer player = game.findPlayerWithName(player_name);
		if (player==null)
			throw new Exception("Could not find player with name "+player_name+"!");
		found.setOwner(player);
		return found;
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"changeowner\":");
		case PLAYER:
			return message.length()>0 && message.charAt(0)==RoboToyServerController.TAKE_ROBOT;
		default:
			return false;
		}
	}

	public static RobotSummary parse(String json) {
		ChangedOwner chg = JSONUtils.fromJSON(json, ChangedOwner.class);
		return (chg==null) ? null : chg.changeowner;
	}

	public static class ChangedOwner {
		private RobotSummary changeowner;
		public RobotSummary getChangeowner() {
			return changeowner;
		}
		public void setChangeowner(RobotSummary changeowner) {
			this.changeowner = changeowner;
		}
	}
}

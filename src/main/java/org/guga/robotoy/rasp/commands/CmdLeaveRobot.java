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
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * Command sent by one player to its controlling robot telling to leave a robot (i.e. the robot won't
 * be controlled anymore by that player)
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdLeaveRobot implements CommandWithBroadcast<GameRobot> {

	private static final Logger log = Logger.getLogger(CmdLeaveRobot.class.getName());

	public static GameRobot run(RoboToyServerContext context,WebSocketActiveSession player_session,String robot_id,InetAddress robot_address) throws Exception {
		
		synchronized (CmdTakeRobot.SYNCHRONIZATION_OBJECT) {
			
			// Check if player really owns the indicated robot
			String player_name = RoboToyServerContext.getPlayerName(player_session);
			GameRobot robot = context.getGame().findRobotWithIdentifier(robot_id);
			if (robot==null)
				throw new Exception("Could not find robot with identifier "+robot_id+"!");
			if (robot.getOwner()==null) {
				// Nobody owns this robot, but will just reinforce it
				
				return robot.cloneIfLocalAddress(robot_address);
			}			
			else if (robot.getOwner().getName()!=null && !robot.getOwner().getName().equals(player_name)) {
				throw new Exception("This robot is owned by \""+robot.getOwner().getName()+"\"!");
			}
			else {
				robot.setOwner(null);
				
				return robot.cloneIfLocalAddress(robot_address);
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
			GameRobot target_robot = CmdLeaveRobot.run(context,session,robot_id,session.getLocalAddress());
			return target_robot;
			
		}
		
		return null;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,GameRobot robot) {
		// Same as 'CmdTakeRobot' (owner will be NULL in this case)
		StringBuilder message = new StringBuilder();
		message.append("{\"changeowner\":");
		message.append(RobotSummary.getRobotInfo(robot));
		message.append("}");
		return message.toString();
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		switch (issuer) {
		case PLAYER:
			return message.length()>0 && message.charAt(0)==RoboToyServerController.LEAVE_ROBOT;
		default:
			return false; // never directly called by a robot
		}
	}

}

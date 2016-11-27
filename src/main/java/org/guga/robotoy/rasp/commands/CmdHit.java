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
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Message broadcast by one robot to other robots or players notifying that some robot
 * was hit by broadcasting robot's fire.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdHit implements CommandWithBroadcast<CmdHit.Hit> {

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		
		if (CommandIssuer.ROBOT.equals(issuer)) {
			Hit hit = parse(message);
			if (hit!=null) {
				GameRobot robot_hit = context.getGame().findRobotWithIdentifier(hit.getHit().getId());
				GameRobot robot_source = context.getGame().findRobotWithIdentifier(hit.getSource().getId());
				CmdHit.evaluateHit(robot_hit, robot_source, context);
			}
			return hit;
		}
		
		return null;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,CmdHit.Hit hit) {
		StringBuilder message = new StringBuilder();
		message.append("{\"hit\":");
		message.append(JSONUtils.toJSON(hit.getHit(),false));
		message.append(",\"source\":");
		message.append(JSONUtils.toJSON(hit.getSource(),false));
		message.append(",\"fatal\":");
		message.append(hit.isFatal());
		message.append("}");
		return message.toString();
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		switch (issuer) {
		case ROBOT:
			return message.startsWith("{\"hit\":");
		default:
			return false;
		}
	}

	public static Hit parse(String json) {
		Hit hit = JSONUtils.fromJSON(json, Hit.class);
		return hit;
	}

	public static class Hit {
		private RobotSummary hit;
		private RobotSummary source;
		private boolean fatal;
		public RobotSummary getHit() {
			return hit;
		}
		public void setHit(RobotSummary hit) {
			this.hit = hit;
		}
		public RobotSummary getSource() {
			return source;
		}
		public void setSource(RobotSummary source) {
			this.source = source;
		}
		public boolean isFatal() {
			return fatal;
		}
		public void setFatal(boolean fatal) {
			this.fatal = fatal;
		}		
	}
	
	public static void evaluateHit(GameRobot hit,GameRobot shooter,RoboToyServerContext context) {
		if (hit.getLife()<=0)
			return; // already dead
		hit.decreaseLife(1);
		
		if (hit.getLife()==0) {
			// dead!
			if (hit.getAddress()==null) {
				// local robot
				if (context.getMotor()!=null && context.getMotor().isMoving()) {
					// stop moving
					context.getMotor().stop();
				}
			}
			shooter.increaseKills();
		}
	}
}

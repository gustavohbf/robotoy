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
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Command that affects overall robot's movement. It can
 * go forwards, backwards and rotate at different speeds
 * based on two factors varying between -1.0 and 1.0:<BR>
 * left factor: for the left side motors. Negative values tells to spin backwards.
 * Positive values tells to spin forwards. The absolute value tells the speed (0.0 = no speed,
 * 1.0 = full speed).<BR>
 * right factor: for the right side motors. Negative values tells to spin backwards.
 * Positive values tells to spin forwards. The absolute value tells the speed (0.0 = no speed,
 * 1.0 = full speed).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdSetMovement implements Command {

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return CommandIssuer.PLAYER.equals(issuer) && message.startsWith("{\"movement\":");
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		Factors factors = CmdSetMovement.parse(message);
		if (factors==null)
			return null;
		context.getMotor().setMovement(
				Math.min(1.0, Math.max(-1.0, factors.getLeft())), 
				Math.min(1.0, Math.max(-1.0, factors.getRight())));
		return null;
	}
	
	public static Factors parse(String json) {
		SetMovement parsed = JSONUtils.fromJSON(json, SetMovement.class);
		return (parsed==null) ? null : parsed.movement;
	}

	public static class SetMovement {
		private Factors movement;
		public Factors getMovement() {
			return movement;
		}
		public void setMovement(Factors movement) {
			this.movement = movement;
		}		
	}

	public static class Factors {
		private double left;
		private double right;
		public double getLeft() {
			return left;
		}
		public void setLeft(double left) {
			this.left = left;
		}
		public double getRight() {
			return right;
		}
		public void setRight(double right) {
			this.right = right;
		}		
	}
}

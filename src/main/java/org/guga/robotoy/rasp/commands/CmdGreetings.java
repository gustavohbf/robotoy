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
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Message sent by one robot to another or by a connected player to this robot.<BR>
 * <BR>
 * This message may also be sent by a connected player in order to assign a HTTP session id
 * with the corresponding WebSocket connection.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdGreetings implements Command {

	@Override
	public String getHelp() {
		return RoboToyServerController.GREETINGS+" - Send greetings from a robot. This command must be issued by a robot.\n"
			+ "{\"greetings\":<player>} - Send  greetings from a player. This command must be issued by a player.";
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.length()>0 
			&& ((message.charAt(0)==RoboToyServerController.GREETINGS && CommandIssuer.ROBOT.equals(issuer))
				|| (message.startsWith("{\"greetings\"") && CommandIssuer.PLAYER.equals(issuer)));
	}
	
	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		if (CommandIssuer.ROBOT.equals(issuer)) {
			// do nothing
		}
		if (CommandIssuer.PLAYER.equals(issuer)) {
			GreetingsFromPlayer contents = JSONUtils.fromJSON(message, GreetingsFromPlayer.class);
			if (contents!=null) {
				String sessionId = contents.getGreetings();
				GamePlayer player = context.getGame().findPlayerWithSessionId(sessionId);
				if (player!=null) {
					player.setPort(session.getRemotePort());
				}
			}
		}
		return null;
	}
	
	public static class GreetingsFromPlayer {
		private String greetings;
		public String getGreetings() {
			return greetings;
		}
		public void setGreetings(String greetings) {
			this.greetings = greetings;
		}		
	}

}

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
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Command received from another robot while in INIT stage if some user
 * connected elsewhere just changed his name.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdChangeName implements CommandWithBroadcast<CmdChangeName.ChangeNameEvent> {

	@Override
	public String getHelp() {
		return "{\"changename\":{\"oldname\":<old name>,\"newname\":<new name>}} - Change a player's name. This command must be issued by a robot.";
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return (message.startsWith("{\"changename\"") && CommandIssuer.ROBOT.equals(issuer));
	}

	@Override
	public CmdChangeName.ChangeNameEvent parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		if (CommandIssuer.ROBOT.equals(issuer)) {
			ChangeNameEvent event = JSONUtils.fromJSON(message, ChangeNameEvent.class);
			if (event!=null 
					&& event.getChangename()!=null 
					&& event.getChangename().getOldname()!=null 
					&& event.getChangename().getNewname()!=null) {
				
				// Updates internal players table
				GamePlayer player = context.getGame().findPlayerWithName(event.getChangename().getOldname());
				if (player!=null) {
					player.setName(event.getChangename().getNewname());
					context.updateWebSocketReferencesAfterNameChanged(event.getChangename().getOldname(), event.getChangename().getNewname());
				}
				return event;
			}
		}
		return null;
	}
		
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,CmdChangeName.ChangeNameEvent event) {
		return JSONUtils.toJSON(event, false);
	}

	@Override
	public String getBroadcastExcludePath(ChangeNameEvent object) {
		return RoboToyServerContext.getWSPathWithPlayerName(object.getChangename().getNewname());
	}

	/**
	 * Broadcasts this message only to connected players
	 */
	@Override
	public boolean hasBroadcastToRobots() {
		return false;
	}

	public static class ChangeNameEvent {
		private ChangeName changename;

		public ChangeName getChangename() {
			return changename;
		}

		public void setChangename(ChangeName changename) {
			this.changename = changename;
		}
	}
	
	public static class ChangeName {
		private String oldname;
		private String newname;
		
		public String getOldname() {
			return oldname;
		}

		public void setOldname(String oldname) {
			this.oldname = oldname;
		}

		public String getNewname() {
			return newname;
		}

		public void setNewname(String newname) {
			this.newname = newname;
		}

	}
}


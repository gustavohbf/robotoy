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
package org.guga.robotoy.rasp.tags;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameState;

/**
 * Custom tag used in CHANGE NAME PAGE.<BR>
 * Tries to change current player's name.<BR>
 * Check if the new name is already being used.<BR>
 * <BR>
 * Parameters:<BR>
 * ===========<BR>
 * userName: new name provided by user<BR>
 * change: flag indicating if it should try to change name or not (in other words, tells if it's a form submition)<BR>
 * alertProperty: name of page-scoped variable to get alert messages, if any<BR>
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
public class ChgNameTag extends RoboToyCommonTag {

	private static final Logger log = Logger.getLogger(ChgNameTag.class.getName());
	
	private boolean change;

	private String userName;
	
	private String alertProperty;
	
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public boolean isChange() {
		return change;
	}

	public void setChange(boolean change) {
		this.change = change;
	}

	public String getAlertProperty() {
		return alertProperty;
	}

	public void setAlertProperty(String alertProperty) {
		this.alertProperty = alertProperty;
	}

	@Override
	public void doTag() throws JspException, IOException {
		GameState game = assertGame();

		String user_address = getRequest().getRemoteAddr();
		GamePlayer current_player = game.findPlayerWithAddress(user_address); 
		if (current_player==null) {
			// If current player is not in our local database, let's redirect to login page
			String forward_to = assertController().getContext().getCurrentGamePage(getSession().getId());
			redirect(forward_to);
			return;			
		}

		// Current logged name
		String current_name = (String)getSession().getAttribute("USERNAME");
		if (current_name==null) {
			// Could not find player's name in this session. Let's check in our local database.
			current_name = current_player.getName();
			getSession().setAttribute("USERNAME", current_name);
		}		
		
		// Process submission on LOGIN PAGE
		
		if (change && userName!=null && userName.trim().length()>0) {
			userName = userName.trim();
			// Check if there was any change
			if (userName.equals(current_name)) {
				// No change at all, let's just return.
				String forward_to = game.getStage().getDefaultPage();
				forward(forward_to);
				return;			
			}
			// Name changed, let's check if new name is already in use by some other player
			GamePlayer player_with_same_name = game.findPlayerWithName(userName);
			if (player_with_same_name!=null) {
				// This name was already taken
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING,"Player from address "+user_address+" tried to set his name to '"+userName
							+"', but this name is already being used by player at address "+player_with_same_name.getAddressString());
				}
				if (alertProperty!=null) {
					getPageContext().setAttribute(alertProperty, "This name is already being used! Please choose another one.");
				}				
			}
			else {
				// This name is not in use. Let's update our local database and broadcast to the others
				String previousName = current_player.getName();
				current_player.setName(userName);
				assertController().getContext().updateWebSocketReferencesAfterNameChanged(previousName, userName);
				try {
					assertController().broadcastNameChanged(current_player,previousName,userName);
				} catch (Exception e) {
					getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while broadcasting name change!");
					log.log(Level.SEVERE, "Error while broadcasting name change to '"+userName+"'!", e);
					return;
				}
				getSession().setAttribute("USERNAME",userName);
				String forward_to = game.getStage().getDefaultPage();
				forward(forward_to);
				return;			
			}
		}
		
		// If got here, will show regular CHANGE NAME PAGE
	}

}

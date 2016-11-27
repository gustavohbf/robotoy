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
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.SkipPageException;

import org.guga.robotoy.rasp.commands.CmdAddNewPlayer;
import org.guga.robotoy.rasp.commands.CmdTakeRobot;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameState;

/**
 * Custom tag used in LOGIN PAGE.<BR>
 * Tries to create a new 'GamePlayer' using provided name.<BR>
 * Check if name is already being used.<BR>
 * Check if user was already logged in.<BR>
 * <BR>
 * Parameters:<BR>
 * ===========<BR>
 * userName: name provided by user<BR>
 * alertProperty: name of page-scoped variable to get alert messages, if any<BR>
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
public class LoginPageTag extends RoboToyCommonTag {
	
	private static final Logger log = Logger.getLogger(LoginPageTag.class.getName());

	private String userName;
	
	private String alertProperty;
	
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
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
		
		// Process submission on LOGIN PAGE
		
		if (userName!=null && userName.trim().length()>0) {
			userName = userName.trim();
			GamePlayer player = game.findPlayerWithName(userName);
			String user_address = getRequest().getRemoteAddr();
			if (player!=null) {
				String player_address = player.getAddressString();
				if (user_address.equalsIgnoreCase(player_address)) {
					// Already logged in (maybe got disconnected for a while)
					getSession().setAttribute("USERNAME",player.getName());
					String forward_to = game.getStage().getDefaultPage();
					checkForAdminUser(player.getName());
					redirect(forward_to);
					return;
				}
				else {
					// This name was already taken
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING,"Player from address "+user_address+" tried to set his name to '"+userName
								+"', but this name is already being used by player at address "+player_address);
					}
					if (alertProperty!=null) {
						getPageContext().setAttribute(alertProperty, "This name is already being used! Please choose another one.");
					}
				}
			}
			else {
				player = game.findPlayerWithAddress(user_address); 
				if (player!=null) {
					// Already logged in (maybe got disconnected for a while or is changing his name)
					String previousName = player.getName();
					player.setName(userName);
					assertController().getContext().updateWebSocketReferencesAfterNameChanged(previousName, userName);
					try {
						assertController().broadcastNameChanged(player,previousName,userName);
					} catch (Exception e) {
						getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while broadcasting name change!");
						log.log(Level.SEVERE, "Error while broadcasting name change to '"+userName+"'!", e);
						return;
					}
					getSession().setAttribute("USERNAME",player.getName());
					String forward_to = game.getStage().getDefaultPage();
					checkForAdminUser(player.getName());
					redirect(forward_to);
					return;			
				}
				else {
					player = new GamePlayer();
					player.setName(userName);
					player.setAddress(InetAddress.getByName(user_address));
					player.setHttpSession(getRequest().getSession().getId());
					player.setOnline(true);
					game.addPlayer(player);
					try {
						assertController().broadcastCommand(new CmdAddNewPlayer(), player,/*includingRobots*/true);
					} catch (Exception e) {
						getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while broadcasting new player!");
						log.log(Level.SEVERE, "Error while broadcasting new player '"+userName+"'!", e);
						return;
					}
					if (game.findRobotWithOwnerName(userName)==null) {
						// If this player does not have a robot yet, check if
						// there is a robot without a owner. Will prefer a robot
						// that had this same user as previous owner
						synchronized (CmdTakeRobot.SYNCHRONIZATION_OBJECT) {
							GameRobot candidate = null;
							for (GameRobot robot:game.getRobots()) {
								if (robot.getOwner()!=null)
									continue; // already taken
								if (robot.getPreviousOwner()!=null 
										&& robot.getPreviousOwner().equalsIgnoreCase(userName)) {
									candidate = robot;
									break;
								}
								if (candidate==null) {
									candidate = robot;
								}
							}
							if (candidate!=null) {
								// Got a robot candidate for this new player. Let's assign it.
								try {
									GameRobot[] target_robots = CmdTakeRobot.run(getController().getContext(),userName,candidate.getIdentifier(),InetAddress.getByName(getRequest().getLocalAddr()));
									if (target_robots!=null && target_robots.length>0) {
										for (GameRobot target_robot:target_robots) {
											assertController().broadcastCommand(new CmdTakeRobot(), target_robot,/*includingRobots*/true);
										}
									}
								} catch (Exception e) {
									log.log(Level.SEVERE, "Error while broadcasting ownership of available robot '"+candidate.getIdentifier()+"' to user '"+userName+"'!", e);
									// will ignore in the event of error
								}
							}
						}
					}
					getSession().setAttribute("USERNAME",player.getName());
					String forward_to = game.getStage().getDefaultPage();
					checkForAdminUser(player.getName());
					redirect(forward_to);
					return;			
				}
			}
		}
		
		// If got here, will show regular LOGIN PAGE
	}
	
	private void checkForAdminUser(String userName) throws SkipPageException, IOException {
		if (userName==null)
			return;
		String adminUserName = assertController().getAdminUserName();
		if (adminUserName==null)
			return;
		if (adminUserName.equalsIgnoreCase(userName)) {
			// Should redirect admin to special page
			getResponse().sendRedirect("options.jsp");
			throw new SkipPageException();
		}
	}
}

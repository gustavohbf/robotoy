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
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.guga.robotoy.rasp.commands.CmdAddNewPlayer;
import org.guga.robotoy.rasp.game.GamePlayMode;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.game.GameState;
import org.guga.robotoy.rasp.utils.URLUtils;

/**
 * Tag used in almost all game pages (except login page) in order to assure
 * the user is logged in (except if running in StandAlone mode).<BR>
 * Also redirects the user to the robot owned by him if game is already in
 * PLAY stage.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class AccessTag extends RoboToyCommonTag {

	private static final Logger log = Logger.getLogger(AccessTag.class.getName());

	@Override
	public void doTag() throws JspException, IOException {

		GameState game = assertGame();
		GamePlayer player;
		
		// Whenever running as 'stand alone', will not check user name (it's 'single player')
		if (GamePlayMode.STANDALONE.equals(assertController().getContext().getGamePlayMode())) {
			return;
		}

		// Check session attribute
		String name = (String)getSession().getAttribute("USERNAME");
		if (name!=null) {
			// session is present, check if player is listed in game database
			player = game.findPlayerWithName(name);
			if (player==null) {
				// player is not listed
				// maybe got disconnected for a while
				// if current game stage is INIT, will add player automatically
				if (GameStage.INIT.equals(game.getStage())) {
					String user_address = getRequest().getRemoteAddr();
					player = new GamePlayer();
					player.setName(name);
					player.setAddress(InetAddress.getByName(user_address));
					player.setHttpSession(getRequest().getSession().getId());
					player.setOnline(true);
					game.addPlayer(player);
					try {
						assertController().broadcastCommand(new CmdAddNewPlayer(), player,/*includingRobots*/true);
					} catch (Exception e) {
						getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while broadcasting new player!");
						log.log(Level.SEVERE, "Error while broadcasting new player '"+name+"'!", e);
						return;
					}
				}
			}
			
			if (player==null) {
				// Not logged in, forwards to login page
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE,"Forwarding player at "+getRequest().getRemoteAddr()
							+" from "+getRequest().getRequestURI()+" to login.jsp because game is in stage "
							+game.getStage()+" and he is not in game with name "+name);
				forward("login.jsp");
				return;
			}
		}
		else {
			// Session is lost, check if game is already aware of user by its address
			String user_address = getRequest().getRemoteAddr();
			player = game.findPlayerWithAddress(user_address);
			if (player==null) {
				// Not logged in, forwards to login page
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE,"Forwarding player at "+getRequest().getRemoteAddr()
							+" from "+getRequest().getRequestURI()+" to login.jsp because he got no session and his address is not in game database");
				forward("login.jsp");
				return;
			}
			else {
				// recover username
				name = player.getName();
				getSession().setAttribute("USERNAME", name);
			}
		}
		
		// Check if already in PLAY stage
		if (GameStage.PLAY.equals(game.getStage())) {
			// Assure player is in his own robot			
			GameRobot robot = game.findRobotWithOwnerName(name);
			if (robot==null) {
				// Player does not own a robot
				String uri = getRequest().getRequestURI();
				if (!uri.endsWith("observer.jsp")) {
					if (log.isLoggable(Level.FINE))
						log.log(Level.FINE,"Forwarding player at "+getRequest().getRemoteAddr()
								+" from "+getRequest().getRequestURI()+" to observer.jsp because game has already started and he does not own a robot");
					forward("observer.jsp");
				}
				return;
			}
			
			InetAddress robot_address = robot.getAddress();
			if (robot_address==null) {
				// Owner currently logged in ourselves, so just leave
				return;
			}
			else {
				// It's another robot, so let's forward user into it
				String new_url = URLUtils.getURL(getRequest(), 
						/*replace_scheme*/null, 
						/*replace_server*/robot_address.getHostAddress(), 
						/*replace_port*/0, 
						/*replace_context*/null, 
						/*replace_servlet*/null, 
						/*replace_path*/null, 
						/*replace_query*/"username="+URLEncoder.encode(name, "UTF-8"));
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST,"Redirecting player '"+player.getName()
					+"' from "+player.getAddressString()
					+" to robot at "+robot_address.getHostAddress()
					+" with URL "+new_url);
				redirect(new_url);
				return;
			}
		}
	}
	
}

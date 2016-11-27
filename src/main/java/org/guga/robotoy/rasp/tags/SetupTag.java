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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GamePlayMode;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameStart;
import org.guga.robotoy.rasp.game.GameState;
import org.guga.robotoy.rasp.utils.InetUtils;

/**
 * Custom tag used in SETUP PAGE.<BR>
 * Parse form submission.<BR>
 * <BR>
 * Parameters:<BR>
 * ===========<BR>
 * alertProperty: name of page-scoped variable to get alert messages, if any<BR>
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
public class SetupTag extends RoboToyCommonTag {

	private static final Logger log = Logger.getLogger(SetupTag.class.getName());
	
	private static final String DEFAULT_GUEST_PLAYER_NAME = "Guest";

	private String alertProperty;

	public String getAlertProperty() {
		return alertProperty;
	}

	public void setAlertProperty(String alertProperty) {
		this.alertProperty = alertProperty;
	}

	@Override
	public void doTag() throws JspException, IOException {
		RoboToyServerController controller = assertController();
		// Check if current mode is different from expected mode
		if (!GamePlayMode.STANDALONE.equals(controller.getContext().getGamePlayMode())) {
			getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "This resource is not acceptable in current mode!");
			log.log(Level.SEVERE, "Access denied to request from "+getRequest().getRemoteAddr());
			return;
		}
				
		String cmd = (String)getRequest().getParameter("cmd");
		
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "User "+getRequest().getRemoteAddr()+" requested "+getRequest().getRequestURI()+" with parameter cmd="+cmd);
		}
		
		if (cmd==null || cmd.trim().length()==0) {
			// It's not a submission, so let's just return
			return;
		}
		
		switch (cmd) {
		case "testdrive":
			doTestDrive();
			break;
		case "config":
			doConfig();
			break;
		case "reboot":
			doReboot();
			break;
		default:
			if (alertProperty!=null) {
				getPageContext().setAttribute(alertProperty, "Invalid request!");
				log.log(Level.SEVERE, "Invalid request from "+getRequest().getRemoteAddr()+". cmd = "+cmd);
			}
		}
	}
	
	private void doTestDrive() throws JspException, IOException {
		// Create a guest player if not logged in
		String user_address = getRequest().getRemoteAddr();
		GameState game = assertGame();
		GamePlayer player = game.findPlayerWithAddress(user_address);
		if (player==null) {
			// Pick a name not being used
			String username = (String)getSession().getAttribute("USERNAME");
			if (username==null || username.trim().length()==0) {
				username = DEFAULT_GUEST_PLAYER_NAME;
			}
			player = game.findPlayerWithName(username);
			if (player!=null && player.isOnline()) {
				for (int seq=2;;seq++) {
					String newname = username+new DecimalFormat("00").format(seq);
					if ((player=game.findPlayerWithName(newname))==null || !player.isOnline()) {
						username = newname;
						break;
					}
				}				
			}
			player = new GamePlayer();
			player.setName(username);
			player.setAddress(InetAddress.getByName(user_address));
			player.setHttpSession(getRequest().getSession().getId());
			player.setOnline(true);
			game.addPlayer(player);
		}
		getSession().setAttribute("USERNAME",player.getName());
		GameStart.startTestDrive(game, player);
		String forward_to = game.getStage().getDefaultPage();
		redirect(forward_to);
		return;			
	}


	private void doConfig() throws JspException, IOException {
		String ssid = getRequest().getParameter("ssid");
		if (ssid==null || ssid.trim().length()==0) {
			if (alertProperty!=null) {
				getPageContext().setAttribute(alertProperty, "Missing network name!");
			}
			return;
		}
		String auth = getRequest().getParameter("auth");
		if (auth==null || auth.trim().length()==0) {
			if (alertProperty!=null) {
				getPageContext().setAttribute(alertProperty, "Missing authentication mode!");
			}
			return;
		}
		String p;
		if (!auth.equalsIgnoreCase("none")) {
			p = getRequest().getParameter("p");
			if ((p==null || p.trim().length()==0)) {
				if (alertProperty!=null) {
					getPageContext().setAttribute(alertProperty, "Missing password!");
				}
				return;
			}
		}
		else {
			p = null;
		}
		// Change network settings (will be considered in next reboot)
		String psk;
		try {
			psk = (p!=null) ? InetUtils.genPSK(ssid, p) : null;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while changing configuration!",e);
			if (alertProperty!=null) {
				getPageContext().setAttribute(alertProperty, "Error while changing configuration!");
			}			
			return;
		}
		String contents = InetUtils.makeNetConfiguration(ssid, /*hidden_ssid*/true, psk);
		File config_file = new File(InetUtils.DEFAULT_PATH_TO_CONFIG_FILES+File.separator+InetUtils.DEFAULT_NETWORK_CONFIG_FILENAME);
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Writing configuration file: "+config_file.getAbsolutePath());
		try (OutputStream output = new BufferedOutputStream(new FileOutputStream(config_file));) {
			output.write(contents.getBytes("UTF-8"));
		}
		// Change interface config file if necessary
		if (!InetUtils.isNetInterfacesFileConfigured(InetUtils.DEFAULT_AP_INTERFACE)) {
			String inet_config = InetUtils.makeNetInterfacesFile();
			File inet_config_file = new File(InetUtils.DEFAULT_NETWORK_INTERFACES_FILE);
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "Writing network interfaces file: "+inet_config_file.getAbsolutePath());			
			try (OutputStream output = new BufferedOutputStream(new FileOutputStream(inet_config_file));) {
				output.write(inet_config.getBytes("UTF-8"));
			}
		}
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Changed configuration file for SSID ("+ssid+"). Need restart.");
		if (alertProperty!=null) {
			getPageContext().setAttribute(alertProperty, "Changed configuration. Need reboot!");
		}
	}


	private void doReboot() throws JspException, IOException {
		ProcessBuilder builder = new ProcessBuilder("reboot");
		builder.start();
		System.exit(0);
	}
}

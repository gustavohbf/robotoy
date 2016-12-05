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
package org.guga.robotoy.rasp.network;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.AutoDiscoverService.ClientCallback;

/**
 * Will get called back whenever finds another RoboToy in local network
 * @author Gustavo Figueiredo
 */
public class AutoDiscoveryRobotsCallback implements ClientCallback {
		
	private static final Logger log = Logger.getLogger(AutoDiscoveryRobotsCallback.class.getName());

	public static final String prefixAutoDiscoveryAnswer = "I'm trully a Robotoy";
	public static final Pattern patternAutoDiscoveryAnswer = Pattern.compile("^"+Pattern.quote(prefixAutoDiscoveryAnswer)+":(.*)$");
	
	private int port = WebServer.DEFAULT_PORT;
	private int portSecure = 0;

	/**
	 * This class wraps minimal information about another point in
	 * local network already acknownledged by this robot as being
	 * another robot.
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	private static class KnownRobot {
		private final String remoteAddress;
		private final String answerReceived;
		KnownRobot(String addr,String answer) {
			this.remoteAddress = addr;
			this.answerReceived = answer;
		}
		public String toString() {
			return remoteAddress + answerReceived;
		}
		public int hashCode() {
			return 17 + 37 * (remoteAddress.hashCode() + 37 * answerReceived.hashCode());
		}
		public boolean equals(Object o) {
			if (this==o)
				return true;
			if (!(o instanceof KnownRobot))
				return false;
			if (!remoteAddress.equals(((KnownRobot)o).remoteAddress))
				return false;
			if (!answerReceived.equals(((KnownRobot)o).answerReceived))
				return false;
			return true;
		}
	}

	private final RoboToyServerController controller;

	private final Set<KnownRobot> knownAddressesWithAnswers;
	
	public AutoDiscoveryRobotsCallback(RoboToyServerController controller) {
		this.controller = controller;
		this.knownAddressesWithAnswers = Collections.synchronizedSet(new HashSet<>());
	}
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPortSecure() {
		return portSecure;
	}

	public void setPortSecure(int portSecure) {
		this.portSecure = portSecure;
	}

	/**
	 * This method gets called when our UDP packet is answered by another robot.
	 */
	@Override
	public void onConnection(InetAddress remoteAddress, int remotePort,String answerReceived) {
		KnownRobot check = new KnownRobot(remoteAddress.getHostAddress(),answerReceived);
		if (knownAddressesWithAnswers.contains(check))
			return;
		knownAddressesWithAnswers.add(check);
		if (log.isLoggable(Level.INFO))
			log.log(Level.INFO,"Found another RoboToy at "+remoteAddress.toString());
		Matcher m = patternAutoDiscoveryAnswer.matcher(answerReceived);
		if (!m.find()) {
			log.log(Level.SEVERE,"Unexpected answer: "+answerReceived);
			return;
		}
		String robot_id = m.group(1);
		int ws_port = port;
		// Find out if we already knew about this robot's address, but with a different id (maybe the robot restarted)
		final RoboToyServerContext context = controller.getContext();
		final GameRobot robot_with_same_addr = context.getGame().findRobotWithAddress(remoteAddress.getHostAddress());
		if (robot_with_same_addr!=null) {
			// Let's update our local database
			robot_with_same_addr.setIdentifier(robot_id);
			sendGreetingsToRobot(robot_with_same_addr,remoteAddress.getHostAddress(),ws_port,
				()->{
					// on success...
					try {
						// broadcast new id to connected players
						controller.broadcastNewRobotIdentification(robot_with_same_addr);
					} catch (Exception ex) {
						log.log(Level.SEVERE, "Could not broadcast new robot identification "+remoteAddress.getHostAddress(), ex);
					}
				});
		}
		else {
			// Add the new robot to our local in-memory database
			final GameRobot new_robot = GameRobot.newRobotWithAddress(robot_id,remoteAddress);
			context.getGame().addRobot(new_robot);
			sendGreetingsToRobot(new_robot,remoteAddress.getHostAddress(),ws_port,
				()->{
					// on success...
					try {
						// broadcast new robot to connected players
						controller.broadcastNewRobot(new_robot);
					} catch (Exception ex) {
						log.log(Level.SEVERE, "Could not broadcast new robot "+remoteAddress.getHostAddress(), ex);
					}
				});
			// If local robot is running as Access Point, add the other robot's address to the
			// exception list in local firewall iptables rules
			if (null!=context.getAccessPointMode()) {
				try {
					InetUtils.addByPassToCaptivePortal(context.getAccessPointMode().getAPName(),remoteAddress.getHostAddress(), ws_port, portSecure);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Could not setup access point's firewall rules for packets with destination to other robot: "
							+remoteAddress.getHostAddress(), e);
				}
			}
		}			
	}

	private void sendGreetingsToRobot(GameRobot robot,String address,int port,Runnable onSuccess) {
		final RoboToyServerContext context = controller.getContext();
		try {
			GameRobot local_robot = context.getGame().findLocalRobot();
			String my_id = (local_robot==null) ? "":local_robot.getIdentifier();
			context.getWebSocketPool().connect(address,port,"/ws/robot/"+my_id);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"Sending greetings from us to "+address+":"+port);
			}
			// Send greetings from us
			context.getWebSocketPool().sendMessage(address, /*port*/0, String.valueOf(RoboToyServerController.GREETINGS));
			// Query for connected players
			context.getWebSocketPool().sendMessage(address, /*port*/0,
				String.valueOf(RoboToyServerController.QUERY_PLAYERS),
				onSuccess,
				(e)->{
					// on failure...
					log.log(Level.SEVERE, "Could not send message to "+address, e);
					context.getGame().removeRobot(robot);
				});
			// Query for robot color
			context.getWebSocketPool().sendMessage(address, /*port*/0,
					String.valueOf(RoboToyServerController.QUERY_COLOR));
			// Query for robot current owner or previous owner
			context.getWebSocketPool().sendMessage(address, /*port*/0,
					String.valueOf(RoboToyServerController.QUERY_OWNER));
		} catch (Throwable e) {
			log.log(Level.SEVERE, "Could not connect to other RoboToy at "+address+" and port number "+port, e);
			context.getGame().removeRobot(robot);
		}
	}	
	
	public void removeKnownAddress(String address) {
		for (Iterator<KnownRobot> it = knownAddressesWithAnswers.iterator(); it.hasNext(); ) {
			KnownRobot kr = it.next();
			if (kr.remoteAddress.equals(address)) 
				it.remove();
		}
	}
	
}

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
package org.guga.robotoy.rasp.game;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SecureRandom;

import org.guga.robotoy.rasp.network.InetUtils;
import org.guga.robotoy.rasp.optics.LedColor;

/**
 * Current data about a robot. Could be ourselves, could be some other robot.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GameRobot implements Cloneable {

	/**
	 * IP address of remote RoboToy, or NULL if it's the local RoboToy
	 */
	private final InetAddress address;
	
	/**
	 * Unique identifier of this RoboToy. Should be unique in a game.<BR>
	 * It's assigned before game is started.
	 */
	private String identifier;
	
	/**
	 * Short identifier of this RoboToy. It's assigned at
	 * the start of the game. It's used for identifying who
	 * fired a beam detected on a robot receiver.
	 */
	private byte shortId;

	private GamePlayer owner;
	
	private String previousOwner;
	
	/**
	 * Indicates that the robot is ready to start playing the game. It's
	 * set to 'true' once the robot's owner clicks 'Start Game' button
	 * at lobby page. It's reset to 'false' once the game is restarted.
	 */
	private boolean ready;
	
	private int life;
	
	private int kills;
	
	private LedColor color;
	
	private GameControlMode controlMode;
	
	public static GameRobot newRobotWithAddress(String id,InetAddress address) {
		GameRobot robot = new GameRobot(address);
		robot.setIdentifier(id);
		robot.setControlMode(GameControlMode.BUTTONS);
		return robot;
	}
	
	public static GameRobot newLocalRobot(String id) {
		GameRobot robot = new GameRobot(/*address*/null);
		robot.setIdentifier(id);
		robot.setControlMode(GameControlMode.BUTTONS);
		return robot;
	}
	
	private GameRobot(InetAddress address) {
		this.address = address;
	}

	public GamePlayer getOwner() {
		return owner;
	}

	public void setOwner(GamePlayer owner) {
		this.owner = owner;
	}

	public String getPreviousOwner() {
		return previousOwner;
	}

	public void setPreviousOwner(String previousOwner) {
		this.previousOwner = previousOwner;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public int getLife() {
		return life;
	}

	public void setLife(int life) {
		this.life = life;
	}
	
	public void decreaseLife(int amount) {
		life -= amount;
	}

	public int getKills() {
		return kills;
	}

	public void setKills(int kills) {
		this.kills = kills;
	}
	
	public void increaseKills() {
		kills ++;
	}

	/**
	 * Indicates that the robot is ready to start playing the game. It's
	 * set to 'true' once the robot's owner clicks 'Start Game' button
	 * at lobby page. It's reset to 'false' once the game is restarted.
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * Indicates that the robot is ready to start playing the game. It's
	 * set to 'true' once the robot's owner clicks 'Start Game' button
	 * at lobby page. It's reset to 'false' once the game is restarted.
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public LedColor getColor() {
		return color;
	}

	public void setColor(LedColor color) {
		this.color = color;
	}

	public GameControlMode getControlMode() {
		return controlMode;
	}

	public void setControlMode(GameControlMode controlMode) {
		this.controlMode = controlMode;
	}

	/**
	 * IP address of remote RoboToy, or NULL if it's the local RoboToy
	 */
	public InetAddress getAddress() {
		return address;
	}
	
	public GameRobot clone() {
		GameRobot clone = new GameRobot(address);
		clone.identifier = identifier;
		clone.owner = owner;
		clone.life = life;
		clone.color = color;
		return clone;
	}
	
	public GameRobot cloneWithDifferentAddress(InetAddress address) {
		GameRobot clone = new GameRobot(address);
		clone.identifier = identifier;
		clone.owner = owner;
		clone.life = life;
		clone.color = color;
		return clone;
	}

	/**
	 * Clone local robot for broadcasting purposes. Otherwise, return this.
	 */
	public GameRobot cloneIfLocalAddress(InetAddress peer_address) {
		if (getAddress()==null)
			return cloneWithDifferentAddress(peer_address);
		else
			return this;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(identifier);
		sb.append("@");
		if (address==null)
			sb.append("localhost");
		else
			sb.append(address.getHostAddress());
		return sb.toString();
	}
	
	/**
	 * Short identifier of this RoboToy. It's assigned at
	 * the start of the game. It's used for identifying who
	 * fired a beam detected on a robot receiver.
	 */
	public byte getShortId() {
		return shortId;
	}

	/**
	 * Short identifier of this RoboToy. It's assigned at
	 * the start of the game. It's used for identifying who
	 * fired a beam detected on a robot receiver.
	 */
	public void setShortId(byte shortId) {
		this.shortId = shortId;
	}

	/**
	 * Use the following crazy heuristic:<BR>
	 * - If no network device is present, generate random string.<BR>
	 * - If there is a wireless device, use its MAC address<BR>
	 * - If there is another type of network device with some MAC address attached to it, except for ones
	 * with 'virtual' in its name, use its MAC address<BR>
	 * - Otherwise, generate random string.
	 */
	public static String getHardwareIdentifier() {
		byte[] mac = InetUtils.getMACAddress();
		if (mac==null || mac.length==0) {
			// Try again with another method
			try {
				mac = InetUtils.unformatMAC(InetUtils.getHWAddress(InetUtils.DEFAULT_WIFI_INTERFACE));
			} catch (Exception e) {
				// stay without MAC address
			}
			if (mac==null || mac.length==0)
				return getRandomIdentifier();
		}
		return InetUtils.formatMAC(mac);
	}
	
	private static String getRandomIdentifier() {
		final SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}
}

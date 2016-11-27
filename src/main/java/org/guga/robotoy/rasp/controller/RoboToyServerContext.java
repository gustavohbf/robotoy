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
package org.guga.robotoy.rasp.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import org.guga.robotoy.rasp.game.GamePlayMode;
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.game.GameState;
import org.guga.robotoy.rasp.motor.Motor;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.network.WebSocketClientPool;
import org.guga.robotoy.rasp.optics.BeamDevice;
import org.guga.robotoy.rasp.optics.RGBLed;
import org.guga.robotoy.rasp.utils.InetUtils;
import org.guga.robotoy.rasp.utils.SimpleLocalStorage;

/**
 * Context for the local hardware interface
 * and also for the current game status.
 * @author Gustavo Figueiredo
 *
 */
public class RoboToyServerContext {
	
	/**
	 * PWM level for motors in 'maximum speed' (number in interval [0.0 - 1.0])
	 */
	public static final double MAX_SPEED = 1.0;

	/**
	 * PWM level for motors in 'medium speed' (number in interval [0.0 - 1.0])
	 */
	public static final double MED_SPEED = 0.7;

	/**
	 * PWM level for motors in 'minimum speed' (number in interval [0.0 - 1.0])
	 */
	public static final double MIN_SPEED = 0.5;

	private Motor motor;
	
	private BeamDevice beamDevice;
	
	private RGBLed rgbLed;
		
	private double speed = MAX_SPEED;

	private final GameState game;
	
	private WebSocketClientPool webSocketPool;
	
	private SimpleLocalStorage localStorage;
	
	private GamePlayMode gamePlayMode = GamePlayMode.MULTIPLAYER;
	
	private InetUtils.WiFiModeEnum accessPointMode = null;
	
	private boolean takeStatistics = true;
	
	public RoboToyServerContext(GameState game) {
		this.game = game;
	}

	public Motor getMotor() {
		return motor;
	}

	public void setMotor(Motor motor) {
		this.motor = motor;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public GameState getGame() {
		return game;
	}

	public BeamDevice getBeamDevice() {
		return beamDevice;
	}

	public void setBeamDevice(BeamDevice beamDevice) {
		this.beamDevice = beamDevice;
	}

	public WebSocketClientPool getWebSocketPool() {
		return webSocketPool;
	}

	public void setWebSocketPool(WebSocketClientPool webSocketPool) {
		this.webSocketPool = webSocketPool;
	}
	
	public WebSocketActiveSession findRobotWebSocketSession(String robotId) {
		if (webSocketPool==null)
			return null;
		return webSocketPool.findSessionWithPath(getWSPathWithRobotIdentifier(robotId));
	}

	public WebSocketActiveSession findPlayerWebSocketSession(String playerName) {
		if (webSocketPool==null)
			return null;
		return webSocketPool.findSessionWithPath(getWSPathWithPlayerName(playerName));
	}

	public RGBLed getRGBLed() {
		return rgbLed;
	}

	public void setRGBLed(RGBLed rgbLed) {
		this.rgbLed = rgbLed;
	}

	public SimpleLocalStorage getLocalStorage() {
		return localStorage;
	}

	public void setLocalStorage(SimpleLocalStorage localStorage) {
		this.localStorage = localStorage;
	}

	public GamePlayMode getGamePlayMode() {
		return gamePlayMode;
	}

	public void setGamePlayMode(GamePlayMode gamePlayMode) {
		this.gamePlayMode = gamePlayMode;
	}
	
	public boolean isTakeStatistics() {
		return takeStatistics;
	}

	public void setTakeStatistics(boolean takeStatistics) {
		this.takeStatistics = takeStatistics;
	}

	public InetUtils.WiFiModeEnum getAccessPointMode() {
		return accessPointMode;
	}

	public void setAccessPointMode(InetUtils.WiFiModeEnum accessPointMode) {
		this.accessPointMode = accessPointMode;
	}

	/**
	 * Return the current game page considering current game stage and gameplay mode.<BR>
	 * It does not consider current user login status.
	 */
	public String getCurrentGamePage() {
		if (GameStage.INIT.equals(game.getStage())
				&& GamePlayMode.STANDALONE.equals(gamePlayMode)) {
			return "setup.jsp";
		}
		else {
			return game.getStage().getDefaultPage();
		}
	}

	/**
	 * Return the current game page considering current game stage and gameplay mode.<BR>
	 * It considers current user login status.
	 */
	public String getCurrentGamePage(String httpSessionId) {
		if (GameStage.INIT.equals(game.getStage())
				&& GamePlayMode.STANDALONE.equals(gamePlayMode)) {
			return "setup.jsp";
		}
		else {
			if (httpSessionId==null || null==game.findPlayerWithSessionId(httpSessionId))
				return "login.jsp";
			else
				return game.getStage().getDefaultPage();
		}
	}

	/**
	 * Changes all internal referentes to active WebSocket sessions that carries on old player's name
	 * after it has been changed.
	 */
	public void updateWebSocketReferencesAfterNameChanged(String previousName,String newName) {
		// We have to update the internal reference to requested path in this case because the player's name
		// is considered as a 'key' for identifying the corresponding active session object
		String newPlayerPath = RoboToyServerContext.getWSPathWithPlayerName(newName);
		String oldPlayerPath = RoboToyServerContext.getWSPathWithPlayerName(previousName);
		for (WebSocketActiveSession session:getWebSocketPool().getActiveSessions()) {
			if (session.getPath()!=null && session.getPath().equalsIgnoreCase(oldPlayerPath)) {
				session.setPath(newPlayerPath);
			}
		}		
	}
	
	/**
	 * Given all current active WebSocket sessions, check those related to 'robots' (not 'players') and
	 * returns their corresponding requested paths.
	 */
	public Set<String> getWebSocketReferencesForRobots() {
		Set<String> references = new HashSet<>();
		for (WebSocketActiveSession session:getWebSocketPool().getActiveSessions()) {
			if (isConnectedToRobot(session))
				references.add(session.getPath());
		}
		return references;
	}

	/**
	 * Given all current active WebSocket sessions, check those related to 'players' (not 'robots') and
	 * returns their corresponding requested paths.
	 */
	public Set<String> getWebSocketReferencesForPlayers() {
		Set<String> references = new HashSet<>();
		for (WebSocketActiveSession session:getWebSocketPool().getActiveSessions()) {
			if (isConnectedToPlayer(session))
				references.add(session.getPath());
		}
		return references;
	}

	/**
	 * Check if a given active WebSocket session is related to another Robot. If it's not, it could
	 * be related to a connected player.<BR>
	 * OBS: we can't tell if it's a robot or a player just by looking its IP address because there can be
	 * a situation where a robot is running as an 'access point' and is routing packages received
	 * by other players. For this reason we also check the requested path that originated this active session.
	 */
	public static boolean isConnectedToRobot(WebSocketActiveSession session) {
		if (session==null)
			return false;
		String path = session.getPath();
		return isConnectedToRobot(path);
	}

	public static boolean isConnectedToRobot(String session_path) {
		if (session_path==null)
			return false;
		return session_path.startsWith("/ws/robot/");
	}

	/**
	 * Check if a given active WebSocket session is related to a connected player. If it's not, it could
	 * be related to a connected robot.<BR>
	 * OBS: we can't tell if it's a robot or a player just by looking its IP address because there can be
	 * a situation where a robot is running as an 'access point' and is routing packages received
	 * by other players. For this reason we also check the requested path that originated this active session.
	 */
	public static boolean isConnectedToPlayer(WebSocketActiveSession session) {
		if (session==null)
			return false;
		String path = session.getPath();
		return isConnectedToPlayer(path);
	}

	public static boolean isConnectedToPlayer(String session_path) {
		if (session_path==null)
			return false;
		return session_path.startsWith("/ws/player/");
	}

	/**
	 * Check if a given active WebSocket session is related to some robot. If it's not, return NULL.<BR>
	 * Otherwise, return robot's identifier.
	 */
	public static String getRobotIdentifier(WebSocketActiveSession session) {
		if (session==null)
			return null;
		String path = session.getPath();
		return getRobotIdentifier(path);
	}

	public static String getRobotIdentifier(String session_path) {
		if (session_path==null)
			return null;
		final String prefix = "/ws/robot/";
		if (!session_path.startsWith(prefix))
			return null;
		return session_path.substring(prefix.length());
	}

	/**
	 * Check if a given active WebSocket session is related to some Player. If it's not, return NULL.<BR>
	 * Otherwise, return player's name.
	 */
	public static String getPlayerName(WebSocketActiveSession session) {
		if (session==null)
			return null;
		String path = session.getPath();
		return getPlayerName(path);
	}

	public static String getPlayerName(String session_path) {
		if (session_path==null)
			return null;
		final String prefix = "/ws/player/";
		if (!session_path.startsWith(prefix))
			return null;
		String name_url_encoded = session_path.substring(prefix.length());
		try {
			return URLDecoder.decode(name_url_encoded, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return a WebSocket request path for a given robot identifier.
	 */
	public static String getWSPathWithRobotIdentifier(String robotId) {
		return "/ws/robot/"+robotId;
	}
	
	/**
	 * Return a WebSocket request path for a given player name.
	 */
	public static String getWSPathWithPlayerName(String playerName) {
		try {
			return "/ws/player/"+URLEncoder.encode(playerName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}

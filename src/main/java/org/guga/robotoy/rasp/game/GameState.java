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

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.guga.robotoy.rasp.commands.CmdQueryPlayers;
import org.guga.robotoy.rasp.commands.PlayerSummary;

/**
 * This object keeps current game state, including all its players and robots.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GameState {
	
	/**
	 * Default maximum time (in miliseconds) that we will wait before declaring the game as 'abandoned'
	 * after every player has left the game.
	 */
	public static final int DEFAULT_ABANDONED_GAME_TIMEOUT_MS = 5000;
	
	public static final int DEFAULT_MAX_LIFE = 10;

	/**
	 * Current game stage
	 */
	private GameStage stage = GameStage.INIT;
	
	/**
	 * Date/time when game started
	 */
	private Date gameStart;
	
	/**
	 * Date/time when game stopped
	 */
	private Date gameStop;
	
	/**
	 * Maximum time (in miliseconds) that we will wait before declaring the game as 'abandoned'
	 * after every player has left the game.
	 */
	private int abandonedGameTimeout = DEFAULT_ABANDONED_GAME_TIMEOUT_MS;
	
	private int maxLife = DEFAULT_MAX_LIFE;
	
	/**
	 * Synchronized list of all players, including ones not directly connected to this robot
	 */
	private final List<GamePlayer> players;
	
	/**
	 * Synchronized list of all robots, including us.
	 */
	private final List<GameRobot> robots;
	
	/**
	 * List of known robot's addresses.
	 */
	private final Map<String,GameRobot> knownRobotAddresses;
	
	public GameState() {
		players = Collections.synchronizedList(new LinkedList<>());
		robots = Collections.synchronizedList(new LinkedList<>());
		knownRobotAddresses = new ConcurrentHashMap<>();
	}

	/**
	 * Maximum time (in miliseconds) that we will wait before declaring the game as 'abandoned'
	 * after every player has left the game.
	 */
	public int getAbandonedGameTimeout() {
		return abandonedGameTimeout;
	}

	/**
	 * Maximum time (in miliseconds) that we will wait before declaring the game as 'abandoned'
	 * after every player has left the game.
	 */
	public void setAbandonedGameTimeout(int abandonedGameTimeout) {
		this.abandonedGameTimeout = abandonedGameTimeout;
	}

	public int getMaxLife() {
		return maxLife;
	}

	public void setMaxLife(int maxLife) {
		this.maxLife = maxLife;
	}

	/**
	 * Current game stage
	 */
	public GameStage getStage() {
		return stage;
	}

	/**
	 * Current game stage
	 */
	public void setStage(GameStage stage) {
		this.stage = stage;
	}

	/**
	 * Date/time when game started
	 */
	public Date getGameStart() {
		return gameStart;
	}

	/**
	 * Date/time when game started
	 */
	public void setGameStart(Date gameStart) {
		this.gameStart = gameStart;
	}

	/**
	 * Date/time when game stopped
	 */
	public Date getGameStop() {
		return gameStop;
	}

	/**
	 * Date/time when game stopped
	 */
	public void setGameStop(Date gameStop) {
		this.gameStop = gameStop;
	}

	/**
	 * Synchronized list of all players, including ones not directly connected to this robot
	 */
	public List<GamePlayer> getPlayers() {
		return players;
	}

	public void addPlayer(GamePlayer player) {
		synchronized (players) {
			players.add(player);
		}
	}

	public void removePlayer(GamePlayer player) {
		players.remove(player);
	}

	public GamePlayer findPlayerWithAddress(String address) {
		return findPlayerWithAddress(address,/*port*/0);
	}
	
	public GamePlayer findPlayerWithAddress(String address,int port) {
		if (address==null || address.length()==0)
			return null;
		if (players.isEmpty())
			return null;
		for (GamePlayer player:players) {
			if (player.getAddress()==null)
				continue;
			if (port!=0 
					&& player.getPort()!=0 
					&& port!=player.getPort())
				continue;
			if (address.equalsIgnoreCase(player.getAddress().getHostAddress())
				|| address.equalsIgnoreCase(player.getAddress().getHostName()))
				return player;
		}
		return null;
	}
	
	public GamePlayer findPlayerWithName(String name) {
		if (name==null || name.length()==0)
			return null;
		if (players.isEmpty())
			return null;
		for (GamePlayer player:players) {
			if (player.getName()==null)
				continue;
			if (name.equalsIgnoreCase(player.getName()))
				return player;
		}
		return null;
	}
	
	public GamePlayer findPlayerWithSessionId(String sessionId) {
		if (sessionId==null || sessionId.length()==0)
			return null;
		if (players.isEmpty())
			return null;
		for (GamePlayer player:players) {
			if (player.getName()==null)
				continue;
			if (sessionId.equalsIgnoreCase(player.getHttpSession()))
				return player;
		}
		return null;
	}

	public List<GamePlayer> mergePlayers(CmdQueryPlayers.Players other_players) {
		if (other_players==null || other_players.getNumPlayers()==0)
			return null;
		List<GamePlayer> new_players = new LinkedList<>();
		synchronized (players) {
			for (PlayerSummary player:other_players.getPlayers()) {
				GamePlayer existing_player = findPlayerWithName(player.getName());
				if (existing_player!=null) {
					existing_player.setOnline(player.isOnline());
					continue;
				}
				GamePlayer new_player = new GamePlayer(player.getName(),player.getAddress(),player.getPort());
				new_player.setOnline(player.isOnline());
				new_players.add(new_player);
				addPlayer(new_player);
			}
		}
		return new_players;
	}
	
	public Set<String> getPlayersAddresses() {
		Set<String> addresses = new HashSet<>();
		for (GamePlayer player:players) {
			InetAddress addr = player.getAddress();
			if (addr==null)
				continue;
			addresses.add(addr.getHostAddress());
		}
		return addresses;
	}

	/**
	 * Synchronized list of all robots, including us.
	 */
	public List<GameRobot> getRobots() {
		return robots;
	}
	
	/**
	 * Return an ordered list of all robots in game, given
	 * their unique identifiers.
	 */
	public List<GameRobot> getOrderedRobots() {
		return robots.stream()
				.sorted(Comparator.comparing(GameRobot::getIdentifier))
				.collect(Collectors.toList());
	}

	public void addRobot(GameRobot robot) {
		robots.add(robot);
		if (robot.getAddress()!=null)
			knownRobotAddresses.put(robot.getAddress().getHostAddress(),robot);
	}
	
	public void removeRobot(GameRobot robot) {
		robots.remove(robot);
		if (robot.getAddress()!=null)
			knownRobotAddresses.remove(robot.getAddress().getHostAddress());
	}
	
	public GameRobot findLocalRobot() {
		if (robots.isEmpty())
			return null;
		for (GameRobot robot:robots) {
			if (robot.getAddress()==null)
				return robot;
		}
		return null;
	}
	
	public GameRobot findRobotWithIdentifier(String id) {
		if (robots.isEmpty() || id==null)
			return null;
		for (GameRobot robot:robots) {
			if (id.equals(robot.getIdentifier())) {
				return robot;
			}
		}
		return null;		
	}

	public GameRobot findRobotWithAddress(String address) {
		final boolean seek_local_robot = (address==null || address.length()==0);
		if (robots.isEmpty())
			return null;
		for (GameRobot robot:robots) {
			if (robot.getAddress()==null) {
				if (seek_local_robot)
					return robot;
			}
			else {
				if (seek_local_robot)
					continue;
				if (address.equals(robot.getAddress().getHostAddress())
						|| address.equals(robot.getAddress().getHostName()))
					return robot;
			}
		}
		return null;		
	}
	
	public GameRobot findRobotWithOwnerName(String name) {
		if (name==null || name.length()==0)
			return null;
		if (robots.isEmpty())
			return null;
		for (GameRobot robot:robots) {
			GamePlayer owner = robot.getOwner();
			if (owner==null)
				continue;
			if (name.equalsIgnoreCase(owner.getName()))
				return robot;
		}
		return null;
	}

	public GameRobot findRobotWithShortId(byte id) {
		if (robots.isEmpty())
			return null;
		for (GameRobot robot:robots) {
			if (id==robot.getShortId())
				return robot;
		}
		return null;
	}
	
	public boolean hasOtherRobots() {
		return !knownRobotAddresses.isEmpty();
	}

	public boolean hasRobotWithAddress(InetAddress address) {
		if (address!=null)
			return knownRobotAddresses.containsKey(address.getHostAddress());
		else
			return true; // local robot is always present
	}

	public boolean hasRobotWithAddress(String address) {
		if (address!=null)
			return knownRobotAddresses.containsKey(address);
		else
			return true; // local robot is always present
	}
	
	public Collection<String> getOtherRobotsAddresses() {
		return knownRobotAddresses.keySet();
	}
	
	/**
	 * Check if all known robots are ready to start a game.
	 */
	public boolean hasAllRobotsReady() {
		if (robots.isEmpty())
			return false;
		for (GameRobot robot:robots) {
			if (!robot.isReady())
				return false;
		}
		return true;
	}
	
	/**
	 * Check if all known players has already dismissed summary screen
	 */
	public boolean hasAllPlayersDismissedSummary(List<GamePlayer> outputPendingPlayers) {
		if (players.isEmpty())
			return false;
		for (GamePlayer player:players) {
			if (!player.isDismissedSummary() && player.isOnline()) {
				if (outputPendingPlayers==null)
					return false;
				else
					outputPendingPlayers.add(player);
			}
		}
		if (outputPendingPlayers==null)
			return true;
		else
			return outputPendingPlayers.isEmpty();
	}
	
	/**
	 * Check if player identified by the given name is the owner
	 * of local robot.
	 */
	public boolean isOwnerThisRobot(String player_name) {
		if (player_name==null)
			return false;
		GameRobot thisRobot = findRobotWithAddress(null);
		if (thisRobot==null)
			return false;
		GamePlayer owner = thisRobot.getOwner();
		if (owner==null)
			return false;
		return player_name.equalsIgnoreCase(owner.getName());
	}
}

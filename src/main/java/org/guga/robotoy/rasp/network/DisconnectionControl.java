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
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.commands.CmdPlayerDisconnected;
import org.guga.robotoy.rasp.commands.CmdRemovePlayer;
import org.guga.robotoy.rasp.commands.CmdRemoveRobot;
import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GameAbandoned;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStage;

/**
 * Additional control for the event of something getting disconnected (another
 * robot or some player).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DisconnectionControl implements RemovalCallback {

	private static final Logger log = Logger.getLogger(DisconnectionControl.class.getName());

	/**
	 * Will wait this time (in ms) before taking some action about
	 * robot or player being disconnected.
	 */
	private static final long DELAY_FOR_CLOSURE_OPERATIONS = 5000;

	/**
	 * Delayed queue for processing events of someone or something getting disconnected
	 */
	private final DelayQueue<CloseEvent> closingEventsQueue;
	
	private final RoboToyServerController controller;

	/**
	 * Object used for signaling events of connections being closed
	 */
	private static class CloseEvent implements Delayed {
		private final InetAddress remoteAddr;
		private final InetAddress localAddr;
		private final String path;
		private final long startTime;
		CloseEvent(InetAddress remoteAddr,InetAddress localAddr,String path) {
			this.remoteAddr = remoteAddr;
			this.localAddr = localAddr;
			this.path = path;
			this.startTime = System.currentTimeMillis() + DELAY_FOR_CLOSURE_OPERATIONS;
		}
		public String getPath() {
			return path;
		}
		public InetAddress getRemoteAddr() {
			return remoteAddr;
		}
		@SuppressWarnings("unused")
		public InetAddress getLocalAddr() {
			return localAddr;
		}
		@Override
		public int compareTo(Delayed o) {
			if (startTime<((CloseEvent)o).startTime)
				return -1;
			if (startTime>((CloseEvent)o).startTime)
				return +1;
			return 0;
		}
		@Override
		public long getDelay(TimeUnit unit) {
			long diff = startTime - System.currentTimeMillis();
			return unit.convert(diff, TimeUnit.MILLISECONDS);
		}		
	}

	public DisconnectionControl(RoboToyServerController controller) {
		this.controller = controller;
		this.closingEventsQueue = new DelayQueue<>();
		new CheckForClosedConnectionThread().start();
	}

	@Override
	public void onCloseConnection(WebSocketActiveSession session) {
		
		if (session.isStartedHere()) {
			if (log.isLoggable(Level.FINEST))
				log.log(Level.FINEST,"The session was started here, so will ignore this event.");
			return;
		}
		
		InetAddress remoteAddr = session.getRemoteAddress();
		InetAddress localAddr = session.getLocalAddress();
		
		String player_name = RoboToyServerContext.getPlayerName(session); // will be 'null' if 'session' refers to a robot
		
		// For safety reason, if robot is moving and player got disconnected, will stop
		// robot.
		if (player_name!=null
			&& controller.getContext()!=null
			&& controller.getContext().getMotor()!=null
			&& controller.getContext().getMotor().isMoving()
			&& controller.getContext().getGame()!=null
			&& controller.getContext().getGame().isOwnerThisRobot(player_name)) {
			if (log.isLoggable(Level.FINEST))
				log.log(Level.FINEST,"Got disconnected from "+remoteAddr+":"+session.getRemotePort()+", owner of this robot, so will stop immediately");
			controller.getContext().getMotor().stop();
		}
		// Give some time before processing this event
		// This way we can proper consider situations where user is just refreshing
		// his page, making the connection temporarily go down
		if (log.isLoggable(Level.FINEST))
			log.log(Level.FINEST,"Will check later: disconnection of path "+session.getPath());
		closingEventsQueue.add(new CloseEvent(remoteAddr,localAddr,session.getPath()));
	}

	/**
	 * Thread used for monitoring connections being closed. Will wait a certain delay
	 * before taking actions (such as removing a player from game).
	 * @author Gustavo Figueiredo
	 *
	 */
	private class CheckForClosedConnectionThread extends Thread {
		CheckForClosedConnectionThread() {
			super("CheckForClosedConnectionThread");
			setDaemon(true);
		}
		public void run() {
			try {
				while (true) {
					CloseEvent event = closingEventsQueue.take();
					String path = event.getPath();
					//InetAddress remoteAddr = event.getRemoteAddr();
					RoboToyServerContext context = controller.getContext();
					try {
						// If there is still another connection to the same peer, ignore this event
						if (context.getWebSocketPool().hasActiveSessionWithPath(path)) {
							if (log.isLoggable(Level.FINEST))
								log.log(Level.FINEST,"Got disconnected from a connection in "+event.getRemoteAddr()+" identified by '"+path
										+"', but there are other active connections to this path...");
							continue;
						}
						
						// While we are in INIT stage, we can remove disconnected robots or disconnected players
						if (GameStage.INIT.equals(context.getGame().getStage())) {
							if (RoboToyServerContext.isConnectedToRobot(path)) {
								// robot disconnected
								String robot_id = RoboToyServerContext.getRobotIdentifier(path);
								GameRobot robot = (robot_id==null) ? null : context.getGame().findRobotWithIdentifier(robot_id);
								if (robot!=null) {
									context.getGame().removeRobot(robot);
									// broadcasts event to all players
									try {
										controller.broadcastCommand(new CmdRemoveRobot(),robot,false);
									} catch (Exception e) {
										log.log(Level.SEVERE, "Error while broadcasting removal of robot at address "+event.getRemoteAddr().getHostAddress(), e);
									}
									// Try to connect again to this robot sometime
									controller.trackRobot(robot);
								}
								continue;
							}
							else if (RoboToyServerContext.isConnectedToPlayer(path)) {
								// player disconnected
								String player_name = RoboToyServerContext.getPlayerName(path);
								GamePlayer player = context.getGame().findPlayerWithName(player_name);
								if (player!=null) {
									context.getGame().removePlayer(player);
									GameRobot robot_owned_by_player = context.getGame().findRobotWithOwnerName(player_name);
									if (robot_owned_by_player!=null) {
										robot_owned_by_player.setOwner(null);
									}
									// broadcasts event to all players and robots
									try {
										controller.broadcastCommand(new CmdRemovePlayer(),player,/*includingRobots*/true);
									} catch (Exception e) {
										log.log(Level.SEVERE, "Error while broadcasting removal of player at address "+event.getRemoteAddr().getHostAddress(), e);
									}
								}
								continue;
							}
						}
						
						// While we are in PLAY stage, we won't remove disconnected players since they may return due to
						// network instability. But we will signal this event anyway so that we can tell when it's time to declare
						// the game as 'abandoned' (and also to prevent a running robot from going crazy).
						if (GameStage.PLAY.equals(context.getGame().getStage())) {
							if (RoboToyServerContext.isConnectedToPlayer(path)) {
								// player disconnected
								String player_name = RoboToyServerContext.getPlayerName(path);
								GamePlayer player = context.getGame().findPlayerWithName(player_name);
								if (player!=null) {
									GameRobot local_robot = context.getGame().findLocalRobot();
									if (local_robot!=null 
											&& local_robot.getOwner()!=null 
											&& local_robot.getOwner().getName().equalsIgnoreCase(player_name)) {
										// the local robot is owned by the disconnected player
										if (context.getMotor().isMoving()) {
											log.log(Level.INFO,"Stopped running robot because player '"+player_name+"' just got disconnected!");
											context.getMotor().stop();
										}
										player.setOnline(false);
										// broadcasts event to all players and robots
										try {
											controller.broadcastCommand(new CmdPlayerDisconnected(),player,/*includingRobots*/true);
										} catch (Exception e) {
											log.log(Level.SEVERE, "Error while broadcasting player disconnected at address "+event.getRemoteAddr().getHostAddress(), e);
										}								
									}
								}
								else {
									if (log.isLoggable(Level.FINEST))
										log.log(Level.FINEST,"Ignoring disconnection event with path "+path+" because there was no player with name "+player_name);
								}
								// After last player has left the game, we will start counting down before
								// declaring game over
								if (!GameAbandoned.hasEnoughOnlinePlayers(context.getGame())) {
									log.log(Level.WARNING, "There are no other online players! Will wait before declaring game over...");
									GameAbandoned.startWalkoverCountdown(controller);
								}
							}
							else {
								if (log.isLoggable(Level.FINEST))
									log.log(Level.FINEST,"Ignoring disconnection event with path "+path);
							}
						}
					}
					catch (Throwable e) {
						log.log(Level.SEVERE, "Error while processing closure event", e);
					}
	
				}
			}
			catch (InterruptedException e) {
				// just leave
			}
		}
	}
}

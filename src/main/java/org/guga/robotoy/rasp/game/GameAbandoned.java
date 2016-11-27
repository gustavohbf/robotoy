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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.commands.CmdStopGame;
import org.guga.robotoy.rasp.controller.RoboToyServerController;

/**
 * Some static methods used for declaring a game as abandoned.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GameAbandoned {

	private static final Logger log = Logger.getLogger(GameAbandoned.class.getName());

	public static boolean hasEnoughOnlinePlayers(GameState game) {
		final int minimal_count = (game.getPlayers().size()>1) ? 2 : 1;
		int count_online_players = 0;
		for (GamePlayer player:game.getPlayers()) {
			if (player.isOnline()) {
				if (null==game.findRobotWithOwnerName(player.getName()))
					continue; // do not consider players without robots
				count_online_players++;
				if (count_online_players>=minimal_count)
					return true;
			}
		}
		return false;
	}
	
	public static void startWalkoverCountdown(RoboToyServerController controller) {
		final ScheduledExecutorService scheduler =
				Executors.newScheduledThreadPool(1);
		scheduler.schedule(new CheckWalkoverAgain(controller), 
				controller.getContext().getGame().getAbandonedGameTimeout(), 
				TimeUnit.MILLISECONDS);
		scheduler.shutdown();
	}
	
	public static class CheckWalkoverAgain implements Runnable {
		private final RoboToyServerController controller;
		CheckWalkoverAgain(RoboToyServerController controller) {
			this.controller = controller;
		}
		@Override
		public void run() {
			// If the game stage changed, do nothing
			if (!GameStage.PLAY.equals(controller.getContext().getGame().getStage())) {
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, "Current game stage is "+controller.getContext().getGame().getStage());
				return;
			}
			// Check again (maybe some player just got reconnected)
			if (hasEnoughOnlinePlayers(controller.getContext().getGame())) {				
				log.log(Level.FINEST, "Some player just got back!");
				return;
			}
			log.log(Level.WARNING, "Terminating the game since all players have left the game!");
			
			// In standalone test-drive we just go back to the first page
			if (GamePlayMode.STANDALONE.equals(controller.getContext().getGamePlayMode())) {
				GameStart.resetGame(controller.getContext().getGame());
				return;
			}
			
			// In normal gameplay we go to summary page
			
			GameOver.stopGame(controller.getContext().getGame());
			
			// Broadcasts to other robots (even if by any chance another player just got reconnected elsewhere
			// just now)
			try {
				controller.broadcastCommand(new CmdStopGame(), Boolean.TRUE, /*mayIncludeRobots*/true);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error while broadcasting game over", e);
			}
		}
	}
}

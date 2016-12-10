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

import java.util.Date;
import java.util.List;

import org.guga.robotoy.rasp.statistics.RoboToyStatistics;

/**
 * Methods used for signaling game start in local robot's memory.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GameStart {
	
	public static void resetGame(GameState game) {
		
		// Remove all ownership status
		for (GameRobot r:game.getRobots()) {
			r.setOwner(null);
		}
		
		// Reset player status
		for (GamePlayer p:game.getPlayers()) {
			p.setDismissedSummary(false);
		}
		
		// Go back to lobby stage and do anything else related to game restart
		restartGame(game);
	}

	public static void startTestDrive(GameState game,GamePlayer driver) {
		
		GameRobot local = game.findLocalRobot();
		if (local!=null) {
			local.setOwner(driver);
			local.setShortId((byte)1);
			local.setLife(game.getMaxLife());
			local.setKills(0);
		}

		if (driver!=null) {
			driver.setDismissedSummary(false);
		}
		
		// Start game
		game.setStage(GameStage.PLAY);		
	}

	public static void startGame(GameState game) {
		
		// Assign robots id's
		// It's the sequential position if all robots
		// are ordered given their unique identifiers
		List<GameRobot> ordered_robots = game.getOrderedRobots();
		int sequential = 1;
		for (GameRobot r:ordered_robots) {
			r.setShortId((byte)(sequential++));
			r.setLife(game.getMaxLife());
			r.setKills(0);
		}
		
		// Reset player status
		for (GamePlayer p:game.getPlayers()) {
			p.setDismissedSummary(false);
		}
		
		// Replenish cards
		for (GameCard c:game.getCards()) {
			c.reset();
		}

		// Start game as local date/time
		game.setGameStart(new Date());
		
		// Start game
		game.setStage(GameStage.PLAY);		
	}

	public static void restartGame(GameState game) {
		
		// Put all players in 'not ready' state to give
		// some time in lobby page
		for (GameRobot r:game.getRobots()) {
			r.setReady(false);
			if (r.getOwner()!=null && !r.getOwner().isOnline()) {
				r.setOwner(null); // player has left the game
			}
		}
		
		// Reset player status
		for (GamePlayer p:game.getPlayers()) {
			p.setResourcesLoaded(false);
		}

		// Restart game in LOBBY screen
		game.setStage(GameStage.INIT);		
		
		// Reset statistics
		RoboToyStatistics.clearAllStatistics();
	}
}

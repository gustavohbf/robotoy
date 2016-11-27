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

/**
 * Some game rules that are enforced during gameplay.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GameRules {

	/**
	 * Tells if it's possible to move the robot further. A robot with
	 * no more 'life' remaining should not move any further.
	 */
	public static boolean canMoveRobot(GameState game,GameRobot robot) {
		if (!GameStage.PLAY.equals( game.getStage()))
			return false;
		if (robot!=null && robot.getLife()<=0)
			return false;
		return true;
	}

	/**
	 * Tells if it's possible to fire the robot's weapon. A robot with
	 * no more 'life' remaining should not fire anymore.
	 */
	public static boolean canFireRobot(GameState game,GameRobot robot) {
		if (!GameStage.PLAY.equals( game.getStage()))
			return false;
		if (robot!=null && robot.getLife()<=0)
			return false;
		return true;		
	}
}

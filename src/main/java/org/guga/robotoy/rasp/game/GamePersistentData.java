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

import org.guga.robotoy.rasp.controller.RoboToyServerContext;

/**
 * Data produced in game that should be persisted locally on disk.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GamePersistentData implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final String DEFAULT_GAMEDATA_FILENAME = "robotoy.config";

	/**
	 * Color of this local robot assigned in game
	 */
	private String color;
	
	/**
	 * Name of the previous owner of this robot
	 */
	private String previousOwner;

	/**
	 * Color of this local robot assigned in game
	 */
	public String getColor() {
		return color;
	}

	/**
	 * Color of this local robot assigned in game
	 */
	public void setColor(String color) {
		this.color = color;
	}
	
	/**
	 * Name of the previous owner of this robot
	 */
	public String getPreviousOwner() {
		return previousOwner;
	}

	/**
	 * Name of the previous owner of this robot
	 */
	public void setPreviousOwner(String previousOwner) {
		this.previousOwner = previousOwner;
	}

	public static GamePersistentData load(RoboToyServerContext context,boolean createNewIfInexistent) throws Exception {
		if (null==context.getLocalStorage())
			throw new Exception("No local storage has been configured yet!");
		GamePersistentData data = context.getLocalStorage().loadData(DEFAULT_GAMEDATA_FILENAME, GamePersistentData.class);
		if (data==null && createNewIfInexistent)
			data = new GamePersistentData();
		return data;
	}
	
	public static void save(RoboToyServerContext context,GamePersistentData data) throws Exception {
		if (null==context.getLocalStorage())
			throw new Exception("No local storage has been configured yet!");
		context.getLocalStorage().saveData(DEFAULT_GAMEDATA_FILENAME, data);
	}
}

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
 * Card used in game for power-up
 * 
 * @author Gustavo Figueiredo
 *
 */
public abstract class GameCard {

	/**
	 * Card identifier
	 */
	private String id;

	/**
	 * Timestamp of last event of this card being used
	 */
	private long timestamp;
	
	/**
	 * Card type
	 */
	public abstract GameCardType getType();
	
	/**
	 * Reset card information upon game startup
	 */
	public abstract void reset();
	
	/**
	 * Update internal card information
	 */
	public abstract void update();

	/**
	 * Card identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * Card identifier
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Timestamp of last event of this card being used
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Timestamp of last event of this card being used
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String toString() {
		return id;
	}

}

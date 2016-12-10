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

import org.guga.robotoy.rasp.rfid.RoboToyPowerUps;

/**
 * Game card used for recharging robots.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GameCardRecharger extends GameCard {

	/**
	 * Minimum time in milisseconds before incrementing charge units of card. Must
	 * be a multiple of {@link RoboToyPowerUps#CARD_MANAGEMENT_DELAY_MS CARD_MANAGEMENT_DELAY_MS}.
	 */
	private static final long DEFAULT_REFILL_RATE = 5000;
	
	/**
	 * Default capacity for each card before depletion.
	 */
	private static final int DEFAULT_MAX_CHARGES = 5;

	/**
	 * Minimum time in milisseconds before incrementing life of a charging robot.
	 */
	private static final long DEFAULT_DEPLETION_RATE = 1000;

	/**
	 * Number of charge units available for recharging
	 */
	private int numChargesAvailable;
		
	/**
	 * Timestamp of last time this card received a self refill
	 */
	private long previousRefillTimestamp;

	/**
	 * Self refill rate, in milliseconds between each charge unit refill.<BR>
	 * Not used if a robot is standing over the card. Must
	 * be a multiple of {@link RoboToyPowerUps#CARD_MANAGEMENT_DELAY_MS CARD_MANAGEMENT_DELAY_MS}.
	 */
	private static long selfRefillRate = DEFAULT_REFILL_RATE;
	
	/**
	 * Maximum capacity of this card in number of charge units
	 */
	private static int maxCharges = DEFAULT_MAX_CHARGES;
	
	/**
	 * Depletion rate, in milliseconds between each charge unit migrating
	 * from card to charging robot.<BR>
	 * Only used if a robot is standing over the card.
	 */
	private static long depletionRate = DEFAULT_DEPLETION_RATE;

	@Override
	public GameCardType getType() {
		return GameCardType.CHARGE_LIFE;
	}

	/**
	 * Reset card information upon game startup
	 */
	@Override
	public void reset() {
		setNumChargesAvailable(getMaxCharges());
		setTimestamp(0);
		previousRefillTimestamp = 0;
	}

	/**
	 * Update internal card information
	 */
	@Override
	public void update() {
		long now = System.currentTimeMillis();
		long previous_timestamp = getTimestamp();
		if (previous_timestamp>0 && (now-previous_timestamp)<GameCardRecharger.getSelfRefillRate()) {
			// does not refill card before a minimum delay after last use of this card
			previousRefillTimestamp = 0;
			return; 
		}
		
		// Check if card charge is already full
		int num_charges = getNumChargesAvailable();
		boolean full = (num_charges>=getMaxCharges());
		
		// If it's not full yet, increase internal charge, unless we just did it
		if (!full) {
			if ((System.currentTimeMillis()-previousRefillTimestamp)>GameCardRecharger.getSelfRefillRate()) {
				setNumChargesAvailable(num_charges+1);
				previousRefillTimestamp = System.currentTimeMillis();
			}
		}

	}

	public int getNumChargesAvailable() {
		return numChargesAvailable;
	}

	public void setNumChargesAvailable(int numChargesAvailable) {
		this.numChargesAvailable = numChargesAvailable;
	}

	/**
	 * Self refill rate, in milliseconds between each charge unit refill.<BR>
	 * Not used if a robot is standing over the card. Must
	 * be a multiple of {@link RoboToyPowerUps#CARD_MANAGEMENT_DELAY_MS CARD_MANAGEMENT_DELAY_MS}.
	 */
	public static long getSelfRefillRate() {
		return selfRefillRate;
	}

	/**
	 * Self refill rate, in milliseconds between each charge unit refill.<BR>
	 * Not used if a robot is standing over the card. Must
	 * be a multiple of {@link RoboToyPowerUps#CARD_MANAGEMENT_DELAY_MS CARD_MANAGEMENT_DELAY_MS}.
	 */
	public static void setSelfRefillRate(long selfRefillRate) {
		GameCardRecharger.selfRefillRate = selfRefillRate;
	}

	public static int getMaxCharges() {
		return maxCharges;
	}

	public static void setMaxCharges(int maxCharges) {
		GameCardRecharger.maxCharges = maxCharges;
	}

	public static long getDepletionRate() {
		return depletionRate;
	}

	public static void setDepletionRate(long depletionRate) {
		GameCardRecharger.depletionRate = depletionRate;
	}
	
}

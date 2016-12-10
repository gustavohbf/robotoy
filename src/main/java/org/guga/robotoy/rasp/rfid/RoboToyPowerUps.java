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
package org.guga.robotoy.rasp.rfid;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.commands.CmdCharge;
import org.guga.robotoy.rasp.commands.RobotSummary;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GameCard;
import org.guga.robotoy.rasp.game.GameCardRecharger;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStage;

/**
 * Power-ups implemented using RFID cards.<BR>
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
public class RoboToyPowerUps implements RFIDRead.Callback {
	
	private static final Logger log = Logger.getLogger(RoboToyPowerUps.class.getName());
	
	/**
	 * Maximum time in milisseconds before considering timeout between successive power-up events
	 */
	public static final long TIMEOUT = 3000;
	
	/**
	 * Time delay in milisseconds for updating internal state for all known cards during gameplay
	 */
	public static final long CARD_MANAGEMENT_DELAY_MS = 200;
	
	private final RoboToyServerController controller;
	
	private long previousRechargeTimestamp;
	
	/**
	 * Schedules a periodic task for updating internal status for each known cards during gameplay
	 */
	public static void scheduleCardsManagement(final RoboToyServerController controller) {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleWithFixedDelay(()->{
			if (controller.getContext()!=null 					
					&& controller.getContext().getGame()!=null
					&& GameStage.PLAY.equals(controller.getContext().getGame().getStage())) {
				for (GameCard card:controller.getContext().getGame().getCards()) {
					card.update();
				}
			}
		}, CARD_MANAGEMENT_DELAY_MS, CARD_MANAGEMENT_DELAY_MS, TimeUnit.MILLISECONDS);
	}
	
	public RoboToyPowerUps(RoboToyServerController controller) {
		this.controller = controller;
	}

	/**
	 * Method invoked whenever robot stays over a RFID card.
	 */
	@Override
	public void onAuthentication(byte[] uid, byte[] sector) {
		String card_id = MFRC522.toString(uid);
		GameCard any_card = controller.getContext().getGame().findCardWithId(card_id);
		if (any_card==null) {
			// New card found in this game
			any_card = getNewCard(card_id, sector);
			controller.getContext().getGame().addCard(any_card);
		}

		any_card.setTimestamp(System.currentTimeMillis());

		if (any_card instanceof GameCardRecharger) {
			onRecharge((GameCardRecharger)any_card);
		}
		else {
			log.log(Level.WARNING, "Unknown card type: "+any_card.getClass().getName());
		}		
	}
	
	/**
	 * A new RFID card was found in game. Return a new instance of GameCard for it.<BR>
	 * The current implementation will always create a new 'GameCardRecharger' instance.
	 */
	private GameCard getNewCard(String card_id, byte[] sector) {
		GameCardRecharger card = new GameCardRecharger();
		card.setId(card_id);
		card.reset();
		return card;
	}

	/**
	 * Method invoked for recharging the robot.
	 */
	private void onRecharge(GameCardRecharger card) {
		// Find local robot
		GameRobot local_robot = controller.getContext().getGame().findLocalRobot();
		if (local_robot==null)
			return;
		
		// Check if card is depleted
		int num_charges = card.getNumChargesAvailable();
		boolean depleted = num_charges==0;
		if (depleted) {
			CmdCharge.Message charge = new CmdCharge.Message();
			charge.setType(card.getType().ordinal());
			charge.setCard(card.getId());
			charge.setCharging(RobotSummary.fromRobot(local_robot));
			charge.setDepleted(true);
			try {
				controller.broadcastCommand(new CmdCharge(), charge, /*mayIncludeRobots*/true);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error while broadcasting 'CmdCharge' event to other robots and players");
			}
			return;
		}
		
		// Check if current life is already full
		boolean full = (controller.getContext().getGame().getMaxLife() == local_robot.getLife());
		
		// If it's not full yet, increase life, unless we just did it
		if (!full) {
			if (previousRechargeTimestamp==0
					|| (System.currentTimeMillis()-previousRechargeTimestamp)>TIMEOUT) {
				previousRechargeTimestamp = System.currentTimeMillis();
			}
			else if ((System.currentTimeMillis()-previousRechargeTimestamp)>GameCardRecharger.getDepletionRate()) {
				
				local_robot.setLife(local_robot.getLife()+1);
				card.setNumChargesAvailable(num_charges-1);
				previousRechargeTimestamp = System.currentTimeMillis();
			}
		}
		
		// We need to broadcast this event to other robots and players
		CmdCharge.Message charge = new CmdCharge.Message();
		charge.setType(card.getType().ordinal());
		charge.setCard(card.getId());
		charge.setCharging(RobotSummary.fromRobot(local_robot));
		charge.setRemaining(card.getNumChargesAvailable());
		charge.setFull(full);
		try {
			controller.broadcastCommand(new CmdCharge(), charge, /*mayIncludeRobots*/true);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while broadcasting 'CmdCharge' event to other robots and players");
		}
	}
}

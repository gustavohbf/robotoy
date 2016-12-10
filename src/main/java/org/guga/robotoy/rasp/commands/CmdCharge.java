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
package org.guga.robotoy.rasp.commands;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.game.GameCard;
import org.guga.robotoy.rasp.game.GameCardRecharger;
import org.guga.robotoy.rasp.game.GameCardType;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Command sent by a charging robot to other robots and players telling about
 * charging process (i.e. the robot is sitting on top of a RFID card capable of
 * recharging the robot's life)
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdCharge implements CommandWithBroadcast<CmdCharge.Message> {

	private static final Logger log = Logger.getLogger(CmdCharge.class.getName());

	@Override
	public String getHelp() {
		return "{\"charging\":<robot summary>} - Notifies about a robot that is currently standing over a recharging cell. This command must be issued by a robot.";
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return CommandIssuer.ROBOT.equals(issuer) && message.startsWith("{\"charging\":");
	}

	@Override
	public CmdCharge.Message parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		Message parsed = parse(message);
		if (parsed==null || parsed.getCharging()==null || parsed.getCharging().getId()==null)
			return null;
		
		if (parsed.getType()!=GameCardType.CHARGE_LIFE.ordinal()) {
			if (log.isLoggable(Level.WARNING))
				log.log(Level.WARNING, "Wrong card type: "+parsed.getType()+", expected: "+GameCardType.CHARGE_LIFE.ordinal());
			return null;
		}
		
		// Update card information
		if (parsed.getCard()!=null) {
			// Update local information about this card
			GameCard any_card = context.getGame().findCardWithId(parsed.getCard());
			if (any_card!=null && !(any_card instanceof GameCardRecharger)) {
				// Local database kept a different version of this card, let's update it.
				context.getGame().removeCard(any_card);
				any_card = null;
			}
			if (any_card==null) {
				// New card found in this game
				GameCardRecharger card = new GameCardRecharger();
				card.setId(parsed.getCard());
				card.reset();
				card.setTimestamp(System.currentTimeMillis());
				context.getGame().addCard(card);
			}
			else {
				// Existing card
				GameCardRecharger card = (GameCardRecharger)any_card;
				card.setNumChargesAvailable(parsed.getRemaining());
				card.setTimestamp(System.currentTimeMillis());
			}
		}
				
		// Locate charging robot
		GameRobot charging_robot = context.getGame().findRobotWithIdentifier(parsed.getCharging().getId());
		if (charging_robot==null)
			return null;
		
		// Update robot status
		charging_robot.setLife(parsed.getCharging().getLife());
		
		return parsed;
	}
	
	@Override
	public String getBroadcastMessage(RoboToyServerContext context,CmdCharge.Message object) {
		StringBuilder message = new StringBuilder();
		message.append("{\"charging\":");
		message.append(JSONUtils.toJSON(object.getCharging(), false));
		message.append(",\"card\":");
		message.append(JSONUtils.quote(object.getCard()));
		message.append(",\"type\":");
		message.append(object.getType());
		message.append(",\"full\":");
		message.append(object.isFull());
		message.append(",\"depleted\":");
		message.append(object.isDepleted());
		message.append(",\"remaining\":");
		message.append(object.getRemaining());
		message.append("}");
		return message.toString();

	}

	public static Message parse(String json) {
		return JSONUtils.fromJSON(json, Message.class);
	}

	public static class Message {
		/**
		 * Identifies the robot standing over the power-up card
		 */
		private RobotSummary charging;
		/**
		 * Identifies the power-up card
		 */
		private String card;
		/**
		 * Type of power-up card
		 */
		private int type;
		/**
		 * Indicates the robot has been fully-charged
		 */
		private boolean full;
		/**
		 * Indicates the power-up card has been fully depleted
		 */
		private boolean depleted;
		/**
		 * Number of charges remaining in power-up card
		 */
		private int remaining;
		/**
		 * Identifies the robot standing over the power-up card
		 */
		public RobotSummary getCharging() {
			return charging;
		}
		/**
		 * Identifies the robot standing over the power-up card
		 */
		public void setCharging(RobotSummary charging) {
			this.charging = charging;
		}		
		/**
		 * Type of power-up card
		 */
		public int getType() {
			return type;
		}
		/**
		 * Type of power-up card
		 */
		public void setType(int type) {
			this.type = type;
		}
		/**
		 * Identifies the power-up card
		 */
		public String getCard() {
			return card;
		}
		/**
		 * Identifies the power-up card
		 */
		public void setCard(String card) {
			this.card = card;
		}
		/**
		 * Indicates the robot has been fully-charged
		 */
		public boolean isFull() {
			return full;
		}
		/**
		 * Indicates the robot has been fully-charged
		 */
		public void setFull(boolean full) {
			this.full = full;
		}
		/**
		 * Indicates the power-up card has been fully depleted
		 */
		public boolean isDepleted() {
			return depleted;
		}
		/**
		 * Indicates the power-up card has been fully depleted
		 */
		public void setDepleted(boolean depleted) {
			this.depleted = depleted;
		}
		/**
		 * Number of charges remaining in power-up card
		 */
		public int getRemaining() {
			return remaining;
		}
		/**
		 * Number of charges remaining in power-up card
		 */
		public void setRemaining(int remaining) {
			this.remaining = remaining;
		}
		
	}

}

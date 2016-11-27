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
package org.guga.robotoy.rasp.optics;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.commands.CmdHit;
import org.guga.robotoy.rasp.commands.CmdStopGame;
import org.guga.robotoy.rasp.commands.RobotSummary;
import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GameOver;
import org.guga.robotoy.rasp.game.GamePlayMode;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStart;
import org.guga.robotoy.rasp.optics.IRSend.PWMType;
import org.guga.robotoy.rasp.statistics.RoboToyStatistics;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;

/**
 * RoboToy weaponary. It includes both 'guns' and 'shields' (for hit detection).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class RoboToyWeaponary {

	/**
	 * Minimum delay between two successive fire shots (in miliseconds). Will ignore anything shorter.
	 */
	public static final int MINIMUM_DELAY_BETWEEN_SHOTS_MS = 1000;

	private static final Logger log = Logger.getLogger(RoboToyWeaponary.class.getName());

	private long lastShotTimestamp;
	
	private IRBeamEncoder beamEncoder;
	
	private boolean allowBackfire;

	private final RoboToyServerController controller;
	
	private final RoboToyServerContext context;
	
	public RoboToyWeaponary(RoboToyServerController controller) {
		this.controller = controller;
		this.context = controller.getContext();
		this.beamEncoder = new IRBeamEncoder();
		beamEncoder.setChecksum(true);
		beamEncoder.setBitsPerByte(4); // half byte

	}
	
	public void buildBeamDevice(Pin pinBeamDevice) {
		IRSend beamDevice = new IRSend(pinBeamDevice,PWMType.HARDWARE,38_000);
		beamDevice.setEncoder(beamEncoder);
		beamDevice.setNumRepeats(15);	// 16 signals total
		beamDevice.setDelayBetweenRepeats(beamEncoder.getHeaderOffPulse());
		context.setBeamDevice(beamDevice);
	}
	
	public void buildBeamDetectors(PinPullResistance internalResistance,Pin... pinDetectorDevices) {
		IRBeamDecoder beamDecoder = IRBeamDecoder.forEncoder(beamEncoder);
		beamDecoder.setBitsPerByte(8);	// chunk together 4 data bits (low order in byte) with 4 checksum bits (high order in byte)
		beamDecoder.setChecksum(false);
		for (int pin_index=0;pin_index<pinDetectorDevices.length;pin_index++) {
			Pin pin = pinDetectorDevices[pin_index];
			IRReceive receiver = new IRReceive(pin,internalResistance,new RobotoyReceiver(pin_index,beamDecoder));
			receiver.setMinStartPulseDelay(beamEncoder.getHeaderOnPulse()*2/3);
			receiver.init();
		}
	}

	public boolean isAllowBackfire() {
		return allowBackfire;
	}

	public void setAllowBackfire(boolean allowBackfire) {
		this.allowBackfire = allowBackfire;
	}

	/**
	 * Output log message with information about incoming signal detected (e.g. IR light detector)
	 * @param detected sequence of alternating 'low' and 'high' levels
	 * @param size number of pulses to consider in array
	 */
	private static void logRawSignal(int[] detected,int size) {
		StringBuilder signal_text = new StringBuilder();
		for (int i=0;i<size;i++) {
			int signal = detected[i];
			if (signal_text.length()>0)
				signal_text.append(" ");
			if ((i%2)==0)
				signal_text.append("[");
			signal_text.append(signal);
			if ((i%2)==0)
				signal_text.append("]");
		}
		log.log(Level.FINEST, "Detected signal with "+size+" pulses: "+signal_text.toString());
	}
	
	/**
	 * Callback routine used for parsing incoming signals.
	 * @author Gustavo Figueiredo
	 */
	private class RobotoyReceiver implements IRReceive.IRReceiveCallback {
		private final int receiverIndex;
		private final IRBeamDecoder beamDecoder;
		RobotoyReceiver(int index,IRBeamDecoder beamDecoder) {
			this.receiverIndex = index;
			this.beamDecoder = beamDecoder;
		}
		@Override
		public void onSignalDetected(int[] signal, int size, PinState startPinLevel) {
			if (size<beamDecoder.getMinSignalSize())
				return; // ignore anything insignificant
			if (log.isLoggable(Level.FINEST)) {
				logRawSignal(signal,size);
			}
			byte[] message = beamDecoder.getMessage(signal, size);
			if (log.isLoggable(Level.FINEST)) {
				logDecodedMessage(message);
			}
			if (message!=null && message.length>0) {
				chkDecodedMessage(receiverIndex,message);
			}
		}
	}
	
	/**
	 * Output log message with information about decoded message in incoming signal detected (e.g. IR light detector)
	 * @param message Decoded bytes
	 */
	private static void logDecodedMessage(byte[] message) {
		if (message==null || message.length==0)
			log.log(Level.FINEST, "Decoded message: NONE");
		else {
			StringBuilder message_text = new StringBuilder();
			for (int i=0;i<message.length;i++) {
				byte b = message[i];
				if (message_text.length()>0)
					message_text.append(", ");
				for (int j=0;j<8;j++)
					message_text.append((b>>(7-j))&1);
			}
			log.log(Level.FINEST, "Decoded message with "+message.length+" bytes: "+message_text.toString());
		}
	}
	
	/**
	 * Verify checkum of incoming signal (e.g. IR light detector)
	 */
	private void chkDecodedMessage(int pin_index,byte[] message) {
		// We expect one single byte with 4 high level bits consisting of a checksum
		// and 4 low level bits consisting of data
		// If we got multiple bytes, there might be some redundance (sender will usually send
		// about 16 signals)
		byte histogram[][] = null; // histogram is only used if we get more than 1 byte
		for (byte b:message) {
			byte data = (byte)(b & 0x0F);
			byte checksum = (byte)((b >> 4) & 0x0F);
			byte checksum_bits = (byte)0b00001010;
			checksum_bits ^= data;
			if (checksum_bits==checksum) {
				// probably correct
								
				if (message.length==1) {
					if (context.isTakeStatistics()) {
						RoboToyStatistics.incIRStatRawSignalsMatch();
					}
					onBeamReceived(data);
				}
				else {
					if (histogram==null)
						histogram = new byte[message.length][2];
					if (histogram[0][0]==data)
						histogram[0][1]++;
					else {
						int pos_in_histogram = 0;
						for (;pos_in_histogram<histogram.length;pos_in_histogram++) {
							if (histogram[pos_in_histogram][0]==data)
								break;
							else if (histogram[pos_in_histogram][0]==0) {
								histogram[pos_in_histogram][0] = data;
								break;
							}
						}
						histogram[pos_in_histogram][1]++;
					}
				}					
			}
			else {
				// checksum failed
				if (context.isTakeStatistics()) {
					RoboToyStatistics.incIRStatRawSignalsMisMatch();
				}
			}
		} // LOOP over incoming message bytes
		if (histogram!=null) {
			// consider only the most frequent data signal
			byte most_frequent_data = histogram[0][0];
			byte most_frequent_count = histogram[0][1];
			int number_of_matching_signals = 1;
			for (int i=1;i<histogram.length;i++) {
				byte data = histogram[i][0];
				if (data==0)
					break; // end of valid data
				number_of_matching_signals++;
				byte count = histogram[i][1];
				if (count>most_frequent_count) {
					most_frequent_data = data;
					most_frequent_count = count;
				}
			}
			
			if (context.isTakeStatistics()) {

				// One signal is right
				RoboToyStatistics.incIRStatRawSignalsMatch();
				// The rest is wrong
				RoboToyStatistics.incIRStatRawSignalsWrong(number_of_matching_signals-1);
			}

			onBeamReceived(most_frequent_data);
		}
	}
	
	/**
	 * Method executed when we detect fire from another robot
	 */
	private void onBeamReceived(byte code) {
		long timestamp = System.currentTimeMillis();
		if (lastShotTimestamp!=0 && (timestamp-lastShotTimestamp)<MINIMUM_DELAY_BETWEEN_SHOTS_MS)
			return; // too fast (may be repetitions of the same fire signal)
		lastShotTimestamp = timestamp;
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE,"Got fire from robot with sequence id #"+code+"!");
		}
		GameRobot robot = context.getGame().findRobotWithShortId(code);
		if (robot==null) {
			log.log(Level.WARNING, "Got fire from an unknown robot with sequence id #"+code);
			return;
		}
		GameRobot local = context.getGame().findLocalRobot();
		if (local==null) {
			log.log(Level.WARNING, "Could not find local robot!");
			return;			
		}
		if (local==robot) {
			log.log(Level.WARNING, "Got fire from myself!");	// maybe our detector is too close to our beam or maybe standing in front of a mirror?
			if (context.isTakeStatistics()) {
				RoboToyStatistics.incIRStatAckBackFire();
			}
			if (!allowBackfire) {
				return;
			}
		}
		else {
			if (context.isTakeStatistics()) {
				RoboToyStatistics.incIRStatAckHits();
			}
		}
		
		CmdHit.evaluateHit(local, robot, context);
		
		// Little LED blink animation whenever we get hit
		if (context.getRGBLed()!=null) {
			GameRobot localRobot = context.getGame().findLocalRobot();
			LedColor localColor = (localRobot!=null && localRobot.getColor()!=null) ? localRobot.getColor() : null;
			if (localColor!=null) {
				context.getRGBLed().startCycleColors(/*delay*/100, /*timeout*/1000, /*color at rest*/localColor, 
						LedColor.OFF, localColor);
			}
		}
		
		// Broadcast event to other robots and players
		CmdHit.Hit hit = new CmdHit.Hit();
		hit.setHit(RobotSummary.fromRobot(local));
		hit.setSource(RobotSummary.fromRobot(robot));
		hit.setFatal(local.getLife()==0);
		try {
			controller.broadcastCommand(new CmdHit(), hit, /*mayIncludeRobots*/true);
		} catch (Exception e) {
			log.log(Level.SEVERE,"Error while broadcasting a HIT event!",e);
		}
		
		// If it was a fatal hit, check if it's game over
		if (hit.isFatal()
			&& GameOver.isGameOver(context.getGame())) {
			log.log(Level.WARNING, "Terminating the game because this robot died!");
			
			// In standalone test-drive we just go back to the first page
			if (GamePlayMode.STANDALONE.equals(context.getGamePlayMode())) {
				GameStart.resetGame(context.getGame());
				return;
			}

			// In normal gameplay we go to summary page

			GameOver.stopGame(controller.getContext().getGame());
			
			// Broadcasts to other robots and players
			try {
				controller.broadcastCommand(new CmdStopGame(), Boolean.TRUE, /*mayIncludeRobots*/true);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error while broadcasting game over", e);
			}
		}

	}

}

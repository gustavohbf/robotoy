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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.util.Arrays;
import org.guga.robotoy.rasp.rfid.MFRC522.CardInfo;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;

/**
 * RFID reader used in RoboToy.<BR>
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
public class RFIDRead {

	private static final Logger log = Logger.getLogger(RFIDRead.class.getName());
	
	private static final int DELAY_POOLING_MS = 200;

	/**
	 * Callback function to be used with RFIDRead
	 */
	@FunctionalInterface
	public static interface Callback {
		/**
		 * This method gets called whenever a card is detected.
		 */
		default public void onCardDetected() { }
		/**
		 * This method gets called whenever a card is detected
		 * and we get its UID.
		 */
		default public void onUID(byte[] uid) { }
		/**
		 * This method gets called whenever a card is detected
		 * and is successfully authenticated.
		 */
		public void onAuthentication(byte[] uid,byte[] sector);
	}

	/**
	 * GPIO controller
	 */
    private GpioController gpio;

	private MFRC522 mfrc522;
	
	/**
	 * NRSTPD = Not Reset and Power-down<BR>
	 * Default value: GPIO 06 (wPi) = BCM25 = Physical 22
	 */
	private Pin pinNRSTPD = RaspiPin.GPIO_06;
	
	/**
	 * SPI channel.<BR>
	 * Default value: CS0
	 */
	private SpiChannel spiChannel = SpiChannel.CS0;
	
	private AtomicBoolean currentThreadRunning;
	
	/**
	 * Callback function
	 */
	private Callback callback;
	
	/**
	 * NRSTPD = Not Reset and Power-down<BR>
	 * Default value: GPIO 06 (wPi) = BCM25 = Physical 22
	 */
	public Pin getPinNRSTPD() {
		return pinNRSTPD;
	}

	/**
	 * NRSTPD = Not Reset and Power-down<BR>
	 * Default value: GPIO 06 (wPi) = BCM25 = Physical 22
	 */
	public void setPinNRSTPD(Pin pinNRSTPD) {
		this.pinNRSTPD = pinNRSTPD;
	}

	/**
	 * SPI channel.<BR>
	 * Default value: CS0
	 */
	public SpiChannel getSpiChannel() {
		return spiChannel;
	}

	/**
	 * SPI channel.<BR>
	 * Default value: CS0
	 */
	public void setSpiChannel(SpiChannel spiChannel) {
		this.spiChannel = spiChannel;
	}

	/**
	 * Callback function
	 */
	public Callback getCallback() {
		return callback;
	}

	/**
	 * Callback function
	 */
	public void setCallback(Callback callback) {
		this.callback = callback;
	}

	public void init() throws IOException {
		
		if (isRunning())
			throw new IOException("Already running!");
		
		if (gpio==null)
			gpio = GpioFactory.getInstance();
		mfrc522 = new MFRC522(gpio,pinNRSTPD,spiChannel);
		
		final AtomicBoolean newThreadRunning = new AtomicBoolean(true);
		Thread pooling_thread = new Thread(()->{ checkRFIDContinuously(mfrc522,newThreadRunning,callback); });
		pooling_thread.setName("RFIDPooling");
		pooling_thread.setDaemon(true);
		pooling_thread.start();
		currentThreadRunning = newThreadRunning;
	}
	
	public boolean isRunning() {
		return currentThreadRunning!=null && currentThreadRunning.get();
	}
	
	public void stop() {
		if (isRunning()) {
			currentThreadRunning.set(false);
		}
	}
	
	private static void checkRFIDContinuously(final MFRC522 mfrc522,final AtomicBoolean running,Callback callback) {
		CardInfo ci = new CardInfo();
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Start RFIDPooling");
		while (running.get()) {
			try {
				Thread.sleep(DELAY_POOLING_MS);
			}
			catch (InterruptedException e) {
				break;
			}
			if (!running.get())
				break;
			ci.clear();
			try {
				mfrc522.request(MFRC522.PICC_REQIDL, ci);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Error while requesting data from MFRC522", e);
				continue;
			}
			if (!running.get())
				break;
			
			if (ci.isOk()) {
				if (callback!=null)
					callback.onCardDetected();
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "Card detected!");
			}
			
			try {
				mfrc522.getAntiCollision(ci);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Error while requesting anti collision data from MFRC522", e);
				continue;
			}
			
			if (ci.isOk()) {
				byte[] uid_with_checksum = ci.getBackBytes();
				byte[] uid = (uid_with_checksum==null || uid_with_checksum.length==0) ? null
						: Arrays.copyOf(uid_with_checksum, uid_with_checksum.length-1);
				if (callback!=null)
					callback.onUID(uid);
				if (log.isLoggable(Level.INFO))
					log.log(Level.INFO, "Card read UID: "+ci.getBackBytesString());

				try {
					mfrc522.selectTag(ci.getBackBytes());
				} catch (IOException e) {
					log.log(Level.SEVERE, "Error while selecting tag in MFRC522 for tag " + ci.getBackBytesString(), e);
					continue;
				}

				// This is the default key for authentication
				byte[] key = new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
				
				// Authenticate
				try {
					mfrc522.auth(MFRC522.PICC_AUTHENT1A, 8, key, ci.getBackBytes());
				} catch (IOException e) {
					log.log(Level.SEVERE, "Error while authenticating in MFRC522 for tag " + ci.getBackBytesString(), e);
					continue;
				}
				
				// Check if authenticated
				if (ci.isOk()) {
				
					try {
						byte[] sector = mfrc522.getSector(8);
						
						if (callback!=null)
							callback.onAuthentication(uid, sector);
						
						if (log.isLoggable(Level.FINEST)) {
							if (sector!=null)
								log.log(Level.FINEST, "Sector 8 for tag " + ci.getBackBytesString() +": "+ MFRC522.toString(sector));
							else
								log.log(Level.FINEST, "Error while reading sector 8 for tag " + ci.getBackBytesString());
						}
					}
					catch (IOException e) {
						log.log(Level.SEVERE, "Error while reading sector 8 in MFRC522 for tag " + ci.getBackBytesString(), e);						
					}
					
					try {
						mfrc522.stopCrypto1();
					}
					catch (IOException e) {
						log.log(Level.SEVERE, "Error while stopping cryptography in MFRC522 for tag " + ci.getBackBytesString(), e);
					}
				}
				else {
					log.log(Level.INFO, "Authentication error in MFRC522 for tag " + ci.getBackBytesString());
				}
			}
		}
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Stop RFIDPooling");
	}

    public void addShutdownHook() {
        Thread shutdown_hook = new Thread(()->{
        	stop();
        	if (gpio!=null) {
        		gpio.shutdown();
        	}
        });
        shutdown_hook.setName("GPIOShutdownHook");
        shutdown_hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdown_hook);    	
    }
}

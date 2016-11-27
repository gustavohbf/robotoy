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

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.wiringpi.Gpio;

/**
 * A quick-and-dirty implementation of operating a LED for sending
 * some signal in the form of pulses with width modulation using
 * a given carrier frequency.<BR>
 * <BR>
 * It tries to use the hardware PWM of Raspberry whenever possible.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class IRSend implements BeamDevice {

	private static final Logger log = Logger.getLogger(IRSend.class.getName());

	public static final int DEFAULT_CARRIER_FREQUENCY = 38000; // Hz
	
    /**
     * Default Raspberry Pi PWM clock frequency: 19.2MHz.
     */
    public static final int DEFAULT_RPI_PWM_CLOCK_FREQUENCY = 19_200_000;

    public static final int MAX_PWM_LEVEL = 100;

	public static enum PWMType {
		HARDWARE,
		SOFTWARE;		
	}
	
	public static interface Encoder {
		
		/**
		 * Returns the maximum signal size (in number of pulses) including
		 * header, tail and data. 
		 */
		public int getMaxSignalSize();

		/**
		 * Fills in 'buffer' with header pulses. Each odd indexed value
		 * will be a 'off' delay and each even indexed value will be a 'on'
		 * delay.
		 * @param buffer Buffer receives the pulse values
		 * @param offset Offset in 'buffer' to start writing
		 * @return Return amount of pulses written to buffer
		 */
		public int getHeader(int[] buffer,int offset);
		
		/**
		 * Fills in 'buffer' with tail pulses. Each odd indexed value
		 * will be a 'off' delay and each even indexed value will be a 'on'
		 * delay.
		 * @param buffer Buffer receives the pulse values
		 * @param offset Offset in 'buffer' to start writing
		 * @return Return amount of pulses written to buffer
		 */
		public int getTail(int[] buffer,int offset);
		
		/**
		 * Fills in 'buffer' with data pulses representing byte 'b'. Each odd indexed value
		 * will be a 'off' delay and each even indexed value will be a 'on'
		 * delay.
		 * @param buffer Buffer receives the pulse values
		 * @param offset Offset in 'buffer' to start writing
		 * @return Return amount of pulses written to buffer
		 */
		public int getPulses(byte b,int[] buffer, int offset);
		
		/**
		 * Get encoded signal in the form of mark/space pulses (starting with 'mark').
		 */
		public int[] getEncodedSignal(byte[] msg);
	}
	
    private GpioController gpio;

	private final Pin pin;
	
	private final GpioPinPwmOutput pwm;
	
	private Encoder encoder;
	
	/**
	 * Number of repetitions of the whole signal. Defaults to 0 repetitions.
	 */
	private int numRepeats;
	
	/**
	 * Delayed off pulse between repetitions (only used if numRepeats > 0)
	 */
	private int delayBetweenRepeats;

    public IRSend(Pin pin, PWMType type) {
        this(pin, type, DEFAULT_CARRIER_FREQUENCY);
    }

    public IRSend(Pin pin, PWMType type, int carrierFrequency) {
        this.pin = pin;
        gpio = GpioFactory.getInstance();
        if (PWMType.SOFTWARE.equals(type))
        	pwm = gpio.provisionSoftPwmOutputPin(pin, "IR_PWM", 0);
        else
        	pwm = gpio.provisionPwmOutputPin(pin, "IR_PWM", 0);
        pwm.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        pwm.setPwm(0);
        Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
        Gpio.pwmSetRange(MAX_PWM_LEVEL);
        
        setFrequency(carrierFrequency);
        
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, "IR LED pin "+pin+" PWM type "+type+" carrier frequency "+carrierFrequency+" Hz");
        }
    }

    public Encoder getEncoder() {
		return encoder;
	}

	public void setEncoder(Encoder encoder) {
		this.encoder = encoder;
	}

	/**
	 * Number of repetitions of the whole signal. Defaults to 0 repetitions.
	 */
	public int getNumRepeats() {
		return numRepeats;
	}

	/**
	 * Number of repetitions of the whole signal. Defaults to 0 repetitions.
	 */
	public void setNumRepeats(int numRepeats) {
		this.numRepeats = numRepeats;
	}

	/**
	 * Delayed off pulse between repetitions (only used if numRepeats > 0)
	 */
	public int getDelayBetweenRepeats() {
		return delayBetweenRepeats;
	}

	/**
	 * Delayed off pulse between repetitions (only used if numRepeats > 0)
	 */
	public void setDelayBetweenRepeats(int delayBetweenRepeats) {
		this.delayBetweenRepeats = delayBetweenRepeats;
	}

	public void addShutdownHook() {
        Thread shutdown_hook = new Thread(()->{gpio.shutdown();});
        shutdown_hook.setName("GPIOShutdownHook");
        shutdown_hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdown_hook);    	
    }
    
    public void setFrequency(double frequency) {
        int pwmClockDivisor = (int) ((double)DEFAULT_RPI_PWM_CLOCK_FREQUENCY / (frequency * (double)MAX_PWM_LEVEL));
        Gpio.pwmSetClock(pwmClockDivisor);
        if (log.isLoggable(Level.FINE)) {
        	log.log(Level.FINE, "Clock Divisor "+pwmClockDivisor+" DEFAULT_RPI_PWM_CLOCK_FREQUENCY "+DEFAULT_RPI_PWM_CLOCK_FREQUENCY);
        }
    }

    public Pin getPin() {
		return pin;
	}

	private void pulse(long micros) {
    	pwm.setPwm(50);	// 50%
        Gpio.delayMicroseconds(micros);
        pwm.setPwm(0);
    }

    public void send(int[] pulses) {
    	send(pulses,pulses.length);
    }

    public void send(int[] pulses,int size) {
    	if (log.isLoggable(Level.FINEST)) {
    		StringBuilder pulse_as_text = new StringBuilder();
    		for (int i=0;i<size;i++) {
    			if (i>0)
    				pulse_as_text.append(",");
            	if ((i%2)==1) {
            		pulse_as_text.append("(l:"+pulses[i]+")");
            	}
            	else {
            		pulse_as_text.append("(h:"+pulses[i]+")");
            	}    			
    		}
    		log.log(Level.FINEST,"Pulse: "+pulse_as_text.toString());
    	}
        for(int i = 0; i < size; i++) {
        	if ((i%2)==1) {
        		Gpio.delayMicroseconds(pulses[i]); // off
        	}
        	else {
        		pulse(pulses[i]); // on
        	}
        }
    }

    @Override
    public void sendBeam(byte[] message) throws Exception {
    	if (encoder==null)
    		throw new Exception("Could not send beam. Did not setup encoder yet!");
    	int signal[] = encoder.getEncodedSignal(message);
    	send(signal);
		for (int i=0;i<numRepeats;i++) {
			Gpio.delayMicroseconds(delayBetweenRepeats); // off
			send(signal);
		}
    }
}

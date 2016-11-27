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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * (Horrible) quick and dirty implementation of an infrared signal receiver that will read pulses through an digital
 * input pin in Raspberry pi modulated over a carrier frequency.<BR>
 * <BR>
 * WARNING: this is not too much reliable for pulses in the range of a few hundreds microseconds and below!<BR>
 * For more precision in IR detection in the wavelength range of microseconds or less, one should consider
 * another approach, such as using some system with real-time capability (e.g. Arduino).<BR>
 * <BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class IRReceive {
	
	private static final Logger log = Logger.getLogger(IRReceive.class.getName());

	/**
	 * Maximum number of mark/pause signals we are going to hold
	 * in memory buffer
	 */
	public static final int MAX_PULSE_PAIRS = 1000;
	
	public static final int MAX_PULSES = MAX_PULSE_PAIRS * 2;
	
	/**
	 * Maximum time in ms we are going to wait before checking if
	 * the last pulse was long enough to be considered a signal closure.
	 */
	public static final int MAX_POOLING_DELAY = 500;

	/**
	 * Maximum duration of a pulse in nanoseconds
	 */
	public static final long MAX_PULSE_NS = 15_000_000;
	   
	/**
	 * Minimum delay in nanoseconds of a pulse duration. Will ignore any
	 * pulse shorter than this interval.
	 */
	public static final int RESOLUTION_NS = 10_000;
	   
	/**
	 * Pin used for receiving signals
	 */
	private final Pin pin;
	
	/**
	 * Internal resistor used for input GPIO configuration
	 */
	private final PinPullResistance resistance;
	
	/**
	 * Provisoned pin for reading values
	 */
	private final GpioPinDigitalInput input;

	/**
	 * GPIO controller
	 */
    private GpioController gpio;

    /**
     * Even indexes: measurements of 'off' state (HIGH pin level).
     * Odd indexes: measurements of 'on' state (LOW pin level).<BR>
     * All measurements are given in microseconds (1^10-6 seconds)<BR>
     */
    private int[] incoming_signal = new int[MAX_PULSES];
    
    /**
     * Same as 'incoming_signal', but kept unchanged for
     * the duration of signal processing thread.
     */
    private int[] pending_signal = new int[MAX_PULSES];
    
    /**
     * Number of pulses in 'pending_signal' that should
     * be considered.
     */
    private int pending_signal_size;
    
    /**
     * Pin level of start of pulses in 'pending_signal' 
     */
    private PinState pending_signal_start_pin_level;
    
    /**
     * Index in 'incoming_signal' array that we are going to
     * update in the next falling or rising event.<BR>
     */
    private volatile int currentPulse;
    
    /**
     * Index in 'incoming_signal' that we will use as a reference
     * for the starting point of a sequence of pulses. 
     */
    private volatile int currentSignalStart;
    
    private volatile LongAdder pulseCount;
    
    /**
     * Counts the number of pulses arrived (may overflow).<BR>
     * Used to avoid trigger several 'onSignalClosure' events
     * by 'IRReceivePooling' thread while we are not receiving
     * any changes.
     */
    private volatile long previousSignalPulseCount;
    
    /**
     * Time reference (in nanoseconds) for the start duration of a pulse.
     */
    private long currentPulseStart;
    
    /**
     * Optional parameter. Zero = ignored. Positive = minimum delay of
     * low level ('on' state) of the heading of desired signal.
     */
    private int minStartPulseDelay;
    
    /**
     * Tells if 'on state' should be considered when pin level is HIGH. The default
     * is false (i.e.: 'on state' = 'low pin level').
     */
    private boolean onIsHigh;

    /**
     * Flag used to keep threads running
     */
    private final AtomicBoolean running;
    
    /**
     * Flag used to hold 'pending_signal' unchanged for the time it
     * takes to process it.
     */
    private final AtomicBoolean pendingSignalSaturated;
    
    /**
     * Synchronization semaphore used to wake the 'processCallback' thread
     * whenever a new signal (sequence pulses) arrives.
     */
    private final Semaphore signalReceived;
    
    /**
     * Keep track of previous edge to check if we missed something
     */
    private PinEdge prevEdge;
    
    @FunctionalInterface
    public static interface IRReceiveCallback
    {
    	public void onSignalDetected(int[] signal,int size,PinState startPinLevel);
    }
    
    private final IRReceiveCallback callback;

	public IRReceive(Pin pin,PinPullResistance resistance,IRReceiveCallback callback) {
		this.pin = pin;
		this.resistance = resistance;
		this.callback = callback;
		running = new AtomicBoolean(false);
		pendingSignalSaturated = new AtomicBoolean();
		signalReceived = new Semaphore(0);
		pulseCount = new LongAdder();
        gpio = GpioFactory.getInstance();
        previousSignalPulseCount = 0;
        input =  gpio.provisionDigitalInputPin(pin, "IR_DETECTOR", resistance);
        input.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        currentPulseStart = (System.nanoTime()-MAX_PULSE_NS);
        currentSignalStart = (onIsHigh) ? 0 : 1;
        currentPulse = input.isHigh() ? 0 : 1; 
        prevEdge = null;
        if (log.isLoggable(Level.FINEST)) {
        	log.log(Level.FINEST, "STARTING AT INDEX "+currentPulse+", START SIGNAL AT "+currentSignalStart);
        }
        input.addListener(new GpioPinListenerDigital() {			
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				switch (event.getEdge()) {
				case FALLING:
					onSignalFalling(event.getState());
					break;
				case RISING:
					onSignalRising(event.getState());
					break;
				default:
				}
			}
		});
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, "IR DETECTOR pin "+pin);
        }
	}

	public Pin getPin() {
		return pin;
	}
	
    public PinPullResistance getResistance() {
		return resistance;
	}

	/**
     * Optional parameter. Zero = ignored. Positive = minimum delay of
     * low level ('on' state) of the heading of desired signal.
     */
	public int getMinStartPulseDelay() {
		return minStartPulseDelay;
	}

    /**
     * Optional parameter. Zero = ignored. Positive = minimum delay of
     * low level ('on' state) of the heading of desired signal.
     */
	public void setMinStartPulseDelay(int minStartPulseDelay) {
		this.minStartPulseDelay = minStartPulseDelay;
	}

    /**
     * Tells if 'on state' should be considered when pin level is HIGH. The default
     * is false (i.e.: 'on state' = 'low pin level').
     */
	public boolean isOnHigh() {
		return onIsHigh;
	}

    /**
     * Tells if 'on state' should be considered when pin level is HIGH. The default
     * is false (i.e.: 'on state' = 'low pin level').
     */
	public void setOnHigh(boolean onIsHigh) {
		this.onIsHigh = onIsHigh;
	}

	public synchronized void init() {
		if (running.get())
			return;
		running.set(true);
		Thread pooling_thread = new Thread(()->{ checkSignalClosure(); });
		pooling_thread.setName("IRReceivePooling");
		pooling_thread.setDaemon(true);
		pooling_thread.start();
		Thread callback_thread = new Thread(()->{ processCallback(); });
		callback_thread.setName("IRReceiveCallback");
		callback_thread.setDaemon(true);
		callback_thread.start();
	}
	
	public synchronized void stop() {
		running.set(false);
		synchronized (running) {
			running.notifyAll();
		}
	}

    public void addShutdownHook() {
        Thread shutdown_hook = new Thread(()->{stop();gpio.shutdown();});
        shutdown_hook.setName("GPIOShutdownHook");
        shutdown_hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdown_hook);    	
    }
    
    /**
     * Get the number of pulses between two indexes.
     */
    private int signalLength(int start_point,int end_point) {
    	if (end_point>=start_point)
    		return end_point-start_point;
    	else
    		return (MAX_PULSES - start_point) + end_point;
    }
    
    /**
     * Take the modulus of a value. Negative values will be
     * considered as complementary value, as in a circular chain.<BR>
     * Example:<BR>
     * For modulus 3, the following values:<BR>
     * -5 -4 -3 -2 -1  0  1  2  3  4  5<BR>
     * Will result in:<BR>
     *  1  2  0  1  2  0  1  2  0  1  2<BR>
     */
    private static int modulus(int value,int mod) {
    	int x = value%mod;
    	if (x<0) x += mod;
    	return x;
    }
   
    /**
     * Pin level changed from HIGH (OFF) to LOW (ON)<BR>
     * Will update even indexes in 'incoming_signal' with
     * the delay of HIGH (OFF) pulse.
     */
    private void onSignalFalling(PinState newState) {
    	if (prevEdge==PinEdge.FALLING) {
    		// we missed 'rising' edge'
    		return;
    	}
    	prevEdge = PinEdge.FALLING;
    	pulseCount.increment();
    	long currentPulseEnd = System.nanoTime();
    	long delay = currentPulseEnd - currentPulseStart;
    	if (delay<RESOLUTION_NS) {
    		// Spurious signal
    		// Let's filter out this pulse and the previous one
    		if (currentPulse!=currentSignalStart) {
    			currentPulse = modulus((currentPulse|1)-2,MAX_PULSES); 	// back to previous odd index
    			currentPulseStart -= incoming_signal[currentPulse];		// continue from previous delay
    		}
    		return;
    	}
    	int delay_micro = ((delay>Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)delay)/1000;
    	final int current_pulse_index = currentPulse = modulus((currentPulse&~1),MAX_PULSES);	// even index
    	currentPulse = modulus(currentPulse+1,MAX_PULSES);	// next odd index
    	incoming_signal[current_pulse_index] = delay_micro;
    	if (minStartPulseDelay>0) {
    		if (!onIsHigh && currentSignalStart==current_pulse_index) {
    			// if we just started, need to start with an odd index (LOW level = ON state)
    			currentSignalStart = modulus(currentSignalStart+1,MAX_PULSES);
    	    	currentPulseStart = System.nanoTime(); // start of 'LOW' (ON) pulse
    			return;
    		}
    		if (incoming_signal[currentSignalStart]<minStartPulseDelay) {
    			// the first pulse was not long enough, so let's ignore this
    			currentSignalStart = currentPulse; // next index
    	    	currentPulseStart = System.nanoTime(); // start of 'LOW' (ON) pulse
    			return;
    		}
    	}
    	if (delay>MAX_PULSE_NS || signalLength(currentSignalStart,current_pulse_index+1)>=MAX_PULSES-1) {
    		onSignalClosure(currentSignalStart,current_pulse_index+1);
    	}
    	currentPulseStart = System.nanoTime(); // start of 'LOW' (ON) pulse
    }
    
    /**
     * Pin level changed from LOW (ON) to HIGH (OFF)
     * Will update odd indexes in 'incoming_signal' with
     * the delay of LOW (ON) pulse.
     */
    private void onSignalRising(PinState newState) {
    	if (prevEdge==PinEdge.RISING) {
    		// we missed 'falling' edge'
    		return;
    	}
    	prevEdge = PinEdge.RISING;
    	pulseCount.increment();
    	long currentPulseEnd = System.nanoTime();
    	long delay = currentPulseEnd - currentPulseStart;
    	if (delay<RESOLUTION_NS) {
    		// Spurious signal
    		// Let's filter out this pulse and the previous one
    		if (currentPulse!=currentSignalStart) {
    			currentPulse &= ~1;	// back to previous even index
    			currentPulseStart -= incoming_signal[currentPulse];		// continue from previous delay 
    		}
    		return;
    	}
    	int delay_micro = ((delay>Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)delay)/1000;
    	final int current_pulse_index = currentPulse = modulus((currentPulse|1),MAX_PULSES);	// odd index
    	currentPulse = modulus(currentPulse+1,MAX_PULSES);	// next even index
    	incoming_signal[current_pulse_index] = delay_micro;
    	if (minStartPulseDelay>0) {
    		if (onIsHigh && currentSignalStart==current_pulse_index) {
    			// if we just started and 'on is high', then we need to start with an even index
    			currentSignalStart = modulus(currentSignalStart+1,MAX_PULSES);
    	    	currentPulseStart = System.nanoTime(); // start of 'LOW' (ON) pulse
    			return;
    		}
    		if (incoming_signal[currentSignalStart]<minStartPulseDelay) {
    			// the first pulse was not long enough, so let's ignore this
    			currentSignalStart = currentPulse; // next index
    	    	currentPulseStart = System.nanoTime(); // start of 'LOW' (ON) pulse
    			return;
    		}
    	}
    	if (delay>MAX_PULSE_NS || signalLength(currentSignalStart,current_pulse_index+1)>=MAX_PULSES-1) {
    		onSignalClosure(currentSignalStart,current_pulse_index+1);
    	}
    	currentPulseStart = System.nanoTime();	// start of 'HIGH' (OFF) pulse
    }

    /**
     * Loops while IRReceive is not stopped<BR>
     * Check if current level is at the same level for long enough
     */
    private void checkSignalClosure() {
    	while (running.get()) {
    		try {
    			synchronized (running) {    			
    				running.wait(MAX_POOLING_DELAY);
    			}
		    	long currentPulseEnd = System.nanoTime();
		    	long delay = currentPulseEnd - currentPulseStart;
		    	if (delay>MAX_PULSE_NS) {
		    		if (previousSignalPulseCount!=pulseCount.longValue()) {
			    		onSignalClosure(currentSignalStart,currentPulse);
		    		}
		    	}
			} catch (InterruptedException e) {	}
    	}
    }
    
    /**
     * Gets called whenever we get a long enough pulse to consider as an being part of the signal itself
     * @param startPulsePosition Index in 'incoming_signal' that we'll consider as being the start of this sequence of pulses
     * @param excludingPulsePosition Index in 'incoming_signal' that we'll consider as being the end (excluding itself) of this sequence of pulses
     */
    private void onSignalClosure(int startPulsePosition, int excludingPulsePosition) {
    	previousSignalPulseCount = pulseCount.longValue();
    	if (pendingSignalSaturated.get()) {
    		// Did not finish processing previous signal, we'll ignore this one
        	currentSignalStart = modulus(excludingPulsePosition,MAX_PULSES);
    		return;
    	}
    	if (startPulsePosition==excludingPulsePosition) {    		
    		// No signal
    		return;
    	}
    	pendingSignalSaturated.set(true);
    	// Copies signal
    	int lastPulsePosition = modulus(excludingPulsePosition-1 , MAX_PULSES);
    	int length;
    	if (lastPulsePosition>=startPulsePosition) {
    		length = lastPulsePosition-startPulsePosition+1;
    		System.arraycopy(incoming_signal, /*srcPos*/startPulsePosition, pending_signal, /*destPos*/0, /*length*/length);
    		pending_signal_size = length;
    	}
    	else if (lastPulsePosition<startPulsePosition) {
    		int part1_length = (MAX_PULSES - startPulsePosition);
    		int part2_length = lastPulsePosition+1;
    		length = part1_length + part2_length;
    		System.arraycopy(incoming_signal, /*srcPos*/startPulsePosition, pending_signal, /*destPos*/0, /*length*/part1_length);
    		System.arraycopy(incoming_signal, /*srcPos*/0, pending_signal, /*destPos*/part1_length, /*length*/part2_length);
    		pending_signal_size = length;
    	}
		pending_signal_start_pin_level = ((startPulsePosition%2)==0) ? PinState.HIGH : PinState.LOW;
    	currentSignalStart = modulus(excludingPulsePosition,MAX_PULSES);
   		signalReceived.release();
    }
    
    /**
     * Wait for signal closure event.<BR>
     * Calls the callback function.
     */
    private void processCallback() {
    	while (running.get()) {
    		try {
	    		try {
					signalReceived.acquire();
				} catch (InterruptedException e) {
					if (!running.get())
						break;
				}
	    		callback.onSignalDetected(pending_signal,pending_signal_size,pending_signal_start_pin_level);
    		}
    		finally {
    			pendingSignalSaturated.set(false);
    		}
    	}
    }
}

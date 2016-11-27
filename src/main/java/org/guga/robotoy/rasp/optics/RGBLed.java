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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;

/**
 * Implements GPIO interface to a RGB led.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class RGBLed {

	private static final Logger log = Logger.getLogger(RGBLed.class.getName());

	/**
	 * Pins used for output of RED, GREEN and BLUE
	 */
	private final Pin pinR, pinG, pinB;
	
    private GpioPinPwmOutput outRed, outGreen, outBlue;

	private final DiodeType type;

    private GpioController gpio;
    
    private LedColor color;
    
    private AnimationThread animateThread;
    
    public static final int MAX_PWM_SOFT = 100;

	public static enum DiodeType {
		ANODE_COMMON,
		CATHODE_COMMON;
	}
	
	public RGBLed(DiodeType type,Pin red,Pin green,Pin blue) {
		this.type = type;
		this.pinR = red;
		this.pinG = green;
		this.pinB = blue;
		this.color = LedColor.OFF;
		gpio = GpioFactory.getInstance();
		int defaultValue = (DiodeType.ANODE_COMMON.equals(type)) ? 1 : 0;
		PinState defaultState = (DiodeType.ANODE_COMMON.equals(type)) ? PinState.HIGH : PinState.LOW;
		outRed = gpio.provisionSoftPwmOutputPin(pinR, "Red", defaultValue);
		outRed.setShutdownOptions(true, defaultState, PinPullResistance.OFF);
		outGreen = gpio.provisionSoftPwmOutputPin(pinG, "Green", defaultValue);
		outGreen.setShutdownOptions(true, defaultState, PinPullResistance.OFF);
		outBlue = gpio.provisionSoftPwmOutputPin(pinB, "Blue", defaultValue);
		outBlue.setShutdownOptions(true, defaultState, PinPullResistance.OFF);
	}
	
    public void addShutdownHook() {
        Thread shutdown_hook = new Thread(()->{
        	if (animateThread!=null)
        		animateThread.stopAnimation();
        	gpio.shutdown();
        });
        shutdown_hook.setName("GPIOShutdownHook");
        shutdown_hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdown_hook);    	
    }

	public void shutdown()
    {
    	if (gpio!=null) {
    		log.log(Level.INFO, "Shutting down  GPIO...");
    		gpio.shutdown();
    		gpio = null;
    	}
    }

	public LedColor getColor() {
		return color;
	}

	public void setColor(LedColor color) {
		setColor(color,/*stopAnimation*/true);
	}

	private void setColor(LedColor color,boolean stopAnimation) {
    	if (stopAnimation && animateThread!=null)
    		animateThread.stopAnimation();
		this.color = color;
		outputColor();
	}
	
	public void startCycleColors(int delay) {
    	if (animateThread!=null)
    		animateThread.stopAnimation();
    	animateThread = new AnimationThread(delay,/*expires*/0,/*colorAtRest*/null,
			()->{
				setColor(getColor().next(),/*stopAnimation*/false);
			});
    	animateThread.start();
	}

	public void startCycleColors(int delay,LedColor... colors) {
		startCycleColors(delay,/*timeout*/0,/*colorAtRest*/null,colors);
	}

	public void startCycleColors(int delay,int timeout,LedColor colorAtRest,LedColor... colors) {
    	if (animateThread!=null)
    		animateThread.stopAnimation();
    	AtomicInteger contador = new AtomicInteger(-1);
    	long expires = (timeout==0) ? 0 : (System.currentTimeMillis() + timeout);
    	animateThread = new AnimationThread(delay,expires,colorAtRest,
			()->{
				int count = contador.incrementAndGet();
				if (count==colors.length) {
					contador.set(0);
					count = 0;
				}
				setColor(colors[count],/*stopAnimation*/false);
			});
    	animateThread.start();
	}

	private int getPWMLevel(float value) {
		if (DiodeType.ANODE_COMMON.equals(type)) {
			if (value>=1.0f)
				return 0;
			else if (value==0)
				return MAX_PWM_SOFT;
			else
				return (int)(MAX_PWM_SOFT*(1.0f-value));
		}
		else {
			if (value>=1.0f)
				return MAX_PWM_SOFT;
			else if (value==0)
				return 0;
			else
				return (int)(MAX_PWM_SOFT*value);
		}
	}

	private void outputColor() {
		outRed.setPwm(getPWMLevel(color.getRed()));
		outGreen.setPwm(getPWMLevel(color.getGreen()));
		outBlue.setPwm(getPWMLevel(color.getBlue()));
	}
	
	private class AnimationThread extends Thread {
		private final int delay;
		private final long expires;
		private final AtomicBoolean stop;
		private final Runnable animation;
		private final LedColor colorAtRest;
		AnimationThread(int delay,long expires,LedColor colorAtRest,Runnable animation) {			
			this.delay = delay;
			this.expires = expires;
			this.animation = animation;
			this.stop = new AtomicBoolean();
			this.colorAtRest = colorAtRest;
			setDaemon(true);
		}
		@Override
		public void run() {
			while (!stop.get()) {
				if (expires>0 && System.currentTimeMillis()>expires)
					break;
				try {
					synchronized (stop) {
						stop.wait(delay);
					}
				}
				catch (InterruptedException e) {
					break;
				}
				if (expires>0 && System.currentTimeMillis()>expires)
					break;
				if (stop.get())
					break;
				animation.run();
			}
			if (colorAtRest!=null) {
				setColor(colorAtRest);
			}
		}
		public void stopAnimation() {
			stop.set(true);
			synchronized (stop) {
				stop.notifyAll();
			}
		}
	}
}

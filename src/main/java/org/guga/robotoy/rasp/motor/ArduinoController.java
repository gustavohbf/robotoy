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
package org.guga.robotoy.rasp.motor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;

/**
 * This class is used for making interface with the Arduino board
 * running the sketch related to the motor control.<BR>
 * Active Arduino sketch file: RoboToy.ino<BR>
 * <BR>
 * If you want to change the number of motors or their wirings over the motor driver board,
 * you have to change the Arduino sketch file accordingly.<BR>
 * <BR>
 * The Arduino program is expecting the following commands via serial port (baud rate 115200):<BR>
 * <BR>
 * l -> make a left turn by spinning the motors accordingly<BR>
 * r -> make a right turn by spinning the motors accordingly<BR>
 * f -> move forward by spinning the motors accordingly<BR>
 * b -> move backward by spinning the motors accordingly<BR>
 * s -> stop motors<BR>
 * P### -> change PWM level for all motors. Inform three digits
 * for PWM level (from 0 up to 255).<BR>
 * D###### -> change PWM level for left and right side motors idependently. Inform three digits
 * for left PWM level (from 0 up to 255) and three digits for right PWM level (from 0 up to 255).<BR>
 * <BR>
 * Some special additional commands (not used here):<BR>
 * x######## -> with each '#' either 0 or 1, this will send these 8 signals to the latch port
 * in motor driver shield. This is only used for debugging purposes.<BR> 
 * L### -> change PWM level for left motors. Inform three digits
 * for PWM level (from 0 up to 255).<BR>
 * R### -> change PWM level for right motors. Inform three digits
 * for PWM level (from 0 up to 255).<BR>
 * <BR>
 * You can try Arduino with other sketchs and other driver control hardware as long as you
 * keep the same interface with this implementation.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ArduinoController implements Motor {
	
	private static final Logger log = Logger.getLogger(ArduinoController.class.getName());

	/**
	 * Time in milliseconds to block waiting for port open
	 */
	private static final int DEFAULT_TIMEOUT = 2000;
	
	private static final int DEFAULT_BAUDRATE = 115200;
	
	private static final int DEFAULT_DATABITS = SerialPort.DATABITS_8;
	
	private static final int DEFAULT_STOPBITS = SerialPort.STOPBITS_1;
	
	private static final int DEFAULT_PARITY = SerialPort.PARITY_NONE;
	
	private static final int MAX_PWM_LEVEL = 255;
		
	public static CommPortIdentifier findSerialPort() throws Exception {
		CommPortIdentifier serialPort = null;
        @SuppressWarnings("unchecked")
		java.util.Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        while ( portEnum.hasMoreElements() ) 
        {
        	CommPortIdentifier portIdentifier = portEnum.nextElement();
        	if (CommPortIdentifier.PORT_SERIAL==portIdentifier.getPortType()) {
        		if (serialPort==null)
        			serialPort = portIdentifier;
        		else
        			throw new Exception("More than one serial port found! "+serialPort.getName()+" and "+portIdentifier.getName());
        	}
        }
        return serialPort;
	}

	private CommPortIdentifier controllerPort;
	
	private SerialPort controller;
	
	private InputStream inputFromController;
	
	private OutputStream outputToController;
	
	private volatile boolean running;
	
	private volatile boolean moving;
	
    private double previous_speed_left;
    
    private double previous_speed_right;

	public static class Config {
		private int baudrate = DEFAULT_BAUDRATE;
		
		private int databits = DEFAULT_DATABITS;
		
		private int stopbits = DEFAULT_STOPBITS;
		
		private int parity = DEFAULT_PARITY;

		public int getBaudrate() {
			return baudrate;
		}

		public void setBaudrate(int baudrate) {
			this.baudrate = baudrate;
		}

		public int getDatabits() {
			return databits;
		}

		public void setDatabits(int databits) {
			this.databits = databits;
		}

		public int getStopbits() {
			return stopbits;
		}

		public void setStopbits(int stopbits) {
			this.stopbits = stopbits;
		}

		public int getParity() {
			return parity;
		}

		public void setParity(int parity) {
			this.parity = parity;
		}
	}
	
	private Config config = new Config();

    /**
     * Keeps track of current state for left side motors
     */
    private double leftFactor;
    
    /**
     * Keeps track of current state for right side motors
     */
    private double rightFactor;
 
	private static final DecimalFormat df3 = new DecimalFormat("000");
	
	private static double EPSILON = Math.ulp(1.0);

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public CommPortIdentifier getControllerPort() {
		return controllerPort;
	}

	public void setControllerPort(CommPortIdentifier controllerPort) {
		this.controllerPort = controllerPort;
	}

	public void setControllerPort(String portName) throws NoSuchPortException {
		this.controllerPort = CommPortIdentifier.getPortIdentifier(portName);
	}

	public void addShutdownHook() {
        Thread shutdown_hook = new Thread(()->{shutdown(/*stopMotors*/true);});
        shutdown_hook.setName("ArduinoControllerShutdownHook");
        shutdown_hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdown_hook);    	
    }
    
    @Override
    protected void finalize() {
    	try {
    		shutdown(/*stopMotors*/true);
    	} catch (Throwable e) {
			log.log(Level.SEVERE,"Error while shutting down", e);
    	}
    }
    
    public synchronized void shutdown(boolean stopMotors) {
    	if (controller==null)
    		return;
    	log.log(Level.INFO, "Shutting down serial communication ...");
    	if (stopMotors) {
    		try {
    			stop();
    		}
    		catch (Throwable e){ }
    	}
    	running = false;
    	try { Thread.sleep(100); }
    	catch (Throwable e){ }
    	if (outputToController!=null) {
    		try {
				outputToController.close();
			} catch (IOException e) {	}
    	}
    	final Semaphore sem = new Semaphore(0);
    	new Thread(()->{
	    	if (inputFromController!=null) {
	    		try {
					inputFromController.close();
				} catch (IOException e) {	}
	    	}
	    	try {
	    		controller.removeEventListener();
	    	} catch (Exception e) {	}
	    	try {
	    		controller.close();
	    	} catch (Exception e) {	}
	    	sem.release();
    	}).start();
    	try {
    		sem.tryAcquire(5, TimeUnit.SECONDS);
    	}
    	catch (InterruptedException e){ }
    	log.log(Level.INFO, "Shutting down serial communication ... done!");
    	controller = null;
    }
    
    private void assertControllerPort() throws Exception {
    	if (controllerPort!=null)
    		return;
    	controllerPort = findSerialPort();
    	if (controllerPort==null)
    		throw new Exception("Could not find an available serial port!");
    }
    
    private void assertController() throws Exception {
    	if (controller!=null)
    		return;
    	assertControllerPort();
    	System.out.println("Starting serial communication with "+controllerPort.getName()+", baud rate "+config.getBaudrate()+", "+config.getDatabits()
    		+" data bits, "+config.getStopbits()+" stopbits");
    	if (controllerPort.isCurrentlyOwned()) {
    		throw new Exception("Port "+controllerPort.getName()+" is currently in use!");
    	}
    	CommPort commPort = controllerPort.open(this.getClass().getName(),DEFAULT_TIMEOUT);
    	if (!(commPort instanceof SerialPort))
    		throw new Exception("Only serial ports are handled by this controller!");
    	
    	controller = (SerialPort)commPort;
    	controller.setSerialPortParams(config.getBaudrate(),config.getDatabits(),config.getStopbits(),config.getParity());
    	inputFromController = controller.getInputStream();
    	outputToController = controller.getOutputStream();
    	Thread t = new Thread(this::talkToSerialPort);
    	t.setDaemon(true);
    	running = true;
    	t.start();
    }
    
    private void reopenController() {
    	
    	// TODO: should try to reopen communication port in case of failure
    	// The following code does not work
    	/*
    	if (controller!=null) {
	    	try {
	    		shutdown(false);
	    	}
	    	catch (Throwable e){ }
    	}
    	controller = null;
    	try {
    		assertController();
    	}
    	catch (Throwable e){
			log.log(Level.SEVERE,"Error while reopening serial port", e);
    	}
    	*/
    }

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#moveForward(double)
	 */
	@Override
	public synchronized void moveForward(double speedFactor) {
		try {
			assertController();
			if (previous_speed_left!=speedFactor
					|| previous_speed_right!=speedFactor) {
				setSpeed(speedFactor);
			}
	        outputToController.write('f');
	        outputToController.flush();
	        moving = true;
	        // remember factors
	        leftFactor = speedFactor;
	        rightFactor = speedFactor;
		}
		catch (Exception e) {
			log.log(Level.SEVERE,"Error while submitting command to serial port", e);
			reopenController();
		}
	}

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#moveBackward(double)
	 */
	@Override
	public synchronized void moveBackward(double speedFactor) {
		try {
			assertController();
			if (previous_speed_left!=speedFactor
					|| previous_speed_right!=speedFactor) {
				setSpeed(speedFactor);
			}
	        outputToController.write('b');
	        outputToController.flush();
	        moving = true;
	        // remember factors
	        leftFactor = -speedFactor;
	        rightFactor = -speedFactor;
		}
		catch (Exception e) {
			log.log(Level.SEVERE,"Error while submitting command to serial port", e);
			reopenController();
		}
	}

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#turnLeft(double)
	 */
	@Override
	public synchronized void turnLeft(double speedFactor) {
		try {
			assertController();
			if (previous_speed_left!=speedFactor
					|| previous_speed_right!=speedFactor) {
				setSpeed(speedFactor);
			}
	        outputToController.write('l');
	        outputToController.flush();
	        moving = true;
	        // remember factors
	        leftFactor = -speedFactor;
	        rightFactor = speedFactor;
		}
		catch (Exception e) {
			log.log(Level.SEVERE,"Error while submitting command to serial port", e);
			reopenController();
		}
	}

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#turnRight(double)
	 */
	@Override
	public synchronized void turnRight(double speedFactor) {
		try {
			assertController();
			if (previous_speed_left!=speedFactor
					|| previous_speed_right!=speedFactor) {
				setSpeed(speedFactor);
			}
	        outputToController.write('r');
	        outputToController.flush();
	        moving = true;
	        // remember factors
	        leftFactor = speedFactor;
	        rightFactor = -speedFactor;
		}
		catch (Exception e) {
			log.log(Level.SEVERE,"Error while submitting command to serial port", e);
			reopenController();
		}
	}

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#stop()
	 */
	@Override
	public synchronized void stop() {
		try {
			assertController();
			setSpeed(0.0);
	        outputToController.write('s');
	        outputToController.flush();
	        moving = false;
	        // remember factors
	        leftFactor = rightFactor = 0.0;
		}
		catch (Exception e) {
			log.log(Level.SEVERE,"Error while submitting command to serial port", e);
		}
	}

	@Override
	public boolean isMoving() {
		return moving;
	}

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#setSpeed(double)
	 */
	@Override
	public void setSpeed(double speedFactor) {
		
		if (previous_speed_left==speedFactor
				&& previous_speed_right==speedFactor) {
				return;
			}

		try {
			assertController();
			int factor = toPWMLevel(speedFactor);
			outputToController.write('P');
			outputToController.write(df3.format(factor).getBytes());
			outputToController.flush();
			// remember factors and speed
			previous_speed_left = previous_speed_right = speedFactor;
			leftFactor = (leftFactor<0) ? -speedFactor : speedFactor;
			rightFactor = (rightFactor<0) ? - speedFactor : speedFactor;
		}
		catch (Exception e) {
			log.log(Level.SEVERE,"Error while submitting command to serial port", e);
			reopenController();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#setMovement(double,double)
	 */
	@Override
	public synchronized void setMovement(double leftFactor,double rightFactor) {
		if (this.leftFactor==leftFactor
			&& this.rightFactor==rightFactor)
			return;
		final double leftSpeed = Math.abs(leftFactor);
		final double rightSpeed = Math.abs(rightFactor);
		final boolean leftForward = leftFactor>=0;
		final boolean rightForward = rightFactor>=0;
		final boolean stopped = ((leftSpeed < EPSILON) && (rightSpeed < EPSILON));
		
		try {
			assertController();
			int factorLeft = toPWMLevel(leftSpeed);
			int factorRight = toPWMLevel(rightSpeed);
			if (stopped) {
		        outputToController.write('s');
		        outputToController.flush();
		        moving = false;				
			}
			if (!stopped) {
				ByteArrayOutputStream cmd_output = new ByteArrayOutputStream(8);
				cmd_output.write('D');
				cmd_output.write(df3.format(factorLeft).getBytes());
				cmd_output.write(df3.format(factorRight).getBytes());
				if (leftForward && rightForward)
					cmd_output.write('f');
				else if (!leftForward && !rightForward)
					cmd_output.write('b');
				else if (leftForward)
					cmd_output.write('r');
				else
					cmd_output.write('l');
				byte[] cmd_contents = cmd_output.toByteArray();
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,"Sending command: "+new String(cmd_contents,"UTF-8"));
				}
				outputToController.write(cmd_contents);
				outputToController.flush();
				moving = true;
			}
			// remember factors and speed
			this.rightFactor = rightFactor;
			this.leftFactor = leftFactor;
			this.previous_speed_left = leftSpeed;
			this.previous_speed_right = rightSpeed;
		}
		catch (Exception e) {
			log.log(Level.SEVERE,"Error while submitting command to serial port", e);
			reopenController();
		}
	}
	
	private static int toPWMLevel(double speedFactor) {
		int factor = (int)(speedFactor*MAX_PWM_LEVEL);
		if (factor<0)
			factor = 0;
		if (factor>MAX_PWM_LEVEL)
			factor = MAX_PWM_LEVEL;
		return factor;
	}

	/**
	 * Keep a thread running for reading the serial port.
	 */
	private void talkToSerialPort() {
		Throwable prev_error = null;
		while (running) {
			byte[] buffer = new byte[ 1024 ];
			int len = -1;
			StringBuilder line = new StringBuilder();
			try {
				while( running && ( len = inputFromController.read( buffer ) ) > -1 ) {
					String received = new String( buffer, 0, len );
					if (log.isLoggable(Level.FINEST)) {
						line.append(received);
						int eol;
						while ((eol=line.indexOf("\n"))>=0) {
							String part = line.substring(0, eol);
							if (part.endsWith("\r"))
								part = part.substring(0, part.length()-1);
							line.delete(0, eol+1);
							log.log(Level.FINEST, "Arduino: "+part);
						}
					}
				}
			} catch( IOException e ) {
				if (running) {
					if (prev_error==null 
						|| !prev_error.getClass().equals(e.getClass())
						|| prev_error.getMessage()==null
						|| !prev_error.getMessage().equals(e.getMessage())) {
						log.log(Level.SEVERE, "Error while reading from serial port", e);
					}
				}
				prev_error = e;
			}
		}
	}

}

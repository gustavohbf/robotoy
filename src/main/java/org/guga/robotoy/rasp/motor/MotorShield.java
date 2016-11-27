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

import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pi4j.io.gpio.*;

/**
 * Controller for Adafruit Motor Shield using Raspberry GPIO directly (without Arduino)
 * 
 * @author Gustavo Figueiredo
 */
public class MotorShield implements Motor
{
	private static final Logger log = Logger.getLogger(MotorShield.class.getName());

    public static final int DEFAULT_FRONT_LEFT = 2;
    public static final int DEFAULT_FRONT_RIGHT = 1;
    public static final int DEFAULT_REAR_LEFT = 3;
    public static final int DEFAULT_REAR_RIGHT = 4;

	public static class PinLayout {
		private Pin pinMotorLatch = RaspiPin.GPIO_00;
		private Pin pinMotorClock = RaspiPin.GPIO_04;
		private Pin pinMotorEnable = RaspiPin.GPIO_02;
		private Pin pinMotorData = RaspiPin.GPIO_03;
		private Pin pinPWM1 = RaspiPin.GPIO_05;
		private Pin pinPWM2 = null;
		private Pin pinPWM3 = null;
		private Pin pinPWM4 = null;
		private PWMType pwmType = PWMType.SINGLE_SOFTWARE_PWM;
		private boolean reversedMotorsDirection = false;
		private int motorFrontLeft = DEFAULT_FRONT_LEFT;
		private int motorFrontRight = DEFAULT_FRONT_RIGHT;
		private int motorRearLeft = DEFAULT_REAR_LEFT;
		private int motorRearRight = DEFAULT_REAR_RIGHT;
		private Wheels wheels = Wheels.FOUR;
		public Pin getPinMotorLatch() {
			return pinMotorLatch;
		}
		public void setPinMotorLatch(Pin pinMotorLatch) {
			this.pinMotorLatch = pinMotorLatch;
		}
		public void setPinMotorLatch(String pinMotorLatch) {
			this.pinMotorLatch = parsePin(pinMotorLatch);
		}
		public Pin getPinMotorClock() {
			return pinMotorClock;
		}
		public void setPinMotorClock(Pin pinMotorClock) {
			this.pinMotorClock = pinMotorClock;
		}
		public void setPinMotorClock(String pinMotorClock) {
			this.pinMotorClock = parsePin(pinMotorClock);
		}
		public Pin getPinMotorEnable() {
			return pinMotorEnable;
		}
		public void setPinMotorEnable(Pin pinMotorEnable) {
			this.pinMotorEnable = pinMotorEnable;
		}
		public void setPinMotorEnable(String pinMotorEnable) {
			this.pinMotorEnable = parsePin(pinMotorEnable);
		}
		public Pin getPinMotorData() {
			return pinMotorData;
		}
		public void setPinMotorData(Pin pinMotorData) {
			this.pinMotorData = pinMotorData;
		}
		public void setPinMotorData(String pinMotorData) {
			this.pinMotorData = parsePin(pinMotorData);
		}
		public Pin getPinPWM1() {
			return pinPWM1;
		}
		public void setPinPWM1(Pin pinPWM1) {
			this.pinPWM1 = pinPWM1;
		}
		public void setPinPWM1(String pinPWM1) {
			this.pinPWM1 = parsePin(pinPWM1);
		}
		public Pin getPinPWM2() {
			return pinPWM2;
		}
		public void setPinPWM2(Pin pinPWM2) {
			this.pinPWM2 = pinPWM2;
		}
		public void setPinPWM2(String pinPWM2) {
			this.pinPWM2 = parsePin(pinPWM2);
		}
		public Pin getPinPWM3() {
			return pinPWM3;
		}
		public void setPinPWM3(Pin pinPWM3) {
			this.pinPWM3 = pinPWM3;
		}
		public void setPinPWM3(String pinPWM3) {
			this.pinPWM3 = parsePin(pinPWM3);
		}
		public Pin getPinPWM4() {
			return pinPWM4;
		}
		public void setPinPWM4(Pin pinPWM4) {
			this.pinPWM4 = pinPWM4;
		}
		public void setPinPWM4(String pinPWM4) {
			this.pinPWM4 = parsePin(pinPWM4);
		}
		public PWMType getPwmType() {
			return pwmType;
		}
		public void setPwmType(PWMType pwmType) {
			this.pwmType = pwmType;
		}
		public void setPwmType(String pwmType) {
			this.pwmType = PWMType.valueOf(pwmType);
		}
		public boolean isReversedMotorsDirection() {
			return reversedMotorsDirection;
		}
		public void setReversedMotorsDirection(boolean reversedMotorsDirection) {
			this.reversedMotorsDirection = reversedMotorsDirection;
		}
		public int getMotorFrontLeft() {
			return motorFrontLeft;
		}
		public void setMotorFrontLeft(int motorFrontLeft) {
			this.motorFrontLeft = motorFrontLeft;
		}
		public int getMotorFrontRight() {
			return motorFrontRight;
		}
		public void setMotorFrontRight(int motorFrontRight) {
			this.motorFrontRight = motorFrontRight;
		}
		public int getMotorRearLeft() {
			return motorRearLeft;
		}
		public void setMotorRearLeft(int motorRearLeft) {
			this.motorRearLeft = motorRearLeft;
		}
		public int getMotorRearRight() {
			return motorRearRight;
		}
		public void setMotorRearRight(int motorRearRight) {
			this.motorRearRight = motorRearRight;
		}
		private Pin parsePin(String input) {
			if (input==null || input.length()==0)
				return null;
			if (input.matches("\\d+")) {
				int num = Integer.parseInt(input);
				return RaspiPin.getPinByAddress(num);
			}
			else {
				return RaspiPin.getPinByName(input);
			}
		}
		public Wheels getWheels() {
			return wheels;
		}
		public void setWheels(Wheels wheels) {
			this.wheels = wheels;
		}
		public void setWheels(String wheels) {
			this.wheels = Wheels.valueOf(wheels);
		}
	}

    private GpioController gpio;
    
    private PinLayout pinLayout;
    
    private GpioPinPwmOutput MOTOR_1_PWM;
    private GpioPinPwmOutput MOTOR_2_PWM;
    private GpioPinPwmOutput MOTOR_3_PWM;
    private GpioPinPwmOutput MOTOR_4_PWM;

    private GpioPinDigitalOutput MOTOR_1_STEADY;
    private GpioPinDigitalOutput MOTOR_2_STEADY;
    private GpioPinDigitalOutput MOTOR_3_STEADY;
    private GpioPinDigitalOutput MOTOR_4_STEADY;

    private GpioPinDigitalOutput MOTORLATCH;
    private GpioPinDigitalOutput MOTORCLK;
    private GpioPinDigitalOutput MOTORENABLE;
    private GpioPinDigitalOutput MOTORDATA;
    
    private static final int MOTOR1_A = 2;
    private static final int MOTOR1_B = 3;
    private static final int MOTOR2_A = 1;
    private static final int MOTOR2_B = 4;
    private static final int MOTOR4_A = 0;
    private static final int MOTOR4_B = 6;
    private static final int MOTOR3_A = 5;
    private static final int MOTOR3_B = 7;
    
    private final BitSet latch_state;
    
    private final BitSet previous_latch_state;
        
    /**
     * Keeps track of current state for left side motors
     */
    private double leftFactor;
    
    /**
     * Keeps track of current state for right side motors
     */
    private double rightFactor;

    public static final int MAX_SPEED_HARD = 1023;
    
    public static final int MAX_SPEED_SOFT = 100;
    
	private static double EPSILON = Math.ulp(1.0);

    /**
     * Commands for singular motors
     *
     */
    public static enum Command {
        FORWARD,
        BACKWARD,
        RELEASE;
    }
    
    private double previous_speed_left;
    private double previous_speed_right;
    
    public static enum PWMType {
    	SINGLE_HARDWARE_PWM,
    	SINGLE_SOFTWARE_PWM,
    	ALL_SOFTWARE_PWM,
    	SINGLE_STEADY,
    	ALL_STEADY;
    }
    
    /**
     * Number of motorized wheels
     */
    public static enum Wheels {
    	TWO_REAR,
    	TWO_FRONT,
    	FOUR;
    }
    
    public MotorShield() 
    {
    	latch_state = new BitSet(8);
    	previous_latch_state = new BitSet(8);
    	pinLayout = new PinLayout();
        gpio = GpioFactory.getInstance();
    }
    
    public void addShutdownHook() {
        Thread shutdown_hook = new Thread(()->{gpio.shutdown();});
        shutdown_hook.setName("GPIOShutdownHook");
        shutdown_hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdown_hook);    	
    }

    
    public PinLayout getPinLayout() {
		return pinLayout;
	}

	public void setPinLayout(PinLayout pinLayout) {
		this.pinLayout = pinLayout;
	}

	public void shutdown()
    {
    	if (gpio!=null) {
    		log.log(Level.INFO, "Shutting down  GPIO...");
    		gpio.shutdown();
    		gpio = null;
    	}
    }
    
    @Override
    protected void finalize() {
    	shutdown();
    }
    
    public void enable()
    {
    	if (log.isLoggable(Level.FINEST)) {
    		log.log(Level.FINEST, "PWM Type: "+pinLayout.getPwmType().name());
    		log.log(Level.FINEST, "wheels: "+pinLayout.getWheels());
    		log.log(Level.FINEST, "pinMotorEnable:" +pinLayout.getPinMotorEnable());
    		log.log(Level.FINEST, "pinMotorLatch:" +pinLayout.getPinMotorLatch());
    		log.log(Level.FINEST, "pinMotorClock:" +pinLayout.getPinMotorClock());
    		log.log(Level.FINEST, "pinMotorData:" +pinLayout.getPinMotorData());
    		log.log(Level.FINEST, "pinPWM1:" +pinLayout.getPinPWM1());
    		log.log(Level.FINEST, "pinPWM2:" +pinLayout.getPinPWM2());
    		log.log(Level.FINEST, "pinPWM3:" +pinLayout.getPinPWM3());
    		log.log(Level.FINEST, "pinPWM4:" +pinLayout.getPinPWM4());
    		log.log(Level.FINEST, "reversedMotorsDirection: "+pinLayout.isReversedMotorsDirection());
    		log.log(Level.FINEST, "motorFrontLeft: "+pinLayout.getMotorFrontLeft());
    		log.log(Level.FINEST, "motorFrontRight: "+pinLayout.getMotorFrontRight());
    		log.log(Level.FINEST, "motorRearLeft: "+pinLayout.getMotorRearLeft());
    		log.log(Level.FINEST, "motorRearRight: "+pinLayout.getMotorRearRight());
    	}
        switch (pinLayout.getPwmType()) {
        case ALL_SOFTWARE_PWM:
        	if (Wheels.FOUR.equals(pinLayout.getWheels())) {
	            MOTOR_1_PWM = gpio.provisionSoftPwmOutputPin(pinLayout.getPinPWM1(), "MOTOR_1_PWM", 0);
	            MOTOR_1_PWM.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
	            MOTOR_2_PWM = gpio.provisionSoftPwmOutputPin(pinLayout.getPinPWM2(), "MOTOR_2_PWM", 0);
	            MOTOR_2_PWM.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        	}
            MOTOR_3_PWM = gpio.provisionSoftPwmOutputPin(pinLayout.getPinPWM3(), "MOTOR_3_PWM", 0);
            MOTOR_3_PWM.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
            MOTOR_4_PWM = gpio.provisionSoftPwmOutputPin(pinLayout.getPinPWM4(), "MOTOR_4_PWM", 0);
            MOTOR_4_PWM.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        	if (Wheels.FOUR.equals(pinLayout.getWheels())) {
	            MOTOR_1_PWM.setPwm(0);
	            MOTOR_2_PWM.setPwm(0);
        	}
            MOTOR_3_PWM.setPwm(0);
            MOTOR_4_PWM.setPwm(0);
            break;
        case SINGLE_HARDWARE_PWM:
            MOTOR_1_PWM = gpio.provisionPwmOutputPin(pinLayout.getPinPWM1(), "MOTOR_PWM", 0);
            MOTOR_1_PWM.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
            MOTOR_1_PWM.setPwm(0);
        	break;
        case SINGLE_SOFTWARE_PWM:
            MOTOR_1_PWM = gpio.provisionSoftPwmOutputPin(pinLayout.getPinPWM1(), "MOTOR_PWM", 0);
            MOTOR_1_PWM.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
            MOTOR_1_PWM.setPwm(0);
        	break;
        case SINGLE_STEADY:
            MOTOR_1_STEADY = gpio.provisionDigitalOutputPin(pinLayout.getPinPWM1(), "MOTOR_PWM", PinState.LOW);
            MOTOR_1_STEADY.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
            MOTOR_1_STEADY.low();
        	break;
        case ALL_STEADY:
        	if (Wheels.FOUR.equals(pinLayout.getWheels())) {
	            MOTOR_1_STEADY = gpio.provisionDigitalOutputPin(pinLayout.getPinPWM1(), "MOTOR_1_PWM", PinState.LOW);
	            MOTOR_1_STEADY.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
	            MOTOR_2_STEADY = gpio.provisionDigitalOutputPin(pinLayout.getPinPWM2(), "MOTOR_2_PWM", PinState.LOW);
	            MOTOR_2_STEADY.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        	}
            MOTOR_3_STEADY = gpio.provisionDigitalOutputPin(pinLayout.getPinPWM3(), "MOTOR_3_PWM", PinState.LOW);
            MOTOR_3_STEADY.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
            MOTOR_4_STEADY = gpio.provisionDigitalOutputPin(pinLayout.getPinPWM4(), "MOTOR_4_PWM", PinState.LOW);
            MOTOR_4_STEADY.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        	if (Wheels.FOUR.equals(pinLayout.getWheels())) {
	            MOTOR_1_STEADY.low();
	            MOTOR_2_STEADY.low();
        	}
            MOTOR_3_STEADY.low();
            MOTOR_4_STEADY.low();
        	break;
        }

    	MOTORLATCH = gpio.provisionDigitalOutputPin(pinLayout.getPinMotorLatch(), "MOTORLATCH", PinState.LOW);
        MOTORLATCH.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        MOTORENABLE = gpio.provisionDigitalOutputPin(pinLayout.getPinMotorEnable(), "MOTORENABLE", PinState.LOW);
        MOTORENABLE.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        MOTORDATA = gpio.provisionDigitalOutputPin(pinLayout.getPinMotorData(), "MOTORDATA", PinState.LOW);
        MOTORDATA.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        MOTORCLK = gpio.provisionDigitalOutputPin(pinLayout.getPinMotorClock(), "MOTORCLK", PinState.LOW);
        MOTORCLK.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        latch_state.clear();
        latch_tx();
        MOTORENABLE.low();
    }
    
    public void motorInit(int motor)
    {
        switch (motor)
        {
            case 1:
            	latch_state.clear(MOTOR1_A);
            	latch_state.clear(MOTOR1_B);
                latch_tx();
                break;
            case 2:
            	latch_state.clear(MOTOR2_A);
            	latch_state.clear(MOTOR2_B);
                latch_tx();
                break;
            case 3:
            	latch_state.clear(MOTOR3_A);
            	latch_state.clear(MOTOR3_B);
                latch_tx();
                break;
            case 4:
            	latch_state.clear(MOTOR4_A);
            	latch_state.clear(MOTOR4_B);
                latch_tx();
                break;
        };
    }
    
    public void issueCommand(int motor,Command cmd)
    {
        int a, b;
        switch (motor)
        {
            case 1:
                a = MOTOR1_A; b = MOTOR1_B;
                break;
            case 2:
                a = MOTOR2_A; b = MOTOR2_B;
                break;
            case 3:
                a = MOTOR3_A; b = MOTOR3_B;
                break;
            case 4:
                a = MOTOR4_A; b = MOTOR4_B;
                break;
            default:
                throw new UnsupportedOperationException("motor "+motor);
        }
        switch (cmd)
        {
            case FORWARD:
            	if (pinLayout.isReversedMotorsDirection()) {
                	latch_state.clear(a);
                	latch_state.set(b);            		
            	}
            	else {
	            	latch_state.set(a);
	            	latch_state.clear(b);
            	}
                break;
            case BACKWARD:
            	if (pinLayout.isReversedMotorsDirection()) {
	            	latch_state.set(a);
	            	latch_state.clear(b);            		
            	}
            	else {
	            	latch_state.clear(a);
	            	latch_state.set(b);
            	}
                break;
            case RELEASE:
            	latch_state.clear(a);
            	latch_state.clear(b);
                break;
             default:
            	 log.log(Level.WARNING, "Unknown command: "+cmd);
        }
    }
    
    public void submit() 
    {
    	if (previous_latch_state.equals(latch_state))
    		return;
        latch_tx();
        previous_latch_state.clear();
        previous_latch_state.or(latch_state);
    }
    
    /**
     * Defines speed for all the DC motors using PWM. This can be generated
     * by hardware or it can be software emulated, depending on how you configured
     * this class.
     * @param speedFactor Factor between 0.0 (no PWM signal) up to 1.0 (maximum PWM signal)
     */
    @Override
    public void setSpeed(double speedFactor)
    {
		if (previous_speed_left==speedFactor
			&& previous_speed_right==speedFactor) {
			return;
		}

    	switch (pinLayout.getPwmType())
    	{
    	case SINGLE_HARDWARE_PWM:
    	case SINGLE_SOFTWARE_PWM:
    	case SINGLE_STEADY:
    		setSpeed(1,toPWMLevel(speedFactor));
    		break;
    	case ALL_SOFTWARE_PWM:
    	case ALL_STEADY:
    		int speed = toPWMLevel(speedFactor);
        	if (Wheels.FOUR.equals(pinLayout.getWheels())) {
	    		setSpeed(1,speed);
	    		setSpeed(2,speed);
        	}
    		setSpeed(3,speed);
    		setSpeed(4,speed);
    		break;
    	}

    	// remember speed
		previous_speed_left = previous_speed_right = speedFactor;
		leftFactor = (leftFactor<0) ? -speedFactor : speedFactor;
		rightFactor = (rightFactor<0) ? - speedFactor : speedFactor;
    }
    
	private int toPWMLevel(double speedFactor) {
    	switch (pinLayout.getPwmType())
    	{
    	case SINGLE_HARDWARE_PWM:
    		return (int)Math.min(MAX_SPEED_HARD,speedFactor*MAX_SPEED_HARD);
    	case SINGLE_SOFTWARE_PWM:
    	case SINGLE_STEADY:
    	case ALL_SOFTWARE_PWM:
    	case ALL_STEADY:
    		return (int)Math.min(MAX_SPEED_SOFT,speedFactor*MAX_SPEED_SOFT);
    	default:
    		throw new UnsupportedOperationException("pwm type: "+pinLayout.getPwmType());
    	}
	}
	
    /**Defines speed for one DC motor using PWM. This can be generated
     * by hardware or it can be software emulated, depending on how you configured
     * this class.
     * @param num_pwm Identifies the PWM generator that should be set.
     * @param speed Defines the PWM speed. Maximum of 100 for software emulation and maximum of 1023 for hardware generated.
     */
    public void setSpeed(int num_pwm,int speed) 
    {
    	final boolean use_pwm = !PWMType.SINGLE_STEADY.equals(pinLayout.getPwmType())
    			&& !PWMType.ALL_STEADY.equals(pinLayout.getPwmType());
    	if (use_pwm) {
	        GpioPinPwmOutput pin_pwm;
	        switch (num_pwm)
	        {
	            case 1:
	                pin_pwm = MOTOR_1_PWM;
	                break;
	            case 2:
	                pin_pwm = MOTOR_2_PWM;
	                break;
	            case 3:
	                pin_pwm = MOTOR_3_PWM;
	                break;
	            case 4:
	                pin_pwm = MOTOR_4_PWM;
	                break;
	            default:
	                throw new UnsupportedOperationException("pwm "+num_pwm);
	        }
	        pin_pwm.setPwm(speed);
    	}
    	else {
    		GpioPinDigitalOutput pin_motor;
	        switch (num_pwm)
	        {
	            case 1:
	            	pin_motor = MOTOR_1_STEADY;
	                break;
	            case 2:
	            	pin_motor = MOTOR_2_STEADY;
	                break;
	            case 3:
	            	pin_motor = MOTOR_3_STEADY;
	                break;
	            case 4:
	            	pin_motor = MOTOR_4_STEADY;
	                break;
	            default:
	                throw new UnsupportedOperationException("pwm "+num_pwm);
	        }
	        if (speed>50)
	        	pin_motor.high();
	        else
	        	pin_motor.low();
    	}
    }

    private void latch_tx()
    {
        MOTORLATCH.low();
        MOTORDATA.low();
        for (int i=0;i<8;i++)
        {
            MOTORCLK.low();
            if (latch_state.get(7-i))
            {
                MOTORDATA.high();
            }
            else
            {
                MOTORDATA.low();
            }
            MOTORCLK.high();
        }
        MOTORLATCH.high();
    }

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#moveForward(double)
	 */
	@Override
	public void moveForward(double speedFactor) {
		moveForward();
		if (previous_speed_left!=speedFactor
				|| previous_speed_right!=speedFactor) {
			setSpeed(speedFactor);
		}
        leftFactor = speedFactor;
        rightFactor = speedFactor;
	}
	
	private void moveForward() {
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_FRONT.equals(pinLayout.getWheels())) {
			issueCommand(pinLayout.getMotorFrontLeft(), Command.FORWARD);
			issueCommand(pinLayout.getMotorFrontRight(), Command.FORWARD);
    	}
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_REAR.equals(pinLayout.getWheels())) {
			issueCommand(pinLayout.getMotorRearLeft(), Command.FORWARD);
			issueCommand(pinLayout.getMotorRearRight(), Command.FORWARD);
    	}
		submit();		
	}

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#moveBackward(double)
	 */
	@Override
	public void moveBackward(double speedFactor) {
		moveBackward();
		if (previous_speed_left!=speedFactor
				|| previous_speed_right!=speedFactor) {
			setSpeed(speedFactor);
		}
        leftFactor = -speedFactor;
        rightFactor = -speedFactor;
	}
	
	private void moveBackward() {
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_FRONT.equals(pinLayout.getWheels())) {
			issueCommand(pinLayout.getMotorFrontLeft(), Command.BACKWARD);
			issueCommand(pinLayout.getMotorFrontRight(), Command.BACKWARD);
    	}
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_REAR.equals(pinLayout.getWheels())) {
			issueCommand(pinLayout.getMotorRearLeft(), Command.BACKWARD);
			issueCommand(pinLayout.getMotorRearRight(), Command.BACKWARD);
    	}
		submit();		
	}

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#turnLeft(double)
	 */
	@Override
	public void turnLeft(double speedFactor) {
		turnLeft();
		if (previous_speed_left!=speedFactor
				|| previous_speed_right!=speedFactor) {
			setSpeed(speedFactor);
		}
        leftFactor = -speedFactor;
        rightFactor = speedFactor;
	}
	
	private void turnLeft() {
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_FRONT.equals(pinLayout.getWheels())) {
    		issueCommand(pinLayout.getMotorFrontLeft(), Command.BACKWARD);
    		issueCommand(pinLayout.getMotorFrontRight(), Command.FORWARD);
    	}
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_REAR.equals(pinLayout.getWheels())) {
			issueCommand(pinLayout.getMotorRearLeft(), Command.BACKWARD);
			issueCommand(pinLayout.getMotorRearRight(), Command.FORWARD);
    	}
		submit();
	}

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#turnRight(double)
	 */
	@Override
	public void turnRight(double speedFactor) {
		turnRight();
		if (previous_speed_left!=speedFactor
				|| previous_speed_right!=speedFactor) {
			setSpeed(speedFactor);
		}
        leftFactor = speedFactor;
        rightFactor = -speedFactor;
	}
	
	private void turnRight() {
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_FRONT.equals(pinLayout.getWheels())) {
			issueCommand(pinLayout.getMotorFrontRight(), Command.BACKWARD);
			issueCommand(pinLayout.getMotorFrontLeft(), Command.FORWARD);
    	}
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_REAR.equals(pinLayout.getWheels())) {
			issueCommand(pinLayout.getMotorRearRight(), Command.BACKWARD);			
			issueCommand(pinLayout.getMotorRearLeft(), Command.FORWARD);
    	}
		submit();		
	}

	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#stop()
	 */
	@Override
	public void stop() {
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_FRONT.equals(pinLayout.getWheels())) {
			issueCommand(pinLayout.getMotorFrontLeft(), Command.RELEASE);
			issueCommand(pinLayout.getMotorFrontRight(), Command.RELEASE);
    	}
    	if (Wheels.FOUR.equals(pinLayout.getWheels())
    		|| Wheels.TWO_REAR.equals(pinLayout.getWheels())) {
			issueCommand(pinLayout.getMotorRearLeft(), Command.RELEASE);
			issueCommand(pinLayout.getMotorRearRight(), Command.RELEASE);
    	}
		submit();
		setSpeed(0.0);
        leftFactor = rightFactor = 0.0;
	}
    
	@Override
	public boolean isMoving() {
		return !latch_state.isEmpty();
	}
	
	/* (non-Javadoc)
	 * @see org.guga.robotoy.rasp.utils.Motor#setMovement(double,double)
	 */
	@Override
	public void setMovement(double leftFactor,double rightFactor) {
		if (this.leftFactor==leftFactor
			&& this.rightFactor==rightFactor)
			return;
		double leftSpeed = Math.abs(leftFactor);
		double rightSpeed = Math.abs(rightFactor);
		final boolean leftForward = leftFactor>=0;
		final boolean rightForward = rightFactor>=0;
		final boolean stopped = ((leftSpeed < EPSILON) && (rightSpeed < EPSILON));
		
		// If we have just one PWM for controlling all motors, we consider
		// the greatest of both provided speeds.
	   	switch (pinLayout.getPwmType())
    	{
    	case SINGLE_HARDWARE_PWM:
    	case SINGLE_SOFTWARE_PWM:
    	case SINGLE_STEADY:
    		leftSpeed = rightSpeed = Math.max(leftSpeed, rightSpeed);
    		break;
    	default:
    	}
		
		if (stopped) {
			stop();		
		}
		else {
			int factorLeft = toPWMLevel(leftSpeed);
			int factorRight = toPWMLevel(rightSpeed);
    		setSpeed(DEFAULT_FRONT_LEFT,factorLeft);
    		setSpeed(DEFAULT_FRONT_RIGHT,factorRight);
    		setSpeed(DEFAULT_REAR_LEFT,factorLeft);
    		setSpeed(DEFAULT_REAR_RIGHT,factorRight);

			if (leftForward && rightForward)
				moveForward();
			else if (!leftForward && !rightForward)
				moveBackward();
			else if (leftForward)
				turnRight();
			else
				turnLeft();
		}
		// remember factors and speed
		this.rightFactor = rightFactor;
		this.leftFactor = leftFactor;
		this.previous_speed_left = leftSpeed;
		this.previous_speed_right = rightSpeed;
	}
}

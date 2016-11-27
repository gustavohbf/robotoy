// This is a modification of the original
// Adafruit Motor shield library (copyright Adafruit Industries LLC, 2009)
// There are several modifications for use of serial port communication
// The original 'AFMotorController::latch_tx' was also modified in order
// to hold state up to the point all the motors states have already been
// assigned.
// Everything related to stepper motors have been taken away.

// This sketch will listen to the following commands:
// l -> make a left turn by spinning the motors accordingly
// r -> make a right turn by spinning the motors accordingly
// f -> move forward by spinning the motors accordingly
// b -> move backward by spinning the motors accordingly
// s -> stop motors
// L### -> change PWM level for left motors. Inform three digits
// for PWM level (from 0 up to 255).
// R### -> change PWM level for right motors. Inform three digits
// for PWM level (from 0 up to 255).
// P### -> change PWM level for all motors. Inform three digits
// for PWM level (from 0 up to 255).
// D###### -> change PWM level for left and right side motors idependently. Inform three digits
// for left PWM level (from 0 up to 255) and three digits for right PWM level (from 0 up to 255).
// x######## -> with each '#' either 0 or 1, this will send these 8 signals to the latch port
// in motor driver shield. This is only used for debugging purposes.


#include "AFMotorModified.h"

// Make it easy to identify the running program
#define PROGRAM_NAME "RoboToy.ino"

// Comment this line if you don't want to spend time with
// diagnostics information being sent to serial port all the
// time.
#define DEBUG_INFO

// Constants used for determining how many and where are the
// motors. You can choose one of these in 'WHEELS' definition later.
#define TWO_REAR  2   // Only two rear motors
#define TWO_FRONT 3   // Only two front motors
#define FOUR    4     // Four motors (two front and two rear)

#define WHEELS  TWO_REAR

// Constants used for determining which motor goes where
// Follow the numbers printed on the motor driver shield
#define REAR_LEFT   4
#define REAR_RIGHT  3
#define FRONT_LEFT  1
#define FRONT_RIGHT 2

// Global variables used for controlling each motor
#if WHEELS==FOUR || WHEELS==TWO_FRONT
AF_DCMotor motor_fl(FRONT_LEFT,DC_MOTOR_PWM_RATE,true); // front-left
AF_DCMotor motor_fr(FRONT_RIGHT,DC_MOTOR_PWM_RATE,true); // front-right
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
AF_DCMotor motor_rl(REAR_LEFT,DC_MOTOR_PWM_RATE,true);  // rear-left
AF_DCMotor motor_rr(REAR_RIGHT,DC_MOTOR_PWM_RATE,true); // rear-right
#endif

// Startup speed (as long as any motor is enabled)
#define STARTUP_SPEED 255

// Variable used with 'x' command
uint8_t input_latch_state = 0;

// Variable used with 'x' command
uint8_t input_latch_count = 0;

// Variables used for speed setup ('L', 'R', 'P' and 'D' commands)
int speed_setup_type = 0;   // First character in command
int speed_setup_count = 0;  // Number of characters in command
int speed_setup = 0;        // Speed in numeric form
int speed2_setup = 0;       // Second speed in numeric form (only for 'D' command)

// Setup function (runs only once at Arduino startup process)
void setup() {  
  Serial.begin(115200);
  printWelcomeMessage();

#if WHEELS==FOUR || WHEELS==TWO_FRONT
  motor_fl.setSpeed(STARTUP_SPEED);
  motor_fl.run(RELEASE);
  motor_fr.setSpeed(STARTUP_SPEED);
  motor_fr.run(RELEASE);
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
  motor_rl.setSpeed(STARTUP_SPEED);
  motor_rl.run(RELEASE);
  motor_rr.setSpeed(STARTUP_SPEED);
  motor_rr.run(RELEASE);
#endif
}


// Loop function (keeps being called all the time)
void loop() {

  // Process command received by serial port
  // As long as we receive something from serial port...
  if (Serial.available()>0) {
    processCommand();
  }  
}


// Function used for processing user command incoming from serial port
void processCommand() {
    int incomingByte = Serial.read();

    // If inside a x######## command, read the following # bits (0's and 1's)
    // This is only used for debugging purposes
    if (input_latch_count>0) {
      if (incomingByte!='0' && incomingByte!='1') {
        // If we got an unexpected character, bails out the 'x' command
        input_latch_count = 0;
      }
      else {
        input_latch_state = input_latch_state*2 + ((incomingByte=='1')?1:0);
        input_latch_count++;
        if (input_latch_count>8) {
          input_latch_count = 0;
#if defined(DEBUG_INFO)
        printLatchState();
#endif
          setlatchstate(input_latch_state);
        }
        return;
      }
    }

    // If inside a L### or R### or P### or D###### command, read the following # digits (0 - 9)
    if (speed_setup_count>0) {
      if (incomingByte<'0' || incomingByte>'9') {
        // If we got an unexpected character, bails out pending command
        speed_setup_count = 0;
      }
      else {
        if (speed_setup_type=='D' && speed_setup_count>3)
          speed2_setup = speed2_setup*10 + (incomingByte-'0');
        else
          speed_setup = speed_setup*10 + (incomingByte-'0');
        speed_setup_count++;
        if (speed_setup_type!='D' && speed_setup_count>3) {
          speed_setup_count = 0;
          if (speed_setup_type=='L')
            setLeftSpeed(speed_setup);
          else if (speed_setup_type=='R')
            setRightSpeed(speed_setup);
          else
            setSpeed(speed_setup);
        }
        else if (speed_setup_type=='D' && speed_setup_count>6) {
          speed_setup_count = 0;
          setLeftSpeed(speed_setup);
          setRightSpeed(speed2_setup);
        }
      }
    }
    
    switch (incomingByte) {      
      
      case 'x':
        // Initiates a x######## command (will read the following '#' bits
        // in next loop iterations)
        input_latch_count = 1;
        input_latch_state = 0;
        break;
        
      case 'l':
        left();
#if defined(DEBUG_INFO)
        printLatchState();
#endif
        break;
        
      case 'r':
        right();
#if defined(DEBUG_INFO)
        printLatchState();
#endif
        break;
        
      case 'f':
        forward();
#if defined(DEBUG_INFO)
        printLatchState();
#endif
        break;
        
      case 'b':
        backward();
#if defined(DEBUG_INFO)
        printLatchState();
#endif
        break;
        
      case 's':
        stopMotor();
#if defined(DEBUG_INFO)
        printLatchState();
#endif
        break;
        
      case 'P':
      case 'L':
      case 'R':
      case 'D':
        // Let's get PWM level in next loop iterations
        speed_setup_type = incomingByte;
        speed_setup_count = 1;
        speed_setup = 0;
        speed2_setup = 0;
        break;
    }
}

void setLeftSpeed(int speed) {
#if defined(DEBUG_INFO)
  Serial.print("PWM left : ");
  Serial.print(speed);
  Serial.println("");
#endif  
#if WHEELS==FOUR || WHEELS==TWO_FRONT
  motor_fl.setSpeed(speed);
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
  motor_rl.setSpeed(speed);
#endif
}

void setRightSpeed(int speed) {
#if defined(DEBUG_INFO)
  Serial.print("PWM right : ");
  Serial.print(speed);
  Serial.println("");
#endif  
#if WHEELS==FOUR || WHEELS==TWO_FRONT
  motor_fr.setSpeed(speed);
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
  motor_rr.setSpeed(speed);
#endif
}

void setSpeed(int speed) {
#if defined(DEBUG_INFO)
  Serial.print("PWM : ");
  Serial.print(speed);
  Serial.println("");
#endif  
#if WHEELS==FOUR || WHEELS==TWO_FRONT
  motor_fl.setSpeed(speed);
  motor_fr.setSpeed(speed);
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
  motor_rl.setSpeed(speed);
  motor_rr.setSpeed(speed);
#endif
}

void left() {
  hold_latch();
#if WHEELS==FOUR || WHEELS==TWO_FRONT
  motor_fl.run(BACKWARD);
  motor_fr.run(FORWARD);
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
  motor_rl.run(BACKWARD);
  motor_rr.run(FORWARD);    
#endif
  release_latch();
}

void right() {
  hold_latch();
#if WHEELS==FOUR || WHEELS==TWO_FRONT
  motor_fl.run(FORWARD);
  motor_fr.run(BACKWARD);
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
  motor_rl.run(FORWARD);
  motor_rr.run(BACKWARD);      
#endif
  release_latch();
}

void forward() {
  hold_latch();
#if WHEELS==FOUR || WHEELS==TWO_FRONT
  motor_fl.run(FORWARD);
  motor_fr.run(FORWARD);
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
  motor_rl.run(FORWARD);
  motor_rr.run(FORWARD);  
#endif
  release_latch();
}

void backward() {
  hold_latch();
#if WHEELS==FOUR || WHEELS==TWO_FRONT
  motor_fl.run(BACKWARD);
  motor_fr.run(BACKWARD);
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
  motor_rl.run(BACKWARD);
  motor_rr.run(BACKWARD);
#endif
  release_latch();
}

void stopMotor() {
  hold_latch();
#if WHEELS==FOUR || WHEELS==TWO_FRONT
  motor_fl.run(RELEASE);
  motor_fr.run(RELEASE);
#endif
#if WHEELS==FOUR || WHEELS==TWO_REAR
  motor_rl.run(RELEASE);
  motor_rr.run(RELEASE);
  release_latch();
#endif
}

void printWelcomeMessage() {
#if defined(DEBUG_INFO)
#if defined(__AVR_ATmega8__)
  Serial.println("__AVR_ATmega8__");
#elif defined(__AVR_ATmega48__)
  Serial.println("__AVR_ATmega48__");
#elif defined(__AVR_ATmega88__)
  Serial.println("__AVR_ATmega88__");
#elif defined(__AVR_ATmega168__)
  Serial.println("__AVR_ATmega168__");
#elif defined(__AVR_ATmega328P__)
  Serial.println("__AVR_ATmega328P__");
#elif defined(__AVR_ATmega1280__)
  Serial.println("__AVR_ATmega1280__");
#elif defined(__AVR_ATmega2560__)
  Serial.println("__AVR_ATmega2560__");
#elif defined(__PIC32MX__)
  Serial.println("__PIC32MX__");
#else
  Serial.println("Unknown");
#endif
#if (ARDUINO >= 100)
  Serial.println("ARDUINO");
#elif defined(__AVR__)
  Serial.println("AVR");
#endif
#if WHEELS==FOUR
  Serial.println("FOUR MOTORIZED WHEELS");
#elif WHEELS==TWO_REAR
  Serial.println("TWO MOTORIZED REAR WHEELS");
#else
  Serial.println("TWO MOTORIZED FRONT WHEELS");
#endif
#endif
  Serial.print("Program: ");
  Serial.print(PROGRAM_NAME);
  Serial.println("");
  Serial.println("Waiting motor shield commands...");
}

#if defined(DEBUG_INFO)
void printLatchState() {
      Serial.print("latch : ");
      Serial.print(getlatchstate());
      Serial.println("");
}
#endif

// Adafruit Motor shield library
// copyright Adafruit Industries LLC, 2009
// this code is public domain, enjoy!


#if (ARDUINO >= 100)
  #include "Arduino.h"
#else
  #if defined(__AVR__)
    #include <avr/io.h>
  #endif
  #include "WProgram.h"
#endif

#include "AFMotorModified.h"



static uint8_t latch_state;
static bool latch_onhold;  

#if (MICROSTEPS == 8)
uint8_t microstepcurve[] = {0, 50, 98, 142, 180, 212, 236, 250, 255};
#elif (MICROSTEPS == 16)
uint8_t microstepcurve[] = {0, 25, 50, 74, 98, 120, 141, 162, 180, 197, 212, 225, 236, 244, 250, 253, 255};
#endif

AFMotorController::AFMotorController(void) {
    TimerInitalized = false;
}

void AFMotorController::enable(void) {
  // setup the latch
  /*
  LATCH_DDR |= _BV(LATCH);
  ENABLE_DDR |= _BV(ENABLE);
  CLK_DDR |= _BV(CLK);
  SER_DDR |= _BV(SER);
  */
  pinMode(MOTORLATCH, OUTPUT);
  pinMode(MOTORENABLE, OUTPUT);
  pinMode(MOTORDATA, OUTPUT);
  pinMode(MOTORCLK, OUTPUT);

  latch_state = 0;

  latch_tx();  // "reset"

  //ENABLE_PORT &= ~_BV(ENABLE); // enable the chip outputs!
  digitalWrite(MOTORENABLE, LOW);
}


void AFMotorController::latch_tx(void) {
  uint8_t i;
  
  if (latch_onhold)
	  return;

  //LATCH_PORT &= ~_BV(LATCH);
  digitalWrite(MOTORLATCH, LOW);

  //SER_PORT &= ~_BV(SER);
  digitalWrite(MOTORDATA, LOW);

  for (i=0; i<8; i++) {
    //CLK_PORT &= ~_BV(CLK);
    digitalWrite(MOTORCLK, LOW);

    if (latch_state & _BV(7-i)) {
      //SER_PORT |= _BV(SER);
      digitalWrite(MOTORDATA, HIGH);
    } else {
      //SER_PORT &= ~_BV(SER);
      digitalWrite(MOTORDATA, LOW);
    }
    //CLK_PORT |= _BV(CLK);
    digitalWrite(MOTORCLK, HIGH);
  }
  //LATCH_PORT |= _BV(LATCH);
  digitalWrite(MOTORLATCH, HIGH);
}

static AFMotorController MC;

/******************************************
               MOTORS
******************************************/
inline void initPWM1(uint8_t freq,bool use_pwm = true) {
	if (!use_pwm) {
        pinMode(11, OUTPUT);
		digitalWrite(11, LOW);
		return;
	}
	
#if defined(__AVR_ATmega8__) || \
    defined(__AVR_ATmega48__) || \
    defined(__AVR_ATmega88__) || \
    defined(__AVR_ATmega168__) || \
    defined(__AVR_ATmega328P__)
    // use PWM from timer2A on PB3 (Arduino pin #11)
    TCCR2A |= _BV(COM2A1) | _BV(WGM20) | _BV(WGM21); // fast PWM, turn on oc2a
    TCCR2B = freq & 0x7;
    OCR2A = 0;
#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
    // on arduino mega, pin 11 is now PB5 (OC1A)
    TCCR1A |= _BV(COM1A1) | _BV(WGM10); // fast PWM, turn on oc1a
    TCCR1B = (freq & 0x7) | _BV(WGM12);
    OCR1A = 0;
#elif defined(__PIC32MX__)
    #if defined(PIC32_USE_PIN9_FOR_M1_PWM)
        // Make sure that pin 11 is an input, since we have tied together 9 and 11
        pinMode(9, OUTPUT);
        pinMode(11, INPUT);
        if (!MC.TimerInitalized)
        {   // Set up Timer2 for 80MHz counting fro 0 to 256
            T2CON = 0x8000 | ((freq & 0x07) << 4); // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=<freq>, T32=0, TCS=0; // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=0, T32=0, TCS=0
            TMR2 = 0x0000;
            PR2 = 0x0100;
            MC.TimerInitalized = true;
        }
         // Setup OC4 (pin 9) in PWM mode, with Timer2 as timebase
        OC4CON = 0x8006;    // OC32 = 0, OCTSEL=0, OCM=6
        OC4RS = 0x0000;
        OC4R = 0x0000;
    #elif defined(PIC32_USE_PIN10_FOR_M1_PWM)
        // Make sure that pin 11 is an input, since we have tied together 9 and 11
        pinMode(10, OUTPUT);
        pinMode(11, INPUT);
        if (!MC.TimerInitalized)
        {   // Set up Timer2 for 80MHz counting fro 0 to 256
            T2CON = 0x8000 | ((freq & 0x07) << 4); // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=<freq>, T32=0, TCS=0; // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=0, T32=0, TCS=0
            TMR2 = 0x0000;
            PR2 = 0x0100;
            MC.TimerInitalized = true;
        }
         // Setup OC5 (pin 10) in PWM mode, with Timer2 as timebase
        OC5CON = 0x8006;    // OC32 = 0, OCTSEL=0, OCM=6
        OC5RS = 0x0000;
        OC5R = 0x0000;
    #else
        // If we are not using PWM for pin 11, then just do digital
        digitalWrite(11, LOW);
    #endif
#else
   #error "This chip is not supported!"
#endif
    #if !defined(PIC32_USE_PIN9_FOR_M1_PWM) && !defined(PIC32_USE_PIN10_FOR_M1_PWM)
        pinMode(11, OUTPUT);
    #endif
}

inline void setPWM1(uint8_t s,bool use_pwm = true) {
	if (!use_pwm) {
        if (s > 127)
        {
            digitalWrite(11, HIGH);
        }
        else
        {
            digitalWrite(11, LOW);
        }
		return;		
	}
#if defined(__AVR_ATmega8__) || \
    defined(__AVR_ATmega48__) || \
    defined(__AVR_ATmega88__) || \
    defined(__AVR_ATmega168__) || \
    defined(__AVR_ATmega328P__)
    // use PWM from timer2A on PB3 (Arduino pin #11)
    OCR2A = s;
#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
    // on arduino mega, pin 11 is now PB5 (OC1A)
    OCR1A = s;
#elif defined(__PIC32MX__)
    #if defined(PIC32_USE_PIN9_FOR_M1_PWM)
        // Set the OC4 (pin 9) PMW duty cycle from 0 to 255
        OC4RS = s;
    #elif defined(PIC32_USE_PIN10_FOR_M1_PWM)
        // Set the OC5 (pin 10) PMW duty cycle from 0 to 255
        OC5RS = s;
    #else
        // If we are not doing PWM output for M1, then just use on/off
        if (s > 127)
        {
            digitalWrite(11, HIGH);
        }
        else
        {
            digitalWrite(11, LOW);
        }
    #endif
#else
   #error "This chip is not supported!"
#endif
}

inline void initPWM2(uint8_t freq,bool use_pwm = true) {
	if (!use_pwm) {
        pinMode(3, OUTPUT);
		digitalWrite(3, LOW);
		return;
	}
#if defined(__AVR_ATmega8__) || \
    defined(__AVR_ATmega48__) || \
    defined(__AVR_ATmega88__) || \
    defined(__AVR_ATmega168__) || \
    defined(__AVR_ATmega328P__)
    // use PWM from timer2B (pin 3)
    TCCR2A |= _BV(COM2B1) | _BV(WGM20) | _BV(WGM21); // fast PWM, turn on oc2b
    TCCR2B = freq & 0x7;
    OCR2B = 0;
#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
    // on arduino mega, pin 3 is now PE5 (OC3C)
    TCCR3A |= _BV(COM1C1) | _BV(WGM10); // fast PWM, turn on oc3c
    TCCR3B = (freq & 0x7) | _BV(WGM12);
    OCR3C = 0;
#elif defined(__PIC32MX__)
    if (!MC.TimerInitalized)
    {   // Set up Timer2 for 80MHz counting fro 0 to 256
        T2CON = 0x8000 | ((freq & 0x07) << 4); // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=<freq>, T32=0, TCS=0; // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=0, T32=0, TCS=0
        TMR2 = 0x0000;
        PR2 = 0x0100;
        MC.TimerInitalized = true;
    }
    // Setup OC1 (pin3) in PWM mode, with Timer2 as timebase
    OC1CON = 0x8006;    // OC32 = 0, OCTSEL=0, OCM=6
    OC1RS = 0x0000;
    OC1R = 0x0000;
#else
   #error "This chip is not supported!"
#endif

    pinMode(3, OUTPUT);
}

inline void setPWM2(uint8_t s,bool use_pwm = true) {
	if (!use_pwm) {
        if (s > 127)
        {
            digitalWrite(3, HIGH);
        }
        else
        {
            digitalWrite(3, LOW);
        }
		return;		
	}
#if defined(__AVR_ATmega8__) || \
    defined(__AVR_ATmega48__) || \
    defined(__AVR_ATmega88__) || \
    defined(__AVR_ATmega168__) || \
    defined(__AVR_ATmega328P__)
    // use PWM from timer2A on PB3 (Arduino pin #11)
    OCR2B = s;
#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
    // on arduino mega, pin 11 is now PB5 (OC1A)
    OCR3C = s;
#elif defined(__PIC32MX__)
    // Set the OC1 (pin3) PMW duty cycle from 0 to 255
    OC1RS = s;
#else
   #error "This chip is not supported!"
#endif
}

inline void initPWM3(uint8_t freq,bool use_pwm = true) {
	if (!use_pwm) {
        pinMode(6, OUTPUT);
		digitalWrite(6, LOW);
		return;
	}
#if defined(__AVR_ATmega8__) || \
    defined(__AVR_ATmega48__) || \
    defined(__AVR_ATmega88__) || \
    defined(__AVR_ATmega168__) || \
    defined(__AVR_ATmega328P__)
    // use PWM from timer0A / PD6 (pin 6)
    TCCR0A |= _BV(COM0A1) | _BV(WGM00) | _BV(WGM01); // fast PWM, turn on OC0A
    //TCCR0B = freq & 0x7;
    OCR0A = 0;
#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
    // on arduino mega, pin 6 is now PH3 (OC4A)
    TCCR4A |= _BV(COM1A1) | _BV(WGM10); // fast PWM, turn on oc4a
    TCCR4B = (freq & 0x7) | _BV(WGM12);
    //TCCR4B = 1 | _BV(WGM12);
    OCR4A = 0;
#elif defined(__PIC32MX__)
    if (!MC.TimerInitalized)
    {   // Set up Timer2 for 80MHz counting fro 0 to 256
        T2CON = 0x8000 | ((freq & 0x07) << 4); // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=<freq>, T32=0, TCS=0; // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=0, T32=0, TCS=0
        TMR2 = 0x0000;
        PR2 = 0x0100;
        MC.TimerInitalized = true;
    }
    // Setup OC3 (pin 6) in PWM mode, with Timer2 as timebase
    OC3CON = 0x8006;    // OC32 = 0, OCTSEL=0, OCM=6
    OC3RS = 0x0000;
    OC3R = 0x0000;
#else
   #error "This chip is not supported!"
#endif
    pinMode(6, OUTPUT);
}

inline void setPWM3(uint8_t s,bool use_pwm = true) {
	if (!use_pwm) {
        if (s > 127)
        {
            digitalWrite(6, HIGH);
        }
        else
        {
            digitalWrite(6, LOW);
        }
		return;		
	}
#if defined(__AVR_ATmega8__) || \
    defined(__AVR_ATmega48__) || \
    defined(__AVR_ATmega88__) || \
    defined(__AVR_ATmega168__) || \
    defined(__AVR_ATmega328P__)
    // use PWM from timer0A on PB3 (Arduino pin #6)
    OCR0A = s;
#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
    // on arduino mega, pin 6 is now PH3 (OC4A)
    OCR4A = s;
#elif defined(__PIC32MX__)
    // Set the OC3 (pin 6) PMW duty cycle from 0 to 255
    OC3RS = s;
#else
   #error "This chip is not supported!"
#endif
}



inline void initPWM4(uint8_t freq,bool use_pwm = true) {
	if (!use_pwm) {
        pinMode(5, OUTPUT);
		digitalWrite(5, LOW);
		return;
	}
#if defined(__AVR_ATmega8__) || \
    defined(__AVR_ATmega48__) || \
    defined(__AVR_ATmega88__) || \
    defined(__AVR_ATmega168__) || \
    defined(__AVR_ATmega328P__)
    // use PWM from timer0B / PD5 (pin 5)
    TCCR0A |= _BV(COM0B1) | _BV(WGM00) | _BV(WGM01); // fast PWM, turn on oc0a
    //TCCR0B = freq & 0x7;
    OCR0B = 0;
#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
    // on arduino mega, pin 5 is now PE3 (OC3A)
    TCCR3A |= _BV(COM1A1) | _BV(WGM10); // fast PWM, turn on oc3a
    TCCR3B = (freq & 0x7) | _BV(WGM12);
    //TCCR4B = 1 | _BV(WGM12);
    OCR3A = 0;
#elif defined(__PIC32MX__)
    if (!MC.TimerInitalized)
    {   // Set up Timer2 for 80MHz counting fro 0 to 256
        T2CON = 0x8000 | ((freq & 0x07) << 4); // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=<freq>, T32=0, TCS=0; // ON=1, FRZ=0, SIDL=0, TGATE=0, TCKPS=0, T32=0, TCS=0
        TMR2 = 0x0000;
        PR2 = 0x0100;
        MC.TimerInitalized = true;
    }
    // Setup OC2 (pin 5) in PWM mode, with Timer2 as timebase
    OC2CON = 0x8006;    // OC32 = 0, OCTSEL=0, OCM=6
    OC2RS = 0x0000;
    OC2R = 0x0000;
#else
   #error "This chip is not supported!"
#endif
    pinMode(5, OUTPUT);
}

inline void setPWM4(uint8_t s,bool use_pwm = true) {
	if (!use_pwm) {
        if (s > 127)
        {
            digitalWrite(5, HIGH);
        }
        else
        {
            digitalWrite(5, LOW);
        }
		return;		
	}	
#if defined(__AVR_ATmega8__) || \
    defined(__AVR_ATmega48__) || \
    defined(__AVR_ATmega88__) || \
    defined(__AVR_ATmega168__) || \
    defined(__AVR_ATmega328P__)
    // use PWM from timer0A on PB3 (Arduino pin #6)
    OCR0B = s;
#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
    // on arduino mega, pin 6 is now PH3 (OC4A)
    OCR3A = s;
#elif defined(__PIC32MX__)
    // Set the OC2 (pin 5) PMW duty cycle from 0 to 255
    OC2RS = s;
#else
   #error "This chip is not supported!"
#endif
}

AF_DCMotor::AF_DCMotor(uint8_t num, uint8_t freq, bool pwm) {
  motornum = num;
  pwmfreq = freq;
  use_pwm = pwm;

  MC.enable();

  switch (num) {
  case 1:
    latch_state &= ~_BV(MOTOR1_A) & ~_BV(MOTOR1_B); // set both motor pins to 0
    MC.latch_tx();
    initPWM1(freq,use_pwm);
    break;
  case 2:
    latch_state &= ~_BV(MOTOR2_A) & ~_BV(MOTOR2_B); // set both motor pins to 0
    MC.latch_tx();
    initPWM2(freq,use_pwm);
    break;
  case 3:
    latch_state &= ~_BV(MOTOR3_A) & ~_BV(MOTOR3_B); // set both motor pins to 0
    MC.latch_tx();
    initPWM3(freq,use_pwm);
    break;
  case 4:
    latch_state &= ~_BV(MOTOR4_A) & ~_BV(MOTOR4_B); // set both motor pins to 0
    MC.latch_tx();
    initPWM4(freq,use_pwm);
    break;
  }
}

void AF_DCMotor::run(uint8_t cmd) {
  uint8_t a, b;
  switch (motornum) {
  case 1:
    a = MOTOR1_A; b = MOTOR1_B; break;
  case 2:
    a = MOTOR2_A; b = MOTOR2_B; break;
  case 3:
    a = MOTOR3_A; b = MOTOR3_B; break;
  case 4:
    a = MOTOR4_A; b = MOTOR4_B; break;
  default:
    return;
  }
  
  switch (cmd) {
  case FORWARD:
    latch_state |= _BV(a);
    latch_state &= ~_BV(b); 
    MC.latch_tx();
    break;
  case BACKWARD:
    latch_state &= ~_BV(a);
    latch_state |= _BV(b); 
    MC.latch_tx();
    break;
  case RELEASE:
    latch_state &= ~_BV(a);     // A and B both low
    latch_state &= ~_BV(b); 
    MC.latch_tx();
    break;
  }
}

void AF_DCMotor::setSpeed(uint8_t speed) {
  switch (motornum) {
  case 1:
    setPWM1(speed,use_pwm); break;
  case 2:
    setPWM2(speed,use_pwm); break;
  case 3:
    setPWM3(speed,use_pwm); break;
  case 4:
    setPWM4(speed,use_pwm); break;
  }
}

uint8_t getlatchstate(void) {
	return latch_state;
}

void setlatchstate(uint8_t state) {
	latch_state = state;
	MC.latch_tx();
}

void hold_latch() { latch_onhold = true; }
void release_latch() { latch_onhold = false; MC.latch_tx(); }

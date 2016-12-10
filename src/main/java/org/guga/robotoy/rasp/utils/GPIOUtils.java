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
package org.guga.robotoy.rasp.utils;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

/**
 * Some utility methods used with GPIO.
 *  
 * @author Gustavo Figueiredo
 *
 */
public class GPIOUtils {

	/**
	 * Parse input value and translates it into GPIO Pin.
	 * @param input Text value of pin number (following wiringPi convention) or
	 * corresponding alphanumeric name.
	 */
	public static Pin parsePin(String input) {
		if (input==null || input.length()==0)
			return null;
		input = input.trim();
		if (input.matches("\\d+")) {
			int num = Integer.parseInt(input);
			return RaspiPin.getPinByAddress(num);
		}
		else {
			return RaspiPin.getPinByName(input);
		}
	}

}

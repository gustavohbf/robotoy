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
package org.guga.robotoy.rasp.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utilities for dealing with Wireless devices in Raspberry Pi (with Raspbian)
 * 
 * @author Gustavo Figueiredo
 *
 */
public class WiFiUtils {
	
	/**
	 * Class for presenting information about some wireless interface
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class WiFiInfo {
		/**
		 * Interface name
		 */
		private String name;
		/**
		 * Quality (0 - 100)
		 */
		private double quality;
		/**
		 * Signal Level (dB)
		 */
		private double level;
		/**
		 * Signal noise (dB)
		 */
		private double noise;
		
		/**
		 * Interface name
		 */
		public String getName() {
			return name;
		}
		/**
		 * Interface name
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * Quality (0 - 100)
		 */
		public double getQuality() {
			return quality;
		}
		/**
		 * Quality (0 - 100)
		 */
		public void setQuality(double quality) {
			this.quality = quality;
		}
		/**
		 * Signal Level (dB)
		 */
		public double getLevel() {
			return level;
		}
		/**
		 * Signal Level (dB)
		 */
		public void setLevel(double level) {
			this.level = level;
		}
		/**
		 * Signal noise (dB)
		 */
		public double getNoise() {
			return noise;
		}
		/**
		 * Signal noise (dB)
		 */
		public void setNoise(double noise) {
			this.noise = noise;
		}
		
	}

	/**
	 * Get Wireless information given its interface name.
	 * @param interfaceName Interface name (e.g.: 'wlan0')
	 */
	public static WiFiInfo getWirelessInfo(String interfaceName) throws Exception {
        String line;
        Process proc = null;
        final String TOKEN_QUALITY_LINK = "link";
        final String TOKEN_LEVEL = "level";
        final String TOKEN_NOISE = "noise";
        BufferedReader input = null;
        WiFiInfo info = null;
        try {
        	proc = Runtime.getRuntime().exec("cat /proc/net/wireless");
        	
/* SAMPLE OUTPUT:
Inter-| sta-|   Quality        |   Discarded packets               | Missed | WE
 face | tus | link level noise |  nwid  crypt   frag  retry   misc | beacon | 22
 wlan0: 0000   34.  -76.  -256        0      0      0      0      0        0
*/        	
            input = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));
            int index_of_quality_link = -1;
            int index_of_level = -1;
            int index_of_noise = -1;
            while ((line = input.readLine()) != null) {
            	if (line.contains(TOKEN_QUALITY_LINK) 
            			&& line.contains(TOKEN_LEVEL) 
            			&& line.contains(TOKEN_NOISE)) {
            		index_of_quality_link = line.indexOf(TOKEN_QUALITY_LINK);
            		index_of_level = line.indexOf(TOKEN_LEVEL);
            		index_of_noise = line.indexOf(TOKEN_NOISE);
            	}
            	else if (line.contains(interfaceName)) {
            		if (index_of_quality_link<0) 
            			throw new Exception("Unexpected response from '/proc/net/wireless'!");
            		info = new WiFiInfo();
            		info.setName(interfaceName);
            		info.setQuality(parseValueFromConsoleLine(line,index_of_quality_link));
            		info.setLevel(parseValueFromConsoleLine(line,index_of_level));
            		info.setNoise(parseValueFromConsoleLine(line,index_of_noise));
            	}
            }
            	
            proc.waitFor();
        } finally {
        	try {
        		if (input!=null)
        			input.close();
        	} catch (IOException e){ }
    		if (proc!=null)
    			proc.destroy();
        }
        return info;
	}
	
	/**
	 * Parse some text like '34.' into double value
	 */
	private static double parseValueFromConsoleLine(String line,int index) {
		String trimmed_line = line.substring(index).trim();
		int space = trimmed_line.indexOf(' ');
		if (space<0)
			return Double.parseDouble(trimmed_line);
		else
			return Double.parseDouble(trimmed_line.substring(0, space));
	}
}

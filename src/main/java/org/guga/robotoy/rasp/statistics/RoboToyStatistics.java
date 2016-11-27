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
package org.guga.robotoy.rasp.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Some statistics taken through game play for debugging purposes.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class RoboToyStatistics {
	
	/**
	 * Simplified version of all statistics (usefull for JSON representation)
	 * 
	 * @author Gustavo Figueiredo
	 */
	public static class Summary {
		
		public Map<String,SummaryStats> lag_by_ip;
		
		public IRStats ir;
	}

	/**
	 * Simplified version of one 'Statistics' (usefull for JSON representation)
	 * 
	 * @author Gustavo Figueiredo
	 */
	public static class SummaryStats {
		public int count;
		public double sum;
		public double max;
		public double min;
		public double mean;
		public double stddev;			
	}
	
	/**
	 * Statistics for IR device (both detector and emitter)
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class IRStats implements Cloneable {
		/**
		 * Acknowledged hits count from other robots
		 */
		public int ack_hits;
		/**
		 * Acknowledged hits count from ourselves
		 */
		public int ack_back_fire;
		/**
		 * Number of beams we fired
		 */
		public int count_fires;
		
		/**
		 * Number of signals we received that matches with respective checksum bits
		 */
		public int raw_signals_match;

		/**
		 * Number of signals we received that does not matches with respective checksum bits
		 */
		public int raw_signals_mismatch;

		/**
		 * Number of signals we received that matches with respective checksum bits but we
		 * treated as 'wrong' due to the presence of other signals
		 */
		public int raw_signals_wrong;

		public void clear() {
			ack_hits = 0;
			ack_back_fire = 0;
			count_fires = 0;
			raw_signals_match = 0;
			raw_signals_mismatch = 0;
			raw_signals_wrong = 0;
		}
		
		@Override
		public IRStats clone() {
			try {
				return (IRStats)super.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Take some statistics over PING time taked for some
	 * IP addresses
	 */
	private static final Map<String,Statistics> lagStatsByIPAddress = new HashMap<>();
	
	private static final IRStats irStats = new IRStats();
	
	public static void addLagStatsForIPAddress(String addr,long ping_ms) {
		if (addr==null || addr.length()==0)
			addr = "localhost";
		synchronized (lagStatsByIPAddress) {
			Statistics stats = lagStatsByIPAddress.get(addr);
			if (stats==null)
				lagStatsByIPAddress.put(addr, stats = new Statistics());
			stats.feed(ping_ms);
		}
	}
	
	public static void clearLagStatsByIPAddress() {
		synchronized (lagStatsByIPAddress) {
			lagStatsByIPAddress.clear();
		}		
	}
	
	public static Statistics getLagStatsForIPAddress(String addr) {
		if (addr==null || addr.length()==0)
			addr = "localhost";
		synchronized (lagStatsByIPAddress) {
			Statistics stats = lagStatsByIPAddress.get(addr);
			return stats;
		}		
	}
	
	public static List<String> getAddressesWithLagStats() {
		synchronized (lagStatsByIPAddress) {
			List<String> addresses = new ArrayList<>(lagStatsByIPAddress.keySet());
			return addresses;
		}		
	}
	
	public static void incIRStatAckHits() {
		synchronized (irStats) {
			irStats.ack_hits++;
		}		
	}

	public static void incIRStatAckBackFire() {
		synchronized (irStats) {
			irStats.ack_back_fire++;
		}				
	}
	
	public static void incIRStatCountFires() {
		synchronized (irStats) {
			irStats.count_fires++;
		}						
	}

	public static void incIRStatRawSignalsMatch() {
		synchronized (irStats) {
			irStats.raw_signals_match++;
		}						
	}

	public static void incIRStatRawSignalsMisMatch() {
		synchronized (irStats) {
			irStats.raw_signals_mismatch++;
		}						
	}

	public static void incIRStatRawSignalsWrong(int amount) {
		synchronized (irStats) {
			irStats.raw_signals_wrong+=amount;
		}						
	}

	public static void clearIRStats() {
		synchronized (irStats) {
			irStats.clear();
		}
	}
	
	public static IRStats getIRStats() {
		synchronized (irStats) {
			return irStats.clone();
		}
	}
	
	public static void clearAllStatistics() {
		clearLagStatsByIPAddress();
		clearIRStats();
	}
	
	public static Summary getSummary() {
		Summary s = new Summary();
		synchronized (lagStatsByIPAddress) {
			if (!lagStatsByIPAddress.isEmpty()) {
				s.lag_by_ip = new TreeMap<>();
				for (Map.Entry<String, Statistics> entry:lagStatsByIPAddress.entrySet()) {
					SummaryStats ss = new SummaryStats();
					ss.count = entry.getValue().getCount();
					ss.sum = entry.getValue().getSum();
					ss.max = entry.getValue().getMaximum();
					ss.min = entry.getValue().getMinimum();
					ss.mean = entry.getValue().getMean();
					ss.stddev = entry.getValue().getStandardDeviation();
					s.lag_by_ip.put(entry.getKey(), ss);
				}
			}
		}
		synchronized (irStats) {
			s.ir = irStats.clone();
		}
		return s;
	}
}

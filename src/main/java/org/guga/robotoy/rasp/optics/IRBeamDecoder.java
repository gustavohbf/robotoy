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

import java.util.Arrays;

/**
 * A simple protocol used for decoding IR signal.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class IRBeamDecoder {
	
	private static float DEFAULT_TOLERANCE = 0.6f;

	private int headerOnPulse;

	private int headerOffPulse;

	private int bit1OnPulse;
	
	private int bit1OffPulse;
	
	private int bit0OnPulse;
	
	private int bit0OffPulse;
	
	private boolean checksum;
	
	private int bitsPerByte = 8;

	private float tolerance = DEFAULT_TOLERANCE;
	
	private IRBeamDecoder() {
		
	}
	
	public static IRBeamDecoder forEncoder(IRBeamEncoder encoder) {
		IRBeamDecoder decoder = new IRBeamDecoder();
		decoder.setHeaderOnPulse(encoder.getHeaderOnPulse());
		decoder.setHeaderOffPulse(encoder.getHeaderOffPulse());
		decoder.setBit1OnPulse(encoder.getBit1OnPulse());
		decoder.setBit1OffPulse(encoder.getBit1OffPulse());
		decoder.setBit0OnPulse(encoder.getBit0OnPulse());
		decoder.setBit0OffPulse(encoder.getBit0OffPulse());
		decoder.setChecksum(encoder.hasChecksum());
		decoder.setBitsPerByte(encoder.getBitsPerByte());
		return decoder;
	}

	public boolean hasChecksum() {
		return checksum;
	}

	public void setChecksum(boolean checksum) {
		this.checksum = checksum;
	}

	public int getBitsPerByte() {
		return bitsPerByte;
	}

	public void setBitsPerByte(int bitsPerByte) {
		this.bitsPerByte = bitsPerByte;
	}

	public int getHeaderOnPulse() {
		return headerOnPulse;
	}

	public void setHeaderOnPulse(int headerOnPulse) {
		this.headerOnPulse = headerOnPulse;
	}

	public int getHeaderOffPulse() {
		return headerOffPulse;
	}

	public void setHeaderOffPulse(int headerOffPulse) {
		this.headerOffPulse = headerOffPulse;
	}

	public int getBit1OnPulse() {
		return bit1OnPulse;
	}

	public void setBit1OnPulse(int bit1OnPulse) {
		this.bit1OnPulse = bit1OnPulse;
	}

	public int getBit1OffPulse() {
		return bit1OffPulse;
	}

	public void setBit1OffPulse(int bit1OffPulse) {
		this.bit1OffPulse = bit1OffPulse;
	}

	public int getBit0OnPulse() {
		return bit0OnPulse;
	}

	public void setBit0OnPulse(int bit0OnPulse) {
		this.bit0OnPulse = bit0OnPulse;
	}

	public int getBit0OffPulse() {
		return bit0OffPulse;
	}

	public void setBit0OffPulse(int bit0OffPulse) {
		this.bit0OffPulse = bit0OffPulse;
	}

	public float getTolerance() {
		return tolerance;
	}

	public void setTolerance(float tolerance) {
		this.tolerance = tolerance;
	}
	
	/**
	 * Minimum signal data length in pulses we will admit
	 * in this implementation per each signal
	 */
	public int getMinSignalSize() {
		return 2 // ON/OFF pair for HEADER of signal
				+ bitsPerByte*2	// ON/OFF pairs for each bit for every byte in signal (minimum of one byte)
				+ (checksum?bitsPerByte*2:0)				// extra ON/OFF pairs for checksum byte
				+  1 // ON pulse for TAIL of signal
				;
	}

	private static enum PulseStage {
		NONE,
		HEADER,
		SIGNAL;
	};
	
	public byte[] getMessage(int[] pulses,int size) {
		if (pulses==null || size==0)
			return null;
		if (size<getMinSignalSize())
			return null;
		PulseStage stage = PulseStage.NONE;
		int estimated_size_bytes = (pulses.length-3)/(2*bitsPerByte) + 1;
		int actual_size_bytes = 0;
		int bit_order = 0;
		byte[] buffer = new byte[estimated_size_bytes];
		int bit_type = -1; // either 0 or 1 according to pulse levels, or -1 if can be any one
		for (int i=0;i<size;i++) {
			int pulse = pulses[i];
			int pulse_more = (int)(pulse + pulse * tolerance);
			int pulse_less = (int)(pulse - pulse * tolerance);
			boolean is_on = ((i%2)==0);
			switch (stage) {
			case NONE:
				if (is_on && headerOnPulse>=pulse_less && headerOnPulse<=pulse_more) {
					stage = PulseStage.HEADER; // go to next stage 
				}
				break;
			case HEADER:
				if (!is_on && headerOffPulse>=pulse_less && headerOffPulse<=pulse_more) {
					stage = PulseStage.SIGNAL; // go to next stage 
				}
				else {
					// go back to NONE stage
					stage = PulseStage.NONE;
				}
				break;
			case SIGNAL:
				if (is_on
					&& i+1<size) {
					boolean maybe_bit1 = (bit1OnPulse>=pulse_less && bit1OnPulse<=pulse_more);
					boolean maybe_bit0 = (bit0OnPulse>=pulse_less && bit0OnPulse<=pulse_more);
					if (maybe_bit1 || maybe_bit0) {
						if (!maybe_bit1)
							bit_type = 0;
						else if (!maybe_bit0)
							bit_type = 1;
						else
							bit_type = -1;
					}
					else {
						// go back to NONE stage
						stage = PulseStage.NONE;
						bit_order = 0;
						bit_type = -1;
						// will check again if it's another HEADER pulse event
						buffer[actual_size_bytes] = 0;
						i--;
					}
				}
				else if (!is_on) {
					if (bit_type!=0 && bit1OffPulse>=pulse_less && bit1OffPulse<=pulse_more) {
						// we got a bit 1
						byte b = (byte) (1 << bit_order);
						buffer[actual_size_bytes] |= b;
						bit_order++;
						if (bit_order==bitsPerByte) {
							bit_order = 0;
							actual_size_bytes++;
						}
						bit_type = -1;
					}
					else if (bit_type!=1 && bit0OffPulse>=pulse_less && bit0OffPulse<=pulse_more) {
						// we got a bit 0
						bit_order++;
						if (bit_order==bitsPerByte) {
							bit_order = 0;
							actual_size_bytes++;
						}
						bit_type = -1;
					}
					else {
						// go back to NONE stage
						stage = PulseStage.NONE;
						bit_order = 0;
						bit_type = -1;						
						// will check again if it's another HEADER pulse event
						buffer[actual_size_bytes] = 0;
						i--;
					}
				}
				else {
					// go back to NONE stage
					stage = PulseStage.NONE;
					bit_order = 0;
					bit_type = -1;
					// will check again if it's another HEADER pulse event
					buffer[actual_size_bytes] = 0;
					i--;
				}
				break;
			default:
			}
		}
		if (actual_size_bytes==estimated_size_bytes)
			return buffer;
		else if (actual_size_bytes==0)
			return new byte[0];
		else
			return Arrays.copyOf(buffer, actual_size_bytes);
	}
}

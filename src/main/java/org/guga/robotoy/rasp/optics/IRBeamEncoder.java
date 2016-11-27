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
 * A simple protocol used for encoding a message into an IR signal.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class IRBeamEncoder implements IRSend.Encoder {
	
	/**
	 * Maximum signal data length in bytes we will admit
	 * in this implementation
	 */
	public static final int MAX_SIGNAL_DATA_BYTES = 6;
	
	private static final int DEFAULT_HEADER_ON_DELAY	=	8000;
	private static final int DEFAULT_HEADER_OFF_DELAY	=	5000;

	private static final int DEFAULT_BIT1_ON_DELAY		=	420;
	private static final int DEFAULT_BIT1_OFF_DELAY	=	1420;

	private static final int DEFAULT_BIT0_ON_DELAY		=	420;
	private static final int DEFAULT_BIT0_OFF_DELAY	=	420;

	private static final int STOP_BIT_DELAY		=	420;
	
	private int headerOnPulse = DEFAULT_HEADER_ON_DELAY;

	private int headerOffPulse = DEFAULT_HEADER_OFF_DELAY;
	
	private int bit1OnPulse = DEFAULT_BIT1_ON_DELAY;
	
	private int bit1OffPulse = DEFAULT_BIT1_OFF_DELAY;
	
	private int bit0OnPulse = DEFAULT_BIT0_ON_DELAY;
	
	private int bit0OffPulse = DEFAULT_BIT0_OFF_DELAY;

	/**
	 * Flag indicating if we should transmit an extra byte for
	 * checksum of data bits.
	 */
	private boolean checksum;
	
	/**
	 * Number of bits to consider for each byte in message (little endian).<BR>
	 * Cannot exceed 8 bits per byte.
	 */
	private int bitsPerByte = 8;
	
	@Override
	public int getMaxSignalSize() {
		return 2 // ON/OFF pair for HEADER of signal
			+ MAX_SIGNAL_DATA_BYTES*bitsPerByte*2	// ON/OFF pairs for each bit for every byte in signal
			+ (checksum?bitsPerByte*2:0)				// extra ON/OFF pairs for checksum byte
			+  1 // ON pulse for TAIL of signal
			;
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

	@Override
	public int getHeader(int[] buffer, int offset) {
		buffer[offset] = headerOnPulse; 	// starting on
		buffer[offset+1] = headerOffPulse;	// and off
		return 2;
	}

	@Override
	public int getTail(int[] buffer, int offset) {
		buffer[offset] = STOP_BIT_DELAY;		// stop bit
		return 1;
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

	/**
	 * Flag indicating if we should transmit an extra byte for
	 * checksum of data bits.
	 */
	public boolean hasChecksum() {
		return checksum;
	}

	/**
	 * Flag indicating if we should transmit an extra byte for
	 * checksum of data bits.
	 */
	public void setChecksum(boolean checksum) {
		this.checksum = checksum;
	}

	/**
	 * Number of bits to consider for each byte in message (little endian).<BR>
	 * Cannot exceed 8 bits per byte.
	 */
	public int getBitsPerByte() {
		return bitsPerByte;
	}

	/**
	 * Number of bits to consider for each byte in message (little endian).<BR>
	 * Cannot exceed 8 bits per byte.
	 */
	public void setBitsPerByte(int bitsPerByte) {
		this.bitsPerByte = bitsPerByte;
	}

	@Override
	public int getPulses(byte b, int[] buffer, int offset) {
		
		int count = 0;
		for (int i=0;i<bitsPerByte;i++) {
			boolean bit = (b&1)!=0;
			b = (byte)(b>>1);
			if (bit) {
				buffer[offset+(count++)] = bit1OnPulse;
				buffer[offset+(count++)] = bit1OffPulse;
			}
			else {
				buffer[offset+(count++)] = bit0OnPulse;
				buffer[offset+(count++)] = bit0OffPulse;				
			}
		}
		
		return count;
	}
	
	public int[] getEncodedSignal(String msg) {
		return getEncodedSignal(msg.getBytes());
	}

	/**
	 * Get encoded signal in the form of mark/space pulses (starting with 'mark').
	 */
	@Override
	public int[] getEncodedSignal(byte[] msg_bytes) {
		if (msg_bytes.length>MAX_SIGNAL_DATA_BYTES) {
			throw new UnsupportedOperationException("Data must not exceed "+MAX_SIGNAL_DATA_BYTES+" bytes!");
		}
    	int signal[] = new int[getMaxSignalSize()];
    	int offset = 0;
    	offset += getHeader(signal, offset);
		byte checksum_bits = (byte)0b10101010;
    	for (int i=0;i<msg_bytes.length;i++) {
    		byte b = msg_bytes[i];
    		offset += getPulses(b, signal, offset);
    		if (checksum)
    			checksum_bits ^= b;
    	}
    	if (checksum) {
    		offset += getPulses(checksum_bits, signal, offset);    		
    	}
    	offset += getTail(signal, offset);
    	if (offset==signal.length)
    		return signal;
    	else
    		return Arrays.copyOf(signal, offset);
	}
	
}

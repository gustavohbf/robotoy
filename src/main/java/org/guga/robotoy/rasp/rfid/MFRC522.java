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
package org.guga.robotoy.rasp.rfid;

import java.io.IOException;
import java.io.PrintStream;
import java.util.BitSet;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;

/**
 * Implementation of MFRC522 module on Raspberry Pi.<BR>
 * Use SPI interface.<BR>
 * Based on the following Python code: https://github.com/mxgxw/MFRC522-python<BR>
 * <BR>
 * @author Gustavo Figueiredo
 */
public class MFRC522 {

	/**
	 * NRSTPD = Not Reset and Power-down<BR>
	 * Default value: GPIO 06 (wPi) = BCM25 = Physical 22
	 */
	private final Pin pinNRSTPD;
	
	/**
	 * Provisioned NRSTPD pin
	 */
	private GpioPinDigitalOutput rstOut;
	
	/**
	 * SPI channel.<BR>
	 * Default value: CS0
	 */
	private final SpiChannel spiChannel;

	/**
	 * SPI device
	 */
	private SpiDevice spi;

	private final int MAX_LEN = 16;

	public static final int PCD_IDLE       = 0x00;
	public static final int PCD_AUTHENT    = 0x0E;
	public static final int PCD_RECEIVE    = 0x08;
	public static final int PCD_TRANSMIT   = 0x04;
	public static final int PCD_TRANSCEIVE = 0x0C;
	public static final int PCD_RESETPHASE = 0x0F;
	public static final int PCD_CALCCRC    = 0x03;

	public static final int PICC_REQIDL    = 0x26;
	public static final int PICC_REQALL    = 0x52;
	public static final int PICC_ANTICOLL  = 0x93;
	public static final int PICC_SELECTTAG = 0x93;
	public static final int PICC_AUTHENT1A = 0x60;
	public static final int PICC_AUTHENT1B = 0x61;
	public static final int PICC_READ      = 0x30;
	public static final int PICC_WRITE     = 0xA0;
	public static final int PICC_DECREMENT = 0xC0;
	public static final int PICC_INCREMENT = 0xC1;
	public static final int PICC_RESTORE   = 0xC2;
	public static final int PICC_TRANSFER  = 0xB0;
	public static final int PICC_HALT      = 0x50;

	public static final int MI_OK       = 0;
	public static final int MI_NOTAGERR = 1;
	public static final int MI_ERR      = 2;

	public static final int Reserved00     = 0x00;
	public static final int CommandReg     = 0x01;
	public static final int CommIEnReg     = 0x02;
	public static final int DivlEnReg      = 0x03;
	public static final int CommIrqReg     = 0x04;
	public static final int DivIrqReg      = 0x05;
	public static final int ErrorReg       = 0x06;
	public static final int Status1Reg     = 0x07;
	public static final int Status2Reg     = 0x08;
	public static final int FIFODataReg    = 0x09;
	public static final int FIFOLevelReg   = 0x0A;
	public static final int WaterLevelReg  = 0x0B;
	public static final int ControlReg     = 0x0C;
	public static final int BitFramingReg  = 0x0D;
	public static final int CollReg        = 0x0E;
	public static final int Reserved01     = 0x0F;

	public static final int Reserved10     = 0x10;
	public static final int ModeReg        = 0x11;
	public static final int TxModeReg      = 0x12;
	public static final int RxModeReg      = 0x13;
	public static final int TxControlReg   = 0x14;
	public static final int TxAutoReg      = 0x15;
	public static final int TxSelReg       = 0x16;
	public static final int RxSelReg       = 0x17;
	public static final int RxThresholdReg = 0x18;
	public static final int DemodReg       = 0x19;
	public static final int Reserved11     = 0x1A;
	public static final int Reserved12     = 0x1B;
	public static final int MifareReg      = 0x1C;
	public static final int Reserved13     = 0x1D;
	public static final int Reserved14     = 0x1E;
	public static final int SerialSpeedReg = 0x1F;

	public static final int Reserved20        = 0x20;  
	public static final int CRCResultRegM     = 0x21;
	public static final int CRCResultRegL     = 0x22;
	public static final int Reserved21        = 0x23;
	public static final int ModWidthReg       = 0x24;
	public static final int Reserved22        = 0x25;
	public static final int RFCfgReg          = 0x26;
	public static final int GsNReg            = 0x27;
	public static final int CWGsPReg          = 0x28;
	public static final int ModGsPReg         = 0x29;
	public static final int TModeReg          = 0x2A;
	public static final int TPrescalerReg     = 0x2B;
	public static final int TReloadRegH       = 0x2C;
	public static final int TReloadRegL       = 0x2D;
	public static final int TCounterValueRegH = 0x2E;
	public static final int TCounterValueRegL = 0x2F;

	public static final int Reserved30      = 0x30;
	public static final int TestSel1Reg     = 0x31;
	public static final int TestSel2Reg     = 0x32;
	public static final int TestPinEnReg    = 0x33;
	public static final int TestPinValueReg = 0x34;
	public static final int TestBusReg      = 0x35;
	public static final int AutoTestReg     = 0x36;
	public static final int VersionReg      = 0x37;
	public static final int AnalogTestReg   = 0x38;
	public static final int TestDAC1Reg     = 0x39;
	public static final int TestDAC2Reg     = 0x3A;
	public static final int TestADCReg      = 0x3B;
	public static final int Reserved31      = 0x3C;
	public static final int Reserved32      = 0x3D;
	public static final int Reserved33      = 0x3E;
	public static final int Reserved34      = 0x3F;
	
	
	public static class CardInfo {
		private int status;
		private final BitSet backBits;
		private int numBits;
		public CardInfo() {
			backBits = new BitSet();
		}
		public int getStatus() {
			return status;
		}
		public void setStatus(int status) {
			this.status = status;
		}
		public BitSet getBackBits() {
			return backBits;
		}
		public byte[] getBackBytes() {
			return backBits.toByteArray();
		}
		public String getBackBytesString() {
			return MFRC522.toString(getBackBytes());
		}
		public int getNumBits() {
			return numBits;
		}
		public void setNumBits(int numBits) {
			this.numBits = numBits;
		}
		public int getNumBytes() {
			return (numBits+7)/8;
		}
		public void append(byte b,int byte_offset) {
			final int bit_offset = byte_offset*8;
			for (int i=0;i<8;i++) {
				boolean on_bit = ((b>>i)&0x1)!=0;
				if (!on_bit)
					continue;
				backBits.set(bit_offset+i, on_bit);
			}
		}
		public boolean isOk() {
			return status==MI_OK;
		}
		public void clear() {
			status = MI_ERR;
			backBits.clear();
			numBits = 0;
		}
	}
	
	
	public MFRC522(GpioController gpio) throws IOException {	
		this(gpio, RaspiPin.GPIO_06, SpiChannel.CS0);
	}

	public MFRC522(GpioController gpio,Pin pinNRSTPD,SpiChannel channel) throws IOException {	
		this.pinNRSTPD = pinNRSTPD;
		this.spiChannel = channel;
        spi = SpiFactory.getInstance(spiChannel,
                SpiDevice.DEFAULT_SPI_SPEED, // default spi speed 1 MHz
                SpiDevice.DEFAULT_SPI_MODE); // default spi mode 0

        rstOut =  gpio.provisionDigitalOutputPin(pinNRSTPD, "RC522RST");
        rstOut.setShutdownOptions(true, PinState.HIGH, PinPullResistance.OFF);
        rstOut.high();
		init();
	}
	
	public void init() throws IOException {
        rstOut.high();
        reset();
        write(TModeReg, 0x8D);
        write(TPrescalerReg, 0x3E);
        write(TReloadRegL, 30);
        write(TReloadRegH, 0);
        
        write(TxAutoReg, 0x40);
        write(ModeReg, 0x3D);
        setAntenna(true);
	}
	
	public void reset() throws IOException {
		write(CommandReg, PCD_RESETPHASE);
	}
	
	public void write(int address,int value) throws IOException {
		byte tmp[] = new byte[2];
		tmp[0] = (byte) ((address << 1) & 0x7E);
		tmp[1] = (byte) value;
		spi.write(tmp);
	}
	
	public byte read(int address) throws IOException {
		byte tmp[]=new byte[2];
		tmp[0] = (byte) (((address << 1) & 0x7E) | 0x80);
		tmp[1] = 0;
		byte[] result = spi.write(tmp);
		return result[1];
	}
	
	public void setBitMask(int address,int mask) throws IOException {
		byte tmp = read(address);
		write(address, tmp | mask);
	}
	
	public void clearBitMask(int address,int mask) throws IOException {
		byte tmp = read(address);
		write(address, tmp & (~mask));
	}

	public void setAntenna(boolean on) throws IOException {
		if (on) {
			setBitMask(TxControlReg, 0x03);
		}
		else {
			clearBitMask(TxControlReg, 0x03);
		}
	}

	public CardInfo request(int reqMode) throws IOException {
		CardInfo cardInfo = new CardInfo();
		request(reqMode, cardInfo);
        return cardInfo;
	}
	
	public void request(int reqMode, CardInfo out_cardInfo) throws IOException {
		write(BitFramingReg, 0x07);
		
		byte[] tagType = new byte[1];
		tagType[0]=(byte)reqMode;
		writeToCard(PCD_TRANSCEIVE, tagType, 1, out_cardInfo);
		
		if (!(out_cardInfo.isOk()) 
				|| out_cardInfo.getNumBits()!=0x10 /* 16 bits*/)
        {
			out_cardInfo.setStatus(MI_ERR);
        }
	}
	
	public void writeToCard(int command, byte[] sendData, int dataLen, CardInfo cardInfo) throws IOException
    {
	    final byte irqEn;
	    final byte waitIRq;
	    int n = 0;
	    int i;

	    if (command==PCD_AUTHENT) {
	    	irqEn = 0x12;
	    	waitIRq = 0x10;
	    }
	    else if (command==PCD_TRANSCEIVE) {
	    	irqEn = 0x77;
	    	waitIRq = 0x30;
	    }
	    else {
	    	irqEn = 0x00;
	    	waitIRq = 0x00;
	    }
	    
	    write(CommIEnReg, irqEn|0x80);
	    clearBitMask(CommIrqReg, 0x80);
	    setBitMask(FIFOLevelReg, 0x80);

	    write(CommandReg, PCD_IDLE); 
	    
	    for (i=0;i<dataLen;i++) {
	    	write(FIFODataReg, sendData[i]);
	    }
	    
	    write(CommandReg, command);
	    
	    if (command == PCD_TRANSCEIVE)
	        setBitMask(BitFramingReg, 0x80);
	    
	    for (i=2000; i!=0; i--) {
	    	n = read(CommIrqReg);
	    	if ((n&0x01)!=0 || (n&waitIRq)!=0)
	    		break;
	    }
	    
	    clearBitMask(BitFramingReg, 0x80);

	    final int status;
	    int backBits = 0;
	    byte lastBits = 0;
	    if (i!=0) {
	    	if ((read(ErrorReg) & 0x1B)==0x00) {

	    		if ((n & irqEn & 0x01)!=0)
	    			status = MI_NOTAGERR;
	    		else
	    			status = MI_OK;

	    		if (command == PCD_TRANSCEIVE) {
	    			n = read(FIFOLevelReg);
	    			lastBits = (byte)(read(ControlReg) & 0x07);
	    			if (lastBits != 0) 
	    				backBits = (n-1)*8 + lastBits;
	    			else
	    				backBits = n*8;

	    			if (n == 0)
	    				n = 1;
	    			if (n > MAX_LEN)
	    				n = MAX_LEN;

	    			for (i=0; i<n; i++) {
	    				byte b = read(FIFODataReg);
	    				cardInfo.append(b, i);
	    			}
	    		}
	    	}
	    	else {
	    		status = MI_ERR;
	    	}
	    }
	    else {
	    	status = MI_ERR;
	    }

	    cardInfo.setNumBits(backBits);
	    cardInfo.setStatus(status);
    }
	
	public void getAntiCollision(CardInfo out_cardInfo) throws IOException {
		write(BitFramingReg, 0x00);
		
		byte[] serNum = new byte[2];
		serNum[0]=(byte)PICC_ANTICOLL;
		serNum[1]=0x20;
		writeToCard(PCD_TRANSCEIVE, serNum, 2, out_cardInfo);

		if (out_cardInfo.isOk()) {
			if (out_cardInfo.getNumBytes()==5) {
				byte[] bytes = out_cardInfo.getBackBytes();
				final byte verify = bytes[4];
				byte check = 0;
				for (int i=0;i<4;i++)
					check ^= bytes[i];
				if (check!=verify) {
					out_cardInfo.setStatus(MI_ERR);
				}
			}
			else {
				out_cardInfo.setStatus(MI_ERR);
			}
		}
	}
	
	public void calculateCRC(byte[] pIndata, int pIndataLen, byte[] out_CRC, int offset_CRC) throws IOException {
		clearBitMask(DivIrqReg, 0x04);
		setBitMask(FIFOLevelReg, 0x80);
		for (int i=0;i<pIndataLen;i++) {
			write(FIFODataReg, pIndata[i]);
		}
		write(CommandReg,PCD_CALCCRC);
		for (int i=0xFF;i!=0;i--) {
			int n = read(DivIrqReg);
			if ((n&0x04)!=0)
				break;
		}
		out_CRC[offset_CRC] = read(CRCResultRegL);
		out_CRC[offset_CRC+1] = read(CRCResultRegM);
	}
	
	public byte selectTag(byte[] serNum) throws IOException {
		byte[] buf = new byte[9];
		buf[0] = (byte)PICC_SELECTTAG;
		buf[1] = 0x70;
		for (int i=0;i<5 && i<serNum.length;i++)
			buf[i+2] = serNum[i];
		calculateCRC(buf, 7, buf, 7);
		CardInfo ci = new CardInfo();
		writeToCard(PCD_TRANSCEIVE, buf, 9, ci);
		if (ci.isOk() && ci.getNumBits()==0x18) {
			return ci.getBackBytes()[0];
		}
		else {
			return 0;
		}
	}
	
	public int auth(int authMode, int blockAddr, byte[] sectorKey, byte[] serNum) throws IOException {
		byte buff[] = new byte[2+sectorKey.length+4];
		// First byte should be the authMode (A or B)
		buff[0] = (byte)authMode;
		// Second byte is the trailerBlock (usually 7)
		buff[1] = (byte)blockAddr;
		// Now we need to append the authKey which usually is 6 bytes of 0xFF
		for (int i=0;i<sectorKey.length;i++)
			buff[2 + i] = sectorKey[i];

	    // Next we append the first 4 bytes of the UID
		for (int i=0;i<4 && i<serNum.length;i++)
			buff[2 + sectorKey.length + i] = serNum[i];

		// Now we start the authentication itself
		CardInfo ci = new CardInfo();
		writeToCard(PCD_AUTHENT, buff, buff.length, ci);

   	    return ci.getStatus();
	}
	
	public void stopCrypto1() throws IOException {
		clearBitMask(Status2Reg, 0x08);
	}
	
	public byte[] getSector(int blockAddr) throws IOException {
		byte[] recvData = new byte[4];
		recvData[0] = PICC_READ;
		recvData[1] = (byte)blockAddr;
		calculateCRC(recvData, 2, recvData, 2);
		CardInfo ci = new CardInfo();
		writeToCard(PCD_TRANSCEIVE, recvData, 4, ci);
		if (!ci.isOk())
			return null;
		else
			return ci.getBackBytes();
	}
	
	public void read(int blockAddr, PrintStream output) throws IOException {
		byte[] sector = getSector(blockAddr);
		if (sector==null)
			output.println("Error while reading!");
		else if (sector.length==16)
			output.println("Sector "+blockAddr+" "+MFRC522.toString(sector));
	}
	
	public void write(int blockAddr, byte[] writeData, PrintStream output) throws IOException {
		byte[] buff = new byte[4];
		buff[0] = (byte)PICC_WRITE;
		buff[1] = (byte)blockAddr;
		calculateCRC(buff, 2, buff, 2);
		CardInfo ci = new CardInfo();
		writeToCard(PCD_TRANSCEIVE, buff, 4, ci);
		if (!ci.isOk() || ci.getNumBits()!=4 || (ci.getBackBytes()[0]&0x0F)!=0x0A)
			ci.setStatus(MI_ERR);
		output.println(ci.getNumBits()+" backdata &0x0F == 0x0A "+(ci.getBackBytes()[0]&0x0F));
		if (ci.isOk()) {
			byte[] buf = new byte[18];
			for (int i=0;i<16 && i<writeData.length;i++) {
				buf[i] = writeData[i];
			}
			calculateCRC(buf, 16, buf, 16);
			ci = new CardInfo();
			writeToCard(PCD_TRANSCEIVE, buf, 18, ci);
			if (!ci.isOk() || ci.getNumBits()!=4 || (ci.getBackBytes()[0]&0x0F)!=0x0A)
				output.println("Error while writing");
			if (ci.isOk())
				output.println("Data written");
		}
	}
	
	public void dumpClassic1K(byte[] key, byte[] uid, PrintStream output) throws IOException {
		for (int i=0; i<64; i++) {
			int status = auth(PICC_AUTHENT1A, i, key, uid);
			// Check if authenticated
			if (status==MI_OK)
				read(i);
			else
				output.println("Authentication error");
		}
	}

	/**
	 * NRSTPD = Not Reset and Power-down<BR>
	 * Default value: GPIO 06 (wPi) = BCM25 = Physical 22
	 */
	public Pin getPinNRSTPD() {
		return pinNRSTPD;
	}

	/**
	 * SPI channel.<BR>
	 * Default value: CS0
	 */
	public SpiChannel getSpiChannel() {
		return spiChannel;
	}

	public static String toString(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b:data) {
			if (sb.length()>0)
				sb.append(" ");
			int i = (int)b;
			if (i<0)
				i += 256;
			sb.append(String.valueOf(i));
		}
		return sb.toString();
	}
	
}

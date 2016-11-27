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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.utils.InetUtils;
import org.guga.robotoy.rasp.utils.InetUtils.NetAdapter;

/**
 * Some methods used for making this RoboToy work as an WiFi Access Point
 * 
 * @author Gustavo Figueiredo
 *
 */
public class RoboToyAccessPoint {
	
	private static final Logger log = Logger.getLogger(RoboToyAccessPoint.class.getName());
	
	/**
	 * Default password that should be set for the case it becomes an Access Point
	 */
	private static String defaultAPPassword = "robotoy1sFun";
	
	/**
	 * Default SSID to be used if this robot becomes an Access Point. The character '#', if present,
	 * will be replaced by the corresponding hexadecimal digit in MAC Address.<BR>
	 * For example, the SSID 'RoboToy##' will be replaced by 'RoboToy' plus the last two hexa-decimal
	 * digits of its MAC Address (see {@link InetUtils#getMACAddress() getMACAddress}).
	 */
	private static String defaultAPName = "RoboToy####";
	
	private static final int DEFAULT_AP_2_5GHz_CHANNEL = 6;

	private static final int DEFAULT_AP_5GHz_CHANNEL = 36;
	
	/**
	 * Changes the default password that should be set for the case it becomes an Access Point
	 */
	public static void setDefaultAPPassword(String newPassword) {
		RoboToyAccessPoint.defaultAPPassword = newPassword;
	}
	
	/**
	 * Changes the default SSID to be used if this robot becomes an Access Point. If it's NULL, it will
	 * choose a name with some pseudo-random parts based on its MAC Address (the actual 
	 * formula is programmed in 'getSomeSSID').
	 */
	public static void setDefaultAPName(String defaultAPName) {
		if (defaultAPName!=null && defaultAPName.trim().length()==0)
			defaultAPName = "RoboToy##";
		RoboToyAccessPoint.defaultAPName = defaultAPName;
	}

	/**
	 * Check if there is connectivity to any wireless network. If there is no connection, will
	 * turn it into an 'Access Point'.
	 */
	public static void checkAndBecomeAccessPoint(RoboToyServerController controller) {
		log.log(Level.FINE, "Checking current WiFi network status in order to decide if turn into Access Point...");
		
		// Check if already running as Access Point
		int hostapd_pid;
		try {
			hostapd_pid = InetUtils.getHostAPD_PID();
		} catch (Exception e1) {
			hostapd_pid = -1;
		}
		if (hostapd_pid>0) {
			log.log(Level.FINE, "HOSTAPD is already running at PID "+hostapd_pid+"!");
			return;
		}
		
    	// Check if already connected to any Wireless Network
		// Do a maximum of 3 attempts
		for (int t=0;t<3;t++) {
			String ssid;
			try {
				ssid = InetUtils.getConnectedWirelessNetwork();
			} catch (Exception e1) {
				ssid = null;
			}
			if (ssid!=null && ssid.length()>0) {
				log.log(Level.FINE, "Already connected to SSID "+ssid+"!");
				return;				
			}
			// will wait before try again
			try { Thread.sleep(100);
			} catch (InterruptedException e) {	}
		}
		
		// If we did not connect to any Wireless Network and we are not running as Access Point, let's
		// try to make it an Access Point.
			
		NetAdapter[] wifi_adapters = InetUtils.getWirelessAdapters();
		if (wifi_adapters==null || wifi_adapters.length==0) {
			log.log(Level.WARNING, "There are no wireless network adapters!");
			return;
		}

		log.log(Level.FINE, "Could not get wireless status!");
		log.log(Level.WARNING, "Starting Access Point at "+InetUtils.WiFiModeEnum.VIRTUAL_AP.getINetName()+"...");
		try {
			RoboToyAccessPoint.becomeAccessPoint(controller,InetUtils.WiFiModeEnum.VIRTUAL_AP);
			log.log(Level.WARNING, "Started Access Point mode!");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while starting access point mode!", e);
		}
	}

	/**
	 * Initializes Access Point. Create virtual interface if it does not exist.
	 */
	public static void becomeAccessPoint(RoboToyServerController controller,InetUtils.WiFiModeEnum mode) throws Exception {
		if (null!=controller.getAutoDiscoverOtherRobots()) {
			controller.getAutoDiscoverOtherRobots().stopService();
		}
		
		// If it's already running as an Access Point, it should stop it
		int hostapd_pid;
		try {
			hostapd_pid = InetUtils.getHostAPD_PID();
		} catch (Exception e1) {
			hostapd_pid = -1;
		}
		if (hostapd_pid>0) {
			InetUtils.stopHostAPD();
		}
		
		// Create host APD configuration file if it does not exists or if does not
		// hold the same expected properties
		String ssid = getSomeSSID();
		if (!InetUtils.isHostAPDFileConfigured(ssid,mode.getAPName(),mode.getBand())) {
			String pwd = defaultAPPassword;
			int channel = (InetUtils.WiFiBand.BAND_2_5_GHz.equals(mode.getBand())) ? DEFAULT_AP_2_5GHz_CHANNEL : DEFAULT_AP_5GHz_CHANNEL;
			//String driver = (mode.isExternal()) ? InetUtils.getDriverName(mode.getINetName()) : null;
			String driver = (mode.isExternal()) ? InetUtils.DEFAULT_EXTERNAL_HOSTAPD_DRIVER : null;
			String hostapd_conf = InetUtils.makeHostAPDConfiguration(ssid, /*hidden_ssid*/false, pwd, channel, mode.getAPName(),
					driver, mode.getBand());
			try (OutputStream output = new BufferedOutputStream(new FileOutputStream(new File(InetUtils.DEFAULT_HOSTAPD_CONFIG_FILE)));) {
				output.write(hostapd_conf.getBytes("UTF-8"));
			}
		}
		// Create virtual network device for Access Point if it does not exists
		if (mode.getVirtualName()!=null && !hasNetInterface(mode.getVirtualName())) {
			InetUtils.createAPInterface(mode.getINetName(),mode.getAPName());
		}
		if (!InetUtils.isDNSMasqConfigured(mode.getAPName())) {
			String dnsmasq_config = InetUtils.makeDNSMasqConfiguration(mode.getAPName());
			try (OutputStream output = new BufferedOutputStream(new FileOutputStream(new File(InetUtils.DEFAULT_DNSMASQ_CONFIG_FILE)));) {
				output.write(dnsmasq_config.getBytes("UTF-8"));
			}			
		}
		InetUtils.restartDNSMASQ();
		InetUtils.enableIPForward();
		int port = controller.getAutoDiscoveryCallback().getPort();
		int portSecure = controller.getAutoDiscoveryCallback().getPortSecure();
		InetUtils.setupNATRulesForCaptivePortal(port,portSecure,mode.getAPName(),InetUtils.DEFAULT_AP_GATEWAY_IP_ADDRESS,InetUtils.DEFAULT_AP_NETWORK);
		if (controller.getContext()!=null 
				&& controller.getContext().getGame()!=null 
				&& controller.getContext().getGame().hasOtherRobots()) {
			for (String address:controller.getContext().getGame().getOtherRobotsAddresses()) {
				InetUtils.addByPassToCaptivePortal(mode.getAPName(),address,port,portSecure);
			}
		}
		InetUtils.restartNetInterface(mode.getAPName());
		InetUtils.startHostAPD();
		if (controller.getContext()!=null) {
			controller.getContext().setAccessPointMode(mode);
		}
	}
	
	/**
	 * Generates a SSID replacing '#' for the corresponding hexa-decimal digit taken from MAC Address.
	 */
	public static String getSomeSSID() {
		if (defaultAPName.indexOf('#')<0)
			return defaultAPName; // no '#' characters found
		byte[] mac = InetUtils.getMACAddress();
		if (mac==null || mac.length==0) {
			return defaultAPName;
		}
		String full_mac_hexa = InetUtils.formatMAC(mac);
		int count_digits = StringUtils.countMatches(defaultAPName, "#");
		int next_mac_digit = (count_digits<full_mac_hexa.length()) ? (full_mac_hexa.length()-count_digits) : 0;
		StringBuilder sb = new StringBuilder();
		for (char c:defaultAPName.toCharArray()) {
			if (c=='#') {
				sb.append(full_mac_hexa.charAt(next_mac_digit));
				next_mac_digit++;
				if (next_mac_digit==full_mac_hexa.length())
					next_mac_digit = 0;
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static boolean hasNetInterface(String inet_name) throws SocketException {
		NetworkInterface iface = NetworkInterface.getByName(inet_name);
		return null!=iface;
	}
	
}

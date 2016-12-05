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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.guga.robotoy.rasp.camera.RPICamera;
import org.guga.robotoy.rasp.utils.IOUtils;
import org.guga.robotoy.rasp.utils.JSONUtils;
import org.guga.robotoy.rasp.utils.ProcessUtils;

/**
 * Utility methods for managing Raspberry network interfaces.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class InetUtils {
	
	private static final Logger log = Logger.getLogger(InetUtils.class.getName());

	public static String DEFAULT_ETH_INTERFACE = "eth0";

	public static String DEFAULT_WIFI_INTERFACE = "wlan0";
	
	public static String DEFAULT_EXTERNAL_WIFI_INTERFACE = "wlan1";
	
	public static String DEFAULT_AP_INTERFACE = "uap0";
	
	public static String DEFAULT_PATH_TO_CONFIG_FILES = "/etc/wpa_supplicant";
	
	public static String DEFAULT_NETWORK_INTERFACES_FILE = "/etc/network/interfaces";

	public static String DEFAULT_NETWORK_CONFIG_FILENAME = "wpa_supplicant.conf";

	public static String DEFAULT_AP_NETWORK = "192.168.2.0";

	public static String DEFAULT_AP_GATEWAY_IP_ADDRESS = "192.168.2.1";

	public static String DEFAULT_AP_NET_MASK = "255.255.255.0";
	
	public static String DEFAULT_HOSTAPD_CONFIG_FILE = "/etc/hostapd/hostapd.conf";
	
	public static String DEFAULT_HOSTAPD_OUTPUT_FILE = "/tmp/hostapdXXXX.log";
	
	public static String DEFAULT_HOSTAPD_PID_FILE = "/run/hostapd.pid";

	public static String DEFAULT_DNSMASQ_CONFIG_FILE = "/etc/dnsmasq.conf";
	
	/**
	 * Minimum length of WPA password according to IEEE standard
	 */
	public static final int MIN_WPA_KEY_LEN = 8;
	
	/**
	 * Driver used with HostAPD whenever using an external WiFi USB adapter
	 */
	public static String DEFAULT_EXTERNAL_HOSTAPD_DRIVER = "nl80211";

	/**
	 * Create new file contents to be used with 'ifup' and 'ifdown'.
	 */
	public static String makeNetInterfacesFile() {
		StringBuilder sb = new StringBuilder();
		sb.append("source-directory /etc/network/interfaces.d\n");
		// Loopback
		sb.append("\n");
		sb.append("auto lo\n");	// 'lo' is initilized with system startup
		sb.append("iface lo inet loopback\n");
		// Ethernet
		sb.append("\n");
		sb.append("allow-hotplug eth0\n");		// 'eth0' is initialized when kernel detects a hotplug event through interface
		sb.append("iface eth0 inet dhcp\n");	// 'eth0' is configured to use DHCP once it starts 
		// WiFi Client
		sb.append("\n");
		sb.append("auto "+DEFAULT_WIFI_INTERFACE+"\n");		// 'wlan0' is initilized with system startup
		sb.append("iface "+DEFAULT_WIFI_INTERFACE+" inet dhcp\n");		// 'wlan0' is configured to use DHCP once it starts
		sb.append("    wpa-conf "+DEFAULT_PATH_TO_CONFIG_FILES+"/"+DEFAULT_NETWORK_CONFIG_FILENAME+"\n");
		// WiFi Access Point
		sb.append("\n");
		sb.append("auto "+DEFAULT_AP_INTERFACE+"\n");				// 'uap0' is initilized with system startup
		sb.append("iface "+DEFAULT_AP_INTERFACE+" inet static\n");	// 'uap0' is configured to use static IP address
		sb.append("    address "+DEFAULT_AP_GATEWAY_IP_ADDRESS+"\n");	// 'uap0' static IP address
		sb.append("    netmask "+DEFAULT_AP_NET_MASK+"\n");				// 'uap0' network mask
		return sb.toString();
	}
	
	/**
	 * Check if current configuration includes our local network configuration for both WiFi client and access point
	 */
	public static boolean isNetInterfacesFileConfigured(String ap_name) throws IOException {
		try (InputStream input = new BufferedInputStream(new FileInputStream(new File(DEFAULT_NETWORK_INTERFACES_FILE)))) {
			String config = IOUtils.readFileContents(input);
			Pattern patterns[] = {
				Pattern.compile("^iface "+DEFAULT_WIFI_INTERFACE+" inet dhcp",Pattern.MULTILINE),
				Pattern.compile("^\\s*wpa-conf "+DEFAULT_PATH_TO_CONFIG_FILES+"/"+DEFAULT_NETWORK_CONFIG_FILENAME,Pattern.MULTILINE),
				Pattern.compile("^iface "+ap_name+" inet",Pattern.MULTILINE),
			};
			for (Pattern pattern:patterns) {
				if (!pattern.matcher(config).find())
					return false;
			}
			return true;
		}
	}
	
	/**
	 * Verifies the configuration in WPA_SUPPLICATION. Look for a given network SSID. Check additional configuration parameters.
	 * @param ssid Network that should be checked in configuration file
	 * @param hidden_ssid If it's true will also check if 'scan_ssid' configuration parameter is set to 1.
	 * @param passphrase If it's not null will also check if 'psk' configuratino parameter is equal to this or to the result of 'wpa_passphrase' convertion of it.
	 */
	public static boolean isWPAFileConfigured(String ssid,boolean hidden_ssid,String passphrase) {
		final String full_conf_filename = DEFAULT_PATH_TO_CONFIG_FILES+File.separator+InetUtils.DEFAULT_NETWORK_CONFIG_FILENAME;
		File config_file = new File(full_conf_filename);
		if (!config_file.exists() || !config_file.isFile())
			return false;
		WPASupplicantConf conf;
		try {
			conf = WPASupplicantConf.loadFile(full_conf_filename);
		} catch (IOException e) {
			return false;
		}
		WPASupplicantConf.NetworkConf net = conf.getNetworkConfigForSSID(ssid);
		if (net==null)
			return false;
		if (hidden_ssid && !new Integer(1).equals(net.getScanSsid()))
			return false;
		if (passphrase!=null && !passphrase.equals(net.getPSK())) {
			try {
				String encoded = genPSK(ssid,passphrase);
				if (!encoded.equals(net.getPSK()))
					return false;
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Change configuration in 'wpa_supplicant' configuration file preserving existing configuration network and parameters as possible. 
	 */
	public static void updateWPAFile(String ssid,boolean hidden_ssid,String passphrase,boolean quotePassphrase,int priority) throws IOException {
		final String full_conf_filename = InetUtils.DEFAULT_PATH_TO_CONFIG_FILES+File.separator+InetUtils.DEFAULT_NETWORK_CONFIG_FILENAME;
		File config_file = new File(full_conf_filename);
		WPASupplicantConf conf = (config_file.exists() && config_file.isFile()) ? WPASupplicantConf.loadFile(full_conf_filename) : new WPASupplicantConf();
		conf.setMissingHeaderConfigDefaultOptions();
		WPASupplicantConf.NetworkConf netconf = conf.getNetworkConfigForSSID(ssid);
		if (netconf==null) {
			// add new configuration
			netconf = new WPASupplicantConf.NetworkConf();
			conf.addPerNetworkConfig(netconf);
		}
		else {
			// edit existing configuration
		}
		netconf.setSSID(ssid);
		netconf.setPSK(passphrase,quotePassphrase);
		if (hidden_ssid)
			netconf.setScanSsid(1); // scan hidden SSID
		netconf.setId(ssid.replaceAll(" ", "_"));
		if (priority>0)
			netconf.setPriority(priority); 
		String contents = conf.getFullContents();
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Writing configuration file: "+config_file.getAbsolutePath());
		try (OutputStream output = new BufferedOutputStream(new FileOutputStream(config_file));) {
			output.write(contents.getBytes("UTF-8"));
		}
	}
	
	/**
	 * Generates a 256-bit PSK for a given pair of SSID and passphrase.
	 * @param passphrase Passphrase to use. Must be between 8 and 63 characters.
	 */
	public static String genPSK(String ssid,String passphrase) throws Exception {
		StringBuilder results = new StringBuilder();
		String[] cmd_line = {
			"wpa_passphrase",
			ssid,
			passphrase
		};
		ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, cmd_line);
		if (results.length()==0)
			throw new Exception("Could not get a valid result!");
		Pattern p = Pattern.compile("(?<!#)psk=([^\n]+)$", Pattern.MULTILINE);
		Matcher m = p.matcher(results.toString());
		if (!m.find()) {
			if (passphrase.length()<MIN_WPA_KEY_LEN)
				throw new Exception("Password should have a minimum of "+MIN_WPA_KEY_LEN+" characters!");
			else
				throw new Exception("Could not get a valid result!");
		}
		return m.group(1).trim();
	}
	
	public static enum WiFiBand {
		BAND_2_5_GHz,
		BAND_5_GHz;
	}
	
	/**
	 * Creates new configuration file used by 'hostapd' for providing Access Point service
	 */
	public static String makeHostAPDConfiguration(String ssid,boolean hidden_ssid,String passphrase,int channel,String ap_name,String driver,WiFiBand band) {
		if (ssid==null || ssid.length()==0) {
			throw new UnsupportedOperationException("SSID is missing!");
		}
		if (ssid.indexOf('\n')>=0
			|| ssid.indexOf('\r')>=0
			|| ssid.indexOf('\b')>=0
			|| ssid.indexOf('\"')>=0
			|| ssid.indexOf('\0')>=0) {
			throw new UnsupportedOperationException("SSID not supported: "+ssid);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("interface="+ap_name);
		if (driver!=null && driver.length()>0) 
			sb.append("\ndriver="+driver);
		sb.append("\nssid="+ssid);
		switch (band) {
		case BAND_2_5_GHz:
			sb.append("\nhw_mode=g");	// Using 2.4GHz band
			break;
		case BAND_5_GHz:
			sb.append("\nhw_mode=a");	// Using 5GHz band
			break;
		}
		sb.append("\nchannel="+channel);
		sb.append("\nmacaddr_acl=0");	// Accepts all MAC addresses
		if (hidden_ssid)
			sb.append("\nignore_broadcast_ssid=1");
		else
			sb.append("\nignore_broadcast_ssid=0");
		if (passphrase==null || passphrase.length()==0) {
			sb.append("\nauth_algs=0");		// Open (no authentication)			
		}
		else {
			sb.append("\nauth_algs=1");		// WPA authentication
			sb.append("\nwpa=2");		// WPA2 only
			sb.append("\nwpa_passphrase="+passphrase);
			sb.append("\nwpa_key_mgmt=WPA-PSK");
			sb.append("\nwpa_pairwise=TKIP");
			sb.append("\nrsn_pairwise=CCMP");
		}
		
		return sb.toString();
	}

	/**
	 * Check if current configuration includes given access point configuration parameters
	 */
	public static boolean isHostAPDFileConfigured(String ssid,String ap,WiFiBand band) throws IOException {
		try (InputStream input = new BufferedInputStream(new FileInputStream(new File(DEFAULT_HOSTAPD_CONFIG_FILE)))) {
			String config = IOUtils.readFileContents(input);
			Pattern patterns[] = {
				Pattern.compile("^interface="+ap,Pattern.MULTILINE),
				Pattern.compile("^ssid="+ssid,Pattern.MULTILINE),
				Pattern.compile("^hw_mode="+(WiFiBand.BAND_2_5_GHz.equals(band)?"g":"a"),Pattern.MULTILINE),
			};
			for (Pattern pattern:patterns) {
				if (!pattern.matcher(config).find())
					return false;
			}
			return true;
		}
	}
	
	/**
	 * Check if current configuration includes given access point configuration parameters
	 */
	public static boolean isDNSMasqConfigured(String ap) throws IOException {
		try (InputStream input = new BufferedInputStream(new FileInputStream(new File(DEFAULT_DNSMASQ_CONFIG_FILE)))) {
			String config = IOUtils.readFileContents(input);
			Pattern patterns[] = {
				Pattern.compile("^interface="+ap,Pattern.MULTILINE),
			};
			for (Pattern pattern:patterns) {
				if (!pattern.matcher(config).find())
					return false;
			}
			return true;
		}		
	}

	/**
	 * Creates new configuration file used by 'dnsmasq' for providing Access Point service
	 */
	public static String makeDNSMasqConfiguration(String ap) throws IOException {
		String config;
		try (InputStream input = new BufferedInputStream(new FileInputStream(new File(DEFAULT_DNSMASQ_CONFIG_FILE)))) {
			config = IOUtils.readFileContents(input);
		}
		// Check if there is already a configuration line indicating a interface name
		Pattern p = Pattern.compile("^interface=([^\n]+)",Pattern.MULTILINE);
		Matcher m = p.matcher(config);
		if (m.find()) {
			// Replace the existing line for a new one
			config = config.substring(0, m.start())
					+ "interface="+ap
					+ config.substring(m.end());
		}
		else {
			// If there is no such line, appends a new one at the end of the file
			config += "\ninterface="+ap;
		}
		return config;
	}
	
	/**
	 * Start some network interface given its name. 
	 */
	public static void startNetInterface(String iface_name) throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "ifup", iface_name);
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"ifup "+iface_name+" results:\n"+results.toString());
			}
		}
	}

	/**
	 * Stop some network interface given its name. 
	 */
	public static void stopNetInterface(String iface_name) throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "ifdown", iface_name);
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"ifdown "+iface_name+" results:\n"+results.toString());
			}
		}
	}

	/**
	 * Restart some network interface given its name. Usefull after changing network configurations.
	 */
	public static void restartNetInterface(String iface_name) throws Exception {
		stopNetInterface(iface_name);
		startNetInterface(iface_name);
	}
	
	/**
	 * Given a network interface name, return its address.
	 */
	public static String getNetInterfaceAddress(String iface_name) {
		try {
			NetworkInterface net = NetworkInterface.getByName(iface_name);
			if (net==null)
				return null;
			Enumeration<InetAddress> addrs = net.getInetAddresses();
			while (addrs.hasMoreElements()) {
				InetAddress addr = addrs.nextElement();
				if (addr.isLoopbackAddress())
					continue;
				return addr.getHostAddress();
			}
			return null;
		} catch (SocketException e) {
			return null;
		}
	}
	
	/**
	 * Given a network interface name and an IP address with optional network mask (e.g.: 192.168.2.1/24),
	 * assign the given address to the given interface.
	 */
	public static void setNetInterfaceAddress(String iface_name,String address_with_mask) throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "ip", "addr", "add",
					address_with_mask,
					"dev", iface_name);
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"ip addr add results:\n"+results.toString());
			}
		}
	}
		
	/**
	 * Attaches the 'uap0' interface to 'wlan0' interface and configure it as an access point
	 */
	public static void createAPInterface(String iface_name,String ap_name) throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iw", "dev", iface_name, "interface", "add", ap_name, "type", "__ap");
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"iw add __ap results:\n"+results.toString());
			}
		}
	}

	/**
	 * Enables 'IP-forwarding'
	 */
	public static void enableIPForward() throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "sysctl", "net.ipv4.ip_forward=1");
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"sysctl results:\n"+results.toString());
			}
		}
	}
	
	/**
	 * Add a exception rule to a given address and given TCP port numbers. This means that incoming packets from
	 * AP network with this destination address will be forwarded (won't be dropped by firewall rules)
	 */
	public static void addByPassToCaptivePortal(String ap_name,String destinationAddress,int port,int portSecure) throws Exception {
		StringBuilder results = new StringBuilder();
		try {
		
			for (int portNumber:new int[]{port,portSecure}) {
				if (portNumber==0)
					continue;
				ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
						"-t", "mangle",		// 'mangle' table
						"-I", "PREROUTING",	// inserting rule to the beginning of this chain
						"-i", ap_name,		// originated from AP interface
						"-p", "tcp",		// TCP protocol
						"--dport", String.valueOf(portNumber),
						"-d", destinationAddress,	// this destination
						"-j", "RETURN");	// target: return to calling chain
			}
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"iptables -A results:\n"+results.toString());
			}
		}
	}
	
	/**
	 * Inserts rules to local firewall in order to provide a 'captive portal' for incoming requests from Access Point network.
	 * @see https://github.com/Byzantium/Byzantium/blob/master/captive_portal/captive-portal.sh
	 */
	public static void setupNATRulesForCaptivePortal(int port,int portSecure,String ap_name,String captiveAddress,String network) throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			// Remove all previous rules in 'filter' and 'nat' tables
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "--flush");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "-t", "nat",	"--flush");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "-t", "mangle",	"--flush");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "--delete-chain");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "-t", "nat",	"--delete-chain");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "-t", "mangle",	"--delete-chain");
			
			// Add new chain for marking packets arriving from AP interface			
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "mangle",		// 'mangle' table
					"-N", "internet");	// creates a new chain called 'internet' 

			// Exempt traffic which does not originate from the client network
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "mangle",		// 'mangle' table
					"-A", "PREROUTING",	// adding rule to this chain
					"-p", "all",		// all protocols
					"!", "-s", network+"/24",	// except this network
					"-j", "RETURN");	// target: return to calling chain
			
			// Exempt traffic related do DNS
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "mangle",		// 'mangle' table
					"-A", "PREROUTING",	// adding rule to this chain
					"-p", "udp",		// UDP protocol
					"--dport", "53",	// DNS port
					"-j", "RETURN");	// target: return to calling chain
			
			// Traffic not exempted by the above rules gets kicked to the captive
			// portal chain.
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "mangle",		// 'mangle' table
					"-A", "PREROUTING",	// adding rule to this chain
					"-i", ap_name,		// originated from AP interface
					"-j", "internet");	// target: jump to 'internet' chain of rules (will mark these packets)
			
			// Traffic in 'internet' chain gets marked 99.
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "mangle",		// 'mangle' table
					"-A", "internet",	// adding rule to this chain
					"-j", "MARK",		// target: mark module
					"--set-mark", "99");	// number to mark (99)
			
			// Traffic which has been marked 99 and is headed for 80/TCP or 443/TCP
			// should be redirected to the captive portal web server.
			for (int port_number:new int[]{port, portSecure}) {
				if (port_number==0)
					continue;
				ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "nat",		// 'nat' table
					"-A", "PREROUTING",	// adding rule to this chain
					"-m", "mark",		// use mark module
					"--mark", "99",		// check for this mark number (99)
					"-p", "tcp",		// TCP protocol
					"--dport", String.valueOf(port_number),	// destination to this port
					"-j", "DNAT",		// change destination
					"--to-destination", captiveAddress+":"+port_number); // to this address and port number
			}
			
			// Other traffic should be masqueraded in order to allow the use of this access point as
			// a mean for other targets
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "nat",			// 'nat' table
					"-I", "POSTROUTING",	// inserting rule to the beginning of this chain
					"-s", network+"/24",	// originated in AP network
					"!", "-d", network+"/24",	// not destined to the same network
					"-j", "MASQUERADE");	// NAT masquerading
			
			// Bypass DNS traffic
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "filter",		// 'filter' table
					"-A", "FORWARD",	// adding rule to this chain
					"-p", "udp",		// UDP protocol
					"--dport", "53",	// DNS port
					"-j", "RETURN");	// target: return to calling chain

			// Bypass Raspberry Pi Camera traffic
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "filter",		// 'filter' table
					"-A", "FORWARD",	// adding rule to this chain
					"-p", "tcp",		// TCP protocol
					"--dport", String.valueOf(RPICamera.DEFAULT_PORT),
					"-j", "RETURN");	// target: return to calling chain

			// All other traffic which is marked 99 is just dropped
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "filter",		// 'filter' table
					"-A", "FORWARD",	// adding rule to this chain
					"-m", "mark",		// use mark module
					"--mark", "99",		// check for this mark number (99)
					"-j", "DROP");		// target: drop packets

			// Allow incoming traffic that is headed for the local node
			for (int port_number:new int[]{53, port, portSecure, 1248, RPICamera.DEFAULT_PORT}) {
				if (port_number==0)
					continue;
				ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
						"-t", "filter",		// 'filter' table
						"-A", "INPUT",		// adding rule to this chain
						"-p", "tcp",		// TCP protocol
						"--dport", String.valueOf(port_number),	// this port number
						"-j", "ACCEPT");		// target: accept packets				
			}
			for (int port_number:new int[]{53, 67, 698, 5353, AutoDiscoverService.DEFAULT_PORT}) {
				if (port_number==0)
					continue;
				ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
						"-t", "filter",		// 'filter' table
						"-A", "INPUT",		// adding rule to this chain
						"-p", "udp",		// UDP protocol
						"--dport", String.valueOf(port_number),	// this port number
						"-j", "ACCEPT");		// target: accept packets								
			}
			
			// Websockets from other robotoy connected through access point to this robotoy
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "filter",		// 'filter' table
					"-A", "INPUT",		// adding rule to this chain
					"-p", "tcp",		// TCP protocol
					"-s", network+"/24",
					"-d", captiveAddress,	
					"--sport", String.valueOf(port),
					"-j", "ACCEPT");		// target: accept packets
			if (portSecure!=0) {
				ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
						"-t", "filter",		// 'filter' table
						"-A", "INPUT",		// adding rule to this chain
						"-p", "tcp",		// TCP protocol
						"-s", network+"/24",
						"-d", captiveAddress,	
						"--sport", String.valueOf(portSecure),
						"-j", "ACCEPT");		// target: accept packets				
			}
			
			// Autodiscovery reply from other robotoy to broadcast from this robotoy
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "filter",		// 'filter' table
					"-A", "INPUT",		// adding rule to this chain
					"-p", "udp",		// UDP protocol
					"-s", network+"/24",
					"-d", captiveAddress,	
					"--sport", String.valueOf(AutoDiscoverService.DEFAULT_PORT),
					"-j", "ACCEPT");		// target: accept packets

			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "filter",		// 'filter' table
					"-A", "INPUT",		// adding rule to this chain
					"-p", "udp",		// UDP protocol
					"-s", captiveAddress,	
					"-d", network+"/24",	
					"-j", "ACCEPT");		// target: accept packets

			// But reject anything else coming from unrecognized users.
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables",
					"-t", "filter",		// 'filter' table
					"-A", "INPUT",		// adding rule to this chain
					"-m", "mark",		// use mark module
					"--mark", "99",		// check for this mark number (99)
					"-j", "DROP");		// target: drop packets
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"iptables -A results:\n"+results.toString());
			}
		}
	}
	
	/**
	 * Add route configuration of multicast network to a given network device
	 */
	public static void setMulticastRoute(String inet_name) throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "ip", 
					"route", 
					"add", AutoDiscoverService.DEFAULT_MULTICAST_NETWORK+"/24", 
					"dev", inet_name);
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"sysctl results:\n"+results.toString());
			}
		}		
	}

	/**
	 * Delete rule inserted by 'setupNATRulesForCaptivePortal'
	 */
	public static void removeNATRules() throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "--flush");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "-t", "nat",	"--flush");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "-t", "mangle",	"--flush");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "--delete-chain");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "-t", "nat",	"--delete-chain");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iptables", "-t", "mangle",	"--delete-chain");
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"iptables -D results:\n"+results.toString());
			}
		}
	}
	
	/**
	 * Restart service responsible for providing DNS + DHCP.
	 */
	public static void restartDNSMASQ() throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "service", "dnsmasq", "restart");
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"service dnsmasq restart results:\n"+results.toString());
			}
		}
	}
	
	/**
	 * Stop service responsible for providing DNS + DHCP.
	 */
	public static void stopDNSMASQ() throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "service", "dnsmasq", "stop");
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"service dnsmasq stop results:\n"+results.toString());
			}
		}
	}

	/**
	 * Start service resposible for hosting Access Point
	 */
	public static void startHostAPD() throws Exception {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String logFile = DEFAULT_HOSTAPD_OUTPUT_FILE.replace("XXXX", timestamp);
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "hostapd",
				"-B", // run daemon in the background
				"-f", logFile, // output log to file
				"-P", DEFAULT_HOSTAPD_PID_FILE, // PID file
				DEFAULT_HOSTAPD_CONFIG_FILE); // configuration file
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"hostapd results:\n"+results.toString());
			}
		}
	}

	/**
	 * Get PID for service resposible for hosting Access Point
	 */
	public static int getHostAPD_PID() throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, 
				"/bin/sh",
				"-c", // run command
				"echo $(ps aux | grep '[h]ostapd' | awk '{print $2}')");
			String s = results.toString();
			if (s.length()==0)
				return -1;
			Pattern p = Pattern.compile("^\\d+$");
			Matcher m = p.matcher(s.trim());
			if (!m.find())
				return -1;
			return Integer.parseInt(m.group());
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"echo pid process results:\n"+results.toString());
			}
		}
	}

	/**
	 * Get MAC address for a given network interface. Use /sys/class/net mount point.
	 */
	public static String getHWAddress(String iface_name) throws Exception {
		File file = new File("/sys/class/net/"+iface_name+"/address");
		try (FileInputStream input = new FileInputStream(file);)
		{
			byte[] buffer = new byte[256];
			int len = input.read(buffer);
			if (len>0)
				return new String(buffer,0,len,"UTF-8");
			else
				return null;
		}
	}

	/**
	 * Get PID for service resposible for hosting Access Point
	 */
	public static String getStatusNetInterface(String iface_name) throws Exception {
		File file = new File("/sys/class/net/"+iface_name+"/operstate");
		try (FileInputStream input = new FileInputStream(file);)
		{
			byte[] buffer = new byte[256];
			int len = input.read(buffer);
			if (len>0)
				return new String(buffer,0,len,"UTF-8");
			else
				return null;
		}
	}
	
	/**
	 * Returns the connected wireless network name.
	 */
	public static String getConnectedWirelessNetwork() throws Exception {
		StringBuilder results = new StringBuilder();
		ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, 
			"iwgetid",
			"-r");
		return results.toString();
	}

	/**
	 * Get driver name for a given interface name
	 */
	public static String getDriverName(String iface_name) {
		File file = new File("/sys/class/net/"+iface_name+"/device/driver");
		Path path = file.toPath();
		if (Files.isSymbolicLink(path)) {
			String target;
			try {
				target = Files.readSymbolicLink(path).toString();
			} catch (IOException e) {
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE,"error reading symbolic link",e);
				return null;
			}
			int sep = target.lastIndexOf(File.separator);
			if (sep>0)
				return target.substring(sep+1);
			else
				return target;
		}
		else {
			return null;
		}
	}

	/**
	 * Stop service resposible for hosting Access Point
	 */
	public static void stopHostAPD() throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, 
				"/bin/sh",
				"-c", // run command
				"kill $(ps aux | grep '[h]ostapd' | awk '{print $2}')"); 
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"stop hostapd results:\n"+results.toString());
			}
		}
	}
	
	public static void reloadNetworkService() throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, 
				"systemctl",
				"daemon-reload");
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, 
				"service",
				"networking", 
				"restart");
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"service networking restart process results:\n"+results.toString());
			}
		}
	}
	
	public static void renewIPAddress(String inet_name) throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, 
				"/sbin/dhclient",
				inet_name);
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"renew IP address results:\n"+results.toString());
			}
		}		
	}
	
	/**
	 * If there is a wireless device, returns its MAC address<BR>
	 * If there is another type of network device with some MAC address attached to it, except for ones
	 * with 'virtual' in its name, returns its MAC address<BR>
	 * Otherwise, return NULL
	 */
	public static byte[] getMACAddress() {
		final Pattern patternWiFi = Pattern.compile("Wireless|WiFi",Pattern.CASE_INSENSITIVE);
		final Pattern patternVirtual = Pattern.compile("Virtual",Pattern.CASE_INSENSITIVE);
		
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			byte[] second_best_candidate = null;
			for (NetworkInterface net:Collections.list(nets)) {
				byte[] mac = net.getHardwareAddress();
				if (mac==null || mac.length<6)
					continue;
				if (net.isLoopback())
					continue;
				String name = net.getDisplayName();
				if (name!=null) {
					if (patternWiFi.matcher(name).find())
						return mac;
					if (patternVirtual.matcher(name).find())
						continue;
					if (mac[0]==0 && mac[1]==0 && mac[2]==0 && mac[3]==0 && mac[4]==0 && mac[5]==0)
						continue;
					if (second_best_candidate==null)
						second_best_candidate = mac;
				}
			}
			if (second_best_candidate!=null)
				return second_best_candidate;
			else
				return null;
		} catch (SocketException e) {
			return null;
		}
	}
	
	/**
	 * Formats a MAC Address using hexa-decimal digits concatenated without
	 * separating symbols.
	 */
	public static String formatMAC(byte[] mac) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mac.length; i++) {
			sb.append(String.format("%02X", mac[i]));
		}
		return sb.toString();
	}
	
	/**
	 * Given a string representation of a MAC address, returns its corresponding
	 * binary representation.
	 */
	public static byte[] unformatMAC(String mac) {
		if (mac==null || mac.length()==0)
			return null;
		mac = mac.replaceAll("[^A-Fa-f\\d]", "");
		if (mac.length()==0)
			return null;
		byte[] ret = new byte[mac.length()/2];
		for (int i=0;i<mac.length()-1;i+=2) {
			ret[i/2] = (byte)Short.parseShort(mac.substring(i, i+2), 16);			
		}
		return ret;
	}

	/**
	 * Return list of installed network adapters
	 */
	public static NetAdapter[] getNetAdapters() {
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			List<NetAdapter> result = new LinkedList<>();
			for (NetworkInterface net:Collections.list(nets)) {
				if (net.isLoopback())
					continue;
				NetAdapter ad = new NetAdapter();
				ad.setName(net.getName());
				ad.setDisplayName(net.getDisplayName());
				ad.setVirtual(net.isVirtual());
				byte[] mac = net.getHardwareAddress();
				if (mac!=null && mac.length>0) {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < mac.length; i++) {
						sb.append(String.format("%02X", mac[i]));
					}
					ad.setMAC(sb.toString());
				}
				ad.setDriver(InetUtils.getDriverName(net.getName()));
				Enumeration<InetAddress> addresses = net.getInetAddresses();
				if (addresses!=null) {
					List<String> list_ips = new LinkedList<>();
					for (InetAddress ip:Collections.list(addresses)) {
						list_ips.add(ip.getHostAddress());
					}
					if (!list_ips.isEmpty())
						ad.setAddresses(list_ips.toArray(new String[list_ips.size()]));
				}
				result.add(ad);
			}
			return result.toArray(new NetAdapter[result.size()]);
		} catch (SocketException e) {
			return null;
		}
	}
	
	/**
	 * Return list of existent wireless adapters (filter by name)
	 */
	public static NetAdapter[] getWirelessAdapters() {

		NetAdapter[] adapters = InetUtils.getNetAdapters();
		if (adapters==null || adapters.length==0)
			return null;
		final Pattern pWiFi = Pattern.compile("^wlan\\d+$",Pattern.CASE_INSENSITIVE);
		int count_wifi = 0;
		for (NetAdapter a:adapters) {
			Matcher mWiFi = pWiFi.matcher(a.getName());
			if (mWiFi.find()) {
				count_wifi++;
			}
		}
		if (count_wifi==0)
			return null;
		if (count_wifi==adapters.length)
			return adapters;
		// There are some network adapters that are not 'wireless'. Let's return a new array without them.
		NetAdapter[] only_wireless = new NetAdapter[count_wifi];
		count_wifi = 0;
		for (NetAdapter a:adapters) {
			Matcher mWiFi = pWiFi.matcher(a.getName());
			if (mWiFi.find()) {
				only_wireless[count_wifi++] = a;
			}			
		}
		return only_wireless;
	}

	/**
	 * Scan for wireless networks
	 */
	public static WiFiNetwork[] scanWiFi(String inet_name) throws Exception {
		StringBuilder results = new StringBuilder();
		try {
			ProcessUtils.execute(null, results, ProcessUtils.DEFAULT_ENCODING, "iwlist", inet_name, 
				"scanning");	// scan for WiFi signals 
			if (results.length()>0) {
				Pattern pCell = Pattern.compile("^\\s*Cell \\d+", Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
				Pattern pAddress = Pattern.compile("\\bAddress\\s*[:=]\\s*([\\d\\:A-F]+)", Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
				Pattern pChannel = Pattern.compile("\\bChannel\\s*[:=]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
				Pattern pFreq = Pattern.compile("\\bFrequency\\s*[:=]\\s*([\\d\\.]+)", Pattern.CASE_INSENSITIVE);
				Pattern pQuality = Pattern.compile("\\bQuality\\s*[:=]\\s*(\\d+)/(\\d+)", Pattern.CASE_INSENSITIVE);
				Pattern pSSID = Pattern.compile("\\bE?SSID\\s*[:=]\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
				String[] cellParts = pCell.split(results.toString());
				List<WiFiNetwork> list = new LinkedList<>();
				for (String cellPart:cellParts) {
					WiFiNetwork net = new WiFiNetwork();
					Matcher mAddress = pAddress.matcher(cellPart);
					Matcher mChannel = pChannel.matcher(cellPart);
					Matcher mFreq = pFreq.matcher(cellPart);
					Matcher mQuality = pQuality.matcher(cellPart);
					Matcher mSSID = pSSID.matcher(cellPart);
					if (mAddress.find())
						net.setAddress(mAddress.group(1));
					if (mChannel.find())
						net.setChannel(Integer.parseInt(mChannel.group(1)));
					if (mFreq.find())
						net.setFreq(Double.parseDouble(mFreq.group(1)));
					if (mQuality.find()) {
						net.setQuality(Integer.parseInt(mQuality.group(1)));
						net.setQualityMax(Integer.parseInt(mQuality.group(2)));
					}
					if (mSSID.find()) 
						net.setSSID(mSSID.group(1));
					if (net.isEmpty())
						continue;
					list.add(net);
				}
				return list.toArray(new WiFiNetwork[list.size()]);
			}
			return null;
		}
		finally {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,"iwlist "+inet_name+" results:\n"+results.toString());
			}
		}
	}

	public static class WiFiNetwork {
		private String address;
		private int channel;
		private double freq;
		private int quality;
		private int qualityMax;
		private String ssid;
		public String getAddress() {
			return address;
		}
		public void setAddress(String address) {
			this.address = address;
		}
		public int getChannel() {
			return channel;
		}
		public void setChannel(int channel) {
			this.channel = channel;
		}
		public double getFreq() {
			return freq;
		}
		public void setFreq(double freq) {
			this.freq = freq;
		}
		public int getQuality() {
			return quality;
		}
		public void setQuality(int quality) {
			this.quality = quality;
		}
		public int getQualityMax() {
			return qualityMax;
		}
		public void setQualityMax(int qualityMax) {
			this.qualityMax = qualityMax;
		}
		public String getSSID() {
			return ssid;
		}
		public void setSSID(String ssid) {
			this.ssid = ssid;
		}
		public boolean isEmpty() {
			return address==null && channel==0 && freq==0 && quality==0 && qualityMax==0 && ssid==null;
		}
		public String toString() {
			return getAddress()+":\""+getSSID()+"\"";
		}
	}
	
	public static class NetAdapter {
		private String name;
		private String displayName;
		private String[] addresses;
		private boolean virtual;
		private String mac;
		private String driver;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDisplayName() {
			return displayName;
		}
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
		public String[] getAddresses() {
			return addresses;
		}
		public void setAddresses(String[] addresses) {
			this.addresses = addresses;
		}
		public boolean isVirtual() {
			return virtual;
		}
		public void setVirtual(boolean virtual) {
			this.virtual = virtual;
		}
		public String getMAC() {
			return mac;
		}
		public void setMAC(String mac) {
			this.mac = mac;
		}
		public String getDriver() {
			return driver;
		}
		public void setDriver(String driver) {
			this.driver = driver;
		}
		public String toString() {
			return JSONUtils.toJSON(this, false);
		}
	}


	/**
	 * Modes of WiFi operation.
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	public static enum WiFiModeEnum{
		/**
		 * Internal WiFi (wlan0) used as Access Point only
		 */
		INTERNAL_AP(DEFAULT_WIFI_INTERFACE),
		
		/**
		 * Internal WiFi used as both client (wlan0) and Access Point (virtual interface uap0)
		 */
		VIRTUAL_AP(DEFAULT_WIFI_INTERFACE, /*virtual*/ DEFAULT_AP_INTERFACE),
		
		/**
		 * External WiFi USB adapter (wlan1) used as Access Point at 2.5 GHz
		 */
		EXTERNAL_2G5(DEFAULT_EXTERNAL_WIFI_INTERFACE, WiFiBand.BAND_2_5_GHz),
		
		/**
		 * External WiFi USB adapter (wlan1) used as Access Point at 5 GHz
		 */
		EXTERNAL_5G(DEFAULT_EXTERNAL_WIFI_INTERFACE, WiFiBand.BAND_5_GHz);
		
		private final String inetName;
		
		private final String virtualName;
		
		private final WiFiBand band;
		
		WiFiModeEnum(String inetName) {
			this.inetName = inetName;
			this.virtualName = null;
			this.band = WiFiBand.BAND_2_5_GHz;
		}

		WiFiModeEnum(String inetName,WiFiBand band) {
			this.inetName = inetName;
			this.virtualName = null;
			this.band = band;
		}

		WiFiModeEnum(String inetName,String virtualName) {
			this.inetName = inetName;
			this.virtualName = virtualName;
			this.band = WiFiBand.BAND_2_5_GHz;
		}

		public String getINetName() {
			return inetName;
		}
		
		public String getVirtualName() {
			return virtualName;
		}

		public WiFiBand getBand() {
			return band;
		}

		public String getAPName() {
			if (virtualName!=null)
				return virtualName;
			else
				return inetName;
		}
		
		public boolean isExternal() {
			return EXTERNAL_2G5.equals(this) || EXTERNAL_5G.equals(this);
		}
	}
}

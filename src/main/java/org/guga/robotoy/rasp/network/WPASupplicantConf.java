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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.guga.robotoy.rasp.utils.IOUtils;

/**
 * This class wraps current WPASupplicant configuration file.<BR>
 * It allows seeking information and easy reconstructing the whole file.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class WPASupplicantConf {
	
	private static final String DEFAULT_CTRL_DIR = "/var/run/wpa_supplicant";
	private static final String DEFAULT_CTRL_GROUP = "netdev";
	private static final int DEFAULT_UPDATE_CONFIG = 1;
	private static final String TABS = "    ";

	private String headerConfig;
	
	private String ctrlInterface;
	
	private Integer updateConfig;
	
	private String country;
	
	public static class NetworkConf {
		private String config;
		private String ssid;
		private String psk;
		private String id;
		private Integer scanSsid;
		private Integer priority;
		
		public String getConfig() {
			return config;
		}

		public String getSSID() {
			return ssid;
		}

		public void setSSID(String ssid) {
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
			this.ssid = ssid;
			config = appendOrReplace("ssid","\""+ssid+"\"",TABS,config);
		}

		public String getPSK() {
			return psk;
		}

		public void setPSK(String psk) {
			setPSK(psk,/*quoted*/true);
		}
		
		public void setPSK(String psk, boolean quoted) {
			this.psk = psk;
			if (quoted)
				config = appendOrReplace("psk","\""+psk+"\"",TABS,config);
			else
				config = appendOrReplace("psk",psk,TABS,config);
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
			config = appendOrReplace("id_str","\""+id+"\"",TABS,config);
		}

		public Integer getScanSsid() {
			return scanSsid;
		}

		public void setScanSsid(int scanSsid) {
			this.scanSsid = scanSsid;
			config = appendOrReplace("scan_ssid",String.valueOf(scanSsid),TABS,config);
		}
	
		public Integer getPriority() {
			return priority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
			config = appendOrReplace("priority",String.valueOf(priority),TABS,config);
		}

		public NetworkConf() {
			this.config = "network={\n}";
		}

		public void parse(String contents) {
			this.config = contents;
			this.ssid = getUnquotedParameter("ssid", contents);
			this.psk = getUnquotedParameter("psk", contents);
			this.id = getUnquotedParameter("id_str", contents);
			this.scanSsid = getIntParameter("scan_ssid", contents);
			this.priority = getIntParameter("priority", contents);
		}
	}
	
	private List<NetworkConf> perNetworkConfig;
		
	public String getHeaderConfig() {
		return headerConfig;
	}

	public void setMissingHeaderConfigDefaultOptions() {
		if (ctrlInterface==null || ctrlInterface.length()==0)
			setCtrlInterface(DEFAULT_CTRL_DIR,DEFAULT_CTRL_GROUP);
		if (updateConfig==null)
			setUpdateConfig(DEFAULT_UPDATE_CONFIG);
	}
	
	public void setCtrlInterface(String dir,String group) {
		this.ctrlInterface = "DIR="+dir+" GROUP="+group;
		headerConfig = appendOrReplace("ctrl_interface",ctrlInterface,null,headerConfig);
	}
	
	public String getCtrlInterface() {
		return ctrlInterface;
	}

	public void setCountry(String country) {
		headerConfig = appendOrReplace("country",country,null,headerConfig);
		this.country = country;
	}
	
	public String getCountry() {
		return country;
	}

	public void setUpdateConfig(int value) {
		headerConfig = appendOrReplace("update_config",String.valueOf(value),null,headerConfig);
		this.updateConfig = value;
	}

	public Integer getUpdateConfig() {
		return updateConfig;
	}

	public List<NetworkConf> getPerNetworkConfig() {
		return perNetworkConfig;
	}

	public void setPerNetworkConfig(List<NetworkConf> perNetworkConfig) {
		this.perNetworkConfig = perNetworkConfig;
	}

	public void addPerNetworkConfig(NetworkConf netConf) {
		if (perNetworkConfig==null)
			perNetworkConfig = new LinkedList<>();
		perNetworkConfig.add(netConf);
	}
	
	public NetworkConf getNetworkConfigForSSID(String ssid) {
		if (perNetworkConfig==null)
			return null;
		for (NetworkConf conf:perNetworkConfig) {
			if (conf.getSSID()!=null && conf.getSSID().equalsIgnoreCase(ssid))
				return conf;
		}
		return null;
	}

	public NetworkConf getNetworkConfigForID(String ssid) {
		if (perNetworkConfig==null)
			return null;
		for (NetworkConf conf:perNetworkConfig) {
			if (conf.getId()!=null && conf.getId().equalsIgnoreCase(ssid))
				return conf;			
		}
		return null;		
	}

	public static WPASupplicantConf loadFile(String filename) throws IOException {
		try (InputStream input = new BufferedInputStream(new FileInputStream(new File(filename)))) {
			String config = IOUtils.readFileContents(input);
			WPASupplicantConf parsed = new WPASupplicantConf();
			parsed.parse(config);
			return parsed;
		}
	}
	
	public String getFullContents() {
		StringBuilder contents = new StringBuilder();
		if (headerConfig!=null) {
			contents.append(headerConfig);
			if (!headerConfig.endsWith("\n"))
				contents.append("\n");
		}
		if (perNetworkConfig!=null && !perNetworkConfig.isEmpty()) {
			for (NetworkConf netconf:perNetworkConfig) {
				contents.append(netconf.getConfig());
				if (!netconf.getConfig().endsWith("\n"))
					contents.append("\n");
			}
		}
		return contents.toString();
	}
	
	public void parse(String contents) {
		if (contents==null || contents.trim().length()==0)
			return;
		this.ctrlInterface = getUnquotedParameter("ctrl_interface", contents);
		this.updateConfig = getIntParameter("update_config", contents);
		this.country = getUnquotedParameter("country", contents);
		Pattern pNetworkInfoStart = Pattern.compile("^\\s*network\\s*=\\s*\\{",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher mNetworkInfoStart = pNetworkInfoStart.matcher(contents);
		int previous_network_info_start = -1;
		while (mNetworkInfoStart.find()) {
			if (previous_network_info_start<0) {
				headerConfig = contents.substring(0, mNetworkInfoStart.start());
			}
			else {
				NetworkConf netConf = new NetworkConf();
				String net_contents = contents.substring(previous_network_info_start, mNetworkInfoStart.start());
				netConf.parse(net_contents);
				addPerNetworkConfig(netConf);
			}
			previous_network_info_start = mNetworkInfoStart.start();
		}
		if (previous_network_info_start<0) {
			headerConfig = contents;
		}
		else {
			NetworkConf netConf = new NetworkConf();
			String net_contents = contents.substring(previous_network_info_start);
			netConf.parse(net_contents);
			addPerNetworkConfig(netConf);			
		}
	}
	
	private static String removeQuotes(String s) {
		if (s==null)
			return s;
		s = s.trim();
		if (s.length()<2)
			return s;
		if (s.startsWith("\"") && s.endsWith("\""))
			return s.substring(1, s.length()-1);
		else
			return s;
	}
	
	private static String getUnquotedParameter(String paramName,String contents) {
		Pattern p = Pattern.compile("^\\s*"+Pattern.quote(paramName)+"\\s*=\\s*([^\r\n]+)$",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher m = p.matcher(contents);
		if (m.find()) {
			return removeQuotes(m.group(1));
		}		
		else {
			return null;
		}
	}
	
	private static Integer getIntParameter(String paramName,String contents) {
		Pattern p = Pattern.compile("^\\s*"+Pattern.quote(paramName)+"\\s*=\\s*(\\d+)$",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher m = p.matcher(contents);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		}		
		else {
			return null;
		}
	}

	private static String appendOrReplace(String paramName,String newContents,String identation,String fullContents) {
		if (fullContents==null || fullContents.length()==0)
			return paramName+"="+newContents;
		Pattern p = Pattern.compile("^\\s*"+Pattern.quote(paramName)+"\\s*=\\s*([^\r\n]+)$",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher m = p.matcher(fullContents);
		String line = paramName+"="+newContents;
		if (identation!=null)
			line = identation + line;
		if (!m.find()) {
			int end_of_block = fullContents.lastIndexOf('}');
			if (end_of_block>0) {
				if (fullContents.charAt(end_of_block-1)=='\n')
					return fullContents.substring(0,end_of_block)
							+ line + "\n"
							+ fullContents.substring(end_of_block);
				else
					return fullContents.substring(0,end_of_block) + "\n"
							+ line + "\n"
							+ fullContents.substring(end_of_block);
			}
			else {
				if (!fullContents.endsWith("\n"))
					fullContents += "\n";
				return fullContents + line;
			}
		}
		if (m.end()==fullContents.length())
			return fullContents.substring(0, m.start())
					+ line;
		else					
			return fullContents.substring(0, m.start())
				+ line
				+ fullContents.substring(m.end());
	}
}

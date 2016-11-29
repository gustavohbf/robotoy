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
import org.junit.Test;
import static org.junit.Assert.*;

import org.guga.robotoy.rasp.network.WPASupplicantConf;

public class WPASupplicantConfTest {

    @Test public void testParsingConf() {
    	
    	String sample_conf = 
	    "ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev\n"+
		"update_config=1\n"+
		"\n"+
		"network={\n"+
		"    ssid=\"SCHOOLS NETWORK NAME\"\n"+
		"    psk=\"SCHOOLS PASSWORD\"\n"+
		"    id_str=\"school\"\n"+
		"}\n"+
		"\n"+
		"network={\n"+
		"    ssid=\"HOME NETWORK NAME\"\n"+
		"    psk=\"HOME PASSWORD\"\n"+
		"    id_str=\"home\"\n"+
		"}";
    	
		WPASupplicantConf parsed = new WPASupplicantConf();
		parsed.parse(sample_conf);
		
		assertEquals("DIR=/var/run/wpa_supplicant GROUP=netdev",parsed.getCtrlInterface());
		assertEquals(new Integer(1),parsed.getUpdateConfig());
		assertNotNull(parsed.getPerNetworkConfig());
		assertEquals(2,parsed.getPerNetworkConfig().size());
		
		WPASupplicantConf.NetworkConf netconf = parsed.getNetworkConfigForSSID("SCHOOLS NETWORK NAME");
		assertNotNull(netconf);
		assertEquals("SCHOOLS PASSWORD",netconf.getPSK());
		assertEquals("school",netconf.getId());

		netconf = parsed.getNetworkConfigForSSID("home network name");
		assertNotNull(netconf);
		assertEquals("HOME PASSWORD",netconf.getPSK());
		assertEquals("home",netconf.getId());
		
		// Do some configuration changes
		netconf.setPSK("NEW PASSWORD");
		
		parsed.setCountry("BR");
		
		netconf = new WPASupplicantConf.NetworkConf();
		netconf.setSSID("NEW NETWORK");
		netconf.setPSK("NEW PASSWORD");
		netconf.setId("new");
		parsed.addPerNetworkConfig(netconf);
		
		
		// Check result
		String new_contents = parsed.getFullContents();
		String expected_contents = 
	    "ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev\n"+
		"update_config=1\n"+
	    "country=BR\n" +
		"\n"+
		"network={\n"+
		"    ssid=\"SCHOOLS NETWORK NAME\"\n"+
		"    psk=\"SCHOOLS PASSWORD\"\n"+
		"    id_str=\"school\"\n"+
		"}\n"+
		"\n"+
		"network={\n"+
		"    ssid=\"HOME NETWORK NAME\"\n"+
		"    psk=\"NEW PASSWORD\"\n"+
		"    id_str=\"home\"\n"+
		"}\n"+
		"network={\n"+
		"    ssid=\"NEW NETWORK\"\n"+
		"    psk=\"NEW PASSWORD\"\n"+
		"    id_str=\"new\"\n"+
		"}";
		assertEquals(expected_contents,new_contents.trim());
    }
}

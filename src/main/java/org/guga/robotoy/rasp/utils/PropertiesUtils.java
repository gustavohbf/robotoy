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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for reading external properties files.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class PropertiesUtils {
	
	private static Map<String,Properties> map_properties_open = new HashMap<>();

	private static final Logger log = Logger.getLogger(PropertiesUtils.class.getName());

	/**
	 * Load property file if exists. Keep contents in memory.
	 */
	public static Properties getProperties(String propFileName) {
		
		Properties config = map_properties_open.get(propFileName);
		
		if (config!=null)
			return config;
				
		Properties prop = new Properties();
		InputStreamReader reader = null;
		try {
			
			// Treats as external file
			File external_file = new File(propFileName);
			if (external_file.exists() && external_file.isFile()) {
				try {
					reader = new FileReader(external_file);
					try {
						prop.load(reader);
					}
					finally {
						reader.close();
					}
				}
				catch (IOException e) {
					log.log(Level.FINE, "Error loading properties file "+propFileName, e);
					log.log(Level.FINE, "Keeping default internal settings");
				}
				config = prop;
				map_properties_open.put(propFileName, config);
				return config;
			}
			
			// Treats as internal resource
			try {
				URL url_resource = Thread.currentThread().getContextClassLoader().getResource(propFileName);
				if (url_resource==null) {
					log.log(Level.FINE, "Could not find properties file "+propFileName);
					log.log(Level.FINE, "Keeping default internal settings");
					config = prop;
					map_properties_open.put(propFileName, config);
					return config;
				}
				reader = new InputStreamReader(url_resource.openStream());
				prop.load(reader);
			}
			catch (IOException e) {
				log.log(Level.FINE, "Error loading properties file "+propFileName, e);
				log.log(Level.FINE, "Keeping default internal settings");
			}
			config = prop;
			map_properties_open.put(propFileName, config);
			return config;
		}
		finally {
			if (reader!=null) {
				try { reader.close(); } catch (Throwable e){ }
			}
		}
	}

}

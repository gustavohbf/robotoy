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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Some utility methods used for local data storage.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class SimpleLocalStorage {

	public static final String DEFAULT_STORAGE_DIR = "/var/robotoy";
	
	private String storageDir = DEFAULT_STORAGE_DIR;

	public String getStorageDir() {
		return storageDir;
	}

	public void setStorageDir(String storageDir) {
		this.storageDir = storageDir;
	}
	
	/**
	 * Read from file data contents in JSON format
	 */
	public <T> T loadData(String fileName,Class<T> dataType) throws IOException {
		File dir = new File(storageDir);
		if (!dir.exists())
			return null;
		File file = new File(dir,fileName);
		try(InputStream input=new FileInputStream(file);) {
			String json = IOUtils.readFileContents(input);
			return JSONUtils.fromJSON(json, dataType);
		}
	}
	
	/**
	 * Write to file data contents in JSON format
	 */
	public void saveData(String fileName,Object data) throws IOException {
		File dir = new File(storageDir);
		if (!dir.exists())
			dir.mkdirs();
		File file = new File(dir,fileName);
		String json = JSONUtils.toJSON(data, /*printPretty*/true);
		try (PrintStream output = new PrintStream(new BufferedOutputStream(new FileOutputStream(file,/*append*/false)));) {
			output.print(json);
		}
	}
}

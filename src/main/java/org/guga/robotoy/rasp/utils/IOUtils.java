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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

/**
 * Some utility methods used with file reading and writing.
 *  
 * @author Gustavo Figueiredo
 *
 */
public class IOUtils {
	
	private static final Pattern pStreamClosed = Pattern.compile("Stream closed",Pattern.CASE_INSENSITIVE);

	public static class BOM {
		public String encoding;

		public int offset;

		public BOM(String enc, int off) {
			encoding = enc;
			offset = off;
		}
	}

	/**
	 * Analisa o 'Byte Order Mask' e retorna o encoding relacionado
	 */
	public static BOM getBOM(final byte[] bom) {
		if (bom.length >= 4 && (bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF))
			return new BOM("UTF-32BE", 4);
		else if (bom.length >= 4 && (bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00))
			return new BOM("UTF-32LE", 4);
		else if (bom.length >= 3 && (bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF))
			return new BOM("UTF-8", 3);
		else if (bom.length >= 2 && (bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF))
			return new BOM("UTF-16BE", 2);
		else if (bom.length >= 2 && (bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE))
			return new BOM("UTF-16LE", 2);
		else
			// Unicode BOM mark not found
			return null;
	}


	/**
	 * Reads file contents
	 */
	public static String readFileContents(final InputStream input) {
		
		try {
			BufferedInputStream bin = new BufferedInputStream(input);
			StringBuffer sb = new StringBuffer();
			boolean first = true;
			String encoding = null;
			while (bin.available() > 0) {
				byte[] buffer = new byte[bin.available()];
				bin.read(buffer);
				if (first) {
					first = false;
					BOM bom = getBOM(buffer);
					if (bom != null) {
						sb.append(new String(buffer, bom.offset, buffer.length - bom.offset, bom.encoding));
						encoding = bom.encoding;
					} else {
						sb.append(new String(buffer));
					}
				} else {
					if (encoding == null) {
						sb.append(new String(buffer));
					} else {
						sb.append(new String(buffer, encoding));
					}
				}
			}
			return sb.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads file contents binary
	 */
	public static byte[] readFileContentsBinary(final InputStream input) {
		
		try {
			BufferedInputStream bin = new BufferedInputStream(input);
			ByteArrayOutputStream temp = new ByteArrayOutputStream();
			while (bin.available() > 0) {
				byte[] buffer = new byte[bin.available()];
				bin.read(buffer);
				temp.write(buffer);
			}
			return temp.toByteArray();
		} catch (IOException e) {
			if (e.getMessage()!=null 
					&& pStreamClosed.matcher(e.getMessage()).find())
				return null;
			throw new RuntimeException(e);
		}
	}

	
	/**
	 * Returns the last modified timestamp of a internal resource.
	 */
	public static long getResourceLastModified(Class<?> clas,String resourceName) {
		URL url = clas.getResource(resourceName);
		if (url==null)
			return 0L;
		URLConnection conn;
		try {
			conn = url.openConnection();
		} catch (IOException e) {
			return 0L;
		}
		try {
			if (conn instanceof JarURLConnection)
				return ((JarURLConnection)conn).getJarEntry().getTime();
			else
				return conn.getLastModified();
		}
		catch (IOException e) {
			return 0L;
		}
	}
}

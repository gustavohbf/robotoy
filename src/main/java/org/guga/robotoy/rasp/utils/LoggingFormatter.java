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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Simple formatter implementation to be used with java logging framework.<BR>
 * @author Gustavo Figueiredo
 */
public class LoggingFormatter extends Formatter {

	public static final ThreadLocal<SimpleDateFormat> sdfts = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		}
	};

	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		sb.append(sdfts.get().format(new Date()));
		sb.append(":");
		sb.append(record.getLevel().getName());
		sb.append(":");
		String scn = record.getSourceClassName();
		if (scn!=null && scn.length()>0) {
			int last_dot = scn.lastIndexOf('.');
			if (last_dot<0)
				sb.append(scn);
			else
				sb.append(scn.substring(last_dot+1));
		}
		sb.append(":");
		sb.append(record.getSourceMethodName());
		sb.append(": ");
		sb.append(record.getMessage());
		sb.append("\n");
		Throwable e = record.getThrown();
		if (e!=null) {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream temp = new PrintStream(buffer);
			e.printStackTrace(temp);
			sb.append(new String(buffer.toByteArray()));
			sb.append("\b");
		}
		return sb.toString();
	}

}

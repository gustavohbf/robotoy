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
package org.guga.robotoy.rasp.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Custom tag used in different pages.<BR>
 * Prints an alert message if one is available.<BR>
 * <BR>
 * Printed message will use 'alert' style class.<BR>
 * <BR>
 * Parameters:<BR>
 * ===========<BR>
 * property: name of page-scoped variable to get alert messages, if any<BR>
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
public class AlertTag extends RoboToyCommonTag {

	private String property;
	
	private String defaultValue;

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public void doTag() throws JspException, IOException {
		if (property==null)
			return;
		String message = (String)getPageContext().getAttribute(property);
		if (message!=null && message.trim().length()>0) {
			getJspContext().getOut().println("<p class=alert><b>"+StringEscapeUtils.escapeHtml(message).replaceAll("\r?\n", "<BR>\r\n")+"</b></p>");
		}
		else if (defaultValue!=null && defaultValue.trim().length()>0) {
			getJspContext().getOut().println("<p class=alert><b>"+StringEscapeUtils.escapeHtml(defaultValue).replaceAll("\r?\n", "<BR>\r\n")+"</b></p>");
		}
	}
}

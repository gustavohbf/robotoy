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

import javax.servlet.http.HttpServletRequest;

/**
 * Some utility methods for dealing with URL syntax 
 * 
 * @author Gustavo Figueiredo
 *
 */
public class URLUtils {

	/**
	 * Retrieves a full URL taking all its parts from the given
	 * HttpServletRequest object and optionally replacing some
	 * of its parts with any of the other parameters.
	 * @param req Request to get URI from. Must be provided.
	 * @param replace_scheme Replace scheme (e.g. 'http'). Optional. May be NULL if you don't want to replace it.
	 * @param replace_server Replace host (e.g. 'hostname.com'). Optional. May be NULL if you don't want to replace it.
	 * @param replace_port Replace port number (e.g. '80'). Optional. May be 0 if you don't want to replace it.
	 * @param replace_context Replace context name (e.g. '/mywebapp'). Optional. May be NULL if you don't want to replace it.
	 * @param replace_servlet Replace servlet mapped name (e.g. '/servlet/MyServlet'). Optional. May be NULL if you don't want to replace it.
	 * @param replace_path Replace aditional path information (e.g. '/a/b'). Optional. May be NULL if you don't want to replace it.
	 * @param replace_query Replace query string (e.g. 'd=789'). Optional. May be NULL if you don't want to replace it.
	 */
	public static String getURL(HttpServletRequest req,
			String replace_scheme,
			String replace_server,
			int replace_port,
			String replace_context,
			String replace_servlet,
			String replace_path,
			String replace_query) {

	    final String scheme = (replace_scheme!=null) ? replace_scheme : req.getScheme();             	// http
	    final String serverName = (replace_server!=null) ? replace_server : req.getServerName();     	// hostname.com
	    final int serverPort = (replace_port!=0) ? replace_port : req.getServerPort();        			// 80
	    final String contextPath = (replace_context!=null) ? replace_context : req.getContextPath();  	// /mywebapp
	    final String servletPath = (replace_servlet!=null) ? replace_servlet : req.getServletPath();  	// /servlet/MyServlet
	    final String pathInfo = (replace_path!=null) ? replace_path : req.getPathInfo();         		// /a/b;c=123
	    final String queryString = (replace_query!=null) ? replace_query : req.getQueryString();      	// d=789

	    // Reconstruct original requesting URL
	    StringBuilder url = new StringBuilder();
	    url.append(scheme).append("://").append(serverName);

	    if (serverPort != 80 && serverPort != 443) {
	        url.append(":").append(serverPort);
	    }

	    url.append(contextPath).append(servletPath);

	    if (pathInfo != null) {
	        url.append(pathInfo);
	    }
	    if (queryString != null) {
	        url.append("?").append(queryString);
	    }
	    return url.toString();
	}
}

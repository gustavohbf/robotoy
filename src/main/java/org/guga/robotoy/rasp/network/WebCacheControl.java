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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Some functions that will work with cache-control headers in HTTP requisitions.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class WebCacheControl {

	/**
	 * Set response to 'no cache'
	 */
	public static void setNoCache(final HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");	// HTTP 1.1.
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		response.setHeader("Expires", "0"); // Proxies.
	}

	/**
	 * Set response to 'public cache' that will expire some time in the future
	 */
	public static void setPublicCache(final HttpServletResponse response,long delay,TimeUnit unit) {
		if (delay>0) {
			final long delay_in_seconds = TimeUnit.SECONDS.convert(delay, unit);
			final long delay_in_milliseconds = TimeUnit.MILLISECONDS.convert(delay, unit);
			response.setHeader("Cache-Control", "public, max-age="+delay_in_seconds);
			response.setDateHeader("Expires", System.currentTimeMillis() + delay_in_milliseconds);
		}
		else {
			response.setHeader("Cache-Control", "public");
		}
	}

	/**
	 * Set response to 'public cache' that will condition on time-based conditional.
	 */
	public static void setPublicCache(final HttpServletResponse response,long delay,TimeUnit unit,long resource_timestamp) {
		setPublicCache(response,delay,unit);
		if (resource_timestamp>0) {
			response.setDateHeader("Last-Modified", resource_timestamp);
		}
	}
	
	/**
	 * Check if the desired resource is up-to-date according to information passed in request header.
	 * @param request Request object
	 * @param resource_timestamp Current timestamp for the requested resource
	 * @return Returns TRUE if it's up-to-date. Returns FALSE otherwise.
	 */
	public static boolean checkCacheTimeBased(final HttpServletRequest request,long resource_timestamp) {
        String ifNoneMatch = request.getHeader("If-None-Match");
		long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        return (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > resource_timestamp);
	}
	
	/**
	 * Reply to a request that has an up-to-date cached resource
	 */
	public static void replyCacheHit(final HttpServletResponse response, final String fileName, final long lastModified) throws IOException {
		final String eTag = fileName + "_" + lastModified;
        response.setHeader("ETag", eTag); // Required in 304.
        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
	}
}

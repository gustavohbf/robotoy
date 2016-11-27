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
package org.guga.robotoy.rasp.game;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Current data about a player.
 *  
 * @author Gustavo Figueiredo
 *
 */
public class GamePlayer {

	private InetAddress address;
	
	/**
	 * Websocket active port number
	 */
	private int port;
	
	/**
	 * HTTP session ID
	 */
	private String httpSession;
	
	private String name;
	
	private boolean online;
	
	private boolean dismissedSummary;
	
	private boolean resourcesLoaded;
	
	private long ping;
		
	public GamePlayer() { }
	
	public GamePlayer(String name,String address,int port) {
		this.name = name;
		this.port = port;
		if (address!=null) {
			try {
				this.address = InetAddress.getByName(address);
			} catch (UnknownHostException e) {	}
		}
	}

	public InetAddress getAddress() {
		return address;
	}
	
	public String getAddressString() {
		if (address==null)
			return null;
		else
			return address.getHostAddress();
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * Websocket active port number
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Websocket active port number
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * HTTP session ID
	 */
	public String getHttpSession() {
		return httpSession;
	}

	/**
	 * HTTP session ID
	 */
	public void setHttpSession(String httpSession) {
		this.httpSession = httpSession;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
		
	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public boolean isDismissedSummary() {
		return dismissedSummary;
	}

	public void setDismissedSummary(boolean dismissedSummary) {
		this.dismissedSummary = dismissedSummary;
	}

	public boolean isResourcesLoaded() {
		return resourcesLoaded;
	}

	public void setResourcesLoaded(boolean resourcesLoaded) {
		this.resourcesLoaded = resourcesLoaded;
	}

	public long getPing() {
		return ping;
	}

	public void setPing(long ping) {
		this.ping = ping;
	}

	public String toString() {
		return name;
	}
}

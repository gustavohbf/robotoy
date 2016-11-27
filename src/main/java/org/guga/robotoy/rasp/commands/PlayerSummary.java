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
package org.guga.robotoy.rasp.commands;

import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Player summary for exchanging between robots and players through messages.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class PlayerSummary {
	private String name;
	private String address;
	private int port;
	private boolean online;
	
	public static PlayerSummary fromPlayer(GamePlayer player) {
		PlayerSummary summary = new PlayerSummary();
		summary.setName(player.getName());
		summary.setAddress(player.getAddressString());
		summary.setPort(player.getPort());
		summary.setOnline(player.isOnline());
		return summary;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public boolean isOnline() {
		return online;
	}
	public void setOnline(boolean online) {
		this.online = online;
	}
	public String toString() {
		return "\""+name+"\":"+address;
	}
	public static String getPlayerInfo(GamePlayer player) {
		StringBuilder response = new StringBuilder();
		response.append("{\"name\":");
		response.append(JSONUtils.quote(player.getName()));
		response.append(",\"address\":\"");
		response.append(player.getAddress().getHostAddress());
		response.append("\",\"ping\":");
		response.append(player.getPing());
		response.append(",\"online\":");
		response.append(player.isOnline());
		if (player.getPort()>0) {
			response.append(",\"port\":");
			response.append(player.getPort());
		}
		response.append("}");		
		return response.toString();
	}
	public String getPlayerInfo() {
		StringBuilder response = new StringBuilder();
		response.append("{\"name\":");
		response.append(JSONUtils.quote(getName()));
		response.append(",\"address\":\"");
		response.append(getAddress());
		response.append("\",\"online\":");
		response.append(isOnline());
		if (getPort()>0) {
			response.append(",\"port\":");
			response.append(getPort());
		}
		response.append("}");		
		return response.toString();
	}
}
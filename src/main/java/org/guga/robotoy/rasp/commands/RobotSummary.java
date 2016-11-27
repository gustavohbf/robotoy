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

import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Robot summary for exchanging between robots and players through messages.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class RobotSummary {
	private String address;		
	private String owner;
	private String id;
	private int life;
	private String color;
	
	public static RobotSummary fromRobot(GameRobot robot) {
		RobotSummary summary = new RobotSummary();
		if (robot.getAddress()!=null)
			summary.setAddress(robot.getAddress().getHostAddress());
		summary.setId(robot.getIdentifier());
		if (robot.getOwner()!=null)
			summary.setOwner(robot.getOwner().getName());
		summary.setLife(robot.getLife());
		if (robot.getColor()!=null)
			summary.setColor(robot.getColor().getName());
		return summary;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getLife() {
		return life;
	}
	public void setLife(int life) {
		this.life = life;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}

	public String toString() {
		return id;
	}
	
	public static String getRobotInfo(GameRobot robot) {
		return getRobotInfo(robot,/*localAddress*/null);
	}
	
	public static String getRobotInfo(GameRobot robot,String localAddress) {
		StringBuilder response = new StringBuilder();

		response.append("{");
		
		String address = (robot.getAddress()!=null) ? robot.getAddress().getHostAddress() : localAddress;
		
		if (address!=null) {
			response.append("\"address\":\"");
			response.append(address);
			response.append("\"");
		}

		if (robot.getIdentifier()!=null) {
			if (response.length()>1)
				response.append(",");
			response.append("\"id\":\"");
			response.append(robot.getIdentifier());
			response.append("\"");
		}

		if (robot.getOwner()!=null) {
			if (response.length()>1)
				response.append(",");
			response.append("\"owner\":");
			response.append(JSONUtils.quote(robot.getOwner().getName()));
		}
		
		if (response.length()>1)
			response.append(",");
		response.append("\"life\":");
		response.append(robot.getLife());
		
		if (robot.getColor()!=null) {
			response.append(",\"color\":");
			response.append(JSONUtils.quote(robot.getColor().getName()));			
		}
		
		response.append("}");

		return response.toString();
	}

}
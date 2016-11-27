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

import org.guga.robotoy.rasp.game.GameControlMode;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameState;

/**
 * Custom tag used in different pages.<BR>
 * Set a page context attribute variable with the control mode
 * chosen for the robot that current user controls.<BR>
 * If the request was made with a parameter of the same name,
 * its value takes precedence over internal game choice.<BR>
 *  
 * @author Gustavo Figueiredo
 *
 */
public class ControlModeTag extends RoboToyCommonTag {

	private String property = "controlmode";

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	@Override
	public void doTag() throws JspException, IOException {
		if (property==null)
			return;
		
		GameControlMode mode = null;
		String param = getRequest().getParameter(property);
		if (param!=null) {
			mode = GameControlMode.parse(param);
		}
		
		if (mode==null) {
			GameState game = assertGame();
			String name = (String)getSession().getAttribute("USERNAME");
			GameRobot robot = (name==null) ? null : game.findRobotWithOwnerName(name);
			mode = (robot==null) ? null : robot.getControlMode();
		}
		
		getPageContext().setAttribute(property, (mode==null)?null:mode.getMode());
	}
}

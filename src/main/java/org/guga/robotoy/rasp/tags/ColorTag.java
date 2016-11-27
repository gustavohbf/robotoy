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

import org.guga.robotoy.rasp.game.GameRobot;

/**
 * Custom tag used in different pages.<BR>
 * Outputs the selected robot color for the player that owns that robot.<BR>
 * The color is output in hex format.
 *  
 * @author Gustavo Figueiredo
 *
 */
public class ColorTag extends RoboToyCommonTag {

	@Override
	public void doTag() throws JspException, IOException {
		String color = "";
		String name = (String)getSession().getAttribute("USERNAME");
		if (name!=null) {
			GameRobot robot = assertGame().findRobotWithOwnerName(name);
			if (robot!=null
				&& robot.getColor()!=null) {
				color = robot.getColor().getName();
			}
		}
		getJspContext().getOut().print(color);
	}

}

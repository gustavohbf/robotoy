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

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.game.GamePlayMode;

/**
 * Custom tag used in admin pages.<BR>
 * Set a page context attribute variable with the play mode name ('STANDALONE' or 'MULTIPLAYER').<BR>
 *  
 * @author Gustavo Figueiredo
 *
 */
public class PlayModeTag extends RoboToyCommonTag {

	private String property = "playmode";

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
		
		RoboToyServerContext context = assertController().getContext();
		if (context==null)
			return;
		
		GamePlayMode mode = context.getGamePlayMode();
		if (mode==null)
			return;
		
		getPageContext().setAttribute(property, mode.name());
	}

}

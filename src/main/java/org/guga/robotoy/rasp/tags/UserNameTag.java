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

/**
 * Custom tag used in different pages.<BR>
 * Outputs the name of the logged user.
 *  
 * @author Gustavo Figueiredo
 *
 */
public class UserNameTag extends RoboToyCommonTag {

	@Override
	public void doTag() throws JspException, IOException {
		String name = (String)getSession().getAttribute("USERNAME");
		if (name==null)
			name = "";
		getJspContext().getOut().print(name);
	}

}

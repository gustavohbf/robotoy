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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GameState;

/**
 * Base classe for application custom tags
 * @author Gustavo Figueiredo
 */
public abstract class RoboToyCommonTag extends SimpleTagSupport {

	private static final String ERROR_MISSING_GAME_STATE = "Game status was not properly configured by web application!";

	private static final String ERROR_MISSING_CONTROLLER = "Game controller was not properly configured by web application!";

	protected GameState getGame() {
		PageContext context = (PageContext)this.getJspContext();
		GameState game = (GameState)context.getServletContext().getAttribute("game");
		return game;
	}

	protected RoboToyServerController getController() {
		PageContext context = (PageContext)this.getJspContext();
		RoboToyServerController controller = (RoboToyServerController)context.getServletContext().getAttribute("controller");
		return controller;
	}

	protected PageContext getPageContext() {
		PageContext context = (PageContext)this.getJspContext();
		return context;
	}

	protected HttpServletRequest getRequest() {
		PageContext context = (PageContext)this.getJspContext();
		return (HttpServletRequest)context.getRequest();
	}

	protected HttpServletResponse getResponse() {
		PageContext context = (PageContext)this.getJspContext();
		return (HttpServletResponse)context.getResponse();
	}
	
	protected HttpSession getSession() {
		PageContext context = (PageContext)this.getJspContext();
		return context.getSession();
	}
	
	/**
	 * Forwards to another relative page. User browser will still be pointing to the same URL as before.
	 */
	protected void forward(String uri) throws JspException, IOException, SkipPageException {
		PageContext context = (PageContext)this.getJspContext();
		try {
			context.forward(uri);
		} catch (ServletException e) {
			throw new JspException("Error in "+getClass().getSimpleName()+" while forwarding to "+uri,e);
		}
		throw new SkipPageException();
	}

	/**
	 * Redirects to another page, maybe in another server. User browser will point to the new URL.
	 */
	protected void redirect(String url) throws JspException, IOException, SkipPageException {
		getResponse().sendRedirect(url);
		throw new SkipPageException();
	}

	protected GameState assertGame() throws IOException, SkipPageException {
		GameState game = getGame();
		if (game==null) {
			HttpServletResponse response = getResponse();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_MISSING_GAME_STATE);
			throw new SkipPageException();
		}
		else {
			return game;
		}
	}
	
	protected RoboToyServerController assertController() throws IOException, SkipPageException {
		RoboToyServerController controller = getController();
		if (controller==null) {
			HttpServletResponse response = getResponse();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_MISSING_CONTROLLER);
			throw new SkipPageException();
		}
		else {
			return controller;
		}
	}
}

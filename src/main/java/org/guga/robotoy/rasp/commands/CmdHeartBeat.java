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

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * Message sent by one robot to other robots keeping its session alive.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdHeartBeat implements CommandWithBroadcast<Boolean> {

	@Override
	public String getBroadcastMessage(RoboToyServerContext context,Boolean object) {
		return String.valueOf(RoboToyServerController.HEARTBEAT);
	}

	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		// Since it's just a heartbeat, we don't have to parse anything
		// It should be enough for keeping connection alive
		return false;
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		return Boolean.TRUE;
	}

}

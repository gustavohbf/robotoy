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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.guga.robotoy.rasp.controller.RoboToyServerContext;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * Queries for current ranking positions.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CmdQueryRanking implements Command {

	public static String run(RoboToyServerContext context) {
		
		List<GameRobot> robots = context.getGame().getRobots();
		List<RankingEntry> ranking = new ArrayList<>(robots.size());
		for (GameRobot robot:robots) {
			RankingEntry e = new RankingEntry();
			if (robot.getOwner()!=null)
				e.setPlayer(robot.getOwner().getName());
			if (robot.getColor()!=null)
				e.setColor(robot.getColor().getName());
			e.setKills(robot.getKills());
			e.setLife(robot.getLife());
			ranking.add(e);
		}
		
		Collections.sort(ranking,new Comparator<RankingEntry>() {
			@Override
			public int compare(RankingEntry e1, RankingEntry e2) {
				if (e1.getKills()>e2.getKills())
					return -1;
				if (e1.getKills()<e2.getKills())
					return +1;
				if (e1.getLife()>e2.getLife())
					return -1;
				if (e1.getLife()<e2.getLife())
					return +1;
				return 0;
			}
		});
		
		StringBuilder response = new StringBuilder();
		response.append("{\"ranking\":[");
		for (int i=0;i<ranking.size();i++) {
			if (i>0)
				response.append(",");
			response.append(JSONUtils.toJSON(ranking.get(i), false));
		}
		response.append("]}");
		return response.toString();
	}
	
	@Override
	public boolean isParseable(CommandIssuer issuer,String message) {
		return message.length()>0 && message.charAt(0)==RoboToyServerController.QUERY_RANKING;
	}

	@Override
	public Object parseMessage(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session) throws Exception {
		return null; // nothing to execute locally
	}
	
	@Override
	public String getReply(CommandIssuer issuer,RoboToyServerContext context,String message,WebSocketActiveSession session,Object parsedMessage) {
		return CmdQueryRanking.run(context);
	}

	public static class RankingEntry {
		private String player;
		private String color;
		private int life;
		private int kills;
		public String getPlayer() {
			return player;
		}
		public void setPlayer(String player) {
			this.player = player;
		}
		public String getColor() {
			return color;
		}
		public void setColor(String color) {
			this.color = color;
		}
		public int getLife() {
			return life;
		}
		public void setLife(int life) {
			this.life = life;
		}
		public int getKills() {
			return kills;
		}
		public void setKills(int kills) {
			this.kills = kills;
		}		
	}
}

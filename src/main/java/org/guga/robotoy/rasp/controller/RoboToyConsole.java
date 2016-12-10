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
package org.guga.robotoy.rasp.controller;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import org.guga.robotoy.rasp.commands.Command;
import org.guga.robotoy.rasp.commands.CommandIssuer;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.network.Server;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;

/**
 * Interactive console used for debugging and testing the current application
 * while it's running.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class RoboToyConsole {

	private final Server server;
	private final RoboToyServerController controller;
	private final InputStream input;
	private final PrintStream output;
	
	private CommandIssuer issuer = CommandIssuer.PLAYER;
	private String playerName;
	
	public RoboToyConsole(Server server,RoboToyServerController controller) {
		this.server = server;
		this.controller = controller;
		this.input = System.in;
		this.output = System.out;
		this.playerName = controller.getAdminUserName();
	}
	
	public void start() {
		Thread console_thread = new Thread(this::consoleThread);
		console_thread.setName("RoboToyConsole");
		console_thread.setDaemon(true);
		console_thread.start();
	}
	
	private void consoleThread() {
		
		// ASCii Art rendered with http://patorjk.com/software/taag/#p=display&f=Big&t=RoboToy
		// Font: Big
		// Width: Default
		// Height: Default
		output.println(
"  _____       _        _______          \n"+
" |  __ \\     | |      |__   __|         \n"+
" | |__) |___ | |__   ___ | | ___  _   _ \n"+
" |  _  // _ \\| '_ \\ / _ \\| |/ _ \\| | | |\n"+
" | | \\ \\ (_) | |_) | (_) | | (_) | |_| |\n"+
" |_|  \\_\\___/|_.__/ \\___/|_|\\___/ \\__, |\n"+
"                                   __/ |\n"+
"                                  |___/ \n");
		
		onGetIssuer();
		output.println("Type 'h' or 'help' or '?' for instructions!");
		
		try (Scanner scanner = new Scanner(input);) {
			while (true) {
				String line = scanner.nextLine();
				if (line.equals("h") || line.equals("help") || line.equals("?"))
					onHelp();
				else if (line.equals("set robot"))
					onSetRobotIssuer();
				else if (line.startsWith("set player"))
					onSetPlayerIssuer(line.substring("set player".length()));
				else if (line.equals("who"))
					onGetIssuer();
				else if (line.equals("stage"))
					onGetStage();
				else if (line.equals("q") || line.equals("quit") || line.equals("exit"))
					server.stopServer();
				else if (!parseCommand(line))
					output.println("Invalid command! Type 'h' or 'help' or '?' for instructions.");
			}
		}
	}
	
	private void onHelp() {
		output.println("h or help or ? - Returns these instructions");
		output.println("q or quit or exit - Stop running this program (both server and console)");
		output.println("who - Return the current console command issuer type (ROBOT or PLAYER) and name  (if it's a PLAYER)");
		output.println("set robot - Make the console command issuer behave like a robot");
		output.println("set player <name> - Make the console command issuer behave like a player with the given name");
		output.println("stage - Get current game stage");
		for (Command cmd:RoboToyServerController.commands) {
			String help = cmd.getHelp();
			if (help!=null)
				output.println(help);
		}		
	}
	
	private void onGetStage() {
		output.println("Current game stage: "+controller.getContext().getGame().getStage().name());
	}
	
	private void onGetIssuer() {
		if (CommandIssuer.PLAYER.equals(issuer)) {
			output.println("Current issuer: "+issuer.name()+" "+playerName);
		}
		else {
			output.println("Current issuer: "+issuer.name());
		}
	}
	
	private void onSetRobotIssuer() {
		issuer = CommandIssuer.ROBOT;
		onGetIssuer();
	}
	
	private void onSetPlayerIssuer(String playerName) {
		issuer = CommandIssuer.PLAYER;
		if (playerName==null || playerName.trim().length()==0) {
			// preserve current name
		}
		else {
			this.playerName = playerName.trim();
		}
		onGetIssuer();
	}
	
	private boolean parseCommand(String line) {
		for (Command cmd:RoboToyServerController.commands) {			
			if (cmd.isParseable(issuer,line)) {
				WebSocketActiveSession session = mockSession(issuer);
				String response = controller.onCommand(line, session);
				if (response!=null) {
					output.println(response);
				}
				return true;
			}
		}
		return false;
	}
	
	private WebSocketActiveSession mockSession(final CommandIssuer issuer) {
		
		final AtomicReference<String> ref_path = new AtomicReference<>();
		switch (issuer) {
			case PLAYER:
				ref_path.set(RoboToyServerContext.getWSPathWithPlayerName(playerName));
				break;
			case ROBOT: {
				GameRobot local_robot = controller.getContext().getGame().findLocalRobot();
				if (local_robot!=null)
					ref_path.set(RoboToyServerContext.getWSPathWithRobotIdentifier(local_robot.getIdentifier()));
				break;
			}
		};
			
		return new WebSocketActiveSession() {
			@Override
			public String getHost() {
				InetAddress addr = getLocalAddress();
				if (addr!=null)
					return addr.getHostAddress();
				else
					return "127.0.0.1";
			}
			@Override
			public InetAddress getRemoteAddress() {
				return getLocalAddress();
			}
			@Override
			public int getRemotePort() {
				return 0;
			}
			@Override
			public InetAddress getLocalAddress() {
				try {
					return InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					return null;
				}
			}
			@Override
			public int getLocalPort() {
				return 0;
			}
			@Override
			public String getPath() {
				return ref_path.get();
			}
			@Override
			public void setPath(String path) {
				ref_path.set(path);
			}
			@Override
			public String getSessionId() {
				return null;
			}
			@Override
			public boolean isStartedHere() {
				return true;
			}
			@Override
			public void close() {
				// Nothing to do
			}			
		};
	}
}

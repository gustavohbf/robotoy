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
package org.guga.robotoy.rasp.admin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpSession;

import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GamePlayMode;
import org.guga.robotoy.rasp.game.GameStart;
import org.guga.robotoy.rasp.game.GameState;
import org.guga.robotoy.rasp.network.RoboToyAccessPoint;
import org.guga.robotoy.rasp.network.Server;
import org.guga.robotoy.rasp.network.WebServer;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.optics.LedColor;
import org.guga.robotoy.rasp.statistics.RoboToyStatistics;
import org.guga.robotoy.rasp.utils.IOUtils;
import org.guga.robotoy.rasp.utils.InetUtils;
import org.guga.robotoy.rasp.utils.JSONUtils;

/**
 * RESTful interface for exposing some state. Usefull for debugging.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DebugWebInterface implements WebServer.CustomRESTfulService {
	
	private static final Logger log = Logger.getLogger(DebugWebInterface.class.getName());

	private static final String DEFAULT_ROBOTOY_LOG_OUT_FILE = "/tmp/robotoy.out";
	private static final String DEFAULT_ROBOTOY_LOG_ERR_FILE = "/tmp/robotoy.err";
	private final GameState game;
	private final RoboToyServerController controller;
	private final Server server;
	
	public DebugWebInterface(RoboToyServerController controller,Server server) {
		this.controller = controller;
		this.game = controller.getContext().getGame();
		this.server = server;
	}

	/**
	 * Responds to HTTP requests in admin RESTful interface.
	 */
	@Override
	public Object getResponse(String method, String uri, String requestContents,String remoteAddress,HttpSession session) throws Exception {
		
		// Denies access for non-admins
		if (controller.getAdminUserName()==null)
			return null; // no admin configured
		String userName = (session)!=null ? (String)session.getAttribute("USERNAME") : null;
		if (userName==null || !controller.getAdminUserName().equalsIgnoreCase(userName)) {
			return null; // user is not logged as admin
		}

		
		if ("get".equalsIgnoreCase(method)) {
			return doGet(uri,requestContents);
		}
		else if ("post".equalsIgnoreCase(method)) {
			return doPost(uri,requestContents,remoteAddress);
		}
		else if ("put".equalsIgnoreCase(method)) {
			return doPut(uri,requestContents);
		}
		return null;
	}

	/**
	 * Action on HTTP-GET method in admin RESTful interface.
	 */
	private Object doGet(String uri, String requestContents) throws Exception {
		if (uri.equals("/robots")) {
			return game.getRobots();
		}
		else if (uri.equals("/players")) {
			return game.getPlayers();
		}
		else if (uri.equals("/log")) {
			int last = 0;
			if (requestContents!=null && requestContents.length()>0) {
				Pattern p = Pattern.compile("\\blast=(\\d+)",Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(requestContents);
				if (m.find()) {
					last = Integer.parseInt(m.group(1));
				}
				else {
					throw new UnsupportedOperationException("Invalid argument: "+requestContents);
				}
			}
			return getLog(last);
		}
		else if (uri.equals("/stats")) {
			return RoboToyStatistics.getSummary();
		}
		else if (uri.equals("/wifi")) {
			if (requestContents==null || requestContents.length()==0)
				return InetUtils.scanWiFi(InetUtils.DEFAULT_WIFI_INTERFACE);
			else {
				Pattern p = Pattern.compile("\\bnet=([^, ]+)",Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(requestContents);
				if (m.find()) {
					String net = m.group(1);
					return InetUtils.scanWiFi(net);
				}
				else {
					throw new UnsupportedOperationException("Invalid argument: "+requestContents);
				}
			}
		}
		else if (uri.equals("/net")) {
			return InetUtils.getNetAdapters();
		}
		else if (uri.equals("/sockets")) {
			return getActiveSockets();
		}
		else {
			return null;
		}
	}

	/**
	 * Action on HTTP-POST method in admin RESTful interface.
	 */
	private Object doPost(String uri, String requestContents,String remoteAddress) throws Exception {
		if (uri.equals("/restart")) {
			GameStart.resetGame(game);
			return "Game status just restarted locally!";
		}
		else if (uri.equals("/reboot")) {
			reboot();
			return "Rebooting"; 
		}
		else if (uri.equals("/shutdown")) {
			shutdown();
			return "Shuting down"; 
		}
		else if (uri.equals("/error")) {
			postError(requestContents,remoteAddress);
			return "";
		}
		else if (uri.equals("/info")) {
			postInfo(requestContents,remoteAddress);
			return "";
		}
		else {
			return null;
		}
	}

	/**
	 * Action on HTTP-PUT method in admin RESTful interface.
	 */
	private Object doPut(String uri, String requestContents) throws Exception {
		if (uri.equals("/wifimode")) {
			if (requestContents.startsWith("{") && requestContents.endsWith("}")) {
				WiFiMode mode = JSONUtils.fromJSON(requestContents, WiFiMode.class);
				if (mode==null || mode.getMode()==null)
					throw new UnsupportedOperationException("Invalid argument: "+requestContents);
				RoboToyAccessPoint.becomeAccessPoint(controller, mode.getMode(), server);
				return "Turned into "+mode.getMode();
			}
			else {
				if (requestContents==null || requestContents.length()==0)
					throw new UnsupportedOperationException("Missing argument!");
				else
					throw new UnsupportedOperationException("Invalid argument: "+requestContents);
			}
		}
		else if (uri.equals("/playmode")) {
			if (requestContents.startsWith("{") && requestContents.endsWith("}")) {
				PlayMode mode = JSONUtils.fromJSON(requestContents, PlayMode.class);
				if (mode==null || mode.getMode()==null)
					throw new UnsupportedOperationException("Invalid argument: "+requestContents);
        		controller.getContext().setGamePlayMode(mode.getMode());
        		if (GamePlayMode.STANDALONE.equals(mode.getMode())) {
	        		// Once it started standalone mode, signal this situation flashing lights
	        		if (null!=controller.getContext().getRGBLed()) {
	        			// cycle through colors BLUE and GREEN
	        			controller.getContext().getRGBLed().startCycleColors(600, LedColor.GREEN, LedColor.BLUE);
	        		}
        		}
				return "Turned into "+mode.getMode();
			}
			else {
				if (requestContents==null || requestContents.length()==0)
					throw new UnsupportedOperationException("Missing argument!");
				else
					throw new UnsupportedOperationException("Invalid argument: "+requestContents);
			}			
		}
		else {
			return null;
		}
	}

	/***
	 * Return LOG file contents.
	 * @param last_lines If greater than zero, returns up to this amount of lines. If less or equal to zero,
	 * return everything.
	 */
	private String getLog(final int last_lines) {
		StringBuilder log = new StringBuilder();
		String[] files = {
			DEFAULT_ROBOTOY_LOG_OUT_FILE,
			DEFAULT_ROBOTOY_LOG_ERR_FILE
		};
		for (String logFile:files) {
			File file = new File(logFile);
			log.append(file.getAbsolutePath()+"  ===========================\n");
			if (!file.exists()) {
				log.append("-- file not found --\n");
			}
			else {
				if (last_lines>0) {
					try (RandomAccessFile input=new RandomAccessFile(file, "r");) {
						long size = input.length();
						long start = size;
						int countdown = last_lines + 1;
						while (countdown>0) {
							long eol = start-1;
							while (true) {
								input.seek(eol);
								int ch = input.read();
								if (ch=='\n')
									break;
								eol--;
								if (eol<0)
									break;
							}
							if (eol<0) {
								start = 0;
								break;
							}
							start = eol;
							if (start==0)
								break;
							countdown--;
						}
						input.seek(start);
						final int size_to_read = (int)Math.min(Integer.MAX_VALUE, (size-start) );
						byte[] temp = new byte[size_to_read];
						input.readFully(temp);
						log.append(new String(temp));
						log.append("\n");
					}
					catch(IOException e) {
						log.append("-- error while loading file ["+e.getClass().getName()+" "+e.getMessage()+"] --\n");
					}
				}
				else {
					try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
						String config = IOUtils.readFileContents(input);
						log.append(config+"\n");
					}
					catch(IOException e) {
						log.append("-- error while loading file ["+e.getClass().getName()+" "+e.getMessage()+"] --\n");
					}
				}
			}
			log.append("\n\n");
		}
		return log.toString();		
	}
	
	/**
	 * Get a list of current active sockets in this robot
	 */
	private List<ActiveSocket> getActiveSockets() {
		List<ActiveSocket> list = new LinkedList<>();
		for (WebSocketActiveSession session:controller.getContext().getWebSocketPool().getActiveSessions()) {
			ActiveSocket socket = new ActiveSocket();
			socket.setRemoteAddress(session.getHost());
			socket.setRemotePort(session.getRemotePort());
			socket.setLocalAddress(session.getLocalAddress().getHostAddress());
			socket.setLocalPort(session.getLocalPort());
			socket.setSession(session.getSessionId());
			socket.setPath(session.getPath());
			socket.setStartedHere(session.isStartedHere());
			list.add(socket);
		}
		return list;
	}
	
	/**
	 * Parse AJAX message generated by the use of 'error_handling.js' at client side 
	 */
	private void postError(String requestContents,String remoteAddress) {
		try {
			ErrorMessageData data = JSONUtils.fromJSON(requestContents, ErrorMessageData.class);
			if (data==null) {
				log.log(Level.SEVERE, "Unparseable contents for 'error' RESTful request: "+requestContents);				
			}
			else {
				log.log(Level.SEVERE, "Error at client side! "+data.getMessage()+"\n"
						+ "IP: "+remoteAddress+" Source: "+data.getSource()+" Line: "+data.getLineno()+" Context: "+data.getContext());
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Unparseable contents for 'error' RESTful request: "+requestContents, e);
		}
	}

	/**
	 * Parse AJAX message generated by the use of 'error_handling.js' at client side 
	 */
	private void postInfo(String requestContents,String remoteAddress) {
		try {
			InfoMessageData data = JSONUtils.fromJSON(requestContents, InfoMessageData.class);
			if (data==null) {
				log.log(Level.SEVERE, "Unparseable contents for 'info' RESTful request: "+requestContents);				
			}
			else {
				log.log(Level.INFO, "Info message at client side! "+data.getMessage()+"\n"
						+ "IP: "+remoteAddress+" Context: "+data.getContext());
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Unparseable contents for 'info' RESTful request: "+requestContents, e);
		}
	}

	/**
	 * Reboot robotoy right now
	 */
	private static void reboot() throws IOException {
		ProcessBuilder builder = new ProcessBuilder("reboot");
		builder.start();
		// do not wait
		System.exit(0);
	}
	
	/**
	 * Shutdown robotoy right now
	 */
	private static void shutdown() throws IOException {
		ProcessBuilder builder = new ProcessBuilder("shutdown","-h","now");
		builder.start();
		// do not wait		
		System.exit(0);
	}

	/**
	 * This class wraps JSON request from admin RESTful interface used for changing
	 * the desired WiFi mode as Access Point.
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class WiFiMode {
		private InetUtils.WiFiModeEnum mode;
		public InetUtils.WiFiModeEnum getMode() {
			return mode;
		}
		public void setMode(InetUtils.WiFiModeEnum mode) {
			this.mode = mode;
		}
	}
	
	/**
	 * This class wraps JSON request from admin RESTful interface used for changing
	 * the desired play mode.
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class PlayMode {
		private GamePlayMode mode;
		public GamePlayMode getMode() {
			return mode;
		}
		public void setMode(GamePlayMode mode) {
			this.mode = mode;
		}		
	}
	
	/**
	 * This class wraps summary information about error captured
	 * in player's browser and transmitted here for debugging purpose.
	 * 
	 * @author Gustavo Figueiredo
	 */
	public static class ErrorMessageData {
		private String context;
		private int lineno;
		private String source;
		private String message;
		public String getContext() {
			return context;
		}
		public void setContext(String context) {
			this.context = context;
		}
		public int getLineno() {
			return lineno;
		}
		public void setLineno(int lineno) {
			this.lineno = lineno;
		}
		public String getSource() {
			return source;
		}
		public void setSource(String source) {
			this.source = source;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}		
	}

	/**
	 * This class wraps summary information about logged information captured
	 * in player's console browser and transmitted here for debugging purpose.
	 * 
	 * @author Gustavo Figueiredo
	 */
	public static class InfoMessageData {
		private String context;
		private String message;
		public String getContext() {
			return context;
		}
		public void setContext(String context) {
			this.context = context;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}		
	}
	
	/**
	 * This class wraps summary information about active sockets.
	 * @author Gustavo Figueiredo
	 */
	public static class ActiveSocket {
		private String localAddress;
		private int localPort;
		private String remoteAddress;
		private int remotePort;
		private String session;
		private String path;
		private boolean startedHere;
		public String getLocalAddress() {
			return localAddress;
		}
		public void setLocalAddress(String localAddress) {
			this.localAddress = localAddress;
		}
		public int getLocalPort() {
			return localPort;
		}
		public void setLocalPort(int localPort) {
			this.localPort = localPort;
		}
		public String getRemoteAddress() {
			return remoteAddress;
		}
		public void setRemoteAddress(String remoteAddress) {
			this.remoteAddress = remoteAddress;
		}
		public int getRemotePort() {
			return remotePort;
		}
		public void setRemotePort(int remotePort) {
			this.remotePort = remotePort;
		}
		public String getSession() {
			return session;
		}
		public void setSession(String session) {
			this.session = session;
		}
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public boolean isStartedHere() {
			return startedHere;
		}
		public void setStartedHere(boolean startedHere) {
			this.startedHere = startedHere;
		}			
	}
}

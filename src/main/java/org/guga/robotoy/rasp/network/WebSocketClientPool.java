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
package org.guga.robotoy.rasp.network;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.WriteCallback;

/**
 * Implementation using web sockets for communication
 * between the robots.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class WebSocketClientPool {

	private static final Logger log = Logger.getLogger(WebSocketClientPool.class.getName());

	private final CommandCentral commandCentral;
	
	private final InclusionCallback inclusionCallback;
	
	private final RemovalCallback removalCallback;
	
	private final ConcurrentLinkedQueue<WebSocketHandlerImpl> activeSessions;
	
	public WebSocketClientPool(CommandCentral commandCentral,InclusionCallback inclusionCallback,RemovalCallback removalCallback) {
		this.commandCentral = commandCentral;
		this.inclusionCallback = inclusionCallback;
		this.removalCallback = removalCallback;
		this.activeSessions = new ConcurrentLinkedQueue<>();
	}
	
	public void connect(String address,int port_number,String path) throws Exception {
		String uri = "ws://"+address+":"+port_number;
		if (path!=null && path.length()>0) {
			if (!path.startsWith("/"))
				uri += "/";
			uri += path; 
		}
		WebSocketHandlerImpl clientEndPoint = new WebSocketHandlerImpl(this,new URI(uri));
		if (!hasSession(clientEndPoint.getSessionId()))
			activeSessions.add(clientEndPoint);
	}
	
	void addSession(WebSocketHandlerImpl handler) {
		if (!hasSession(handler.getSessionId()))
			activeSessions.add(handler);
	}
	
	void removeSession(WebSocketHandlerImpl handler) {
		activeSessions.remove(handler);
	}
	
	boolean hasSession(String sessionId) {
		if (sessionId==null)
			return false;
		for (WebSocketActiveSession handler:activeSessions) {
			if (sessionId.equals(handler.getSessionId()))
				return true;
		}
		return false;
	}

	public CommandCentral getCommandCentral() {
		return commandCentral;
	}

	public RemovalCallback getRemovalCallback() {
		return removalCallback;
	}

	public InclusionCallback getInclusionCallback() {
		return inclusionCallback;
	}

	public void sendMessage(String address, int port, String message, Runnable onSuccess, Consumer<Throwable> onFailure) throws Exception {
		WebSocketHandlerImpl active_session = getConnection(address, port);
		if (active_session==null)
			return;
		active_session.sendMessage(message,new WriteCallback() {			
			@Override
			public void writeSuccess() {
				onSuccess.run();
			}			
			@Override
			public void writeFailed(Throwable paramThrowable) {
				onFailure.accept(paramThrowable);
			}
		});
	}

	protected WebSocketHandlerImpl getConnection(String address,int port) {
		for (WebSocketHandlerImpl session:activeSessions) {
			if (port!=0 && port!=session.getRemotePort())
				continue;
			if (session.getHost().equals(address)) 
				return session;
		}
		return null;
	}
	
	public boolean hasActiveSession(InetAddress address) {
		String host_address = address.getHostAddress();
		for (WebSocketHandlerImpl session:activeSessions) {
			if (!host_address.equals(session.getHost()))
				continue;
			if (!session.isActive())
				continue;
			return true;
		}		
		return false;
	}

	public boolean hasActiveSessionWithPath(String path) {
		for (WebSocketHandlerImpl session:activeSessions) {
			if (session.getPath()!=path) {
				if (session.getPath()==null || path==null)
					continue;
				if (!session.getPath().equalsIgnoreCase(path))
					continue;
			}
			if (!session.isActive())
				continue;
			return true;
		}		
		return false;
	}

	/**
	 * Send a message to all web-sockets clients.
	 * @param excludePath If not NULL, avoid sending message to sessions with this requested path
	 */
	public void sendMessageAll(String message,String... excludePath) throws Exception {
		if (activeSessions.isEmpty())
			return;
		Set<String> avoid_duplicity = new HashSet<>();
		for (WebSocketHandlerImpl session:activeSessions) {
			if (excludePath!=null && excludePath.length>0) {
				boolean ignore_this = false;
				String session_path = session.getPath();
				if (session_path!=null) {
					for (String s:excludePath) {
						if (s==null)
							continue;
						if (s.equalsIgnoreCase(session_path)) {
							ignore_this = true;
							break;
						}
					}
				}
				if (ignore_this)
					continue;
			}
			String session_id = session.getSessionId();
			if (avoid_duplicity.contains(session_id))
				continue;
			avoid_duplicity.add(session_id);
			try {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,"Broadcast to "+session.getHost()+":"+session.getRemotePort()+" "+message);
				}
				session.sendMessage(message,null);
			}				
			catch (Throwable e) {
				log.log(Level.SEVERE,"Error sending response to "+session.getHost(),e);
			}

		}
	}

	public void sendMessageAll(String message, Set<String> filterPaths) throws Exception {
		if (activeSessions.isEmpty())
			return;
		Set<String> avoid_duplicity = new HashSet<>();
		for (WebSocketHandlerImpl session:activeSessions) {
			String path = session.getPath();
			if (filterPaths!=null) {
				if (path==null || !filterPaths.contains(path))
					continue;
			}
			String session_id = session.getSessionId();
			if (avoid_duplicity.contains(session_id))
				continue;
			avoid_duplicity.add(session_id);
			try {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,"Broadcast to "+session.getHost()+":"+session.getRemotePort()+" "+message);
				}
				session.sendMessage(message,null);
			}				
			catch (Throwable e) {
				log.log(Level.SEVERE,"Error sending response to "+session.getHost(),e);
			}

		}
	}

	/**
	 * Send a message to one connected client
	 */
	public void sendMessage(String address, int port, String message) throws Exception {
		WebSocketHandlerImpl active_session = getConnection(address, port);
		if (active_session==null)
			throw new Exception("Not connected to \""+address+"\"!");
		active_session.sendMessage(message,null);
	}
	
	/**
	 * Returns all current active sessions
	 */
	public Collection<WebSocketActiveSession> getActiveSessions() {
		return Collections.unmodifiableCollection(activeSessions);
	}
	
	public WebSocketActiveSession findSessionWithPath(String path) {
		for (WebSocketActiveSession session:activeSessions) {
			if (path==session.getPath())
				return session;
			if (path!=null && path.equals(session.getPath()))
				return session;
		}
		return null;
	}
}

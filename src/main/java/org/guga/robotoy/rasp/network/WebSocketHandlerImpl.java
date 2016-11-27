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
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

/**
 * Handler for WebSocket communications.
 * 
 * @author Gustavo Figueiredo
 *
 */
@WebSocket(maxTextMessageSize = 128 * 1024)
public class WebSocketHandlerImpl implements WebSocketActiveSession {

	private static final Logger log = Logger.getLogger(WebSocketHandlerImpl.class.getName());

	private final boolean startedHere;
	private final WebSocketClient client;
	private final URI endpointURI;
	private Session session;
    private CompletableFuture<Session> futureSession;
	private InetAddress addr;
	private int remotePort;
	private InetAddress localAddr;
	private int localPort;
	private String path;
	private String key;
	
	private WebSocketClientPool pool;
	
	/**
	 * Constructor used when connection starts outside.
	 */
	public WebSocketHandlerImpl(WebSocketClientPool pool) {
		this.pool = pool;
		this.client = null;
		this.endpointURI = null;
		this.startedHere = false;
	}

	/**
	 * Constructor used when connection starts here.
	 */
    public WebSocketHandlerImpl(WebSocketClientPool pool,URI endpointURI) {
    	this.pool = pool;
    	this.endpointURI = endpointURI;
    	this.startedHere = true;
    	try {
			this.addr = InetAddress.getByName(endpointURI.getHost());
		} catch (UnknownHostException e1) {
			throw new RuntimeException(e1);
		}
        try {
        	client = new WebSocketClient();
        	client.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
        	connect();
        } catch (Exception e) {
        	try { client.stop(); } catch (Throwable e2){ }
            throw new RuntimeException(e);
        }
    }

    private void connect() {
    	
    	ClientUpgradeRequest request = new ClientUpgradeRequest();
        if (log.isLoggable(Level.FINEST)) 
        	log.log(Level.FINEST,"Requesting connection to "+addr.getHostAddress());
        try {
        	Future<Session> f = client.connect(this, endpointURI, request);
        	futureSession = CompletableFuture.supplyAsync(()->{
        		try {
        			return f.get();
				} catch (Exception e) {
					log.log(Level.SEVERE, "Error while trying to connect to "+addr.getHostAddress()+" through "+endpointURI.toString(), e);
					futureSession = null;
					return null;
				}
        	});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
    	if (client!=null) {
	    	try {
	    		client.stop();
	    	}
	    	catch (Exception e) {
	    		log.log(Level.FINE,"Error while closing websocket client",e);
	    	}
    	}
    	else if (session!=null) {
    		session.close();
    	}
    }
    
    /**
     * Tells if this connection was started here in this robot.
     */
    @Override
    public boolean isStartedHere() {
    	return startedHere;
    }

    public boolean isActive() {
    	return session!=null && session.isOpen();
    }

    /**
     * Get client host name (same as 'remote address')
     */
    @Override
    public String getHost() {
    	if (addr==null)
    		return null;
    	else
    		return addr.getHostAddress();
	}
    
    /**
     * Get remote address used in this session
     */
    @Override
    public InetAddress getRemoteAddress() {
    	return addr;
    }
    
    /**
     * Get client port number
     */
    @Override
    public int getRemotePort() {
    	return remotePort;
    }

    /**
     * Get our local address used in this session
     */
    @Override
    public InetAddress getLocalAddress() {
    	return localAddr;
    }
    
    /**
     * Get our local port used in this session
     */
    @Override
    public int getLocalPort() {
    	return localPort;
    }
    
    /**
     * Get requested path
     */
    @Override
    public String getPath() {
    	return path;
    }

    /**
     * Change requested path reference
     */
    @Override
    public void setPath(String path) {
    	this.path = path;
    }

    /**
     * Will try to reconnect if not already connected and if there is no connection in progress
     */
    private void assertSessionOrFuture() {
    	if (session==null && futureSession==null) {
    		// not connected anymore
    		// try again
    		connect();
    	}
    }
    
    /**
     * Get this unique session ID
     */
    @Override
    public String getSessionId() {
    	return key;
    }

    /**
     * Callback hook for Connection close events.
     */
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
    	session = null;
    	futureSession = null;
    	if (log.isLoggable(Level.INFO))
    		log.log(Level.INFO,"Disconnected from "+((addr==null)?"<null>":addr.toString())+" statusCode:"+statusCode+", reason:"+reason);
		pool.removeSession(this);
    	pool.getRemovalCallback().onCloseConnection(this);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        log.log(Level.SEVERE,"Error in WebSocket Handler (remote: "+((addr==null)?"<null>":addr.toString())
        		+" local: "+((localAddr==null)?"<null>":localAddr.getHostAddress())+")",t);
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param session the user session which is opened.
     */
    @OnWebSocketConnect
    public void onConnect(Session session) {
    	this.session = session;
    	this.futureSession = null; // don't need this anymore
    	this.addr = session.getRemoteAddress().getAddress();
    	this.remotePort = session.getRemoteAddress().getPort();
    	this.localAddr = session.getLocalAddress().getAddress();
    	this.localPort = session.getLocalAddress().getPort();
    	this.path = session.getUpgradeRequest().getRequestURI().getPath();
    	if (session.getUpgradeRequest() instanceof ClientUpgradeRequest) {
    		this.key = ((ClientUpgradeRequest)session.getUpgradeRequest()).getKey();
    	}
    	else {
    		this.key = session.getUpgradeRequest().getHeader("Sec-WebSocket-Key");
    	}
        if (log.isLoggable(Level.INFO)) 
        	log.log(Level.INFO,"Connected with "+((addr==null)?"<null>":addr.toString())+" through local address "+localAddr.getHostAddress());
        pool.addSession(this);
        pool.getInclusionCallback().onStartConnection(this);
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     */
    @OnWebSocketMessage
    public void onMessage(String message) {
		String response = pool.getCommandCentral().onCommand(message, this);
		if (response!=null) {
			try {
				session.getRemote().sendString(response);
			}
			catch (Throwable e) {
				log.log(Level.SEVERE,"Error sending response",e);
			}
		}
    }

    /**
     * Send a message assynchronously.
     */
    public void sendMessage(String message,WriteCallback callback) {
    	assertSessionOrFuture();
    	if (this.session!=null) {
    		this.session.getRemote().sendString(message,callback);
    	}
    	else if (this.futureSession!=null) {
    		// If connection has not completed yet, chain a new task that
    		// will wait for completion and then will try to send the message
    		// afterwards.
    		this.futureSession =
    		this.futureSession.thenApply((session)->{session.getRemote().sendString(message,callback);return session;});
    	}
    	else {
    		log.log(Level.SEVERE,"Could not send message to "+getHost()+" because there is no session neither a future session!");
    	}
    }
}

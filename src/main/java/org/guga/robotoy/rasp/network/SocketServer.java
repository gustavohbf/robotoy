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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a simple raw sockets interface<BR>
 * <BR>
 * This implementation supports only one connection at a time.<BR>
 * <BR>
 * @author Gustavo Figueiredo
 */
public class SocketServer implements Server {
	
	private static final Logger log = Logger.getLogger(SocketServer.class.getName());

	public static final int DEFAULT_PORT = 8089;
	
	/**
	 * The time (in milliseconds) to wait when accepting a client connection.
	 * The accept will be retried until the Daemon is told to stop. So this
	 * interval is the longest time that the Daemon will have to wait after
	 * being told to stop.
	 */
	public static final int ACCEPT_TIMEOUT = 1000;
	
	public static String DEFAULT_CHARSET = "UTF8";

	/** The port to listen on. */
	private int daemonPort = DEFAULT_PORT;

	/** True if the Daemon is currently running. */
	private volatile boolean running;
	
	private String charsetName = DEFAULT_CHARSET;
	
	private final CommandCentral commandCentral;
	
	private Runnable onCloseListener;
	
	/** keeps a weak reference to the current connected client output stream */
	private WeakReference<PrintWriter> currentClientOutput;
	
	/** keeps a reference to the current connected client address */
	private SocketAddress currentClientAddress;
	
	private Runnable onStartCallback;

	public SocketServer(CommandCentral commandCentral)
	{
		this.commandCentral = commandCentral;
	}

	public int getDaemonPort() {
		return daemonPort;
	}

	public void setDaemonPort(int daemonPort) {
		this.daemonPort = daemonPort;
	}

	public String getCharsetName() {
		return charsetName;
	}

	public void setCharsetName(String charsetName) {
		this.charsetName = charsetName;
	}

    public Runnable getOnCloseListener() {
		return onCloseListener;
	}

	@Override
	public void setOnCloseListener(Runnable onCloseListener) {
		this.onCloseListener = onCloseListener;
	}

	@Override
	public void addShutdownHook() {
        Thread shutdown_hook = new Thread(this::stopServer);
        shutdown_hook.setName("SocketServerShutdownHook");
        shutdown_hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdown_hook);    	
    }

	public Runnable getOnStartCallback() {
		return onStartCallback;
	}

	@Override
	public void setOnStartCallback(Runnable onStartCallback) {
		this.onStartCallback = onStartCallback;
	}

	@Override
	public void run() {
		ServerSocket mainSocket = null;
		running = true;

		try {
			log.log(Level.INFO,"Creating Daemon Socket... on port " + daemonPort);

			// no SSL
			mainSocket = new ServerSocket(daemonPort);

			mainSocket.setSoTimeout(ACCEPT_TIMEOUT);
			log.log(Level.INFO,"Daemon up and running!");
			
			if (onStartCallback!=null)
				onStartCallback.run();

			mainLoop(mainSocket);
			
			log.log(Level.INFO,"Daemon Server stopped");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Daemon Server stopped due to error", e);
		} finally {
			try {
				if (mainSocket != null)
					mainSocket.close();
			} catch (Exception exc) {
			}
		}

	}
	
	private void mainLoop(final ServerSocket mainSocket) throws IOException
	{
		while (running) {
			try {
				// Listen on main socket
				Socket clientSocket = mainSocket.accept();
				if (running) {
					try {
						talkToClient(clientSocket);
					}
					catch (Exception e) {
						log.log(Level.SEVERE, "Communication stopped due to error", e);
					}
					finally {
						if (running && onCloseListener!=null) {
							onCloseListener.run();
						}
					}
				} else {
					// The socket was accepted after we were told to stop.
					try {
						clientSocket.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			} catch (InterruptedIOException e) {
				// Timeout occurred. Ignore, and keep looping until we're
				// told to stop running.
			}
		}
	}
	
	private void talkToClient(Socket clientSocket) throws Exception
	{
		SocketAddress addr = clientSocket.getRemoteSocketAddress();
		log.log(Level.INFO,"Connected with "+((addr==null)?"<null>":addr.toString()));
		
		Charset charset = Charset.forName(charsetName);
		try (BufferedReader input =
              new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),charset));
				PrintWriter output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), charset), true);)
		{
			currentClientOutput = new WeakReference<>(output);
			currentClientAddress = addr;
			
			
			final WebSocketActiveSession wrapper = new WebSocketActiveSession() {

				@Override
				public String getHost() {
					return clientSocket.getInetAddress().getHostAddress();
				}

				@Override
				public InetAddress getRemoteAddress() {
					return clientSocket.getInetAddress();
				}

				@Override
				public int getRemotePort() {
					return clientSocket.getPort();
				}

				@Override
				public InetAddress getLocalAddress() {
					return clientSocket.getLocalAddress();
				}

				@Override
				public int getLocalPort() {
					return clientSocket.getLocalPort();
				}

				@Override
				public String getPath() {
					return null;
				}
				
				@Override
				public void setPath(String path) {
					// nothing to do
				}

				@Override
				public boolean isStartedHere() {
					return false;
				}

				@Override
				public String getSessionId() {
					return String.valueOf(System.identityHashCode(clientSocket));
				}
				
			};
			
			while (running) {
				String received_line = processBackspaces(input.readLine());
				if (received_line==null) {
					break;
				}
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST,"Received: "+received_line);
				String response = commandCentral.onCommand(received_line,wrapper);
				if (response!=null) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST,"Response: "+response);
					if (response.endsWith("\n"))
						output.print(response);
					else
						output.println(response);
				}
			}
		}
		finally {	
			if (currentClientOutput!=null)
				currentClientOutput.clear(); // remove reference to PrintWriter
			currentClientAddress = null;
			log.log(Level.INFO,"Disconnected from "+((addr==null)?"<null>":addr.toString()));
		}
	}
	
	/**
	 * Evaluates BACKSPACE character removing characters to the left side.
	 */
	private static String processBackspaces(String s) {
		if (s == null)
			return null;
		s = s.trim();
		if (s.length() == 0)
			return s;
		while (s.indexOf('\b') >= 0) {
			s = s.replaceAll("[^\b]\b", "");
			if (s.startsWith("\b"))
				s = s.substring(1);
		}
		return s;
	}

	public boolean isRunning() {
		return running;
	}

	/**
	 * Stop the proxy daemon. The daemon may not stop immediately.
	 * 
	 * see #ACCEPT_TIMEOUT
	 */
	public void stopServer() {
		running = false;
	}

	public SocketAddress getCurrentClientAddress() {
		return currentClientAddress;
	}

	@Override
	public int getDaemonPortSecure() {
		return 0;
	}

	@Override
	public void setDaemonPortSecure(int daemonPortSecure) {
		throw new UnsupportedOperationException();
	}

}

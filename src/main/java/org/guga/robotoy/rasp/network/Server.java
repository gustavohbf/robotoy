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

/**
 * Simple interface for a server that will listen to
 * incoming requests and will process commands locally.
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface Server extends Runnable {

	/**
	 * Stop the proxy daemon.
	 */
	public void stopServer();

	/**
	 * Set daemon port number. 
	 */
	public void setDaemonPort(int daemonPort);

	/** 
	 * Set the secure port to listen on. 
	 * 0 = no secure port 
	 */
    public int getDaemonPortSecure();

	/** 
	 * The secure port to listen on.<BR>
	 * 0 = no secure port 
	 */
	public void setDaemonPortSecure(int daemonPortSecure);

	/**
	 * Daemon port number this server is listening.
	 */
	public int getDaemonPort();

	/**
	 * Register a shutdown hook that will be called when the JVM is
	 * shutting down.
	 */
	public void addShutdownHook();

	/**
	 * Register a procedure that will be called when this server
	 * is terminating.
	 */
	public void setOnCloseListener(Runnable onCloseListener);

	public void setOnStartCallback(Runnable onStartCallback);
}

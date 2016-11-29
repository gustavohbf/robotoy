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

/**
 * This interface represents an active WebSocket session. 
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface WebSocketActiveSession {

    /**
     * Get client host name (same as 'remote address')
     */
    public String getHost();

    /**
     * Get remote address used in this session
     */
    public InetAddress getRemoteAddress();

    /**
     * Get client port number
     */
    public int getRemotePort();

    /**
     * Get our local address used in this session
     */
    public InetAddress getLocalAddress();

    /**
     * Get our local port used in this session
     */
    public int getLocalPort();

    /**
     * Get requested path
     */
    public String getPath();
    
    /**
     * Change requested path reference
     */
    public void setPath(String path);

    /**
     * Get this unique session ID
     */
    public String getSessionId();

    /**
     * Tells if this connection was started here in this robot.
     */
    public boolean isStartedHere();
    
    /**
     * Close the active session
     */
    public void close();
}

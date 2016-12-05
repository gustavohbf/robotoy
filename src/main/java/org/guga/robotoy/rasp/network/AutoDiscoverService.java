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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Simple auto-discovery service using UDP multicast over local network
 * 
 * @author Gustavo Figueiredo
 *
 */
public class AutoDiscoverService {

	private static final Logger log = Logger.getLogger(AutoDiscoverService.class.getName());

	/**
	 * Chosen at random from local network block at http://www.iana.org/assignments/multicast-addresses/multicast-addresses.xhtml<BR>
	 * Any address, from 239.0.0.0 to 239.255.255.255, will work.
	 */
    public static final String DEFAULT_MULTICAST_ADDRESS = "239.255.123.111";  
    public static final String DEFAULT_MULTICAST_NETWORK = "239.255.123.0";  
	public static final int DEFAULT_PORT = 8090;
	private static final int DEFAULT_CLIENT_TIMEOUT_MS = 2000;
	
	/**
	 * Multicast Address
	 */
    public String multicastAddress = DEFAULT_MULTICAST_ADDRESS;  
    
    /**
     * UDP port number
     */
    private int port = DEFAULT_PORT;

    /**
     * Signal current client/server monitoring in progress
     */
    private final AtomicBoolean runningClient = new AtomicBoolean();

    /**
     * Set to TRUE whenever wants to stop a running client/server
     */
    private final AtomicBoolean stopRunningClient = new AtomicBoolean();
    
    /**
     * Socket for current client in progress
     */
    private DatagramSocket runningClientSocket;
    
    /**
     * Socket for current server in progress
     */
    private MulticastSocket runningServerSocket;
    
    /**
     * Shutdown hook configured by 'addShutdownHook'
     */
    private Thread shutdown_hook;

	/**
	 * Multicast Address
	 */
	public String getMulticastAddress() {
		return multicastAddress;
	}

	/**
	 * Multicast Address
	 */
	public void setMulticastAddress(String multicastAddress) {
		this.multicastAddress = multicastAddress;
	}

    /**
     * UDP port number
     */
	public int getPort() {
		return port;
	}

    /**
     * UDP port number
     */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Registers a 'shutdown hook' to this JVM in order to automatically and
	 * gracefully terminate this service. Should not be invoked more than
	 * once.
	 */
	public void addShutdownHook() {
        shutdown_hook = new Thread(this::stopService);
        shutdown_hook.setName("AutoDiscoverShutdownHook");
        shutdown_hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdown_hook);
    }
	
	public void removeShutdownHook() {
		if (shutdown_hook!=null)
			Runtime.getRuntime().removeShutdownHook(shutdown_hook);
		shutdown_hook = null;		
	}

	/**
	 * Starts asynchronous client for monitoring other components in local network 
	 * @param inet_name Network interface name, or NULL if it should try to use the default
	 * @param challengeQuestion Application specific magic phrase used to query other services
	 * @param ourAnswer Application specific answer if our service get challenged from others
	 * @param expectedAnswer Application specific answer to challenge question used to recognize other services
	 * @param callback Callback routine used whenever some client responds
	 * if you don't want to filter by address.
	 */
    public void startService(String inet_name,String challengeQuestion,String ourAnswer,Pattern expectedAnswer,ClientCallback callback) {
    	
    	synchronized (runningClient) {
    		
    		// Stop running client if there is any
	    	if (runningClient.get()) {
	    		long wait_running_client_timeout = DEFAULT_CLIENT_TIMEOUT_MS*2+System.currentTimeMillis();
	    		stopService();
	    		try {
		    		while (runningClient.get() && System.currentTimeMillis()<wait_running_client_timeout) {
		    			try { Thread.sleep(100); } catch (InterruptedException e) { }
		    		}
	    		}
	    		finally {
	    			stopRunningClient.set(false);
	    		}
	    	}
	    	
	    	// Starts a new thread for running client
	    	DatagramSocket clientSocket = null;
	    	MulticastSocket serverSocket = null;
	        try {
	            clientSocket = new DatagramSocket();
                serverSocket = new MulticastSocket(port);
                
                if (inet_name!=null) {
                	try {
                		serverSocket.setNetworkInterface(NetworkInterface.getByName(inet_name));
                	}
                	catch (Throwable e) {
                		log.log(Level.SEVERE,"Error while trying to set network interface '"+inet_name+"'. Will ignore this.", e);
                	}
                }

                boolean serving_multicast;
                try {
	                InetAddress group = InetAddress.getByName(multicastAddress);
	                serverSocket.joinGroup(group);
	                serving_multicast = true;
                }
                catch (Throwable e) {
                	log.log(Level.INFO, "Coult not start to broadcast with multicast address!", e);
                	serving_multicast = false;
                }
	            
	            stopRunningClient.set(false);
	        	runningClient.set(true);
	        	runningClientSocket = clientSocket;
	        	runningServerSocket = serverSocket;
	        	if (log.isLoggable(Level.INFO))
	        		log.log(Level.INFO, "Starting multicast for other services alike in local network...");
	        	if (serving_multicast) {
	        		new ServerThread(challengeQuestion,ourAnswer,serverSocket).start();
	        	}
	        	int maxLengthAnswer = ourAnswer.length()*2; // estimate maximum length for others answers
	            new ClientThread(challengeQuestion,expectedAnswer,maxLengthAnswer,callback,clientSocket).start();
	        } catch (Exception e) {
	            log.log(Level.SEVERE, "Error while starting multicast client for auto discovery service", e);
	            if (log.isLoggable(Level.FINE)) {
	            	try {
		            	InetUtils.NetAdapter[] adapters = InetUtils.getNetAdapters();
		            	if (adapters==null || adapters.length==0) {
		            		log.log(Level.FINE,"Not network adapters found!");
		            	}
		            	else {
		            		log.log(Level.FINE,"Network adapters found: "+Arrays.toString(adapters));
		            	}
	            	}
	            	catch (Throwable e2) {
	            		log.log(Level.FINE, "Error while reporting current network adapters", e2);
	            	}
	            }
	            if (clientSocket!=null) {
	            	try {
	            		clientSocket.close();
	            	}
	            	catch (Exception e2) {
	            	}
	            }
	            if (serverSocket!=null) {
	            	try {
	            		serverSocket.close();
	            	}
	            	catch (Exception e2) {	            		
	            	}
	            }
	        }
    	}
    }
    
    /**
     * Stop service that you started with a previous call to {@link #startService(String, String, ClientCallback) startService}.<BR>
     * It will terminate both the client side and the server side of this service.
     */
    public void stopService() {
    	synchronized (runningClient) {
    		stopRunningClient.set(true);
            if (runningClientSocket!=null) {
            	try {
            		runningClientSocket.close();
            	}
            	catch (Throwable e){ }
            }
            if (runningServerSocket!=null) {
            	try {
            		runningServerSocket.close();
            	}
            	catch (Throwable e){ }
            }
    	}
    }
   
    /**
     * This interface should be implemented and provided with {@link AutoDiscoverService#startService(String, String, ClientCallback) startService}
     * in order to receive notification events about the auto discovery service replies.
     * @author Gustavo Figueiredo
     *
     */
    @FunctionalInterface
    public static interface ClientCallback {
    	/**
    	 * Method invoked whenever another service running in the same network replies to this service request.
    	 * @param remoteAddress Remote IP address
    	 * @param remotePort Remote UDP port number
    	 * @param answerReceived Answer received from UDP message
    	 */
    	public void onConnection(InetAddress remoteAddress,int remotePort,String answerReceived);
    }
    
    /**
     * This thread runs the server side of auto discovery service through UDP multicast.<BR>
     * <BR>
     * It will listen in multicast address for some magic phrase. Then it will send a reply with
     * an expected answer back to the requesting client.<BR>
     * <BR>
     * In order to stop this thread, the method {@link AutoDiscoverService#stopService() stopService} should be called.
     * 
     * @author Gustavo Figueiredo
     *
     */
    private class ServerThread extends Thread {
    	private final String challengeQuestion;
    	private final String expectedAnswer;
    	private final MulticastSocket socket;
    	ServerThread(String challengeQuestion,String expectedAnswer,MulticastSocket socket) {
    		super("AutoDiscoverServiceServer");
    		setDaemon(true);
    		this.challengeQuestion = challengeQuestion;
    		this.expectedAnswer = expectedAnswer;
    		this.socket = socket;
    	}    	
    	@Override
    	public void run() {
    		if (log.isLoggable(Level.FINE))
    			log.log(Level.FINE, "Starting ServerThread #"+this.getId());
    		// Waits for another clients from different hosts asking for service
        	while (runningClient.get() && !stopRunningClient.get()) {
        		
                byte[] recData = new byte[challengeQuestion.getBytes().length];
                DatagramPacket receivePacket = new DatagramPacket(recData, recData.length);
                try {
                	socket.receive(receivePacket);
                } catch (java.net.SocketTimeoutException e) {
                	break; // will ask again in outer loop
				} catch (Exception e) {
					if (stopRunningClient.get())
						break;
					log.log(Level.SEVERE, "Error while waiting for datagram queries from clients", e);
					continue;
				}
                
                String strrec = new String(recData,0,receivePacket.getLength());
                if (challengeQuestion.equals(strrec)) {
                	// Got magic question from supposed client of alike service
                	// Will send expected reply, unless it's from the same machine.
                	if (!isLocalAddress(receivePacket.getAddress())) {
                        byte[] msgData = expectedAnswer.getBytes();
                        DatagramPacket msgPacket = new DatagramPacket(msgData, msgData.length, receivePacket.getAddress(), receivePacket.getPort());
                        try {
							socket.send(msgPacket);
						} catch (IOException e) {
							if (stopRunningClient.get())
								break;
							log.log(Level.SEVERE, "Error while answering requesting client (address: "+receivePacket.getAddress()
								+", port: "+receivePacket.getPort()+", length:"+msgData.length+")", e);
						}                		
                	}
                }

        	} // LOOP while running client and not stop running client
    		if (log.isLoggable(Level.FINE))
    			log.log(Level.FINE, "Stopping ServerThread #"+this.getId());
    	}
    }
    
    private static boolean isLocalAddress(InetAddress addr) {
    	if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;
    	try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }
    
    /**
     * This thread runs the client side of auto discovery service through UDP multicast.<BR>
     * <BR>
     * It will periodically submit some magic phrase using a multicast address over local network and will wait for
     * replies. If it receives a reply with an expected answer, it will call a 'callback' routine.<BR>
     * <BR>
     * In order to stop this thread, the method {@link AutoDiscoverService#stopService() stopService} should be called.
     * 
     * @author Gustavo Figueiredo
     *
     */
    private class ClientThread extends Thread {
    	private final String challengeQuestion;
    	private final Pattern expectedAnswer;
    	private final int maxLengthAnswer;
    	private final ClientCallback callback;
    	final DatagramSocket socket;
    	ClientThread(String challengeQuestion,Pattern expectedAnswer,int maxLengthAnswer,ClientCallback callback,DatagramSocket socket) {
    		super("AutoDiscoverServiceClient");
    		setDaemon(true);
    		this.challengeQuestion = challengeQuestion;
    		this.expectedAnswer = expectedAnswer;
    		this.maxLengthAnswer = maxLengthAnswer;
    		this.callback = callback;
    		this.socket = socket;
    	}
    	@Override
    	public void run() {
            final byte[] recData = new byte[maxLengthAnswer];
            final DatagramPacket receivePacket = new DatagramPacket(recData, recData.length);
            try {
				socket.setSoTimeout(DEFAULT_CLIENT_TIMEOUT_MS);
			} catch (SocketException e1) {
			}
        	while (runningClient.get() && !stopRunningClient.get()) {
        		
        		// Ask magic question in local network
	            byte[] msgData = challengeQuestion.getBytes();
	            DatagramPacket datagramPacket;
				try {
					datagramPacket = new DatagramPacket(msgData, msgData.length, 
							InetAddress.getByName(multicastAddress), port);
		            socket.send(datagramPacket);
				} catch (UnknownHostException e) {
					log.log(Level.SEVERE, "Error while sending challenge question", e);
					break;
				} catch (IOException e) {
					if (stopRunningClient.get())
						break;
					log.log(Level.SEVERE, "Error while sending challenge question", e);
					continue;
				}

				// May have multiple replies if there are more than two services in local network
				while (runningClient.get() && !stopRunningClient.get()) {
	                try {
	                	socket.receive(receivePacket);
	                } catch (java.net.SocketTimeoutException e) {
	                	break; // will ask again in outer loop
					} catch (Exception e) {
						if (stopRunningClient.get())
							break;
						log.log(Level.SEVERE, "Error while waiting for datagram replies", e);
						continue;
					}
	                String strrec = new String(recData,0,receivePacket.getLength());
	                if (expectedAnswer.matcher(strrec).find()) {
	                	callback.onConnection(receivePacket.getAddress(), receivePacket.getPort(), strrec);
	                }
				}
        	}
        	if (log.isLoggable(Level.INFO))
        		log.log(Level.INFO, "Stopped multicast for other services alike in local network...");
        	runningClient.set(false);
        	socket.close();    		
    	}
    }
}

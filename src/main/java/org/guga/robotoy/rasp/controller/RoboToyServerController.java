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

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

import org.guga.robotoy.rasp.commands.*;
import org.guga.robotoy.rasp.game.GamePersistentData;
import org.guga.robotoy.rasp.game.GamePlayMode;
import org.guga.robotoy.rasp.game.GamePlayer;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.game.GameState;
import org.guga.robotoy.rasp.motor.ArduinoController;
import org.guga.robotoy.rasp.motor.DummyMotor;
import org.guga.robotoy.rasp.motor.MotorShield;
import org.guga.robotoy.rasp.motor.MotorShield.PinLayout;
import org.guga.robotoy.rasp.motor.MotorShield.Wheels;
import org.guga.robotoy.rasp.network.AutoDiscoverService;
import org.guga.robotoy.rasp.network.AutoDiscoveryRobotsCallback;
import org.guga.robotoy.rasp.network.CommandCentral;
import org.guga.robotoy.rasp.network.DisconnectionControl;
import org.guga.robotoy.rasp.network.InclusionCallback;
import org.guga.robotoy.rasp.network.WebServer;
import org.guga.robotoy.rasp.network.WebSocketActiveSession;
import org.guga.robotoy.rasp.network.WebSocketClientPool;
import org.guga.robotoy.rasp.optics.LedColor;
import org.guga.robotoy.rasp.optics.RGBLed;
import org.guga.robotoy.rasp.optics.RoboToyWeaponary;
import org.guga.robotoy.rasp.rfid.RFIDRead;
import org.guga.robotoy.rasp.rfid.RoboToyPowerUps;
import org.guga.robotoy.rasp.utils.GPIOUtils;
import org.guga.robotoy.rasp.utils.JSONUtils;
import org.guga.robotoy.rasp.utils.SimpleLocalStorage;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.spi.SpiChannel;

/**
 * Controller for the local hardware interface. 
 * @author Gustavo Figueiredo
 *
 */
public class RoboToyServerController implements CommandCentral, InclusionCallback, WebServer.AutoRedirection {
	
	private static final Logger log = Logger.getLogger(RoboToyServerController.class.getName());
	
	private static final int HEARTBEATS_DELAY_MS = 1000;

	private static final int PING_DELAY_MS = 2000;

	// General actions
	public static final char QUERY_PLAYERS = 'P';
	public static final char QUERY_COLOR = 'C';
	public static final char QUERY_ROBOTS = 'R';	
	public static final char QUERY_STATUS = '?';
	public static final char QUERY_OWNER = 'O';
	public static final char GREETINGS = 'G';
	public static final char HEARTBEAT = '.';
	public static final char PING = 'p';

	// Lobby actions
	public static final char TAKE_ROBOT = 'T';
	public static final char LEAVE_ROBOT = 'L';
	public static final char START_GAME = 'S';
	
	// Driving actions
	public static final char MOVE_FORWARD = 'f';
	public static final char MOVE_BACKWARD = 'b';
	public static final char TURN_LEFT = 'l';
	public static final char TURN_RIGHT = 'r';
	public static final char STOP = 's';
	public static final char SET_MIN_SPEED = '1';
	public static final char SET_MED_SPEED = '2';
	public static final char SET_MAX_SPEED = '3';
	public static final char FIRE = ' ';
	
	// Summary actions
	public static final char QUERY_RANKING = 'K';
	public static final char PLAY_AGAIN = 'A';
	
	private static final String DEFAULT_ADMIN_USER_NAME = "admin";
	
	// Commands from players or other robots
	public static Command commands[] = {
		new CmdAddNewPlayer(),
		new CmdChangeName(),
		new CmdCharge(),
		new CmdFire(),
		new CmdGreetings(),
		new CmdHeartBeat(),
		new CmdHit(),
		new CmdLeaveRobot(),
		new CmdMoveBackward(),
		new CmdMoveForward(),
		new CmdPing(),
		new CmdQueryColor(),
		new CmdQueryOwner(),
		new CmdQueryPlayers(),
		new CmdQueryRanking(),
		new CmdQueryRobots(),
		new CmdQueryStatus(),
		new CmdPlayAgain(),
		new CmdPlayerDisconnected(),
		new CmdPlayerReconnected(),
		new CmdRemovePlayer(),
		new CmdRemoveRobot(),
		new CmdResourcesLoaded(),
		new CmdRestartGame(),
		new CmdSetColor(),
		new CmdSetMovement(),
		new CmdSetReady(),
		new CmdSetSpeed(),
		new CmdStartGame(),
		new CmdStopGame(),
		new CmdStop(),
		new CmdTakeRobot(),
		new CmdTurnLeft(),
		new CmdTurnRight(),
	};
			
	private final RoboToyServerContext context;
		
	private AutoDiscoverService autoDiscoverOtherRobots;
	
	private AutoDiscoveryRobotsCallback autoDiscoveryCallback;
	
	private ControllerType controllerType = ControllerType.RASPBERRY;
	
	private PinLayout raspPinLayout;
	
	private ArduinoController.Config arduinoControllerConfig;
	
	private Pin pinBeamDevice;
	
	private Pin pinDetectorDevices[];
	
	private PinPullResistance pinDetectorResistor = PinPullResistance.PULL_DOWN;
	
	private RGBLed.DiodeType rgbType;
	
	private Pin pinRed, pinGreen, pinBlue;
	
	private String adminUserName = DEFAULT_ADMIN_USER_NAME;
	
	private Pin pinRFID;
	
	private SpiChannel csRFID;
				
	public RoboToyServerController(GameState game) {
		this.context = new RoboToyServerContext(game);
		this.context.setWebSocketPool(new WebSocketClientPool(this,this,new DisconnectionControl(this)));
		this.context.setLocalStorage(new SimpleLocalStorage());
	}
	
	public boolean hasArduinoController() {
		return ControllerType.ARDUINO.equals(controllerType);
	}

	public void setArduinoController(ArduinoController.Config arduinoControllerConfig) {
		controllerType = ControllerType.ARDUINO;
		this.arduinoControllerConfig = arduinoControllerConfig;
		this.raspPinLayout = null;
	}
	
	public boolean hasRaspberryController() {
		return ControllerType.RASPBERRY.equals(controllerType);
	}
	
	public void setRaspberryController(PinLayout pinLayout) {
		controllerType = ControllerType.RASPBERRY;
		this.raspPinLayout = pinLayout;
		this.arduinoControllerConfig = null;
	}
	
	public boolean hasDummyController() {
		return ControllerType.DUMMY.equals(controllerType);
	}
	
	public void setDummyController() {
		controllerType = ControllerType.DUMMY;
		this.raspPinLayout = null;
	}

	public Pin getPinBeamDevice() {
		return pinBeamDevice;
	}

	public void setPinBeamDevice(Pin pinBeamDevice) {
		this.pinBeamDevice = pinBeamDevice;
	}

	public void setPinBeamDevice(String pinBeamDevice) {
		this.pinBeamDevice = GPIOUtils.parsePin(pinBeamDevice);
	}

	public Pin[] getPinDetectorDevices() {
		return pinDetectorDevices;
	}

	public void setPinDetectorDevices(Pin... pinDetectorDevices) {
		this.pinDetectorDevices = pinDetectorDevices;
	}

	public void setPinDetectorDevices(String... pinDetectorDevices) {
		if (pinDetectorDevices==null || pinDetectorDevices.length==0)
			this.pinDetectorDevices = null;
		else {
			this.pinDetectorDevices = new Pin[pinDetectorDevices.length];
			for (int i=0;i<pinDetectorDevices.length;i++) {
				this.pinDetectorDevices[i] = GPIOUtils.parsePin(pinDetectorDevices[i].trim());
			}
		}
	}

	public PinPullResistance getPinDetectorResistor() {
		return pinDetectorResistor;
	}

	public void setPinDetectorResistor(PinPullResistance pinDetectorResistor) {
		this.pinDetectorResistor = pinDetectorResistor;
	}

	public void setPinDetectorResistor(String pinDetectorResistor) {
		if (pinDetectorResistor==null || pinDetectorResistor.trim().length()==0)
			this.pinDetectorResistor = PinPullResistance.OFF;
		else {
			switch (pinDetectorResistor.trim().toUpperCase()) {
			case "UP":
				this.pinDetectorResistor = PinPullResistance.PULL_UP;
				break;
			case "DOWN":
				this.pinDetectorResistor = PinPullResistance.PULL_DOWN;
				break;
			case "OFF":
				this.pinDetectorResistor = PinPullResistance.OFF;
				break;
			default:
				throw new UnsupportedOperationException("Unknown value for internal resistor used with IR detector pin: "+pinDetectorResistor);
			}
		}
	}

	public String getAdminUserName() {
		return adminUserName;
	}

	public void setAdminUserName(String adminUserName) {
		this.adminUserName = adminUserName;
	}

	public RoboToyServerContext getContext() {
		return context;
	}
	
	public void setRGBLed(RGBLed.DiodeType rgbType,Pin pinRed, Pin pinGreen, Pin pinBlue) {
		this.rgbType = rgbType;
		this.pinRed = pinRed;
		this.pinGreen = pinGreen;
		this.pinBlue = pinBlue;
	}

	public void setRGBLed(RGBLed.DiodeType rgbType,String pinRed, String pinGreen, String pinBlue) {
		this.rgbType = rgbType;
		this.pinRed = GPIOUtils.parsePin(pinRed);
		this.pinGreen = GPIOUtils.parsePin(pinGreen);
		this.pinBlue = GPIOUtils.parsePin(pinBlue);
	}
	
	public void setRFID(Pin pinRST, SpiChannel cs) {
		this.pinRFID = pinRST;
		this.csRFID = cs;
	}

	public void setRFID(String pinRST, String cs) {
		this.pinRFID = GPIOUtils.parsePin(pinRST);
		if (cs!=null && cs.length()>0) {
			int cs_i = Integer.parseInt(cs);
			this.csRFID = SpiChannel.getByNumber(cs_i);
			if (this.csRFID==null)
				throw new UnsupportedOperationException("Invalid rfid.cs option:"+cs);
		}
		else {
			this.csRFID = SpiChannel.CS0;
		}
	}

	public void init() {
		
		if (rgbType!=null) {
			context.setRGBLed(new RGBLed(rgbType,pinRed,pinGreen,pinBlue));
		}
		
		switch (controllerType) {
		case ARDUINO: {
			ArduinoController arduino = new ArduinoController();
			if (arduinoControllerConfig!=null)
				arduino.setConfig(arduinoControllerConfig);
			arduino.addShutdownHook();
			
			arduino.stop();
			
			context.setMotor(arduino);
			break;
		}
		case RASPBERRY: {
			MotorShield ms = new MotorShield();
			if (raspPinLayout!=null)
				ms.setPinLayout(raspPinLayout);
	        ms.addShutdownHook();
	        
	        ms.enable();
        	if (Wheels.FOUR.equals(raspPinLayout.getWheels())) {
		        ms.motorInit(1);
		        ms.motorInit(2);
        	}
	        ms.motorInit(3);
	        ms.motorInit(4);
	        
			context.setMotor(ms);
			break;
		}
		case DUMMY: {
			context.setMotor(new DummyMotor());
			break;
		}
		default:
			throw new UnsupportedOperationException("Unexpected controller type: "+controllerType);
		};
				
		RoboToyWeaponary weaponary = new RoboToyWeaponary(this);
		if (pinBeamDevice!=null) {
			weaponary.buildBeamDevice(pinBeamDevice);
		}
		
		if (pinDetectorDevices!=null && pinDetectorDevices.length>0) {
			weaponary.buildBeamDetectors(pinDetectorResistor,pinDetectorDevices);
		}
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		startHeartBeats(scheduler);
		startPingPlayers(scheduler);
		
		try {
			GamePersistentData persistent_data = GamePersistentData.load(context, /*createNewIfInexistent*/false);
			if (persistent_data!=null) {
				if (persistent_data.getColor()!=null) {
					LedColor color = LedColor.get(persistent_data.getColor());
					if (color!=null) {
						GameRobot localRobot = context.getGame().findLocalRobot();
						if (localRobot!=null) {
							localRobot.setColor(color);
						}
					}
				}
				if (persistent_data.getPreviousOwner()!=null) {
					GameRobot localRobot = context.getGame().findLocalRobot();
					if (localRobot!=null) {
						localRobot.setPreviousOwner(persistent_data.getPreviousOwner());
					}
				}
			}
		}
		catch (Throwable e) {
			log.log(Level.WARNING, "Error while loading persistent data!",e);
		}
		
		if (context.getRGBLed()!=null) {
			GameRobot localRobot = context.getGame().findLocalRobot();
			if (localRobot!=null && localRobot.getColor()!=null) {
				context.getRGBLed().setColor(localRobot.getColor());
			}
			else {
				context.getRGBLed().startCycleColors(500);
			}
		}
		
		if (pinRFID!=null && csRFID!=null) {
			try {
				RFIDRead rfidRead = new RFIDRead();
				rfidRead.setPinNRSTPD(pinRFID);
				rfidRead.setSpiChannel(csRFID);
				rfidRead.setCallback(new RoboToyPowerUps(this));
				rfidRead.init();
				rfidRead.addShutdownHook();
				context.setRFIDReader(rfidRead);
				RoboToyPowerUps.scheduleCardsManagement(this);
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "Error while initializing RFID reader!", e);
			}
		}
	}
	
	/**
	 * Stats auto-discover service. This will make this bot discoverable by other bots and
	 * this will also make it possible for this bot to recognize others.<BR>
	 * You should only call this function after starting everything else.
	 */
	public void startAutoDiscoverService(int port,int portSecure) {
		if (context!=null && context.getGame()!=null && context.getGame().findLocalRobot()!=null) {
			autoDiscoverOtherRobots = new AutoDiscoverService();
			autoDiscoverOtherRobots.addShutdownHook();
			String ourMagicAnswer = AutoDiscoveryRobotsCallback.prefixAutoDiscoveryAnswer+":"+context.getGame().findLocalRobot().getIdentifier();
			autoDiscoveryCallback = new AutoDiscoveryRobotsCallback(this);
			autoDiscoveryCallback.setPort(port);
			autoDiscoveryCallback.setPortSecure(portSecure);
			String inet_name = (context.getAccessPointMode()!=null) ? context.getAccessPointMode().getAPName() : null;
			autoDiscoverOtherRobots.startService(
					inet_name,
					"Are you a Robotoy?", 				// magic question
					ourMagicAnswer,						// our answer to others
					AutoDiscoveryRobotsCallback.patternAutoDiscoveryAnswer,  		// expected answer from others
					autoDiscoveryCallback				// auto discovery callback
				);
		}
	}
	
	public void stopAutoDiscoverService() {
		if (autoDiscoverOtherRobots!=null) {
			autoDiscoverOtherRobots.removeShutdownHook();
			autoDiscoverOtherRobots.stopService();
		}
	}
	
	public AutoDiscoveryRobotsCallback getAutoDiscoveryCallback() {
		return autoDiscoveryCallback;
	}

	public AutoDiscoverService getAutoDiscoverOtherRobots() {
		return autoDiscoverOtherRobots;
	}

	@Override
	public void onStartConnection(WebSocketActiveSession session) {
		// If some known player reconnected to us, let's update his online status and
		// let's broadcast this situation to others
		if (context==null || context.getGame()==null)
			return;
		if (RoboToyServerContext.isConnectedToPlayer(session)) {
			String player_name = RoboToyServerContext.getPlayerName(session);
			GamePlayer player = (player_name==null) ? null : context.getGame().findPlayerWithName(player_name);
			if (player!=null) {
				player.setOnline(true);
				try {
					broadcastCommand(new CmdPlayerReconnected(),player,/*includingRobots*/true);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Error while broadcasting player reconnected at address "+session.getHost()+":"+session.getRemotePort(), e);
				}
			}
		}
	}

	/**
	 * Got message requested from a player or from another BOT.
	 */
	@Override
	public String onCommand(String t,WebSocketActiveSession session) {
		if (t==null || t.length()==0)
			return null;
		
		if (t.charAt(0)==HEARTBEAT)
			return null; // This kind of message does not require additional treatment
		
		final InetAddress remoteAddr = session.getRemoteAddress();
		final int remotePort = session.getRemotePort(); 
				
    	if (log.isLoggable(Level.FINEST)
    		&& t.charAt(0)!=QUERY_STATUS
    		&& t.charAt(0)!=PING
    		&& !t.startsWith("{\"ping\"")
    		&& !t.startsWith("{\"updateping\"")
    		&& !t.startsWith("{\"movement\"")) {
    		log.log(Level.FINEST,"Received: "+t+" from "+remoteAddr+":"+remotePort);
    	}
    	
    	CommandIssuer issuer;
    	if (RoboToyServerContext.isConnectedToRobot(session))
    		issuer = CommandIssuer.ROBOT;
    	else
    		issuer = CommandIssuer.PLAYER;

		String response = null;

		// For each command there might be two different ways:
		// One for commands as should be issued by directly connected players
		// Other for commands as should be issued by directly connected robots
		final boolean issuer_is_a_player = CommandIssuer.PLAYER.equals(issuer); 
		// Loop through known commands
		for (Command cmd:commands) {
			
			if (cmd.isParseable(issuer,t)) {
				
				Object to_broadcast;
				try {
					to_broadcast = cmd.parseMessage(issuer, context, t, session);
				}
				catch (Exception e) {
					log.log(Level.WARNING, "Error while parsing "+cmd.getClass().getSimpleName()+" message received from "+remoteAddr, e);
					if (issuer_is_a_player)
						response = e.getMessage();
					break;
				}
				if (to_broadcast!=null
						&& (cmd instanceof CommandWithBroadcast)) {
					@SuppressWarnings("unchecked")
					CommandWithBroadcast<Object> bcmd = (CommandWithBroadcast<Object>)cmd;
					// Do not broadcast to other robots if issuer is another robot
					final boolean includingRobots = issuer_is_a_player;
					try {
						if (to_broadcast instanceof Collection) {
							// multi-broadcast
							for (Object element:((Collection<?>)to_broadcast)) {
								broadcastCommand(bcmd,element,includingRobots);
							}
						}
						else {
							// single broadcast
							broadcastCommand(bcmd,to_broadcast,includingRobots);
						}
					} catch (Exception e) {
						log.log(Level.SEVERE, "Error while broadcasting to other players "+cmd.getClass().getSimpleName()+" message received from "+remoteAddr, e);
						if (issuer_is_a_player)
							response = e.getMessage();
					}
				}
				response = cmd.getReply(issuer, context, t, session, to_broadcast);

				break;
			}
			
		} // Loop through known commands
				
		if (response!=null 
				&& log.isLoggable(Level.FINEST)
	    		&& t.charAt(0)!=QUERY_STATUS)
			log.log(Level.FINEST,"Response to "+remoteAddr+":"+remotePort+": "+response);

		return response;
	}

	public class AutoParkOnDisconnection implements Runnable
	{
		@Override
		public void run() {
			if (context.getMotor()!=null)
				CmdStop.run(context);
		}		
	}

	/**
	 * Get mandatory HTTP redirections based on the following rules:<BR>
	 * - If in first page or in login page, return the page itself.<BR>
	 * - If user was not previously logged in, given his IP address, redirects to first page.<BR>
	 * - Return NULL otherwise (no mandatory redirection).
	 */
	@Override
	public String getHTTPRedirection(String remoteAddress,String requestedURI,String queryString,String resourceName,HttpSession session) {
		if (context==null)
			return null;
		if (resourceName.endsWith("index.html")
			|| resourceName.endsWith("index.jsp")
			|| requestedURI.equals("/"))
			return null; // goto default index page
		
		if (resourceName.endsWith("admin.jsp") 
				|| resourceName.endsWith("options.jsp")
				|| resourceName.endsWith("tableview.jsp")) {
			String userName = (session==null) ? null : (String)session.getAttribute("USERNAME");
			if (userName!=null && adminUserName!=null && adminUserName.equalsIgnoreCase(userName)) {
				return null; // goto administration page
			}
			else {
				return "index.jsp";	// not an administrator
			}
		}
		if (resourceName.endsWith("setup.jsp")) {
			if (GamePlayMode.STANDALONE.equals(context.getGamePlayMode()))
				return null; // go to setup page in STANDALONE mode for whatever user logged in
			else {
				String userName = (session==null) ? null : (String)session.getAttribute("USERNAME");
				if (userName!=null && adminUserName!=null && adminUserName.equalsIgnoreCase(userName)) {
					return null; // go to setup page if not in STANDALONE mode as long as the user is admin
				}
				else {
					return "index.jsp";	// not an administrator
				}				
			}
		}
		if (GamePlayMode.STANDALONE.equals(context.getGamePlayMode())) {
			if (resourceName.endsWith("login.html")
					|| resourceName.endsWith("login.jsp")
					|| resourceName.endsWith("lobby.jsp")
					|| resourceName.endsWith("summary.jsp")) {
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, remoteAddress+" requested "+requestedURI+" about resource "+resourceName+". Will redirect to setup.jsp");
				return "setup.jsp"; // user was in the wrong page for this WiFi mode
			}
			return null; // goto desired page
		}
		else {
			if (resourceName.endsWith("login.html")
				|| resourceName.endsWith("login.jsp"))
				return null; // goto default login page
			if (null==context.getGame().findPlayerWithAddress(remoteAddress)) {
				// Could not find a player with the remote address. Maybe he got redirected.
				// Check if the 'username' was given in URL
				String username = getUserNameInQueryParameter(queryString);
				if (username!=null) {
					GamePlayer player = context.getGame().findPlayerWithName(username);
					if (player!=null) {
						// Found a player with this name. Now let's check if this player is
						// already connected to us or to anybody else.
						WebSocketActiveSession ws_session = context.findPlayerWebSocketSession(username);
						if (ws_session!=null) {
							if (log.isLoggable(Level.FINEST))
								log.log(Level.FINEST, remoteAddress+" requested "+requestedURI+"?"+queryString+" about resource "+resourceName
										+" related to a player named '"+username+"', but it seems this player is already online through "
										+ws_session.getHost()+":"+ws_session.getRemotePort()+"!");							
						}
						else if (player.isOnline() && !context.getGame().isOwnerThisRobot(username)) {
							if (log.isLoggable(Level.FINEST))
								log.log(Level.FINEST, remoteAddress+" requested "+requestedURI+"?"+queryString+" about resource "+resourceName
										+" related to a player named '"+username+"', but it seems this player is already online through "
										+player.getAddressString()+":"+player.getPort()+"!");														
						}
						else {
							// If the player is not connected yet, let's log him here
							try {
								player.setAddress(InetAddress.getByName(remoteAddress));
								player.setOnline(true);
								broadcastCommand(new CmdPlayerReconnected(),player,/*includingRobots*/true);
								// Will keep player here
								return null;
							} catch (UnknownHostException e) {
								log.log(Level.SEVERE,"Error while assigning player to this robot", e);
							} catch (Exception e) {
								log.log(Level.SEVERE, "Error while broadcasting player reconnected at address "+remoteAddress, e);
							}
						}
					}
					else {
						if (log.isLoggable(Level.FINEST))
							log.log(Level.FINEST, remoteAddress+" requested "+requestedURI+" about resource "+resourceName
									+", but could not find a player with name '"+username+"'!");
					}
				}
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, remoteAddress+" requested "+requestedURI+" about resource "+resourceName+". Will redirect to index.html");
				return "index.jsp";
			}
			else {
				// Found some player with the same address, presumes it's OK to proceed
				return null;
			}
		}
	}
	
	/**
	 * Given a URI, search for a query parameter named 'username'. Return its value if
	 * found. Return NULL otherwise.
	 */
	private static String getUserNameInQueryParameter(String queryString) {
		if (queryString==null)
			return null;
		String[] query_params = queryString.split("&");
		for (String query_param:query_params) {
			String pair[] = query_param.split("=");
			if (pair.length!=2)
				continue;
			if ("username".equals(pair[0])) {
				try {
					return URLDecoder.decode(pair[1], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					return null;
				}
			}
		}
		return null;
	}
	
	/**
	 * Broadcasts to all connected users and robots the event of one player's name change
	 */
	public void broadcastNameChanged(GamePlayer player,String previousName,String newName) throws Exception {
		if (context==null || context.getWebSocketPool()==null)
			return;
		StringBuilder message = new StringBuilder();
		message.append("{\"changename\":{");
		message.append("\"oldname\":");
		message.append(JSONUtils.quote(previousName));
		message.append(",\"newname\":");
		message.append(JSONUtils.quote(newName));
		message.append("}}");
		String excludePath = RoboToyServerContext.getWSPathWithPlayerName(newName); // avoid sending message back to the same player that got his name changed
		context.getWebSocketPool().sendMessageAll(message.toString(),excludePath);
	}

	/**
	 * Broadcasts to all connected users (but not robots) the event of a new robot entering the game
	 */
	public void broadcastNewRobot(GameRobot robot) throws Exception {
		if (context==null || context.getWebSocketPool()==null)
			return;
		StringBuilder message = new StringBuilder();
		message.append("{\"newrobot\":");
		message.append(RobotSummary.getRobotInfo(robot));
		message.append("}");
		Set<String> player_references = context.getWebSocketReferencesForPlayers();
		context.getWebSocketPool().sendMessageAll(message.toString(),player_references);
	}
	
	/**
	 * Broadcasts to all connected users (but not robots) the event of a new identification for
	 * existing robot (i.e. with the same address).
	 */
	public void broadcastNewRobotIdentification(GameRobot robot) throws Exception {
		if (context==null || context.getWebSocketPool()==null)
			return;
		StringBuilder message = new StringBuilder();
		message.append("{\"newid\":");
		message.append(RobotSummary.getRobotInfo(robot));
		message.append("}");
		Set<String> player_references = context.getWebSocketReferencesForPlayers();
		context.getWebSocketPool().sendMessageAll(message.toString(),player_references);		
	}

	/**
	 * Broadcasts to all connected users some command given its parameter. May also broadcasts
	 * to other robots if command suports robot broadcasting and if parameter 'mayIncludeRobots' is true.
	 */
	public <T> void broadcastCommand(CommandWithBroadcast<T> command,T object,boolean mayIncludeRobots) throws Exception {
		if (context==null || context.getWebSocketPool()==null)
			return;
		String message = command.getBroadcastMessage(context,object);
		String excludePath = command.getBroadcastExcludePath(object);
		if (mayIncludeRobots && command.hasBroadcastToRobots()) {
			GameRobot local_robot = context.getGame().findLocalRobot();
			String excludeSelf = (local_robot==null) ? null : RoboToyServerContext.getWSPathWithRobotIdentifier(local_robot.getIdentifier());
			context.getWebSocketPool().sendMessageAll(message.toString(),excludeSelf,excludePath);
		}
		else {
			Set<String> player_references = context.getWebSocketReferencesForPlayers();
			if (excludePath!=null)
				player_references.remove(excludePath);
			context.getWebSocketPool().sendMessageAll(message.toString(),player_references);
		}		
	}
	
	/**
	 * Tries to listen again to this robot once it broadcasts back to us
	 */
	public void trackRobot(GameRobot robot) {
		if (autoDiscoveryCallback!=null && robot.getAddress()!=null) {
			autoDiscoveryCallback.removeKnownAddress(robot.getAddress().getHostAddress());
		}
	}
	
	/**
	 * Schedules a heartbeat system so that we keep connections alive to other robots
	 */
	public void startHeartBeats(ScheduledExecutorService scheduler) {
		scheduler.scheduleWithFixedDelay(this::sendHeartBeats, HEARTBEATS_DELAY_MS, HEARTBEATS_DELAY_MS, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Send heartbeats from us to other robots (we don't need to do this to other 'human players' because
	 * we will probably be sending other types of messages to them anyway).
	 */
	public void sendHeartBeats() {
		// Send heartbeats to all known robots
		for (GameRobot robot:context.getGame().getRobots()) {
			if (robot.getAddress()==null)
				continue; // this is the local robot (do not need heartbeats from ourselves)
			try {
				WebSocketActiveSession handler = context.findRobotWebSocketSession(robot.getIdentifier());
				int port = (handler!=null) ? handler.getRemotePort() : 0;
				context.getWebSocketPool().sendMessage(robot.getAddress().getHostAddress(), port,
					String.valueOf(RoboToyServerController.HEARTBEAT),
					()->{
						// on success... just ignore it
					},
					(e)->{
						// on failure... just ignore it
					});
			} catch (Exception e) {
				// on failure... just ignore it
			}
		}
	}
	
	/**
	 * Schedules a PING system so that we monitor time to travel to connected players in LOBBY screen
	 */
	public void startPingPlayers(ScheduledExecutorService scheduler) {
		scheduler.scheduleWithFixedDelay(this::sendPings, PING_DELAY_MS, PING_DELAY_MS, TimeUnit.MILLISECONDS);
	}

	/**
	 * Send PING messages from us to connected players as long as we stay at INIT or PLAY game stages.
	 */
	public void sendPings() {
		if (!GameStage.INIT.equals(context.getGame().getStage())
			&& !GameStage.PLAY.equals(context.getGame().getStage()))
			return;
		if (context.getWebSocketPool()==null)
			return;
		for (GamePlayer player:context.getGame().getPlayers()) {
			if (!player.isOnline())
				continue;
			if (player.getAddress()==null)
				continue;
			if (!context.getWebSocketPool().hasActiveSession(player.getAddress()))
				continue; // must be connected directly to us (not through another RoboToy)
			try {
				CmdPing.pingPlayer(player, context);
			}
			catch (Throwable e){ }
		}
	}

}

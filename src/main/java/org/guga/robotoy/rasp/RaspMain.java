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
package org.guga.robotoy.rasp;

import java.io.File;
import java.security.KeyStore;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.guga.robotoy.rasp.admin.DebugWebInterface;
import org.guga.robotoy.rasp.camera.RPICamera;
import org.guga.robotoy.rasp.controller.RoboToyConsole;
import org.guga.robotoy.rasp.controller.RoboToyServerController;
import org.guga.robotoy.rasp.game.GamePlayMode;
import org.guga.robotoy.rasp.game.GameRobot;
import org.guga.robotoy.rasp.game.GameStage;
import org.guga.robotoy.rasp.game.GameStart;
import org.guga.robotoy.rasp.game.GameState;
import org.guga.robotoy.rasp.motor.ArduinoController;
import org.guga.robotoy.rasp.motor.MotorShield;
import org.guga.robotoy.rasp.network.RoboToyAccessPoint;
import org.guga.robotoy.rasp.network.Server;
import org.guga.robotoy.rasp.network.SocketServer;
import org.guga.robotoy.rasp.network.WebServer;
import org.guga.robotoy.rasp.optics.LedColor;
import org.guga.robotoy.rasp.optics.RGBLed.DiodeType;
import org.guga.robotoy.rasp.tags.ErrorsTag;
import org.guga.robotoy.rasp.utils.CryptoUtils;
import org.guga.robotoy.rasp.utils.PropertiesUtils;

/**
 * Main class for starting everything up.<BR>
 * <BR>
 * The command line may include some additional optional arguments:<BR>
 * <UL>
 * <LI><B>-config {filename}</B>   Indicates an alternative filename used for configuration. 
 * If it's not provided, will try to locate a file named 'config.properties' in 'conf' 
 * directory of the current application directory.</LI>
 * <LI><B>-web</B>	Overrides 'server.option' property with 'web' option.</LI>
 * <LI><B>-socket</B>	Overrides 'server.option' property with 'socket' option.</LI>
 * <LI><B>-control {arduino|raspberry|none}</B>   Overrides 'motor.control' property with the given option.</LI>
 * <LI><B>-stage {play|summary}</B>   Skips to a given game stage ('play' or 'summary').</LI>
 * <LI><B>-port {number}</B>   Overrides 'server.port' property with given port number.</LI>
 * <LI><B>-dontredirect</B>   Overrides 'server.redirect' property with 'false' value</LI>
 * <LI><B>-dontredirecterrors</B>   Overrides 'client.redirect' property with 'false' value</LI>
 * <LI><B>-nostats</B>  Overrides 'server.statistics' property with 'false' value</LI>
 * <LI><B>-stats</B>  Overrides 'server.statistics' property with 'true' value</LI>
 * <LI><B>-auto_ap</B>   Overrides 'auto.hostap' property with 'true' value</LI>
 * <LI><B>-no_gpio</B>   Disables use of GPIO by this application</LI>
 * <LI><B>-console</B>   Enable an interactive console for issuing commands for debugging and testing this RoboToy while it's running</LI>
 * </UL>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class RaspMain {
	
	private static final Logger log = Logger.getLogger(RaspMain.class.getName());
	
	private static final String DEFAULT_CONFIG_FILENAME = "config.properties";

	private static final String WEB_RESOURCES_PACKAGE_NAME = "/org/guga/robotoy/rasp/res";
	
	private static final String ARG_CONFIG = "-config";
	private static final String ARG_WEB = "-web";
	private static final String ARG_SOCKET = "-socket";
	private static final String ARG_CONTROL = "-control";
	private static final String ARG_STAGE = "-stage";
	private static final String ARG_PORT = "-port";
	private static final String ARG_DONT_REDIRECT = "-dontredirect";
	private static final String ARG_DONT_REDIRECT_ERRORS = "-dontredirecterrors";
	private static final String ARG_DONT_TAKE_STATISTICS = "-nostats";
	private static final String ARG_TAKE_STATISTICS = "-stats";
	private static final String ARG_AUTO_AP = "-auto_ap";
	private static final String ARG_NO_GPIO = "-no_gpio";
	private static final String ARG_CONSOLE = "-console";
	
	private static final String DEFAULT_SSL_CERT_NAME = "robotoy.local";	
	private static final String DEFAULT_SSL_KEYSTORE_ALIAS = "robotoy";	
	private static final String DEFAULT_SSL_KEYSTORE_PASSWORD = "S3cr3tK3y";
	
	/**
	 * Options for controlling robot's motors (configuration property 'motor.control')
	 * @author Gustavo Figueiredo
	 */
	public static enum ControlOption {
		NONE,
		RASPBERRY,
		ARDUINO;
	}

	/**
	 * Options for server implementation (configuration property 'server.option')
	 * @author Gustavo Figueiredo
	 */
	public static enum ServerOption {
		WEB,
		SOCKET;
	}

	/**
	 * Main application startup method
	 * @param args Some optional arguments documented in this class
	 */
	public static void main(String[] args) {

        System.out.println("Robotoy Server");
        
        // Prefer IPv4 in order to render proper address numbers (such as in JSP pages)
        System.setProperty("java.net.preferIPv4Stack", "true");
        
        // Print provided arguments
        if (args!=null && args.length>0) {
        	System.out.println("Arguments: ");
        	for (String arg:args) {
        		System.out.println("\t"+arg);
        	}
        	System.out.println();
        }
        
        // Load configuration file
        String config_file = getArgument(args,ARG_CONFIG);
        if (config_file==null) {
        	File ref = new File(RaspMain.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        	if (ref.isFile())
        		ref = ref.getParentFile();
        	final File parentDir = ref.getParentFile();
        	final File confDir = new File(parentDir,"conf");
        	File default_config_file = new File(confDir,DEFAULT_CONFIG_FILENAME);
        	if (default_config_file.exists()) {
        		config_file = default_config_file.getAbsolutePath();
        		System.out.println("Loading default config properties at "+config_file);
        	}
        }
        else {
    		System.out.println("Loading config properties at "+config_file);
        }
        Properties config;
        if (config_file!=null && config_file.length()>0)
        	config = PropertiesUtils.getProperties(config_file);
        else
        	config = null;
        
        // Instantiates application objects
        
        GameState game = new GameState();
        GameRobot local_robot = GameRobot.newLocalRobot(GameRobot.getHardwareIdentifier());
        game.addRobot(local_robot);
        
        ControlOption motorControl = ControlOption.ARDUINO;
        if (hasArgument(args,ARG_CONTROL)) {
        	String motorControlProp = getArgument(args,ARG_CONTROL);
        	if (motorControlProp==null)
        		throw new RuntimeException("Missing argument for "+ARG_CONTROL);
        	motorControl = ControlOption.valueOf(motorControlProp.toUpperCase());
        }
        else if (config!=null) {
        	String motorControlProp = config.getProperty("motor.control");
        	if (motorControlProp!=null) {
        		motorControl = ControlOption.valueOf(motorControlProp.toUpperCase());
        	}
        }
        
        if (config!=null) {
        	String cameraPortProp = config.getProperty("camera.port");
        	if (cameraPortProp!=null && cameraPortProp.trim().length()>0)
        		RPICamera.DEFAULT_PORT = Integer.valueOf(cameraPortProp);
        }
        
        RoboToyServerController controller = new RoboToyServerController(game);
        if (ControlOption.ARDUINO.equals(motorControl)) {
        	System.out.println("Setting up Arduino as intermediate controller for the motor drive");
        	ArduinoController.Config arduinoConfig = null;
        	if (config!=null) {
        		arduinoConfig = loadArduinoConfig(config);
        	}
        	controller.setArduinoController(arduinoConfig);
        }
        else if (ControlOption.NONE.equals(motorControl)) {
        	System.out.println("Setting up dummy motor drive");
        	controller.setDummyController();        	
        }
        else if (ControlOption.RASPBERRY.equals(motorControl)){
        	System.out.println("Setting up Raspberry as direct controller for the motor drive");
        	MotorShield.PinLayout pinLayout = null;
        	if (config!=null) {
        		pinLayout = loadMotorShieldConfig(config);
        	}
        	controller.setRaspberryController(pinLayout);
        }
        else {
        	throw new UnsupportedOperationException("Not implemented motor.control:"+config.getProperty("motor.control"));
        }
        
        if (!hasArgument(args,ARG_NO_GPIO) && config!=null) {

        	// IR Led settings
        	String prop = config.getProperty("ir.pinEmitter");
        	if (prop!=null && prop.trim().length()>0)
        		controller.setPinBeamDevice(prop);
        	
        	// IR detector settings
        	prop = config.getProperty("ir.pinDetectors");
        	if (prop!=null && prop.trim().length()>0) {
        		String[] pins = prop.split(",");
        		controller.setPinDetectorDevices(pins);
        	}
        	prop = config.getProperty("ir.pinDetector.resistor");
        	if (prop!=null && prop.trim().length()>0) {
        		controller.setPinDetectorResistor(prop);
        	}
	        
        	// RGB Led settings
        	String red = config.getProperty("rgb.pinRed");
        	String green = config.getProperty("rgb.pinGreen");
        	String blue = config.getProperty("rgb.pinBlue");
        	String common = config.getProperty("rgb.common");
        	if (red!=null && red.trim().length()>0 
        		&& green!=null && green.trim().length()>0 
        		&& blue!=null && blue.trim().length()>0
        		&& common!=null && common.trim().length()>0) {
        		
        		DiodeType dt;
        		if (common.startsWith("A") || common.startsWith("a"))
        			dt = DiodeType.ANODE_COMMON;
        		else if (common.startsWith("C") || common.startsWith("c"))
        			dt = DiodeType.CATHODE_COMMON;
        		else
        			throw new RuntimeException("Invalid value for 'rgb.common': "+common);
        		
    	        controller.setRGBLed(dt, red, green, blue);
        	}
	        	   
        	// RFID settings
    		String pin_rst = config.getProperty("rfid.pinReset");
    		if (pin_rst!=null && pin_rst.trim().length()>0) {
    			String cs = config.getProperty("rfid.cs");
    			controller.setRFID(pin_rst, cs);
    		}

        }
        
        try {
        	controller.init();
        }
        catch (Throwable e) {
        	log.log(Level.SEVERE, "Error while initializing RoboToy Controller", e);
        }

        ServerOption serverOption = ServerOption.WEB;
        if (config!=null) {
        	String serverOptionProp = config.getProperty("server.option");
        	if (serverOptionProp!=null) {
        		serverOption = ServerOption.valueOf(serverOptionProp.toUpperCase());
        	}
        }
        Server server;
        if (ServerOption.WEB.equals(serverOption) || hasArgument(args,ARG_WEB)) {
        	System.out.println("Setting up Web server");
        	boolean dont_redirect = (hasArgument(args,ARG_DONT_REDIRECT)
        			|| (config!=null && "false".equalsIgnoreCase(config.getProperty("server.redirect"))));
        	boolean dont_redirect_client = (hasArgument(args,ARG_DONT_REDIRECT_ERRORS)
        			|| (config!=null && "false".equalsIgnoreCase(config.getProperty("client.redirect"))));
        	server= new WebServer((dont_redirect)?null:controller,
        			controller.getContext().getWebSocketPool(),
        			WEB_RESOURCES_PACKAGE_NAME,
        			new File(new File(System.getProperty("java.io.tmpdir")),"robotoy").getAbsolutePath());
        	((WebServer)server).addServletHandlerAttribute("game", game);
        	((WebServer)server).addServletHandlerAttribute("controller", controller);
        	((WebServer)server).setDefaultCssPackageName(WEB_RESOURCES_PACKAGE_NAME+"/css");
        	((WebServer)server).setDefaultImagesPackageName(WEB_RESOURCES_PACKAGE_NAME+"/images");
        	((WebServer)server).setDefaultJsPackageName(WEB_RESOURCES_PACKAGE_NAME+"/js");        	
        	((WebServer)server).setCustomRESTfulService(new DebugWebInterface(controller, server));
        	((WebServer)server).setWebSocketContext("/ws/");
        	
    		String keystoreInMemoryPassword = DEFAULT_SSL_KEYSTORE_PASSWORD;
    		try {
    			KeyStore keyStore =
    			CryptoUtils.genKeyStoreWithSelfSignedCert(DEFAULT_SSL_CERT_NAME, CryptoUtils.DEFAULT_KEY_ALGORITHM, CryptoUtils.DEFAULT_KEY_SIZE, 
    					/*days*/365, CryptoUtils.DEFAULT_SIGNATURE_ALGORITHM, keystoreInMemoryPassword.toCharArray(), 
    					/*keystoreAlias*/DEFAULT_SSL_KEYSTORE_ALIAS);
    			((WebServer)server).setSSLKeyStore(keyStore, keystoreInMemoryPassword.toCharArray());
            	((WebServer)server).setDaemonPortSecure(WebServer.DEFAULT_PORT_SECURE);
    		}
    		catch (Throwable e) {
    			log.log(Level.SEVERE,"Error while generating self signed certificate for SSL connection",e);
    			return;
    		}
    		
    		if (dont_redirect_client)
    			ErrorsTag.setRedirectErrors(false);
        }
        else if (ServerOption.SOCKET.equals(serverOption) || hasArgument(args,ARG_SOCKET)) {
        	System.out.println("Setting up raw sockets server");
        	server= new SocketServer(controller);
        }
        else {
        	throw new UnsupportedOperationException("Not implemented server.option:"+config.getProperty("server.option"));
        }
        
    	String port_argument = getArgument(args,ARG_PORT);
    	if (port_argument==null && config!=null) {
    		port_argument = config.getProperty("server.port");
    	}
    	if (port_argument!=null) {
    		int port_number = Integer.parseInt(port_argument);
    		server.setDaemonPort(port_number);
    	}
    	if (server instanceof WebServer) {
    		String port_secure_argument = (config!=null) ? config.getProperty("server.secure.port") : null;
    		if (port_secure_argument!=null) {
    			server.setDaemonPortSecure(Integer.parseInt(port_secure_argument));
    		}
    	}
    	        
        String stage_argument = getArgument(args,ARG_STAGE);
        if ("play".equalsIgnoreCase(stage_argument)) {
    		GameStart.startGame(game);
        }
        else if ("summary".equalsIgnoreCase(stage_argument)) {
        	game.setStage(GameStage.SUMMARY);
        }
        
        if (hasArgument(args,ARG_DONT_TAKE_STATISTICS)) {
        	controller.getContext().setTakeStatistics(false);
        }
        else if (hasArgument(args,ARG_TAKE_STATISTICS)) {
        	controller.getContext().setTakeStatistics(true);
        }
        else if (config!=null && "true".equalsIgnoreCase(config.getProperty("server.statistics"))) {
        	controller.getContext().setTakeStatistics(true);
        }
        else {
        	controller.getContext().setTakeStatistics(false);
        }
        
        server.addShutdownHook();
        server.setOnCloseListener(controller.new AutoParkOnDisconnection());
        
        // Make this bot discoverable by others and make it recognize others
        server.setOnStartCallback(()->{controller.startAutoDiscoverService(server.getDaemonPort(),server.getDaemonPortSecure());});
        
        if (config!=null) {
        	String hostap_ssid = config.getProperty("auto.hostap.ssid");
        	if (hostap_ssid!=null && hostap_ssid.trim().length()>0) {
        		RoboToyAccessPoint.setDefaultAPName(hostap_ssid);
        	}
        	String hostap_passwd = config.getProperty("auto.hostap.password");
        	if (hostap_passwd!=null && hostap_passwd.trim().length()>0) {
        		RoboToyAccessPoint.setDefaultAPPassword(hostap_passwd);
        	}
        	String admin_user = config.getProperty("admin.user");
        	if (admin_user!=null) {
        		if (admin_user.trim().length()==0)
        			controller.setAdminUserName(null);
        		else
        			controller.setAdminUserName(admin_user);
        	}
        }
        
        if (config!=null) {
        	String playmode = config.getProperty("playmode");
        	if (playmode!=null && playmode.trim().length()>0) {
        		// Change current mode in context global variable
        		GamePlayMode m = GamePlayMode.valueOf(playmode.toUpperCase());
        		controller.getContext().setGamePlayMode(m);
        		if (GamePlayMode.STANDALONE.equals(m)) {
	        		// Once it started standalone mode, signal this situation flashing lights
	        		if (null!=controller.getContext().getRGBLed()) {
	        			// cycle through colors BLUE and GREEN
	        			controller.getContext().getRGBLed().startCycleColors(600, LedColor.GREEN, LedColor.BLUE);
	        		}
        		}
        	}
        }

        if (hasArgument(args,ARG_AUTO_AP) 
        		|| (config!=null && "true".equalsIgnoreCase(config.getProperty("auto.hostap")))) {
        	Thread t = new Thread(()->{
        		RoboToyAccessPoint.checkAndBecomeAccessPoint(controller,server);
        	});
        	t.setName("RoboToyAccessPointThread");
        	t.setDaemon(true);
        	t.start();
        }
        
        if (hasArgument(args,ARG_CONSOLE)) {
        	new RoboToyConsole(server,controller).start();
        }

        server.run();
	}

	/**
	 * Check if a given argument exists in the arguments array
	 */
	public static boolean hasArgument(String[] args,String option) {
		if (args==null || args.length==0)
			return false;
		for (String arg:args) {
			if (arg.equals(option))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks arguments passed as an array for a given argument name
	 * and returns the subsequent argument string.
	 * @param args Arguments as an array
	 * @param option Argument to search
	 */
	public static String getArgument(String[] args,String option) {
		if (args==null || args.length==0)
			return null;		
		for (int i=0;i<args.length;i++) {
			String arg = args[i];
			if (arg.equals(option)) {
				if (i+1<args.length)
					return args[i+1];
				else
					return "";
			}
			else if (arg.startsWith(option+"=")) {
				String param = arg.substring(option.length()+1).trim();
				return param;
			}
		}
		return null;
	}
	
	/**
	 * Loads configuration options in configuration properties file used for controlling
	 * Arduino which in turn will control motors.
	 */
	private static ArduinoController.Config loadArduinoConfig(Properties config) {
		ArduinoController.Config arduinoConfig = new ArduinoController.Config();
		String baudrate = config.getProperty("motor.baudRate");
		if (baudrate!=null)
			arduinoConfig.setBaudrate(Integer.parseInt(baudrate));
		String databits = config.getProperty("motor.dataBits");
		if (databits!=null)
			arduinoConfig.setDatabits(Integer.parseInt(databits));
		String stopbits = config.getProperty("motor.stopBits");
		if (stopbits!=null)
			arduinoConfig.setStopbits(Integer.parseInt(stopbits));
		String parity = config.getProperty("motor.parity");
		if (parity!=null)
			arduinoConfig.setParity(Integer.parseInt(parity));
		return arduinoConfig;
	}
	
	/**
	 * Loads configuration options in configuration properties file used for controlling
	 * a Motor Driver Shield directly connected to the Raspberry GPIOs.
	 */
	private static MotorShield.PinLayout loadMotorShieldConfig(Properties config) {
		MotorShield.PinLayout pinLayout = new MotorShield.PinLayout();
		String pinMotorLatch = config.getProperty("motor.pinMotorLatch");
		if (pinMotorLatch!=null)
			pinLayout.setPinMotorLatch(pinMotorLatch);
		String pinMotorClock = config.getProperty("motor.pinMotorClock");
		if (pinMotorClock!=null)
			pinLayout.setPinMotorClock(pinMotorClock);
		String pinMotorEnable = config.getProperty("motor.pinMotorEnable");
		if (pinMotorEnable!=null)
			pinLayout.setPinMotorEnable(pinMotorEnable);
		String pinMotorData = config.getProperty("motor.pinMotorData");
		if (pinMotorData!=null)
			pinLayout.setPinMotorData(pinMotorData);
		String pinPWM1 = config.getProperty("motor.pinPWM1");
		if (pinPWM1!=null)
			pinLayout.setPinPWM1(pinPWM1);
		String pinPWM2 = config.getProperty("motor.pinPWM2");
		if (pinPWM2!=null)
			pinLayout.setPinPWM2(pinPWM2);
		String pinPWM3 = config.getProperty("motor.pinPWM3");
		if (pinPWM3!=null)
			pinLayout.setPinPWM3(pinPWM3);
		String pinPWM4 = config.getProperty("motor.pinPWM4");
		if (pinPWM4!=null)
			pinLayout.setPinPWM4(pinPWM4);
		String pwmType = config.getProperty("motor.pwmType");
		if (pwmType!=null)
			pinLayout.setPwmType(pwmType);
		String wheels = config.getProperty("motor.wheels");
		if (wheels!=null)
			pinLayout.setWheels(wheels);
		String reversedMotorsDirection = config.getProperty("motor.reversedMotorsDirection");
		if (reversedMotorsDirection!=null)
			pinLayout.setReversedMotorsDirection(Boolean.parseBoolean(reversedMotorsDirection));
		String motorFrontLeft = config.getProperty("motor.frontLeft");
		if (motorFrontLeft!=null)
			pinLayout.setMotorFrontLeft(Integer.parseInt(motorFrontLeft));
		String motorFrontRight = config.getProperty("motor.frontRight");
		if (motorFrontRight!=null)
			pinLayout.setMotorFrontRight(Integer.parseInt(motorFrontRight));
		String motorRearLeft = config.getProperty("motor.rearLeft");
		if (motorRearLeft!=null)
			pinLayout.setMotorRearLeft(Integer.parseInt(motorRearLeft));
		String motorRearRight = config.getProperty("motor.rearRight");
		if (motorRearRight!=null)
			pinLayout.setMotorRearRight(Integer.parseInt(motorRearRight));
		return pinLayout;
	}
}

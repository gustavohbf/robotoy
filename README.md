# RoboToy

RoboToy is a game with real robots that you can control and watch through a mobile device or a personal computer. 
You can think about it as some kind of 'first person shooter' style of game with real world graphics. In the place of some 3d rendered scene you will see video streamed by a mounted camera on your real world robot. Whenever you press the movement buttons, your robot will respond accordingly. Whenever you 'shoot' your 'cannon' using on screen game pad, a LED cannon mounted on your robot will flash and hopefully will hit an amplified light detector mounted on another robot. Player controllers and robots communicates with each other through an existent LAN or through an Access Point setup in one of the robots. Basically this is the general game idea.

## Home Page

You can find more information at the [RoboToy Home Page](http://gustavohbf.github.io/robotoy).


## Collaboration

Please feel free to contribute with any ideas about this project. For example, it would be nice to have a good looking 'chassis' for the robots. E.g. something like 'Dalek' (from 'Dr. Who') would be awesome.


## Ongoing development

The following items should be developed sometime in future:

 * Better looking pages in game.

 * Provide movement sensors in each robot (such as gyros and accelerometers) and use it with some control algorithm (such as PID).
 
 * Implement different alternative game goals (e.g. 'capture the flag').


## Build from source

 First you should download RoboToy source code from GITHub. From command line you can run this if you already have 'git' package installed:
 
	git clone https://github.com/gustavohbf/robotoy.git


 After this, you should 'build' the binaries from the source. If you are running it through some *nix (such as Raspbian), just run this:
 
	cd robotoy
	chmod +x gradlew
	./gradlew build
 	 
 You must have a Internet connection for this process to complete. This can take a while since all required third-party libraries used by RoboToy will be downloaded automatically before building it. If you don't have 'gradle' installed yet, this script will take care of this.
 
 After it completes, you should have the following alternative archives with the compiled binaries and its dependencies included (you only need one of these, preferably the 'tar.gz' one):
 
	./build/distributions/robotoy-1.0.zip			-> ZIP file
	./build/distributions/robotoy-1.0.tar.gz		-> TAR.GZ file

Please note the version number may be different from the example above.


## RoboToy Installation

 You should first make sure that you have installed all the required packages in Raspbian before installing RoboToy.
 
 This means you should get all these packages installed and working (look at Wiki sections for more detailed information about these procedures):
 
 * Java JDK 1.8
 * Raspberry Pi Camera
 * UV4L + WebRTC
 * WiringPi + Pi4J
 * RXTXcomm (unless you are not using Arduino with Raspberry Pi for controlling motors)
 
 These packages are optional for the purpose of running RoboToy, but are highly recommended:
 
 * HostAPD (make it possible to turn your Robot into a Wireless Access Point)
 * DNS Masq (usually it's used with hostapd for providing DHCP and DNS support)
 * Avahi Daemon (make it easy to find you robot in your local network)
 
 Note: I've only tested it with Raspbian OS so far.
 
 Of course, you should also have a network interface on your Raspberry up and running and have a SSH terminal for doing all the configurations. It's recommended that you do this wirelessly, since you probably don't want cables attached to your robot once it starts moving around.
 
 The 'hardware' part of RoboToy should also be assembled before installing the 'software' part. There may be different robot versions with different parts and with the proper adjustments you could make use of the same source code. These instructions applies to the default 'robot setup': the robot model '**Alpha**'.  
 
 '**Alpha**' robot model hardware parts (look at Wiki pages for a more elaborated list of parts):
 
 * DC motors + wheels
 * Motor driver
 * Arduino (optional, but makes the full setup more 'clean')
 * RGB led (for easy identification of one robot among other robots of the same model)
 * IR emitter LED and IR detectors set plus needed components
 * External batteries for powering everything (motors and Raspberry)
 * WiFi USB dongle (optional since Raspberry model 3 already have one internal)
 * Mouting parts (3d-printed parts or just a bunch of scraps)
 * Screws (M2.5 and M3)
 * Wires (jumpers, USB cables, etc.)
 * MFRC522 RFID module + tags (optional, but it's good for providing 'power-ups' during gameplay)
 
 From this point on, we assume all the required external packages have been already installed.
	
 Unpack and expand RoboToy binary package to your Raspberry '/usr/local' directory. It should be performed as root.
  
<pre>
	sudo tar -zxvf robotoy-1.0.tar.gz -C /usr/local
</pre>

 This should create a directory called /usr/local/robotoy with the following contents:
 
<pre>
	/usr/local/robotoy
		/bin/
		    checkup.sh -> Script for checking system prerequisites
			RoboToy		-> *nix script file for starting RoboToy manually
			RoboToy.bat	-> Windows file, you may ignore this
			robotoy_service.sh -> Script in System-V style for running RoboToy as a service
		/conf/
			config.properties 	-> Several configurations about RoboToy
			logging.properties	-> Logging configurations
		/lib/
			RoboToy-1.0.jar		-> RoboToy Java library
			*.jar					-> Several third-party Java libraries
</pre>

  Change configurations as needed. The default configuration file is located at:
  
<pre>
	/usr/local/robotoy/config.properties
</pre>

  Before running RoboToy script you may want to run the 'checkup.sh' script located in 'bin' expanded directory.
  
<pre>
    /usr/local/robotoy/bin/checkup.sh
</pre>

  This script will check if your system satisfies all RoboToy requirements (e.g.: Java version, other packages, etc.). If any item is shown as 'FAIL', you should review the installation steps described in software installation page in RoboToy Wiki. 
  
  Test if everything is ok by running RoboToy script as root.
  
<pre>
	sudo /usr/local/robotoy/bin/RoboToy
</pre>

  There should be some messages displayed on console. 
  The amount of messages displayed depends on the logging configuration done in 'logging.properties'.
  
  Some output like this is considered normal:
  
<pre>
	Robotoy Server
	Loading default config properties at /usr/local/robotoy/conf/config.properties
	Setting up Arduino as intermediate controller for the motor drive
	Starting serial communication with /dev/ttyUSB0, baud rate 115200, 8 data bits, 1 stopbits
	etc.
	Setting up Web server
	etc.
</pre>

  There should not be anything like this (look for phrases starting with 'Exception in thread' expressions):
  
<pre>
	Exception in thread "main" java.lang.NullPointerException
        at org.guga.... etc
</pre>

  The RGB led should be turned on. If it's not, there might be a problem with the service or with the hardware wiring.
  
  You should be able to connect to RoboToy using the following URL in any computer on the same network:
  
<pre>
	http://robotoy.local
</pre>

  Note: if you got a 'page not found' error, check these:
  
  a) The Raspberry Pi should have been configured with a mDNS (Multicast-DNS) service (see RoboToy wiki about this)
  
  b) The Raspberry Pi hostname should match the one you entered in URL address (I'm assuming it's called 'robotoy').
  
  c) Your computer should be able to resolve mDNS names. For example, if you are testing from a Windows machine, you should install some third-party mDNS client implementation, such as 'Bonjour'.
  
  d) You can try again with numerical IP address of Raspberry Pi (whatever it is) just to make sure you don't have another problem beyond name resolution.

  This should bring you the initial screen followed by the login screen.
  
  If everything is ok, you can press CTRL + C in order to close the running service and install it as a 'service' that will run every time after boot.
  
<pre>
	sudo /usr/local/robotoy/bin/robotoy_service.sh install
</pre>
  
  Then you can start it at once running:
  
<pre>
	sudo service robotoy start
</pre>

  Note that this time there is not going to be messages displayed on the console. Every console messages generated by RoboToy service should be redirected to temporary log files located at /tmp. The temporary log filenames will have this signature:
  
<pre>
	/tmp/robotoy.out	-> For standard output generated by the RoboToy service
	/tmp/robotoy.err	-> For standard error generated by the RoboToy service	
</pre>

  The majority of information should be output to '.err' files. If you restart RoboToy service, a new file is created and the previous one is renamed with a 'prev' suffix appended to its name.

>> Please note that usually in Raspbian the /tmp directory is mounted over an 'in memory' file system, what means that everything stored there is lost when Raspberry powers off. If you don't want to use RAM for storing the '/tmp' directory, edit the file **/etc/default/rcS** and change the last line from **RAMTMP=yes** to **RAMTMP=no** and then reboot.
  
  You can use the following commands to visualize these files:
  
  Outputs last 10 lines of LOG files:
  
	tail /tmp/robotoy.out
	tail /tmp/robotoy.err
	
  Outputs last lines of LOG files and keep doing this until you press CTRL+C
  
	tail -f /tmp/robotoy.out
	tail -f /tmp/robotoy.err
	
  Opens 'vi' editor for displaying all file contents (close with `:!q` )
  
	vi /tmp/robotoy.out
	vi /tmp/robotoy.err

>> Please note that the date/time information provided in LOG file may be incorrect eventually. This may happen more often when you start it for the first time after turning it on because Raspberry Pi lacks a 'real time' circuitry. It needs an Internet connection in order to keep the current date and time updated in memory, and this may not happen before the RoboToy service is initialized at system startup.


## RoboToy Stop and Removal

 If you have installed RoboToy as a service, you can stop it running the following command line:
 
	sudo service robotoy stop
 
 If you want also to remove it from the startup process, you can do it using the following command lines:

	sudo update-rc.d -f robotoy remove
	sudo rm /etc/init.d/robotoy
 
 
## RoboToy Architecture

This project is made of some **software** parts and some **hardware** parts. 

The RoboToy software is powered by these third-party technologies:
  * Java
  * HTML5
  * JavaScript
  * CSS
  * JSP
  * JSP Expression Language (EL)
  * JSTL
  * jQuery
  * DataTables (js)
  * Arduino C language

External RoboToy Java Library dependencies:
(note: these dependencies are automatically resolved with the 'Gradle' build)

  * Jetty (http://www.eclipse.org/jetty/)  
  * BouncyCastle (http://www.bouncycastle.org/)  
  * GSON - Google JSON (http://github.com/google/gson)  
  * Apache Commons Lang (http://commons.apache.org/proper/commons-lang/)  
  
External RoboToy dependencies (installation required, see Wiki):

  * WiringPi + Pi4J (http://pi4j.com/)  
  * UV4L + WebRTC (http://www.linux-projects.org/uv4l/)  
  * RXTX Serial Communication (http://rxtx.qbang.org/)
  * SPI bus (Serial Peripheral Interface)
  
The (suggested) RoboToy hardware (**Alpha** robot model) is powered by:

  * Raspberry Pi 3 B
  * Arduino Uno r3
  * Raspberry Pi Camera (model 1.3 or 2)
  * 3D printed parts (e.g. the 'turret', see wiki)
  * DC motors
  * Motor Shield (H bridge)
  * LEDs, sensors and some other components
  * battery (USB power bank)
  * optional USB WiFi dongle
  * MFRC522 RFID module
 
## RoboToy Modules

These are briefly the RoboToy software parts implemented in this project:

**WebServer**

Robotoy uses an embedded JETTY web server for distribution of its graphical user interface and also provides 'web-sockets' communication between robots and between players. There is no need for an external server. Each robot is a server on its own right and is capable of synchronizing its information with others.

These are the features implemented in this module:

* Listen to HTTP and HTTPS ports and provides a web context with all resources embedded in the application (JSP, JS, TLD, CSS, MP3, PNG).
* Listen to web sockets calls and keep a pool of connections for syncronization of game internal state among players and robots.
* Provides a simple RESTful interface for debugging purpose. 

**Auto-Discovery Engine**

Each Robotoy is capable of recognizing other Robotoy in the same LAN automatically. There is no need to pre-program a list of available RoboToys. You can add more RoboToys to your game as needed, but be aware that each one will be streaming video on the same LAN, increasing network traffic. I guess that 8 robots should be enough (you need at least 2 robots). If you want more robots, you will have to tweak some internal parts of the code.

These are the features implemented in this module:

* Broadcasts to the local network a multicast message informing about the robot existence.
* Acknownledges other robot's broadcast messages.

**Motor Driver**

This module controls the robot movements.

These are the features implemented in this module:

* Control robot's DC motors using different approaches: 

a) Delegating the motor control to an Arduino board, which in turn have a serial communication via USB; 

b) Direct wiring between Raspberry's GPIO and an external motor driver board.

* Control robot's speed using PWM (either hardware or software depending on the robot configuration).

**Optical Circuit**

This module is related to the weaponary and shield of each robot. It uses infrared signals with pulse width modulation and a simple protocol to make this game behave like a 'laser tag' game.

These are the features implemented in this module:

* IR with pulse width modulation is used for communication between robot that fires a laser beam and the robot that detects it.
* Additional RGB led used for easy robot identification (it's not part of the targeting system).

**Game Engine**

Several rules related to the game are managed by each robot and the overall game state is synchronized among the robots.

These are the features implemented in this module:

* Holds the full game logic about players, robots and goals.
* Broadcasts all the game logic between participants.

**Graphical User Interface**

This module includes graphics, sound and some special effects displayed during gameplay.
The game can be played by any device that supports HTML5 standard, including smartphones, tablets and computers with not-too-old browsers. Tested with:

  * Google Chrome (version 54)
  * Safari on iPhone 6 (iOS 10)
  * Safari on Ipad 2 (iOS 10)

These are the features implemented in this module:

  * Login screen for entering an user name.
  * Lobby room screen for waiting other players to join and take control of available robots and choosing a color for identifying the robot.
  * While the game is in the 'PLAY' stage, renders the streaming video image and over it renders some additional information, such as the total 'in game' life remaining, robot's WiFi signal strength, gamepad buttons, etc.
  * Motion detection capabilities on the controlling device (e.g. accelerometer and gyroscope) may be used to control the robot, just like an analogic joystick would do (e.g. make it go forward, backward and spinning around).
  * Explosions and laser effects are rendered on the display above the streaming video image.
  * Play sound effects.
  * After the game is over, presents a summary screen with stats.
  * Additional administration pages for debugging and for setup of a new WiFi network.

**RFID**

This module is related to RFID cards and sensors. Each RoboToy is equiped with a RFID module working at 13.56 MHz attached to its base. It's used for detecting some RFID cards that are spread over the playfield. The game will treat these RFID cards in a special way, usually making the robot power up (i.e. restoring life points that it may have lost in a previous fight).

These are the features implemented in this module:

* Scanning for RFID signals that are reflected back from RFID tags.
* Keep and synchronizes status about each RFID tag found during gameplay.
  
  
## Credits

Explosion animation were built with http://www.explosiongenerator.com/.

Animation sprites were built with http://css.spritegen.com/

Audio effects were downloaded from different authors at http://soundbible.com/

Laser sounds:
http://soundbible.com/tags-laser.html
http://soundbible.com/1771-Laser-Cannon.html

Explosion sounds:
http://soundbible.com/107-Bomb-Explosion-1.html
http://soundbible.com/1986-Bomb-Exploding.html

Damage sound:
http://soundbible.com/1793-Flashbang.html

Sound API in Javascript (Howler):
https://github.com/goldfire/howler.js

Canvas Layers in Javascript:
https://bitbucket.org/ant512/canvaslayers/wiki/Home

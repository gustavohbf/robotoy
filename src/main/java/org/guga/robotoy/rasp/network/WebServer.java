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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.guga.robotoy.rasp.utils.IOUtils;
import org.guga.robotoy.rasp.utils.JSONUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Implementation of a web application that can also provide service
 * by means of web sockets.<BR>
 * <BR>
 * This implementation provides support to:<BR>
 * - JSP pages with support to JSTL, EL and TLD<BR>
 * - HTML pages<BR>
 * - JS and CSS files<BR>
 * - image files (PNG, JPG)<BR>
 * - web sockets issued commands<BR>
 * <BR>
 * The required pages will be located in the classpath. Usually inside
 * a JAR file.<BR>
 * All other file types will be blocked.<BR>
 * <BR>
 * You must provide the package name of all resources in constructor.<BR>
 * You also must provide a 'CommandCentral' implementation that will be
 * called by web sockets interface.<BR>
 * <BR>
 * You may optionally provide an implementation to 'AutoRedirection' interface
 * to be able to automatically redirect pages upon requests. Provide this using
 * {@link WebServer#setCustomAutoRedirection(AutoRedirection) setCustomAutoRedirection} method
 * or by making your 'CommandCentral' implements the 'AutoRedirection' interface.<BR>
 * <BR>
 * You may optionally provide some 'attributes' to be set at application scope level
 * using {@link WebServer#addServletHandlerAttribute(String, Object) addServletHandlerAttribute}.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class WebServer implements org.guga.robotoy.rasp.network.Server {

	private static final Logger log = Logger.getLogger(WebServer.class.getName());

	public static final int DEFAULT_PORT = 8089;

	public static final int DEFAULT_PORT_SECURE = 8443;

	/** Default expire date for web cacheable resources */
    private static final long DEFAULT_EXPIRE_TIME = 604800L; // one week in seconds 

	/** The port to listen on. */
	private int daemonPort = DEFAULT_PORT;
	
	/** The secure port to listen on. 0 = no secure port */
	private int daemonPortSecure = 0;
	
	/** Keyword in .JS files that gets overriden by server IP address **/
	private static final String MASK_SERVERHOST = "@SERVERHOST";
			
	/**
	 * Package name for all resources provided by web application.<BR>
	 * Must use '/' character to separate internal directory parts
	 * and must not end with '/'.
	 */
	private final String resourcesPackageName;
	
	/**
	 * Context used to distinguish raw websocket requests from HTTP requests
	 */
	private String webSocketContext;
	
	/**
	 * Directory for storing temporary files generated by JSP servlet.<BR>
	 * Must be used exclusively for this purpose. Do not use a common
	 * directory (e.g. /tmp).
	 */
	private final String workDir;
	
	private Runnable onCloseListener;
	
	/**
	 * JETTY server
	 */
	private Server server;
	
	private final WebSocketClientPool socketsPool;
		
	private AutoRedirection customAutoRedirection;
	
	private CustomRESTfulService customRESTfulService;
	
	private Map<String,Object> servletHandlerAttributes;
	
	private Runnable onStartCallback;
	
	private String defaultJsPackageName;
	
	private String defaultCssPackageName;
	
	private String defaultImagesPackageName;
	
	private char[] sslKeyStorePassword;
	
	private String sslKeyStoreFile;
	
	private KeyStore sslKeyStore;
	
	private WebAppContext webHandler;
		
	public WebServer(AutoRedirection autoRedirect,WebSocketClientPool socketsPool,String resourcesPackageName,String workDir)
	{
		this.socketsPool = socketsPool;
		this.resourcesPackageName = resourcesPackageName;
		this.defaultJsPackageName = this.defaultCssPackageName = this.defaultImagesPackageName = resourcesPackageName;
		this.workDir = workDir;
		if (autoRedirect!=null) {
			setCustomAutoRedirection(autoRedirect);
		}
	}

	/**
	 * Daemon port number this server is listening.
	 */
	@Override
	public int getDaemonPort() {
		return daemonPort;
	}

	/**
	 * Set daemon port number. If you choose 0, it will choose any random available port once it starts running.
	 */
	@Override
	public void setDaemonPort(int daemonPort) {
		this.daemonPort = daemonPort;
	}

	/** 
	 * Set the secure port to listen on. 
	 * 0 = no secure port 
	 */
	@Override
    public int getDaemonPortSecure() {
		return daemonPortSecure;
	}

	/** 
	 * The secure port to listen on.<BR>
	 * 0 = no secure port 
	 */
	@Override
	public void setDaemonPortSecure(int daemonPortSecure) {
		this.daemonPortSecure = daemonPortSecure;
	}
	
	/**
	 * Context used to distinguish raw websocket requests from HTTP requests
	 */
	public String getWebSocketContext() {
		return webSocketContext;
	}

	/**
	 * Context used to distinguish raw websocket requests from HTTP requests
	 */
	public void setWebSocketContext(String webSocketContext) {
		if (webSocketContext!=null && !webSocketContext.startsWith("/"))
			webSocketContext = "/" + webSocketContext;
		this.webSocketContext = webSocketContext;
	}

	public void setSSLKeyStore(String file,char[] password) {
		sslKeyStorePassword = password;
		sslKeyStoreFile = file;
		sslKeyStore = null;
	}

	public void setSSLKeyStore(KeyStore ks,char[] password) {
		sslKeyStorePassword = password;
		sslKeyStore = ks;
		sslKeyStoreFile = null;
	}
	
	public boolean hasSSLKeyStore() {
		return sslKeyStore!=null || sslKeyStoreFile!=null;
	}

	public Runnable getOnCloseListener() {
		return onCloseListener;
	}

	@Override
	public void setOnCloseListener(Runnable onCloseListener) {
		this.onCloseListener = onCloseListener;
	}

	public AutoRedirection getCustomAutoRedirection() {
		return customAutoRedirection;
	}

	public void setCustomAutoRedirection(AutoRedirection customAutoRedirection) {
		this.customAutoRedirection = customAutoRedirection;
	}

	public CustomRESTfulService getCustomRESTfulService() {
		return customRESTfulService;
	}

	public void setCustomRESTfulService(CustomRESTfulService customRESTfulService) {
		this.customRESTfulService = customRESTfulService;
	}

	public void addServletHandlerAttribute(String name,Object value) {
		if (servletHandlerAttributes==null)
			servletHandlerAttributes = new HashMap<>();
		servletHandlerAttributes.put(name, value);
	}

	public Runnable getOnStartCallback() {
		return onStartCallback;
	}

	@Override
	public void setOnStartCallback(Runnable onStartCallback) {
		this.onStartCallback = onStartCallback;
	}

	@Override
	public void addShutdownHook() {
        Thread shutdown_hook = new Thread(this::stopServer);
        shutdown_hook.setName("WebSocketServer");
        shutdown_hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdown_hook);    	
    }

	public String getDefaultJsPackageName() {
		return defaultJsPackageName;
	}

	public void setDefaultJsPackageName(String defaultJsPackageName) {
		this.defaultJsPackageName = defaultJsPackageName;
	}

	public String getDefaultCssPackageName() {
		return defaultCssPackageName;
	}

	public void setDefaultCssPackageName(String defaultCssPackageName) {
		this.defaultCssPackageName = defaultCssPackageName;
	}

	public String getDefaultImagesPackageName() {
		return defaultImagesPackageName;
	}

	public void setDefaultImagesPackageName(String defaultImagesPackageName) {
		this.defaultImagesPackageName = defaultImagesPackageName;
	}

	@Override
	public void run() {
		
		// Thread Pool
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(500);
		server = new Server(threadPool);
		
		// HTTP Configuration
		HttpConfiguration http_config = new HttpConfiguration();
		if (daemonPortSecure>0) {
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "Starting up secure port "+daemonPortSecure);
			http_config.setSecureScheme("https");
			http_config.setSecurePort(daemonPortSecure);
		}
		
		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
		http.setPort(daemonPort);
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Starting up HTTP port "+daemonPort);
		http.setIdleTimeout(Integer.MAX_VALUE);
		server.addConnector(http);

		if (daemonPortSecure>0) {
			setupSSL(server,http_config);
		}
		
		// Servlets Handler
		ServletHandler servletHandler = new ServletHandler();

        // WebSockets Handler
        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                //factory.register(MyWebSocketHandler.class);
            	factory.getPolicy().setIdleTimeout(Integer.MAX_VALUE);
            	factory.setCreator(new WebSocketCreator() {					
					@Override
					public Object createWebSocket(ServletUpgradeRequest arg0, ServletUpgradeResponse arg1) {
						return new WebSocketHandlerImpl(socketsPool);
					}
				});
            }            
        };

        HandlerCollection handlerCollection = new HandlerCollection();

        webHandler = new WebAppContext();
        webHandler.setContextPath("/");
        webHandler.setAttribute("javax.servlet.context.tempdir", workDir);
        webHandler.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
        		".*/[^/]*jstl.*\\.jar$");
        
        // Setup embedded resource path
        URL indexUri = this.getClass().getResource(resourcesPackageName+"/");
        if (indexUri==null)
        	throw new RuntimeException("Could not find resources at "+resourcesPackageName);
        try {
			webHandler.setResourceBase(indexUri.toURI().toASCIIString());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}        
        
        // JSP stuff
        webHandler.setConfigurations(new Configuration[] {
				new AnnotationConfiguration(),
				new WebInfConfiguration(),
				new WebXmlConfiguration(),
				new MetaInfConfiguration(),
				new FragmentConfiguration(),
				new EnvConfiguration(),
				new PlusConfiguration(),
				new JettyWebXmlConfiguration()
				});
        System.setProperty("org.apache.jasper.compiler.disablejsr199", "false");
        webHandler.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
        webHandler.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        webHandler.addBean(new ServletContainerInitializersStarter(webHandler), true);
        webHandler.setClassLoader(new URLClassLoader(new URL[0], WebServer.class.getClassLoader()));
        
        // Other handlers (e.g. RESTful)
        if (servletHandlerAttributes!=null && !servletHandlerAttributes.isEmpty()) {
        	for (Map.Entry<String, Object> entry:servletHandlerAttributes.entrySet()) {
        		webHandler.setAttribute(entry.getKey(), entry.getValue());
        	}
        }
        
        handlerCollection.setHandlers(new Handler[] {servletHandler, wsHandler, webHandler});
        
        server.setHandler(handlerCollection);
        try {
	        server.start();
	        if (daemonPort==0) {
	        	// Random port number
	        	daemonPort = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
	        }
	        if (onStartCallback!=null)
	        	onStartCallback.run();
	        server.join();
        }
        catch (Throwable e) {
        	log.log(Level.SEVERE,"Error starting WebSocket Server",e);
        }
	}
	
	@Override
	public boolean isRunning() {
		return server!=null && server.isRunning();
	}
	
	private static List<ContainerInitializer> jspInitializers() {
		JettyJasperInitializer sci = new JettyJasperInitializer();
		ContainerInitializer initializer = new ContainerInitializer(sci, null);
		List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();
		initializers.add(initializer);
		return initializers;
	}

	private void setupSSL(Server server,HttpConfiguration http_config) {
		SslContextFactory sslContextFactory = new SslContextFactory();
		
		if (sslKeyStoreFile!=null)
			sslContextFactory.setKeyStorePath(sslKeyStoreFile);
		else if (sslKeyStore!=null)
			sslContextFactory.setKeyStore(sslKeyStore);
		else {
			log.log(Level.SEVERE,"Error while configuring SSL connection. Missing KeyStore!");
			return;
		}
		sslContextFactory.setKeyStorePassword(new String(sslKeyStorePassword));
		sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA",
				"SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA",
				"SSL_RSA_EXPORT_WITH_RC4_40_MD5",
				"SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
				"SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
				"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
		HttpConfiguration https_config = new HttpConfiguration(http_config);
		https_config.addCustomizer(new SecureRequestCustomizer());
		ServerConnector sslConnector = new ServerConnector(server,
			new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
			new HttpConnectionFactory(https_config));
		sslConnector.setPort(daemonPortSecure);
		server.addConnector(sslConnector);
	}
	
	/**
	 * Stop the proxy daemon.
	 */
	public void stopServer() {
		if (server!=null) {
			try {
				server.stop();
			}
			catch (Throwable e) {
				log.log(Level.SEVERE,"Error stopping WebSocket Server",e);
			}
		}
		server = null;
		if (onCloseListener!=null) {
			onCloseListener.run();
		}
	}
	
	private class ServletHandler extends AbstractHandler {
		
		/**
		 * In case we need access to the HttpSession object related to a request
		 * in this 'handler', we will refer to the same session found in the 'web application' handler'.
		 */
		private HttpSession getSession(HttpServletRequest request) {

			if (webHandler==null 
					|| webHandler.getSessionHandler()==null 
					|| webHandler.getSessionHandler().getSessionManager()==null)
				return null;
			String id = getSessionId(request);
			if (id==null)
				return null;
			HttpSession session = webHandler.getSessionHandler().getSessionManager().getHttpSession(id);
			return session;
		}

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) 
				throws IOException, ServletException {
			if (log.isLoggable(Level.FINE)) {
				String protocol = baseRequest.getProtocol();
				log.log(Level.FINE,protocol.toUpperCase()+" REQUEST "+request.getRequestURI()+" FROM "+baseRequest.getRemoteAddr());
			}
			String uri = request.getRequestURI();
			final String uri_lc = uri.toLowerCase();
			if ("/".equals(uri) || "/index.html".equalsIgnoreCase(uri) || "/index.htm".equalsIgnoreCase(uri) || "/index.jsp".equalsIgnoreCase(uri)) {
				return; // this request will be handled by 'WebAppContext'
			}
			else if (webSocketContext!=null && uri_lc.startsWith(webSocketContext)) {
				return; // this request will be handled by 'WebSocketHandler'
			}
			else if (uri_lc.endsWith(".html") || uri_lc.endsWith(".htm") || uri_lc.endsWith(".jsp")) {
				RequestedPathParts parts = breakURIParts(uri,resourcesPackageName);
				if (uri_lc.endsWith(".jsp")) {
					WebCacheControl.setNoCache(response);
				}
				if (customAutoRedirection!=null) {				
					String redirection = customAutoRedirection.getHTTPRedirection(request.getRemoteAddr(),request.getRequestURI(),
							request.getQueryString(),
							parts.resourceFullName,
							getSession(request));
					if (redirection!=null) {
						String new_location = getFullURLAddress(request,redirection);
						if (log.isLoggable(Level.FINEST))
							log.log(Level.FINEST, "Redirecting "+baseRequest.getRemoteAddr()+" to "+new_location);
						response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
						response.setHeader("Location", new_location);
						baseRequest.setHandled(true);
						return;
					}
				}
				return; // this request will be handled by 'WebAppContext'
			}
			else if ("/manifest.json".equalsIgnoreCase(uri)) {
				serveTextContents(resourcesPackageName+"/manifest.json","application/manifest+json",baseRequest,request,response,/*cacheable*/false);
				baseRequest.setHandled(true);				
			}
			else if (uri_lc.endsWith(".js")) {
				RequestedPathParts parts = breakURIParts(uri,defaultJsPackageName);
				final long resource_timestamp = IOUtils.getResourceLastModified(WebServer.class, parts.resourceFullName);
				if (resource_timestamp>0 && WebCacheControl.checkCacheTimeBased(request, resource_timestamp)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Requested resource ("+uri+") is already cached and up-to-date!");
					}
					WebCacheControl.replyCacheHit(response, parts.filename, resource_timestamp);
					baseRequest.setHandled(true);
					return;
				}
				serveTextContents(parts.resourceFullName,"application/javascript",baseRequest,request,response,/*cacheable*/true);
				baseRequest.setHandled(true);				
			}
			else if (uri_lc.endsWith(".css")) {
				RequestedPathParts parts = breakURIParts(uri,defaultCssPackageName);
				final long resource_timestamp = IOUtils.getResourceLastModified(WebServer.class, parts.resourceFullName);
				if (resource_timestamp>0 && WebCacheControl.checkCacheTimeBased(request, resource_timestamp)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Requested resource ("+uri+") is already cached and up-to-date!");
					}
					WebCacheControl.replyCacheHit(response, parts.filename, resource_timestamp);
					baseRequest.setHandled(true);
					return;
				}
				serveTextContents(parts.resourceFullName,"text/css",baseRequest,request,response,/*cacheable*/true);
				baseRequest.setHandled(true);				
			}
			else if (uri_lc.endsWith(".png")
					|| uri_lc.endsWith(".ico")
					|| uri_lc.endsWith(".jpg")
					|| uri_lc.endsWith(".gif")) {
				if (uri_lc.endsWith("apple-touch-icon.png"))
					uri = "/hi_def.png";
				RequestedPathParts parts = breakURIParts(uri,defaultImagesPackageName);
				final long resource_timestamp = IOUtils.getResourceLastModified(WebServer.class, parts.resourceFullName);
				if (resource_timestamp>0 && WebCacheControl.checkCacheTimeBased(request, resource_timestamp)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Requested resource ("+uri+") is already cached and up-to-date!");
					}
					WebCacheControl.replyCacheHit(response, parts.filename, resource_timestamp);
					baseRequest.setHandled(true);
					return;
				}
				try (InputStream input = WebServer.class.getResourceAsStream(parts.resourceFullName);) {					
					byte[] img_contents = IOUtils.readFileContentsBinary(input);
					if (img_contents==null) {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
					}
					else {
						response.setHeader("Content-Type", "image/"+parts.ext);
						response.setHeader("Content-Length", String.valueOf(img_contents.length));
						WebCacheControl.setPublicCache(response, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS, resource_timestamp);
						response.getOutputStream().write(img_contents);
					}
				}
				baseRequest.setHandled(true);
			}
			else if (uri_lc.endsWith(".mp3")
					|| uri_lc.endsWith(".ogg")
					|| uri_lc.endsWith(".wav")) {
				RequestedPathParts parts = breakURIParts(uri,resourcesPackageName);
				final long resource_timestamp = IOUtils.getResourceLastModified(WebServer.class, parts.resourceFullName);
				if (resource_timestamp>0 && WebCacheControl.checkCacheTimeBased(request, resource_timestamp)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Requested resource ("+uri+") is already cached and up-to-date!");
					}
					WebCacheControl.replyCacheHit(response, parts.filename, resource_timestamp);
					baseRequest.setHandled(true);
					return;
				}
				try (InputStream input = WebServer.class.getResourceAsStream(parts.resourceFullName);) {					
					byte[] sound_contents = IOUtils.readFileContentsBinary(input);
					if (sound_contents==null) {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
					}
					else {
						if (uri_lc.endsWith(".ogg"))
							response.setHeader("Content-Type", "audio/ogg");
						else if (uri_lc.endsWith(".wav"))
							response.setHeader("Content-Type", "audio/wav");
						else
							response.setHeader("Content-Type", "audio/mpeg");
						response.setHeader("Content-Disposition", "filename="+parts.filename);
						response.setHeader("Content-Length", String.valueOf(sound_contents.length));						
						WebCacheControl.setPublicCache(response, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS, resource_timestamp);
						response.getOutputStream().write(sound_contents);
					}
				}
				baseRequest.setHandled(true);
			}
			else {
				if (customRESTfulService!=null) {
					
					String requestContents = null;
					if ("put".equalsIgnoreCase(request.getMethod())
						|| "post".equalsIgnoreCase(request.getMethod())) {
						try (InputStream in = request.getInputStream();) {
							requestContents = IOUtils.readFileContents(in);
						}						
					}
					if ("get".equalsIgnoreCase(request.getMethod())) {
						if (request.getQueryString()!=null)
							requestContents = request.getQueryString();
					}
					
					Object obj;
					try {						
						obj = customRESTfulService.getResponse(request.getMethod(),request.getRequestURI(),requestContents,
								request.getRemoteAddr(),getSession(request));					
					}
					catch (UnsupportedOperationException e) {
						log.log(Level.SEVERE, "Error while runnning "+request.getMethod()+" "+request.getRequestURI(), e);
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
						baseRequest.setHandled(true);
						return;
					}
					catch (Exception e) {
						log.log(Level.SEVERE, "Error while runnning "+request.getMethod()+" "+request.getRequestURI(), e);
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
						baseRequest.setHandled(true);
						return;
					}
					if (obj!=null) {
						if (obj instanceof String) {
							// raw text contents
							Charset charset = Charset.defaultCharset();
							response.setContentType("text/plain; charset="+charset.name());
							WebCacheControl.setNoCache(response);
							response.setStatus(HttpServletResponse.SC_OK);
							PrintWriter out = response.getWriter();
							out.print((String)obj);
						}
						else {
							// json contents
							String accept_format = request.getHeader("accept");
							boolean accept_json = (accept_format!=null && accept_format.contains("application/json"));
							Charset charset = Charset.defaultCharset();
							if (accept_json)
								response.setContentType("application/manifest+json; charset="+charset.name());
							else
								response.setContentType("text/html; charset="+charset.name());
							WebCacheControl.setNoCache(response);
							response.setStatus(HttpServletResponse.SC_OK);
							PrintWriter out = response.getWriter();
							if (accept_json)
								out.print(JSONUtils.toJSON(obj, false));
							else
								out.print(putInHTMLBody(JSONUtils.toJSON(obj, true)));
						}
						baseRequest.setHandled(true);
						return;
					}
				}
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
	}
	
	/**
	 * Get the session ID stored in a cookie coming from request
	 */
	private static String getSessionId(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies==null || cookies.length==0)
			return null;
		for (Cookie cookie:cookies) {
			if ("JSESSIONID".equals(cookie.getName()))
				return cookie.getValue();
		}
		return null;
	}
	
	private void serveTextContents(String resourceName,String contentType,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response,
			boolean cacheable) throws IOException {
		Charset charset = Charset.defaultCharset();
		response.setContentType(contentType+"; charset="+charset.name());
		if (!cacheable) {
			WebCacheControl.setNoCache(response);
		}
		else {
			WebCacheControl.setPublicCache(response, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS, IOUtils.getResourceLastModified(WebServer.class, resourceName));
		}
		response.setStatus(HttpServletResponse.SC_OK);
		PrintWriter out = response.getWriter();
		try (InputStream input = WebServer.class.getResourceAsStream(resourceName);) {
			String contents = IOUtils.readFileContents(input);
			// Replace special keywords
			if (contents.contains(MASK_SERVERHOST)) {
				contents = contents.replace(MASK_SERVERHOST, baseRequest.getLocalAddr());
			}
			out.write(contents);
		}
		catch (RuntimeException|IOException e) {
			log.log(Level.SEVERE, "Error while serving contents for "+resourceName, e);
			throw e;
		}
	}
	
	private static String putInHTMLBody(String contents) {
		StringBuilder html = new StringBuilder();
		html.append("<HTML>");
		html.append("<HEAD><meta http-equiv=\"Content-Type\" content=\"text/html; charset="+Charset.defaultCharset().name()+"\"></HEAD>");
		html.append("<BODY>");
		html.append(StringEscapeUtils.escapeHtml(contents).replaceAll("\r?\n", "<BR>\r\n"));
		html.append("</BODY>");
		html.append("</HTML>");
		return html.toString();
	}

	private static String getFullURLAddress(HttpServletRequest request,String redirection) {
		if (redirection.startsWith("/"))
			redirection = redirection.substring(1);
		String new_location = request.getRequestURL().toString();
		if (request.getRequestURI().length()>0
				&& !"/".equals(request.getRequestURI())) {
			int token = new_location.indexOf(request.getRequestURI());
			if (token>0) {
				new_location = new_location.substring(0,token);
			}
		}
		if (!new_location.endsWith("/"))
			new_location += "/";
		new_location += redirection;
		return new_location;
	}
	
	/**
	 * Interface used for page redirections.<BR>
	 * Application may provide an implementation to provide its logic through pages.
	 * @author Gustavo Figueiredo
	 */
	@FunctionalInterface
	public static interface AutoRedirection {
		
		/**
		 * Get mandatory HTTP redirections. Will prevent the HTML contents from being loaded. Will return
		 * HTTP response headers accordingly.<BR>
		 * Should return NULL if no redirections are needed.<BR>
		 * This method gets invoked only for HTML requests, excluding 'index.html'.  
		 */
		public String getHTTPRedirection(String remoteAddress,String requestedURI,String queryString,String resourceName,HttpSession session);
		
	}
	
	/**
	 * Interface for customized RESTful service provided by application
	 * @author Gustavo Figueiredo
	 */
	@FunctionalInterface
	public static interface CustomRESTfulService {
		public Object getResponse(String method,String uri,String requestContents,String remoteAddress,HttpSession session) throws Exception;
	}

	/**
	 * Parts of requested URI
	 */
	private static class RequestedPathParts {
		String filename;
		String ext;
		String resourceFullName;
	}

	/**
	 * Break a URI into its parts
	 */
	private static RequestedPathParts breakURIParts(String uri,String packageName) {
		RequestedPathParts parts = new RequestedPathParts();
		int first_sep = uri.indexOf('/');
		if (first_sep==0) {
			int last_sep_in_pkg_name = packageName.lastIndexOf('/');
			if (last_sep_in_pkg_name==packageName.length()-1)
				last_sep_in_pkg_name = packageName.lastIndexOf('/',last_sep_in_pkg_name-1);
			if (last_sep_in_pkg_name>0) {
				String last_path_in_pkg_name = packageName.substring(last_sep_in_pkg_name);
				if (uri.startsWith(last_path_in_pkg_name))
					first_sep = uri.indexOf('/',last_path_in_pkg_name.length()-1);
			}
		}
		final String pathname = (first_sep>=0) ? uri.substring(first_sep+1) : uri;
		final int last_sep = uri.lastIndexOf('/');
		parts.filename = (last_sep>=0) ? uri.substring(last_sep+1) : uri;
		final int ext_sep = pathname.lastIndexOf('.');
		parts.ext = pathname.substring(ext_sep+1);
		parts.resourceFullName = packageName+"/"+pathname;
		return parts;
	}
}
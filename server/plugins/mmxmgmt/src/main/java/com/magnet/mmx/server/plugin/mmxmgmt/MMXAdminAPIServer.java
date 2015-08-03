/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.server.plugin.mmxmgmt;

import com.magnet.mmx.server.plugin.mmxmgmt.servlet.AdminRESTResourceListing;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.ConfigServlet;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.DeviceServlet;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.JacksonJSONObjectMapperProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MMXRestEasyServletWrapper;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MessageServlet;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.PushMessageServlet;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.PushServlet;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.UserServlet;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MMXAdminAPIServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXAdminAPIServer.class);

  private Server privateApiServer = null;

  /**
   * Start the server
   */
  public void start() {

    MMXConfiguration configuration = MMXConfiguration.getConfiguration();

    final int callBackHandlerPort = configuration.getInt(MMXConfigKeys.ADMIN_API_PORT,
        MMXServerConstants.ADMIN_API_PORT);

    int linger = JiveGlobals.getIntProperty("http.socket.linger", -1);

    SslSocketConnector sslConnector = getSSLConnector();

    if(sslConnector != null && linger > -1) {
      LOGGER.trace("start : setting https socket linger={}", linger);
      sslConnector.setSoLingerTime(linger);
    }


    /**
     * Regular HTTP Connector.
     */
    SelectChannelConnector httpConnector = new SelectChannelConnector();
    httpConnector.setPort(callBackHandlerPort);

    if(linger > -1) {
      LOGGER.trace("start : setting http socket linger={}", linger);
      httpConnector.setSoLingerTime(linger);
    }

    privateApiServer = new Server(callBackHandlerPort);
    if (sslConnector != null) {
      privateApiServer.setConnectors(new Connector[]{sslConnector, httpConnector});
    } else {
      privateApiServer.setConnectors(new Connector[]{httpConnector});
    }
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath(MMXServerConstants.ADMIN_API_CONTEXT);
    privateApiServer.setHandler(context);

    context.addServlet(new ServletHolder(new MessageServlet()), "/messages");
    context.addServlet(new ServletHolder(new UserServlet()), "/users");
    context.addServlet(new ServletHolder(new PushServlet()), "/push");
    context.addServlet(new ServletHolder(new DeviceServlet()), "/devices/*");
    context.addServlet(new ServletHolder(new PushMessageServlet()), "/pushmessages");
    context.addServlet(new ServletHolder(new ConfigServlet()), "/config");

    /**
     * add the rest easy end point handling the admin rest API.
     */
    String[] resourceClasses = AdminRESTResourceListing.getResources();
    String resources = StringUtils.join(resourceClasses, ",");
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Resource classes:{}", resources);
    }
    ServletHolder holder = new ServletHolder(new MMXRestEasyServletWrapper());
    holder.setInitParameter(MMXServerConstants.RESTEASY_SERVLET_MAPPING_PREFIX_KEY,
        MMXServerConstants.ADMIN_API_SERVLET_MAPPING_PREFIX);

    holder.setInitParameter(MMXServerConstants.RESTEASY_RESOURCES_KEY, resources);
    holder.setInitParameter(MMXServerConstants.RESTEASY_PROVIDERS_KEY, JacksonJSONObjectMapperProvider.class.getName());


    context.addServlet(holder, MMXServerConstants.ADMIN_API_REST_MAPPING);
    try {
      LOGGER.info("Admin API server starting at port:" + callBackHandlerPort) ;
      privateApiServer.start();
      LOGGER.info("Admin API server started at port:" + callBackHandlerPort) ;
    } catch (Exception e) {
      LOGGER.error("Exception in starting the jetty Admin API server", e);
    }
  }

  public void stop() {
    try {
      LOGGER.info("Stopping Admin API server");
      privateApiServer.stop();
      LOGGER.info("Admin API server server stopped");
    } catch (Exception e) {
      LOGGER.warn("Exception in stopping Admin API server", e);
    }
  }

  protected MMXSSLConfiguration getSSLConfiguration() {
    return MMXSSLConfiguration.usingOpenFireCert();
  }

  /**
   * Get a configured SSLConnector.
   * @return SSLConnector. null if SSL shouldn't be enabled.
   */
  protected SslSocketConnector getSSLConnector() {
    MMXConfiguration configuration = MMXConfiguration.getConfiguration();
    SslSocketConnector sslConnector = null;
    if (configuration.getBoolean(MMXConfigKeys.ADMIN_API_ENABLE_HTTPS, MMXServerConstants.ADMIN_API_ENABLE_HTTPS)) {
      LOGGER.info("Enabling HTTPS connector for REST");
      int httpsPort = configuration.getInt(MMXConfigKeys.ADMIN_API_HTTPS_PORT, MMXServerConstants.ADMIN_API_HTTPS_PORT);
      MMXSSLConfiguration mmxsslConfiguration = getSSLConfiguration();
      String keyStoreFile = mmxsslConfiguration.getKeyStoreFilePath();
      String keyStorePassword = mmxsslConfiguration.getKeyStorePassword();
      LOGGER.info("HTTPS Config:{} port:{}", mmxsslConfiguration, httpsPort);
      SslContextFactory sslContextFactory = new SslContextFactory(keyStoreFile);
      sslContextFactory.setKeyStorePassword(keyStorePassword);
      // create an https connector
      sslConnector = new SslSocketConnector(sslContextFactory);
      sslConnector.setPort(httpsPort);
    } else {
      LOGGER.info("Not enabling HTTPS connector for REST");
    }
    return sslConnector;
  }

}

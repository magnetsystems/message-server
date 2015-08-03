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

import com.magnet.mmx.server.plugin.mmxmgmt.api.RESTResourceListing;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.JacksonJSONObjectMapperProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.PushReplyServlet;
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
 * Class that holds the jetty server that serves as the endpoint for public REST API
 * and call back server.
 */
public class MMXPublicAPIServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXPublicAPIServer.class);
  private Server server = null;


  public void start() {
    MMXConfiguration configuration = MMXConfiguration.getConfiguration();
    final int port = configuration.getInt(MMXConfigKeys.REST_HTTP_PORT,
        MMXServerConstants.DEFAULT_REST_HTTP_PORT);

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
    httpConnector.setPort(port);

    if(linger > -1) {
      LOGGER.trace("start : setting http socket linger={}", linger);
      httpConnector.setSoLingerTime(linger);
    }

    server = new Server(port);
    if (sslConnector != null) {
      server.setConnectors(new Connector[]{sslConnector, httpConnector});
    } else {
      server.setConnectors(new Connector[]{httpConnector});
    }
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath(MMXServerConstants.PUSH_CALLBACK_CONTEXT);
    server.setHandler(context);

    context.addServlet(new ServletHolder(new PushReplyServlet()), MMXServerConstants.PUSH_CALLBACK_ENDPOINT);
    /**
     * add the rest easy endpoint handling the public rest API.
     */
    String[] resourceClasses = RESTResourceListing.getResources();
    String[] providerList = RESTResourceListing.getProviders();
    String resources = StringUtils.join(resourceClasses, ",");
    String providers = StringUtils.join(providerList, ",");
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Resource classes:{}", resources);
    }
    ServletHolder holder = new ServletHolder(new org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher());
    holder.setInitParameter(MMXServerConstants.RESTEASY_SERVLET_MAPPING_PREFIX_KEY, MMXServerConstants.PUBLIC_API_SERVLET_MAPPING_PREFIX);
    holder.setInitParameter(MMXServerConstants.RESTEASY_RESOURCES_KEY, resources);
    holder.setInitParameter(MMXServerConstants.RESTEASY_PROVIDERS_KEY, providers);
    context.addServlet(holder, MMXServerConstants.PUBLIC_REST_API_MAPPING);


    try {
      LOGGER.info("Public REST API server starting at port:" + port);
      server.start();
      LOGGER.info("Public REST API server started at port:" + port);
    } catch (Exception e) {
      LOGGER.error("Exception in starting the Public REST API server", e);
    }

  }

  public void stop() {
    try {
      LOGGER.info("Stopping Public REST API server");
      server.stop();
      LOGGER.info("Stopped Public REST API server");
    } catch (Exception e) {
      LOGGER.warn("Exception in stopping Public REST API server", e);
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
    SslSocketConnector connector = null;
    if (configuration.getBoolean(MMXConfigKeys.REST_ENABLE_HTTPS, MMXServerConstants.DEFAULT_REST_ENABLE_HTTPS)) {
      LOGGER.info("Enabling HTTPS connector for REST");
      MMXSSLConfiguration sslConfiguration = getSSLConfiguration();
      int httpsPort = configuration.getInt(MMXConfigKeys.REST_HTTPS_PORT, MMXServerConstants.DEFAULT_REST_HTTPS_PORT);
      LOGGER.info("Using SSL Config:{} and port:{}" + sslConfiguration, httpsPort);
      String keyStoreFile = sslConfiguration.getKeyStoreFilePath();
      String keyStorePassword = sslConfiguration.getKeyStorePassword();
      SslContextFactory sslContextFactory = new SslContextFactory(keyStoreFile);
      sslContextFactory.setKeyStorePassword(keyStorePassword);
      // create an https connector
      connector = new SslSocketConnector(sslContextFactory);
      connector.setPort(httpsPort);
    } else {
      LOGGER.info("Not enabling HTTPS connector for REST");
    }
    return connector;
  }

}

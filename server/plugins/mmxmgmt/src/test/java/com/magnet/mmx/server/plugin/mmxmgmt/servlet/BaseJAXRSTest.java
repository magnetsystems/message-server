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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXAdminAPIServer;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXPublicAPIServer;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.util.JiveGlobalsMock;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.jivesoftware.util.JiveGlobals;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;


/**
*/
public class BaseJAXRSTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseJAXRSTest.class);

  private URI baseUri;
  private static MMXPublicAPIServer apiServer;
  private static MMXAdminAPIServer adminServer;
  private Client client;

  public BaseJAXRSTest(String baseUri) {
    java.util.logging.Logger.getLogger("com.google.inject").setLevel(java.util.logging.Level.SEVERE);
    this.baseUri = UriBuilder.fromUri(baseUri).build();
  }

  /**
   * Start the jetty servers for admin and public REST APIs
   * @throws Exception
   */
  @BeforeClass
  public static void startServer() throws Exception {
    DBTestUtil.setDataSourceFromPropertyFile();
    JiveGlobalsMock.setup();
    JiveGlobals.setProperty(MMXConfigKeys.REST_ENABLE_HTTPS, "false");
    JiveGlobals.setProperty(MMXConfigKeys.ADMIN_API_ENABLE_HTTPS, "false");
    JiveGlobals.setProperty(MMXConfigKeys.REST_HTTP_PORT, "8086");
    JiveGlobals.setProperty(MMXConfigKeys.ADMIN_API_PORT, "8087");
    LOGGER.trace("Starting jetty servers");
    apiServer = new SSLDisabledMMXPublicAPIServer();
    apiServer.start();
    adminServer = new SSLDisabledMMXAdminAPIServer();
    adminServer.start();
  }

  @Before
  public void buildClient() {
    client = ClientBuilder.newClient();
  }

  @AfterClass
  public static void stopServer() throws Exception {
    LOGGER.trace("Stopping jetty servers");
    apiServer.stop();
    adminServer.stop();
  }

  protected URI getBaseURI() {
    return baseUri;
  }

  protected Client getClient() {
    return client;
  }

  /**
   * Stub MMXPublicAPIServer that disables SSL no matter what the configuration is set to.
   */
  protected static class SSLDisabledMMXPublicAPIServer extends MMXPublicAPIServer {
    @Override
    protected SslSocketConnector getSSLConnector() {
      return null;
    }
  }

  /**
   * Stub MMXAdminAPIServer that disables SSL no matter what the configuration is set to.
   */
  protected static class SSLDisabledMMXAdminAPIServer extends MMXAdminAPIServer {
    @Override
    protected SslSocketConnector getSSLConnector() {
      return null;
    }
  }

}

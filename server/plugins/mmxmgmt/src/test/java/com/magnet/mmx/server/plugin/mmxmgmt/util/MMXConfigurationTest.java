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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.MMXTopicTagsResourceTest;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.ConfigServlet;
import com.magnet.mmx.util.JiveGlobalsMock;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.jivesoftware.util.JiveGlobals;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.*;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

//import org.eclipse.jetty.http.HttpTester;
//import org.eclipse.jetty.servlet.ServletTester;

/**
*/
@RunWith(JMockit.class)
public class MMXConfigurationTest {
  private static BasicDataSource ds;

  private final Triple<String, String, String>[] mbeanAttributes = new Triple[]{
          new ImmutableTriple<String, String, String>("WakeupInitialWait", MMXConfigKeys.WAKEUP_INITIAL_WAIT_KEY, "int"),
          new ImmutableTriple<String, String, String>("WakeupFrequencySecs", MMXConfigKeys.WAKEUP_FREQUENCY_KEY, "int"),
          new ImmutableTriple<String, String, String>("SMTPHostName", MMXConfigKeys.SMTP_HOSTNAME_KEY, "java.lang.String"),
          new ImmutableTriple<String, String, String>("SMTPPort", MMXConfigKeys.SMTP_PORT_KEY, "int"),
          new ImmutableTriple<String, String, String>("SMTPEnableTLS", MMXConfigKeys.SMTP_ENABLE_TLS_KEY, "java.lang.String"),
          new ImmutableTriple<String, String, String>("SMTPLingerDelaySecs", MMXConfigKeys.SMTP_LINGERDELAY_SECS_KEY, "java.lang.String"),
          new ImmutableTriple<String, String, String>("SMTPUserDisplayName", MMXConfigKeys.SMTP_USER_DISPLAY_NAME_KEY, "java.lang.String"),
          new ImmutableTriple<String, String, String>("SMTPUserEmailAddress", MMXConfigKeys.SMTP_USER_EMAIL_ADDRESS_KEY, "java.lang.String"),
          new ImmutableTriple<String, String, String>("SMTPReplyEmailAddress", MMXConfigKeys.SMTP_REPLY_EMAIL_ADDRESS_KEY, "java.lang.String"),
          new ImmutableTriple<String, String, String>("ClusterMaxDevicesPerApp", MMXConfigKeys.MAX_DEVICES_PER_APP, "long"),
          new ImmutableTriple<String, String, String>("ClusterMaxDevicesPerUser", MMXConfigKeys.MAX_DEVICES_PER_USER, "long"),
          new ImmutableTriple<String, String, String>("ClusterMaxAppsPerServer", MMXConfigKeys.MAX_APP_PER_OWNER, "long"),
          new ImmutableTriple<String, String, String>("MmxAlertEmailHost", MMXConfigKeys.ALERT_EMAIL_HOST, "java.lang.String"),
          new ImmutableTriple<String, String, String>("MmxAlertEmailPort", MMXConfigKeys.ALERT_EMAIL_PORT, "java.lang.String"),
          new ImmutableTriple<String, String, String>("MmxAlertEmailUser", MMXConfigKeys.ALERT_EMAIL_USER, "java.lang.String"),
          new ImmutableTriple<String, String, String>("MmxAlertEmailPassword", MMXConfigKeys.ALERT_EMAIL_PASSWORD, "java.lang.String"),
          new ImmutableTriple<String, String, String>("MmxAlertEmailSubject", MMXConfigKeys.ALERT_EMAIL_SUBJECT, "java.lang.String"),
          new ImmutableTriple<String, String, String>("MmxAlertEmailBccList", MMXConfigKeys.ALERT_EMAIL_BCC_LIST, "java.lang.String")
  };
  private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
  private final String MMX_MBEAN_DOMAIN = "com.magnet.mmx.server.plugin.mmxmgmt.util";
  private final String MMX_MBEAN_KEY_PROPERTY = "type=MMXManagedConfigurationMBean";
  private final String MMX_MBEAN_OBJECT_NAME = MMX_MBEAN_DOMAIN + ":" + MMX_MBEAN_KEY_PROPERTY;
  private final String JMX_SERVICE_URI = "service:jmx:rmi://127.0.0.1";
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXConfigurationTest.class);
  private JMXConnectorServer jmxConnectorServer;
  private JMXServiceURL url;

  @BeforeClass
  public static void setupDatabase() throws Exception {
    InputStream inputStream = MMXTopicTagsResourceTest.class.getResourceAsStream("/test.properties");

    Properties testProperties = new Properties();
    testProperties.load(inputStream);

    String host = testProperties.getProperty("db.host");
    String port = testProperties.getProperty("db.port");
    String user = testProperties.getProperty("db.user");
    String password = testProperties.getProperty("db.password");
    String driver = testProperties.getProperty("db.driver");
    String schema = testProperties.getProperty("db.schema");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + schema;

    ds = new BasicDataSource();
    ds.setDriverClassName(driver);
    ds.setUsername(user);
    ds.setPassword(password);
    ds.setUrl(url);

  }

  @Before
  public void setup() throws Exception {
    MMXManagedConfiguration mmxConfigMBean = new MMXManagedConfiguration(MMXConfiguration.getConfiguration());
    ObjectName name = new ObjectName(MMX_MBEAN_OBJECT_NAME);
    server.registerMBean(mmxConfigMBean, name);
    url = new JMXServiceURL(JMX_SERVICE_URI);
    JiveGlobalsMock.setup();
    jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, server);
    jmxConnectorServer.start();

    new MockUp<JiveGlobals>() {
      @Mock
      public boolean isPropertyEncrypted(String name) {
        return false;
      }
    };
  }

  @After
  public void teardown() throws Exception {
    ObjectName name = new ObjectName(MMX_MBEAN_OBJECT_NAME);
    server.unregisterMBean(name);
    jmxConnectorServer.stop();
    restoreDefaults();
  }

  /**
   * Set the MBean attribute value using the local platform MBean server and check whether the
   * returned value matches the set value
   *
   * @throws Exception
   */
  @Test
  public void testSetGetMBeanLocal() throws Exception {
    ObjectName name = new ObjectName(MMX_MBEAN_OBJECT_NAME);
    testSetGetAttribute(server, name);
  }

  /**
   * Set the MBean attribute value via a remote JMX Connection and check whether the
   * returned value matches the set value
   *
   * @throws Exception
   */
  @Test
  public void testSetGetMBeanRemote() throws Exception {
    JMXServiceURL jmxServiceUrl = jmxConnectorServer.getAddress();
    JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceUrl);
    MBeanServerConnection mbeanServerConnection = jmxConnector.getMBeanServerConnection();
    ObjectName name = new ObjectName(MMX_MBEAN_OBJECT_NAME);
    testSetGetAttribute(mbeanServerConnection, name);
  }

  /**
   * Set the Configuration property via JMX and check whether the REST interface returns the same value
   *
   * @throws Exception
   */
  //TODO: Use the web target JAXRS API to execute these request possibly ?
  @Ignore
  @Test
  public void testSetMBeanLocalGetREST() throws Exception {
    ObjectName name = new ObjectName(MMX_MBEAN_OBJECT_NAME);
    ServletTester tester = new ServletTester();
    tester.addServlet(LaxConfigServlet.class, "/config");
    tester.start();

    for (Triple<String, String, String> triple : mbeanAttributes) {
      String attrName = triple.getLeft();
      String attrType = triple.getRight();
      Object attrValue;
      if (attrType.equals("int")) {
        attrValue = RandomUtils.nextInt(30000, 65535);
      } else if(attrType.equals("long")) {
        attrValue = RandomUtils.nextLong(10, 1000);
      } else {
        attrValue = RandomStringUtils.randomAlphabetic(10);
      }
      Attribute attr1 = new Attribute(attrName, attrValue);
      server.setAttribute(name, attr1);
      Object attr2 = server.getAttribute(name, attrName);
      assertEquals("Attribute values do not match", attrValue, attr2);
      HttpTester request = new HttpTester();
     // HttpTester.Request request = HttpTester.newRequest();
      request.setMethod("GET");
      request.setHeader("Host", "tester");
      request.setURI("/config");
      request.setContent("");
      HttpTester response = new HttpTester();
      response.parse(tester.getResponses(request.generate()));
      JsonElement jelement = new JsonParser().parse(response.getContent());
      JsonObject jobject = jelement.getAsJsonObject();
      jobject = jobject.getAsJsonObject("configs");
      String attrValueRest = jobject.get(triple.getMiddle()).getAsString();
      if (attrType.equals("int"))
        assertEquals("Values do not match", attrValue, Integer.parseInt(attrValueRest));
      else if(attrType.equals("long"))
        assertEquals("Values do not match", attrValue, Long.parseLong(attrValueRest));
      else
        assertEquals("Values do not match", attrValue, attrValueRest);
    }
  }

  /**
   * Set the Configuration property via REST interface and check whether the JMX interface returns the same value
   *
   * @throws Exception
   */
  //TODO: Use the web target JAXRS API to execute these request possibly ?
  @Ignore
  @Test
  public void testSetRESTGetMBeanLocal() throws Exception {
    LOGGER.debug("testSetRESTGetMBeanLocal");
    ObjectName name = new ObjectName(MMX_MBEAN_OBJECT_NAME);
    ServletTester tester = new ServletTester();
    tester.addServlet(LaxConfigServlet.class, "/config");
    tester.start();

    for (Triple<String, String, String> triple : mbeanAttributes) {
      String attrName = triple.getLeft();
      String attrType = triple.getRight();

      String attrValueStr;
      if (attrType.equals("int"))
        attrValueStr = Integer.toString(RandomUtils.nextInt(30000, 65535));
      else if (attrType.equals("long"))
        attrValueStr = Long.toString(RandomUtils.nextLong(10, 1000));
      else
        attrValueStr = RandomStringUtils.randomAlphabetic(10);

      String str = constructGson(triple.getMiddle(), attrValueStr);

      HttpTester request = new HttpTester();
      request.setMethod("POST");
      request.setHeader("Host", "tester");
      request.setContent(str);
      request.setHeader("Content-Type", "application/json");

      request.setURI("/config");
      HttpTester response = new HttpTester();
      //response = response.parse(tester.getResponses(request.generate()));
      response.parse(tester.getResponses(request.generate()));
      assertEquals("Values do not match", server.getAttribute(name, triple.getLeft()).toString(), attrValueStr);

    }
  }


  private void testSetGetAttribute(MBeanServerConnection connection, ObjectName name) throws Exception {
    for (Triple<String, String, String> pair : mbeanAttributes) {
      String attrName = pair.getLeft();
      String attrType = pair.getRight();
      Object attrValue;
      if (attrType.equals("int"))
        attrValue = RandomUtils.nextInt(30000, 65535);
      else if (attrType.equals("long"))
        attrValue = RandomUtils.nextLong(10, 1000);
      else
        attrValue = RandomStringUtils.randomAlphabetic(10);
      Attribute attr1 = new Attribute(attrName, attrValue);
      server.setAttribute(name, attr1);
      Object attr2 = server.getAttribute(name, attrName);
      assertEquals("Attribute values do not match", attrValue, attr2);
    }
  }

  private String constructGson(String attrName, String attrValue) {
    HashMap<String, Map> configMap = new HashMap<String, Map>();
    HashMap<String, String> propertiesMap = new HashMap<String, String>();

    propertiesMap.put("push.callbackurl", "http://citest01.magneteng.com:9090/plugins/mmxmgmt/pushreply");
    propertiesMap.put("smtp.hostname", "smpt.gmail.com");
    propertiesMap.put("smtp.lingerdelay.secs", "0");
    propertiesMap.put("smtp.port", "587");
    propertiesMap.put("smtp.tls.enable", "true");
    propertiesMap.put("system.user.display.name", "MagnetMMXAdmin");
    propertiesMap.put("system.user.email.address", "magnet.android@gmail.com");
    propertiesMap.put("system.user.email.password", "Magnet789");
    propertiesMap.put("system.user.reply.email.address", "magnet.android@gmail.com");
    propertiesMap.put("wakeup.frequency", "1899120289");
    propertiesMap.put("wakeup.initialwait", "1341891748");
    propertiesMap.put("cluster.max.devices.per.app", "10");
    propertiesMap.put("cluster.max.devices.perx.user", "5");
    propertiesMap.put("cluster.max.apps", "100");
    propertiesMap.put("instance.max.inapp.message.rate", "100");
    propertiesMap.put("instance.max.push.message.rate", "100");
    propertiesMap.put(attrName, attrValue);
    configMap.put("configs", propertiesMap);
    Gson gson = new Gson();
    String str = gson.toJson(configMap);
    return str;
  }

  private void restoreDefaults() {
    MMXConfiguration mmxConfiguration = MMXConfiguration.getConfiguration();
    mmxConfiguration.setValue(MMXConfigKeys.WAKEUP_INITIAL_WAIT_KEY, "10");
    mmxConfiguration.setValue(MMXConfigKeys.WAKEUP_FREQUENCY_KEY, "30");
    mmxConfiguration.setValue(MMXConfigKeys.SMTP_HOSTNAME_KEY, "smtp.gmail.com");
    mmxConfiguration.setValue(MMXConfigKeys.SMTP_PORT_KEY, "587");
    mmxConfiguration.setValue(MMXConfigKeys.SMTP_ENABLE_TLS_KEY, "true");
    mmxConfiguration.setValue(MMXConfigKeys.SMTP_LINGERDELAY_SECS_KEY, "0");
    mmxConfiguration.setValue(MMXConfigKeys.SMTP_USER_EMAIL_ADDRESS_KEY, "magnet.android@gmail.com");
    mmxConfiguration.setValue(MMXConfigKeys.SMTP_USER_DISPLAY_NAME_KEY, "MagnetMMXAdmin");
    mmxConfiguration.setValue(MMXConfigKeys.SMTP_REPLY_EMAIL_ADDRESS_KEY, "magnet.android@gmail.com");
    mmxConfiguration.setValue(MMXConfigKeys.SMTP_USER_EMAIL_PASSWORD_KEY, "Magnet789");
    mmxConfiguration.setValue(MMXConfigKeys.MAX_DEVICES_PER_APP, "10");
    mmxConfiguration.setValue(MMXConfigKeys.MAX_DEVICES_PER_USER, "10");
    mmxConfiguration.setValue(MMXConfigKeys.MAX_APP_PER_OWNER, "5");
    mmxConfiguration.setValue(MMXConfigKeys.MAX_XMPP_RATE, "-1");
    mmxConfiguration.setValue(MMXConfigKeys.MAX_HTTP_RATE, "-1");
    mmxConfiguration.setValue(MMXConfigKeys.ALERT_EMAIL_HOST, MMXServerConstants.DEFAULT_EMAIL_HOST);
    mmxConfiguration.setValue(MMXConfigKeys.ALERT_EMAIL_PORT, Integer.toString(MMXServerConstants.DEFAULT_SMTP_PORT));
    mmxConfiguration.setValue(MMXConfigKeys.ALERT_EMAIL_USER, MMXServerConstants.DEFAULT_EMAIL_USER);
    mmxConfiguration.setValue(MMXConfigKeys.ALERT_EMAIL_PASSWORD, MMXServerConstants.DEFAULT_EMAIL_PASSWORD);
    mmxConfiguration.setValue(MMXConfigKeys.ALERT_EMAIL_SUBJECT, MMXServerConstants.DEFAULT_ALERT_EMAIL_SUBJECT);
    mmxConfiguration.setValue(MMXConfigKeys.ALERT_EMAIL_BCC_LIST, "");
  }

  /**
   * Stub config servlet that disable auth checks which don't work when not running in openfire environment.
   */
  public static class LaxConfigServlet extends ConfigServlet {
    @Override
    protected boolean isAuthorized(HttpServletRequest request) {
      return true;
    }
  }
}


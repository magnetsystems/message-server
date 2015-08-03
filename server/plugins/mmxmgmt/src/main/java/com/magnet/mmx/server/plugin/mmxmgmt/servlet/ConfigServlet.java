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

import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.util.GsonData;
import org.apache.commons.lang.StringUtils;
import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.magnet.mmx.server.plugin.mmxmgmt.servlet.WebConstants.CONTENT_TYPE_JSON;

public class ConfigServlet extends AbstractSecureServlet {
  private static Logger LOGGER = LoggerFactory.getLogger(ConfigServlet.class);

  private static final String PATH = "mmxmgmt/config";

  @Override
  public void init(ServletConfig config) throws ServletException {
    LOGGER.info("Initializing:" + ConfigServlet.class);
    super.init(config);
    AuthCheckFilter.addExclude(PATH);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    LOGGER.trace("doGet : Getting MMX Configuration");
    response.setContentType(CONTENT_TYPE_JSON);
    PrintWriter out = response.getWriter();
    SerializableConfig sconfig = new SerializableConfig(MMXConfiguration.getConfiguration());
    GsonData.getGson().toJson(sconfig, out);
    out.flush();
    response.setStatus(HttpServletResponse.SC_OK);
    return;
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    LOGGER.trace("doPost : Setting MMX Configuration");
    response.setContentType(CONTENT_TYPE_JSON);
    PrintWriter out = response.getWriter();

    String postBody = PushServlet.getRequestBody(req);

    if (postBody != null && !postBody.trim().isEmpty()) {
      LOGGER.trace("doPost : Received Configuration : {}", postBody);
      SerializableConfig config = GsonData.getGson().fromJson(postBody, SerializableConfig.class);

      MMXConfiguration configuration = MMXConfiguration.getConfiguration();
      Set<String> keys = config.configs.keySet();
      for (String key : keys) {
        //set the value for this configuration
        // see if this is an XMPP configuration
        if (MMXConfiguration.isXmppProperty(key)) {
          JiveGlobals.setProperty(key, config.configs.get(key));
        } else {
          configuration.setValue(key, config.configs.get(key));
        }
      }
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  /**
   * Config that can be serialized to JSON and de-serialized from JSON.
   */
  private static class SerializableConfig  {
    Map<String, String> configs = new TreeMap<String, String>();

    public SerializableConfig() {};

    public SerializableConfig(MMXConfiguration configuration) {
      Iterator<String> keys = configuration.getKeys();
      LOGGER.trace("SerializableConfig : configuration={}", configuration);
      while (keys.hasNext()) {
        String key = keys.next();
        if(MMXConfigKeys.ALERT_EMAIL_BCC_LIST.equals(key)) {
          configs.put(key, StringUtils.join(configuration.getList(key), ","));
        } else {
          configs.put(key, configuration.getString(key));
        }
      }
      // add XMPP ones to expose via Rest
      if (JiveGlobals.getProperty(MMXConfigKeys.XMPP_CLIENT_TLS_POLICY) != null) {
        configs.put(MMXConfigKeys.XMPP_CLIENT_TLS_POLICY, JiveGlobals.getProperty(MMXConfigKeys.XMPP_CLIENT_TLS_POLICY));
      }
      // add db info
      Connection dbConnection = null;
      try {
        dbConnection = new OpenFireDBConnectionProvider().getConnection();
        configs.put(MMXConfigKeys.DATABASE_URL, dbConnection.getMetaData().getURL());
        configs.put(MMXConfigKeys.DATABASE_USER, dbConnection.getMetaData().getUserName());
      } catch (SQLException e) {
        e.printStackTrace();
      }
      finally {
        DbConnectionManager.closeConnection(dbConnection);
      }
    }
  }
}

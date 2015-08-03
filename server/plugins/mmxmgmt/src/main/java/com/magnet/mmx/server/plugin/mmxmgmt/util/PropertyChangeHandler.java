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

import com.google.common.base.Strings;
import com.magnet.mmx.server.plugin.mmxmgmt.monitoring.RateLimiterService;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.component.ExternalComponentManager;
import org.jivesoftware.util.ModificationNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyChangeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyChangeHandler.class);

  public static void handle(String property) {
    if(Strings.isNullOrEmpty(property)) {
      return;
    }
    if (property.equals(MMXConfigKeys.MAX_XMPP_RATE)) {
      int rate = MMXConfiguration.getConfiguration().getInt(MMXConfigKeys.MAX_XMPP_RATE, MMXServerConstants.DEFAULT_MAX_XMPP_RATE);
      RateLimiterService.updateRates(MMXServerConstants.XMPP_RATE_TYPE, rate);
    } else if(property.equals(MMXConfigKeys.MAX_HTTP_RATE)) {
      int rate = MMXConfiguration.getConfiguration().getInt(MMXConfigKeys.MAX_HTTP_RATE, MMXServerConstants.DEFAULT_MAX_HTTP_RATE);
      RateLimiterService.updateRates(MMXServerConstants.HTTP_RATE_TYPE, rate);
    } else if (MMXConfigKeys.EXT_SERVICE_EVENT_GEO_SECRET.equals(property)) {
      // update the secret for geo component
      // TODO add proper Rest API to configure components
      try {
        String secret = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.EXT_SERVICE_EVENT_GEO_SECRET);
        if (secret != null && secret.length() > 0) {
          ExternalComponentManager.setDefaultSecret(secret);
        }
      } catch (ModificationNotAllowedException e) {
          LOGGER.warn("failed to update secret for components.", e);
      }
    } else if (MMXConfigKeys.EXT_SERVICE_PORT.equals(property)) {
      // update the secret for geo component
      // TODO add proper Rest API to configure components
      try {
        String port = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.EXT_SERVICE_PORT);
        if (port != null && port.length() > 0) {
          ExternalComponentManager.setServicePort(Integer.parseInt(port));
          XMPPServer.getInstance().getConnectionManager().enableComponentListener(true);
        }
      } catch (ModificationNotAllowedException e) {
        LOGGER.warn("failed to update port for components.", e);
      }
    } else if (MMXConfigKeys.EXT_SERVICE_ENABLED.equals(property)) {
      // enable or disable external component service
      boolean enabled = MMXConfiguration.getConfiguration().getBoolean(MMXConfigKeys.EXT_SERVICE_ENABLED, false);
      XMPPServer.getInstance().getConnectionManager().enableComponentListener(enabled);
    }
  }
}

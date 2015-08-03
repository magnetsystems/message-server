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

import com.magnet.mmx.server.plugin.mmxmgmt.event.*;

import java.util.List;

/**
 */
public class EmailBodyCreationUtil {
  public static String getBodyFromEvents(List<MMXEvent> eventList) {
    StringBuffer buffer = new StringBuffer(1000);
    for(MMXEvent event : eventList) {
      buffer.append(getBodyFromEvent(event));
      buffer.append("\n");
    }
    return buffer.toString();
  }

  private static String getBodyFromEvent(MMXEvent event) {
    if(event instanceof MMXXmppRateExceededEvent) {
      return getString((MMXXmppRateExceededEvent) event);

    } else if (event instanceof MMXHttpRateExceededEvent) {
      return getString((MMXHttpRateExceededEvent) event);

    } else if (event instanceof MMXMaxAppLimitReachedEvent) {
      return getString((MMXMaxAppLimitReachedEvent) event);

    } else if (event instanceof MMXMaxDevicesPerAppLimitReachedEvent) {
      return getString((MMXMaxDevicesPerAppLimitReachedEvent) event);

    }
    return "";
  }

  private static String getString(MMXXmppRateExceededEvent event) {
    return getAppPrefix(event) + String.format(MMXServerConstants.MAX_XMPP_RATE_EXCEEDED_EMAIL_BODY, event.getRate());
  }

  private static String getString(MMXHttpRateExceededEvent event) {
    return getAppPrefix(event) + String.format(MMXServerConstants.MAX_HTTP_RATE_EXCEEDED_EMAIL_BODY, event.getRate());
  }

  private static String getString(MMXMaxAppLimitReachedEvent event) {
    return getPrefix(event) + "ownerId:" + event.getOwnerId() + ",\t" + String.format(MMXServerConstants.MAX_APP_LIMIT_BODY, event.getLimit());
  }

  private static String getString(MMXMaxDevicesPerAppLimitReachedEvent event) {
    return getAppPrefix(event) + String.format(MMXServerConstants.MAX_DEV_PER_APP_BODY, event.getLimit());
  }

  private static String getAppPrefix(MMXAppEvent event) {
    return event.getTimestamp() + ",\t" + "appId:" + event.getAppId() + ",\t";
  }

  private static String getPrefix(MMXEvent event) {
    return  event.getTimestamp() + ",\t";
  }
}

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

import com.magnet.mmx.util.JiveGlobalsMock;
import org.jivesoftware.util.JiveGlobals;

/**
 */
//@RunWith(JMockit.class)
public class MMXEmailSenderTest {
  //@Test
  public void testEmailSender() throws Exception {
    JiveGlobalsMock.setup();
    JiveGlobals.setProperty(MMXConfigKeys.ALERT_EMAIL_HOST, "");
    JiveGlobals.setProperty(MMXConfigKeys.ALERT_EMAIL_PORT, "");
    JiveGlobals.setProperty(MMXConfigKeys.ALERT_EMAIL_USER, "");
    JiveGlobals.setProperty(MMXConfigKeys.ALERT_EMAIL_PASSWORD, "");
    JiveGlobals.setProperty(MMXConfigKeys.ALERT_EMAIL_BCC_LIST, "");
   /* String host = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_HOST, MMXServerConstants.DEFAULT_EMAIL_HOST);
    String port = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_PORT, Integer.toString(MMXServerConstants.DEFAULT_SMTP_PORT));
    String user = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_USER, MMXServerConstants.DEFAULT_EMAIL_USER);
    String password = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_PASSWORD, MMXServerConstants.DEFAULT_EMAIL_PASSWORD);
    */
    new MMXEmailSender().sendToBccOnly("Hey there");
  }
}

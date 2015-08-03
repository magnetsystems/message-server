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
package com.magnet.mmx.util;

import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXEmailSender;

/**
 */
public class MMXEmailSenderTest {
  public void testSendingEmail() {
    MMXConfiguration.getConfiguration().setValue(MMXConfigKeys.ALERT_EMAIL_BCC_LIST, "firstname9.lastname9@magnet.com,firstname5.lastname5@magnet.com,login973@gmail.com,firstname5lastname5@gmail.com");
    MMXConfiguration.getConfiguration().setValue(MMXConfigKeys.ALERT_EMAIL_ENABLED, "true");
    new MMXEmailSender().sendToBccOnly("Test message");
  }
}

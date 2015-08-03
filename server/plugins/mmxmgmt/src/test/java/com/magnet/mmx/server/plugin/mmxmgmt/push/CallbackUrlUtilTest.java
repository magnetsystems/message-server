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
package com.magnet.mmx.server.plugin.mmxmgmt.push;

import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.util.JiveGlobalsMock;
import com.sun.syndication.io.impl.Base64;
import org.jivesoftware.util.JiveGlobals;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 */
public class CallbackUrlUtilTest {

  @BeforeClass
  public static void startServer() throws Exception {
    JiveGlobalsMock.setup();
    JiveGlobals.setProperty(MMXConfigKeys.REST_ENABLE_HTTPS, "false");
    JiveGlobals.setProperty(MMXConfigKeys.ADMIN_API_ENABLE_HTTPS, "false");
    JiveGlobals.setProperty(MMXConfigKeys.REST_HTTP_PORT, "8086");
    JiveGlobals.setProperty(MMXConfigKeys.ADMIN_API_PORT, "8087");
    JiveGlobals.setProperty(MMXConfigKeys.PUSH_CALLBACK_PORT, "8086");
    JiveGlobals.setProperty(MMXConfigKeys.PUSH_CALLBACK_HOST, "login3s-macbook-pro.local");
  }

  @Test
  public void testCallBackURLToken1() throws Exception {
    String pushMessageId = "8dbef120271b1b4a974acaa7e32dcc66";
    long time = 1423615926L * 1000;
    Date current = new Date(time);
    String token = CallbackUrlUtil.callBackURLToken(pushMessageId, current);
    assertNotNull("Token is null", token);

    String[] split = CallbackUrlUtil.decodeToken(token);

    String messageId = split[0];
    assertEquals("Non matching push message id", pushMessageId, messageId);
  }

  @Test
  public void testBadToken() throws Exception {
    String token = Base64.encode("this is a really bad token");
    String[] split = CallbackUrlUtil.decodeToken(token);
    assertNull("Didn't get expected null", split);
  }

  @Test
  public void testBuildCallbackURL() throws Exception {
    String url = CallbackUrlUtil.buildCallBackURL("10");
    assertNotNull(url);
    String prefix = "http://login3s-macbook-pro.local:8086/mmxmgmt/v1/pushreply?tk=";
    boolean startWith = url.startsWith(prefix);
    assertTrue("URL doesn't start with:" + prefix, startWith);

  }

}

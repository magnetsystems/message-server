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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test some of the helper methods in JIDUtil.
 */
public class JIDUtilTest {
  @Test
  public void testGetAppId() throws Exception {
    String serialJID = "nexus%i26u1lmv7uc@192.168.101.146/8D2F9E5595E9989FEF3D1D3A5BA0FBE0BB318ED0";

    String appId = JIDUtil.getAppId(serialJID);
    String expected = "i26u1lmv7uc";
    assertEquals("Non matching appId", expected, appId);
  }

  @Test
  public void testGetAppId2() throws Exception {
    String serialJID = "nexus@192.168.101.146/8D2F9E5595E9989FEF3D1D3A5BA0FBE0BB318ED0";
    String appId = JIDUtil.getAppId(serialJID);
    assertNull(appId);
  }

  @Test
  public void testGetAppId3() throws Exception {
    String serialJID = "nexus%i26u1lmv7uc192.168.101.146/8D2F9E5595E9989FEF3D1D3A5BA0FBE0BB318ED0";
    String appId = JIDUtil.getAppId(serialJID);
    assertNull(appId);
  }

  @Test
  public void testGetResource() throws Exception {
    String serialJID = "nexus%i26u1lmv7uc@192.168.101.146/8D2F9E5595E9989FEF3D1D3A5BA0FBE0BB318ED0";
    String deviceId = JIDUtil.getResource(serialJID);
    String expected = "8D2F9E5595E9989FEF3D1D3A5BA0FBE0BB318ED0";
    assertEquals("Non matching appId", expected, deviceId);
  }
}

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
package com.magnet.mmx.server.plugin.mmxmgmt.apns;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 */
public class APNSPayloadInfoTest {
  @Test
  public void testParse() throws Exception {
    String json = "{ \"aps\": {\"content-available\":1},\"_mmx\" : {\"ty\" : \"mmx:w:ping\",\"id\" : \"261ty171765\"," +
        "\"cu\":\"http://preview.magnet.com:5221/mmxmgmt/v1/pushreply?pushmessageid=261ty171765\"}}";
    APNSPayloadInfo info = APNSPayloadInfo.parse(json);
    assertNotNull(info);
    String id = info.getMessageId();
    assertEquals("Non matching message id", "261ty171765", id);
    String type = info.getType();
    assertEquals("Non matching message id", "mmx:w:ping", type);

  }

  @Test
  public void testParseBadJSON() throws Exception {
    String json = "{ ";
    APNSPayloadInfo info = APNSPayloadInfo.parse(json);
    assertNull("Expected null info", info);
  }

  @Test
  public void testParseMissingMMXElement() throws Exception {
    String json = "{ \"aps\": {\"content-available\":1}}";
    APNSPayloadInfo info = APNSPayloadInfo.parse(json);
    assertNull("Expected null info", info);
  }

  @Test
  public void testParseWakeupMessage() throws Exception {
    String json = "{\"_mmx\":{\"ty\":\"mmx:w:retrieve\"},\"aps\":{\"content-available\":1}}";
    APNSPayloadInfo info = APNSPayloadInfo.parse(json);
    assertNotNull("Expected null info", info);
    assertNull("Expected null message id", info.getMessageId());
  }
}

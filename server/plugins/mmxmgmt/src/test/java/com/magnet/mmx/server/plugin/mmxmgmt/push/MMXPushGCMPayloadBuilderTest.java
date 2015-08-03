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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magnet.mmx.protocol.Constants;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class MMXPushGCMPayloadBuilderTest {

  @Test
  public void testBuild() throws Exception {
    MMXPushGCMPayloadBuilder builder = new MMXPushGCMPayloadBuilder();
    builder.setBody("Your order is ready for pickup at 465 E El Camino Real, Palo Alto, CA");
    builder.setTitle("Order status");
    MMXPushHeader header = new MMXPushHeader(Constants.MMX, Constants.MMX_ACTION_CODE_PUSH);
    builder.setType(header);
    builder.setId("261ty171890");
    builder.setCallBackURL("http://preview.magnet.com:5221/mmxmgmt/v1/pushreply?pushmessageid=261ty171890");

    Map<String, String> custom = new LinkedHashMap<String, String>();
    custom.put("action", "doSomething");
    custom.put("url" ,"http://live.sports.espn.com");
    builder.setCustomDictionary(custom);

    String json = builder.buildJSON();
    assertNotNull("json is null", json);
    JsonParser parser = new JsonParser();
    JsonObject jsonObject = parser.parse(json).getAsJsonObject();
    JsonElement title = jsonObject.get("title");
    assertNotNull("Didn't find title object", title);
    JsonElement mmx = jsonObject.get("_mmx");
    assertNotNull("Didn't find mmx object", mmx);
    String payload = builder.build();
    assertTrue("Payload doesn't begin with prefix", payload.startsWith(header.toString(true)));

  }


  @Test
  public void testBuildWithNoMMXValuesSet() {
    MMXPushGCMPayloadBuilder builder = new MMXPushGCMPayloadBuilder();
    builder.setBody("Your order is ready for pickup at 465 E El Camino Real, Palo Alto, CA");
    builder.setTitle("Order status");
    boolean gotException = false;
    try {
      builder.build();
    } catch (IllegalStateException e) {
      gotException = true;
    } catch (Throwable t) {

    }
    assertTrue("didn't get expected exception", gotException);
  }

  @Test
  public void testWakeupPayload() {
    String payload = MMXPushGCMPayloadBuilder.wakeupPayload();

    String prefix = new MMXPushHeader(Constants.MMX, Constants.MMX_ACTION_CODE_WAKEUP, Constants.PingPongCommand.retrieve.name()).toString();
    int index = payload.indexOf(prefix);
    String json = payload.substring(index+prefix.length());

    JsonParser parser = new JsonParser();
    JsonObject jsonObject = parser.parse(json).getAsJsonObject();

    JsonElement mmxElement = jsonObject.get("_mmx");
    JsonObject mmx = (JsonObject) mmxElement;
    assertNotNull("Didn't find aps object", mmxElement);
    assertTrue("mmx is not a JSON object", mmxElement.isJsonObject());
    JsonElement tye = mmx.get("ty");
    String value = tye.getAsString();
    assertNotNull(value);
    assertEquals("Didn't get expected value of type", "mmx:w:retrieve", value);
  }
}

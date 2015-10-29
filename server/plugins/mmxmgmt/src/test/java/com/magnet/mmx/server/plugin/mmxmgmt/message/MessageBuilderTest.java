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
package com.magnet.mmx.server.plugin.mmxmgmt.message;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.junit.Test;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;

/**
 */
public class MessageBuilderTest {


  @Test
  public void test1Build() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setRecipientUsernames(Collections.singletonList("importantuser"));



    String appId = "i0sq7ddvi17";

    String message = "This is a very important message";
    HashMap<String,String> contentMap = new HashMap<String, String>();
    contentMap.put("content", message);

    request.setReceipt(true);

    AppEntity app = new AppEntity();
    app.setAppId("testapp");
    app.setServerUserId("admin%i1cglsw8dsa");

    MessageBuilder builder = new MessageBuilder();
    builder.setSenderId(app.getServerUserId());
    builder.setAppEntity(app);
    builder.setDomain("localhost");
    builder.setUserId(request.getRecipientUsernames().get(0));
    builder.setUtcTime(System.currentTimeMillis());
    builder.setMetadata(request.getContent());
    builder.setIdGenerator(new MessageIdGeneratorImpl());
    Message m = builder.build();
    /**
     * Simply assert the the message object is not null.
     */
    assertNotNull("Message shouldn't be null", m);
  }

  /**
   * Test with content containing XML characters.
   * @throws Exception
   */
  @Test
  public void test2Build() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setRecipientUsernames(Collections.singletonList("test"));
    String message = "<message><subject>This is a test</subject><content>Tell me a story</content></message>";

    Map<String, String> contentMap = new HashMap<String, String>();
    contentMap.put("content", message);
    contentMap.put("contentType", "text");
    request.setReceipt(true);

    AppEntity app = new AppEntity();
    app.setAppId("i1cglsw8dsa");
    app.setServerUserId("admin%i1cglsw8dsa");

    MessageBuilder builder = new MessageBuilder();
    builder.setAppEntity(app);
    builder.setDomain("localhost");
    builder.setMetadata(request.getContent());
    builder.setUtcTime(System.currentTimeMillis());
    builder.setIdGenerator(new MessageIdGeneratorImpl());
    builder.setSenderId(app.getServerUserId());
    builder.setUserId(request.getRecipientUsernames().get(0));
    Message m = builder.build();
    assertNotNull("Message shouldn't be null", m);
    Element mmx = m.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    assertNotNull("mmx element is null", mmx);
    Element payload = mmx.element(Constants.MMX_PAYLOAD);
    assertNotNull("payload element is null", payload);
    String content = payload.getText();
    assertNotNull("payload content is null", content);
    assertEquals("Non matching content", "", content);

    JID from = m.getFrom();
    assertEquals ("Non matching from jid", "admin%i1cglsw8dsa@localhost", from.toString());
    String body = m.getBody();
    assertEquals("Message body is not expected", MMXServerConstants.MESSAGE_BODY_DOT, body);
  }


  @Test
  public void test3BuildWithNameAndValue() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setRecipientUsernames(Collections.singletonList("test"));
    String message = "Simple Test";
    Map<String, String> contentMap = new HashMap<String, String>();
    contentMap.put("content", message);
    contentMap.put("contentType", "text");
    contentMap.put("apple", "washington");
    contentMap.put("avocado", "california");

    request.setContent(contentMap);

    request.setReceipt(true);

    AppEntity app = new AppEntity();
    app.setAppId("i1cglsw8dsa");
    app.setServerUserId("admin%i1cglsw8dsa");

    MessageBuilder builder = new MessageBuilder();
    builder.setAppEntity(app);
    builder.setDomain("localhost");
    builder.setSenderId(app.getServerUserId());
    builder.setUserId(request.getRecipientUsernames().get(0));
    builder.setMetadata(request.getContent());
    builder.setUtcTime(System.currentTimeMillis());
    builder.setIdGenerator(new MessageIdGeneratorImpl());
    Message m = builder.build();

    assertNotNull("Message shouldn't be null", m);

    Element mmx = m.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    assertNotNull("mmx element is null", mmx);

    Element meta = mmx.element(Constants.MMX_META);
    assertNotNull("meta element is null", meta);
    String json = meta.getText();
    assertNotNull("meta json is null", json);

    JsonParser parser = new JsonParser();
    JsonObject jsonObject = null;
    try {
      jsonObject =  parser.parse(json).getAsJsonObject();
    } catch (JsonSyntaxException e) {
      fail("JsonSyntax exception");
    }

    for (String key : contentMap.keySet()) {
      JsonElement entry = jsonObject.get(key);
      assertNotNull("No entry found for key:" + key, entry);
      String value = entry.getAsString();
      assertEquals("Non match value for key:"+key, contentMap.get(key), value);
    }

    Element payload = mmx.element(Constants.MMX_PAYLOAD);
    assertNotNull("payload element is null", payload);
    String content = payload.getText();
    assertNotNull("payload content is null", content);
    assertEquals("Non matching content", "", content);

  }
}

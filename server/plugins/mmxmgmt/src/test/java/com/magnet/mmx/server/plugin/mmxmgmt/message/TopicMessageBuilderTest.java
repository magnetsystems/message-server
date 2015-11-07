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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.junit.Assert;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;

public class TopicMessageBuilderTest {

  @Test
  public void testBuild() throws Exception {
    TopicPostMessageRequest request = new TopicPostMessageRequest();
    String topicId = "/i1cglsw8dsa/*/demo/chatter";
    String message = "<message><subject>This is a test</subject><content>Tell me a story</content></message>";
    Map<String, String> content = new HashMap<String, String>();
    content.put("textContent", message);
    content.put("ts", Long.toString(new Date().getTime()));

    request.setContent(content);
    request.setContentType("text");

    AppEntity app = new AppEntity();
    app.setAppId("i1cglsw8dsa");
    app.setServerUserId("adminserver");

    TopicMessageBuilder builder = new TopicMessageBuilder();
    builder.setPubUserId(app.getServerUserId());
    builder.setDomain("localhost");
    builder.setRequest(request);
    builder.setUtcTime(System.currentTimeMillis());
    builder.setItemId(new MessageIdGeneratorImpl().generateItemIdentifier(topicId));
    builder.setTopicId(topicId);
    builder.setAppId(app.getAppId());
    IQ iqPublishMessage = builder.build();

    assertNotNull("Message shouldn't be null", iqPublishMessage);


    JID from = iqPublishMessage.getFrom();
    assertEquals("No matching from", "adminserver%i1cglsw8dsa@localhost", from.toString());

    JID to = iqPublishMessage.getTo();
    assertEquals("No matching to", "pubsub.localhost", to.toString());

    Element iqRoot = iqPublishMessage.getElement();

    Element pubsub = iqRoot.element(TopicMessageBuilder.PUBSUB_NAME);
    Element publish = pubsub.element(TopicMessageBuilder.PUBLISH_NAME);
    Element item = publish.element(TopicMessageBuilder.ITEM_NAME);
    Element mmx = item.element(Constants.MMX);
    Element mmxMeta = mmx.element(Constants.MMX_META);
    assertNotNull("meta element is null", mmxMeta);
    String json = mmxMeta.getText();
    assertNotNull("meta json is null", json);

    JsonParser parser = new JsonParser();
    JsonObject jsonObject = null;
    try {
      jsonObject =  parser.parse(json).getAsJsonObject();
    } catch (JsonSyntaxException e) {
      fail("JsonSyntax exception");
    }

    for (String key : content.keySet()) {
      JsonElement entry = jsonObject.get(key);
      assertNotNull("No entry found for key:" + key, entry);
      String value = entry.getAsString();
      Assert.assertEquals("Non match value for key:" + key, content.get(key), value);
    }

  }
}

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

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.server.plugin.mmxmgmt.web.SendMessageRequest;
import org.dom4j.Element;
import org.junit.Test;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 */
public class MessageBuilderTest {


  @Test
  public void test1Build() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setClientId("importantuser");
    request.setContent("This is a very important message");
    request.setContentType("text");
    request.setRequestAck(true);

    AppEntity app = new AppEntity();
    app.setAppId("testapp");
    app.setServerUserId("admin%i1cglsw8dsa");

    MessageBuilder builder = new MessageBuilder();
    builder.setAppEntity(app);
    builder.setDomain("localhost");
    builder.setUserId(request.getClientId());
    builder.setUtcTime(System.currentTimeMillis());
    builder.setMessageContent(request.getContent());
    builder.setIdGenerator(new MessageIdGeneratorImpl());
    Message m = builder.build();
    /**
     * Simply assert the the message object is not null.
     */
    assertNotNull("Message shouldn't be null", m);
    //TODO add more assertions
  }

  /**
   * Test with content containing XML characters.
   * @throws Exception
   */
  @Test
  public void test2Build() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setClientId("test");
    String message = "<message><subject>This is a test</subject><content>Tell me a story</content></message>";
    request.setContent(message);
    request.setContentType("text");
    request.setRequestAck(true);

    AppEntity app = new AppEntity();
    app.setAppId("i1cglsw8dsa");
    app.setServerUserId("admin%i1cglsw8dsa");

    MessageBuilder builder = new MessageBuilder();
    builder.setAppEntity(app);
    builder.setDomain("localhost");
    builder.setMessageContent(request.getContent());
    builder.setUtcTime(System.currentTimeMillis());
    builder.setIdGenerator(new MessageIdGeneratorImpl());
    builder.setUserId(request.getClientId());
    Message m = builder.build();
    assertNotNull("Message shouldn't be null", m);
    Element mmx = m.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    assertNotNull("mmx element is null", mmx);
    Element payload = mmx.element(Constants.MMX_PAYLOAD);
    assertNotNull("payload element is null", payload);
    String content = payload.getText();
    assertNotNull("payload content is null", content);
    assertEquals("Non matching content", message, content);

    JID from = m.getFrom();
    assertEquals ("Non matching from jid", "admin%i1cglsw8dsa@localhost", from.toString());
    String body = m.getBody();
    assertEquals("Message body is not expected", MMXServerConstants.MESSAGE_BODY_DOT, body);
  }


  @Test
  public void test3BuildWithNameAndValue() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setClientId("test");
    String message = "Simple Test";
    request.setContent(message);
    request.setContentType("text");
    request.setRequestAck(true);
    request.addMeta("apple", "washington");
    request.addMeta("avocado", "california");

    AppEntity app = new AppEntity();
    app.setAppId("i1cglsw8dsa");
    app.setServerUserId("admin%i1cglsw8dsa");

    MessageBuilder builder = new MessageBuilder();
    builder.setAppEntity(app);
    builder.setDomain("localhost");
    builder.setUserId(request.getClientId());
    builder.setMessageContent(request.getContent());
    builder.setUtcTime(System.currentTimeMillis());
    builder.setIdGenerator(new MessageIdGeneratorImpl());
    Message m = builder.build();

    assertNotNull("Message shouldn't be null", m);
  }
}

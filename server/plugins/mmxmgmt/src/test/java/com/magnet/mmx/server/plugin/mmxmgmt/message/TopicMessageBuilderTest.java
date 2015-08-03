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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class TopicMessageBuilderTest {

  @Test
  public void testBuild() throws Exception {
    TopicPostMessageRequest request = new TopicPostMessageRequest();
    String topicId = "/i1cglsw8dsa/*/demo/chatter";
    String message = "<message><subject>This is a test</subject><content>Tell me a story</content></message>";
    request.setContent(message);
    request.setContentType("text");

    AppEntity app = new AppEntity();
    app.setAppId("i1cglsw8dsa");
    app.setServerUserId("adminserver");

    TopicMessageBuilder builder = new TopicMessageBuilder();
    builder.setAppEntity(app);
    builder.setDomain("localhost");
    builder.setRequest(request);
    builder.setUtcTime(System.currentTimeMillis());
    builder.setIdGenerator(new MessageIdGeneratorImpl());
    builder.setTopicId(topicId);
    builder.setAppId(app.getAppId());
    IQ iqPublishMessage = builder.build();

    assertNotNull("Message shouldn't be null", iqPublishMessage);


    JID from = iqPublishMessage.getFrom();
    assertEquals("No matching from", "adminserver%i1cglsw8dsa@localhost", from.toString());

    JID to = iqPublishMessage.getTo();
    assertEquals("No matching to", "pubsub.localhost", to.toString());

  }
}

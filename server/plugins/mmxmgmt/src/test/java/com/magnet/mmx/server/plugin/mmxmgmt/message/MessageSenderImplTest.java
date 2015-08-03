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
import com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Count;
import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplSearchTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.dom4j.Element;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class MessageSenderImplTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageSenderImplTest.class);

  private static BasicDataSource ds;

  @BeforeClass
  public static void setup() throws Exception {
    ds = UnitTestDSProvider.getDataSource();

    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/user-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/app-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/device-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
  }

  @AfterClass
  public static void teardown() {
    try {
      ds.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testSendNonExistingRecipient() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setRecipientUsernames(Collections.singletonList("unknown"));
    String message = "<message><subject>This is a test</subject><content>Tell me a story</content></message>";
    request.setContent(message);

    String appId = "i0sq7ddvi17";

    MessageSenderImpl sender = new StubMessageSenderImpl();
    SendMessageResult result = sender.send(appId, request);
    assertNotNull("message result is null", result);
    Count count = result.getCount();
    assertNotNull("Count object is null", count);
    assertEquals("Non matching requested count", 1, count.getRequested());
    assertEquals("Non matching sent count", 0, count.getSent());
    assertEquals("Non matching unsent count", 1, count.getUnsent());
    List<UnsentMessage> unsentList = result.getUnsentList();
    assertEquals("Unsent list size is not matching", 1, unsentList.size());

  }

  @Test
  public void testSendWithValidRecipient() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setRecipientUsernames(Collections.singletonList("login3"));
    String message = "<message><subject>This is a test</subject><content>Tell me a story</content></message>";
    request.setContent(message);

    String appId = "i0sq7ddvi17";

    MessageSenderImpl sender = new StubMessageSenderImpl();
    SendMessageResult result = sender.send(appId, request);
    assertNotNull("message result is null", result);
    assertFalse("Expect error flag to be set", result.isError());
 }


  @Test
  public void testSendWithBadAppId() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setRecipientUsernames(Collections.singletonList("importantuser"));
    String message = "<message><subject>This is a test</subject><content>Tell me a story</content></message>";
    request.setContent(message);
    request.setReceipt(true);

    String appId = "unknown";

    MessageSenderImpl sender = new StubMessageSenderImpl();
    SendMessageResult result = sender.send(appId, request);
    assertNotNull("message result is null", result);

    boolean error = result.isError();
    assertTrue("Message status doesn't match expected value", error);
    assertTrue("Sent count should null", result.getCount() == null);
  }

  @Test
  public void testSendWithDeviceIdSpecifiedButNotRecipientId() throws Exception {
    SendMessageRequest request = new SendMessageRequest();
    request.setDeviceId("12345678987654322");
    String message = "Simple Message";
    request.setContent(message);
    request.setReceipt(true);
    request.setReplyTo("login3");

    String appId = "AAABSNIBKOstQST7";

    MessageSenderImpl sender = new MessageReturningStubMessageSenderImpl();
    SendMessageResult result = sender.send(appId, request);
    assertNotNull("message result is null", result);

    assertNotNull("Message Id is expected to be not null", result.getSentList().get(0).getMessageId());
    /**
     * validate the message contents
     */
    Message builtMessage = ((MessageReturningStubMessageSenderImpl) sender).messageList.get(0);
    JID to = builtMessage.getTo();
    String expectedUserId = "magnet.way";
    String expectedTo = expectedUserId + JIDUtil.APP_ID_DELIMITER + appId.toLowerCase() + "@" + sender.getDomain() + "/" + request.getDeviceId();  //importantuser%aaabsnibkostqst7@localhost/12345678987654322
    String builtMessageXML = builtMessage.toXML();
    assertNotNull(builtMessageXML);
    assertEquals("Non matching to", expectedTo, to.toString());

    Element receipt = builtMessage.getChildElement(Constants.XMPP_REQUEST, Constants.XMPP_NS_RECEIPTS);
    assertNotNull("Receipt element is missing", receipt);
  }



  private static class StubMessageSenderImpl extends MessageSenderImpl {
    @Override
    protected ConnectionProvider getConnectionProvider() {
      return new BasicDataSourceConnectionProvider(ds);
    }

    @Override
    protected void routeMessage(Message message) {
      assertNotNull(message.getID());
      LOGGER.warn("I am stubbed out and do no routing");
    }

    @Override
    protected String getDomain() {
      return "localhost";
    }
  }

  private static class MessageReturningStubMessageSenderImpl extends StubMessageSenderImpl {
    private List<Message> messageList = new ArrayList<Message>(10);
    @Override
    protected ConnectionProvider getConnectionProvider() {
      return new BasicDataSourceConnectionProvider(ds);
    }

    @Override
    protected void routeMessage(Message message) {
      super.routeMessage(message);
      messageList.add(message);
    }

    @Override
    protected String getDomain() {
      return "localhost";
    }
  }
}

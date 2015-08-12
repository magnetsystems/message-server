package com.magnet.mmx.server.plugin.mmxmgmt.interceptor;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.Utils;
import junit.framework.TestCase;
import org.dom4j.Element;
import org.xmpp.packet.Message;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rphadnis on 8/11/15.
 */
public class MessageDistributedAnnotatorTest extends TestCase {

  public void test1Annotate() throws Exception {

    String appId = "testapp1";
    String domain = "mmx";
    String user = "rahul";

    Message myMessage = new Message();
    myMessage.setType(Message.Type.chat);
    myMessage.setFrom(appId + "%" + appId + "@" + domain);
    myMessage.setTo(user + "%" + appId + "@" + domain);
    myMessage.setID("10");
    Element mmxElement = myMessage.addChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    Element mmxMetaElement = mmxElement.addElement(Constants.MMX_MMXMETA);

    Map<String, String> mmxMetaMap = new HashMap<String, String>();
    mmxMetaMap.put("txId", "2010");
    mmxMetaMap.put("node", "test1_node");

    String mmxMetaJSON = GsonData.getGson().toJson(mmxMetaMap);
    mmxMetaElement.setText(mmxMetaJSON);

    Element payloadElement = mmxElement.addElement(Constants.MMX_PAYLOAD);

    DateFormat fmt = Utils.buildISO8601DateFormat();
    String formattedDateTime = fmt.format(new Date());
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = ".";
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, MessageBuilder.buildChunkAttributeValue(text));
    myMessage.setBody(MMXServerConstants.MESSAGE_BODY_DOT);

    MessageDistributedAnnotator messageDistributedAnnotator = new MessageDistributedAnnotator();
    messageDistributedAnnotator.annotate(myMessage);

    Element mmx = myMessage.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    Element internalMeta = mmx.element(Constants.MMX_MMXMETA);
    String revisedJSON = internalMeta.getText();
    assertNotNull(revisedJSON);

    String expected = "{\"node\":\"test1_node\",\"txId\":\"2010\",\"mmxdistributed\":true}";

    assertEquals("Non matching mmxmeta json", expected, revisedJSON);

  }

  /**
   * Case where the incoming message has no mmxmeta
   * @throws Exception
   */
  public void test2Annotate() throws Exception {

    String appId = "testapp1";
    String domain = "mmx";
    String user = "rahul";

    Message myMessage = new Message();
    myMessage.setType(Message.Type.chat);
    myMessage.setFrom(appId + "%" + appId + "@" + domain);
    myMessage.setTo(user + "%" + appId + "@" + domain);
    myMessage.setID("10");
    Element mmxElement = myMessage.addChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);

    Element payloadElement = mmxElement.addElement(Constants.MMX_PAYLOAD);

    DateFormat fmt = Utils.buildISO8601DateFormat();
    String formattedDateTime = fmt.format(new Date());
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = ".";
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, MessageBuilder.buildChunkAttributeValue(text));
    myMessage.setBody(MMXServerConstants.MESSAGE_BODY_DOT);

    MessageDistributedAnnotator messageDistributedAnnotator = new MessageDistributedAnnotator();
    messageDistributedAnnotator.annotate(myMessage);

    Element mmx = myMessage.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    Element internalMeta = mmx.element(Constants.MMX_MMXMETA);
    String revisedJSON = internalMeta.getText();
    assertNotNull(revisedJSON);

    String expected = "{\"mmxdistributed\":true}";

    assertEquals("Non matching mmxmeta json", expected, revisedJSON);

  }

  /**
   * Test the case where we already have the distributed flag set.
   * @throws Exception
   */
  public void test3Annotate() throws Exception {

    String appId = "testapp1";
    String domain = "mmx";
    String user = "rahul";

    Message myMessage = new Message();
    myMessage.setType(Message.Type.chat);
    myMessage.setFrom(appId + "%" + appId + "@" + domain);
    myMessage.setTo(user + "%" + appId + "@" + domain);
    myMessage.setID("10");
    Element mmxElement = myMessage.addChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    Element mmxMetaElement = mmxElement.addElement(Constants.MMX_MMXMETA);

    Map<String, String> mmxMetaMap = new HashMap<String, String>();
    mmxMetaMap.put("txId", "2010");
    mmxMetaMap.put("node", "test1_node");
    mmxMetaMap.put(MMXServerConstants.DISTRIBUTED_KEY, "false");

    String mmxMetaJSON = GsonData.getGson().toJson(mmxMetaMap);
    mmxMetaElement.setText(mmxMetaJSON);

    Element payloadElement = mmxElement.addElement(Constants.MMX_PAYLOAD);

    DateFormat fmt = Utils.buildISO8601DateFormat();
    String formattedDateTime = fmt.format(new Date());
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = ".";
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, MessageBuilder.buildChunkAttributeValue(text));
    myMessage.setBody(MMXServerConstants.MESSAGE_BODY_DOT);

    MessageDistributedAnnotator messageDistributedAnnotator = new MessageDistributedAnnotator();
    messageDistributedAnnotator.annotate(myMessage);

    Element mmx = myMessage.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    Element internalMeta = mmx.element(Constants.MMX_MMXMETA);
    String revisedJSON = internalMeta.getText();
    assertNotNull(revisedJSON);

    String expected = "{\"node\":\"test1_node\",\"txId\":\"2010\",\"mmxdistributed\":true}";

    assertEquals("Non matching mmxmeta json", expected, revisedJSON);
  }


  /**
   * Test the case where we already have the distributed flag set.
   * @throws Exception
   */
  public void test1IsAnnotated() throws Exception {

    String appId = "testapp1";
    String domain = "mmx";
    String user = "rahul";

    Message myMessage = new Message();
    myMessage.setType(Message.Type.chat);
    myMessage.setFrom(appId + "%" + appId + "@" + domain);
    myMessage.setTo(user + "%" + appId + "@" + domain);
    myMessage.setID("10");
    Element mmxElement = myMessage.addChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    Element mmxMetaElement = mmxElement.addElement(Constants.MMX_MMXMETA);

    Map<String, String> mmxMetaMap = new HashMap<String, String>();
    mmxMetaMap.put("txId", "2010");
    mmxMetaMap.put("node", "test1_node");
    mmxMetaMap.put(MMXServerConstants.DISTRIBUTED_KEY, "false");

    String mmxMetaJSON = GsonData.getGson().toJson(mmxMetaMap);
    mmxMetaElement.setText(mmxMetaJSON);

    Element payloadElement = mmxElement.addElement(Constants.MMX_PAYLOAD);

    DateFormat fmt = Utils.buildISO8601DateFormat();
    String formattedDateTime = fmt.format(new Date());
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = ".";
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, MessageBuilder.buildChunkAttributeValue(text));
    myMessage.setBody(MMXServerConstants.MESSAGE_BODY_DOT);

    MessageDistributedAnnotator messageDistributedAnnotator = new MessageDistributedAnnotator();
    messageDistributedAnnotator.annotate(myMessage);

    boolean isAnnotated = messageDistributedAnnotator.isAnnotated(myMessage);
    assertTrue("Message is not annotated", isAnnotated);
  }


}
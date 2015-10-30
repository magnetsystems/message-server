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
package com.magnet.mmx.server.plugin.mmxmgmt.bot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import com.google.gson.Gson;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.MmxHeaders;
import com.magnet.mmx.protocol.MsgAck;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageIdGenerator;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageIdGeneratorImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.GsonData;

public class BotRegistrationImpl implements BotRegistration {
  private static final Logger LOGGER = LoggerFactory.getLogger(BotRegistrationImpl.class);

  private static final String BOT_DEVICE_ID_TEMPLATE = "%s-%s-bot-device";
  private static final String BOT_DEVICE_NAME_TEMPLATE = "%s Device";
  private static final OSType BOT_DEVICE_OS_TYPE = OSType.OTHER;
  private static final String AMAZING_MESSAGE = "This is simply amazing";

  /**
   * Register a bot with the specified mmxUserName.
   *
   * @param appId
   * @param botUserName
   */
  @Override
  public void registerBot(String appId, String botUserName, AutoResponseProcessor processor) {
    String mmxUserName = JIDUtil.makeNode(botUserName, appId);
    LOGGER.debug("Registering bot with name:{}", mmxUserName);
    String botDeviceId = deviceId(appId, botUserName);
    AutoRespondingConnection bot = new AutoRespondingConnection(processor);
    try {
      bot.login(mmxUserName, botDeviceId);
      // Set the user's presence
      Presence presence = new Presence();
      presence.setStatus("Online");
      bot.sendPacket(presence);

      /*
       * Register a device for the bot user
       */
      LOGGER.debug("Creating a device for the bot user");
      DeviceDAO deviceDAO = getDeviceDAO();
      DeviceEntity deviceEntity = deviceDAO.getDevice(botDeviceId, BOT_DEVICE_OS_TYPE, appId);
      if (deviceEntity == null) {
        //we need to create a new one
        DevReg deviceRegistration = new DevReg();
        deviceRegistration.setOsType(BOT_DEVICE_OS_TYPE.name());
        deviceRegistration.setDisplayName(deviceName(appId, botUserName));
        deviceRegistration.setDevId(botDeviceId);
        deviceDAO.addDevice(botUserName, appId, deviceRegistration);
        LOGGER.debug("Created device with id:{} for bot user:{}", botDeviceId, botUserName);
      }
    } catch (Throwable e) {
      LOGGER.warn("Exception", e);
    }
  }


  /**
   * Generate a deviceId for bot's device.
   *
   * @param appId
   * @param botUserName
   * @return
   */
  protected String deviceId(String appId, String botUserName) {
    String deviceId = String.format(BOT_DEVICE_ID_TEMPLATE, appId, botUserName);
    return deviceId;
  }

  protected DeviceDAO getDeviceDAO() {
    return new DeviceDAOImpl(new OpenFireDBConnectionProvider());

  }

  /**
   * Get the device name for bot's device.
   *
   * @param appId
   * @param botUserName
   * @return
   */
  protected String deviceName(String appId, String botUserName) {
    String name = String.format(BOT_DEVICE_NAME_TEMPLATE, botUserName);
    return name;
  }


  protected static IQ buildAckIQ(String fromJID, String toJID, String incomingMessageId) {
    IQ ackIQ = new IQ(IQ.Type.set);

    Element mmxElement = ackIQ.setChildElement(Constants.MMX, Constants.MMX_NS_MSG_ACK);
    mmxElement.addAttribute(Constants.MMX_ATTR_COMMAND, Constants.MessageCommand.ack.toString());
    mmxElement.addAttribute(Constants.MMX_ATTR_CTYPE, GsonData.CONTENT_TYPE_JSON);

    MsgAck msgAck = new MsgAck(fromJID, toJID, incomingMessageId);
    Gson gson = GsonData.getGson();
    String ackJSON = gson.toJson(msgAck);
    mmxElement.setText(ackJSON);
    return ackIQ;
  }

  /**
   * Bot processor that responds with "This is amazing" to all messages.
   */
  public static class AmazingBotProcessor implements AutoResponseProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazingBotProcessor.class);
    protected AutoRespondingConnection autoRespondingConnection;

    @Override
    public void initialize(AutoRespondingConnection connection) {
      this.autoRespondingConnection = connection;
    }

    public void processIncoming(Packet packet) {
      if (packet instanceof Message) {
        Message message = (Message) packet;

        LOGGER.debug("Sending the amazing message back");
        JID fromJID = packet.getFrom();
        JID toJID = packet.getTo();
        packet.setTo(fromJID);

        Element mmx = message.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
        Element recieptRequest = message.getChildElement(Constants.XMPP_REQUEST, Constants.XMPP_NS_RECEIPTS);
        boolean receiptRequested = recieptRequest != null;

        List<Element> payloadList = mmx.elements(Constants.MMX_PAYLOAD);
        if (payloadList != null || !payloadList.isEmpty()) {
          Element payload = payloadList.get(0);
          payload.setText(AMAZING_MESSAGE);
        }
        // mmx meta
        Element internalMeta = mmx.element(Constants.MMX_MMXMETA);
        if (internalMeta != null) {
          mmx.remove(internalMeta);
        }
        String userId = JIDUtil.getUserId(fromJID);
        String devId = fromJID.getResource();
        String senderId = JIDUtil.getUserId(toJID);
        String senderDevId = toJID.getResource();
        MmxHeaders mmxMeta = new MmxHeaders();
        mmxMeta.put(MmxHeaders.TO, new MMXid[] {new MMXid(userId, devId, null)});
        mmxMeta.put(MmxHeaders.FROM, new MMXid(senderId, senderDevId, null));
        Element revisedMeta = mmx.addElement(Constants.MMX_MMXMETA);
        revisedMeta.setText(GsonData.getGson().toJson(mmxMeta));
        
        //add the content to meta (as requested by iOS team) and replace the meta object.
        Map<String, String> metaMap =  new HashMap<String, String>();
        metaMap.put(MMXServerConstants.TEXT_CONTENT_KEY, AMAZING_MESSAGE);
        Element meta = mmx.element(Constants.MMX_META);
        if (meta == null) {
          meta = mmx.addElement(Constants.MMX_META);
        }
        meta.setText(GsonData.getGson().toJson(metaMap));

        //remove the receipt request element if it exists in the incoming request
        if (receiptRequested) {
          message.deleteExtension(Constants.XMPP_REQUEST, Constants.XMPP_NS_RECEIPTS);
        }
        try {
          //add a little sleep to differentiate between the send and recvd time.
          Thread.sleep(1000L);
        } catch (InterruptedException e) {
          LOGGER.debug("Interrupted exception", e);
        } finally {
        }
        autoRespondingConnection.sendPacket(packet);
        /*
         * Acknowledge the receipt by sending an IQ
         */
        String originalMessageId = packet.getID();
        String from = fromJID.toString();
        String to = toJID.toString();

        LOGGER.debug("Attempting to deliver ack");
        IQ ackIQ = buildAckIQ(from, to, originalMessageId);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Ack IQ:{}", ackIQ.toXML());
        }
        autoRespondingConnection.sendPacket(ackIQ);

        if (receiptRequested) {
          LOGGER.debug("Sending delivery receipt for message with id:{}", originalMessageId);
          String appId = JIDUtil.getAppId(to);
          Message receipt = buildDeliveryReceipt(appId, from, to, originalMessageId);
          autoRespondingConnection.sendPacket(receipt);
        }

      }
    }

    public void processIncomingRaw(String rawText) {
      LOGGER.info("process Incoming raw");
    }

    public void terminate() {
      LOGGER.info("Terminating Bot processor ");
      if (autoRespondingConnection != null) {
        autoRespondingConnection = null;
      }
    }




    protected Message buildDeliveryReceipt(String appId, String incomingMessageFromJID, String incomingMessageToJID,
                                           String incomingMessageId) {
      MessageIdGenerator generator = new MessageIdGeneratorImpl();
      String id = generator.generate(incomingMessageFromJID, appId, null);
      Message receipt = new Message();
      receipt.setID(id);
      receipt.setFrom(incomingMessageToJID);
      receipt.setTo(incomingMessageFromJID);
      Element received = receipt.addChildElement(Constants.XMPP_RECEIVED, Constants.XMPP_NS_RECEIPTS);
      received.addAttribute(Constants.XMPP_ATTR_ID, incomingMessageId);
      return receipt;
    }
  }


  /**
   * Bot that echoes incoming messages.
   */
  public static class EchoBotProcessor extends AmazingBotProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoBotProcessor.class);

    public void processIncoming(Packet packet) {
      if (packet instanceof Message) {
        LOGGER.debug("Sending the same message back");
        Message message = (Message) packet;
        Element mmx = message.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
        boolean receiptRequested = message.getChildElement(Constants.XMPP_REQUEST, Constants.XMPP_NS_RECEIPTS) != null;

        JID fromJID = message.getFrom();
        JID toJID = message.getTo();
        message.setTo(fromJID);

        Element internalMeta = mmx.element(Constants.MMX_MMXMETA);
        String userId = JIDUtil.getUserId(fromJID);
        String devId = fromJID.getResource();
        String mmxMetaJSON = MMXMetaBuilder.build(userId, devId);
        if (internalMeta != null) {
          mmx.remove(internalMeta);
        }

        Element revisedMeta = mmx.addElement(Constants.MMX_MMXMETA);
        revisedMeta.setText(mmxMetaJSON);

        if (receiptRequested) {
          //remove the receipt requested element to ensure we don't have a loop
          message.deleteExtension(Constants.XMPP_REQUEST, Constants.XMPP_NS_RECEIPTS);
        }
        try {
          //add a little sleep to differentiate between the send and recvd time.
          Thread.sleep(1000L);
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
        }
        autoRespondingConnection.sendPacket(message);
          /*
           * Acknowledge the receipt by sending an IQ
           */
        String originalMessageId = message.getID();
        String from = fromJID.toString();
        String to = toJID.toString();

        LOGGER.debug("Attempting to deliver ack");
        IQ ackIQ = buildAckIQ(from, to, originalMessageId);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Ack IQ:{}", ackIQ.toXML());
        }
        autoRespondingConnection.sendPacket(ackIQ);

        if (receiptRequested) {
          LOGGER.debug("Sending delivery receipt for message with id:{}", originalMessageId);
          String appId = JIDUtil.getAppId(to);
          Message receipt = buildDeliveryReceipt(appId, from, to, originalMessageId);
          autoRespondingConnection.sendPacket(receipt);
        } else {
          LOGGER.debug("Not sending delivery receipt for message with id:{}", originalMessageId);
        }
      }
    }
  }

}

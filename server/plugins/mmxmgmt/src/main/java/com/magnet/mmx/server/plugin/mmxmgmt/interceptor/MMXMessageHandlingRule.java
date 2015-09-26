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
package com.magnet.mmx.server.plugin.mmxmgmt.interceptor;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.PacketExtension;

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXError;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.MmxHeaders;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceNotFoundException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXXmppRateExceededEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.message.ErrorMessageBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.message.ServerAckMessageBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.monitoring.RateLimiterDescriptor;
import com.magnet.mmx.server.plugin.mmxmgmt.monitoring.RateLimiterService;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AlertEventsManager;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AlertsUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXExecutors;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXOfflineStorageUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.server.plugin.mmxmgmt.util.WakeupUtil;
import com.magnet.mmx.util.GsonData;

public class MMXMessageHandlingRule {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXMessageHandlingRule.class);
  private static final String SERVER_USER = "serveruser";
  private static final String SERVER_ACK_SENDER_POOL = "ServerAckSenderPool";

  public void handle(MMXMsgRuleInput input) throws PacketRejectedException {
    LOGGER.trace("handle : input={}", input);
    /*
     * If the message is processed do nothing.
     */
    if(input.isProcessed()) {
      LOGGER.trace("handle : Already processed returning input={}", input);
      return;
    }

    /**
     * Process any incoming messages which are addressed to "serveruser" bareJID
     * and that are receipts.
     */

    if (input.isBareJID() && input.isReceipt() && input.getMessage().getTo().toString().startsWith(SERVER_USER)) {
      processMessageReceiptForServerUser(input);
      //stop processing of the message since server user is not going to have any devices and hence the
      //message can't be routed.
      LOGGER.trace("handle : done processing bareJID, receipt, stop further processing input={}", input);
      throw new PacketRejectedException("Stopping processing for the message addressed to bareJID=" + input.getMessage().getTo());
    }

    /**
     * At this point only unprocessed messages will be processed
     *
     * A message with bareJID can only be incoming.
     * A message with bareJID can never be a receipt
     *
     * For matched rule distribute the message, then stop processing by throwing an exception
     *
     */

    if(input.isBareJID()) {
      LOGGER.trace("handle : processing bareJID input={}", input);
      if(input.isIncoming() && !input.isReceipt()) {
        LOGGER.trace("handle : handling incoming non-receipt message with bareJID input={}", input);
        handleBareJID(input.getMessage());
      }
      LOGGER.trace("handle : done processing bareJID, stop further processing input={}", input);
      throw new PacketRejectedException("Stopping processing for the message addressed to bareJID=" + input.getMessage().getTo());
    }

    /**
     * At this point only unprocessed messages with fullJID's will be handled
     *
     * If the message is an outgoing message, store the message and return
     *
     */

    if(!input.isIncoming()) {
      LOGGER.trace("handle : message is an outgoing message storing input={}", input);
      MMXOfflineStorageUtil.storeMessage(input.getMessage());
      return;
    }

    /*
     * At this point only unprocessed incoming messages with fullJID will be handled
     *
     * If the message is a receipt, store the message, make the relevant state changes and return
     *
     */

    if(input.isReceipt()) {
      processMessageReceipt(input);
      return;
    }

    /**
     * At this point only unprocessed, incoming, fullJID, non-receipt messages will be processed
     *
     */
    {
      LOGGER.trace("handle : handling unprocessed, incoming, non-receipt message with fullJID messageId={}", input.getMessage().getID());

      String appId = JIDUtil.getAppId(input.getMessage().getTo());
      int rate = MMXConfiguration.getConfiguration().getInt(MMXConfigKeys.MAX_XMPP_RATE, MMXServerConstants.DEFAULT_MAX_XMPP_RATE);
      RateLimiterDescriptor descriptor = new RateLimiterDescriptor(MMXServerConstants.XMPP_RATE_TYPE, appId, rate);
      LOGGER.trace("handle : checking rate limite for descriptor={}", descriptor);
      if(!RateLimiterService.isAllowed(descriptor)) {
        LOGGER.error("handle : Max xmpp message rate reached : {}, appId : {}", rate, appId);
        AlertEventsManager.post(new MMXXmppRateExceededEvent(appId, AlertsUtil.getMaxXmppRate()));
        throw new PacketRejectedException("Max message rate has been reached");
      }

      String deviceId = input.getMessage().getTo().getResource();
      DeviceEntity deviceEntity = null;
      if(!Strings.isNullOrEmpty(deviceId) && !Strings.isNullOrEmpty(appId)) {
        try {
          DeviceDAO deviceDao = DBUtil.getDeviceDAO();
          deviceEntity = deviceDao.getDevice(appId, deviceId);
          if(deviceEntity == null) {
            LOGGER.trace("isValidDistributableMessage : No device found for appId={}, deviceId={}, sending error message", appId, deviceId);
            sendDeviceNotFoundErrorMsg(input.getMessage());
            throw new PacketRejectedException("Invalid deviceId : " + deviceId);
          }
        } catch (SQLException e) {
          LOGGER.error("isValidDistributableMessage : No device found for appId={}, deviceId={}, sending error message", appId, deviceId, e);
          sendDeviceNotFoundErrorMsg(input.getMessage());
          throw new PacketRejectedException("Invalid deviceId : " + deviceId);
        } catch (DeviceNotFoundException e) {
          LOGGER.error("isValidDistributableMessage : No device found for appId={}, deviceId={}, sending error message", appId, deviceId, e);
          sendDeviceNotFoundErrorMsg(input.getMessage());
          throw new PacketRejectedException("Invalid deviceId : " + deviceId);
        }
      }

      MessageEntity messageEntity = getMessageEntity(input.getMessage());
      MMXPresenceFinder presenceFinder = new MMXPresenceFinderImpl();
      boolean isOnline = presenceFinder.isOnline(input.getMessage().getTo());
      AppDAO appDAO = DBUtil.getAppDAO();
      AppEntity appEntity = appDAO.getAppForAppKey(appId);
      if (!isOnline) {
        MMXOfflineStorageUtil.storeMessage(input.getMessage());
        /**
         * Check if the device has a valid client token that can be used for wakeup. If it doesn't then
         * we set the message state to PENDING.
         */
        boolean wakeupPossible = canBeWokenUp(deviceEntity);
        if (wakeupPossible) {
          messageEntity.setState(MessageEntity.MessageState.WAKEUP_REQUIRED);
          WakeupUtil.queueWakeup(appEntity, deviceEntity, messageEntity.getMessageId());
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("isValidDistributableMessage : wakeup not possible for device with id:{}", deviceId);
          }
          messageEntity.setState(MessageEntity.MessageState.PENDING);
        }
        DBUtil.getMessageDAO().persist(messageEntity);
      } else {
        messageEntity.setState(MessageEntity.MessageState.DELIVERY_ATTEMPTED);
        DBUtil.getMessageDAO().persist(messageEntity);
      }
      //send a server ack message if the message is direct message to a full JID and not a distributed message.
      Message mmxMessage = input.getMessage();
      MessageAnnotator annotator = new MessageDistributedAnnotator();
      boolean isDistributed = annotator.isAnnotated(mmxMessage);
      if (!isDistributed) {
        //send the server ack message.
        ServerAckMessageBuilder serverAckMessageBuilder = new ServerAckMessageBuilder(mmxMessage, appEntity.getAppId());
        Message serverAck = serverAckMessageBuilder.build();
        sendServerAckMessage(serverAck);
      }
      if (!isOnline) {
        //stop further processing of the message by throwing packet rejected exception since the user is not online.
        throw new PacketRejectedException("Device offline, stopping processing for the message addressed to fullJID="
            + input.getMessage().getTo());
      }
    }
  }

  private static MessageEntity getMessageEntity(Message message) {
    MessageEntity messageEntity = new MessageEntity();
    messageEntity.setMessageId(message.getID());
    messageEntity.setAppId(JIDUtil.getAppId(message.getTo()));
    messageEntity.setFrom(message.getFrom().toString());
    messageEntity.setTo(message.getTo().toString());
    messageEntity.setDeviceId( message.getTo().getResource());
    return messageEntity;
  }

  public void handleMMXMulticast(MMXMsgRuleInput input) throws PacketRejectedException {
    Message message = input.getMessage();
    JID mcastJid = message.getTo();
    String appId = JIDUtil.getAppId(mcastJid);
    PacketExtension payload = message.getExtension(Constants.MMX,
        Constants.MMX_NS_MSG_PAYLOAD);
    if (payload == null) {
      // TODO: not an MMX msg.  We should send an error instead of drop it.
      LOGGER.warn("Dropping a malformed MMX multicast message.");
      throw new PacketRejectedException("Stop processing for malformed multicast message");
    }

    MMXid[] recipients = getRecipients(payload);
    if (recipients == null || recipients.length == 0) {
      LOGGER.warn("No recipients found in MMX multicast message");
    } else {
      PacketRouter pktRouter = XMPPServer.getInstance().getPacketRouter();
      for (MMXid recipient : recipients) {
        JID jid = new JID(JIDUtil.makeNode(recipient.getUserId(), appId),
                            mcastJid.getDomain(), recipient.getDeviceId(), true);
        // TODO: payload has a DOM parent; do a deep copy.  Need a cheaper way.
        Message unicastMsg = message.createCopy();
        unicastMsg.setTo(jid);
        pktRouter.route(unicastMsg);
      }
    }
    throw new PacketRejectedException("MMX multicast message is processed");
  }

  private MMXid[] getRecipients(PacketExtension payload) {
    Element mmxElement = payload.getElement();
    if (mmxElement == null) {
      return null;
    }
    Element element = mmxElement.element(Constants.MMX_MMXMETA);
    if (element == null) {
      return null;
    }
    MmxHeaders mmxMeta = GsonData.getGson().fromJson(element.getText(),
        MmxHeaders.class);
    List<Map<String, String>> list = (List<Map<String, String>>) mmxMeta.getHeader(
        MmxHeaders.TO, null);
    if (list == null) {
      return null;
    }
    int i = 0;
    MMXid[] recipients = new MMXid[list.size()];
    for (Map<String, String> map : list) {
      recipients[i++] = MMXid.fromMap(map);
    }
    return recipients;
  }
  
  private void handleBareJID(Message message) {
    if (message.getTo().getNode() == null) {
      LOGGER.trace("handleBareJID: ignoring a multicast message={}", message);
      // It is a multicast message (XEP-0033); let MulticastRouter handle it.
      return;
    }
    //annotate the message to indicate that we have distributed it.
    MessageAnnotator annotator = new MessageDistributedAnnotator();
    annotator.annotate(message);
    LOGGER.trace("handleBareJID : message={}", message);
    MessageEntity messageEntity = MMXMessageHandlingRule.getMessageEntity(message);
    String domain = message.getTo().getDomain();
    String userId = JIDUtil.getUserId(message.getTo());
    MessageDistributor distributor = new MessageDistributorImpl();
    MessageDistributor.DistributionContext context = new DistributionContextImpl(userId, messageEntity.getAppId(), domain, messageEntity.getMessageId());
    MessageDistributor.DistributionResult result = distributor.distribute(message, context);
    AppDAO appDAO = DBUtil.getAppDAO();
    AppEntity appEntity = appDAO.getAppForAppKey(messageEntity.getAppId());
    List<MessageDistributor.JIDDevicePair> undistributed = result.getNotDistributed();
    for(MessageDistributor.JIDDevicePair pair : undistributed) {
      message.setTo(pair.getJID());
      MMXOfflineStorageUtil.storeMessage(message);
      messageEntity.setTo(message.getTo().toString());
      messageEntity.setDeviceId(pair.getJID().getResource());
      boolean wokenUpPossible = canBeWokenUp(pair.getDevice());
      if (wokenUpPossible) {
        messageEntity.setState(MessageEntity.MessageState.WAKEUP_REQUIRED);
        WakeupUtil.queueWakeup(appEntity, pair.getDevice(), messageEntity.getMessageId());
      } else {
        messageEntity.setState(MessageEntity.MessageState.PENDING);
      }
      DBUtil.getMessageDAO().persist(messageEntity);
    }

    if (result.noDevices()) {
      LOGGER.warn("message={} addressed to user={} is dropped because the user has no active devices. Sending an error message back to originator.", message, userId);
      //TODO: return a message to the sender using the error stanza
      //use the thread pool to send the error message.
      MMXError error = new MMXError(StatusCode.NOT_FOUND)
          .setMessage(PacketError.Condition.recipient_unavailable.toString())
          .setSeverity(MMXError.Severity.TRIVIAL)
          .setParams(JIDUtil.getReadableUserId(message.getTo()));
      Message errorMessage = new ErrorMessageBuilder(message)
          .setError(error)
          .build();
      XMPPServer.getInstance().getRoutingTable().routePacket(message.getFrom(), errorMessage, true);
    } else {
      //build a message sent ack message to the sender of this message.
      ServerAckMessageBuilder serverAckMessageBuilder = new ServerAckMessageBuilder(message, appEntity.getAppId());
      Message serverAck = serverAckMessageBuilder.build();
      sendServerAckMessage(serverAck);
    }
  }

  /**
   * Process message receipts for messages addressed to serveruser.
   * @param input
   */
  private void processMessageReceiptForServerUser(MMXMsgRuleInput input) {
    LOGGER.trace("handle : handling incoming, unprocessed receipt message with bareJID messageId={}", input.getMessage().getID());
    DeliveryConfirmationMessage confirmation = DeliveryConfirmationMessage.build(input.getMessage());
    LOGGER.trace("handle : built from message, confirmation={}", confirmation);
    DBUtil.getMessageDAO().messageReceived(confirmation.getMessageId(), confirmation.getConfirmingDeviceId());
  }

  /**
   * Process regular message receipts addressed to full JIDs.
   * @param input
   */
  private void processMessageReceipt(MMXMsgRuleInput input) {
    LOGGER.trace("handle : handling incoming, unprocessed receipt message with fullJID messageId={}", input.getMessage().getID());
    DeliveryConfirmationMessage confirmation = DeliveryConfirmationMessage.build(input.getMessage());
    LOGGER.trace("handle : built from message, confirmation={}", confirmation);
    DBUtil.getMessageDAO().messageReceived(confirmation.getMessageId(), confirmation.getConfirmingDeviceId());
    MessageEntity messageEntity = getMessageEntity(input.getMessage());
    messageEntity.setSourceMessageId(confirmation.getMessageId());
    messageEntity.setType(MessageEntity.MessageType.RECEIPT);
    messageEntity.setState(MessageEntity.MessageState.DELIVERY_ATTEMPTED);
    DBUtil.getMessageDAO().persist(messageEntity);
  }

  private void sendDeviceNotFoundErrorMsg(Message mmxMessage) {
    MMXError error = new MMXError(StatusCode.BAD_REQUEST)
            .setMessage(PacketError.Condition.item_not_found.toString())
            .setSeverity(MMXError.Severity.TRIVIAL);
    Message errorMessage = new ErrorMessageBuilder(mmxMessage)
            .setError(error)
            .build();
    XMPPServer.getInstance().getRoutingTable().routePacket(mmxMessage.getFrom(), errorMessage, true);
    LOGGER.trace("sendDeviceNotFoundErrorMsg : errorMessage={}", errorMessage.getBody());
  }

  private boolean canBeWokenUp(DeviceEntity deviceEntity) {
    return deviceEntity != null && deviceEntity.getClientToken() != null &&
        deviceEntity.getPushStatus() != PushStatus.INVALID;
  }


  /**
   * Send the serverAck message asynchronously.
   * @param serverAckMessage
   */
  protected void sendServerAckMessage(final Message serverAckMessage) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Sending server ack message:{}", serverAckMessage);
    }
    ExecutorService service = MMXExecutors.getOrCreate(SERVER_ACK_SENDER_POOL, 10);
    service.submit(new Runnable() {
      @Override
      public void run() {
        PacketRouter router = XMPPServer.getInstance().getPacketRouter();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Sending server ack message:{}", serverAckMessage);
        }
        router.route(serverAckMessage);
      }
    });
  }
}

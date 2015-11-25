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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.models.PublisherModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.magnet.mmx.protocol.Count;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.SentMessageId;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Target;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DeviceQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.UserQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceTargetResolver;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserTargetResolver;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXTopicManager;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.TopicResource;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXExecutors;
import com.magnet.mmx.util.TopicHelper;

/**
 */
public class MessageSenderImpl implements MessageSender {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageSenderImpl.class);
  private static final String ERROR_INVALID_APPID = "Supplied application id is invalid.";
  private static final String ERROR_INVALID_SEND_MESSAGE_REQUEST = "Supplied send message request is invalid.";
  private static final String ERROR_INVALID_SEND_MESSAGE_DEVICE = "Supplied device id is invalid.";
  private static final String ERROR_INVALID_POST_MESSAGE_TOPIC = "Supplied topic id is invalid.";
  private static final String ERROR_NULL_OR_EMPTY_META = "No content provided for the message.";

  private static final int FAILURE_CODE_NULL_REQUEST = 1001;
  private static final int FAILURE_CODE_BAD_DEVICE_ID = 1003;
  private static final int FAILURE_CODE_NO_CONTENT = 1004;

  private static final String SEND_MESSAGE_STATUS_OK = "OK";
  private static final String SEND_MESSAGE_STATUS_ERROR = "ERROR";

  private static final String POOL_NAME = "RESTMessageSender";
  private static final int POOL_SIZE = 10;
  private static final int MAX_USERNAME_COUNT = 1000;


  @Override
  public SendMessageResult send(String senderUserId, String appId,
        com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageRequest request) {
    ConnectionProvider provider = getConnectionProvider();
    AppDAO appDAO = new AppDAOImpl(provider);
    DeviceDAO deviceDAO = new DeviceDAOImpl(provider);
    SendMessageResult result = null;

    String domain = getDomain();

    ValidationResult validationResult = validateRequest(appId, request, appDAO, deviceDAO);
    if (validationResult.isValid()) {
      /*
        process the request and send the message
       */
      int requested = 0;
      int sent = 0;
      int unsent = 0;

      List<SentMessageId> sentList = new LinkedList<SentMessageId>();
      List<UnsentMessage> unsentList = new LinkedList<UnsentMessage>();
      Count count = null;

      // Only one message ID as a multicast message.
      String msgId = (new MessageIdGeneratorImpl()).generateItemIdentifier(appId);
      ArrayList<MMXid> recipients = new ArrayList<MMXid>();
      if (request.getRecipientUserIds() != null && !request.getRecipientUserIds().isEmpty()) {
        List<String> userList = request.getRecipientUserIds();
        UserDAO userDAO = new UserDAOImpl(getConnectionProvider());
        for (String userId : userList) {
          requested++;
          String mmxUsername = Helper.getMMXUsername(userId, appId);
          UserEntity userEntity = userDAO.getUser(mmxUsername);
          // Although it is redundant to verify the user ID (the intercepter
          // will validate it), the REST response has the unsent list.
          if (userEntity == null) {
            LOGGER.info("User with id:{} not found", userId);
            UnsentMessage badUser = new UnsentMessage(userId,
                ErrorCode.INVALID_USER_NAME.getCode(), ErrorMessages.ERROR_USERNAME_NOT_FOUND);
            unsentList.add(badUser);
            unsent++;
          } else {
            recipients.add(new MMXid(userId, null));
            SentMessageId sentMessageId = new SentMessageId(userId, null, msgId);
            sentList.add(sentMessageId);
            sent++;
          }
        }
        if (sent > 0) {
          MessageBuilder builder = new MessageBuilder();
          Message message = builder.setAppId(appId)
              .setDomain(domain)
              .setId(msgId)
              .setUtcTime(System.currentTimeMillis())
              .setSenderId(new MMXid(senderUserId, null))
              .setRecipientIds(recipients.toArray(new MMXid[sent]))
              .setReplyTo(request.getReplyTo())
              .setNoAck(true)
              .setMetadata(request.getContent())
              .setReceipt(request.isReceipt())
              .build();
          routeMessage(message);
        }
        //prepare the response
        count = new Count(requested, sent, unsent);
        SendMessageResult internalResult = new SendMessageResult();
        internalResult.setError(false);
        internalResult.setSentList(sentList);
        internalResult.setUnsentList(unsentList);
        internalResult.setCount(count);
        result = internalResult;
      } else if (request.getDeviceId() != null) {
        String devId = (validationResult.getDeviceEntity() == null) ? null :
          validationResult.getDeviceEntity().getDeviceId();
        MessageBuilder builder = new MessageBuilder();
        String recipient = validationResult.getDeviceEntity().getOwnerId();
        builder.setAppId(appId)
            .setId(msgId)
            .setUtcTime(System.currentTimeMillis())
            .setSenderId(new MMXid(senderUserId, null))
            .setRecipientId(new MMXid(recipient, devId, null))
            .setReplyTo(request.getReplyTo())
            .setNoAck(true)
            .setMetadata(request.getContent())
            .setDomain(domain)
            .setReceipt(request.isReceipt());
        Message message = builder.build();
        LOGGER.info("Message has been built");
        String messageId = message.getID();
        routeMessage(message);
        requested++;
        sent++;
        count = new Count(requested, sent, 0);
        SendMessageResult internalResult = new SendMessageResult();
        internalResult.setError(false);
        internalResult.setErrorMessage(SEND_MESSAGE_STATUS_OK);
        internalResult.setCount(count);
        sentList.add(new SentMessageId(recipient, request.getDeviceId(), messageId));
        internalResult.setSentList(sentList);
        result = internalResult;
      } else {
        //else use the target definition
        Target target = request.getTarget();
        UserQuery userQuery = target.getUserQuery();
        DeviceQuery deviceQuery = target.getDeviceQuery();
        if (userQuery != null) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Using user query");
          }
          UserTargetResolver resolver = new UserTargetResolver();
          List<UserEntity> userEntityList = resolver.resolve(appId, target);
          requested = userEntityList.size();
          for (UserEntity ue : userEntityList) {
            //get the bared user id.
            String recipient = JIDUtil.getUserId(ue.getUsername());
            MessageBuilder builder = new MessageBuilder();
            builder.setAppId(appId)
                .setId(msgId)
                .setUtcTime(System.currentTimeMillis())
                .setSenderId(new MMXid(senderUserId, null))
                .setRecipientId(new MMXid(recipient, null))
                .setReplyTo(request.getReplyTo())
                .setNoAck(true)
                .setMetadata(request.getContent())
                .setDomain(domain)
                .setReceipt(request.isReceipt());
            Message message = builder.build();
            LOGGER.info("Message has been built for recipient:{}", recipient);
            String messageId = message.getID();
            routeMessage(message);
            sent++;
            sentList.add(new SentMessageId(recipient, null, messageId));
          }
          count = new Count(requested, sent, 0);
          SendMessageResult internalResult = new SendMessageResult();
          internalResult.setError(false);
          internalResult.setErrorMessage(SEND_MESSAGE_STATUS_OK);
          internalResult.setCount(count);
          internalResult.setSentList(sentList);
          result = internalResult;
        } else if (deviceQuery != null) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Using user deviceQuery");
          }
          DeviceTargetResolver resolver = new DeviceTargetResolver();
          List<DeviceEntity> deviceEntityList = resolver.resolve(appId, target);
          requested = deviceEntityList.size();
          for (DeviceEntity de : deviceEntityList) {
            //get the bare user id.
            String recipient = de.getOwnerId();
            MessageBuilder builder = new MessageBuilder();
            builder.setAppId(appId)
                .setId(msgId)
                .setUtcTime(System.currentTimeMillis())
                .setSenderId(new MMXid(senderUserId, null))
                .setRecipientId(new MMXid(recipient, de.getDeviceId(), null))
                .setReplyTo(request.getReplyTo())
                .setNoAck(true)
                .setMetadata(request.getContent())
                .setDomain(domain)
                .setReceipt(request.isReceipt());
            Message message = builder.build();
            LOGGER.info("Message has been built for device id:{}", de.getDeviceId());
            String messageId = message.getID();
            routeMessage(message);
            sent++;
            sentList.add(new SentMessageId(recipient, de.getDeviceId(), messageId));
          }
          //completed sending messages
          count = new Count(requested, sent, 0);
          SendMessageResult internalResult = new SendMessageResult();
          internalResult.setError(false);
          internalResult.setErrorMessage(SEND_MESSAGE_STATUS_OK);
          internalResult.setCount(count);
          internalResult.setSentList(sentList);
          result = internalResult;
        }
      }
      return result;
    } else {
      LOGGER.info("Send message request validation failed");
      result = new SendMessageResult();
      result.setErrorMessage(validationResult.getFailureMessage());
      result.setError(true);
      result.setErrorCode(validationResult.getValidationFailureCode());
    }
    return result;
  }

  /**
   * Post/publish a message to a topic
   *
   * @param request
   * @return SendMessageResult
   */
  @Override
  public TopicPostResult postMessage(String pubUserId, String topicName,
      String appId, TopicPostMessageRequest request) {
    MMXTopicManager topicManager = MMXTopicManager.getInstance();
    ConnectionProvider provider = getConnectionProvider();
    AppDAO appDAO = new AppDAOImpl(provider);
    TopicPostResult result = null;
    ValidationResult validationResult = validateRequest(topicName, appId, request, appDAO, topicManager);
    if (validationResult.isValid()) {
      /**
       * Validate that we can publish to this topic
       */
      AppEntity appEntity = validationResult.getAppEntity();
      Node topicNode = validationResult.getTopicNode();
      String domain = getDomain();
      PublisherModel publisherModel = validationResult.getTopicNode().getPublisherModel();
      JID from = new JID(JIDUtil.makeNode(pubUserId, appEntity.getAppId()), domain, null);

      boolean canPublish = publisherModel.canPublish(topicNode, from);
      if (!canPublish) {
        LOGGER.info("Server user:{} for app with id:{} is not allowed to publish to topic:{}",
            pubUserId, appEntity.getAppId(), topicName);
        result = new TopicPostResult();
        result.setError(true);
        result.setErrorMessage(String.format(ErrorMessages.ERROR_TOPIC_PUBLISHING_NOT_ALLOWED, topicName));
        result.setStatus(SEND_MESSAGE_STATUS_ERROR);
        result.setErrorCode(ErrorCode.TOPIC_PUBLISH_FORBIDDEN.getCode());
        return result;
      }

      // TODO: hack for MOB-2516 that user topic is shown as "userID#topicName"
      MMXTopicId tid = TopicResource.nameToId(topicName);
      String topicId = TopicHelper.makeTopic(appId, tid.getEscUserId(), tid.getName());
      //ok validated.
      TopicMessageBuilder builder = new TopicMessageBuilder();
      MessageIdGeneratorImpl idGen = new MessageIdGeneratorImpl();
      String itemId = idGen.generateItemIdentifier(topicId);
      builder.setPubUserId(pubUserId)
          .setItemId(itemId)
          .setUtcTime(System.currentTimeMillis())
          .setRequest(request)
          .setDomain(domain)
          .setTopicId(topicId)
          .setAppId(appId);
      IQ publishIQ = builder.build();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Publish topic message has been built");
        LOGGER.debug("Topic message:" + publishIQ.toXML());
      }

      routeMessage(publishIQ);
      result = new TopicPostResult();
      result.setError(false);
      result.setMessageId(itemId);
      result.setStatus(SEND_MESSAGE_STATUS_OK);
    } else {
      LOGGER.info("topic post errorMessage validation failed");
      result = new TopicPostResult();
      result.setError(true);
      result.setErrorMessage(validationResult.getFailureMessage());
      result.setStatus(SEND_MESSAGE_STATUS_ERROR);
    }
    return result;
  }


  protected ValidationResult validateRequest(String appId, com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageRequest request, AppDAO appDAO, DeviceDAO deviceDAO) {
    if (request == null) {
      return ValidationResult.failure(ERROR_INVALID_SEND_MESSAGE_REQUEST, FAILURE_CODE_NULL_REQUEST);
    }
    AppEntity appEntity = appDAO.getAppForAppKey(appId);
    if (appEntity == null) {
      ValidationResult result = new ValidationResult();
      result.setValid(false, ERROR_INVALID_APPID);
      return result;
    }

    List<String> userList = request.getRecipientUserIds();
    String deviceId = request.getDeviceId();

    if ((userList == null || userList.isEmpty()) && deviceId == null && request.getTarget() == null) {
      //neither device id nor userid is present.
      return ValidationResult.failure(ErrorMessages.ERROR_SEND_MESSAGE_INVALID_USER_ID_DEVICE_ID,
          ErrorCode.SEND_MESSAGE_NO_TARGET.getCode());
    }
    if (userList != null) {
      int size = userList.size();
      if (size > MAX_USERNAME_COUNT){
        String message = String.format(ErrorMessages.ERROR_USERNAME_LIST_TOO_LONG, MAX_USERNAME_COUNT);
        return ValidationResult.failure(message,
            ErrorCode.SEND_MESSAGE_USERNAME_LIST_TOO_BIG.getCode());
      }
    }

    DeviceEntity deviceEntity = null;
    if (deviceId != null && !deviceId.isEmpty()) {
      deviceEntity = deviceDAO.getDeviceUsingId(appId, deviceId, DeviceStatus.ACTIVE);
      if (deviceEntity == null) {
        return ValidationResult.failure(ERROR_INVALID_SEND_MESSAGE_DEVICE, FAILURE_CODE_BAD_DEVICE_ID);
      }
    }
    // need to ensure that content isn't empty.
    Map<String, String> meta = request.getContent();
    if (meta == null || meta.isEmpty()) {
      return ValidationResult.failure(ERROR_NULL_OR_EMPTY_META, FAILURE_CODE_NO_CONTENT);
    }
    //things are fine here
    ValidationResult result = new ValidationResult();
    result.setValid(true);
    result.setAppEntity(appEntity);
    result.setDeviceEntity(deviceEntity);
    return result;
  }

  protected ValidationResult validateRequest(String topicName, String appId, TopicPostMessageRequest request, AppDAO appDAO, MMXTopicManager topicManager) {
    if (appId == null || appId.isEmpty()) {
      ValidationResult result = new ValidationResult();
      result.setValid(false, ERROR_INVALID_APPID);
      return result;
    }
    AppEntity appEntity = appDAO.getAppForAppKey(appId);
    if (appEntity == null) {
      ValidationResult result = new ValidationResult();
      result.setValid(false, ERROR_INVALID_APPID);
      return result;
    }
    if (topicName == null || topicName.isEmpty()) {
      ValidationResult result = new ValidationResult();
      result.setValid(false, ERROR_INVALID_POST_MESSAGE_TOPIC);
      return result;
    }
    // TODO: hack for MOB-2516 that user topic is shown as "userID#topicName".
    MMXTopicId tid = TopicResource.nameToId(topicName);
    String topicId = TopicHelper.makeTopic(appId, tid.getEscUserId(), tid.getName());
    Node topic = topicManager.getTopicNode(topicId);
    if (topic == null) {
      ValidationResult result = new ValidationResult();
      result.setValid(false, String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND, topicName));
      return result;
    }
    Map<String, String> messageContent =  request.getContent();
    if (messageContent == null || messageContent.isEmpty()) {
      ValidationResult result = new ValidationResult();
      result.setValid(false, String.format(ErrorMessages.ERROR_TOPIC_INVALID_CONTENT, topicName));
      return result;
    }

    ValidationResult result = new ValidationResult();
    result.setValid(true);
    result.setAppEntity(appEntity);
    result.setTopicNode(topic);
    return result;
  }

  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }

  public PacketRouter getPacketRouter() {
    PacketRouter router = XMPPServer.getInstance().getPacketRouter();
    return router;
  }

  protected void routeMessage(final Message message) {
      ExecutorService service = MMXExecutors.getOrCreate(POOL_NAME, POOL_SIZE);
      service.submit(new Runnable() {
        @Override
        public void run() {
          getPacketRouter().route(message);
        }
      });
  }

  protected void routeMessage(IQ message) {
    PacketRouter router = getPacketRouter();
    router.route(message);
  }

  protected String getDomain() {
    return XMPPServer.getInstance().getServerInfo().getXMPPDomain();
  }

  /**
   * Private class for validation result encapsulation
   */
  private static class ValidationResult {

    private int validationFailureCode;
    private boolean valid;
    private AppEntity appEntity;
    private DeviceEntity deviceEntity;
    private String failureMessage;
    private Node topicNode;

    public boolean isValid() {
      return valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    public void setValid(boolean valid, String failureMessage) {
      this.valid = valid;
      this.failureMessage = failureMessage;
    }


    public AppEntity getAppEntity() {
      return appEntity;
    }

    public void setAppEntity(AppEntity appEntity) {
      this.appEntity = appEntity;
    }

    public DeviceEntity getDeviceEntity() {
      return deviceEntity;
    }

    public void setDeviceEntity(DeviceEntity deviceEntity) {
      this.deviceEntity = deviceEntity;
    }

    public String getFailureMessage() {
      return failureMessage;
    }

    public int getValidationFailureCode() {
      return validationFailureCode;
    }

    public void setValidationFailureCode(int validationFailureCode) {
      this.validationFailureCode = validationFailureCode;
    }

    public void setFailureMessage(String failureMessage) {
      this.failureMessage = failureMessage;
    }

    public Node getTopicNode() {
      return topicNode;
    }

    public void setTopicNode(Node topicNode) {
      this.topicNode = topicNode;
    }

    /**
     * Construct a failure validation result.
     *
     * @param failureMessage
     * @param failureCode
     * @return
     */
    public static ValidationResult failure(String failureMessage, int failureCode) {
      ValidationResult result = new ValidationResult();
      result.setValid(false, failureMessage);
      result.setValidationFailureCode(failureCode);
      return result;
    }

  }

}

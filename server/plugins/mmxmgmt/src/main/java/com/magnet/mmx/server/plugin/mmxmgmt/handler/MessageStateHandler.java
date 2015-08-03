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

package com.magnet.mmx.server.plugin.mmxmgmt.handler;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MsgId;
import com.magnet.mmx.protocol.MsgEvents;
import com.magnet.mmx.protocol.MsgTags;
import com.magnet.mmx.protocol.MsgsState;
import com.magnet.mmx.protocol.MsgsState.MessageStatusList;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.util.IQUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.web.MessageSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.MessageSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import com.magnet.mmx.util.GsonData;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for message states and tags.
 */
public class MessageStateHandler extends IQHandler {
  private static Logger LOGGER = LoggerFactory.getLogger(MessageStateHandler.class);
  private static final MsgsState.MessageStatus UNKNOWN_MSG_STATUS = new 
      MsgsState.MessageStatus().setState(Constants.MessageState.UNKNOWN);
  

  /**
   * Constructor that takes the module name
   *
   * @param moduleName
   */
  public MessageStateHandler(String moduleName) {
    super(moduleName);
  }


  @Override
  public IQ handleIQ(IQ iq) throws UnauthorizedException {
    JID fromJID = iq.getFrom();
    String appId = JIDUtil.getAppId(fromJID);
    //read the command
    Element element = iq.getChildElement();
    String payload = element.getText();
    String commandId = element.attributeValue(Constants.MMX_ATTR_COMMAND);

    if (commandId == null || commandId.isEmpty() || commandId.trim().isEmpty()) {
      return IQUtils.createErrorIQ(iq,
          MessageStatusCode.INVALID_COMMAND.getMessage(),
          MessageStatusCode.INVALID_COMMAND.getCode());
    }

    Constants.MessageCommand command = null;
    try {
      command = Constants.MessageCommand.valueOf(commandId);
    } catch (IllegalArgumentException e) {
      LOGGER.info("Invalid device command string:" + commandId, e);
    }
    if (command == null) {
      return IQUtils.createErrorIQ(iq,
          MessageStatusCode.INVALID_COMMAND.getMessage(),
          MessageStatusCode.INVALID_COMMAND.getCode());
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Processing command:" + command.toString());
    }

    switch (command) {
    case query:
      return processMessageQuery(iq, appId, payload);
    case getTags:
      return processGetTags(iq, appId, payload);
    case setTags:
      return processSetTags(iq, appId, payload);
    case addTags:
      return processAddTags(iq, appId, payload);
    case removeTags:
      return processRemoveTags(iq, appId, payload);
    case getEvents:
      return processGetEvents(iq, appId, payload);
    case setEvents:
      return processSetEvents(iq, appId, payload);
    case addEvents:
      return processAddEvents(iq, appId, payload);
    case removeEvents:
      return processRemoveEvents(iq, appId, payload);
    default:
      LOGGER.info("Unknown message command");
      return IQUtils.createErrorIQ(iq,
          MessageStatusCode.INVALID_COMMAND.getMessage(),
          MessageStatusCode.INVALID_COMMAND.getCode());
    }
  }

  @Override
  public IQHandlerInfo getInfo() {
    return new IQHandlerInfo(Constants.MMX, Constants.MMX_NS_MSG_STATE);
  }

  /**
   * Validate the incoming packet
   *
   * @param iq
   */
  private void validate(IQ iq) {
    //TODO: add validation rules
  }

  private IQ processMessageQuery(IQ source, String appId, String payload) {
    MsgsState.Request requestList = MsgsState.Request.fromJson(payload);
    if (requestList.size() == 0) {
      //no ids here
      return IQUtils.createErrorIQ(source,
          MessageStatusCode.INVALID_MSG_ID_LIST.getMessage(),
          MessageStatusCode.INVALID_MSG_ID_LIST.getCode());
    }
    MsgsState.Response response = new MsgsState.Response();
    MessageDAO dao = getMessageDAO();
    // TODO: it would be more efficient if the DAO can get messages with a list
    // of message ID's.
    for (String messageId : requestList) {
      List<MessageEntity> messageList = dao.getMessages(appId, messageId);
      if (messageList.size() == 0) {
        // For non-existing message ID, return UNKNOWN state.
        MessageStatusList list = new MessageStatusList(1);
        list.add(UNKNOWN_MSG_STATUS);
        response.put(messageId, list);
        continue;
      }
      HashMap<String, HashMap<String, MessageEntity.MessageState>> result = new
          HashMap<String, HashMap<String, MessageEntity.MessageState>>();
      for (MessageEntity me : messageList) {
        HashMap<String, MessageEntity.MessageState> userMsg = result.get(messageId);
        if (userMsg == null) {
          userMsg = new HashMap<String, MessageEntity.MessageState>();
          result.put(messageId, userMsg);
        }
        String userId = JIDUtil.getUserId(me.getTo());
        MessageEntity.MessageState state = userMsg.get(userId);
        if (state == null) {
          state = me.getState();
          userMsg.put(userId, state);
        } else {
          if (state.getPriority() < me.getState().getPriority()) {
            // Do the state aggregation; use the priority of a state.
            // If multiple states having same priority is supported, it should 
            // consider device priority and the modified time.
            userMsg.put(userId, me.getState());
          }
        }
      }

      // Convert the aggregated result to the final response.
      // TODO: consolidate the MessageEntity.MessageState with Constants.MessageState
      for (Map.Entry<String, HashMap<String, MessageEntity.MessageState>> entry : result.entrySet()) {
        MessageStatusList list = new MessageStatusList(entry.getValue().size());
        for (Map.Entry<String, MessageEntity.MessageState> stateEntry : entry.getValue().entrySet()) {
          list.add(new MsgsState.MessageStatus()
            .setRecipient(stateEntry.getKey())
            .setState(Constants.MessageState.valueOf(stateEntry.getValue().toString())));
        }
        response.put(entry.getKey(), list);
      }
    }
    
    return IQUtils.createResultIQ(source, response.toJson());
  }

  private IQ processGetTags(IQ source, String appId, String payload) {
    MsgId rqt = GsonData.getGson().fromJson(payload, MsgId.class);
    if (rqt == null || rqt.getMsgId() == null) {
      return IQUtils.createErrorIQ(source,
          MessageStatusCode.INVALID_MSG_ID.getMessage()+"null",
          MessageStatusCode.INVALID_MSG_ID.getCode());
    }
    MessageDAO dao = getMessageDAO();
    
    // TODO: verify the msg ID (normal or push message) and get the tags.
    List<String> tags = new ArrayList<String>();
    MsgTags response = new MsgTags(rqt.getIdType(), rqt.getMsgId(),
        tags, new Date());
    
    return IQUtils.createResultIQ(source, response.toJson());
  }
  
  private IQ processSetTags(IQ source, String appId, String payload) {
    MsgTags msgTags = MsgTags.fromJson(payload);
    MsgId.IdType idType = msgTags.getIdType();
    String msgId = msgTags.getMsgId();
    if (msgId == null) {
      return IQUtils.createErrorIQ(source,
          MessageStatusCode.INVALID_MSG_ID.getMessage()+msgId,
          MessageStatusCode.INVALID_MSG_ID.getCode());
    }
    MessageDAO dao = getMessageDAO();
    // TODO: verify the msg ID (normal or push message) and set the tags.
    
    return IQUtils.createErrorIQ(source, "Update tags is not implemented",
        StatusCode.NOT_IMPLEMENTED);
    
//    MMXStatus status = new MMXStatus()
//        .setCode(MessageStatusCode.SUCCESS.getCode())
//        .setMessage(MessageStatusCode.SUCCESS.getMessage());
//    return IQUtils.createResultIQ(source, status.toJson());
  }

  private IQ processAddTags(IQ source, String appId, String payload) {
    MsgTags msgTags = MsgTags.fromJson(payload);
    MsgId.IdType idType = msgTags.getIdType();
    String msgId = msgTags.getMsgId();
    if (msgId == null) {
      return IQUtils.createErrorIQ(source,
          MessageStatusCode.INVALID_MSG_ID.getMessage()+msgId,
          MessageStatusCode.INVALID_MSG_ID.getCode());
    }
    MessageDAO dao = getMessageDAO();
    // TODO: verify the msg ID (normal or push message) and add the tags.
    
    return IQUtils.createErrorIQ(source, "Adding tags is not implemented",
        StatusCode.NOT_IMPLEMENTED);
    
//    MMXStatus status = new MMXStatus()
//        .setCode(MessageStatusCode.SUCCESS.getCode())
//        .setMessage(MessageStatusCode.SUCCESS.getMessage());
//    return IQUtils.createResultIQ(source, status.toJson());
  }
  
  private IQ processRemoveTags(IQ source, String appId, String payload) {
    MsgTags msgTags = MsgTags.fromJson(payload);
    MsgId.IdType idType = msgTags.getIdType();
    String msgId = msgTags.getMsgId();
    if (msgId == null) {
      return IQUtils.createErrorIQ(source,
          MessageStatusCode.INVALID_MSG_ID.getMessage()+msgId,
          MessageStatusCode.INVALID_MSG_ID.getCode());
    }
    MessageDAO dao = getMessageDAO();
    // TODO: verify the msg ID (normal or push message) and remove the tags.
    
    return IQUtils.createErrorIQ(source, "Tags removal is not implemented",
        StatusCode.NOT_IMPLEMENTED);
    
//    MMXStatus status = new MMXStatus()
//        .setCode(MessageStatusCode.SUCCESS.getCode())
//        .setMessage(MessageStatusCode.SUCCESS.getMessage());
//    return IQUtils.createResultIQ(source, status.toJson());
  }
  
  private IQ processGetEvents(IQ source, String appId, String payload) {
    MsgId rqt = GsonData.getGson().fromJson(payload, MsgId.class);
    if (rqt == null || rqt.getMsgId() == null) {
      return IQUtils.createErrorIQ(source,
          MessageStatusCode.INVALID_MSG_ID.getMessage()+"null",
          MessageStatusCode.INVALID_MSG_ID.getCode());
    }
    MessageDAO dao = getMessageDAO();
    
    // TODO: verify the msg ID (normal or push message) and get the events.
    List<String> tags = new ArrayList<String>();
    MsgEvents response = new MsgEvents(rqt.getIdType(), rqt.getMsgId(),
        tags, new Date());
    
    return IQUtils.createResultIQ(source, response.toJson());
  }
  
  private IQ processSetEvents(IQ source, String appId, String payload) {
    MsgEvents msgEvents = MsgEvents.fromJson(payload);
    MsgId.IdType idType = msgEvents.getIdType();
    String msgId = msgEvents.getMsgId();
    if (msgId == null) {
      return IQUtils.createErrorIQ(source,
          MessageStatusCode.INVALID_MSG_ID.getMessage()+msgId,
          MessageStatusCode.INVALID_MSG_ID.getCode());
    }
    MessageDAO dao = getMessageDAO();
    // TODO: verify the msg ID (normal or push message) and set the events.
    
    return IQUtils.createErrorIQ(source, "Update tags is not implemented",
        StatusCode.NOT_IMPLEMENTED);
    
//    MMXStatus status = new MMXStatus()
//        .setCode(MessageStatusCode.SUCCESS.getCode())
//        .setMessage(MessageStatusCode.SUCCESS.getMessage());
//    return IQUtils.createResultIQ(source, status.toJson());
  }

  private IQ processAddEvents(IQ source, String appId, String payload) {
    MsgEvents msgEvents = MsgEvents.fromJson(payload);
    MsgId.IdType idType = msgEvents.getIdType();
    String msgId = msgEvents.getMsgId();
    if (msgId == null) {
      return IQUtils.createErrorIQ(source,
          MessageStatusCode.INVALID_MSG_ID.getMessage()+msgId,
          MessageStatusCode.INVALID_MSG_ID.getCode());
    }
    MessageDAO dao = getMessageDAO();
    // TODO: verify the msg ID (normal or push message) and add the events.
    
    return IQUtils.createErrorIQ(source, "Adding tags is not implemented",
        StatusCode.NOT_IMPLEMENTED);
    
//    MMXStatus status = new MMXStatus()
//        .setCode(MessageStatusCode.SUCCESS.getCode())
//        .setMessage(MessageStatusCode.SUCCESS.getMessage());
//    return IQUtils.createResultIQ(source, status.toJson());
  }
  
  private IQ processRemoveEvents(IQ source, String appId, String payload) {
    MsgEvents msgEvents = MsgEvents.fromJson(payload);
    MsgId.IdType idType = msgEvents.getIdType();
    String msgId = msgEvents.getMsgId();
    if (msgId == null) {
      return IQUtils.createErrorIQ(source,
          MessageStatusCode.INVALID_MSG_ID.getMessage()+msgId,
          MessageStatusCode.INVALID_MSG_ID.getCode());
    }
    MessageDAO dao = getMessageDAO();
    // TODO: verify the msg ID (normal or push message) and remove the events.
    
    return IQUtils.createErrorIQ(source, "Tags removal is not implemented",
        StatusCode.NOT_IMPLEMENTED);
    
//    MMXStatus status = new MMXStatus()
//        .setCode(MessageStatusCode.SUCCESS.getCode())
//        .setMessage(MessageStatusCode.SUCCESS.getMessage());
//    return IQUtils.createResultIQ(source, status.toJson());
  }
  
  public MessageDAO getMessageDAO() {
    MessageDAO dao = new MessageDAOImpl(new OpenFireDBConnectionProvider());
    return dao;
  }

  /**
   * Enum for the status codes
   */
  public static enum MessageStatusCode {
    SUCCESS(200, "SUCCESS"),
    INVALID_COMMAND(400, "Invalid command"),
    INVALID_MSG_ID(400, "Invalid message id: "),
    INVALID_MSG_ID_LIST(400, "Invalid message id list"),;

    private int code;
    private String message;

    MessageStatusCode(int c, String m) {
      code = c;
      message = m;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }
  }
}

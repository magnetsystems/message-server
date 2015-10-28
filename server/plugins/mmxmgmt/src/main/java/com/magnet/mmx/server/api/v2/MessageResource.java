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
package com.magnet.mmx.server.api.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.api.v2.UserResource.User;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSenderImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.message.SendMessageResult;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;

/**
 * Messages functionality using auth token.
 * 1. /messages/{message-id}  (get the state of a message sent by current user)
 * 2. /messages/send_to_user_ids (XMPP message)
 * 3. /messages/send_to_user_names (XMPP message)
 */
@Path("/messages")
public class MessageResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageResource.class);

  /**
   * Get the status of a message identified by the message ID.  The message must
   * be sent by the current user; otherwise, a status code FORBIDDEN will be
   * returned.
   * @param headers
   * @param messageId
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}")
  public Response getMessageById(@Context HttpHeaders headers, @PathParam("id") String messageId) {
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    LOGGER.trace("getUserTags : username={}", tokenInfo.getUserName());

    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    try {
      long startTime = System.nanoTime();
      MessageDAO messageDAO = new MessageDAOImpl(new OpenFireDBConnectionProvider());
      List<MessageEntity> messageEntityList = messageDAO.getMessages(appId, messageId);
      
      if (messageEntityList.size() > 0) {
        MessageEntity me = messageEntityList.get(0);
        JID jid = new JID(me.getFrom());
        if (!jid.getNode().equalsIgnoreCase(from.getNode())) {
          ErrorResponse response = new ErrorResponse(ErrorCode.MESSAGE_SENDER_NOT_MATCHED, "User is not the sender");
          return RestUtils.getJAXRSResp(Response.Status.FORBIDDEN, response);
        }
      }
      List<SentMessage> sentMessageList = new ArrayList<SentMessage>(messageEntityList.size());
      for (MessageEntity me : messageEntityList) {
        sentMessageList.add(SentMessage.from(me));
      }
      
      long endTime = System.nanoTime();
      LOGGER.info("Completed processing getMessageById in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return RestUtils.getOKJAXRSResp(sentMessageList);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.warn("Throwable during getMessageById", t);
      ErrorResponse errorResponse = new ErrorResponse(ErrorCode.GET_MESSAGE_BY_ID_ISE, t.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
  }

  /**
   * Send a message to a list of user ID's.
   * @param headers
   * @param request
   * @return
   */
  @POST
  @Path("/send_to_user_ids")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response sendMessageToUserIds(@Context HttpHeaders headers, SendMessageRequest request) {
    try {
      long startTime = System.nanoTime();

      TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
      if (tokenInfo == null) {
        return RestUtils.getUnauthJAXRSResp();
      }
      
      MessageSender sender = new MessageSenderImpl();
      String appId = tokenInfo.getMmxAppId();
      SendMessageResult result = sender.send(appId, request);
      Response rv = null;
      if (result.isError()) {
        ErrorResponse response = new ErrorResponse(result.getErrorCode(), result.getErrorMessage());
        rv = RestUtils.getBadReqJAXRSResp(response);
      } else {
        SendMessageResponse response = new SendMessageResponse();
        response.setCount(result.getCount());
        response.setSentList(result.getSentList());
        response.setUnsentList(result.getUnsentList());
        rv = RestUtils.getOKJAXRSResp(response);
      }
      long endTime = System.nanoTime();
      LOGGER.info("Completed processing sendMessage in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return rv;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.warn("Throwable during send message", t);
      ErrorResponse response = new ErrorResponse(ErrorCode.SEND_MESSAGE_ISE, t.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(response);
    }
  }
  
  /**
   * Send a message to a list of user names.
   * @param headers
   * @param request
   * @return
   */
  @POST
  @Path("/send_to_user_names")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response sendMessageToUserNames(@Context HttpHeaders headers, SendMessageRequest request) {
    String authToken = RestUtils.getAuthToken(headers);
    List<String> ids = userNamesToIds(authToken, request.getRecipientUsernames());
    request.setRecipientUsernames(ids);
    return sendMessageToUserIds(headers, request);
  }
  
  private List<String> userNamesToIds(String authToken, List<String> names) {
    List<String> userIds = new ArrayList<String>(names.size());
    HashMap<String, String[]> reqt = new HashMap<String, String[]>(1);
    String[] array = new String[names.size()];
    reqt.put("userNames", names.toArray(array));
    try {
      User[] users = RestUtils.doMAXGet(authToken, "/user/users", reqt, User[].class);
      for (User user : users) {
        userIds.add(user.getUserIdentifier());
      }
      return userIds;
    } catch (IOException e) {
      return null;
    }
  }
  
  /**
   * Simplified representation of MessageEntity
   */
  protected static class SentMessage {
    private String state;
    private String recipient;
    private String sender;
    private String appId;
    private String deviceId;
    private String messageId;
    private Date queuedAt;
    private Date receivedAt;

    public String getState() {
      return state;
    }

    public String getRecipient() {
      return recipient;
    }

    public String getAppId() {
      return appId;
    }

    public String getSender() {
      return sender;
    }

    public String getDeviceId() {
      return deviceId;
    }

    public Date getQueuedAt() {
      return queuedAt;
    }

    public String getMessageId() {
      return messageId;
    }

    public Date getReceivedAt() {
      return receivedAt;
    }

    public static SentMessage from(MessageEntity me) {
      SentMessage message = new SentMessage();
      message.messageId = me.getMessageId();
      message.appId = me.getAppId();
      message.deviceId = me.getDeviceId();
      message.receivedAt = me.getDeliveryAckAt();
      message.queuedAt = me.getQueuedAt();
      message.sender = JIDUtil.getReadableUserId(me.getFrom());
      message.recipient = JIDUtil.getReadableUserId(me.getTo());
      message.state = me.getState().name();
      return message;
    }
  }
}

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
package com.magnet.mmx.server.plugin.mmxmgmt.api.message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.AbstractBaseResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;

/**
 * V1 Admin REST API to get message status.  It is used by Console only.
 */
@Path("/message")
public class AdminMessageResource extends AbstractBaseResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdminMessageResource.class);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}")
  public Response getMessageById(@Context HttpHeaders headers, @PathParam("id") String messageId) {
    try {
      long startTime = System.nanoTime();
      AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
      AppEntityHolder holder = new AppEntityHolderImpl();

      ErrorResponse authCheck = isAuthenticated(headers, appDAO, holder);
      if (authCheck != null) {
        return Response
            .status(Response.Status.UNAUTHORIZED)
            .entity(authCheck)
            .build();
      }
      AppEntity appEntity = holder.getAppEntity();
      String appId = appEntity.getAppId();
      MessageDAO messageDAO = new MessageDAOImpl(getConnectionProvider());
      List<MessageEntity> messageEntityList = messageDAO.getMessages(appId, messageId);

      List<SentMessage> sentMessageList = new ArrayList<SentMessage>(messageEntityList.size());
      for (MessageEntity me : messageEntityList) {
        sentMessageList.add(SentMessage.from(me));
      }

      Response response = Response
          .status(Response.Status.OK)
          .entity(sentMessageList)
          .build();

      long endTime = System.nanoTime();
      LOGGER.info("Completed processing getMessageById in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return response;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.warn("Throwable during getMessageById", t);
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse(ErrorCode.GET_MESSAGE_BY_ID_ISE, t.getMessage()))
              .build()
      );
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

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
package com.magnet.mmx.server.plugin.mmxmgmt.api;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSenderImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.message.SendMessageResult;

/**
 * Resource that provides API related to message functions via:
 * 1. /messages/send_to_user_ids (XMPP message)
 * 2. /messages/send_to_user_names (XMPP message)
 * This API uses the auth token with Blowfish server.
 */
@Path("/messages")
public class MessageSendResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageSendResource.class);

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
//  
//  @POST
//  @Path("/send_to_user_names")
//  @Consumes(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_JSON)
//  public Response sendMessageToUserNames(@Context HttpHeaders headers, SendMessageRequest request) {
//    List<String> ids = userNamesToIds(request.getRecipientUsernames());
//    return sendMessageToUserIds(headers, request);
//  }
//  
//  private List<String> userNamesToIds(List<String> names) {
//    // TODO: not implemented
//    return null;
//  }
}

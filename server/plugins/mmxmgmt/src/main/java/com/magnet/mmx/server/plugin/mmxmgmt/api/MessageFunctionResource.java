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

import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSenderImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.message.SendMessageResult;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

/**
 * Resource that provides API related to message functions viz:
 * 1. send_message (XMPP message)
 */
@Path("/send_message")
public class MessageFunctionResource extends AbstractBaseResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageFunctionResource.class);


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response sendMessage(@Context HttpHeaders headers, SendMessageRequest request) {
    try {
      long startTime = System.nanoTime();
      MessageSender sender = new MessageSenderImpl();
      AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
      ErrorResponse authCheck = isAuthenticated(headers, appDAO);
      if (authCheck != null) {
        return Response
            .status(Response.Status.UNAUTHORIZED)
            .entity(authCheck)
            .build();
      }
      MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
      String appId = requestHeaders.getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);
      SendMessageResult result = sender.send(appId, request);
      Response rv = null;
      if (result.isError()) {
        rv = Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(result.getErrorCode(), result.getErrorMessage()))
            .build();
      } else {
        SendMessageResponse response = new SendMessageResponse();
        response.setCount(result.getCount());
        response.setSentList(result.getSentList());
        response.setUnsentList(result.getUnsentList());
        rv = Response
            .status(Response.Status.OK)
            .entity(response)
            .build();
      }
      long endTime = System.nanoTime();
      LOGGER.info("Completed processing sendMessage in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return rv;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.warn("Throwable during send message", t);
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse(ErrorCode.SEND_MESSAGE_ISE, t.getMessage()))
              .build()
      );
    }
  }

}

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
package com.magnet.mmx.server.plugin.mmxmgmt.api.push;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.AbstractBaseResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSPushMessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceTargetResolver;
import com.magnet.mmx.server.plugin.mmxmgmt.gcm.GCMPushMessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.push.DeviceHolder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushAPNSPayloadBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushGCMPayloadBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushHeader;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Resource that provides API for sending push messages.
 */
@Path("/send_push")
public class PushMessageFunctionResource extends AbstractBaseResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(PushMessageFunctionResource.class);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response sendPushMessage(@Context HttpHeaders headers, SendPushMessageRequest request) {
    try {
      long startTime = System.nanoTime();
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

      DeviceTargetResolver resolver = new DeviceTargetResolver();
      List<DeviceEntity> deviceEntityList = resolver.resolve(appId, request.getTarget());
      DeviceHolder holder = DeviceHolder.build(deviceEntityList);

      List<DeviceEntity> iosDevices = holder.getDevices(PushType.APNS);
      int iosCount = iosDevices.size();
      List<DeviceEntity> androidDevices = holder.getDevices(PushType.GCM);
      int androidCount = androidDevices.size();

      SendPushMessageResponse response = new SendPushMessageResponse();

      if (iosCount == 0 && androidCount == 0) {
        //nothing found.
        //check if deviceIds were specified
        boolean hasDeviceIds = request.getTarget() != null && request.getTarget().getDeviceIds() != null && !request.getTarget().getDeviceIds().isEmpty();
        int requested = 0;
        if (hasDeviceIds) {
          requested = request.getTarget().getDeviceIds().size();
        }
        Count count = new Count();
        count.setRequested(requested);
        count.setSent(0);
        List<PushIdTuple> sent = Collections.emptyList();
        List<Unsent> unsent;
        if (hasDeviceIds) {
          unsent = new LinkedList<Unsent>();
          for (String deviceId : request.getTarget().getDeviceIds()) {
            Unsent devUnsent = new Unsent(deviceId, ErrorCode.INVALID_DEVICE_ID.getCode(), ErrorMessages.ERROR_INVALID_DEVICE_ID);
            unsent.add(devUnsent);
          }
        } else {
          unsent = Collections.emptyList();
        }
        count.setUnsent(unsent.size());
        response.setCount(count);
        response.setSentList(sent);
        response.setUnsentList(unsent);
      } else {
        AppEntity appEntity = appDAO.getAppForAppKey(appId);
        int sent = 0;
        int unsent = 0;
        int requested = 0;
        if (iosCount > 0) {
          APNSPushMessageSender sender = new APNSPushMessageSender(appEntity, request.getOptions());
          MMXPushAPNSPayloadBuilder builder = builder(request);
          PushResult result = sender.sendPush(iosDevices, builder);
          Count iCount = result.getCount();
          sent = sent + iCount.getSent();
          requested = requested + iCount.getRequested();
          unsent = unsent + iCount.getUnsent();
          response.addSentList(result.getSentList());
          response.addUnsentList(result.getUnsentList());
        }
        if (androidCount > 0) {
          GCMPushMessageSender sender = new GCMPushMessageSender(appEntity, request.getOptions());
          //build the payload
          MMXPushGCMPayloadBuilder builder = gcmBuilder(request);
          PushResult result = sender.sendPush(androidDevices, builder);
          Count aCount = result.getCount();
          sent = sent + aCount.getSent();
          requested = requested + aCount.getRequested();
          unsent = unsent + aCount.getUnsent();
          response.addSentList(result.getSentList());
          response.addUnsentList(result.getUnsentList());
        }
        response.setCount(requested, sent, unsent);
      }
      Response rv = Response
          .status(Response.Status.OK)
          .entity(response)
          .build();
      long endTime = System.nanoTime();
      LOGGER.info("Completed processing sendPushMessage in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return rv;
    } catch (ResolutionException e) {
      throw new WebApplicationException(
          Response
              .status(Response.Status.BAD_REQUEST)
              .entity(new ErrorResponse(ErrorCode.SEND_PUSH_MESSAGE_BAD_REQUEST, ErrorMessages.ERROR_SEND_PUSH_INVALID_TARGET))
              .build());
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.warn("Throwable during sendPushMessage", t);
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse(ErrorCode.SEND_PUSH_MESSAGE_ISE, t.getMessage()))
              .build()
      );
    }
  }


  protected MMXPushAPNSPayloadBuilder builder(SendPushMessageRequest request) {
    MMXPushAPNSPayloadBuilder builder = new MMXPushAPNSPayloadBuilder();
    builder.setBody(request.getBody())
        .setTitle(request.getTitle());

    if (request.getIos() != null) {
      builder.setBadge(request.getIos().getBadge());
      if (request.getIos().isSilent()) {
        builder.silent();
      }
      builder.setCategory(request.getIos().getCategory());
    }
    builder.setCustomDictionary(request.getCustom())
        .setSound(request.getSound())
        .setType(new MMXPushHeader(Constants.MMX, Constants.MMX_ACTION_CODE_PUSH));
    return builder;
  }

  protected MMXPushGCMPayloadBuilder gcmBuilder(SendPushMessageRequest request) {
    MMXPushGCMPayloadBuilder builder = new MMXPushGCMPayloadBuilder();
    builder.setBody(request.getBody())
        .setTitle(request.getTitle());

    if (request.getAndroid() != null) {
      builder.setIcon(request.getAndroid().getIcon());
    }
    builder.setCustomDictionary(request.getCustom())
        .setSound(request.getSound())
        .setType(new MMXPushHeader(Constants.MMX, Constants.MMX_ACTION_CODE_PUSH));
    return builder;
  }
}

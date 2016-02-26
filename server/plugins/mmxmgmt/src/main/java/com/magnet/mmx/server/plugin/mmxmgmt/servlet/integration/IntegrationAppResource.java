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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;/*   Copyright (c) 2015 Magnet Systems, Inc.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.magnet.mmx.protocol.AppCreate;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.bot.BotStarter;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppAlreadyExistsException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppManagementException;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXAppManager;
import com.magnet.mmx.server.plugin.mmxmgmt.monitoring.MaxAppLimitExceededException;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.AppInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.JSONFriendlyAppEntityDecorator;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;

/**
 * An integrated App Resource.
 *
 * TODO: it is missing the API to specify the APNS certificate in app creation,
 * get and update.
 *
 */
@Path("/integration/apps")
public class IntegrationAppResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationAppResource.class);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createApp(AppInfo appInfo) {
    LOGGER.trace("createApp : appInfo={}", appInfo);
    return createMmxApp(appInfo);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateApp(AppInfo appInfo) {
    try {
      AppDAO dao = DBUtil.getAppDAO();
      AppEntity appEntity = dao.getAppForAppKey(appInfo.getAppId());
      if(appEntity == null) {
        LOGGER.trace("updateApp : app does not exist so creating app for appInfo={}", appInfo);
        return createMmxApp(appInfo);
      } else {
        dao.updateApp(appInfo.getAppId(), appInfo.getName(), appInfo.getGoogleApiKey(),appInfo.getGoogleProjectId(),
            appInfo.getApnsCertPassword(), appInfo.getOwnerEmail(), appInfo.getGuestSecret(), appInfo.isApnsCertProduction(),
            appInfo.getServerUserId());
        AppEntity revised = dao.getAppForAppKey(appInfo.getAppId());
        return Response.status(Response.Status.CREATED)
            .entity(new JSONFriendlyAppEntityDecorator(revised))
            .build();
      }
    } catch (Exception e) {
      LOGGER.error("updateApp : {}", e);
      return RestUtils.getBadReqJAXRSResp(new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Bad attribute"));
    }
  }

  private Response createMmxApp(AppInfo appInfo) {
    try {
      LOGGER.trace("createMmxApp : creating app={}", appInfo);
      MMXAppManager mmxAppManager = MMXAppManager.getInstance();
      AppCreate.Response response = mmxAppManager.createApp(appInfo.getName(), appInfo.getServerUserId(), null,
          appInfo.getGuestSecret(), appInfo.getGoogleApiKey(), appInfo.getGoogleProjectId(),
          appInfo.getApnsCertPassword(), appInfo.getOwnerId(), appInfo.getOwnerEmail(),
          appInfo.isApnsCertProduction());

      AppDAO appDAO = DBUtil.getAppDAO();
      AppEntity appEntity = appDAO.getAppForAppKey(response.getAppId());
      if(appEntity == null) {
        return RestUtils.getInternalErrorJAXRSResp(new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "Unable to create app"));
      }

      ///// BOT support
      String appName = appInfo.getName();
      String appId =  appEntity.getAppId();//appInfo.getAppId();
      if (BotStarter.isBotEnabled(appName)) {
        Future<Boolean> resultFuture = BotStarter.startApplicableBots(appName, appId, Executors.newSingleThreadExecutor());
        try {
          if (resultFuture != null && resultFuture.get()) {
            LOGGER.debug("Created/Started bots for appId:{} and name:{}", appId, appName);
          } else {
            LOGGER.warn("Failed to create bots for appId:{} and name:{}", appId, appName);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }

      return Response.status(Response.Status.CREATED)
          .entity(new JSONFriendlyAppEntityDecorator(appEntity))
          .build();
    } catch (AppAlreadyExistsException e) {
      LOGGER.error("createApp : caught exception {}", e);
      return RestUtils.getBadReqJAXRSResp(new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
          "app already exists"));
    } catch (AppManagementException e) {
      LOGGER.error("createMmxApp : caught exception {}", e);
      return RestUtils.getBadReqJAXRSResp(new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Bad attribute"));
    } catch (MaxAppLimitExceededException e) {
      LOGGER.error("createMmxApp : Caught max app limit reached exception {}", e);
      return RestUtils.getBadReqJAXRSResp(new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
          "Maximum apps limit reached"));
    }
  }

  @GET
  @Path("{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApp(@PathParam("appId") String appId) {
    try {
      AppDAO dao = DBUtil.getAppDAO();
      AppEntity entity = dao.getAppForAppKey(appId);
      if(entity == null) {
        return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, new ErrorResponse(ErrorCode.AUTH_BAD_APP_ID, "App not found"));
      }
      // TODO: shouldn't the status code be OK instead of CREATED?
      Response createdResponse = Response.status(Response.Status.CREATED)
          .entity(new JSONFriendlyAppEntityDecorator((entity)))
          .build();
      return createdResponse;
    } catch (Exception e) {
      return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, new ErrorResponse(ErrorCode.AUTH_BAD_APP_ID, "App not found"));
    }
  }

  @DELETE
  @Path("{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteApp(@PathParam("appId") String appId) {
    try {
      AppDAO dao = DBUtil.getAppDAO();
      AppEntity entity = dao.getAppForAppKey(appId);

      MMXAppManager manager = MMXAppManager.getInstance();
      manager.deleteAppQuietly(appId); // change to a quite delete i.e do not complain if related serveruser is not found.
      return RestUtils.getOKJAXRSResp();
    } catch (Exception e) {
      return RestUtils.getBadReqJAXRSResp(new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "Unknow internal error"));
    }
  }
}

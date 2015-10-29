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

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.DeviceTagInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.TagList;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceNotFoundException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TagDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.Utils;

/**
 * REST API v2 for device tags using auth token.  It deprecates
 * MMXDeviceTagsResource.
 * TODO: this file should become the new DevicesResource.
 */

@Path("devices/{" + MMXServerConstants.DEVICEID_PATH_PARAM + "}/tags")
public class DeviceTagsResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeviceTagsResource.class);

  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response getDeviceTags(@Context HttpHeaders headers,
                                @PathParam(MMXServerConstants.DEVICEID_PATH_PARAM) String deviceId) {
    ErrorResponse errorResponse;

    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }

    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    try {
      DeviceEntity entity = DBUtil.getDeviceDAO().getDevice(appId, deviceId);
      if(entity != null) {
        List<String> tagnames = DBUtil.getTagDAO().getTagsForDevice(entity);
        DeviceTagInfo tagInfo = new DeviceTagInfo(deviceId, tagnames);
        return RestUtils.getOKJAXRSResp(tagInfo);
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Device not found");
        return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
      }
    } catch (DeviceNotFoundException e) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "DeviceId not found");
      return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
    } catch (Exception e) {
      LOGGER.error("getDeviceTags : caught exception getting tags for device = {}", deviceId);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Invalid deviceId");
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addDeviceTags(@Context HttpHeaders headers,
                                @PathParam(MMXServerConstants.DEVICEID_PATH_PARAM) String deviceId,
                                TagList tagList) {
    ErrorResponse errorResponse;

    LOGGER.trace("addDeviceTags : tagList={}", tagList);

    if(tagList == null || Utils.isNullOrEmpty(tagList.getTags())) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Tag list is not set");
      return RestUtils.getBadReqJAXRSResp(errorResponse);
    }

    if(!Helper.validateTags(tagList.getTags())) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
          "Invalid tag : tag cannot be empty and can have a max length of : " +
              MMXServerConstants.MAX_TAG_LENGTH);
      return RestUtils.getBadReqJAXRSResp(errorResponse);
    }

    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }

    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    try {
      DeviceEntity deviceEntity = DBUtil.getDeviceDAO().getDevice(appId, deviceId);
      if (deviceEntity != null) {
        TagDAO tagDao = DBUtil.getTagDAO();
        for(String tag : tagList.getTags()) {
          tagDao.createDeviceTag(tag, appId, deviceEntity.getId());
        }
        return Response
                .status(Response.Status.CREATED)
                .type(MediaType.TEXT_PLAIN)
                .entity("Successfully Created Tags")
                .build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "DeviceId not found");
        return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
      }
    } catch (DeviceNotFoundException e) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "DeviceId not found");
      return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
    } catch (Exception e) {
      LOGGER.error("addDeviceTags : caught exception creating tags for deviceId={}", deviceId);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error creating tags");
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
  }

  @DELETE
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response deleteAllTags(@Context HttpHeaders headers,
                                @PathParam(MMXServerConstants.DEVICEID_PATH_PARAM) String deviceId) {
    ErrorResponse errorResponse;
    
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }

    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    LOGGER.trace("deleteAllTags : deviceId={}", deviceId);

    try {
      DeviceEntity deviceEntity = DBUtil.getDeviceDAO().getDevice(appId, deviceId);
      if(deviceEntity != null) {
        DBUtil.getTagDAO().deleteAllTagsForDevice(deviceEntity);
        return Response.status(Response.Status.OK)
                        .type(MediaType.TEXT_PLAIN).entity("Deleted all tags").build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Device not found");
        return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
      }
    } catch (DeviceNotFoundException e) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "DeviceId not found");
      return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
    } catch (Exception e) {
      LOGGER.error("deleteAllTags : caught exception deleting tags for device={}", deviceId);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error deleting tags for device");
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
  }

  @DELETE
  @Path("{" + MMXServerConstants.TAGNAME_PATH_PARAM + "}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response deleteTag(@Context HttpHeaders headers,
                            @PathParam(MMXServerConstants.DEVICEID_PATH_PARAM) String deviceId,
                            @PathParam(MMXServerConstants.TAGNAME_PATH_PARAM) String tag) {
    ErrorResponse errorResponse;
    
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();
    
    LOGGER.trace("deleteTag : deviceId={}, tag={}", deviceId, tag);

    try {
      DeviceEntity deviceEntity = DBUtil.getDeviceDAO().getDevice(appId, deviceId);
      if(deviceEntity != null) {
        DBUtil.getTagDAO().deleteTagsForDevice(deviceEntity, Arrays.asList(tag));
        return Response.status(Response.Status.OK)
            .type(MediaType.TEXT_PLAIN)
            .entity("Deleted tag").build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Device not found");
        return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
      }
    } catch (DeviceNotFoundException e) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "DeviceId not found");
      return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
    } catch (Exception e) {
      LOGGER.error("deleteTag : caught exception deleting tags for deviceId={}", deviceId, e);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error deleting tags for device");
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
  }
}

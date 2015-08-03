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
package com.magnet.mmx.server.plugin.mmxmgmt.api.tags;

import com.magnet.mmx.server.plugin.mmxmgmt.api.AbstractBaseResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceNotFoundException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TagDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 */

@Path("/devices/{"+MMXServerConstants.DEVICEID_PATH_PARAM+"}/tags")
public class MMXDeviceTagsResource extends AbstractBaseResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXDeviceTagsResource.class);

  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response getDeviceTags(@Context HttpHeaders headers,
                                @PathParam(MMXServerConstants.DEVICEID_PATH_PARAM) String deviceId) {
    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    try {
      DeviceEntity entity = DBUtil.getDeviceDAO().getDevice(appId, deviceId);
      if(entity != null) {
        List<String> tagnames = DBUtil.getTagDAO().getTagsForDevice(entity);
        DeviceTagInfo tagInfo = new DeviceTagInfo(deviceId, tagnames);
        return Response.status(Response.Status.OK).entity(tagInfo).build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Device not found");
        return Response
                .status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
      }
    } catch (DeviceNotFoundException e) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "DeviceId not found");
      return Response
              .status(Response.Status.NOT_FOUND)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    } catch (Exception e) {
      LOGGER.error("getDeviceTags : caught exception getting tags for device = {}", deviceId);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Invalid deviceId");
      return Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addDeviceTags(@Context HttpHeaders headers,
                                @PathParam(MMXServerConstants.DEVICEID_PATH_PARAM) String deviceId,
                                TagList tagList) {

    LOGGER.trace("addDeviceTags : tagList={}", tagList);

    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    if(tagList == null || Utils.isNullOrEmpty(tagList.getTags())) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Tag list is not set");
      return Response
              .status(Response.Status.BAD_REQUEST)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }

    if(!Helper.validateTags(tagList.getTags())) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Invalid tag : tag cannot be empty and can have a max length of : " + MMXServerConstants.MAX_TAG_LENGTH);
      return Response.status(Response.Status.BAD_REQUEST)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }

    TagDAO tagDao = DBUtil.getTagDAO();

    try {
      DeviceEntity deviceEntity = DBUtil.getDeviceDAO().getDevice(appId, deviceId);
      if(deviceEntity != null) {
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
        return Response
                .status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
      }
    } catch (DeviceNotFoundException e) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "DeviceId not found");
      return Response
              .status(Response.Status.NOT_FOUND)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    } catch (Exception e) {
      LOGGER.error("addDeviceTags : caught exception creating tags for deviceId={}", deviceId);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error creating tags");
      return Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }
  }

  @DELETE
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response deleteAllTags(@Context HttpHeaders headers,
                                @PathParam(MMXServerConstants.DEVICEID_PATH_PARAM) String deviceId) {
    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    LOGGER.trace("deleteAllTags : deviceId={}", deviceId);

    try {
      DeviceEntity deviceEntity = DBUtil.getDeviceDAO().getDevice(appId, deviceId);
      if(deviceEntity != null) {
        DBUtil.getTagDAO().deleteAllTagsForDevice(deviceEntity);
        return Response.status(Response.Status.OK)
                        .type(MediaType.TEXT_PLAIN).entity("deleted all tags").build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Device not found");
        return Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(errorResponse).build();
      }
    } catch (DeviceNotFoundException e) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "DeviceId not found");
      return Response
              .status(Response.Status.NOT_FOUND)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    } catch (Exception e) {
      LOGGER.error("deleteAllTags : caught exception deleting tags for device={}", deviceId);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error deleting tags for device");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse).build();
    }
  }

  @DELETE
  @Path("/{" + MMXServerConstants.TAGNAME_PATH_PARAM + "}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteTag(@Context HttpHeaders headers,
                            @PathParam(MMXServerConstants.DEVICEID_PATH_PARAM) String deviceId,
                            @PathParam(MMXServerConstants.TAGNAME_PATH_PARAM) String tag) {

    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    LOGGER.trace("deleteTag : deviceId={}, tag={}", deviceId, tag);

    try {
      DeviceEntity deviceEntity = DBUtil.getDeviceDAO().getDevice(appId, deviceId);
      if(deviceEntity != null) {
        DBUtil.getTagDAO().deleteTagsForDevice(deviceEntity, Arrays.asList(tag));
        return Response.status(Response.Status.OK).build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Device not found");
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse).build();
      }
    } catch (DeviceNotFoundException e) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "DeviceId not found");
      return Response
              .status(Response.Status.NOT_FOUND)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    } catch (Exception e) {
      LOGGER.error("deleteTag : caught exception deleting tags for deviceId={}", deviceId, e);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error deleting tags for device");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse).build();
    }
  }
}

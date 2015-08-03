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

import com.google.common.base.Strings;
import com.magnet.mmx.server.plugin.mmxmgmt.api.AbstractBaseResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DbInteractionException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TagDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserEntity;
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

@Path("/users/{"+MMXServerConstants.USERNAME_PATH_PARAM +"}/tags")
public class MMXUserTagsResource extends AbstractBaseResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXUserTagsResource.class);

  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response getUserTags(@Context HttpHeaders headers,
                              @PathParam(MMXServerConstants.USERNAME_PATH_PARAM) String username) {
    LOGGER.trace("getUserTags : username={}", username);

    ErrorResponse response = isAuthenticated(headers, DBUtil.getAppDAO());

    if (response != null)
      return response.toJaxRSResponse();

    if(Strings.isNullOrEmpty(username)) {
      ErrorResponse errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "No username specified");
      return Response
              .status(Response.Status.BAD_REQUEST)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    try {
      String mmxUsername = Helper.getMMXUsername(username, appId);
      UserEntity entity = DBUtil.getUserDAO().getUser(mmxUsername);
      if(entity != null) {
        LOGGER.trace("getUserTags : getting tags for username : {}", mmxUsername);
        List<String> tagnames = DBUtil.getTagDAO().getTagsForUsername(appId, mmxUsername);
        UserTagInfo tagInfo = new UserTagInfo(username, tagnames);
        return Response.status(Response.Status.OK).entity(tagInfo).build();
      }
    } catch (Exception e) {
      LOGGER.error("getUserTags : caught exception retrieiving user tags for username={}", username, null);
    }

    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "username not found");
    return Response
            .status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(errorResponse)
            .build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response addUserTags(@Context HttpHeaders headers,
                              @PathParam(MMXServerConstants.USERNAME_PATH_PARAM) String username,
                              TagList tagList) {

    LOGGER.trace("addUserTags : tagList={}", tagList);

    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

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

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);
    String mmxUsername = Helper.getMMXUsername(username, appId);
    try {
      UserEntity userEntity = DBUtil.getUserDAO().getUser(mmxUsername);
      if(userEntity != null) {
        TagDAO tagDao = DBUtil.getTagDAO();
        for (String tag : tagList.getTags()) {
          tagDao.createUsernameTag(tag, appId, mmxUsername);
        }
        return Response
                .status(Response.Status.CREATED)
                .type(MediaType.TEXT_PLAIN)
                .entity("Successfully Created Tags")
                .build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "username not found");
        return Response
                .status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
      }
    } catch (Exception e) {
      LOGGER.error("addUserTags : caught exception creating tags for username={}", username);
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
  public Response deleteAllUserTags(@Context HttpHeaders headers,
                                    @PathParam(MMXServerConstants.USERNAME_PATH_PARAM) String username) {
    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());
    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    if(Strings.isNullOrEmpty(username)) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "No username param specified");
      return Response.status(Response.Status.BAD_REQUEST)
              .type(MediaType.APPLICATION_JSON_TYPE)
              .entity(errorResponse)
              .build();
    }

    TagDAO tagDAO = DBUtil.getTagDAO();

    try {
      String mmxUsername = Helper.getMMXUsername(username, appId);
      UserEntity entity = DBUtil.getUserDAO().getUser(mmxUsername);
      if(entity != null) {
        tagDAO.deleteAllTagsForUsername(appId, mmxUsername);
        return Response.status(Response.Status.OK)
                .type(MediaType.TEXT_PLAIN)
                .entity("Deleted all tags").build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "username not found");
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse).build();
      }
    } catch (Exception e) {
      LOGGER.error("deleteAllUserTags : caught exception deleting tags");
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Invalid username");
      return Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }
  }

  @DELETE
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  @Path("/{"+MMXServerConstants.TAGNAME_PATH_PARAM+"}")
  public Response deleteUserTag(@Context HttpHeaders headers,
                                @PathParam(MMXServerConstants.USERNAME_PATH_PARAM) String username,
                                @PathParam(MMXServerConstants.TAGNAME_PATH_PARAM) String tag) {

    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    if(Strings.isNullOrEmpty(username)) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "No username param specified");
      return Response.status(Response.Status.BAD_REQUEST)
              .type(MediaType.TEXT_PLAIN)
              .entity(errorResponse)
              .build();
    }

    LOGGER.trace("deleteUserTag : username={}, tag={}", username, tag);

    TagDAO tagDAO = DBUtil.getTagDAO();

    try {
      String mmxUsername = Helper.getMMXUsername(username, appId);
      UserEntity entity = DBUtil.getUserDAO().getUser(mmxUsername);
      if(entity != null) {
        tagDAO.deleteTagsForUsername(appId, mmxUsername, Arrays.asList(tag));
        return Response.status(Response.Status.OK)
                         .type(MediaType.TEXT_PLAIN)
                         .entity("Deleted tag").build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "User not found");
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
      }
    } catch (DbInteractionException e) {
      LOGGER.error("deleteUserTag : username={}, tag={}", username, tag, e);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Invalid username");
      return Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }
  }
}

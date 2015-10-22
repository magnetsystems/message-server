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

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.api.v1.CheckAppId;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.*;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;

@Path("/integration/users")
@CheckAppId
public class IntegrationUserResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationUserResource.class);

  @Context
  private HttpServletRequest servletRequest;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response createUsers(MMXUserInfo userInfo) {
    LOGGER.trace("createUsers : userInfo={}", userInfo);
    try {
      AppEntity appEntity;
      Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);

      if(o instanceof AppEntity) {
        appEntity = (AppEntity) o;
        LOGGER.debug("getDeviceById : retrieiving appEntity from servletRequestContext entity={}", appEntity);
      } else {
        LOGGER.error("getDeviceById : appEntity is not set");
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .build();
      }
      userInfo.setAppId(appEntity.getAppId());
      UserManagerService.createUser(appEntity.getAppId(), userInfo);
    } catch (UserAlreadyExistsException e) {
      LOGGER.info("createUser : exception caught userInfo={}", userInfo, e);
      String message = String.format(ErrorMessages.ERROR_USERNAME_EXISTS, userInfo.getUsername());
      throw new WebApplicationException(
          Response
              .status(Response.Status.CONFLICT)
              .entity(new ErrorResponse(ErrorCode.INVALID_USER_NAME, message))
              .build()
      );
    } catch (ServerNotInitializedException e) {
      LOGGER.error("createUser : exception caught userInfo={}", userInfo, e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (IllegalArgumentException e) {
      LOGGER.error("createUser : exception caught userInfo={}", userInfo, e);
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
    return Response.status(Response.Status.CREATED).build();
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public Response createOrReplace(MMXUserInfo userInfo) {
    LOGGER.trace("createOrReplace : userInfo={}", userInfo);
    boolean created = false;
    try {
      String appId = userInfo.getAppId();
      AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
      created = UserManagerService.updateUser(appId, userInfo);
      if (created)
        return Response.status(Response.Status.CREATED).build();
      else
        return Response.status(Response.Status.OK).build();
    } catch (ServerNotInitializedException e) {
      LOGGER.error("createOrReplace : exception caught userInfo={}", userInfo, e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (UserNotFoundException e) {
      LOGGER.error("createOrReplace : exception caught userInfo={}", userInfo, e);
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    } catch (IllegalArgumentException e) {
      LOGGER.error("createOrReplace : exception caught userInfo={}", userInfo, e);
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
  }

  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{username}")
  public Response getUser(@PathParam("username") String username) {
    AppEntity appEntity;
    Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);

    if(o instanceof AppEntity) {
      appEntity = (AppEntity) o;
      LOGGER.debug("getUser : retrieiving appEntity from servletRequestContext entity={}", appEntity);
      try {
        MMXUserInfo userInfo = UserManagerService.getUser(appEntity.getAppId(), username);
        return Response.status(Response.Status.OK).entity(userInfo).build();
      } catch (UserNotFoundException e) {
        return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(ErrorCode.INVALID_USER_NAME, "User not found")).build();
      }
    } else {
      LOGGER.error("getUser : appEntity is not set");
      return Response
          .status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Illegal app id"))
          .build();
    }
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{username}")
  public Response deleteUser(@PathParam("username") String username) {
    LOGGER.trace("deleteUser : username={}", username);
    try {
      AppEntity appEntity;
      Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);

      if(o instanceof AppEntity) {
        appEntity = (AppEntity) o;
        MMXUserInfo userInfo = new MMXUserInfo();
        userInfo.setUsername(username);
        UserManagerService.deleteUser(appEntity.getAppId(), userInfo);
        return RestUtils.getOKJAXRSResp();
      } else {
        LOGGER.error("deleteUser : appEntity is not set");
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .build();
      }
    } catch (UserNotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse(ErrorCode.INVALID_USER_NAME, "user not found"))
          .build();
    } catch (ServerNotInitializedException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "server not initialized"))
          .build();
    }
  }

  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }


  protected ErrorResponse validateUserName(MMXUserInfo info) {
    String username = info.getUsername();
    ErrorResponse error;
    if (username == null || username.isEmpty()) {
      error = new ErrorResponse(ErrorCode.INVALID_USER_NAME.getCode(), ErrorMessages.ERROR_INVALID_USERNAME_VALUE);
      return error;
    }
    {
      int length = -1;
      try {
        length = username.getBytes(Constants.UTF8_CHARSET).length;
      } catch (UnsupportedEncodingException e) {
        LOGGER.warn("UnsupportedEncodingException", e);
      }
      if (length < Constants.MMX_MIN_USERID_LEN || length > Constants.MMX_MAX_USERID_LEN) {
        error = new ErrorResponse(ErrorCode.INVALID_USER_NAME.getCode(), String.format(ErrorMessages.ERROR_USERNAME_INVALID_LENGTH, Constants.MMX_MIN_USERID_LEN, Constants.MMX_MAX_USERID_LEN));
        return error;
      }
    }
    {
      boolean hasInvalidChars = JIDUtil.checkUsernameForInvalidCharacters(username);
      if (hasInvalidChars) {
        error = new ErrorResponse(ErrorCode.INVALID_USER_NAME.getCode(), ErrorMessages.ERROR_USERNAME_INVALID_CHARACTERS);
        return error;
      }
    }
    return null;
  }
}
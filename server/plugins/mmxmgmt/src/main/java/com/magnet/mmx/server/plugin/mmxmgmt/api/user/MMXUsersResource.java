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
package com.magnet.mmx.server.plugin.mmxmgmt.api.user;

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.AbstractBaseResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.UserQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserQueryBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserSearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PostProcessor;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.server.plugin.mmxmgmt.search.UserEntityPostProcessor;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXUserInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.util.ServerNotInitializedException;
import com.magnet.mmx.server.plugin.mmxmgmt.util.UserManagerService;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 */
@Path("/users")
public class MMXUsersResource extends AbstractBaseResource {
  public static final String USERNAME_PARAM = "username";
  public static final String EMAIL_PARAM = "email";
  public static final String NAME_PARAM = "name";
  public static final String TAG_PARAM = "tag";

  private static final Logger LOGGER = LoggerFactory.getLogger(MMXUsersResource.class);

  @Context
  private HttpServletRequest servletRequest;


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUsers2(@QueryParam(USERNAME_PARAM) String username,
                           @QueryParam(NAME_PARAM) String name,
                           @QueryParam(EMAIL_PARAM) String email,
                           @QueryParam(MMXServerConstants.SORT_BY_PARAM) String sortby,
                           @QueryParam(MMXServerConstants.SORT_ORDER_PARAM) String sortorder,
                           @QueryParam(MMXServerConstants.SIZE_PARAM) int size,
                           @QueryParam(MMXServerConstants.OFFSET_PARAM) int offset,
                           @QueryParam(TAG_PARAM) List<String> tags) {

    LOGGER.trace("getUsers : email={}, name={}, sortby={}, sortorder={}", new Object[]{email, name, sortby, sortorder});

    try {
      AppEntity appEntity = RestUtils.getAppEntity(servletRequest);
      if(appEntity == null) {
        return RestUtils.getInternalErrorJAXRSResp(new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "App id not set"));
      }

      if(size < 0)
        return RestUtils.getBadReqJAXRSResp(new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                                                              "size should be non-zero positive value"));
      if(offset < 0)
        return RestUtils.getBadReqJAXRSResp(new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                                                              "offset should be non-negative value"));
      String appId = appEntity.getAppId();
      UserQuery userQuery = new UserQuery();
      userQuery.setDisplayName(name);
      userQuery.setEmail(email);
      userQuery.setTags(tags);
      if(!Strings.isNullOrEmpty(username)) {
        userQuery.setUsername(username);
      }
      UserQueryBuilder.UserSortBy userSortBy = validateSortBy(sortby);

      if(userSortBy == null) {
        userSortBy = UserQueryBuilder.UserSortBy.USERNAME;
      }


      SortOrder sortOrder = Helper.validateSortOrder(sortorder);

      if (sortOrder == null) {
        sortOrder = SortOrder.ASCENDING;
      }

      SortInfo sortInfo = SortInfo.build(userSortBy.name(), sortOrder.name());
      PaginationInfo paginationInfo = PaginationInfo.build(size, offset);

      UserQueryBuilder userQueryBuilder = new UserQueryBuilder(true);
      QueryBuilderResult result = userQueryBuilder.buildSearchQuery(userQuery, appId, paginationInfo, sortInfo);
      SearchResult<UserEntity> userEntitySearchResult = DBUtil.getUserDAO().getUsersWithPagination(result, paginationInfo);
      UserSearchResult searchResult = new UserSearchResult();

      List<UserEntity> userEntities = userEntitySearchResult.getResults();
      PostProcessor<UserEntity> postProcessor = new UserEntityPostProcessor();
      for (UserEntity me : userEntities) {
        postProcessor.postProcess(me);
      }

      searchResult.setResults(userEntities);
      searchResult.setSize(userEntities.size());
      searchResult.setOffset(userEntitySearchResult.getOffset());
      searchResult.setTotal(userEntitySearchResult.getTotal());
      Response response = Response
              .status(Response.Status.OK)
              .entity(searchResult)
              .build();
      return response;
    } catch (ValidationException e) {
      LOGGER.warn("ValidationException", e);
      throw new WebApplicationException(
              Response
                      .status(Response.Status.BAD_REQUEST)
                      .entity(e.getError())
                      .build()
      );
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response createUsers(@Context HttpHeaders headers, MMXUserInfo userCreationInfo) {
    LOGGER.trace("createUsers : userCreationInfo={}", userCreationInfo);
    try {
      AppEntity appEntity = RestUtils.getAppEntity(servletRequest);
      if(appEntity == null) {
        return RestUtils.getInternalErrorJAXRSResp(new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "App id not set"));
      }
      String appId = appEntity.getAppId();
      {
        ErrorResponse validationResponse = validateUserName(userCreationInfo);
        if (validationResponse != null) {
          return Response
              .status(Response.Status.BAD_REQUEST)
              .entity(validationResponse)
              .build();
        }
      }
      {
        ErrorResponse validationResponse = validatePassword(userCreationInfo);
        if (validationResponse != null) {
          return Response
              .status(Response.Status.BAD_REQUEST)
              .entity(validationResponse)
              .build();
        }
      }
      UserManagerService.createUser(appId, userCreationInfo);
    } catch (UserAlreadyExistsException e) {
      LOGGER.info("createUser : exception caught userCreationInfo={}", userCreationInfo, e);
      String message = String.format(ErrorMessages.ERROR_USERNAME_EXISTS, userCreationInfo.getUsername());
      throw new WebApplicationException(
          Response
              .status(Response.Status.CONFLICT)
              .entity(new ErrorResponse(ErrorCode.INVALID_USER_NAME, message))
              .build()
      );
    } catch (ServerNotInitializedException e) {
      LOGGER.error("createUser : exception caught userCreationInfo={}", userCreationInfo, e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (IllegalArgumentException e) {
      LOGGER.error("createUser : exception caught userCreationInfo={}", userCreationInfo, e);
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
    return Response.status(Response.Status.CREATED).build();
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public Response createOrReplace(@Context HttpHeaders headers, MMXUserInfo userCreationInfo) {
    LOGGER.trace("createOrReplace : userCreationInfo={}", userCreationInfo);
    AppEntity appEntity = RestUtils.getAppEntity(servletRequest);
    if(appEntity == null) {
      return RestUtils.getInternalErrorJAXRSResp(new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "App id not set"));
    }
    boolean created;
    try {
      String appId = appEntity.getAppId();
      {
        ErrorResponse validationResponse = validateUserName(userCreationInfo);
        if (validationResponse != null) {
          return Response
              .status(Response.Status.BAD_REQUEST)
              .entity(validationResponse)
              .build();
        }
      }
      created = UserManagerService.updateUser(appId, userCreationInfo);
      if (created)
        return Response.status(Response.Status.CREATED).build();
      else
        return Response.status(Response.Status.OK).build();
    } catch (ServerNotInitializedException e) {
      LOGGER.error("createOrReplace : exception caught userCreationInfo={}", userCreationInfo, e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (UserNotFoundException e) {
      LOGGER.error("createOrReplace : exception caught userCreationInfo={}", userCreationInfo, e);
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    } catch (IllegalArgumentException e) {
      LOGGER.error("createOrReplace : exception caught userCreationInfo={}", userCreationInfo, e);
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
  }

  @DELETE
  @Path("{username}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteUser(@Context HttpHeaders headers, @PathParam("username") String username) {
    AppEntity appEntity = RestUtils.getAppEntity(servletRequest);
    if(appEntity == null) {
      return RestUtils.getInternalErrorJAXRSResp(new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "App id not set"));
    }
    String appId = appEntity.getAppId();
    LOGGER.trace("deleteUser : username={}, appId={}", username, appId);
    MMXUserInfo userInfo = new MMXUserInfo();
    userInfo.setUsername(username);
    try {
      AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
      UserManagerService.deleteUser(appId, userInfo);
    } catch (UserNotFoundException e) {
      LOGGER.error("deleteUser : exception caught userInfo={}", userInfo, e);
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    } catch (ServerNotInitializedException e) {
      LOGGER.error("deleteUser : exception caught userInfo={}", userInfo, e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    return Response.status(Response.Status.OK).build();
  }

  private UserQueryBuilder.UserSortBy validateSortBy(String input) throws ValidationException {
    if (input == null || input.isEmpty()) {
      return null;
    }
    UserQueryBuilder.UserSortBy sortBy = UserQueryBuilder.UserSortBy.find(input);
    if (sortBy == null) {
      String message = String.format(ErrorMessages.ERROR_INVALID_SORT_BY_VALUE, input);
      LOGGER.warn(message);
      throw new ValidationException(new ErrorResponse(ErrorCode.INVALID_SORT_BY_VALUE, message));
    }
    return sortBy;
  }


  public static ErrorResponse validateUserName(MMXUserInfo info) {
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

  public static ErrorResponse validatePassword(MMXUserInfo info) {
    ErrorResponse error;
    //check password
    String password = info.getPassword();
    if (password == null || password.isEmpty()) {
      error = new ErrorResponse(ErrorCode.INVALID_USER_PASSWORD.getCode(), ErrorMessages.ERROR_INVALID_PASSWORD_VALUE);
      return error;
    }
    {
      int length = -1;
      try {
        length = password.getBytes(Constants.UTF8_CHARSET).length;
      } catch (UnsupportedEncodingException e) {
        LOGGER.warn("UnsupportedEncodingException", e);
      }
      if (length > MMXServerConstants.MMX_MAX_PASSWORD_LEN) {
        error = new ErrorResponse(ErrorCode.INVALID_USER_NAME.getCode(), String.format(ErrorMessages.ERROR_PASSWORD_INVALID_LENGTH, MMXServerConstants.MMX_MAX_PASSWORD_LEN));
        return error;
      }
    }
    return null;
  }

}


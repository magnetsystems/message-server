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
import java.util.Map;

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

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.TagList;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.UserTagInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DbInteractionException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TagDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.Utils;

/**
 * A proxy to manage an existing XMPP session, and tags using an oauth token.
 * In the future, most of the functionality from IntegrationUserResource should be
 * migrated to this class, but it should remain as administrative functionality.
 */
@Path("users")
public class UserResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserResource.class);
  
  public enum UserStatus {
    ACTIVE,
    CREATED,
    INACTIVE
  };
  
  public enum UserRealm {
    AD,
    DB,
    FACEBOOK,
    GOOGLEPLUS,
    LDAP,
    OTHER,
    TWITTER
  };
    
  public class User {    
    @SerializedName("userIdentifier")
    private String mUserIdentifier;
    @SerializedName("email")
    private String mEmail;
    @SerializedName("roles")
    private String[] mRoles;
    @SerializedName("userStatus")
    private UserStatus mUserStatus;
    @SerializedName("userName")
    private String mUserName;
    @SerializedName("userRealm")
    private UserRealm mUserRealm;
    @SerializedName("firstName")
    private String mFirstName;
    @SerializedName("lastName")
    private String mLastName;
    @SerializedName("clientId")
    private String mClientId;
    @SerializedName("userAccountData")
    private Map<String, String> mUserAccountData;
    
    public String getUserIdentifier() {
      return mUserIdentifier;
    }
    public String getEmail() {
      return mEmail;
    }
    public String[] getRoles() {
      return mRoles;
    }
    public UserStatus getUserStatus() {
      return mUserStatus;
    }
    public String getUserName() {
      return mUserName;
    }
    public UserRealm getUserRealm() {
      return mUserRealm;
    }
    public String getFirstName() {
      return mFirstName;
    }
    public String getLastName() {
      return mLastName;
    }
    public String getClientId() {
      return mClientId;
    }
    public Map<String, String> getUserAccountData() {
      return mUserAccountData;
    }
  }
  
  /**
   * Disconnect an XMPP session of a user identified by an oauth token.
   * @param headers
   * @return
   */
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("logout")
  public Response logout(@Context HttpHeaders headers) {
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    
    JID from = RestUtils.createJID(tokenInfo);
    XMPPServer xmppSrv = XMPPServer.getInstance();
    SessionManager sessionMgr = xmppSrv.getSessionManager();
    ClientSession session = sessionMgr.getSession(from);
    if (session == null) {
      ErrorResponse response = new ErrorResponse(ErrorCode.USER_NOT_LOGIN,
          String.format("Session is not found: %s [%s]", from, tokenInfo.getUserName()));
      return RestUtils.getJAXRSResp(Response.Status.GONE, response);
    }
    // Terminate the session now.
    session.close();
    return RestUtils.getOKJAXRSResp();
  }

  /**
   * Get the tags of the current user.
   * @param headers
   * @return
   */
  @GET
  @Path("tags")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response getUserTags(@Context HttpHeaders headers) {
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    LOGGER.trace("getUserTags : username={}", tokenInfo.getUserName());

    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    try {
      String mmxUsername = Helper.getMMXUsername(tokenInfo.getUserId(), appId);
      UserEntity entity = DBUtil.getUserDAO().getUser(mmxUsername);
      if(entity != null) {
        LOGGER.trace("getUserTags : getting tags for username : {}", mmxUsername);
        List<String> tagnames = DBUtil.getTagDAO().getTagsForUsername(appId, mmxUsername);
        UserTagInfo tagInfo = new UserTagInfo(tokenInfo.getUserId(), tagnames);
        return Response.status(Response.Status.OK).entity(tagInfo).build();
      }
    } catch (Exception e) {
      LOGGER.error("getUserTags : caught exception retrieiving user tags for username={}",
          tokenInfo.getUserName(), null);
    }

    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "username not found");
    return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
  }

  /**
   * Add tags to the current user.
   * @param headers
   * @param tagList
   * @return
   */
  @POST
  @Path("tags")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response addUserTags(@Context HttpHeaders headers, TagList tagList) {
    ErrorResponse errorResponse;

    LOGGER.trace("addUserTags : tagList={}", tagList);

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
      String mmxUsername = Helper.getMMXUsername(tokenInfo.getUserId(), appId);
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
        return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
      }
    } catch (Exception e) {
      LOGGER.error("addUserTags : caught exception creating tags for username={}", tokenInfo.getUserName());
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error creating tags");
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
  }

  /**
   * Delete all tags from the current user.
   * @param headers
   * @return
   */
  @DELETE
  @Path("tags")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response deleteAllUserTags(@Context HttpHeaders headers) {
    ErrorResponse errorResponse;
    
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }

    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    TagDAO tagDAO = DBUtil.getTagDAO();
    try {
      String mmxUsername = Helper.getMMXUsername(tokenInfo.getUserId(), appId);
      UserEntity entity = DBUtil.getUserDAO().getUser(mmxUsername);
      if(entity != null) {
        tagDAO.deleteAllTagsForUsername(appId, mmxUsername);
        return Response.status(Response.Status.OK)
                .type(MediaType.TEXT_PLAIN)
                .entity("Deleted all tags").build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "username not found");
        return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
      }
    } catch (Exception e) {
      LOGGER.error("deleteAllUserTags : caught exception deleting tags");
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Invalid username");
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
  }

  /**
   * Delete a specified tag from the current user.
   * @param headers
   * @param tag
   * @return
   */
  @DELETE
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  @Path("tags/{"+MMXServerConstants.TAGNAME_PATH_PARAM+"}")
  public Response deleteUserTag(@Context HttpHeaders headers,
                                @PathParam(MMXServerConstants.TAGNAME_PATH_PARAM) String tag) {
    ErrorResponse errorResponse;
    
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }

    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    LOGGER.trace("deleteUserTag : username={}, tag={}", tokenInfo.getUserName(), tag);

    try {
      String mmxUsername = Helper.getMMXUsername(tokenInfo.getUserId(), appId);
      UserEntity entity = DBUtil.getUserDAO().getUser(mmxUsername);
      if(entity != null) {
        TagDAO tagDAO = DBUtil.getTagDAO();
        tagDAO.deleteTagsForUsername(appId, mmxUsername, Arrays.asList(tag));
        return Response.status(Response.Status.OK)
                         .type(MediaType.TEXT_PLAIN)
                         .entity("Deleted tag").build();
      } else {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "User not found");
        return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
      }
    } catch (DbInteractionException e) {
      LOGGER.error("deleteUserTag : username={}, tag={}", tokenInfo.getUserName(), tag, e);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Invalid username");
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
  }
}

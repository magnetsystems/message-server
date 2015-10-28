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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
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

/**
 * A proxy to manage an existing XMPP session using an oauth token.  In the
 * future, most of the functionality from IntegrationUserResource should be
 * migrated to this class, but it should remain as administrative functionality.
 */
@Path("users/")
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
  
  @Context
  private HttpServletRequest servletRequest;
  
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
}

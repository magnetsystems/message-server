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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
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
  
  @Context
  private HttpServletRequest servletRequest;
  
  /**
   * Disconnect an XMPP session of a user identified by an oauth token.
   * @param headers
   * @return
   */
  @GET
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

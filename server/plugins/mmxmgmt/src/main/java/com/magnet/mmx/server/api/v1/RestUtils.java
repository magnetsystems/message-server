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
package com.magnet.mmx.server.api.v1;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.magnet.mmx.sasl.BFOAuthAccessor;
import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;

/**
 */
public class RestUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(RestUtils.class);

  private static String MISSING_HEADER = "Authentication failed : mandatory header %s is missing";
  private static String INVALID_HEADER_VALUE = "Authentication failed : header %s has an invalid value %s";
 
  private static Map<String, TokenInfo> sAuthTokenCache = new Hashtable<String, TokenInfo>();
  
  /**
   * Get the authentication information via the auth token from HTTP header.
   * The "Authorization" header should be in the form of "Bearer auth-token".
   * We also accept a malformed value without the "Bearer" token.
   * @param headers
   * @return The token info object or null
   */
  public static TokenInfo getAuthTokenInfo(HttpHeaders headers) {
    String authToken = null;
    TokenInfo tokenInfo = null;
    String authHeader = headers.getHeaderString("Authorization");
    if (authHeader != null) {
      // Skip the standard "Bearer " token, and allow the malformed value.
      int index = authHeader.indexOf(' ');
      authToken = (index < 0) ? authHeader : authHeader.substring(index+1);
    }
    if (authToken != null) {
      tokenInfo = sAuthTokenCache.get(authToken);
      if (tokenInfo == null || isTokenExpired(tokenInfo)) {
        if (tokenInfo != null) {
          // remove the expired cached auth token info.
          sAuthTokenCache.remove(authToken);
        }
        try {
          tokenInfo = BFOAuthAccessor.getTokenInfo(authToken);
          sAuthTokenCache.put(authToken, tokenInfo);
        } catch (IOException e) {
          LOGGER.warn("Unable to get auth token info from MAX server", e);
        }
      }
    }
    return tokenInfo;
  }
  
  private static boolean isTokenExpired(TokenInfo tokenInfo) {
    return !tokenInfo.isAuthenticated();
//    // TODO: is it in seconds?
//    Integer expiresIn = tokenInfo.getTokenExpiresIn();
//    if (expiresIn == null) {
//      return false;
//    }
//    Long created = tokenInfo.getTokenCreationTime();
//    if (created == null) {
//      return false;
//    }
//    return (System.currentTimeMillis() > (created + expiresIn * 1000));
  }
  
  /**
   * Create a JID from an OAuth token information.
   * @param tokenInfo
   * @return
   * @see #getAuthTokenInfo(HttpHeaders)
   */
  public static JID createJID(TokenInfo tokenInfo) {
    String node = JIDUtil.makeNode(tokenInfo.getUserId(), tokenInfo.getMmxAppId());
    return XMPPServer.getInstance().createJID(node, tokenInfo.getDeviceId(), true);
  }
  
  public static JID createJID(String userId, String appId, String deviceId) {
    String node = JIDUtil.makeNode(userId, appId);
    return XMPPServer.getInstance().createJID(node, deviceId, true);
  }
  
  public static Response getJAXRSResp(Response.Status status, ErrorResponse errorResponse) {
    return Response.status(status).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
  }

  public static Response getBadReqJAXRSResp(ErrorResponse errorResponse) {
    return getJAXRSResp(Response.Status.BAD_REQUEST, errorResponse);
  }

  public static Response getUnauthJAXRSResp() {
    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.AUTH_BAD_TOKEN,
        "Auth token is unavailable, expired or invalid");
    return RestUtils.getJAXRSResp(Response.Status.UNAUTHORIZED, errorResponse);
  }
  
  public static Response getOKJAXRSResp() {
    return Response.status(Response.Status.OK).build();
  }

  public static Response getOKJAXRSResp(Object entity) {
    return Response.status(Response.Status.OK).entity(entity).type(MediaType.APPLICATION_JSON).build();
  }

  public static Response getCreatedJAXRSResp() {
    return Response.status(Response.Status.CREATED).build();
  }

  public static Response getInternalErrorJAXRSResp(ErrorResponse errorResponse) {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                   .type(MediaType.APPLICATION_JSON)
                   .entity(errorResponse).build();
  }

  public static AppEntity getAppEntity(HttpServletRequest request) {
    Object o = request.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);
    AppEntity appEntity = null;
    if(o instanceof AppEntity) {
      appEntity = (AppEntity)o;
    }
    return appEntity;
  }

  public static Response buildMissingHeaderResponse(String header) {
    ErrorResponse mmxErrorResponse = new ErrorResponse(ErrorCode.AUTH_MISSING,
            String.format(MISSING_HEADER, header));
    Response httpErrorResponse = Response.status(Response.Status.UNAUTHORIZED)
            .entity(mmxErrorResponse).build();
    return httpErrorResponse;
  }

  public static Response buildInvalidHeaderResponse(ErrorCode code, String header, String value) {
    ErrorResponse mmxErrorResponse = new ErrorResponse(code,
            String.format(INVALID_HEADER_VALUE, header, value));
    Response httpErrorResponse = Response.status(Response.Status.UNAUTHORIZED)
            .entity(mmxErrorResponse).build();
    return httpErrorResponse;
  }
}

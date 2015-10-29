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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.google.gson.Gson;
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
  private static String DEFAULT_MAX_SERVER_BASE_URL = "http://localhost:8443/api/com.magnet.server";
 
  private static Map<String, TokenInfo> sAuthTokenCache = new Hashtable<String, TokenInfo>();
  
  public static <T> T doMAXGet(String authToken, String path, Map<String, List<String>> reqt,
                             Class<T> respClz) throws IOException {
    String maxServerBaseUrl = JiveGlobals.getProperty("mmx.auth.server.base.url", DEFAULT_MAX_SERVER_BASE_URL);
    LOGGER.debug("Sending GET to " + maxServerBaseUrl+path);
    Reader reader = null;
    HttpURLConnection conn = null;
    try {
      URL url;
      if (reqt == null || reqt.isEmpty()) {
        url = new URL(maxServerBaseUrl + path);
      } else {
        UriBuilder builder = UriBuilder.fromUri(maxServerBaseUrl + path);
        url = builder.buildFromMap(reqt).toURL();
      }
      LOGGER.debug("Sending GET to "+url.toString());
      conn = getConnection(url);
      conn.setDoOutput(false);
      conn.setUseCaches(false);
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
      conn.setRequestProperty("Authorization", "Bearer " + authToken);
      int respCode = conn.getResponseCode();
      if (respCode != HttpURLConnection.HTTP_OK && respCode != HttpURLConnection.HTTP_CREATED) {
        LOGGER.error("Unexpected response "+respCode+" from "+(maxServerBaseUrl+path));
        return null;
      }
      Gson gson = new Gson();
      reader = new InputStreamReader(conn.getInputStream());
      T resp = gson.fromJson(reader, respClz);
      return resp;
    } catch (Throwable e) {
      LOGGER.error("Unable to get response from "+(maxServerBaseUrl+path), e);
      return null;
    } finally {
      if (reader != null) {
        reader.close();
      }
      if (conn != null) {
        conn.disconnect();
      }
    }
  }
  
  public static <T> T doMAXPost(String authToken, String path, Object reqt,
                                 Class<T> respClass) throws IOException {
    String maxServerBaseUrl = JiveGlobals.getProperty("mmx.auth.server.base.url", DEFAULT_MAX_SERVER_BASE_URL);
    LOGGER.debug("Sending POST to " + maxServerBaseUrl+path);
    Reader reader = null;
    Writer writer = null;
    HttpURLConnection conn = null;
    try {
      conn = getConnection(new URL(maxServerBaseUrl+path));
      conn.setDoOutput(true);
      conn.setUseCaches(false);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
      conn.setRequestProperty("Authorization", "Bearer " + authToken);
      Gson gson = new Gson();
      if (reqt != null) {
        writer = new OutputStreamWriter(conn.getOutputStream());
        gson.toJson(reqt, writer);
      }
      int respCode = conn.getResponseCode();
      if (respCode != HttpURLConnection.HTTP_OK && respCode != HttpURLConnection.HTTP_CREATED) {
        LOGGER.error("Unexpected response "+respCode+" from "+(maxServerBaseUrl+path));
        return null;
      }
      reader = new InputStreamReader(conn.getInputStream());
      T resp = gson.fromJson(reader, respClass);
      return resp;
    } catch (Throwable e) {
      LOGGER.error("Unable to get response from "+(maxServerBaseUrl+path), e);
      return null;
    } finally {
      if (writer != null) {
        writer.close();
      }
      if (reader != null) {
        reader.close();
      }
      if (conn != null) {
        conn.disconnect();
      }
    }
  }
  
  private static HttpURLConnection getConnection(URL url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    return conn;
  }
  
  /**
   * Get the authentication token from HTTP header.
   * @param headers
   * @return The auth token or null.
   */
  public static String getAuthToken(HttpHeaders headers) {
    String authToken = null;
    String authHeader = headers.getHeaderString("Authorization");
    if (authHeader != null) {
      // Skip the standard "Bearer " token, and allow the malformed value.
      int index = authHeader.indexOf(' ');
      authToken = (index < 0) ? authHeader : authHeader.substring(index+1);
    }
    return authToken;
  }
  
  /**
   * Get the authentication information via the auth token from HTTP header.
   * The "Authorization" header should be in the form of "Bearer auth-token".
   * We also accept a malformed value without the "Bearer" token.  If the token
   * is expired, null will be returned.
   * @param headers
   * @return The token info object or null
   */
  public static TokenInfo getAuthTokenInfo(HttpHeaders headers) {
    TokenInfo tokenInfo = null;
    String authToken = getAuthToken(headers);
    if (authToken != null) {
//      tokenInfo = sAuthTokenCache.get(authToken);
      if (isTokenExpired(tokenInfo)) {
        if (tokenInfo != null) {
          // remove the expired cached auth token info.
//          sAuthTokenCache.remove(authToken);
        }
        try {
          tokenInfo = BFOAuthAccessor.getTokenInfo(authToken);
          if (!isTokenExpired(tokenInfo)) {
//            sAuthTokenCache.put(authToken, tokenInfo);
          } else {
            tokenInfo = null;
          }
        } catch (IOException e) {
          LOGGER.warn("Unable to get auth token info from MAX server", e);
        }
      }
    }
    return tokenInfo;
  }
  
  private static boolean isTokenExpired(TokenInfo tokenInfo) {
    return tokenInfo == null || !tokenInfo.isAuthenticated();
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

  public static Response getNotFoundJAXRSResp(ErrorResponse errorResponse) {
    return getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
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

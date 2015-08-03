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

import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.push.CallbackUrlUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Servlet that handles the call back from the devices to push messages
 * Note: This doesn't extend AbstractSecureServlet because the clients can't
 * supply the auth credentials.
 */
public class PushReplyServlet extends HttpServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(PushReplyServlet.class);

  private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

  private static final String ERROR_PUSH_TOKEN = "Supplied token is invalid.";
  private static final String STATUS_OK = "OK";
  private static final String STATUS_ERROR = "ERROR";
  private static final String TOKEN_EXPIRY_MESSAGE_FORMAT = "Supplied token expired at:%s";

  @Override
  public void init(ServletConfig config) throws ServletException {
    LOGGER.info("Initializing:" + PushReplyServlet.class);
    super.init(config);
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String token = request.getParameter(CallbackUrlUtil.KEY_CALL_BACK_URL_TOKEN);

    response.setContentType(CONTENT_TYPE_JSON);

    PrintWriter out = response.getWriter();
    PushResponse presponse = new PushResponse();

    if (token == null || token.isEmpty()) {
      LOGGER.info("null or empty token");
      presponse.setStatus(STATUS_ERROR);
      presponse.setMessage(ERROR_PUSH_TOKEN);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeResponse(out, presponse);
      out.flush();
      return;
    }
    //decode the token
    String[] parts = CallbackUrlUtil.decodeToken(token);
    if (parts == null || parts.length < 2 ) {
      LOGGER.info("Token parsing error for token:" + token);
      presponse.setStatus(STATUS_ERROR);
      presponse.setMessage(ERROR_PUSH_TOKEN);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeResponse(out, presponse);
      out.flush();
      return;
    }

    String tokenTime = parts[1];
    String pushMessageId = parts[0];
    Date currentTime = new Date();
    long currentTimeSeconds = currentTime.getTime()/1000L;
    long parsedTokenTime = Long.parseLong(tokenTime);
    long delta = currentTimeSeconds - parsedTokenTime;
    long tokenTTL = MMXConfiguration.getConfiguration().getLong(MMXConfigKeys.PUSH_CALLBACK_TOKEN_TTL, MMXServerConstants.DEFAULT_PUSH_CALLBACK_TOKEN_TTL);
    if (delta > tokenTTL) {
      LOGGER.info("Token expired for token:" + token);
      Date expiredAt = new Date((parsedTokenTime + tokenTTL) * 100L);
      String message = String.format(TOKEN_EXPIRY_MESSAGE_FORMAT, Utils.buildISO8601DateFormat().format(expiredAt));
      presponse.setStatus(STATUS_ERROR);
      presponse.setMessage(message);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeResponse(out, presponse);
      out.flush();
      return;
    }
    PushMessageDAO dao = new PushMessageDAOImpl(new OpenFireDBConnectionProvider());
    int count = dao.acknowledgePushMessage(pushMessageId, currentTime);

    if (count == 0) {
      LOGGER.info("No push message updated for id:" + token);
      presponse.setStatus(STATUS_ERROR);
      presponse.setMessage(ERROR_PUSH_TOKEN);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } else {
      presponse.setStatus(STATUS_OK);
      response.setStatus(HttpServletResponse.SC_OK);
    }
    writeResponse(out, presponse);
    out.flush();
    return;
  }

  protected void writeResponse(PrintWriter writer, PushResponse response) {
    GsonData.getGson().toJson(response, writer);
  }

  private static class PushResponse {
    private String status;
    private String message;

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }

}

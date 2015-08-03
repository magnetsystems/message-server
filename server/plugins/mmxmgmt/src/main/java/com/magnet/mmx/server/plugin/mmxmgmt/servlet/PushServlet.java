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

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.plugin.mmxmgmt.push.*;
import com.magnet.mmx.server.plugin.mmxmgmt.web.BasicResponse;
import com.magnet.mmx.util.GsonData;
import org.jivesoftware.admin.AuthCheckFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Servlet that handles requests for pushing messages via GCM/APNS to a device id
 */
public class PushServlet extends AbstractSecureServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(PushServlet.class);

  private static final String PATH = "mmxmgmt/push";
  private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
  private static final String PUSH_TEXT = "pingtest";

  private static final String KEY_APP_ID = "appId";
  private static final String KEY_DEVICE_ID = "deviceid";
  private static final String KEY_PING_TYPE = "pingtype";

  private static final String ERROR_INVALID_APPID = "Supplied application id is invalid.";
  private static final String ERROR_INVALID_SEARCH_VALUE = "Supplied search value is invalid.";

  @Override
  public void init(ServletConfig config) throws ServletException {
    LOGGER.info("Initializing:" + PushServlet.class);
    super.init(config);
    // Exclude this check so that the request won't be redirected to the login page.
    AuthCheckFilter.addExclude(PATH);
  }

  @Override
  public void destroy() {
    super.destroy();
    AuthCheckFilter.removeExclude(PATH);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String appId = request.getParameter(KEY_APP_ID);
    String pingType = request.getParameter(KEY_PING_TYPE);
    String deviceId = request.getParameter(KEY_DEVICE_ID);
    String pushText = getRequestBody(request);
    if (pushText != null && pushText.trim().isEmpty()) {
      pushText = null;
    }

    if(Strings.isNullOrEmpty(pingType))
      pingType = Constants.PingPongCommand.ping.name();

    //TODO: Refactor this
    PushSender sender = new PushMessageSender();
    PushRequest pushRequest = new PushRequest(appId, deviceId, new MMXWakeupPayload(pingType, pushText));
    pushRequest.setAppId(appId);
    pushRequest.setDeviceId(deviceId);
    pushRequest.setText(pushText);

    PushResult result = sender.push(pushRequest);

    response.setContentType(CONTENT_TYPE_JSON);

    PrintWriter out = response.getWriter();
    BasicResponse presponse = new BasicResponse();

    if (result.isError()) {
      LOGGER.info("Problem sending push:" + result.getMessage());
      presponse.setMessage(result.getMessage());
      presponse.setStatus(result.getStatus());
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } else {
      presponse.setStatus(result.getStatus());
      response.setStatus(HttpServletResponse.SC_OK);
    }

    writeResponse(out, presponse);
    out.flush();
  }

  /**
   * Read the body from the servlet request.
   * @param request
   * @return
   * @throws IOException
   */
  protected static String getRequestBody(HttpServletRequest request) throws IOException {

    String body = null;
    StringBuilder stringBuilder = null;
    BufferedReader bufferedReader = null;

    try {
      InputStream inputStream = request.getInputStream();
      if (inputStream != null) {
        stringBuilder = new StringBuilder();
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"utf-8") );
        char[] charBuffer = new char[128];
        int bytesRead = -1;
        while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
          stringBuilder.append(charBuffer, 0, bytesRead);
        }
      }
    } catch (IOException ex) {
      throw ex;
    } finally {
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (IOException ex) {
          throw ex;
        }
      }
    }
    body = stringBuilder != null ? stringBuilder.toString() : null;
    return body;
  }

  protected void writeResponse(PrintWriter writer, BasicResponse response) {
    GsonData.getGson().toJson(response, writer);
  }

}

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
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageEntity;
import com.magnet.mmx.util.GsonData;
import org.jivesoftware.admin.AuthCheckFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * PushMessage Servlet to retrieve the push messages
 */
public class PushMessageServlet extends AbstractSecureServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(PushMessageServlet.class);

  private static final String PATH = "mmxmgmt/pushmessages";
  private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
  private static final String KEY_APP_ID = "appId";
  private static final String KEY_DEVICE_ID ="deviceid";
  private static final String ERROR_INVALID_APPID = "Supplied application id is invalid.";
  private static final String ERROR_INVALID_DEVICE_ID = "Supplied device id is invalid.";

  @Override
  public void init(ServletConfig config) throws ServletException {
    LOGGER.info("Initializing:" + PushMessageServlet.class);
    super.init(config);
    AuthCheckFilter.addExclude(PATH);
  }

  @Override
  public void destroy() {
    super.destroy();
    AuthCheckFilter.removeExclude(PATH);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String appId = request.getParameter(KEY_APP_ID);
    String deviceId = request.getParameter(KEY_DEVICE_ID);

    response.setContentType(CONTENT_TYPE_JSON);
    PrintWriter out = response.getWriter();

    //validate stuff
    if (appId == null || appId.isEmpty()) {
      writeErrorResponse(out, MessageCode.INVALID_APPLICATION_ID.name(), ERROR_INVALID_APPID);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    if (deviceId == null || deviceId.isEmpty()) {
      writeErrorResponse(out, MessageCode.INVALID_DEVICE_ID.name(), ERROR_INVALID_DEVICE_ID);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    PushMessageDAO pushMessageDAO = new PushMessageDAOImpl(new OpenFireDBConnectionProvider());
    List<PushMessageEntity> plist = pushMessageDAO.getPushMessages(appId, deviceId);
    PushMessagesResponse presponse = new PushMessagesResponse(plist);
    GsonData.getGson().toJson(presponse, out);
    out.flush();
    response.setStatus(HttpServletResponse.SC_OK);
    return;

  }

  protected void writeErrorResponse (PrintWriter writer, String code, String message) {
    ErrorResponse error = new ErrorResponse();
    error.setCode(code);
    error.setMessage(message);
    GsonData.getGson().toJson(error, writer);
  }

  protected void writeErrorResponse (PrintWriter writer, ErrorResponse error) {
    GsonData.getGson().toJson(error, writer);
  }

  private static class PushMessagesResponse {

    private List<PushMessageEntity> mlist;

    private PushMessagesResponse(List<PushMessageEntity> mlist) {
      this.mlist = mlist;
    }

    public List<PushMessageEntity> getMlist() {
      return mlist;
    }
  }
}

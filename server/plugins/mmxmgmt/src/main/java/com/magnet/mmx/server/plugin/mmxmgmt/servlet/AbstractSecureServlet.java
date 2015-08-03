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

import com.magnet.mmx.server.plugin.mmxmgmt.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Abstract Servlet that enforces BASIC authorization. All servlets that need to be protected using
 * basic authorization should subclass this servlet.
 */
public abstract class AbstractSecureServlet extends HttpServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSecureServlet.class);

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
    boolean isAuthorized = isAuthorized(request);
    if (isAuthorized) {
      super.service(request, resp);
    } else {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }

  protected boolean isAuthorized(HttpServletRequest request) throws IOException {
    return AuthUtil.isAuthorized(request);
  }
}

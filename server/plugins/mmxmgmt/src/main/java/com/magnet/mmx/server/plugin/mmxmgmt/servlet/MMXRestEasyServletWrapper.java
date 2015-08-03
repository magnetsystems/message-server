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
import org.codehaus.jackson.JsonParseException;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Wrapper around the REST Easy end point.
 */
public class MMXRestEasyServletWrapper extends HttpServletDispatcher {
  private static Logger LOGGER = LoggerFactory.getLogger(MMXRestEasyServletWrapper.class);
  private static final long serialVersionUID = 1L;


  public MMXRestEasyServletWrapper() {
  }

  @Override
  public void init(ServletConfig servletConfig) throws ServletException {
    super.init(servletConfig);
    LOGGER.trace("init : {}", servletConfig.getInitParameterNames());
  }

  @Override
  public void destroy() {
    super.destroy();
    LOGGER.trace("destroy : Destroying");
  }

  @Override
  public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
    if(!AuthUtil.isAuthorized(req)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN ,"User is not authorized");
      return;
    }
    /**
     * UGLY ideally need to use JAX-RS Implementation's Exception mapping functionality but seems like there is a bug with its usage,
     * will need to figure out how to programmatically register  exception mappers.
     */
    try {
     super.service(req, resp);
    } catch (javax.servlet.ServletException e) {
      Throwable cause = e.getCause();
      LOGGER.error("service : received malformed json caused by exception of type={}", cause.getClass(), e);
      if(cause instanceof JsonParseException) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON");
      } else {
        throw e;
      }
    }
  }
}

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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 */
public class RestUtils {
  private static String MISSING_HEADER = "Authentication failed : mandatory header %s is missing";
  private static String INVALID_HEADER_VALUE = "Authentication failed : header %s has an invalid value %s";

  public static Response getJAXRSResp(Response.Status status, ErrorResponse errorResponse) {
    return Response.status(status).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
  }

  public static Response getBadReqJAXRSResp(ErrorResponse errorResponse) {
    return getJAXRSResp(Response.Status.BAD_REQUEST, errorResponse);
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

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
package com.magnet.mmx.server.plugin.mmxmgmt.api;


import javax.ws.rs.core.Response;

/**
 */
public class ErrorResponse {
  private int code;
  private String message;

  public ErrorResponse(int errorCode, String errorMessage) {
    this.code = errorCode;
    this.message = errorMessage;
  }

  public ErrorResponse() {
  }

  public ErrorResponse(ErrorCode code, String errorMessage) {
    this (code.getCode(), errorMessage);
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Response toJaxRSResponse() {
    if(code == ErrorCode.ILLEGAL_ARGUMENT.getCode()) {
      return javax.ws.rs.core.Response
              .status(Response.Status.BAD_REQUEST)
              .type("application/json")
              .entity(this)
              .build();
    }
    return javax.ws.rs.core.Response
            .status(javax.ws.rs.core.Response.Status.UNAUTHORIZED)
            .type("application/json")
            .entity(this)
            .build();
  }
}

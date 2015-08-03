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

/**
 * Group together constants required for the servlet code.
 */
public class WebConstants {

  /**
   * Private constructor.
   */
  private WebConstants() {}

  public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

  public static final String KEY_SEARCH_BY = "searchby";
  public static final String KEY_APP_ID = "appId";
  public static final String KEY_SEARCH_VALUE = "value";
  public static final String KEY_SEARCH_VALUE2 = "value2";
  public static final String KEY_SORT_BY = "sortby";
  public static final String KEY_SORT_ORDER = "sortorder";
  public static final String KEY_OFFSET = "offset";
  public static final String KEY_SIZE = "size";
  public static final String KEY_COMMAND = "command";

  public static final String KEY_TOPIC_ID = "topicid";

  public static final String ERROR_INVALID_APPID = "Supplied application identifier is invalid.";
  public static final String ERROR_INVALID_TOPIC_ID = "Supplied topic identifier is invalid.";
  public static final String STATUS_OK = "OK";
  public static final String STATUS_ERROR = "ERROR";


  public static final int APNS_CERT_MAX_SIZE = 5000;

}

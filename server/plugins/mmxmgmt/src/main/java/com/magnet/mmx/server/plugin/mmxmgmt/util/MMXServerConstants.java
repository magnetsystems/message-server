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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import javax.ws.rs.Priorities;

/**
 */
public final class MMXServerConstants {
  //private constructor
  private MMXServerConstants() {}

  public static final String DEVICE_UNAVAILABLE = "NOTAVAILABLE";
  public static final String EMAIL_SENDER = "no-reply@magnet.com";
  public static final String DEFAULT_EMAIL_HOST = "email-smtp.us-east-1.amazonaws.com";
  public static final String DEFAULT_EMAIL_USER = "AKIAJTSTEPNSTK7VH26Q";
  public static final String DEFAULT_EMAIL_PASSWORD = "At2Qas96wh2+afdRVylF0lhiwWnE/Zo/jl+Od49H5Exb";
  public static final String DEFAULT_ALERT_EMAIL_SUBJECT = "Usage limit exceeded";
  public static final String MAX_APP_LIMIT_BODY = "Reached maximum application limit of %d";
  public static final String MAX_HTTP_RATE_EXCEEDED_EMAIL_BODY = "Reached maximum http message rate of %d messages per sec";
  public static final String MAX_XMPP_RATE_EXCEEDED_EMAIL_BODY = "Reached maximum xmpp message rate of %d messages per sec";
  public static final String MAX_DEV_PER_APP_BODY = "Reached maximum device limit of %d";
  public static final int DEFAULT_TIMEOUT_MINUTES = 180;
  public static final int DEFAULT_RETRY_INTERVAL_MINUTES = 15;
  public static final int MIN_RETRY_INTERVAL_MINUTES = 5;
  public static final int DEFAULT_RETRY_COUNT = 3;
  public static final int DEFAULT_WAKEUP_FREQUENCY = 30;
  public static final int DEFAULT_WAKEUP_INITIAL_WAIT = 10;
  public static final int DEFAULT_SMTP_PORT = 587;
  public static final int DEFAULT_REST_HTTP_PORT = 5220;
  public static final int DEFAULT_MAX_APP_PER_OWNER = -1;
  public static final int DEFAULT_MAX_DEVICES_PER_APP = -1;
  public static final int DEFAULT_MAX_XMPP_RATE = -1;
  public static final int DEFAULT_MAX_HTTP_RATE = -1;

  public static final String PUSH_CALLBACK_CONTEXT = "/mmxmgmt";
  public static final String PUSH_CALLBACK_ENDPOINT = "/v1/pushreply";
  public static final long DEFAULT_PUSH_CALLBACK_TOKEN_TTL = 24*60*60; // 24 hrs in seconds
  public static final String PUBLIC_REST_API_MAPPING = "/api/v1/*";
  public static final String DEFAULT_PUBSUB_SERVICE_ID = "pubsub";
  public static final String MMX_MBEAN_NAME = "com.magnet.mmx.server.plugin.mmxmgmt:type=MMXManagedConfigurationMBean";

  /**
   *  15 emails per hour
   */
  public static final int DEFAULT_INTER_EMAIL_TIME_MINUTES = 15;

  public static final String HTTP_HEADER_APP_ID = "X-mmx-app-id";
  public static final String HTTP_HEADER_REST_API_KEY = "X-mmx-api-key";
  public static final String HTTP_HEADER_ADMIN_APP_OWNER_KEY = "X-mmx-app-owner";

  public final static String REPLY_TO = "Reply-To";

  /**
   * APNS Connection pool related default values
   */
  public static final int APNS_POOL_MAX_TOTAL_CONNECTIONS = 100;
  public static final int APNS_POOL_MAX_CONNECTIONS_PER_APP = 20;
  public static final int APNS_POOL_MAX_IDLE_CONNECTIONS_PER_APP = 1;
  public static final int APNS_POOL_IDLE_TTL_MINUTES = 10;

  /**
   * Default values  related to https for the rest API
   */
  public static final boolean DEFAULT_REST_ENABLE_HTTPS = true;
  public static final int DEFAULT_REST_HTTPS_PORT = 5221;

  public static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

  public static final String PUBLIC_API_SERVLET_MAPPING_PREFIX = "/api/v1/";
  public static final String ADMIN_API_SERVLET_MAPPING_PREFIX = "/rest/v1/";

  public static final String RESTEASY_SERVLET_MAPPING_PREFIX_KEY = "resteasy.servlet.mapping.prefix";
  public static final String RESTEASY_RESOURCES_KEY = "resteasy.resources";
  public static final String RESTEASY_PROVIDERS_KEY = "resteasy.providers";

  /**
   * Default values related to admin API
   */
  public static final int ADMIN_API_PORT = 7070;
  public static final boolean ADMIN_API_ENABLE_HTTPS = true;
  public static final String ADMIN_API_CONTEXT = "/mmxadmin";
  public static final String ADMIN_API_REST_MAPPING = "/rest/v1/*";
  public static final int ADMIN_API_HTTPS_PORT = 7077;
  public static final String ADMIN_REST_API_ACCESS_CONTROL_MODE_STRICT = "strict";
  public static final String ADMIN_REST_API_ACCESS_CONTROL_MODE_RELAXED = "relaxed";
  public static final String DEFAULT_ADMIN_REST_API_ACCESS_CONTROL_MODE = ADMIN_REST_API_ACCESS_CONTROL_MODE_STRICT;

  public static final String SORT_ORDER_ASCENDING = "ASC";
  public static final String SORT_ORDER_DESCENDING = "DESC";

  public static final String UTF8_ENCODING = "UTF-8";
  public static final String MESSAGE_BODY_DOT = ".";

  public static final String DEFAULT_PUSH_CALLBACK_PROTOCOL = "http";
  /**
   * Default values related APNS feedback processing.
   */
  public static final int DEFAULT_APNS_FEEDBACK_PROCESS_INITIAL_DELAY_MINUTES = 10;
  public static final int DEFAULT_APNS_FEEDBACK_PROCESS_FREQUENCY_MINUTES = 6*60; //6 hrs in minutes

  public static final String OFFSET_PARAM = "offset";
  public static final String SIZE_PARAM = "size";
  public static final String SORT_BY_PARAM = "sort_by";
  public static final String SORT_ORDER_PARAM = "sort_order";
  public static final String SORT_ORDER_ASC = "asc";
  public static final String SORT_ORDER_DESC = "desc";

  public final static Integer DEFAULT_PAGE_SIZE = Integer.valueOf(100);
  public final static Integer DEFAULT_OFFSET = Integer.valueOf(0);
  public final static String MMX_APP_ENTITY_PROPERTY = "mmxAppEntityProperty";
  public final static String USERNAME_PATH_PARAM = "username";
  public final static String DEVICEID_PATH_PARAM = "deviceId";
  public final static String TAGNAME_PATH_PARAM = "tagname";
  public final static String TOPICNAME_PATH_PARAM = "topic";
  public final static int MAX_TAG_LENGTH = 25;
  public final static int MAX_TOPIC_NAME_LEN = 50;
  public final static int MAX_TOPIC_DESCRIPTION_LEN = 255;

  public final static int MMX_MAX_PASSWORD_LEN = 32;

  public final static int WAKEUP_MUTE_PERIOD_MINUTES_DEFAULT = 30;

  public final static String HTTP_RATE_TYPE = "HTTP";
  public final static String XMPP_RATE_TYPE = "XMPP";

  /**
   * check JAX-RS priorities, we need to make sure that this priority is right after
   * authentication.
   */
  public final static int MMX_RATE_LIMIT_PRIORITY = Priorities.AUTHENTICATION + 1;

  public final static String AMAZING_BOT_NAME = "amazing_bot";
  public final static String ECHO_BOT_NAME = "echo_bot";
  public final static String PLAYER_BOT_NAME = "player_bot";
  public final static String QUICKSTART_APP = "quickstart";
  public final static String RPSLS_APP = "rpsls";

  public final static String DISTRIBUTED_KEY = "mmxdistributed";
  public final static String SERVER_ACK_KEY = "serverack";
  /**
   * Content is included in meta using the following key.
   */
  public static final String TEXT_CONTENT_KEY = "textContent";

  public final static String TOPIC_ROLE_PUBLIC = "public";
}

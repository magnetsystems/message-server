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

/**
 * Keys for the configuration values.
 */
public interface MMXConfigKeys {

  public static final String WAKEUP_INITIAL_WAIT_KEY = "mmx.wakeup.initialwait";
  public static final String WAKEUP_FREQUENCY_KEY = "mmx.wakeup.frequency";
  public static final String WAKEUP_MUTE_PERIOD_MINUTES = "mmx.wakeup.mute.minutes";

  public static final String SMTP_HOSTNAME_KEY = "mmx.smtp.hostname";
  public static final String SMTP_PORT_KEY = "mmx.smtp.port";
  public static final String SMTP_ENABLE_TLS_KEY = "mmx.smtp.tls.enable";
  public static final String SMTP_LINGERDELAY_SECS_KEY = "mmx.smtp.lingerdelay.secs";

  public static final String SMTP_USER_DISPLAY_NAME_KEY = "mmx.system.user.display.name";
  public static final String SMTP_USER_EMAIL_ADDRESS_KEY = "mmx.system.user.email.address";
  public static final String SMTP_REPLY_EMAIL_ADDRESS_KEY = "mmx.system.user.reply.email.address";
  public static final String SMTP_USER_EMAIL_PASSWORD_KEY = "mmx.system.user.email.password";

  public static final String EXT_SERVICE_EVENT_GEO = "mmx.ext.service.event.geo";
  public static final String EXT_SERVICE_EVENT_GEO_SECRET = "mmx.ext.service.event.geo.secret";
  public static final String EXT_SERVICE_PORT = "mmx.ext.service.port";
  public static final String EXT_SERVICE_ENABLED = "mmx.ext.service.enabled";


  public static final String EXT_SERVICE_EVENT_MESSAGE = "mmx.ext.service.event.message";

  /**
   * Number of minutes after which we should retry sending a wakeup message
   */
  public static final String RETRY_INTERVAL_MINUTES = "mmx.retry.interval.minutes";
  /**
   * Number of wakeup message retries. Minimum value is 1.
   */
  public static final String RETRY_COUNT = "mmx.retry.count";
  /**
   * Mechanism that should be used when doing retry. Possible value are
   * Standard -- use the basic wakeup mechanism (GCM/APNS)
   * Enhanced -- use something like SMS ? for retry
   */
  public static final String RETRY_MECHANISM = "mmx.retry.mechanism";

  /**
   * Key to get the number of minutes after which a message is marked as timed out.
   */
  public static final String MESSAGE_TIMEOUT_MINUTES = "mmx.timeout.period.minutes";

  @Deprecated
  public static final String PUSH_CALLBACK_URL = "mmx.push.callbackurl";

  /**
   * Duration in seconds for which push callback url token is valid.
   */
  public static final String PUSH_CALLBACK_TOKEN_TTL = "push.token.ttl";
  /**
   * User name that should be used for sending message from the console.
   */
  public static final String CONSOLE_MESSAGE_SEND_USER = "mmx.console.messagesender";

  /**
   * Server domain name that should be used for construction the TO and From
   * addresses when delivering messages triggered from the console.
   */
  public static final String SERVER_DOMAIN_NAME = "mmx.domain.name";
  public static final String MAX_DEVICES_PER_APP = "mmx.cluster.max.devices.per.app";
  public static final String MAX_DEVICES_PER_USER = "mmx.cluster.max.devices.per.user";
  public static final String MAX_APP_PER_OWNER = "mmx.cluster.max.apps";
  public static final String MAX_XMPP_RATE = "mmx.instance.max.xmpp.rate.per.sec";
  public static final String MAX_HTTP_RATE = "mmx.instance.max.http.rate.per.sec";
  public static final String ALERT_EMAIL_SUBJECT = "mmx.alert.email.subject";
  public static final String ALERT_EMAIL_HOST="mmx.alert.email.host";
  public static final String ALERT_EMAIL_PORT="mmx.alert.email.port";
  public static final String ALERT_EMAIL_USER="mmx.alert.email.user";
  public static final String ALERT_EMAIL_PASSWORD="mmx.alert.email.password";
  public static final String ALERT_EMAIL_BCC_LIST = "mmx.alert.email.bcc.list";
  public static final String ALERT_EMAIL_ENABLED = "mmx.alert.email.enabled";

  public static final String ALERT_INTER_EMAIL_TIME_MINUTES = "mmx.alert.inter.email.time.minutes";

  /**
   * Keys related to APNS Connection pool.
   */
  public static final String APNS_POOL_MAX_TOTAL_CONNECTIONS = "mmx.apns.pool.max.connections";
  public static final String APNS_POOL_MAX_CONNECTIONS_PER_APP = "mmx.apns.pool.max.app.connections";
  public static final String APNS_POOL_MAX_IDLE_CONNECTIONS_PER_APP = "mmx.apns.pool.max.idle.count";
  public static final String APNS_POOL_IDLE_TTL_MINUTES = "mmx.apns.pool.idle.ttl.min";

  /**
   * Keys related to https for the rest API
   */
  public static final String REST_ENABLE_HTTPS = "mmx.rest.enable.https";
  public static final String REST_HTTP_PORT = "mmx.rest.http.port";
  public static final String REST_HTTPS_PORT = "mmx.rest.https.port";

  /**
   * Keys related to admin api server
   */
  public static final String ADMIN_API_PORT = "mmx.admin.api.port";
  public static final String ADMIN_API_ENABLE_HTTPS = "mmx.admin.api.enable.https";
  public static final String ADMIN_API_HTTPS_PORT = "mmx.admin.api.https.port";
  /**
   * Key related to admin security mode
   */
  public static final String ADMIN_REST_API_ACCESS_CONTROL_MODE = "mmx.admin.api.access.control.mode";
  public static final String ADMIN_API_HTTPS_CERT_FILE = "mmx.admin.api.https.certfile";
  public static final String ADMIN_API_HTTPS_CERT_PASSWORD = "mmx.admin.api.https.certpwd";

  /**
   * XMPP keys to expose via config servlet
   */
  public static final String XMPP_CLIENT_TLS_POLICY = "xmpp.client.tls.policy";
  public static final String DATABASE_URL = "mmx.db.url";
  public static final String DATABASE_USER = "mmx.db.user";

  /**
   * Host name to be used for call back URL construction
   */
  public static final String PUSH_CALLBACK_HOST = "mmx.push.callback.host";
  /**
   * Protocol to be used for call back URL construction. Choices are http or https
   */
  public static final String PUSH_CALLBACK_PROTOCOL = "mmx.push.callback.protocol";

  /**
   * Port to be used for call back URL construction. If not specified this will default to REST_HTTP_PORT
   */
  public static final String PUSH_CALLBACK_PORT = "mmx.push.callback.port";

  public static final String MMX_VERSION = "mmx.version";


  /*
   * APNS Feedback
   */
  public static final String APNS_FEEDBACK_PROCESS_INITIAL_DELAY_MINUTES = "mmx.apns.feedback.initialwait.min";
  public static final String APNS_FEEDBACK_PROCESS_FREQUENCY_MINUTES = "mmx.apns.feedback.frequency.min";
}

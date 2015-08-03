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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MMXManagedConfiguration implements MMXManagedConfigurationMBean {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXManagedConfiguration.class);
  private MMXConfiguration configuration;

  public MMXManagedConfiguration(MMXConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public int getWakeupInitialWait() {
    int wakeupInitialWait = configuration.getInt(MMXConfigKeys.WAKEUP_INITIAL_WAIT_KEY, MMXServerConstants.DEFAULT_WAKEUP_INITIAL_WAIT);
    LOGGER.trace("getWakeupInitialWait : {}", wakeupInitialWait);
    return wakeupInitialWait;
  }

  @Override
  public void setWakeupInitialWait(int wakeupInitialWait) {
    LOGGER.trace("setWakeupInitialWait : {}", wakeupInitialWait);
    configuration.setValue(MMXConfigKeys.WAKEUP_INITIAL_WAIT_KEY, Integer.toString(wakeupInitialWait));
  }

  /**
   * Frequency in seconds at which we need to run wakeup processing.
   *
   * @return
   */
  public int getWakeupFrequencySecs() {
    Integer wakeupFrequencySecs = configuration.getInt(MMXConfigKeys.WAKEUP_FREQUENCY_KEY, MMXServerConstants.DEFAULT_WAKEUP_FREQUENCY);
    LOGGER.trace("getWakeupFrequencySecs : {}", wakeupFrequencySecs);
    return wakeupFrequencySecs;

  }

  @Override
  public void setWakeupFrequencySecs(int wakeupFrequencySecs) {
    LOGGER.trace("setWakeupFrequencySecs : {}", wakeupFrequencySecs);
    configuration.setValue(MMXConfigKeys.WAKEUP_FREQUENCY_KEY, Integer.toString(wakeupFrequencySecs));
  }

  public String getSMTPHostName() {
    String smtpHostName = configuration.getString(MMXConfigKeys.SMTP_HOSTNAME_KEY);
    LOGGER.trace("getSMTPHostName : {}", smtpHostName);
    return smtpHostName;
  }

  @Override
  public void setSMTPHostName(String smtpHostName) {
    LOGGER.trace("setSMTPHostName : {}", smtpHostName);
    configuration.setValue(MMXConfigKeys.SMTP_HOSTNAME_KEY, smtpHostName);
  }

  public int getSMTPPort() {
    String smtpPort = configuration.getString(MMXConfigKeys.SMTP_PORT_KEY);
    LOGGER.trace("getSMTPPort : {}", smtpPort);
    return Integer.parseInt(smtpPort);
  }

  @Override
  public void setSMTPPort(int smtpPort) {
    LOGGER.trace("setSMTPPort : {}", smtpPort);
    configuration.setValue(MMXConfigKeys.SMTP_PORT_KEY, Integer.toString(smtpPort));
  }

  public String getSMTPEnableTLS() {
    String smtpEnableTls = configuration.getString(MMXConfigKeys.SMTP_ENABLE_TLS_KEY);
    LOGGER.trace("getSMTPEnableTLS : {}", smtpEnableTls);
    return smtpEnableTls;
  }

  @Override
  public void setSMTPEnableTLS(String smtpEnableTLS) {
    LOGGER.trace("setSMTPEnableTLS : {}", smtpEnableTLS);
    configuration.setValue(MMXConfigKeys.SMTP_ENABLE_TLS_KEY, smtpEnableTLS);
  }

  public String getSMTPLingerDelaySecs() {
    MMXConfiguration configuration = MMXConfiguration.getConfiguration();
    String smtpLingerDelaySecs = configuration.getString(MMXConfigKeys.SMTP_LINGERDELAY_SECS_KEY);
    return smtpLingerDelaySecs;
  }

  @Override
  public void setSMTPLingerDelaySecs(String smtpLingerDelaySecs) {
    LOGGER.trace("setSMTPLingerDelaySecs : {}`", smtpLingerDelaySecs);
    configuration.setValue(MMXConfigKeys.SMTP_LINGERDELAY_SECS_KEY, smtpLingerDelaySecs);
  }

  @Override
  public String getSMTPUserDisplayName() {
    String retVal = configuration.getString(MMXConfigKeys.SMTP_USER_DISPLAY_NAME_KEY);
    LOGGER.trace("getSMTPUserDisplayName : {}", retVal);
    return retVal;
  }

  @Override
  public void setSMTPUserDisplayName(String smtpUserDisplayName) {
    LOGGER.trace("setSMTPUserDisplayName : {}", smtpUserDisplayName);
    configuration.setValue(MMXConfigKeys.SMTP_USER_DISPLAY_NAME_KEY, smtpUserDisplayName);
  }

  public String getSMTPUserEmailAddress() {
    String smtpUserEmailAddress = configuration.getString(MMXConfigKeys.SMTP_USER_EMAIL_ADDRESS_KEY);
    LOGGER.trace("getSMTPUserEmailAddress : {}", smtpUserEmailAddress);
    return smtpUserEmailAddress;
  }

  @Override
  public void setSMTPUserEmailAddress(String smtpUserEmailAddress) {
    LOGGER.trace("setSMTPUserEmailAddress : {}", smtpUserEmailAddress);
    configuration.setValue(MMXConfigKeys.SMTP_USER_EMAIL_ADDRESS_KEY, smtpUserEmailAddress);
  }

  public String getSMTPReplyEmailAddress() {
    String retVal = configuration.getString(MMXConfigKeys.SMTP_REPLY_EMAIL_ADDRESS_KEY);
    LOGGER.trace("getSMTPReplyEmailAddress : {}", retVal);
    return retVal;
  }

  @Override
  public void setSMTPReplyEmailAddress(String smtpReplyEmailAddress) {
    LOGGER.trace("setSMTPReplyEmailAddress : {}", smtpReplyEmailAddress);
    configuration.setValue(MMXConfigKeys.SMTP_REPLY_EMAIL_ADDRESS_KEY, smtpReplyEmailAddress);
  }

  @Override
  public String getPushCallbackUrl() {
    String retVal = configuration.getString(MMXConfigKeys.PUSH_CALLBACK_URL);
    LOGGER.trace("getPushCallbackUrl : {}", retVal);
    return retVal;
  }

  @Override
  public void setPushCallbackUrl(String pushCallbackUrl) {
    LOGGER.trace("setPushCallbackUrl : {}", pushCallbackUrl);
    configuration.setValue(MMXConfigKeys.PUSH_CALLBACK_URL, pushCallbackUrl);
  }

  @Override
  public void setServerDomainName(String domainName) {
    LOGGER.trace("setServerDomainName : {}", domainName);
    configuration.setValue(MMXConfigKeys.SERVER_DOMAIN_NAME, domainName);
  }

  @Override
  public String getServerDomainName() {
    String serverDomainName = configuration.getString(MMXConfigKeys.SERVER_DOMAIN_NAME);
    LOGGER.trace("getServerDomainName : {}", serverDomainName);
    return serverDomainName;
  }

  @Override
  public String getConsoleSendMessageUser() {
    String consoleMessageSenderUser = configuration.getString(MMXConfigKeys.CONSOLE_MESSAGE_SEND_USER);
    LOGGER.trace("getConsoleSendMessageUser : {}", consoleMessageSenderUser);
    return consoleMessageSenderUser;
  }

  @Override
  public void setConsoleSendMessageUser(String consoleMessageSenderUser) {
    LOGGER.trace("setConsoleSendMessageUser : {}");
    configuration.setValue(MMXConfigKeys.CONSOLE_MESSAGE_SEND_USER, consoleMessageSenderUser);
  }

  @Override
  public int getRetryIntervalMinutes() {
    int retryIntervalMinutes = configuration.getInt(MMXConfigKeys.RETRY_INTERVAL_MINUTES, MMXServerConstants.DEFAULT_RETRY_INTERVAL_MINUTES);
    LOGGER.trace("getRetryIntervalMinutes : {}", retryIntervalMinutes);
    return retryIntervalMinutes;
  }

  @Override
  public void setRetryIntervalMinutes(int retryIntervalMinutes) {
    LOGGER.trace("setRetryIntervalMinutes : {}", retryIntervalMinutes);
    configuration.setValue(MMXConfigKeys.RETRY_INTERVAL_MINUTES, Integer.toString(retryIntervalMinutes));
  }

  @Override
  public int getRetryCount() {
    int retryCount = configuration.getInt(MMXConfigKeys.RETRY_COUNT, MMXServerConstants.DEFAULT_RETRY_COUNT);
    LOGGER.trace("getRetryCount : {}", retryCount);
    return retryCount;
  }

  @Override
  public void setRetryCount(int count) {
    if (0 < count && count < 6) {
      LOGGER.trace("setRetryCount : {}", count);
      configuration.setValue(MMXConfigKeys.RETRY_COUNT, Integer.toString(count));
    } else {
      throw new IllegalArgumentException("Bad retry count value");
    }
  }

  @Override
  public int getMessageTimeoutMinutes() {
    int messageTimeoutMinutes = configuration.getInt(MMXConfigKeys.MESSAGE_TIMEOUT_MINUTES, MMXServerConstants.DEFAULT_TIMEOUT_MINUTES);
    LOGGER.trace("getMessageTimeoutMinutes : {}", messageTimeoutMinutes);
    return messageTimeoutMinutes;
  }

  @Override
  public void setMessageTimeoutMinutes(int minutes) {
    configuration.setValue(MMXConfigKeys.MESSAGE_TIMEOUT_MINUTES, Integer.toString(minutes));
    LOGGER.trace("setMessageTimeoutMinutes : {}", minutes);
  }

  @Override
  public long getClusterMaxDevicesPerApp() {
    long maxDevicesPerApp = configuration.getLong(MMXConfigKeys.MAX_DEVICES_PER_APP, -1);
    LOGGER.trace("getMaxDevicesPerApp : {}", maxDevicesPerApp);
    return maxDevicesPerApp;
  }

  @Override
  public void setClusterMaxDevicesPerApp(long maxDevicesPerApp) {
    LOGGER.trace("setMaxDevicesPerApp : {}", maxDevicesPerApp);
    configuration.setValue(MMXConfigKeys.MAX_DEVICES_PER_APP, Long.toString(maxDevicesPerApp));
  }

  @Override
  public long getClusterMaxDevicesPerUser() {
    long maxDevicesPerUser = configuration.getLong(MMXConfigKeys.MAX_DEVICES_PER_USER, -1);
    LOGGER.trace("getMaxDevicesPerUser : {}", maxDevicesPerUser);
    return maxDevicesPerUser;
  }

  @Override
  public void setClusterMaxDevicesPerUser(long maxDevicesPerUser) {
    LOGGER.trace("setMaxDevicesPerUser : {}", maxDevicesPerUser);
    configuration.setValue(MMXConfigKeys.MAX_DEVICES_PER_USER, Long.toString(maxDevicesPerUser));
  }

  @Override
  public long getClusterMaxAppsPerServer() {
    long maxAppsPerServer = configuration.getLong(MMXConfigKeys.MAX_APP_PER_OWNER, -1);
    LOGGER.trace("getMaxAppsPerServer : {}", maxAppsPerServer);
    return maxAppsPerServer;
  }

  @Override
  public void setClusterMaxAppsPerServer(long maxAppsPerServer) {
    LOGGER.trace("setMaxAppsPerServer : {}", maxAppsPerServer);
    configuration.setValue(MMXConfigKeys.MAX_APP_PER_OWNER, Long.toString(maxAppsPerServer));
  }

  @Override
  public void setInstanceMaxXmppRate(long maxXmppRate) {
    LOGGER.trace("setInstanceMaxXmppMessageRate : {}", maxXmppRate);
    configuration.setValue(MMXConfigKeys.MAX_XMPP_RATE, Long.toString(maxXmppRate));
  }

  @Override
  public long getInstanceMaxXmppRate() {
    long maxXmppRate = configuration.getLong(MMXConfigKeys.MAX_XMPP_RATE, -1);
    LOGGER.trace("getInstanceMaxXmppRate : {}", maxXmppRate);
    return maxXmppRate;
  }

  @Override
  public void setInstanceMaxHttpRate(long maxHttpRate) {
    LOGGER.trace("setInstanceMaxHttpRate : {}", maxHttpRate);
    configuration.setValue(MMXConfigKeys.MAX_HTTP_RATE, Long.toString(maxHttpRate));
  }

  @Override
  public long getInstanceMaxHttpRate() {
    long maxPushMessageRate = configuration.getLong(MMXConfigKeys.MAX_HTTP_RATE, -1);
    LOGGER.trace("getPushMaxMessageRate : {}", maxPushMessageRate);
    return maxPushMessageRate;
  }

  @Override
  public void setMmxAlertEmailHost(String mmxAlertEmailHost) {
    LOGGER.trace("setMmxAlertEmailHost : {}", mmxAlertEmailHost);
    configuration.setValue(MMXConfigKeys.ALERT_EMAIL_HOST, mmxAlertEmailHost);
  }

  @Override
  public String getMmxAlertEmailHost() {
    String mmxAlertEmailHost = configuration.getString(MMXConfigKeys.ALERT_EMAIL_HOST);
    LOGGER.trace("getMmxAlertEmailHost : {}", mmxAlertEmailHost);
    return mmxAlertEmailHost;
  }

  @Override
  public String getMmxAlertEmailPort() {
    String mmxAlertEmailPort = configuration.getString(MMXConfigKeys.ALERT_EMAIL_PORT);
    LOGGER.trace("getMmxAlertEmailPort : {}", mmxAlertEmailPort);
    return mmxAlertEmailPort;
  }

  @Override
  public void setMmxAlertEmailPort(String mmxAlertEmailPort) {
    LOGGER.trace("setMmxAlertEmailPort : {}", mmxAlertEmailPort);
    configuration.setValue(MMXConfigKeys.ALERT_EMAIL_PORT, mmxAlertEmailPort);
  }

  @Override
  public String getMmxAlertEmailUser() {
    String mmxAlertEmailUser = configuration.getString(MMXConfigKeys.ALERT_EMAIL_USER);
    LOGGER.trace("getMmxAlertEmailUser : {}", mmxAlertEmailUser);
    return mmxAlertEmailUser;
  }

  @Override
  public void setMmxAlertEmailUser(String mmxAlertEmailUser) {
    LOGGER.trace("setMmxAlertEmailUser : {}", mmxAlertEmailUser);
    configuration.setValue(MMXConfigKeys.ALERT_EMAIL_USER, mmxAlertEmailUser);
  }

  @Override
  public String getMmxAlertEmailPassword() {
    String mmxAlertEmailPassword = configuration.getString(MMXConfigKeys.ALERT_EMAIL_PASSWORD);
    LOGGER.trace("getMmxAlertEmailPassword : {}", mmxAlertEmailPassword);
    return mmxAlertEmailPassword;
  }

  @Override
  public void setMmxAlertEmailPassword(String mmxAlertEmailPassword) {
    LOGGER.trace("setMmxAlertEmailPassword : {}", mmxAlertEmailPassword);
    configuration.setValue(MMXConfigKeys.ALERT_EMAIL_PASSWORD, mmxAlertEmailPassword);
  }

  @Override
  public String getMmxAlertEmailSubject() {
    String mmxAlertEmailSubject = configuration.getString(MMXConfigKeys.ALERT_EMAIL_SUBJECT);
    LOGGER.trace("getMmxAlertEmailSubject : {}", mmxAlertEmailSubject);
    return mmxAlertEmailSubject;
  }

  @Override
  public void setMmxAlertEmailSubject(String mmxAlertEmailSubject) {
    LOGGER.trace("setMmxAlertEmailSubject : {}", mmxAlertEmailSubject);
    configuration.setValue(MMXConfigKeys.ALERT_EMAIL_SUBJECT, mmxAlertEmailSubject);
  }

  @Override
  public String getMmxAlertEmailBccList() {
    String mmxAlertEmailBccList = configuration.getString(MMXConfigKeys.ALERT_EMAIL_BCC_LIST);
    LOGGER.trace("getMmxAlertEmailBccList : {}", mmxAlertEmailBccList);
    return mmxAlertEmailBccList;
  }

  @Override
  public void setMmxAlertEmailBccList(String mmxAlertEmailBccList) {
    LOGGER.trace("setMmxAlertEmailBccList : {}", mmxAlertEmailBccList);
    configuration.setValue(MMXConfigKeys.ALERT_EMAIL_BCC_LIST, mmxAlertEmailBccList);
  }
}

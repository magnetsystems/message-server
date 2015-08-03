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
 * MBean to expose configuration properties.
 */
public interface MMXManagedConfigurationMBean {

  /**
   * Initial wait before starting wakeup processing.
   *
   * @return
   */
  public int getWakeupInitialWait();

  public void setWakeupInitialWait(int wakeupInitialWait);

  public int getWakeupFrequencySecs();

  public void setWakeupFrequencySecs(int wakeupFrequencySecs);

  public String getSMTPHostName();

  public void setSMTPHostName(String smtpHostName);

  public int getSMTPPort();

  public void setSMTPPort(int port);

  public String getSMTPEnableTLS();

  public void setSMTPEnableTLS(String enableTLS);

  public String getSMTPLingerDelaySecs();

  public void setSMTPLingerDelaySecs(String smtpLingerDelaySecs);

  public String getSMTPUserDisplayName();

  public void setSMTPUserDisplayName(String smtpUserDisplayName);

  public String getSMTPUserEmailAddress();

  public void setSMTPUserEmailAddress(String smtpUserEmailAddress);

  public String getSMTPReplyEmailAddress();

  public void setSMTPReplyEmailAddress(String smtpReplyEmailAddress);

  public String getPushCallbackUrl();

  public void setPushCallbackUrl(String pushCallbackUrl);

  public void setServerDomainName (String domainName);

  public String getServerDomainName();

  public String getConsoleSendMessageUser();

  public void setConsoleSendMessageUser(String s);

  public int getRetryIntervalMinutes();

  public void setRetryIntervalMinutes(int s);

  public int getRetryCount();

  public void setRetryCount(int count);

  public int getMessageTimeoutMinutes();

  public void setMessageTimeoutMinutes(int minutes);

  public long getClusterMaxDevicesPerApp();
  public void setClusterMaxDevicesPerApp(long maxDevicesPerApp);

  public long getClusterMaxDevicesPerUser();
  public void setClusterMaxDevicesPerUser(long maxDevicesPerUser);

  public long getClusterMaxAppsPerServer();
  public void setClusterMaxAppsPerServer(long maxAppsPerServer);

  public long getInstanceMaxXmppRate();
  public void setInstanceMaxXmppRate(long maxXmppRate);

  public long getInstanceMaxHttpRate();
  public void setInstanceMaxHttpRate(long maxHttpRate);

  public String getMmxAlertEmailHost();
  public void setMmxAlertEmailHost(String mmxAlertEmailHost);

  public String getMmxAlertEmailPort();
  public void setMmxAlertEmailPort(String mmxAlertEmailPort);

  public String getMmxAlertEmailUser();
  public void setMmxAlertEmailUser(String mmxAlertEmailUser);

  public String getMmxAlertEmailPassword();
  public void setMmxAlertEmailPassword(String mmxAlertEmailPassword);

  public String getMmxAlertEmailSubject();
  public void setMmxAlertEmailSubject(String mmxAlertEmailSubject);

  public String getMmxAlertEmailBccList();
  public void setMmxAlertEmailBccList(String mmxAlertEmailBccList);

}

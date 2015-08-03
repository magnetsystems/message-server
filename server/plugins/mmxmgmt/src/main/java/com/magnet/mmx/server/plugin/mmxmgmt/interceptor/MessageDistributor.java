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
package com.magnet.mmx.server.plugin.mmxmgmt.interceptor;

import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.List;

/**
 */
public interface MessageDistributor {

  /**
   * Take an XMPP message addressed to a particular user and distribute it to all
   * resources (devices) registered to that user.
   * Distribute shouldn't be called with a message that is already addressed to
   * a specific resource for an user.
   * @param message
   * @param context context with information about distribution
   * @return DistributionResult representing the result of the distribution
   */
  public DistributionResult distribute(Message message, DistributionContext context);


  /**
   * Interface that defines the context with information about the distribution.
   * This helps with us not having to parse that information multiple times.
   */
  public interface DistributionContext {
    /**
     * Get the bare userId for the user to which the message is targetted to
     * @return
     */
    public String getBareUserId();

    /**
     * Get the server hostname
     * @return
     */
    public String getServerHostname();

    public String getAppId();

    /**
     * Get id of the message we are processing
     * @return
     */
    public String getMessageId();

  }

  /**
   * Representation of the distribution
   */
  public interface DistributionResult {
    /**
     * Get a count of the resources to which the message was distributed to.
     * @return
     */
    public List<JID> getDistributed();

    public List<JIDDevicePair> getNotDistributed();

    /**
     * Flag to indicate the case where we attempted to distribute the message
     * but found no devices.
     * @return true if no devices found. false if devices were found.
     */
    public boolean noDevices();
  }

  public interface JIDDevicePair {

    public JID getJID();

    public DeviceEntity getDevice();

  }
}

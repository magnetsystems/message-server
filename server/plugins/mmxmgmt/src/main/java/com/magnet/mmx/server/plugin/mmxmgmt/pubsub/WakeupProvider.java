/**
 * $RCSfile: $
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2015 Magnet Systems, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.magnet.mmx.server.plugin.mmxmgmt.pubsub;

import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.PublishedItem;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXPacketExtension;

/**
 * A provider to wake up devices for a subscriber.  This Magnet enhancement
 * allows a subscriber to be waken up by a native push mechanism (e.g. GCM/APNS)
 * and to fetch the published item.
 */
public interface WakeupProvider {

  /**
   * Scope of the wake-up.
   */
  public enum Scope {
    no_wakeup,
    all_devices,
    any_devices,
  }

  /**
   * Wake up the devices using native push messaging.
   * @param userOrDev A bare JID as user or full JID as the disconnected device.
   * @param node The pub-sub node which the new items are published to.
   * @param nItems Number of items in this event
   * @param mmx The MMX stanza of the oldest published item in the item list
   */
  public void wakeup(JID userOrDev, Node node, int nItems, MMXPacketExtension mmx);
}

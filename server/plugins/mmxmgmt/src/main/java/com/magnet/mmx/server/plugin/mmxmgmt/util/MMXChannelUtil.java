/*   Copyright (c) 2016 Magnet Systems, Inc.
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

import com.magnet.mmx.util.ChannelHelper;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeAffiliate;
import org.xmpp.packet.JID;

/**
 * Channel helper class.
 */
public class MMXChannelUtil {

    public static void addUserToChannelWhiteList(String channelName, String ownerId, String appId, String inviteeUserId) {

        JID jid = XMPPServer.getInstance().createJID(JIDUtil.makeNode(inviteeUserId, appId), null, true);
        addUserToChannelWhiteList(channelName, ownerId, appId, jid);
    }
    public static void addUserToChannelWhiteList(String channelName, String ownerId, String appId, JID subJID) {

        String channel = ChannelHelper.normalizePath(channelName);
        String realChannel = ChannelHelper.makeChannel(appId, ownerId, channel);
        Node node = XMPPServer.getInstance().getPubSubModule().getNode(realChannel);
        addUserToChannelWhiteList(node, subJID);
    }
    public static void addUserToChannelWhiteList(Node node, String inviteeUserId, String appId) {

        JID subJID = XMPPServer.getInstance().createJID(JIDUtil.makeNode(inviteeUserId, appId), null, true);
        addUserToChannelWhiteList(node, subJID);
    }
    public static void addUserToChannelWhiteList(Node node, JID subJID) {

        NodeAffiliate currentRole = node.getAffiliate(subJID);
        if (currentRole != null && currentRole.getAffiliation() == NodeAffiliate.Affiliation.owner) {
            return;
        } else {
            node.addMember(subJID);
        }
    }
}

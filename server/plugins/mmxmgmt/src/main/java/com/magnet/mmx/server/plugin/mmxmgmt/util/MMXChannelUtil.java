package com.magnet.mmx.server.plugin.mmxmgmt.util;

import com.magnet.mmx.util.ChannelHelper;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeAffiliate;
import org.xmpp.packet.JID;

/**
 * Created by mmicevic on 3/30/16.
 *
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

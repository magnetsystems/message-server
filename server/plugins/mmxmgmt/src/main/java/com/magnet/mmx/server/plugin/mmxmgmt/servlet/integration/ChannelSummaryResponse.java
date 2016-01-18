package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.protocol.*;
import com.magnet.mmx.server.api.v2.ChannelResource;
import com.magnet.mmx.server.api.v2.MessageResource;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXChannelSummary;
import com.magnet.mmx.server.plugin.mmxmgmt.message.PubSubItemResult;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import org.jivesoftware.openfire.pubsub.NodeSubscription;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//
//@XmlRootElement
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(propOrder = {"channelName", "publishedItemCount", "lastPublishedTime","subscriberCount","subscribers"})
public class ChannelSummaryResponse extends MMXChannelSummary{


    private  int subscriberCount =0;
    private List<UserInfo> subscribers;
    private UserInfo owner;
    private List<ChannelResource.MMXPubSubItemChannel2> messages;



    public ChannelSummaryResponse(String userId, String channelName,UserEntity owner,
                                  int publishedItemCount,
                                  Date lastPublishedItem,
                                  int subscriberCount,
                                  List<UserInfo> subscribers,
                                  List<ChannelResource.MMXPubSubItemChannel2>messages){
        super(userId, channelName, publishedItemCount, lastPublishedItem);
        this.subscriberCount = subscriberCount;
        this.subscribers = subscribers;
        this.messages = messages;
        this.owner = new UserInfo();
        if(owner != null) {
            this.owner.setUserId(owner.getUsername());
            this.owner.setDisplayName(owner.getName());
        }
    }

    public List<ChannelResource.MMXPubSubItemChannel2> getMessages() {
        return messages;
    }

    public void setMessages(List<ChannelResource.MMXPubSubItemChannel2> messages) {
        this.messages = messages;
    }

    public int getSubscriberCount() {
        return subscriberCount;
    }

    public void setSubscriberCount(int subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    public List<UserInfo> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<UserInfo> subscribers) {
        this.subscribers = subscribers;
    }

    public UserInfo getOwner() {
        return owner;
    }

    public ChannelSummaryResponse setOwner(UserInfo owner) {
        this.owner = owner;
        return this;
    }
}

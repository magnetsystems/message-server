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
package com.magnet.mmx.server.api.v1.protocol;

import com.magnet.mmx.protocol.TopicAction;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Request object describing a channel create request.
 */
public class ChannelCreateInfo {

    private int maxItems = -1;
    private String channelName;
    private String description;
    private boolean privateChannel;
    private boolean subscriptionEnabled = true;
    private boolean subscribeOnCreate;
    private List<String> roles;
    private TopicAction.PublisherType publishPermission;
    private String pushConfigName;


    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public boolean isPrivateChannel() {
        return privateChannel;
    }

    public void setPrivateChannel(boolean privateChannel) {
        this.privateChannel = privateChannel;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSubscriptionEnabled() {
        return subscriptionEnabled;
    }

    public void setSubscriptionEnabled(boolean subscriptionEnabled) {
        this.subscriptionEnabled = subscriptionEnabled;
    }

    public boolean isSubscribeOnCreate() {
        return subscribeOnCreate;
    }

    public void setSubscribeOnCreate(boolean subscribeOnCreate) {
        this.subscribeOnCreate = subscribeOnCreate;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public TopicAction.PublisherType getPublishPermission() {
        return publishPermission;
    }

    public void setPublishPermission(TopicAction.PublisherType permission) {
        this.publishPermission = permission;
    }

    public String getPushConfigName() {
        return pushConfigName;
    }

    public void setPushConfigName(String pushConfigName) {
        this.pushConfigName = pushConfigName;
    }

    @Override
    public String toString() {
        return "ChannelInfo{" +
                "maxItems=" + maxItems +
                ", channelName='" + channelName + '\'' +
                ", description='" + description + '\'' +
                ", privateChannel='" + privateChannel + '\'' +
                ", roles='" + roles + '\'' +
                ", subscriptionEnabled=" + subscriptionEnabled +
                ", subscribeOnCreate=" + subscribeOnCreate +
                ", publishPermission=" + publishPermission +
                (StringUtils.isNotBlank(pushConfigName) ? ", pushConfigName=" + pushConfigName : "") +
                '}';
    }
}

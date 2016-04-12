package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.api.v1.protocol.ChannelCreateInfo;

import java.util.List;


public class CreateChannelRequest extends ChannelCreateInfo {

    private List<String> subscribers;
    private String userId;
    private String mmxAppId;
    private String deviceId;
    private String pushConfigName;

    public List<String> getSubscribers() {
        return subscribers;
    }

    public CreateChannelRequest setSubscribers(List<String> subscribers) {
        this.subscribers = subscribers;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public CreateChannelRequest setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getMmxAppId() {
        return mmxAppId;
    }

    public CreateChannelRequest setMmxAppId(String mmxAppId) {
        this.mmxAppId = mmxAppId;
        return this;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public CreateChannelRequest setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    @Override
    public String getPushConfigName() {
        return pushConfigName;
    }

    @Override
    public void setPushConfigName(String pushConfigName) {
        this.pushConfigName = pushConfigName;
    }
}

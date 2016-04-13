package com.magnet.mmx.server.plugin.mmxmgmt.push.config.model;

/**
 * Created by mmicevic on 4/13/16.
 *
 */
public class MMXPushSuppress {

    private int suppressId;
    private String userId;
    private String appId;
    private String channelName;

    public int getSuppressId() {
        return suppressId;
    }

    public void setSuppressId(int suppressId) {
        this.suppressId = suppressId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
}

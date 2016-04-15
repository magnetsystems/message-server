package com.magnet.mmx.server.plugin.mmxmgmt.push.config.model;

/**
 * Created by mmicevic on 4/13/16.
 *
 */
public class MMXPushSuppress {

    private int suppressId;
    private String userId;
    private String appId;
    private String channelId;
    private Long untilDate;

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

    public String getChannelId() {
        return channelId;
    }
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public Long getUntilDate() {
        return untilDate;
    }
    public void setUntilDate(Long untilDate) {
        this.untilDate = untilDate;
    }
}

package com.magnet.mmx.server.plugin.mmxmgmt.push.config.model;

import java.io.Serializable;


public class MMXPushSuppressStatus implements Serializable {

    private Integer suppressId;
    private String userId;
    private String appId;
    private String channelId;
    private Long untilDate;
    private Boolean suppressed;

    public MMXPushSuppressStatus(MMXPushSuppress pushSuppress) {
        this.userId = pushSuppress.getUserId();
        this.appId = pushSuppress.getAppId();
        this.channelId = pushSuppress.getChannelId();
        this.untilDate = pushSuppress.getUntilDate();
        suppressed = true;
    }

    public MMXPushSuppressStatus(String appId, String userId, String channelId) {
        this.userId = userId;
        this.appId = appId;
        this.channelId = channelId;
        suppressed = false;
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

    public boolean isSuppressed() {
        return suppressed;
    }

    public MMXPushSuppressStatus setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
        return this;
    }
}

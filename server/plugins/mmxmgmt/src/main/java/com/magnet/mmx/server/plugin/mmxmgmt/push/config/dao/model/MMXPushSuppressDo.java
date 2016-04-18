package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model;

import javax.persistence.*;

/**
 * Created by mmicevic on 3/31/16.
 *
 */

@Entity
@Table(name = "mmxPushSuppress")
public class MMXPushSuppressDo {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer suppressId;
    private String userId;
    private String appId;
    private String channelId;
    private Long untilDate;


    public Integer getSuppressId() {
        return suppressId;
    }
    public void setSuppressId(Integer suppressId) {
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

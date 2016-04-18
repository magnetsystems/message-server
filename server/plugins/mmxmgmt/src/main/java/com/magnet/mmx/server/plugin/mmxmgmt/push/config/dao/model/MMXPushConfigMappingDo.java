package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model;

import javax.persistence.*;

/**
 * Created by mmicevic on 3/31/16.
 *
 */

@Entity
@Table(name = "mmxPushConfigMapping")
public class MMXPushConfigMappingDo {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer mappingId;
    private Integer configId;
    private String appId;
    private String channelId;

    public Integer getMappingId() {
        return mappingId;
    }
    public void setMappingId(Integer mappingId) {
        this.mappingId = mappingId;
    }

    public Integer getConfigId() {
        return configId;
    }
    public void setConfigId(Integer configId) {
        this.configId = configId;
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
}

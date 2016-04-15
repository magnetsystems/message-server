package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushConfigMappingDo {

    private int mappingId;
    private int configId;
    private String appId;
    private String channelId;

    public int getMappingId() {
        return mappingId;
    }
    public void setMappingId(int mappingId) {
        this.mappingId = mappingId;
    }

    public int getConfigId() {
        return configId;
    }
    public void setConfigId(int configId) {
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

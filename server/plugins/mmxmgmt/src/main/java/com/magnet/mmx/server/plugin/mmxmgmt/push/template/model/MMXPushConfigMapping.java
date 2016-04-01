package com.magnet.mmx.server.plugin.mmxmgmt.push.template.model;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushConfigMapping {

    private int mappingId;
    private int configId;
    private String appId;
    private String channelName;

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

    public String getChannelName() {
        return channelName;
    }
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
}

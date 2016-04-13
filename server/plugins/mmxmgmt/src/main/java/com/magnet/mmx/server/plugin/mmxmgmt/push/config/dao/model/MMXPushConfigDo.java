package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushConfigDo {

    private int configId;
    private String appId;
    private String configName;
    private int templateId;
    private boolean silentPush;
    private boolean enabled;

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

    public String getConfigName() {
        return configName;
    }
    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public int getTemplateId() {
        return templateId;
    }
    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public boolean isSilentPush() {
        return silentPush;
    }
    public void setSilentPush(boolean silentPush) {
        this.silentPush = silentPush;
    }

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

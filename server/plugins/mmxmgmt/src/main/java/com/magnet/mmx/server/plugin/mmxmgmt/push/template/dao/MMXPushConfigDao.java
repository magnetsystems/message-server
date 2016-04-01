package com.magnet.mmx.server.plugin.mmxmgmt.push.template.dao;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushConfigDao {

    private int configId;
    private String appId;
    private String configName;
    private int templateId;
    private boolean isSilentPush;

    private Map<String, String> meta = new HashMap<>();

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
        return isSilentPush;
    }
    public void setIsSilentPush(boolean isSilentPush) {
        this.isSilentPush = isSilentPush;
    }

    public Map<String, String> getMeta() {
        return meta;
    }
    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }
}
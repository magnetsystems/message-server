package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model;

import javax.persistence.*;

/**
 * Created by mmicevic on 3/31/16.
 *
 */

@Entity
@Table(name = "mmxPushConfig")
public class MMXPushConfigDo {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer configId;
    private String appId;
    private String configName;
    private Integer templateId;
    @Column(name = "isSilentPush")
    private boolean silentPush;
    @Column(name = "isEnabled")
    private boolean enabled;

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

    public String getConfigName() {
        return configName;
    }
    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public Integer getTemplateId() {
        return templateId;
    }
    public void setTemplateId(Integer templateId) {
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

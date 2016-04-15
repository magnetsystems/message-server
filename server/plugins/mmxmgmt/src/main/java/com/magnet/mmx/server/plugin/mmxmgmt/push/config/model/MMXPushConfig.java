/*   Copyright (c) 2016 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.server.plugin.mmxmgmt.push.config.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushConfig {

    private int configId;
    private String appId;
    private String configName;
    private MMXTemplate template;
    private boolean silentPush;
    private boolean enabled;
    private Map<String, String> meta = new HashMap<>();
    private Set<String> channelIds = new HashSet<>();

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

    public MMXTemplate getTemplate() {
        return template;
    }
    public void setTemplate(MMXTemplate template) {
        this.template = template;
    }

    public boolean isSilentPush() {
        return silentPush;
    }
    public void setSilentPush(boolean isSilentPush) {
        this.silentPush = isSilentPush;
    }

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getMeta() {
        return meta;
    }
    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    public Set<String> getChannelIds() {
        return channelIds;
    }
    public void setChannelIds(Set<String> channelIds) {
        this.channelIds = channelIds;
    }
}

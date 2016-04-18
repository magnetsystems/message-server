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

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushConfigMapping {

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

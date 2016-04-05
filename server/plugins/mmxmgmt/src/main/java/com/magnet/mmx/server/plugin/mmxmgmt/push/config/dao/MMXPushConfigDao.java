package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public interface MMXPushConfigDao {

//    public MMXPushConfigDo createConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta);
    public MMXPushConfigDo createConfig(MMXPushConfigDo config);
    public MMXPushConfigDo getConfig(int configId);
    public MMXPushConfigDo getConfig(String appId, String configName);
    public MMXPushConfigDo updateConfig(MMXPushConfigDo config);
    public void deleteConfig(MMXPushConfigDo config);
}

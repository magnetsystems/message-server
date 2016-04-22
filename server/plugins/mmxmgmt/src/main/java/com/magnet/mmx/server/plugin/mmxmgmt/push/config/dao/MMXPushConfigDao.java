package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;

import java.util.Collection;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public interface MMXPushConfigDao {

//    public MMXPushConfigDo createConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta);
    public void createConfig(MMXPushConfigDo config);
    public MMXPushConfigDo getConfig(Integer configId);
    public MMXPushConfigDo getConfig(String appId, String configName);
    public Collection<MMXPushConfigDo> getAllConfigs(String appId);
    public void updateConfig(MMXPushConfigDo config);
    public void deleteConfig(MMXPushConfigDo config);
}

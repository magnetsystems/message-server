package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;

import java.util.Collection;

/**
 * Created by mmicevic on 4/4/16.
 *
 */
public class MMXPushConfigDaoMock implements MMXPushConfigDao {

    @Override
    public void createConfig(MMXPushConfigDo config) {
        MMXPushConfigMockStorage.createConfig(config);
    }

    @Override
    public MMXPushConfigDo getConfig(Integer configId) {
        return MMXPushConfigMockStorage.getConfig(configId);
    }

    @Override
    public MMXPushConfigDo getConfig(String appId, String configName) {
        return MMXPushConfigMockStorage.getConfig(appId, configName);
    }

    @Override
    public Collection<MMXPushConfigDo> getAllConfigs(String appId) {
        return MMXPushConfigMockStorage.getAllConfigs(appId);
    }

    @Override
    public void updateConfig(MMXPushConfigDo config) {
        MMXPushConfigMockStorage.updateConfig(config);
    }

    @Override
    public void deleteConfig(MMXPushConfigDo config) {
        MMXPushConfigMockStorage.deleteConfig(config);
    }
}

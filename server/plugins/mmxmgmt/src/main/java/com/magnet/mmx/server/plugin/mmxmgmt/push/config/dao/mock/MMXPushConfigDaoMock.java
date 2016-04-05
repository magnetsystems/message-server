package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;

/**
 * Created by mmicevic on 4/4/16.
 *
 */
public class MMXPushConfigDaoMock implements MMXPushConfigDao {

    @Override
    public MMXPushConfigDo createConfig(MMXPushConfigDo config) {
        return MMXPushConfigMockStorage.createConfig(config);
    }

    @Override
    public MMXPushConfigDo getConfig(int configId) {
        return MMXPushConfigMockStorage.getConfig(configId);
    }

    @Override
    public MMXPushConfigDo getConfig(String appId, String configName) {
        return MMXPushConfigMockStorage.getConfig(appId, configName);
    }

    @Override
    public MMXPushConfigDo updateConfig(MMXPushConfigDo config) {
        return MMXPushConfigMockStorage.updateConfig(config);
    }

    @Override
    public void deleteConfig(MMXPushConfigDo config) {
        MMXPushConfigMockStorage.deleteConfig(config);
    }
}

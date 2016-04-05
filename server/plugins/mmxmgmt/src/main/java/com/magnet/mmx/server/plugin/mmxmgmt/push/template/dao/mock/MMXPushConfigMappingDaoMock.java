package com.magnet.mmx.server.plugin.mmxmgmt.push.template.dao.mock;

import com.magnet.mmx.server.plugin.mmxmgmt.push.template.dao.MMXPushConfigMappingDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.dao.model.MMXPushConfigMappingDo;

/**
 * Created by mmicevic on 4/4/16.
 *
 */
public class MMXPushConfigMappingDaoMock implements MMXPushConfigMappingDao {
    @Override
    public MMXPushConfigMappingDo createConfigMapping(MMXPushConfigMappingDo mapping) {
        return MMXPushConfigMockStorage.createConfigMapping(mapping);
    }

    @Override
    public MMXPushConfigMappingDo getConfigMapping(int mappingId) {
        return MMXPushConfigMockStorage.getConfigMapping(mappingId);
    }

    @Override
    public MMXPushConfigMappingDo getConfigMapping(String appId, String channelName) {
        return MMXPushConfigMockStorage.getConfigMapping(appId, channelName);
    }

    @Override
    public MMXPushConfigMappingDo updateConfigMapping(MMXPushConfigMappingDo mapping) {
        return MMXPushConfigMockStorage.updateConfigMapping(mapping);
    }

    @Override
    public void deleteConfigMapping(MMXPushConfigMappingDo mapping) {
        MMXPushConfigMockStorage.deleteConfigMapping(mapping);
    }
}

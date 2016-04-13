package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushSuppressDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushSuppressDo;

import java.util.Collection;

/**
 * Created by mmicevic on 4/12/16.
 *
 */
public class MMXPushSuppressDaoMock implements MMXPushSuppressDao {

    @Override
    public MMXPushSuppressDo createSuppress(MMXPushSuppressDo suppress) {
        return MMXPushConfigMockStorage.createSuppress(suppress);
    }

    @Override
    public MMXPushSuppressDo getSuppress(int suppressId) {
        return MMXPushConfigMockStorage.getSuppress(suppressId);
    }

    @Override
    public Collection<MMXPushSuppressDo> getSuppress(String userId) {
        return MMXPushConfigMockStorage.getSuppressForUser(userId);
    }

    @Override
    public void deleteSuppress(MMXPushSuppressDo suppress) {
        MMXPushConfigMockStorage.deleteSuppress(suppress);
    }
}

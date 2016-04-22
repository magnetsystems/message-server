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
    public void suppress(MMXPushSuppressDo suppress) {
        MMXPushConfigMockStorage.suppress(suppress);
    }

    @Override
    public void unSuppress(MMXPushSuppressDo suppress) {
        MMXPushConfigMockStorage.unSuppress(suppress);
    }

    @Override
    public MMXPushSuppressDo getSuppress(Integer suppressId) {
        return MMXPushConfigMockStorage.getSuppress(suppressId);
    }

    @Override
    public Collection<MMXPushSuppressDo> getSuppress(String appId, String userId) {
        return MMXPushConfigMockStorage.getSuppressForUser(appId, userId);
    }

    @Override
    public Collection<MMXPushSuppressDo> getAllSuppress(String appId) {
        return MMXPushConfigMockStorage.getSuppressForApp(appId);
    }

    @Override
    public MMXPushSuppressDo getSuppress(String appId, String userId, String channelId) {
        Collection<MMXPushSuppressDo> list = getSuppress(appId, userId);
        if (list != null) {
            for (MMXPushSuppressDo s : list) {
                if (channelId == null && s.getChannelId() == null) {
                    return s;
                }
                if (channelId != null && channelId.equals(s.getChannelId())) {
                    return s;
                }
            }
        }
        return null;
    }

    @Override
    public void deleteSuppress(MMXPushSuppressDo suppress) {
        MMXPushConfigMockStorage.deleteSuppress(suppress);
    }
}

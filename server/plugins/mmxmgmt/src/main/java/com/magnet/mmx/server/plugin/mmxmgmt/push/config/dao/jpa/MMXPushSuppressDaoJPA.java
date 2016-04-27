package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.jpa;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushSuppressDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushSuppressDo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushSuppressDaoJPA extends JPABase<MMXPushSuppressDo> implements MMXPushSuppressDao {

    public MMXPushSuppressDaoJPA() {
        super(MMXPushSuppressDo.class);
    }

    @Override
    public void suppress(MMXPushSuppressDo suppress) {
        if (suppress.getSuppressId() != null) {
            update(suppress);
        } else {
            save(suppress);
        }
    }

    @Override
    public void unSuppress(MMXPushSuppressDo suppress) {
        deleteSuppress(suppress);

    }

    @Override
    public MMXPushSuppressDo getSuppress(Integer suppressId) {
        return findById(suppressId);
    }

    @Override
    public Collection<MMXPushSuppressDo> getSuppress(String appId, String userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        params.put("userId", userId == null ? "" : userId);
        return findManyByCriteria(params);
    }

    @Override
    public Collection<MMXPushSuppressDo> getAllSuppress(String appId) {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        return findManyByCriteria(params);
    }

    @Override
    public MMXPushSuppressDo getSuppress(String appId, String userId, String channelId) {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        params.put("userId", userId == null ? "" : userId);
        params.put("channelId", channelId == null ? "" : channelId);
        return findSingleByCriteria(params);
    }

    @Override
    public void deleteSuppress(MMXPushSuppressDo suppress) {
        delete(suppress);
    }
}

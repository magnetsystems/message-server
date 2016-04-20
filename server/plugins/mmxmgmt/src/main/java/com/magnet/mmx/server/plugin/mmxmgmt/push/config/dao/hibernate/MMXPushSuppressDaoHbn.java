package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushSuppressDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushSuppressDo;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushSuppressDaoHbn extends HibernateBase<MMXPushSuppressDo> implements MMXPushSuppressDao {

    public MMXPushSuppressDaoHbn() {
        super(MMXPushSuppressDo.class);
    }

    @Override
    public MMXPushSuppressDo suppress(MMXPushSuppressDo suppress) {
        save(suppress);
        return suppress;
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
        return findManyByCriteria(
                Restrictions.eq("appId", appId),
                Restrictions.eq("userId", userId == null ? "" : userId)
        );
    }

    @Override
    public Collection<MMXPushSuppressDo> getAllSuppress(String appId) {
        return findManyByCriteria(Restrictions.eq("appId", appId));
    }

    @Override
    public MMXPushSuppressDo getSuppress(String appId, String userId, String channelId) {
        return findSingleByCriteria(
                Restrictions.eq("appId", appId),
                Restrictions.eq("userId", userId),
                Restrictions.eq("channelId", channelId)
        );
    }

    @Override
    public void deleteSuppress(MMXPushSuppressDo suppress) {
        delete(suppress);
    }
}

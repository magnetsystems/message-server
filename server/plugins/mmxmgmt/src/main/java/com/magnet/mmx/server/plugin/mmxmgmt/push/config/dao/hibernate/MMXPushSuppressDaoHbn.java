package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushSuppressDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushSuppressDo;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushSuppressDaoHbn extends HibernateBase implements MMXPushSuppressDao {

    @Override
    public MMXPushSuppressDo suppress(MMXPushSuppressDo suppress) {
        openCurrentSessionwithTransaction();
        getCurrentSession().saveOrUpdate(suppress);
        closeCurrentSessionwithTransaction();
        return suppress;
    }

    @Override
    public void unSuppress(MMXPushSuppressDo suppress) {
        deleteSuppress(suppress);

    }

    @Override
    public MMXPushSuppressDo getSuppress(Integer suppressId) {
        openCurrentSession();
        MMXPushSuppressDo suppress = (MMXPushSuppressDo) getCurrentSession().get(MMXPushSuppressDo.class, suppressId);
        closeCurrentSession();
        return suppress;
    }

    @Override
    public Collection<MMXPushSuppressDo> getSuppress(String appId, String userId) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushSuppressDo.class);
        Collection<MMXPushSuppressDo> list = (Collection<MMXPushSuppressDo>) criteria
                .add(Restrictions.eq("appId", appId))
                .add(Restrictions.eq("userId", userId))
                .list();
        closeCurrentSession();
        return list;
    }

    @Override
    public Collection<MMXPushSuppressDo> getAllSuppress(String appId) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushSuppressDo.class);
        Collection<MMXPushSuppressDo> list = (Collection<MMXPushSuppressDo>) criteria
                .add(Restrictions.eq("appId", appId))
                .list();
        closeCurrentSession();
        return list;
    }

    @Override
    public MMXPushSuppressDo getSuppress(String appId, String userId, String channelId) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushSuppressDo.class);
        MMXPushSuppressDo result = (MMXPushSuppressDo) criteria
                .add(Restrictions.eq("appId", appId))
                .add(Restrictions.eq("userId", userId))
                .add(Restrictions.eq("channelId", channelId))
                .uniqueResult();
        closeCurrentSession();
        return result;
    }

    @Override
    public void deleteSuppress(MMXPushSuppressDo suppress) {
        openCurrentSessionwithTransaction();
        getCurrentSession().delete(suppress);
        closeCurrentSessionwithTransaction();
    }
}

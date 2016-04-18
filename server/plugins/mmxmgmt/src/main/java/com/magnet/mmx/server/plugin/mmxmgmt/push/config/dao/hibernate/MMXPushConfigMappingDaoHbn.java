package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigMappingDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMappingDo;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigMappingDaoHbn extends HibernateBase implements MMXPushConfigMappingDao {

    @Override
    public MMXPushConfigMappingDo createConfigMapping(MMXPushConfigMappingDo mapping) {
        openCurrentSessionwithTransaction();
        getCurrentSession().save(mapping);
        closeCurrentSessionwithTransaction();
        return mapping;
    }

    @Override
    public MMXPushConfigMappingDo getConfigMapping(Integer mappingId) {
        openCurrentSession();
        MMXPushConfigMappingDo mapping = (MMXPushConfigMappingDo) getCurrentSession().get(MMXPushConfigMappingDo.class, mappingId);
        closeCurrentSession();
        return mapping;
    }

    @Override
    public MMXPushConfigMappingDo getConfigMapping(String appId, String channelId) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushConfigMappingDo.class);
        MMXPushConfigMappingDo mapping = (MMXPushConfigMappingDo) criteria
                .add(Restrictions.eq("appId", appId))
                .add(Restrictions.eq("channelId", channelId))
                .uniqueResult();
        closeCurrentSession();
        return mapping;
    }

    @Override
    public Collection<MMXPushConfigMappingDo> getAllConfigMappings(String appId) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushConfigMappingDo.class);
        Collection<MMXPushConfigMappingDo> list = (Collection<MMXPushConfigMappingDo>) criteria.add(Restrictions.eq("appId", appId)).list();
        closeCurrentSession();
        return list;
    }

    @Override
    public MMXPushConfigMappingDo updateConfigMapping(MMXPushConfigMappingDo mapping) {
        openCurrentSessionwithTransaction();
        getCurrentSession().saveOrUpdate(mapping);
        closeCurrentSessionwithTransaction();
        return mapping;
    }

    @Override
    public void deleteConfigMapping(MMXPushConfigMappingDo mapping) {
        openCurrentSessionwithTransaction();
        getCurrentSession().delete(mapping);
        closeCurrentSessionwithTransaction();
    }

    @Override
    public void deleteAllMappingsForConfig(Integer configId) {
        Collection<MMXPushConfigMappingDo> toBeDeleted = getAllMappingsForConfig(configId);
        if (toBeDeleted != null) {
            for (MMXPushConfigMappingDo m : toBeDeleted) {
                deleteConfigMapping(m);
            }
        }
    }

    @Override
    public Collection<MMXPushConfigMappingDo> getAllMappingsForConfig(Integer configId) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushConfigMappingDo.class);
        Collection<MMXPushConfigMappingDo> list = (Collection<MMXPushConfigMappingDo>) criteria
                .add(Restrictions.eq("configId", configId))
                .list();
        closeCurrentSession();
        return list;
    }
}

package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigDaoHbn extends HibernateBase implements MMXPushConfigDao {


    @Override
    public MMXPushConfigDo createConfig(MMXPushConfigDo config) {

        openCurrentSessionwithTransaction();
        Integer configId = (Integer) getCurrentSession().save(config);
        closeCurrentSessionwithTransaction();
        return config;
    }

    @Override
    public MMXPushConfigDo getConfig(Integer configId) {
        openCurrentSession();
        MMXPushConfigDo config = (MMXPushConfigDo) getCurrentSession().get(MMXPushConfigDo.class, configId);
        closeCurrentSession();
        return config;
    }

    @Override
    public MMXPushConfigDo getConfig(String appId, String configName) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushConfigDo.class);
        MMXPushConfigDo config = (MMXPushConfigDo) criteria.add(Restrictions.eq("appId", appId)).add(Restrictions.eq("configName", configName)).uniqueResult();
        closeCurrentSession();
        return config;
    }

    @Override
    public Collection<MMXPushConfigDo> getAllConfigs(String appId) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushConfigDo.class);
        Collection<MMXPushConfigDo> list = (Collection<MMXPushConfigDo>) criteria.add(Restrictions.eq("appId", appId)).list();
        closeCurrentSession();
        return list;
    }

    @Override
    public MMXPushConfigDo updateConfig(MMXPushConfigDo config) {
        openCurrentSessionwithTransaction();
        getCurrentSession().saveOrUpdate(config);
        closeCurrentSessionwithTransaction();
        return config;
    }

    @Override
    public void deleteConfig(MMXPushConfigDo config) {
        openCurrentSessionwithTransaction();
        getCurrentSession().delete(config);
        closeCurrentSessionwithTransaction();
    }
}

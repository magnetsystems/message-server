package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigDaoHbn extends HibernateBase<MMXPushConfigDo> implements MMXPushConfigDao {

    public MMXPushConfigDaoHbn() {
        super(MMXPushConfigDo.class);
    }

    @Override
    public void createConfig(MMXPushConfigDo config) {
        save(config);
    }

    @Override
    public MMXPushConfigDo getConfig(Integer configId) {
        return findById(configId);
    }

    @Override
    public MMXPushConfigDo getConfig(String appId, String configName) {
        return findSingleByCriteria(Restrictions.eq("appId", appId), Restrictions.eq("configName", configName));
    }

    @Override
    public Collection<MMXPushConfigDo> getAllConfigs(String appId) {
        return findManyByCriteria(Restrictions.eq("appId", appId));
    }

    @Override
    public void updateConfig(MMXPushConfigDo config) {
        if (config.getConfigId() != null) {
            update(config);
        } else {
            save(config);
        }
    }

    @Override
    public void deleteConfig(MMXPushConfigDo config) {
        delete(config);
    }
}

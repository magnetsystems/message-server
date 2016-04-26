package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.jpa;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigDaoJPA extends JPABase<MMXPushConfigDo> implements MMXPushConfigDao {

    public MMXPushConfigDaoJPA() {
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
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        params.put("configName", configName);
        return findSingleByCriteria(params);
    }

    @Override
    public Collection<MMXPushConfigDo> getAllConfigs(String appId) {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        return findManyByCriteria(params);
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

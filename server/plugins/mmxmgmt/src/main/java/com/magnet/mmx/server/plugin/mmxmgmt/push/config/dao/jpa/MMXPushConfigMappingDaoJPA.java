package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.jpa;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigMappingDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMappingDo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigMappingDaoJPA extends JPABase<MMXPushConfigMappingDo> implements MMXPushConfigMappingDao {

    public MMXPushConfigMappingDaoJPA() {
        super(MMXPushConfigMappingDo.class);
    }

    @Override
    public void createConfigMapping(MMXPushConfigMappingDo mapping) {
        save(mapping);
    }

    @Override
    public MMXPushConfigMappingDo getConfigMapping(Integer mappingId) {
        return findById(mappingId);
    }

    @Override
    public MMXPushConfigMappingDo getConfigMapping(String appId, String channelId) {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        params.put("channelId", (channelId == null ? "" : channelId));
        return findSingleByCriteria(params);
    }

    @Override
    public Collection<MMXPushConfigMappingDo> getAllConfigMappings(String appId) {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        return findManyByCriteria(params);
    }

    @Override
    public void updateConfigMapping(MMXPushConfigMappingDo mapping) {
        if (mapping.getMappingId() != null) {
            update(mapping);
        } else {
            save(mapping);
        }
    }

    @Override
    public void deleteConfigMapping(MMXPushConfigMappingDo mapping) {
        delete(mapping);
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
        Map<String, Object> params = new HashMap<>();
        params.put("configId", configId);
        return findManyByCriteria(params);
    }
}

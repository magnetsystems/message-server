package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.jpa;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigMetadataDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMetadataDo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigMetadataDaoJPA extends JPABase<MMXPushConfigMetadataDo> implements MMXPushConfigMetadataDao {

    public MMXPushConfigMetadataDaoJPA() {
        super(MMXPushConfigMetadataDo.class);
    }

    @Override
    public void createMetadata(MMXPushConfigMetadataDo meta) {
        save(meta);
    }

    @Override
    public MMXPushConfigMetadataDo getMetadata(Integer metaId) {
        return findById(metaId);
    }

    @Override
    public MMXPushConfigMetadataDo getMetadata(Integer configId, String name) {
        Map<String, Object> params = new HashMap<>();
        params.put("configId", configId);
        params.put("name", name);
        return findSingleByCriteria(params);
    }

    @Override
    public void updateMetadata(MMXPushConfigMetadataDo meta) {
        if (meta.getMetadataId() != null) {
            update(meta);
        } else {
            save(meta);
        }
    }

    @Override
    public void deleteMetadata(MMXPushConfigMetadataDo meta) {
        delete(meta);
    }

    @Override
    public Collection<MMXPushConfigMetadataDo> getConfigAllMetadata(Integer configId) {
        Map<String, Object> params = new HashMap<>();
        params.put("configId", configId);
        return findManyByCriteria(params);
    }

    @Override
    public void updateConfigAllMetadata(Integer configId, Collection<MMXPushConfigMetadataDo> list) {
        deleteConfigAllMetadata(configId);
        if (list != null) {
            for (MMXPushConfigMetadataDo m : list) {
                m.setConfigId(configId);
                createMetadata(m);
            }
        }
    }

    @Override
    public void deleteConfigAllMetadata(Integer configId) {
        Collection<MMXPushConfigMetadataDo> toBeDeleted = getConfigAllMetadata(configId);
        if (toBeDeleted != null) {
            for (MMXPushConfigMetadataDo m : toBeDeleted) {
                deleteMetadata(m);
            }
        }
    }
}

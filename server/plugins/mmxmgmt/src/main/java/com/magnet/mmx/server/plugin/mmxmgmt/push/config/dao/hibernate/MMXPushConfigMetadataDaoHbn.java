package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigMetadataDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMetadataDo;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigMetadataDaoHbn extends HibernateBase<MMXPushConfigMetadataDo> implements MMXPushConfigMetadataDao {

    public MMXPushConfigMetadataDaoHbn() {
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
        return findSingleByCriteria(Restrictions.eq("configId", configId), Restrictions.eq("name", name));
    }

    @Override
    public void updateMetadata(MMXPushConfigMetadataDo meta) {
        update(meta);
    }

    @Override
    public void deleteMetadata(MMXPushConfigMetadataDo meta) {
        delete(meta);
    }

    @Override
    public Collection<MMXPushConfigMetadataDo> getConfigAllMetadata(Integer configId) {
        return findManyByCriteria(Restrictions.eq("configId", configId));
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

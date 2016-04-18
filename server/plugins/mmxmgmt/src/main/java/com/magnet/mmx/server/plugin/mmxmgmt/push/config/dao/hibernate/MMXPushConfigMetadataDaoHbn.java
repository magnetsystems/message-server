package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigMetadataDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMetadataDo;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigMetadataDaoHbn extends HibernateBase implements MMXPushConfigMetadataDao {
    @Override
    public MMXPushConfigMetadataDo createMetadata(MMXPushConfigMetadataDo meta) {
        openCurrentSessionwithTransaction();
        getCurrentSession().save(meta);
        closeCurrentSessionwithTransaction();
        return meta;
    }

    @Override
    public MMXPushConfigMetadataDo getMetadata(Integer metaId) {
        openCurrentSession();
        MMXPushConfigMetadataDo meta = (MMXPushConfigMetadataDo) getCurrentSession().get(MMXPushConfigMetadataDo.class, metaId);
        closeCurrentSession();
        return meta;
    }

    @Override
    public MMXPushConfigMetadataDo getMetadata(Integer configId, String name) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushConfigMetadataDo.class);
        MMXPushConfigMetadataDo meta = (MMXPushConfigMetadataDo) criteria
                .add(Restrictions.eq("configId", configId))
                .add(Restrictions.eq("name", name))
                .uniqueResult();
        closeCurrentSession();
        return meta;
    }

    @Override
    public MMXPushConfigMetadataDo updateMetadata(MMXPushConfigMetadataDo meta) {
        openCurrentSessionwithTransaction();
        getCurrentSession().saveOrUpdate(meta);
        closeCurrentSessionwithTransaction();
        return meta;
    }

    @Override
    public void deleteMetadata(MMXPushConfigMetadataDo meta) {
        openCurrentSessionwithTransaction();
        getCurrentSession().delete(meta);
        closeCurrentSessionwithTransaction();
    }

    @Override
    public Collection<MMXPushConfigMetadataDo> getConfigAllMetadata(Integer configId) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXPushConfigMetadataDo.class);
        Collection<MMXPushConfigMetadataDo> list = (Collection<MMXPushConfigMetadataDo>) criteria
                .add(Restrictions.eq("configId", configId))
                .list();
        closeCurrentSession();
        return list;
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

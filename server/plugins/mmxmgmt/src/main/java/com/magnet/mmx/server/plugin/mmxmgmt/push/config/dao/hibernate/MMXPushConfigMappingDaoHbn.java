package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigMappingDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMappingDo;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigMappingDaoHbn extends HibernateBase<MMXPushConfigMappingDo> implements MMXPushConfigMappingDao {

    public MMXPushConfigMappingDaoHbn() {
        super(MMXPushConfigMappingDo.class);
    }

    @Override
    public MMXPushConfigMappingDo createConfigMapping(MMXPushConfigMappingDo mapping) {
        save(mapping);
        return mapping;
    }

    @Override
    public MMXPushConfigMappingDo getConfigMapping(Integer mappingId) {
        return findById(mappingId);
    }

    @Override
    public MMXPushConfigMappingDo getConfigMapping(String appId, String channelId) {
        return findSingleByCriteria(Restrictions.eq("appId", appId), Restrictions.eq("channelId", (channelId == null ? "" : channelId)));
    }

    @Override
    public Collection<MMXPushConfigMappingDo> getAllConfigMappings(String appId) {
        return findManyByCriteria(Restrictions.eq("appId", appId));
    }

    @Override
    public MMXPushConfigMappingDo updateConfigMapping(MMXPushConfigMappingDo mapping) {
        update(mapping);
        return mapping;
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
        return findManyByCriteria(Restrictions.eq("configId", configId));
    }
}

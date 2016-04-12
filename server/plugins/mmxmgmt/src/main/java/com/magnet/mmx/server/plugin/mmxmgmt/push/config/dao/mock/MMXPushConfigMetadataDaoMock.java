package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigMetadataDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMetadataDo;

import java.util.Collection;

/**
 * Created by mmicevic on 4/4/16.
 *
 */
public class MMXPushConfigMetadataDaoMock implements MMXPushConfigMetadataDao {


    @Override
    public MMXPushConfigMetadataDo createMetadata(MMXPushConfigMetadataDo meta) {
        return MMXPushConfigMockStorage.createConfigMetadata(meta);
    }

    @Override
    public MMXPushConfigMetadataDo getMetadata(int metaId) {
        return MMXPushConfigMockStorage.getConfigMetadata(metaId);
    }

    @Override
    public MMXPushConfigMetadataDo getMetadata(int configId, String name) {
        return MMXPushConfigMockStorage.getConfigMetadata(configId, name);
    }

    @Override
    public MMXPushConfigMetadataDo updateMetadata(MMXPushConfigMetadataDo meta) {
        return MMXPushConfigMockStorage.updateConfigMetadata(meta);
    }

    @Override
    public void deleteMetadata(MMXPushConfigMetadataDo meta) {
        MMXPushConfigMockStorage.deleteConfigMetadata(meta);
    }

    @Override
    public Collection<MMXPushConfigMetadataDo> getConfigAllMetadata(int configId) {
        return MMXPushConfigMockStorage.getConfigAllMetadata(configId);
    }

    @Override
    public void updateConfigAllMetadata(int configId, Collection<MMXPushConfigMetadataDo> list) {
        MMXPushConfigMockStorage.updateConfigAllMetadata(configId, list);
    }

    @Override
    public void deleteConfigAllMetadata(int configId) {
        MMXPushConfigMockStorage.deleteConfigAllMetadata(configId);
    }
}
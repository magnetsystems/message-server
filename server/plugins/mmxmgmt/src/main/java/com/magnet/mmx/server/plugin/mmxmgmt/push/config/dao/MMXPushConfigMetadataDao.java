package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMetadataDo;

import java.util.Collection;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public interface MMXPushConfigMetadataDao {

    public MMXPushConfigMetadataDo createMetadata(MMXPushConfigMetadataDo meta);
    public MMXPushConfigMetadataDo getMetadata(Integer metaId);
    public MMXPushConfigMetadataDo getMetadata(Integer configId, String name);
    public MMXPushConfigMetadataDo updateMetadata(MMXPushConfigMetadataDo meta);
    public void deleteMetadata(MMXPushConfigMetadataDo meta);

    public Collection<MMXPushConfigMetadataDo> getConfigAllMetadata(Integer configId);
    public void updateConfigAllMetadata(Integer configId, Collection<MMXPushConfigMetadataDo> list);
    public void deleteConfigAllMetadata(Integer configId);

}

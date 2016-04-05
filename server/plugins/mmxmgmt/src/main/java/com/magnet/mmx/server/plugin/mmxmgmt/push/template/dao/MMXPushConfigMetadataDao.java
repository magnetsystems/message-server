package com.magnet.mmx.server.plugin.mmxmgmt.push.template.dao;

import com.magnet.mmx.server.plugin.mmxmgmt.push.template.dao.model.MMXPushConfigMetadataDo;

import java.util.Collection;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public interface MMXPushConfigMetadataDao {

    public MMXPushConfigMetadataDo createMetadata(MMXPushConfigMetadataDo meta);
    public MMXPushConfigMetadataDo getMetadata(int metaId);
    public MMXPushConfigMetadataDo getMetadata(int configId, String name);
    public MMXPushConfigMetadataDo updateMetadata(MMXPushConfigMetadataDo meta);
    public void deleteMetadata(MMXPushConfigMetadataDo meta);

    public Collection<MMXPushConfigMetadataDo> getConfigAllMetadata(int configId);
    public void updateConfigAllMetadata(int configId, Collection<MMXPushConfigMetadataDo> list);
    public void deleteConfigAllMetadata(int configId);

}

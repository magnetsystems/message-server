package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMappingDo;

import java.util.Collection;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public interface MMXPushConfigMappingDao {

    public MMXPushConfigMappingDo createConfigMapping(MMXPushConfigMappingDo mapping);

    public MMXPushConfigMappingDo getConfigMapping(int mappingId);
    public MMXPushConfigMappingDo getConfigMapping(String appId, String channelName);
    public Collection<MMXPushConfigMappingDo> getAllConfigMappings(String appId);

    public MMXPushConfigMappingDo updateConfigMapping(MMXPushConfigMappingDo mapping);

    public void deleteConfigMapping(MMXPushConfigMappingDo mapping);
}

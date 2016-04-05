package com.magnet.mmx.server.plugin.mmxmgmt.push.template.dao;

import com.magnet.mmx.server.plugin.mmxmgmt.push.template.dao.model.MMXPushConfigMappingDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfigMapping;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public interface MMXPushConfigMappingDao {

    public MMXPushConfigMappingDo createConfigMapping(MMXPushConfigMappingDo mapping);

    public MMXPushConfigMappingDo getConfigMapping(int mappingId);
    public MMXPushConfigMappingDo getConfigMapping(String appId, String channelName);

    public MMXPushConfigMappingDo updateConfigMapping(MMXPushConfigMappingDo mapping);

    public void deleteConfigMapping(MMXPushConfigMappingDo mapping);
}

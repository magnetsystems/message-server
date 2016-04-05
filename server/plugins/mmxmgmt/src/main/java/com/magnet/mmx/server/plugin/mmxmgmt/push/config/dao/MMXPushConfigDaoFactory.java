package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao;

/**
 * Created by mmicevic on 4/4/16.
 *
 */
public interface MMXPushConfigDaoFactory {

    public MMXTemplateDao getMMXTemplateDao();
    public MMXPushConfigDao getMMXPushConfigDao();
    public MMXPushConfigMappingDao getMMXPushConfigMappingDao();
    public MMXPushConfigMetadataDao getMXPushConfigMetadataDao();
}

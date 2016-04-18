package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.*;

/**
 * Created by mmicevic on 4/4/16.
 *
 */
public class MMXPushDaoFactoryMock implements MMXPushConfigDaoFactory {

    private static final MMXTemplateDao TEMPLATE_DAO = new MMXTemplateDaoMock();
    private static final MMXPushConfigMappingDao CONFIG_MAPPING_DAO = new MMXPushConfigMappingDaoMock();
    private static final MMXPushConfigDao CONFIG_DAO = new MMXPushConfigDaoMock();
    private static final MMXPushConfigMetadataDao METADATA_DAO = new MMXPushConfigMetadataDaoMock();
    private static final MMXPushSuppressDao SUPPRESS_DAO = new MMXPushSuppressDaoMock();

    @Override
    public MMXTemplateDao getMMXTemplateDao() {
        return TEMPLATE_DAO;
    }

    @Override
    public MMXPushConfigDao getMMXPushConfigDao() {
        return CONFIG_DAO;
    }

    @Override
    public MMXPushConfigMappingDao getMMXPushConfigMappingDao() {
        return CONFIG_MAPPING_DAO;
    }

    @Override
    public MMXPushConfigMetadataDao getMXPushConfigMetadataDao() {
        return METADATA_DAO;
    }

    @Override
    public MMXPushSuppressDao getMXPushSuppressDao() {
        return SUPPRESS_DAO;
    }
}

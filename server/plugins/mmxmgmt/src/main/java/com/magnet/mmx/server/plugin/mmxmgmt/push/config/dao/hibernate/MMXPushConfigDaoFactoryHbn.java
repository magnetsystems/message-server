package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.*;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigDaoFactoryHbn implements MMXPushConfigDaoFactory {
    @Override
    public MMXTemplateDao getMMXTemplateDao() {
        return new MMXTemplateDaoHbn();
    }

    @Override
    public MMXPushConfigDao getMMXPushConfigDao() {
        return new MMXPushConfigDaoHbn();
    }

    @Override
    public MMXPushConfigMappingDao getMMXPushConfigMappingDao() {
        return new MMXPushConfigMappingDaoHbn();
    }

    @Override
    public MMXPushConfigMetadataDao getMXPushConfigMetadataDao() {
        return new MMXPushConfigMetadataDaoHbn();
    }

    @Override
    public MMXPushSuppressDao getMXPushSuppressDao() {
        return new MMXPushSuppressDaoHbn();
    }
}

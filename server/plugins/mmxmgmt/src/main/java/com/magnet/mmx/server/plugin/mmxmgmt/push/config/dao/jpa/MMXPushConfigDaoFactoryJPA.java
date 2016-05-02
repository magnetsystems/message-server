package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.jpa;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.*;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXPushConfigDaoFactoryJPA implements MMXPushConfigDaoFactory {
    @Override
    public MMXTemplateDao getMMXTemplateDao() {
        return new MMXTemplateDaoJPA();
    }

    @Override
    public MMXPushConfigDao getMMXPushConfigDao() {
        return new MMXPushConfigDaoJPA();
    }

    @Override
    public MMXPushConfigMappingDao getMMXPushConfigMappingDao() {
        return new MMXPushConfigMappingDaoJPA();
    }

    @Override
    public MMXPushConfigMetadataDao getMXPushConfigMetadataDao() {
        return new MMXPushConfigMetadataDaoJPA();
    }

    @Override
    public MMXPushSuppressDao getMXPushSuppressDao() {
        return new MMXPushSuppressDaoJPA();
    }
}

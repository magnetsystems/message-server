package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXTemplateDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXTemplateDo;

/**
 * Created by mmicevic on 4/4/16.
 *
 */
public class MMXTemplateDaoMock implements MMXTemplateDao {

    @Override
    public MMXTemplateDo createTemplate(MMXTemplateDo template) {
        return MMXPushConfigMockStorage.createTemplate(template);
    }
    @Override
    public MMXTemplateDo getTemplate(String appId, String templateName) {
        return MMXPushConfigMockStorage.getTemplate(appId, templateName);
    }
    @Override
    public MMXTemplateDo getTemplate(int templateId) {
        return MMXPushConfigMockStorage.getTemplate(templateId);
    }
    @Override
    public MMXTemplateDo updateTemplate(MMXTemplateDo template) {
        return MMXPushConfigMockStorage.updateTemplate(template);
    }
    @Override
    public void deleteTemplate(MMXTemplateDo template) {
        MMXPushConfigMockStorage.deleteTemplate(template);
    }
}

package com.magnet.mmx.server.plugin.mmxmgmt.push.template;

import com.magnet.mmx.server.plugin.mmxmgmt.push.template.mock.MMXPushMock;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXTemplate;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXTemplateService {

    private static MMXTemplate instance = new MMXTemplate();

    public static MMXTemplate getInstance() {
        return instance;
    }

    private MMXTemplateService() {
    }

    public MMXPushConfig getPushConfig(String appId, String channelName, String templateName) {
        return null;
    }

    //TEMPLATE CRUD
    public MMXTemplate createTemplate(String appId, String templateName) {
        return MMXPushMock.createTemplate(appId, templateName);
    }
    public MMXTemplate getTemplate(String appId, String templateName) {
        return MMXPushMock.getTemplate(appId, templateName);
    }
    public MMXTemplate getTemplate(int templateId) {
        return MMXPushMock.getTemplate(templateId);
    }
    public MMXTemplate updateTemplate(String appId, String templateName, String template) {
        return MMXPushMock.updateTemplate(appId, templateName, template);
    }
    public void deleteTemplate(String appId, String templateName) {
        MMXPushMock.deleteTemplate(appId, templateName);
    }
    public void deleteTemplate(int templateId) {
        MMXPushMock.deleteTemplate(templateId);
    }

    //CONFIG CURD

}

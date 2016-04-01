package com.magnet.mmx.server.plugin.mmxmgmt.push.template;

import com.magnet.mmx.server.plugin.mmxmgmt.push.template.mock.MMXPushMock;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfigMapping;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXTemplate;

import java.util.HashMap;
import java.util.Map;

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
    public MMXTemplate createTemplate(String appId, String templateName, String template) {
        return MMXPushMock.createTemplate(appId, templateName, template);
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

    //CONFIG CRUD
    public MMXPushConfig createConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) {
        return MMXPushMock.createConfig(appId, configName, templateName, isSilentPush, meta);
    }
    public MMXPushConfig getConfig(String appId, String configName) {
        return MMXPushMock.getConfig(appId, configName);
    }
    public MMXPushConfig getConfig(int configId) {
        return MMXPushMock.getConfig(configId);
    }
    public MMXPushConfig updateConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) {
        return MMXPushMock.updateConfig(appId, configName, templateName, isSilentPush, meta);
    }
    public void deleteConfig(String appId, String configName) {
        MMXPushMock.deleteConfig(appId, configName);
    }
    public void deleteConfig(int configId) {
        MMXPushMock.deleteConfig(configId);
    }

    //CONFIG MAPPING CRUD
    public MMXPushConfigMapping createConfigMapping(int configId, String appId, String channelName) {
        return MMXPushMock.createConfigMapping(configId, appId, channelName);
    }
    public MMXPushConfigMapping getConfigMapping(int mppingId) {
        return MMXPushMock.getConfigMapping(mppingId);
    }
    public MMXPushConfigMapping updateConfigMapping(int mappingId, int configId, String appId, String channelName) {
        return MMXPushMock.updateConfigMapping(mappingId, configId, appId, channelName);
    }
    public void deleteConfigMapping(int mappingId) {
        MMXPushMock.deleteConfigMapping(mappingId);
    }
}

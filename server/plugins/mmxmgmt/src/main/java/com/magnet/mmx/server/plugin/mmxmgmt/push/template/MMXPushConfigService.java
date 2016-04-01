package com.magnet.mmx.server.plugin.mmxmgmt.push.template;

import com.magnet.mmx.server.plugin.mmxmgmt.push.template.mock.MMXPushMock;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfigMapping;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXTemplate;

import java.util.Map;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushConfigService {

    public static final String SYSTEM_APP = "system";
    public static final String DEFAULT_TEMPLATE = "default-template";
    public static final String DEFAULT_CONFIG = "default-config";

    private static MMXPushConfigService instance = new MMXPushConfigService();

    public static MMXPushConfigService getInstance() {
        return instance;
    }

    private MMXPushConfigService() {
    }

    public MMXPushConfig getPushConfig(String appId, String channelName, String configName) {
        return null;
    }

    //TEMPLATE CRUD
    public MMXTemplate createTemplate(String appId, String templateName, String template) {
        return MMXPushMock.createTemplate(appId, templateName, template);
    }
    public MMXTemplate createTemplate(MMXTemplate template) {
        return MMXPushMock.createTemplate(template);
    }
    public MMXTemplate getTemplate(String appId, String templateName) {
        return MMXPushMock.getTemplate(appId, templateName);
    }
    public MMXTemplate getTemplate(int templateId) {
        return MMXPushMock.getTemplate(templateId);
    }
    public MMXTemplate updateTemplate(MMXTemplate template) {
        return MMXPushMock.updateTemplate(template);
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
    public void deleteTemplate(MMXTemplate template) {
        MMXPushMock.deleteTemplate(template);
    }

    //CONFIG CRUD
    public MMXPushConfig createConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) {
        return MMXPushMock.createConfig(appId, configName, templateName, isSilentPush, meta);
    }
    public MMXPushConfig createConfig(MMXPushConfig config) {
        return MMXPushMock.createConfig(config);
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
    public MMXPushConfig updateConfig(MMXPushConfig config) {
        return MMXPushMock.updateConfig(config);
    }
    public void deleteConfig(String appId, String configName) {
        MMXPushMock.deleteConfig(appId, configName);
    }
    public void deleteConfig(int configId) {
        MMXPushMock.deleteConfig(configId);
    }
    public void deleteConfig(MMXPushConfig config) {
        MMXPushMock.deleteConfig(config);
    }

    //CONFIG MAPPING CRUD
    public MMXPushConfigMapping createConfigMapping(int configId, String appId, String channelName) {
        return MMXPushMock.createConfigMapping(configId, appId, channelName);
    }
    public MMXPushConfigMapping createConfigMapping(MMXPushConfigMapping mapping) {
        return MMXPushMock.createConfigMapping(mapping);
    }
    public MMXPushConfigMapping getConfigMapping(int mappingId) {
        return MMXPushMock.getConfigMapping(mappingId);
    }
    public MMXPushConfigMapping updateConfigMapping(int mappingId, int configId, String appId, String channelName) {
        return MMXPushMock.updateConfigMapping(mappingId, configId, appId, channelName);
    }
    public MMXPushConfigMapping updateConfigMapping(MMXPushConfigMapping mapping) {
        return MMXPushMock.updateConfigMapping(mapping);
    }
    public void deleteConfigMapping(int mappingId) {
        MMXPushMock.deleteConfigMapping(mappingId);
    }
    public void deleteConfigMapping(MMXPushConfigMapping mapping) {
        MMXPushMock.deleteConfigMapping(mapping);
    }
}

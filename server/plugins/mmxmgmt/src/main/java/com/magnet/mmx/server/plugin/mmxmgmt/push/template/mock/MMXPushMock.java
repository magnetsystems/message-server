package com.magnet.mmx.server.plugin.mmxmgmt.push.template.mock;


import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfigMapping;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushMock {

    private static int SEQUENCE = 0;
    private static Map<Integer, MMXTemplate> TEMPLATE_BY_ID = new HashMap<>();
    private static Map<String, MMXTemplate> TEMPLATE_BY_APP_AND_NAME = new HashMap<>();
    private static Map<Integer, MMXPushConfig> CONFIG_BY_ID = new HashMap<>();
    private static Map<String, MMXPushConfig> CONFIG_BY_APP_AND_NAME = new HashMap<>();
    private static Map<Integer, MMXPushConfigMapping> CONFIG_MAPPING_BY_ID = new HashMap<>();

    //TEMPLATE
    public static MMXTemplate createTemplate(String appId, String templateName, String template) {

        int id = SEQUENCE++;
        MMXTemplate t = new MMXTemplate();
        t.setTemplateId(id);
        t.setAppId(appId);
        t.setTemplate(template);
        t.setTemplateType("PUSH");
        t.setTemplateName(templateName);
        TEMPLATE_BY_ID.put(id, t);
        TEMPLATE_BY_APP_AND_NAME.put(getKey(appId, templateName), t);
        return t;
    }

    public static MMXTemplate getTemplate(String appId, String templateName) {
        return TEMPLATE_BY_APP_AND_NAME.get(getKey(appId, templateName));
    }

    public static MMXTemplate getTemplate(int templateId) {
        return TEMPLATE_BY_ID.get(templateId);
    }

    public static MMXTemplate updateTemplate(String appId, String templateName, String template) {
        MMXTemplate t = getTemplate(appId, templateName);
        t.setTemplate(template);
        return t;
    }

    public static void deleteTemplate(String appId, String templateName) {

        MMXTemplate t = getTemplate(appId, templateName);
        if (t != null) {
            TEMPLATE_BY_APP_AND_NAME.remove(getKey(appId, templateName));
            TEMPLATE_BY_ID.remove(t.getTemplateId());
        }
    }

    public static void deleteTemplate(int id) {

        MMXTemplate t = getTemplate(id);
        if (t != null) {
            TEMPLATE_BY_APP_AND_NAME.remove(getKey(t.getAppId(), t.getTemplateName()));
            TEMPLATE_BY_ID.remove(id);
        }
    }

    //KEY
    private static String getKey(String appId, String name) {
        return name + "@" + appId;
    }

    // CONFIG
    public static MMXPushConfig createConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) {

        int id = SEQUENCE++;
        MMXPushConfig config = new MMXPushConfig();
        config.setConfigId(id);
        config.setAppId(appId);
        config.setConfigName(configName);
        MMXTemplate template = getTemplate(appId, templateName);
        if (template != null) {
            config.setTemplate(template);
        }
        config.setIsSilentPush(isSilentPush);
        config.setMeta(meta);
        CONFIG_BY_ID.put(id, config);
        CONFIG_BY_APP_AND_NAME.put(getKey(appId, configName), config);
        return config;
    }

    public static MMXPushConfig getConfig(String appId, String configName) {
        return CONFIG_BY_APP_AND_NAME.get(getKey(appId, configName));
    }

    public static MMXPushConfig getConfig(Integer configId) {
        return CONFIG_BY_ID.get(configId);
    }

    public static MMXPushConfig updateConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) {

        MMXPushConfig config = getConfig(appId, configName);
        if (config != null) {
            config.setTemplate(getTemplate(appId, templateName));
            config.setIsSilentPush(isSilentPush);
            config.setMeta(meta);
        }
        return config;
    }

    public static void deleteConfig(int id) {

        MMXPushConfig config = getConfig(id);
        if (config != null) {
            CONFIG_BY_APP_AND_NAME.remove(getKey(config.getAppId(), config.getConfigName()));
            CONFIG_BY_ID.remove(id);
        }
    }

    public static void deleteConfig(String appId, String configName) {

        MMXPushConfig config = getConfig(appId, configName);
        if (config != null) {
            CONFIG_BY_APP_AND_NAME.remove(getKey(appId, configName));
            CONFIG_BY_ID.remove(config.getConfigId());
        }
    }

    //CONFIG MAPPING
    public static MMXPushConfigMapping createConfigMapping(int configId, String appId, String channelName) {

        int id = SEQUENCE++;
        MMXPushConfigMapping mapping = new MMXPushConfigMapping();
        mapping.setMappingId(id);
        mapping.setConfigId(configId);
        mapping.setAppId(appId);
        mapping.setChannelName(channelName);
        CONFIG_MAPPING_BY_ID.put(id, mapping);
        return mapping;
    }
    public static MMXPushConfigMapping getConfigMapping(int mappingId) {
        return CONFIG_MAPPING_BY_ID.get(mappingId);
    }
    public static MMXPushConfigMapping updateConfigMapping(int mappingId, int configId, String appId, String channelName) {

        MMXPushConfigMapping mapping = getConfigMapping(mappingId);
        if (mapping != null) {
            mapping.setConfigId(configId);
            mapping.setAppId(appId);
            mapping.setChannelName(channelName);
        }
        return mapping;
    }
    public static void deleteConfigMapping(int mappingId) {
        CONFIG_MAPPING_BY_ID.remove(mappingId);
    }
}

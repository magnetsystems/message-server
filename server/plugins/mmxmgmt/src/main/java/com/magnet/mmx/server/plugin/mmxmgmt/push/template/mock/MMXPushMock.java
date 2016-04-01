package com.magnet.mmx.server.plugin.mmxmgmt.push.template.mock;


import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfigMapping;
import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXTemplate;

import java.util.*;

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
    private static Map<String, MMXPushConfigMapping> CONFIG_MAPPING_BY_APP_AND_CHANNEL = new HashMap<>();

    private static final String SYSTEM_APP = "system";
    private static final String DEFAULT_TEMPLATE = "default-template";
    private static final String DEFAULT_CONFIG = "default-config";
    static {
        MMXTemplate t = createTemplate(SYSTEM_APP, DEFAULT_TEMPLATE, "DEFAULT TEMPLATE");
        MMXPushConfig c = new MMXPushConfig();
        c.setAppId(SYSTEM_APP);
        c.setConfigName(DEFAULT_CONFIG);
        c.setTemplate(t);
        c = createConfig(c);
        MMXPushConfigMapping m = new MMXPushConfigMapping();
        m.setAppId(SYSTEM_APP);
        m.setConfigId(c.getConfigId());
        createConfigMapping(m);
    }

    //TEMPLATE
    public static MMXTemplate createTemplate(String appId, String templateName, String template) {
        MMXTemplate t = new MMXTemplate();
        t.setAppId(appId);
        t.setTemplateName(templateName);
        t.setTemplate(template);
        t.setTemplateType("PUSH");
        return createTemplate(t);
    }
    public static MMXTemplate createTemplate(MMXTemplate template) {
        int id = SEQUENCE++;
        template.setTemplateId(id);
        TEMPLATE_BY_ID.put(id, template);
        TEMPLATE_BY_APP_AND_NAME.put(getKey(template.getAppId(), template.getTemplateName()), template);
        return template;
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
    public static MMXTemplate updateTemplate(MMXTemplate template) {
        TEMPLATE_BY_ID.put(template.getTemplateId(), template);
        TEMPLATE_BY_APP_AND_NAME.put(getKey(template.getAppId(), template.getTemplateName()), template);
        return template;
    }
    public static void deleteTemplate(String appId, String templateName) {
        deleteTemplate(getTemplate(appId, templateName));
    }
    public static void deleteTemplate(int id) {
        deleteTemplate(getTemplate(id));
    }
    public static void deleteTemplate(MMXTemplate template) {
        deleteTemplate(template.getTemplateId());
    }

    //KEY
    private static String getKey(String appId, String name) {
        return name + "@" + appId;
    }

    // CONFIG
    public static MMXPushConfig createConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) {
        MMXPushConfig config = new MMXPushConfig();
        config.setAppId(appId);
        config.setConfigName(configName);
        MMXTemplate template = getTemplate(appId, templateName);
        if (template != null) {
            config.setTemplate(template);
        }
        config.setIsSilentPush(isSilentPush);
        config.setMeta(meta);
        return createConfig(config);
    }
    public static MMXPushConfig createConfig(MMXPushConfig config) {
        int id = SEQUENCE++;
        config.setConfigId(id);
        CONFIG_BY_ID.put(id, config);
        CONFIG_BY_APP_AND_NAME.put(getKey(config.getAppId(), config.getConfigName()), config);
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
    public static MMXPushConfig updateConfig(MMXPushConfig config) {
        if (config != null) {
            CONFIG_BY_ID.put(config.getConfigId(), config);
            CONFIG_BY_APP_AND_NAME.put(getKey(config.getAppId(), config.getConfigName()), config);
        }
        return config;
    }
    public static void deleteConfig(int id) {
        deleteConfig(getConfig(id));
    }
    public static void deleteConfig(String appId, String configName) {
        deleteConfig(getConfig(appId, configName));
    }
    public static void deleteConfig(MMXPushConfig config) {
        if (config != null) {
            CONFIG_BY_APP_AND_NAME.remove(getKey(config.getAppId(), config.getConfigName()));
            CONFIG_BY_ID.remove(config.getConfigId());
        }
    }
    public static MMXPushConfig getPushConfig(String appId, String channelName, String configName) {
        MMXPushConfig config = getConfig(appId, configName);
        if (config != null) {
            return config;
        }
        MMXPushConfigMapping mapping = getConfigMapping(appId, channelName);
        if (mapping == null) {
            mapping = getConfigMapping("system", null);
        }
        return getConfig(mapping.getConfigId());
    }

    //CONFIG MAPPING
    public static MMXPushConfigMapping createConfigMapping(int configId, String appId, String channelName) {
        MMXPushConfigMapping mapping = new MMXPushConfigMapping();
        mapping.setConfigId(configId);
        mapping.setAppId(appId);
        mapping.setChannelName(channelName);
        return createConfigMapping(mapping);
    }
    public static MMXPushConfigMapping createConfigMapping(MMXPushConfigMapping mapping) {
        int id = SEQUENCE++;
        mapping.setMappingId(id);
        CONFIG_MAPPING_BY_ID.put(id, mapping);
        CONFIG_MAPPING_BY_APP_AND_CHANNEL.put(getKey(mapping.getAppId(), mapping.getChannelName()), mapping);
        return mapping;
    }
    public static MMXPushConfigMapping getConfigMapping(int mappingId) {
        return CONFIG_MAPPING_BY_ID.get(mappingId);
    }
    public static MMXPushConfigMapping getConfigMapping(String appId, String channelName) {
        return CONFIG_MAPPING_BY_APP_AND_CHANNEL.get(getKey(appId, channelName));
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
    public static MMXPushConfigMapping updateConfigMapping(MMXPushConfigMapping mapping) {
        if (mapping != null) {
            CONFIG_MAPPING_BY_ID.put(mapping.getMappingId(), mapping);
            CONFIG_MAPPING_BY_APP_AND_CHANNEL.put(getKey(mapping.getAppId(), mapping.getChannelName()), mapping);
        }
        return mapping;
    }
    public static void deleteConfigMapping(int mappingId) {
        deleteConfigMapping(getConfigMapping(mappingId));
    }
    public static void deleteConfigMapping(String appId, String channelName) {
        deleteConfigMapping(getConfigMapping(appId, channelName));
    }
    public static void deleteConfigMapping(MMXPushConfigMapping mapping) {
        if (mapping != null) {
            CONFIG_MAPPING_BY_ID.remove(mapping.getMappingId());
            CONFIG_MAPPING_BY_APP_AND_CHANNEL.remove(getKey(mapping.getAppId(), mapping.getChannelName()));
        }
    }
}

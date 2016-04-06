package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMappingDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMetadataDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXTemplateDo;

import java.util.*;

/**
 * Created by mmicevic on 4/4/16.
 *
 */
public class MMXPushConfigMockStorage {

    private static int SEQUENCE = 0;
    private static Map<Integer, MMXTemplateDo> TEMPLATE_BY_ID = new HashMap<>();
    private static Map<String, MMXTemplateDo> TEMPLATE_BY_APP_AND_NAME = new HashMap<>();
    private static Map<Integer, MMXPushConfigDo> CONFIG_BY_ID = new HashMap<>();
    private static Map<String, MMXPushConfigDo> CONFIG_BY_APP_AND_NAME = new HashMap<>();
    private static Map<Integer, MMXPushConfigMetadataDo> CONFIG_META_BY_ID = new HashMap<>();
    private static Map<String, MMXPushConfigMetadataDo> CONFIG_META_BY_CONFIG_AND_NAME = new HashMap<>();
    private static Map<Integer, MMXPushConfigMappingDo> CONFIG_MAPPING_BY_ID = new HashMap<>();
    private static Map<String, MMXPushConfigMappingDo> CONFIG_MAPPING_BY_APP_AND_CHANNEL = new HashMap<>();

    private static String getKey(String appId, String name) {
        return name + "@" + appId;
    }

    //TEMPLATE
    public static MMXTemplateDo createTemplate(MMXTemplateDo template) {
        int id = SEQUENCE++;
        template.setTemplateId(id);
        TEMPLATE_BY_ID.put(id, template);
        TEMPLATE_BY_APP_AND_NAME.put(getKey(template.getAppId(), template.getTemplateName()), template);
        return template;
    }
    public static MMXTemplateDo getTemplate(int templateId) {
        return TEMPLATE_BY_ID.get(templateId);
    }
    public static MMXTemplateDo getTemplate(String appId, String templateName) {
        return TEMPLATE_BY_APP_AND_NAME.get(getKey(appId, templateName));
    }
    public static MMXTemplateDo updateTemplate(MMXTemplateDo template) {

        //remove ald template from index
        MMXTemplateDo oldTemplate = getTemplate(template.getTemplateId());
        if (oldTemplate != null) {
            TEMPLATE_BY_APP_AND_NAME.remove(getKey(oldTemplate.getAppId(), oldTemplate.getTemplateName()));
        }
        //store new template
        TEMPLATE_BY_ID.put(template.getTemplateId(), template);
        TEMPLATE_BY_APP_AND_NAME.put(getKey(template.getAppId(), template.getTemplateName()), template);

        return template;
    }
    public static void deleteTemplate(MMXTemplateDo template) {
        TEMPLATE_BY_ID.remove(template.getTemplateId());
        TEMPLATE_BY_APP_AND_NAME.remove(getKey(template.getAppId(), template.getTemplateName()));
    }

    //CONFIG
    public static MMXPushConfigDo createConfig(MMXPushConfigDo config) {
        int id = SEQUENCE++;
        config.setConfigId(id);
        CONFIG_BY_ID.put(id, config);
        CONFIG_BY_APP_AND_NAME.put(getKey(config.getAppId(), config.getConfigName()), config);
        return config;
    }
    public static MMXPushConfigDo getConfig(int configId) {
        MMXPushConfigDo c = CONFIG_BY_ID.get(configId);
        return c;
    }
    public static MMXPushConfigDo getConfig(String appId, String configName) {
        return CONFIG_BY_APP_AND_NAME.get(getKey(appId, configName));
    }
    public static MMXPushConfigDo updateConfig(MMXPushConfigDo config) {

        //remove old config from index
        MMXPushConfigDo oldConfig = getConfig(config.getConfigId());
        if (oldConfig != null) {
            CONFIG_BY_APP_AND_NAME.remove(getKey(oldConfig.getAppId(), oldConfig.getConfigName()));
        }

        CONFIG_BY_ID.put(config.getConfigId(), config);
        CONFIG_BY_APP_AND_NAME.put(getKey(config.getAppId(), config.getConfigName()), config);
        return config;
    }
    public static void deleteConfig(MMXPushConfigDo config) {
        CONFIG_BY_ID.remove(config.getConfigId());
        CONFIG_BY_APP_AND_NAME.remove(getKey(config.getAppId(), config.getConfigName()));
    }

    //CONFIG META
    public static MMXPushConfigMetadataDo createConfigMetadata(MMXPushConfigMetadataDo meta) {
        int id = SEQUENCE++;
        meta.setMetadataId(id);
        CONFIG_META_BY_ID.put(id, meta);
        CONFIG_META_BY_CONFIG_AND_NAME.put(getKey("" + meta.getConfigId(), meta.getName()), meta);
        return meta;
    }
    public static MMXPushConfigMetadataDo getConfigMetadata(int metaId) {
        return CONFIG_META_BY_ID.get(metaId);
    }
    public static MMXPushConfigMetadataDo getConfigMetadata(int configId, String name) {
        return CONFIG_META_BY_CONFIG_AND_NAME.get(getKey("" + configId, name));
    }
    public static Collection<MMXPushConfigMetadataDo> getConfigAllMetadata(int configId) {
        List<MMXPushConfigMetadataDo> list = new ArrayList<>();
        for(Integer key: CONFIG_META_BY_ID.keySet()) {
            MMXPushConfigMetadataDo meta =  CONFIG_META_BY_ID.get(key);
            if (configId == meta.getConfigId()) {
                list.add(meta);
            }
        }
        return list;
    }
    public static void updateConfigAllMetadata(int configId, Collection<MMXPushConfigMetadataDo> list) {

        deleteConfigAllMetadata(configId);
        if (list != null) {
            for (MMXPushConfigMetadataDo meta : list) {
                if (configId == meta.getConfigId()) {
                    createConfigMetadata(meta);
                }
            }
        }
    }
    public static MMXPushConfigMetadataDo updateConfigMetadata(MMXPushConfigMetadataDo meta) {
        CONFIG_META_BY_ID.put(meta.getMetadataId(), meta);
        CONFIG_META_BY_CONFIG_AND_NAME.put(getKey("" + meta.getConfigId(), meta.getName()), meta);
        return meta;
    }
    public static void deleteConfigAllMetadata(int configId) {
        for(Integer key: CONFIG_META_BY_ID.keySet()) {
            MMXPushConfigMetadataDo meta =  CONFIG_META_BY_ID.get(key);
            if (configId == meta.getConfigId()) {
                deleteConfigMetadata(meta);
            }
        }
    }
    public static void deleteConfigMetadata(MMXPushConfigMetadataDo meta) {
        CONFIG_META_BY_ID.remove(meta.getMetadataId());
        CONFIG_META_BY_CONFIG_AND_NAME.remove(getKey("" + meta.getConfigId(), meta.getName()));
    }


    //CONFIG MAPPING
    public static MMXPushConfigMappingDo createConfigMapping(MMXPushConfigMappingDo mapping) {
        int id = SEQUENCE++;
        mapping.setMappingId(id);
        CONFIG_MAPPING_BY_ID.put(id, mapping);
        CONFIG_MAPPING_BY_APP_AND_CHANNEL.put(getKey(mapping.getAppId(), mapping.getChannelName()), mapping);
        return mapping;
    }
    public static MMXPushConfigMappingDo getConfigMapping(int mappingId) {
        return CONFIG_MAPPING_BY_ID.get(mappingId);
    }
    public static MMXPushConfigMappingDo getConfigMapping(String appId, String channelName) {
        return CONFIG_MAPPING_BY_APP_AND_CHANNEL.get(getKey(appId, channelName));
    }
    public static MMXPushConfigMappingDo updateConfigMapping(MMXPushConfigMappingDo mapping) {

        //remove old config from index
        MMXPushConfigMappingDo oldMapping = getConfigMapping(mapping.getMappingId());
        if (oldMapping != null) {
            CONFIG_MAPPING_BY_APP_AND_CHANNEL.remove(getKey(oldMapping.getAppId(), oldMapping.getChannelName()));
        }

        CONFIG_MAPPING_BY_ID.put(mapping.getMappingId(), mapping);
        CONFIG_MAPPING_BY_APP_AND_CHANNEL.put(getKey(mapping.getAppId(), mapping.getChannelName()), mapping);
        return mapping;
    }
    public static void deleteConfigMapping(MMXPushConfigMappingDo mapping) {
        if (mapping != null) {
            CONFIG_MAPPING_BY_ID.remove(mapping.getMappingId());
            CONFIG_MAPPING_BY_APP_AND_CHANNEL.remove(getKey(mapping.getAppId(), mapping.getChannelName()));
        }
    }
}

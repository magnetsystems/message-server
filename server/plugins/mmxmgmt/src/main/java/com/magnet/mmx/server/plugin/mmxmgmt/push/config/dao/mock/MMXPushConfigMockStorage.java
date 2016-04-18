package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by mmicevic on 4/4/16.
 *
 */
public class MMXPushConfigMockStorage {

    private static Integer SEQUENCE = 0;
    private static final Map<Integer, MMXPushSuppressDo> SUPPRESS_BY_ID = new HashMap<>();
    private static Map<Integer, MMXTemplateDo> TEMPLATE_BY_ID = new HashMap<>();
    private static Map<String, MMXTemplateDo> TEMPLATE_BY_APP_AND_NAME = new HashMap<>();
    private static Map<Integer, MMXPushConfigDo> CONFIG_BY_ID = new HashMap<>();
    private static Map<String, MMXPushConfigDo> CONFIG_BY_APP_AND_NAME = new HashMap<>();
    private static Map<Integer, MMXPushConfigMetadataDo> CONFIG_META_BY_ID = new HashMap<>();
    private static Map<String, MMXPushConfigMetadataDo> CONFIG_META_BY_CONFIG_AND_NAME = new HashMap<>();
    private static Map<Integer, MMXPushConfigMappingDo> CONFIG_MAPPING_BY_ID = new HashMap<>();
    private static Map<String, MMXPushConfigMappingDo> CONFIG_MAPPING_BY_APP_AND_CHANNEL = new HashMap<>();

    private static String normalize(String str) {
        return StringUtils.isBlank(str) ? "" : str;
    }
    private static String getKey(String appId, String name) {
        return normalize(name) + "@" + normalize(appId);
    }
    private static String getKeyWithChannel(String appId, String userId, String channelId) {
        return normalize(userId) + "@" + normalize(channelId) + "@" + normalize(appId);
    }

    //TEMPLATE
    public static MMXTemplateDo createTemplate(MMXTemplateDo template) {
        Integer id = SEQUENCE++;
        template.setTemplateId(id);
        TEMPLATE_BY_ID.put(id, template);
        TEMPLATE_BY_APP_AND_NAME.put(getKey(template.getAppId(), template.getTemplateName()), template);
        return template;
    }
    public static MMXTemplateDo getTemplate(Integer templateId) {
        return TEMPLATE_BY_ID.get(templateId);
    }
    public static MMXTemplateDo getTemplate(String appId, String templateName) {
        return TEMPLATE_BY_APP_AND_NAME.get(getKey(appId, templateName));
    }
    public static Collection<MMXTemplateDo> getAllTemplates(String appId) {

        List<MMXTemplateDo> list = new ArrayList<>();
        for (MMXTemplateDo t : TEMPLATE_BY_ID.values()) {
            if (appId.equals(t.getAppId())) {
                list.add(t);
            }
        }
        return list;
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
        Integer id = SEQUENCE++;
        config.setConfigId(id);
        CONFIG_BY_ID.put(id, config);
        CONFIG_BY_APP_AND_NAME.put(getKey(config.getAppId(), config.getConfigName()), config);
        return config;
    }
    public static MMXPushConfigDo getConfig(Integer configId) {
        MMXPushConfigDo c = CONFIG_BY_ID.get(configId);
        return c;
    }
    public static MMXPushConfigDo getConfig(String appId, String configName) {
        return CONFIG_BY_APP_AND_NAME.get(getKey(appId, configName));
    }
    public static Collection<MMXPushConfigDo> getAllConfigs(String appId) {
        List<MMXPushConfigDo> list = new ArrayList<>();
        for (MMXPushConfigDo c : CONFIG_BY_ID.values()) {
            if (appId.equals(c.getAppId())) {
                list.add(c);
            }
        }
        return list;
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
        Integer id = SEQUENCE++;
        meta.setMetadataId(id);
        CONFIG_META_BY_ID.put(id, meta);
        CONFIG_META_BY_CONFIG_AND_NAME.put(getKey("" + meta.getConfigId(), meta.getName()), meta);
        return meta;
    }
    public static MMXPushConfigMetadataDo getConfigMetadata(Integer metaId) {
        return CONFIG_META_BY_ID.get(metaId);
    }
    public static MMXPushConfigMetadataDo getConfigMetadata(Integer configId, String name) {
        return CONFIG_META_BY_CONFIG_AND_NAME.get(getKey("" + configId, name));
    }
    public static Collection<MMXPushConfigMetadataDo> getConfigAllMetadata(Integer configId) {
        List<MMXPushConfigMetadataDo> list = new ArrayList<>();
        for(Integer key: CONFIG_META_BY_ID.keySet()) {
            MMXPushConfigMetadataDo meta =  CONFIG_META_BY_ID.get(key);
            if (configId == meta.getConfigId()) {
                list.add(meta);
            }
        }
        return list;
    }
    public static void updateConfigAllMetadata(Integer configId, Collection<MMXPushConfigMetadataDo> list) {

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
    public static void deleteConfigAllMetadata(Integer configId) {

        Set<Integer> keys = new HashSet<>();
        keys.addAll(CONFIG_META_BY_ID.keySet());
        for(Integer key: keys) {
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
        Integer id = SEQUENCE++;
        mapping.setMappingId(id);
        CONFIG_MAPPING_BY_ID.put(id, mapping);
        CONFIG_MAPPING_BY_APP_AND_CHANNEL.put(getKey(mapping.getAppId(), mapping.getChannelId()), mapping);
        return mapping;
    }
    public static MMXPushConfigMappingDo getConfigMapping(Integer mappingId) {
        return CONFIG_MAPPING_BY_ID.get(mappingId);
    }
    public static MMXPushConfigMappingDo getConfigMapping(String appId, String channelId) {
        return CONFIG_MAPPING_BY_APP_AND_CHANNEL.get(getKey(appId, channelId));
    }
    public static Collection<MMXPushConfigMappingDo> getAllMappingsForConfig(String appId) {
        List<MMXPushConfigMappingDo> list = new ArrayList<>();
        for (MMXPushConfigMappingDo c : CONFIG_MAPPING_BY_ID.values()) {
            if (appId.equals(c.getAppId())) {
                list.add(c);
            }
        }
        return list;
    }
    public static Collection<MMXPushConfigMappingDo> getAllMappingsForConfig(Integer configId) {
        List<MMXPushConfigMappingDo> list = new ArrayList<>();
        for (MMXPushConfigMappingDo c : CONFIG_MAPPING_BY_ID.values()) {
            if (configId == c.getConfigId()) {
                list.add(c);
            }
        }
        return list;
    }
    public static MMXPushConfigMappingDo updateConfigMapping(MMXPushConfigMappingDo mapping) {

        //remove old config from index
        MMXPushConfigMappingDo oldMapping = getConfigMapping(mapping.getMappingId());
        if (oldMapping != null) {
            CONFIG_MAPPING_BY_APP_AND_CHANNEL.remove(getKey(oldMapping.getAppId(), oldMapping.getChannelId()));
        }

        CONFIG_MAPPING_BY_ID.put(mapping.getMappingId(), mapping);
        CONFIG_MAPPING_BY_APP_AND_CHANNEL.put(getKey(mapping.getAppId(), mapping.getChannelId()), mapping);
        return mapping;
    }
    public static void deleteConfigMapping(MMXPushConfigMappingDo mapping) {
        if (mapping != null) {
            CONFIG_MAPPING_BY_ID.remove(mapping.getMappingId());
            CONFIG_MAPPING_BY_APP_AND_CHANNEL.remove(getKey(mapping.getAppId(), mapping.getChannelId()));
        }
    }

    public static void deleteAllMappingsForConfig(Integer configId) {
        Collection<MMXPushConfigMappingDo> mappings = getAllMappingsForConfig(configId);
        if (mappings != null) {
            for (MMXPushConfigMappingDo mapping : mappings) {
                deleteConfigMapping(mapping);
            }
        }
    }


    ///////////// SUPPRESS
    public static MMXPushSuppressDo suppress(MMXPushSuppressDo suppress) {
        Integer id = SEQUENCE++;
        suppress.setSuppressId(id);
        SUPPRESS_BY_ID.put(id, suppress);
        return suppress;
    }
    public static void unSuppress(MMXPushSuppressDo suppress) {
        deleteSuppress(findSuppress(suppress.getUserId(), suppress.getAppId(), suppress.getChannelId()));
    }
    public static MMXPushSuppressDo findSuppress(String userId, String appId, String channelId) {
        for (MMXPushSuppressDo s : SUPPRESS_BY_ID.values()) {
            if (compareStrings(s.getAppId(), appId) && compareStrings(s.getUserId(), userId) && compareStrings(s.getChannelId(), channelId)) {
                return s;
            }
        }
        return null;
    }
    public static MMXPushSuppressDo getSuppress(Integer suppressId) {
        return SUPPRESS_BY_ID.get(suppressId);
    }
    private static boolean compareStrings(String str1, String str2) {
        return (StringUtils.isBlank(str1) && StringUtils.isBlank(str2)) || (str1 != null && str1.equals(str2));
    }
    public static Collection<MMXPushSuppressDo> getSuppressForUser(String appId, String userId) {
        List<MMXPushSuppressDo> list = new ArrayList<>();
        for (MMXPushSuppressDo s : SUPPRESS_BY_ID.values()) {
            if (compareStrings(s.getAppId(), appId) &&  compareStrings(s.getUserId(), userId)) {
                list.add(s);
            }
        }
        return list;
    }
    public static Collection<MMXPushSuppressDo> getSuppressForApp(String appId) {
        List<MMXPushSuppressDo> list = new ArrayList<>();
        for (MMXPushSuppressDo s : SUPPRESS_BY_ID.values()) {
            if (compareStrings(s.getAppId(), appId)) {
                list.add(s);
            }
        }
        return list;
    }
    public static void deleteSuppress(MMXPushSuppressDo suppress) {
        if (suppress != null) {
            SUPPRESS_BY_ID.remove(suppress.getSuppressId());
        }
    }
}

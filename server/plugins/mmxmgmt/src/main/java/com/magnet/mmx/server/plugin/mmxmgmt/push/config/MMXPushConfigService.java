/*   Copyright (c) 2016 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigDaoFactory;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate.MMXPushConfigDaoFactoryHbn;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock.MMXPushDaoFactoryMock;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.*;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.*;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushConfigService {

    public static final String SYSTEM_APP = "system";
    public static final String DEFAULT_TEMPLATE = "default-template";
    public static final String DEFAULT_CONFIG = "default-config";

    public static final String TITLE = "";
    public static final String BODY = "${msg.from}: ${msg.content.message[0..*30]}...";
    public static final String PUSH_CONFIG_TEMPLATE =
        MMXConfigKeys.PUBSUB_NOTIFICATION_TYPE+"=push\n"+
        MMXConfigKeys.PUBSUB_NOTIFICATION_TITLE+'='+TITLE+'\n'+
        MMXConfigKeys.PUBSUB_NOTIFICATION_BODY+'='+BODY+'\n'+
        MMXConfigKeys.PUBSUB_NOTIFICATION_SOUND+"=default\n";

    private static MMXPushConfigService instance = new MMXPushConfigService();

    public static MMXPushConfigService getInstance() {
        return instance;
    }

    private final MMXPushConfigDaoFactory daoFactory = new MMXPushDaoFactoryMock();
//    private final MMXPushConfigDaoFactory daoFactory = new MMXPushConfigDaoFactoryHbn();

    private MMXPushConfigService() {

        try {
            MMXTemplate t = createTemplate(SYSTEM_APP, DEFAULT_TEMPLATE, MMXTemplateType.PUSH, PUSH_CONFIG_TEMPLATE);

            MMXPushConfig c = new MMXPushConfig();
            c.setAppId(SYSTEM_APP);
            c.setConfigName(DEFAULT_CONFIG);
            c.setTemplateId(t.getTemplateId());
            c.setEnabled(true);
            c = createConfig(c);

            MMXPushConfigMapping m = new MMXPushConfigMapping();
            m.setAppId(SYSTEM_APP);
            m.setConfigId(c.getConfigId());
            createConfigMapping(m);
        } catch (MMXException e) {
            e.printStackTrace();
        }
    }

    public boolean isPushSuppressedByUser(String userId, String appId, String channelId) {

        Collection<MMXPushSuppress> suppressList = getPushSuppressForAppAndUser(appId, userId);
        if (suppressList != null) {
            for (MMXPushSuppress s : suppressList) {
                if (s.getUntilDate() != null && s.getUntilDate() > 0 ) {
                    if (s.getUntilDate() < System.currentTimeMillis()) {
                        //expired
                        continue;
                    }
                }
            }
            if (s.getAppId().equals(appId)) {
                if (StringUtils.isBlank(s.getChannelId())) {
                    //blocked on app level
                    return true;
                } else if (s.getChannelId().equals(channelId)) {
                    //blocked on channel level
                    return true;
                }
            }
        }
        return false;
    }
//    private boolean isPushSuppressedByUser(String userId, String appId, String channelId) {
//
//        Collection<MMXPushSuppress> suppressList = getPushSuppressForAppAndUser(appId, userId);
//        if (suppressList != null) {
//            for (MMXPushSuppress s : suppressList) {
//                if (s.getUntilDate() != null) {
//                    if (s.getUntilDate() < System.currentTimeMillis()) {
//                        //expired
//                        continue;
//                    }
//                }
//                if (s.getAppId().equals(appId)) {
//                    if (StringUtils.isBlank(s.getChannelId())) {
//                        //blocked on app level
//                        return true;
//                    } else if (s.getChannelId().equals(channelId)) {
//                        //blocked on channel level
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }

    public MMXPushConfig getPushConfig(String userId, String appId, String channelId, String configName) {

        //check suppress on user level
        if (isPushSuppressedByUser(userId, appId, channelId)) {
            return null;
        }
        //check suppress on app level
        if (isPushSuppressedByUser(null, appId, channelId)) {
            return null;
        }

//        if (appId == null) {
//            appId = SYSTEM_APP;
//        }

        MMXPushConfig config = null;

        //try to find config for passed config name
        if (configName != null) {
            config = getEnabledConfigIgnoreException(appId, configName);
        }
        //fall down on channel level
        if (config == null && channelId != null) {
                config = getEnabledConfigIgnoreException(getConfigMappingIgnoreException(appId, channelId));
        }
        //if cannot find mapping for channel - try to find mapping for app
        if (config == null && appId != null) {
            config = getEnabledConfigIgnoreException(getConfigMappingIgnoreException(appId, null));
        }
        //if nothing works fall down on system level
        if (config == null) {
            config = getEnabledConfigIgnoreException(getConfigMappingIgnoreException(SYSTEM_APP, null));
        }

        return config;
    }
    private MMXPushConfig getEnabledConfigIgnoreException(MMXPushConfigMapping mapping) {

        if (mapping == null) {
            return null;
        }
        MMXPushConfig config = null;
        try {
            config = getConfig(mapping.getConfigId());
            if (!config.isEnabled()) {
                return null;
            }
        }
        catch (MMXException e) {
        }
        return config;
    }
    private MMXPushConfig getEnabledConfigIgnoreException(String appId, String configName) {

        MMXPushConfig config = null;
        try {
            config = getConfig(appId, configName);
            if (!config.isEnabled()) {
                config = null;
            }
        }
        catch (MMXException e) {
        }
        return config;
    }
    private MMXPushConfigMapping getConfigMappingIgnoreException(String appId, String channelId) {

        MMXPushConfigMapping mapping = null;
        try {
            mapping = getConfigMapping(appId, channelId);
        }
        catch (MMXException e) {
        }
        return mapping;
    }



    //TEMPLATE CRUD
    private MMXTemplate templateDo2Bo(MMXTemplateDo templateDo) {

        if (templateDo == null) {
            return null;
        }
        MMXTemplate bo = new MMXTemplate();
        bo.setTemplateId(templateDo.getTemplateId());
        bo.setAppId(templateDo.getAppId());
        bo.setTemplateName(templateDo.getTemplateName());
        bo.setTemplateType(templateDo.getTemplateType() == null ? null : MMXTemplateType.valueOf(templateDo.getTemplateType()));
        bo.setTemplate(templateDo.getTemplate());
        return bo;
    }
    private Collection<MMXTemplate> templateDo2Bo(Collection<MMXTemplateDo> allTemplates) {

        if (allTemplates == null) {
            return null;
        }
        Collection<MMXTemplate> list = new ArrayList<>();
        for (MMXTemplateDo templateDo : allTemplates) {
            list.add(templateDo2Bo(templateDo));
        }
        return list;
    }
    private MMXTemplateDo templateBo2Do(MMXTemplate bo) {

        if (bo == null) {
            return null;
        }
        MMXTemplateDo templateDo = new MMXTemplateDo();
        templateDo.setTemplateId(bo.getTemplateId());
        templateDo.setAppId(bo.getAppId());
        templateDo.setTemplateName(bo.getTemplateName());
        templateDo.setTemplateType(bo.getTemplateType() == null ? null : bo.getTemplateType().toString());
        templateDo.setTemplate(bo.getTemplate());
        return templateDo;
    }
    public MMXTemplate createTemplate(String appId, String templateName, MMXTemplateType type, String template) throws MMXException {

        MMXTemplate t = new MMXTemplate();
        t.setAppId(appId);
        t.setTemplateName(templateName);
        t.setTemplateType(type);
        t.setTemplate(template);
        return createTemplate(t);
    }
    public MMXTemplate createTemplate(MMXTemplate template) throws MMXException {
        validateTemplate(template);
        return templateDo2Bo(daoFactory.getMMXTemplateDao().createTemplate(templateBo2Do(template)));
    }
    public Collection<MMXTemplate> getAllTemplates(String appId) throws MMXException {
        validateMandatoryArgument("appId", appId);
        return templateDo2Bo(daoFactory.getMMXTemplateDao().getAllTemplates(appId));
    }


    public MMXTemplate getTemplate(String appId, String templateName) throws MMXException {
        validateMandatoryArgument("appId", appId);
        validateMandatoryArgument("templateName", templateName);
        MMXTemplateDo t = daoFactory.getMMXTemplateDao().getTemplate(appId, templateName);
        if (t == null) {
            throw new MMXException("template not found", ErrorCode.NOT_FOUND.getCode());
        }
        return templateDo2Bo(t);
    }
    public MMXTemplate getTemplate(Integer templateId) throws MMXException {
        MMXTemplateDo t = daoFactory.getMMXTemplateDao().getTemplate(templateId);
        if (t == null) {
            throw new MMXException("template not found '" + templateId + "'", ErrorCode.NOT_FOUND.getCode());
        }
        return templateDo2Bo(t);
    }
    public MMXTemplate updateTemplate(MMXTemplate template) throws MMXException {
        validateTemplate(template);
        return templateDo2Bo(daoFactory.getMMXTemplateDao().updateTemplate(templateBo2Do(template)));
    }
//    public MMXTemplate updateTemplate(String appId, String templateName, String template) throws MMXException {
//        MMXTemplate t = getTemplate(appId, templateName);
//        t.setTemplate(template);
//        return updateTemplate(t);
//    }
    public void deleteTemplate(String appId, String templateName) throws MMXException {
        deleteTemplate(getTemplate(appId, templateName));
    }
    public void deleteTemplate(Integer templateId) throws MMXException {
        deleteTemplate(getTemplate(templateId));
    }
    public void deleteTemplate(MMXTemplate template) throws MMXException {
        validateTemplate(template);
        daoFactory.getMMXTemplateDao().deleteTemplate(templateBo2Do(template));
    }
    private void validateTemplate(MMXTemplate template) throws MMXException {
        validateMandatoryObject("template", template);
        validateMandatoryArgument("template.appId", template.getAppId());
        validateMandatoryObject("template.templateType", template.getTemplateType());
        validateMandatoryArgument("template.templateName", template.getTemplateName());
        validateMandatoryArgument("template.template", template.getTemplate());
    }


    //CONFIG CRUD
    private Map<String, String> metaDo2Bo(Collection<MMXPushConfigMetadataDo> meta) {

        Map<String, String> metaBo = new HashMap<>();
        for (MMXPushConfigMetadataDo metaDo : meta) {
            metaBo.put(metaDo.getName(), metaDo.getValue());
        }
        return metaBo;
    }
    private Collection<MMXPushConfigMetadataDo> metaBo2Do(Integer configId, Map<String, String> metaBo) {

        if (metaBo == null) {
            return null;
        }
        List<MMXPushConfigMetadataDo> list = new ArrayList<>();
        for (String key : metaBo.keySet()) {
            MMXPushConfigMetadataDo metaDo = new MMXPushConfigMetadataDo();
            metaDo.setConfigId(configId);
            metaDo.setName(key);
            metaDo.setValue(metaBo.get(key));
            list.add(metaDo);
        }
        return list;
    }
    private MMXPushConfig configDo2Bo(MMXPushConfigDo configDo) throws MMXException {

        if (configDo == null) {
            return null;
        }
        MMXPushConfig bo = new MMXPushConfig();
        bo.setConfigId(configDo.getConfigId());
        bo.setAppId(configDo.getAppId());
        bo.setConfigName(configDo.getConfigName());
        bo.setSilentPush(configDo.isSilentPush());
        bo.setEnabled(configDo.isEnabled());
        bo.setEnabled(configDo.isEnabled());
        //template
        bo.setTemplateId(configDo.getTemplateId());
        Map<String, String> meta = metaDo2Bo(daoFactory.getMXPushConfigMetadataDao().getConfigAllMetadata(configDo.getConfigId()));
        bo.setMeta(meta);

        decorateConfigWithMapping(bo);

        return bo;
    }
    private void decorateConfigWithMapping(MMXPushConfig config) {

        Collection<MMXPushConfigMappingDo> mappings = daoFactory.getMMXPushConfigMappingDao().getAllMappingsForConfig(config.getConfigId());
        if (mappings != null && mappings.size() > 0) {
            Set<String> channelIds = new HashSet<>();
            for (MMXPushConfigMappingDo mapping : mappings) {
                if (StringUtils.isNotBlank(mapping.getChannelId())) {
                    channelIds.add(mapping.getChannelId());
                }
            }
            config.setChannelIds(channelIds);
        }
    }

    private Collection<MMXPushConfig> configDo2Bo(Collection<MMXPushConfigDo> allConfigs) throws MMXException {
        if (allConfigs == null) {
            return null;
        }
        List<MMXPushConfig> list = new ArrayList<>();
        for (MMXPushConfigDo configDo : allConfigs) {
            list.add(configDo2Bo(configDo));
        }
        return list;
    }
    private MMXPushConfigDo configBo2Do(MMXPushConfig bo) {

        if (bo == null) {
            return null;
        }
        MMXPushConfigDo configDo = new MMXPushConfigDo();
        configDo.setConfigId(bo.getConfigId());
        configDo.setAppId(bo.getAppId());
        configDo.setConfigName(bo.getConfigName());
        configDo.setSilentPush(bo.isSilentPush());
        configDo.setEnabled(bo.isEnabled());
        configDo.setTemplateId(bo.getTemplateId());
        return configDo;
    }


    public MMXPushConfig createConfig(MMXPushConfig config) throws MMXException {
        validateConfig(config);
        Map<String, String> meta = config.getMeta();
        MMXPushConfigDo newConf = daoFactory.getMMXPushConfigDao().createConfig(configBo2Do(config));
        config.setConfigId(newConf.getConfigId());
        //meta
        daoFactory.getMXPushConfigMetadataDao().updateConfigAllMetadata(config.getConfigId(), metaBo2Do(config.getConfigId(), meta));
        //mappings
        updateMappings(config);
        //retrieve fresh
        MMXPushConfigDo y = daoFactory.getMMXPushConfigDao().getConfig(config.getConfigId());
        MMXPushConfig x = configDo2Bo(y);
        return x;
    }
    private void updateMappings(MMXPushConfig config) {

        daoFactory.getMMXPushConfigMappingDao().deleteAllMappingsForConfig(config.getConfigId());
        if (config.getChannelIds() != null && config.getChannelIds().size() > 0) {
            for (String channelId : config.getChannelIds()) {
                MMXPushConfigMappingDo mapping = new MMXPushConfigMappingDo();
                mapping.setConfigId(config.getConfigId());
                mapping.setAppId(config.getAppId());
                mapping.setChannelId(channelId);
                daoFactory.getMMXPushConfigMappingDao().createConfigMapping(mapping);
            }
        } else {
            MMXPushConfigMappingDo mapping = new MMXPushConfigMappingDo();
            mapping.setConfigId(config.getConfigId());
            mapping.setAppId(config.getAppId());
            mapping.setChannelId(null);
            daoFactory.getMMXPushConfigMappingDao().createConfigMapping(mapping);
        }
    }

    public Collection<MMXPushConfig> getAllConfigs(String appId) throws MMXException {
        validateMandatoryArgument("appId", appId);
        Collection<MMXPushConfigDo> c = daoFactory.getMMXPushConfigDao().getAllConfigs(appId);
        return configDo2Bo(c);
    }


    public MMXPushConfig getConfig(String appId, String configName) throws MMXException {
        validateMandatoryArgument("appId", appId);
        validateMandatoryArgument("configName", configName);
        MMXPushConfigDo c = daoFactory.getMMXPushConfigDao().getConfig(appId, configName);
        if (c == null) {
            throw new MMXException("config not found", ErrorCode.NOT_FOUND.getCode());
        }
        return configDo2Bo(c);
    }
    public MMXPushConfig getConfig(Integer configId) throws MMXException {
        MMXPushConfigDo c = daoFactory.getMMXPushConfigDao().getConfig(configId);
        if (c == null) {
            throw new MMXException("config not found '" + configId + "'" , ErrorCode.NOT_FOUND.getCode());
        }
        return configDo2Bo(c);
    }
//    public MMXPushConfig updateConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) throws MMXException {
//        MMXPushConfig config = new MMXPushConfig();
//        config.setAppId(appId);
//        config.setConfigName(configName);
//        config.setTemplate(getTemplate(appId, templateName));
//        config.setIsSilentPush(isSilentPush);
//        config.setMeta(meta);
//        return updateConfig(config);
//    }
    public MMXPushConfig updateConfig(MMXPushConfig config) throws MMXException {
        validateConfig(config);
        MMXPushConfigDo configDo = configBo2Do(config);
        daoFactory.getMMXPushConfigDao().updateConfig(configDo);
        Map<String, String> meta = config.getMeta();
        Integer configId = config.getConfigId();
        daoFactory.getMXPushConfigMetadataDao().updateConfigAllMetadata(configId, metaBo2Do(configId, meta));
        updateMappings(config);
        return config;
    }
    public void deleteConfig(Integer configId) throws MMXException {
        deleteConfig(getConfig(configId));
    }
    public void deleteConfig(MMXPushConfig config) throws MMXException {
        validateConfig(config);
        Integer configId = config.getConfigId();
        daoFactory.getMXPushConfigMetadataDao().deleteConfigAllMetadata(configId);
        daoFactory.getMMXPushConfigMappingDao().deleteAllMappingsForConfig(configId);
        daoFactory.getMMXPushConfigDao().deleteConfig(configBo2Do(config));
    }
    private void validateConfig(MMXPushConfig config) throws MMXException {
        validateMandatoryObject("config", config);
        validateMandatoryArgument("config.appId", config.getAppId());
        validateMandatoryArgument("config.configName", config.getConfigName());
        validateMandatoryArgumentObject("config.templateId", config.getTemplateId());
    }


    //CONFIG MAPPING CRUD
    private MMXPushConfigMapping mappingDo2Bo(MMXPushConfigMappingDo mappingDo) {

        if (mappingDo == null) {
            return null;
        }
        MMXPushConfigMapping bo = new MMXPushConfigMapping();
        bo.setMappingId(mappingDo.getMappingId());
        bo.setAppId(mappingDo.getAppId());
        bo.setConfigId(mappingDo.getConfigId());
        bo.setChannelId(mappingDo.getChannelId());
        return bo;
    }
    private Collection<MMXPushConfigMapping> mappingDo2Bo(Collection<MMXPushConfigMappingDo> allConfigMappings) {
        if (allConfigMappings == null) {
            return null;
        }
        List<MMXPushConfigMapping> list = new ArrayList<>();
        for (MMXPushConfigMappingDo mappingDo : allConfigMappings) {
            list.add(mappingDo2Bo(mappingDo));
        }
        return list;
    }
    private MMXPushConfigMappingDo mappingBo2Do(MMXPushConfigMapping bo) {

        if (bo == null) {
            return null;
        }
        MMXPushConfigMappingDo mappingDo = new MMXPushConfigMappingDo();
        mappingDo.setMappingId(bo.getMappingId());
        mappingDo.setAppId(bo.getAppId());
        mappingDo.setConfigId(bo.getConfigId());
        mappingDo.setChannelId(bo.getChannelId());
        return mappingDo;
    }
    public MMXPushConfigMapping createConfigMapping(Integer configId, String appId, String channelId) throws MMXException {
        MMXPushConfigMapping mapping = new MMXPushConfigMapping();
        mapping.setConfigId(configId);
        mapping.setAppId(appId);
        mapping.setChannelId(channelId);
        return createConfigMapping(mapping);
    }
    public MMXPushConfigMapping createConfigMapping(MMXPushConfigMapping mapping) throws MMXException {
        validateMapping(mapping);
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().createConfigMapping(mappingBo2Do(mapping)));
    }
    public MMXPushConfigMapping getConfigMapping(Integer mappingId) throws MMXException {
        MMXPushConfigMappingDo m = daoFactory.getMMXPushConfigMappingDao().getConfigMapping(mappingId);
        if (m == null) {
            throw new MMXException("mapping not found", ErrorCode.NOT_FOUND.getCode());
        }
        return mappingDo2Bo(m);
    }
    public MMXPushConfigMapping getConfigMapping(String appId, String channelId) throws MMXException {
        validateMandatoryArgument("appId", appId);
        MMXPushConfigMappingDo m = daoFactory.getMMXPushConfigMappingDao().getConfigMapping(appId, channelId);
        if (m == null) {
            throw new MMXException("mapping not found", ErrorCode.NOT_FOUND.getCode());
        }
        return mappingDo2Bo(m);

//        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().getConfigMapping(appId, channelId));
    }
    public Collection<MMXPushConfigMapping> getAllConfigMappings(String appId) throws MMXException {
        validateMandatoryArgument("appId", appId);
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().getAllConfigMappings(appId));
    }


    //    public MMXPushConfigMapping updateConfigMapping(Integer mappingId, Integer configId, String appId, String channelId) throws MMXException {
//        MMXPushConfigMapping mapping = getConfigMapping(mappingId);
//        mapping.setConfigId(configId);
//        mapping.setAppId(appId);
//        mapping.setChannelId(channelId);
//        return updateConfigMapping(mapping);
//    }
    public MMXPushConfigMapping updateConfigMapping(MMXPushConfigMapping mapping) throws MMXException {
        validateMapping(mapping);
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().updateConfigMapping(mappingBo2Do(mapping)));
    }
    public void deleteConfigMapping(Integer mappingId) throws MMXException {
        deleteConfigMapping(getConfigMapping(mappingId));
    }
    public void deleteConfigMapping(MMXPushConfigMapping mapping) throws MMXException {
        validateMapping(mapping);
        daoFactory.getMMXPushConfigMappingDao().deleteConfigMapping(mappingBo2Do(mapping));
    }
    private void validateMapping(MMXPushConfigMapping mapping) throws MMXException {
        validateMandatoryObject("configMapping", mapping);
        validateMandatoryArgument("configMapping.appId", mapping.getAppId());
        getConfig(mapping.getConfigId());
    }

//    //
//    public void updateConfigAndMappings(MMXPushConfig config, Collection<String> channelIds) throws MMXException {
//
//        validateConfig(config);
//
//        //transaction
//        updateConfig(config);
//        deleteAllConfigMappings(config.getAppId());
//        if (channelIds == null || channelIds.size() == 0) {
//            createConfigMapping(config.getConfigId(), config.getAppId(), null);
//        } else {
//            for (String channelId : channelIds) {
//                createConfigMapping(config.getConfigId(), config.getAppId(), channelId);
//            }
//        }
//    }



    //SUPPRESS
    private static MMXPushSuppressDo suppressBo2Do(MMXPushSuppress bo) {

        MMXPushSuppressDo suppressDo = new MMXPushSuppressDo();
        suppressDo.setSuppressId(bo.getSuppressId());
        suppressDo.setUserId(bo.getUserId());
        suppressDo.setAppId(bo.getAppId());
        suppressDo.setChannelId(bo.getChannelId());
        suppressDo.setUntilDate(bo.getUntilDate());
        return suppressDo;
    }
    private static MMXPushSuppress suppressDo2Bo(MMXPushSuppressDo suppressDo) {

        if (suppressDo == null) {
            return null;
        }
        MMXPushSuppress bo = new MMXPushSuppress();
        bo.setSuppressId(suppressDo.getSuppressId());
        bo.setUserId(suppressDo.getUserId());
        bo.setAppId(suppressDo.getAppId());
        bo.setChannelId(suppressDo.getChannelId());
        bo.setUntilDate(suppressDo.getUntilDate());
        return bo;
    }
    private static Collection<MMXPushSuppress> suppressDo2Bo(Collection<MMXPushSuppressDo> listDo) {

        if (listDo == null) {
            return null;
        }
        Collection<MMXPushSuppress> listBo = new ArrayList<>();
        for (MMXPushSuppressDo suppressDo : listDo) {
            listBo.add(suppressDo2Bo(suppressDo));
        }
        return listBo;
    }
    private void validateSuppress(MMXPushSuppress suppress) throws MMXException {
        validateMandatoryObject("suppress", suppress);
        validateMandatoryArgument("suppress.appId", suppress.getAppId());
    }
    public MMXPushSuppress createPushSuppress(String userId, String appId, String channelId) throws MMXException {

        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setUserId(userId);
        suppress.setAppId(appId);
        suppress.setChannelId(channelId);
        return createPushSuppress(suppress);
    }
    public MMXPushSuppress createPushSuppress(MMXPushSuppress suppress) throws MMXException {
        validateSuppress(suppress);
        return suppressDo2Bo(daoFactory.getMXPushSuppressDao().suppress(suppressBo2Do(suppress)));
    }
    public void createPushUnSuppress(MMXPushSuppress suppress) throws MMXException {
        validateSuppress(suppress);
        daoFactory.getMXPushSuppressDao().unSuppress(suppressBo2Do(suppress));
    }
    public MMXPushSuppress getPushSuppress(Integer suppressId) throws MMXException {
        MMXPushSuppressDo s = daoFactory.getMXPushSuppressDao().getSuppress(suppressId);
        if (s == null) {
            throw new MMXException("suppress record not found", ErrorCode.NOT_FOUND.getCode());
        }
        return suppressDo2Bo(s);
    }
    public Collection<MMXPushSuppress> getAllPushSuppress(String appId) {
        Collection<MMXPushSuppressDo> listDo = daoFactory.getMXPushSuppressDao().getAllSuppress(appId);
        return suppressDo2Bo(listDo);
    }
    public Collection<MMXPushSuppress> getPushSuppressForAppAndUser(String appId, String userId) {
        Collection<MMXPushSuppressDo> listDo = daoFactory.getMXPushSuppressDao().getSuppress(appId, userId);
        return suppressDo2Bo(listDo);
    }
    public MMXPushSuppress getPushSuppressForAppUserAndChannel(String appId, String userId, String channelId) {
        MMXPushSuppressDo s = daoFactory.getMXPushSuppressDao().getSuppress(appId, userId, channelId);
        return suppressDo2Bo(s);
    }
    public void deletePushSuppress(Integer suppressId) throws MMXException {
        deletePushSuppress(getPushSuppress(suppressId));
    }
    public void deletePushSuppress(MMXPushSuppress suppress) throws MMXException {
        validateSuppress(suppress);
        daoFactory.getMXPushSuppressDao().deleteSuppress(suppressBo2Do(suppress));
    }



    //VALIDATION
    private void validateMandatoryObject(String name, Object obj) throws MMXException {
        if (obj == null) {
            throw new MMXException("Object " + name + " is null", ErrorCode.ILLEGAL_ARGUMENT.getCode());
        }
    }
    private void validateMandatoryArgument(String name, String value) throws MMXException {
        if (StringUtils.isBlank(value)) {
            throw new MMXException("Argument " + name + " is null or empty", ErrorCode.ILLEGAL_ARGUMENT.getCode());
        }
    }
    private void validateMandatoryArgumentObject(String name, Object value) throws MMXException {
        if (value == null) {
            throw new MMXException("Argument " + name + " is null or empty", ErrorCode.ILLEGAL_ARGUMENT.getCode());
        }
    }

}

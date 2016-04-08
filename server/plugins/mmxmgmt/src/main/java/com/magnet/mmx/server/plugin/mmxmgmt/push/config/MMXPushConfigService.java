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
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock.MMXPushMockDaoFactory;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMappingDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMetadataDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXTemplateDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfigMapping;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplateType;
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

    public static final String TITLE = "${channel.name}";
    public static final String BODY = "New message from ${msg.from}";
    public static final String PUSH_CONFIG_TEMPLATE =
        MMXConfigKeys.PUBSUB_NOTIFICATION_TYPE+"=push\n"+
        MMXConfigKeys.PUBSUB_NOTIFICATION_TITLE+'='+TITLE+'\n'+
        MMXConfigKeys.PUBSUB_NOTIFICATION_BODY+'='+BODY+'\n'+
        MMXConfigKeys.PUBSUB_NOTIFICATION_SOUND+"=default\n";

    private static MMXPushConfigService instance = new MMXPushConfigService();

    public static MMXPushConfigService getInstance() {
        return instance;
    }

    private final MMXPushConfigDaoFactory daoFactory = new MMXPushMockDaoFactory();

    private MMXPushConfigService() {

        try {
            MMXTemplate t = createTemplate(SYSTEM_APP, DEFAULT_TEMPLATE, MMXTemplateType.PUSH, PUSH_CONFIG_TEMPLATE);

            MMXPushConfig c = new MMXPushConfig();
            c.setAppId(SYSTEM_APP);
            c.setConfigName(DEFAULT_CONFIG);
            c.setTemplate(t);
            c = createConfig(c);

            MMXPushConfigMapping m = new MMXPushConfigMapping();
            m.setAppId(SYSTEM_APP);
            m.setConfigId(c.getConfigId());
            createConfigMapping(m);
        } catch (MMXException e) {
            e.printStackTrace();
        }
    }

    public MMXPushConfig getPushConfig(String appId, String channelName, String configName) throws MMXException {

        MMXPushConfig config = null;
        if (appId == null) {
            appId = SYSTEM_APP;
        }
        //try to find config for passed config name
        if (configName != null) {
            config = getConfig(appId, configName);
            if (config != null) {
                return config;
            }
        }

        MMXPushConfigMapping mapping = null;
        //try to find mapping for passed channelName
        if (channelName != null) {
            mapping = getConfigMapping(appId, channelName);
        } else {
            mapping = getConfigMapping(appId, null);
        }
        //if nothing works find mapping for system
        if (mapping == null) {
            mapping = getConfigMapping(SYSTEM_APP, null);
        }

        return getConfig(mapping.getConfigId());
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
    public MMXTemplate getTemplate(int templateId) throws MMXException {
        MMXTemplateDo t = daoFactory.getMMXTemplateDao().getTemplate(templateId);
        if (t == null) {
            throw new MMXException("template not found", ErrorCode.NOT_FOUND.getCode());
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
    public void deleteTemplate(int templateId) throws MMXException {
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
    private Collection<MMXPushConfigMetadataDo> metaBo2Do(int configId, Map<String, String> metaBo) {

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
        bo.setIsSilentPush(configDo.isSilentPush());
        //template
        MMXTemplate template = getTemplate(configDo.getTemplateId());
        bo.setTemplate(template);
        Map<String, String> meta = metaDo2Bo(daoFactory.getMXPushConfigMetadataDao().getConfigAllMetadata(configDo.getConfigId()));
        bo.setMeta(meta);

        return bo;
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
        configDo.setIsSilentPush(bo.isSilentPush());
        configDo.setTemplateId(bo.getTemplate().getTemplateId());
        return configDo;
    }

    public MMXPushConfig createConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) throws MMXException {
        MMXPushConfig config = new MMXPushConfig();
        config.setAppId(appId);
        config.setConfigName(configName);
        config.setIsSilentPush(isSilentPush);
        config.setTemplate(getTemplate(appId, templateName));
        config.setMeta(meta);
        return createConfig(config);
    }
    public MMXPushConfig createConfig(MMXPushConfig config) throws MMXException {
        validateConfig(config);
        Map<String, String> meta = config.getMeta();
        config = configDo2Bo(daoFactory.getMMXPushConfigDao().createConfig(configBo2Do(config)));
        daoFactory.getMXPushConfigMetadataDao().updateConfigAllMetadata(config.getConfigId(), metaBo2Do(config.getConfigId(), meta));
        //
        // attach meta to config
        config.setMeta(meta);
        //
        return config;
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
    public MMXPushConfig getConfig(int configId) throws MMXException {
        MMXPushConfigDo c = daoFactory.getMMXPushConfigDao().getConfig(configId);
        if (c == null) {
            throw new MMXException("config not found", ErrorCode.NOT_FOUND.getCode());
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
        int configId = config.getConfigId();
        daoFactory.getMXPushConfigMetadataDao().updateConfigAllMetadata(configId, metaBo2Do(configId, meta));
        return config;
    }
    public void deleteConfig(int configId) throws MMXException {
        deleteConfig(getConfig(configId));
    }
    public void deleteConfig(MMXPushConfig config) throws MMXException {
        validateConfig(config);
        int configId = config.getConfigId();
        daoFactory.getMXPushConfigMetadataDao().deleteConfigAllMetadata(configId);
        daoFactory.getMMXPushConfigDao().deleteConfig(configBo2Do(config));
    }
    private void validateConfig(MMXPushConfig config) throws MMXException {
        validateMandatoryObject("config", config);
        validateMandatoryArgument("config.appId", config.getAppId());
        validateMandatoryArgument("config.configName", config.getConfigName());
        validateMandatoryObject("config.template", config.getTemplate());
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
        bo.setChannelName(mappingDo.getChannelName());
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
        mappingDo.setChannelName(bo.getChannelName());
        return mappingDo;
    }
    public MMXPushConfigMapping createConfigMapping(int configId, String appId, String channelName) throws MMXException {
        MMXPushConfigMapping mapping = new MMXPushConfigMapping();
        mapping.setConfigId(configId);
        mapping.setAppId(appId);
        mapping.setChannelName(channelName);
        return createConfigMapping(mapping);
    }
    public MMXPushConfigMapping createConfigMapping(MMXPushConfigMapping mapping) throws MMXException {
        validateMapping(mapping);
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().createConfigMapping(mappingBo2Do(mapping)));
    }
    public MMXPushConfigMapping getConfigMapping(int mappingId) throws MMXException {
        MMXPushConfigMappingDo m = daoFactory.getMMXPushConfigMappingDao().getConfigMapping(mappingId);
        if (m == null) {
            throw new MMXException("mapping not found", ErrorCode.NOT_FOUND.getCode());
        }
        return mappingDo2Bo(m);
    }
    public MMXPushConfigMapping getConfigMapping(String appId, String channelName) throws MMXException {
        validateMandatoryArgument("appId", appId);
        MMXPushConfigMappingDo m = daoFactory.getMMXPushConfigMappingDao().getConfigMapping(appId, channelName);
        if (m == null) {
            throw new MMXException("mapping not found", ErrorCode.NOT_FOUND.getCode());
        }
        return mappingDo2Bo(m);

//        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().getConfigMapping(appId, channelName));
    }
    public Collection<MMXPushConfigMapping> getAllConfigMappings(String appId) throws MMXException {
        validateMandatoryArgument("appId", appId);
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().getAllConfigMappings(appId));
    }


    //    public MMXPushConfigMapping updateConfigMapping(int mappingId, int configId, String appId, String channelName) throws MMXException {
//        MMXPushConfigMapping mapping = getConfigMapping(mappingId);
//        mapping.setConfigId(configId);
//        mapping.setAppId(appId);
//        mapping.setChannelName(channelName);
//        return updateConfigMapping(mapping);
//    }
    public MMXPushConfigMapping updateConfigMapping(MMXPushConfigMapping mapping) throws MMXException {
        validateMapping(mapping);
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().updateConfigMapping(mappingBo2Do(mapping)));
    }
    public void deleteConfigMapping(int mappingId) throws MMXException {
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

}

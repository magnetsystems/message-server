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

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXPushConfigDaoFactory;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.mock.MMXPushMockDaoFactory;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMappingDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushConfigMetadataDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXTemplateDo;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfigMapping;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;

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
        MMXConfigKeys.PUBSUB_NOTIFICATION_SOUND+"=true\n";

    private static MMXPushConfigService instance = new MMXPushConfigService();

    public static MMXPushConfigService getInstance() {
        return instance;
    }

    private final MMXPushConfigDaoFactory daoFactory = new MMXPushMockDaoFactory();

    private MMXPushConfigService() {

        MMXTemplate t = createTemplate(SYSTEM_APP, DEFAULT_TEMPLATE, PUSH_CONFIG_TEMPLATE);

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

    public MMXPushConfig getPushConfig(String appId, String channelName, String configName) {

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



    //TEMPLATE CRUD
    private MMXTemplate templateDo2Bo(MMXTemplateDo templateDo) {

        if (templateDo == null) {
            return null;
        }
        MMXTemplate bo = new MMXTemplate();
        bo.setTemplateId(templateDo.getTemplateId());
        bo.setAppId(templateDo.getAppId());
        bo.setTemplateName(templateDo.getTemplateName());
        bo.setTemplateType(templateDo.getTemplateType());
        bo.setTemplate(templateDo.getTemplate());
        return bo;
    }
    private MMXTemplateDo templateBo2Do(MMXTemplate bo) {

        if (bo == null) {
            return null;
        }
        MMXTemplateDo templateDo = new MMXTemplateDo();
        templateDo.setTemplateId(bo.getTemplateId());
        templateDo.setAppId(bo.getAppId());
        templateDo.setTemplateName(bo.getTemplateName());
        templateDo.setTemplateType(bo.getTemplateType());
        templateDo.setTemplate(bo.getTemplate());
        return templateDo;
    }
    public MMXTemplate createTemplate(String appId, String templateName, String template) {

        MMXTemplate t = new MMXTemplate();
        t.setAppId(appId);
        t.setTemplateName(templateName);
        t.setTemplate(template);
        t.setTemplateType("PUSH");
        return createTemplate(t);
    }
    public MMXTemplate createTemplate(MMXTemplate template) {
        return templateDo2Bo(daoFactory.getMMXTemplateDao().createTemplate(templateBo2Do(template)));
    }
    public MMXTemplate getTemplate(String appId, String templateName) {
        return templateDo2Bo(daoFactory.getMMXTemplateDao().getTemplate(appId, templateName));
    }
    public MMXTemplate getTemplate(int templateId) {
        return templateDo2Bo(daoFactory.getMMXTemplateDao().getTemplate(templateId));
    }
    public MMXTemplate updateTemplate(MMXTemplate template) {
        return templateDo2Bo(daoFactory.getMMXTemplateDao().updateTemplate(templateBo2Do(template)));
    }
    public MMXTemplate updateTemplate(String appId, String templateName, String template) {
        MMXTemplate t = getTemplate(appId, templateName);
        t.setTemplate(template);
        return updateTemplate(t);
    }
    public void deleteTemplate(String appId, String templateName) {
        deleteTemplate(getTemplate(appId, templateName));
    }
    public void deleteTemplate(int templateId) {
        deleteTemplate(getTemplate(templateId));
    }
    public void deleteTemplate(MMXTemplate template) {
        daoFactory.getMMXTemplateDao().deleteTemplate(templateBo2Do(template));
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
    private MMXPushConfig configDo2Bo(MMXPushConfigDo configDo) {

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
    private MMXPushConfigDo configBo2Do(MMXPushConfig bo) {

        if (bo == null) {
            return null;
        }
        MMXPushConfigDo configDo = new MMXPushConfigDo();
        configDo.setConfigId(bo.getConfigId());
        configDo.setAppId(bo.getAppId());
        configDo.setConfigName(bo.getConfigName());
        configDo.setIsSilentPush(bo.isSilentPush());
        return configDo;
    }


    public MMXPushConfig createConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) {
        MMXPushConfig config = new MMXPushConfig();
        config.setAppId(appId);
        config.setConfigName(configName);
        config.setIsSilentPush(isSilentPush);
        config.setTemplate(getTemplate(appId, templateName));
        config.setMeta(meta);
        return createConfig(config);
    }
    public MMXPushConfig createConfig(MMXPushConfig config) {
        Map<String, String> meta = config.getMeta();
        config = configDo2Bo(daoFactory.getMMXPushConfigDao().createConfig(configBo2Do(config)));
        daoFactory.getMXPushConfigMetadataDao().updateConfigAllMetadata(config.getConfigId(), metaBo2Do(config.getConfigId(), meta));
        return config;
    }
    public MMXPushConfig getConfig(String appId, String configName) {
        return configDo2Bo(daoFactory.getMMXPushConfigDao().getConfig(appId, configName));
    }
    public MMXPushConfig getConfig(int configId) {
        return configDo2Bo(daoFactory.getMMXPushConfigDao().getConfig(configId));
    }
    public MMXPushConfig updateConfig(String appId, String configName, String templateName, boolean isSilentPush, Map<String, String> meta) {
        MMXPushConfig config = new MMXPushConfig();
        config.setAppId(appId);
        config.setConfigName(configName);
        config.setTemplate(getTemplate(appId, templateName));
        config.setIsSilentPush(isSilentPush);
        config.setMeta(meta);
        return updateConfig(config);
    }
    public MMXPushConfig updateConfig(MMXPushConfig config) {
        MMXPushConfigDo configDo = configBo2Do(config);
        daoFactory.getMMXPushConfigDao().updateConfig(configDo);
        Map<String, String> meta = config.getMeta();
        int configId = config.getConfigId();
        daoFactory.getMXPushConfigMetadataDao().updateConfigAllMetadata(configId, metaBo2Do(configId, meta));
        return config;
    }

    //TODO delete
//    public void deleteConfig(String appId, String configName) {
//        MMXPushMock.deleteConfig(appId, configName);
//    }
//    public void deleteConfig(int configId) {
//        MMXPushMock.deleteConfig(configId);
//    }
//    public void deleteConfig(MMXPushConfig config) {
//        MMXPushMock.deleteConfig(config);
//    }

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
    public MMXPushConfigMapping createConfigMapping(int configId, String appId, String channelName) {
        MMXPushConfigMapping mapping = new MMXPushConfigMapping();
        mapping.setConfigId(configId);
        mapping.setAppId(appId);
        mapping.setChannelName(channelName);
        return createConfigMapping(mapping);
    }
    public MMXPushConfigMapping createConfigMapping(MMXPushConfigMapping mapping) {
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().createConfigMapping(mappingBo2Do(mapping)));
    }
    public MMXPushConfigMapping getConfigMapping(int mappingId) {
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().getConfigMapping(mappingId));
    }
    public MMXPushConfigMapping getConfigMapping(String appId, String channelName) {
//        daoFactory.getMMXPushConfigMappingDao()
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().getConfigMapping(appId, channelName));
    }
    public MMXPushConfigMapping updateConfigMapping(int mappingId, int configId, String appId, String channelName) {
        MMXPushConfigMapping mapping = getConfigMapping(mappingId);
        mapping.setConfigId(configId);
        mapping.setAppId(appId);
        mapping.setChannelName(channelName);
        return updateConfigMapping(mapping);
    }
    public MMXPushConfigMapping updateConfigMapping(MMXPushConfigMapping mapping) {
        return mappingDo2Bo(daoFactory.getMMXPushConfigMappingDao().updateConfigMapping(mappingBo2Do(mapping)));
    }
    public void deleteConfigMapping(int mappingId) {
        deleteConfigMapping(getConfigMapping(mappingId));
    }
    public void deleteConfigMapping(MMXPushConfigMapping mapping) {
        daoFactory.getMMXPushConfigMappingDao().updateConfigMapping(mappingBo2Do(mapping));
    }
}

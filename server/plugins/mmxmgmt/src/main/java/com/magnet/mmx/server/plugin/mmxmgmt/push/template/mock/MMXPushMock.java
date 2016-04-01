package com.magnet.mmx.server.plugin.mmxmgmt.push.template.mock;


import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXPushConfig;
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
    private static Map<Integer, MMXPushConfig> CONFIG_MAP = new HashMap<>();

    public static MMXTemplate createTemplate(String appId, String templateName) {

        int id = SEQUENCE++;
        MMXTemplate template = new MMXTemplate();
        template.setTemplateId(id);
        template.setAppId(appId);
        template.setTemplate("T-" + id);
        template.setTemplateType("PUSH");
        template.setTemplateName(templateName);
        TEMPLATE_BY_ID.put(id, template);
        TEMPLATE_BY_APP_AND_NAME.put(getTemplateKey(appId, templateName), template);
        return template;
    }
    public static MMXTemplate getTemplate(String appId, String templateName) {
        return TEMPLATE_BY_APP_AND_NAME.get(getTemplateKey(appId, templateName));
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
            TEMPLATE_BY_APP_AND_NAME.remove(getTemplateKey(appId, templateName));
            TEMPLATE_BY_ID.remove(t.getTemplateId());
        }
    }
    public static void deleteTemplate(int id) {

        MMXTemplate t = getTemplate(id);
        if (t != null) {
            TEMPLATE_BY_APP_AND_NAME.remove(getTemplateKey(t.getAppId(), t.getTemplateName()));
            TEMPLATE_BY_ID.remove(id);
        }
    }

    private static String getTemplateKey(String appId, String templateName) {
        return templateName + "@" + appId;
    }

    private static MMXPushConfig createConfig(int id, String configName, String appId, int templateId, boolean is) {

        MMXPushConfig config = new MMXPushConfig();
        config.setConfigId(id);
        config.setConfigName(configName);
        config.setAppId(appId);
        config.setTemplate(TEMPLATE_BY_ID.get(templateId));
//        config.setTemplateType("PUSH");
        CONFIG_MAP.put(id, config);
        return config;
    }
}

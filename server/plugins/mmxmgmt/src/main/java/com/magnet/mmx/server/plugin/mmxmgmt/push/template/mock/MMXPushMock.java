package com.magnet.mmx.server.plugin.mmxmgmt.push.template.mock;


import com.google.gson.Gson;
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
    private static Map<Integer, MMXTemplate> TEMPLATE_MAP = new HashMap<>();
    private static Map<String, MMXTemplate> TEMPLATE_UNIQUE = new HashMap<>();
    private static Map<Integer, MMXPushConfig> CONFIG_MAP = new HashMap<>();

    static {
        createTemplate("app1", "t1");
        createTemplate("app1", "t2");
        createTemplate("app1", "t3");
        ///////////////////////////////
        createTemplate("app2", "t1");
        createTemplate("app2", "t2");
    }

    public static MMXTemplate createTemplate(String appId, String templateName) {

        int id = SEQUENCE++;
        MMXTemplate template = new MMXTemplate();
        template.setTemplateId(id);
        template.setAppId(appId);
        template.setTemplate("T-" + id);
        template.setTemplateType("PUSH");
        template.setTemplateName(templateName);
        TEMPLATE_MAP.put(id, template);
        TEMPLATE_UNIQUE.put(getTemplateKey(appId, templateName), template);
        return template;
    }
    public static MMXTemplate getTemplate(String appId, String templateName) {
        return TEMPLATE_UNIQUE.get(getTemplateKey(appId, templateName));
    }
    public static MMXTemplate getTemplate(int templateId) {
        return TEMPLATE_UNIQUE.get(templateId);
    }
    public static MMXTemplate updateTemplate(String appId, String templateName, String template) {
        MMXTemplate t = getTemplate(appId, templateName);
        t.setTemplate(template);
        return t;
    }

    private static String getTemplateKey(String appId, String templateName) {
        return templateName + "@" + appId;
    }

    private static MMXPushConfig createConfig(int id, String configName, String appId, int templateId, boolean is) {

        MMXPushConfig config = new MMXPushConfig();
        config.setConfigId(id);
        config.setConfigName(configName);
        config.setAppId(appId);
        config.setTemplate(TEMPLATE_MAP.get(templateId));
//        config.setTemplateType("PUSH");
        CONFIG_MAP.put(id, config);
        return config;
    }



    //    private static String serialize(Object obj) {
//
//        Gson gson = new Gson();
//        return gson.toJson(obj);
//    }
//    private static <T> T deserialize(String jsonStr, Class<T> clazz) {
//
//        Gson gson = new Gson();
//        return gson.fromJson(jsonStr, clazz);
//    }
//
//    public static void main(String[] args) {
//
//        MMXPushMock x = new MMXPushMock();
//
//    }
}

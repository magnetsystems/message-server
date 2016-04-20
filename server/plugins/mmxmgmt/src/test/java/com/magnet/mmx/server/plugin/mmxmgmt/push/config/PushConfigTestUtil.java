package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.*;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by mmicevic on 4/7/16.
 *
 */
public class PushConfigTestUtil {

    private static int SEQ = 0;

    public static MMXPushConfigMapping createMapping(String appId, Integer configId, String channelId) throws MMXException {
        MMXPushConfigMapping m = new MMXPushConfigMapping();
        m.setAppId(appId);
        m.setConfigId(configId);
        m.setChannelId(channelId);
        return MMXPushConfigService.getInstance().createConfigMapping(m);
    }

    public static MMXPushConfigMapping createMapping2(String appId, Integer configId, String channelId) throws MMXException {
        return MMXPushConfigService.getInstance().createConfigMapping(configId, appId, channelId);
    }

    public static MMXPushConfig updateConfig(String appId, String configName, boolean isSilentPush, boolean isEnabled, Map<String, String> meta, Set<String> channelIds) throws MMXException {
        MMXPushConfig c = MMXPushConfigService.getInstance().getConfig(appId, configName);
        c.setSilentPush(isSilentPush);
        c.setEnabled(isEnabled);
        c.setMeta(meta);
        c.setChannelIds(channelIds);
        return MMXPushConfigService.getInstance().updateConfig(c);
    }
    public static MMXPushConfig createConfig(String appId, String configName, boolean isSilentPush, boolean isEnabled, Map<String, String> meta, Set<String> channelIds) throws MMXException {
        MMXPushConfig c = new MMXPushConfig();
        c.setAppId(appId);
        c.setConfigName(configName);
        MMXTemplate t = createTemplate(appId, MMXTemplateType.PUSH, "nn" + SEQ++, "tt");
        c.setTemplateId(t.getTemplateId());
        c.setSilentPush(isSilentPush);
        c.setEnabled(isEnabled);
        c.setMeta(meta);
        c.setChannelIds(channelIds);
        return MMXPushConfigService.getInstance().createConfig(c);
    }
    public static MMXPushConfig createConfig2(String appId, String configName, boolean isSilentPush, boolean isEnabled, Map<String, String> meta) throws MMXException {
        String name = "nn" + SEQ++;
        MMXTemplate t = createTemplate(appId, MMXTemplateType.PUSH, name, "tt" + SEQ++);
        return createConfig(appId, configName, name, isSilentPush, isEnabled, meta);
    }
    private static MMXPushConfig createConfig(String appId, String configName, String templateName, boolean isSilentPush, boolean isEnabled, Map<String, String> meta) throws MMXException {
        MMXPushConfig config = new MMXPushConfig();
        config.setAppId(appId);
        config.setConfigName(configName);
        config.setSilentPush(isSilentPush);
        config.setEnabled(isEnabled);
        config.setTemplateId(MMXPushConfigService.getInstance().getTemplate(appId, templateName).getTemplateId());
        config.setMeta(meta);
        return MMXPushConfigService.getInstance().createConfig(config);
    }


    public static MMXTemplate createTemplate(String appId, MMXTemplateType type, String name, String template) throws MMXException {
        return MMXPushConfigService.getInstance().createTemplate(appId, name, type, template);
    }

    public static void deleteAllDataForApp(String appId) throws MMXException {

        deleteAllTemplatesForApp(appId);
        deleteAllMappingForApp(appId);
        deleteAllConfigsForApp(appId);
        deleteAllSuppressForApp(appId);
    }
    public static void deleteAllMappingForApp(String appId) throws MMXException {

        Collection<MMXPushConfigMapping> list = MMXPushConfigService.getInstance().getAllConfigMappings(appId);
        if (list != null) {
            for (MMXPushConfigMapping mapping : list) {
                MMXPushConfigService.getInstance().deleteConfigMapping(mapping);
            }
        }
    }
    public static void deleteAllTemplatesForApp(String appId) throws MMXException {

        Collection<MMXTemplate> list = MMXPushConfigService.getInstance().getAllTemplates(appId);
        if (list != null) {
            for (MMXTemplate mapping : list) {
                MMXPushConfigService.getInstance().deleteTemplate(mapping);
            }
        }
    }
    public static void deleteAllConfigsForApp(String appId) throws MMXException {

        Collection<MMXPushConfig> list = MMXPushConfigService.getInstance().getAllConfigs(appId);
        if (list != null) {
            for (MMXPushConfig config : list) {
                MMXPushConfigService.getInstance().deleteConfig(config);
            }
        }
    }
    public static void deleteAllSuppressForApp(String appId) throws MMXException {

        Collection<MMXPushSuppress> list = MMXPushConfigService.getInstance().getAllPushSuppress(appId);
        if (list != null) {
            for (MMXPushSuppress suppress : list) {
                MMXPushConfigService.getInstance().deletePushSuppress(suppress);
            }
        }
    }

    public static void main (String[] args) throws MMXException {
//        deleteAllDataForApp("system");
//        deleteAllDataForApp("appId");
//        deleteAllDataForApp("aa");
        deleteAllDataForApp("test-app");
    }
}

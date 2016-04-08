package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfigMapping;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplateType;

import java.util.Map;

/**
 * Created by mmicevic on 4/7/16.
 *
 */
public class PushConfigTestUtil {

    public static MMXPushConfigMapping createMapping(String appId, int configId, String channelName) throws MMXException {
        MMXPushConfigMapping m = new MMXPushConfigMapping();
        m.setAppId(appId);
        m.setConfigId(configId);
        m.setChannelName(channelName);
        return MMXPushConfigService.getInstance().createConfigMapping(m);
    }

    public static MMXPushConfigMapping createMapping2(String appId, int configId, String channelName) throws MMXException {
        return MMXPushConfigService.getInstance().createConfigMapping(configId, appId, channelName);
    }

    public static MMXPushConfig createConfig(String appId, String configName, boolean isSilentPush, Map<String, String> meta) throws MMXException {
        MMXPushConfig c = new MMXPushConfig();
        c.setAppId(appId);
        c.setConfigName(configName);
        MMXTemplate t = createTemplate(appId, MMXTemplateType.PUSH, "nn", "tt");
        c.setTemplate(t);
        c.setIsSilentPush(isSilentPush);
        c.setMeta(meta);
        return MMXPushConfigService.getInstance().createConfig(c);
    }
    public static MMXPushConfig createConfig2(String appId, String configName, boolean isSilentPush, Map<String, String> meta) throws MMXException {
        MMXTemplate t = createTemplate(appId, MMXTemplateType.PUSH, "nn2", "tt2");
        return MMXPushConfigService.getInstance().createConfig(appId, configName, "nn2", isSilentPush, meta);
    }


    public static MMXTemplate createTemplate(String appId, MMXTemplateType type, String name, String template) throws MMXException {
        return MMXPushConfigService.getInstance().createTemplate(appId, name, type, template);
    }
}

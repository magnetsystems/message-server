package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by mmicevic on 4/1/16.
 *
 */
public class MMXPushConfigServiceDefaultsTest {

    @Test
    public void checkDefultTemplate() throws MMXException {
        MMXTemplate t = MMXPushConfigService.getInstance().getTemplate(MMXPushConfigService.SYSTEM_APP, MMXPushConfigService.DEFAULT_TEMPLATE);
        assertDefaultTemplate(t);
    }
    private void assertDefaultTemplate(MMXTemplate t) {
        Assert.assertNotNull(t);
        Assert.assertEquals(MMXPushConfigService.SYSTEM_APP, t.getAppId());
        Assert.assertEquals(MMXPushConfigService.DEFAULT_TEMPLATE, t.getTemplateName());
        Assert.assertNotNull(t.getTemplate());
    }
    @Test
    public void checkDefaultConfig() throws MMXException {

        MMXPushConfig c = MMXPushConfigService.getInstance().getConfig(MMXPushConfigService.SYSTEM_APP, MMXPushConfigService.DEFAULT_CONFIG);
        assertDefaultConfig(c);
    }
    private void assertDefaultConfig(MMXPushConfig c ) {

        Assert.assertNotNull(c);
        Assert.assertEquals(MMXPushConfigService.SYSTEM_APP, c.getAppId());
        Assert.assertEquals(MMXPushConfigService.DEFAULT_CONFIG, c.getConfigName());
        assertDefaultTemplate(c.getTemplate());
    }
    @Test
    public void checkDefaultMapping() throws MMXException {

        MMXPushConfig c = MMXPushConfigService.getInstance().getPushConfig(null, null, null);
        assertDefaultConfig(c);
    }
    @Test
    public void checkDefaultMappingWrongArguments() throws MMXException {

        String appId = "my-fake-app";
        String configName = "my-fake-config";
        String channelName = "my-fake-channel";
        MMXPushConfig c = MMXPushConfigService.getInstance().getPushConfig(appId, channelName, configName);
        assertDefaultConfig(c);
    }
}

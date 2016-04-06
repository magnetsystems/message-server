package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplateType;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by mmicevic on 4/5/16.
 *
 */
public class MMXPushConfigServiceConfigTest {

    @Test(expected = MMXException.class)
    public void createConfigNull() throws MMXException {
        MMXPushConfig config = null;
        MMXPushConfigService.getInstance().createConfig(config);
    }
    @Test(expected = MMXException.class)
    public void createConfigEmpty() throws MMXException {
        MMXPushConfig config = new MMXPushConfig();
        MMXPushConfigService.getInstance().createConfig(config);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_1_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig(null, "cc", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_1_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig("", "cc", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_1_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig("   ", "cc", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_2_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig2(null, "cc", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_2_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig2("", "cc", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_2_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig2("   ", "cc", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingConfigName_1_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig("aa", null, true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingConfigName_1_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig("aa", "", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingConfigName_1_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig("aa", "   ", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingConfigName_2_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig2("aa", null, true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingConfigName_2_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig2("aa", "", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingConfigName_2_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        createConfig2("aa", "   ", true, meta);
    }

    @Test
    public void createTemplateMissingMeta() throws MMXException {
        Map<String,String> meta = null;
        MMXPushConfig c = createConfig("aa", "cc", true, meta);
        Assert.assertNotNull(c);
        c = createConfig2("aa", "cc", true, meta);
        Assert.assertNotNull(c);
    }
    private static MMXPushConfig createConfig(String appId, String configName, boolean isSilentPush, Map<String, String> meta) throws MMXException {
        MMXPushConfig c = new MMXPushConfig();
        c.setAppId(appId);
        c.setConfigName(configName);
        MMXTemplate t = createTemplate(appId, MMXTemplateType.PUSH, "nn", "tt");
        c.setTemplate(t);
        c.setIsSilentPush(isSilentPush);
        c.setMeta(meta);
        return MMXPushConfigService.getInstance().createConfig(c);
    }
    private static MMXPushConfig createConfig2(String appId, String configName, boolean isSilentPush, Map<String, String> meta) throws MMXException {
        MMXTemplate t = createTemplate(appId, MMXTemplateType.PUSH, "nn2", "tt2");
        return MMXPushConfigService.getInstance().createConfig(appId, configName, "nn2", isSilentPush, meta);
    }

    private static MMXTemplate createTemplate(String appId, MMXTemplateType type, String name, String template) throws MMXException {
        return MMXPushConfigService.getInstance().createTemplate(appId, name, type, template);
    }

    @Test
    public void createAndRetrieveConfig() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        MMXPushConfig c = createConfig("aa", "cc", true, meta);
        assertConfig(c, "aa", "cc", true, meta);

        //retrieve
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, meta);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, meta);
    }
    @Test
    public void createAndUpdateConfig() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        MMXPushConfig c = createConfig("aa", "cc", true, meta);
        assertConfig(c, "aa", "cc", true, meta);
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, meta);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, meta);

        //do update
        Map<String,String> meta2 = new HashMap<>();
        meta2.put("kk1", "vv1");
        meta2.put("kk2", "vv2");
        c3.setConfigName("cc2");
        c3.setMeta(meta2);
        c3.setIsSilentPush(false);
        MMXPushConfig c4 = MMXPushConfigService.getInstance().updateConfig(c3);
        assertConfig(c4, "aa", "cc2", false, meta2);

        //retrieve after update
        MMXPushConfig c5 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c5, "aa", "cc2", false, meta2);
        MMXPushConfig c6 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c5.getConfigName());
        assertConfig(c6, "aa", "cc2", false, meta2);

        //make sure there is no old record
        MMXPushConfig c7 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), "cc");
        Assert.assertNull(c7);

    }
    @Test
    public void createAndDeleteConfig() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        MMXPushConfig c = createConfig("aa", "cc", true, meta);
        assertConfig(c, "aa", "cc", true, meta);
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, meta);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, meta);

        //do delete
        MMXPushConfigService.getInstance().deleteConfig(c3);

        //make sure there is no old record
        MMXPushConfig c4 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        Assert.assertNull(c4);
        MMXPushConfig c5 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), "cc");
        Assert.assertNull(c5);

    }
    private static void assertConfig(MMXPushConfig c, String appId, String configName, boolean isSilentPush, Map<String, String> meta) throws MMXException {

        Assert.assertNotNull(c);
        Assert.assertEquals(appId, c.getAppId());
        Assert.assertEquals(configName, c.getConfigName());
        Assert.assertEquals(isSilentPush, c.isSilentPush());
        assertMeta(meta, c.getMeta());
    }
    private static void assertMeta(Map<String, String> expected, Map<String, String> actual) throws MMXException {

        if (expected == null && actual == null) {
            return;
        }
        if (expected == null) {
            Assert.fail("expexted null but was not null");
        }
        if (actual == null) {
            Assert.fail("expexted not null but was null");
        }
        if(expected.size() != actual.size()) {
            Assert.fail("expexted size = "  + expected.size() + " but was " + actual.size());
        }
        Set<String> keys = expected.keySet();
        for (String key : keys) {
            String expectedValue = expected.get(key);
            String actualValue = actual.get(key);
            Assert.assertEquals(expectedValue, actualValue);
        }
    }
}

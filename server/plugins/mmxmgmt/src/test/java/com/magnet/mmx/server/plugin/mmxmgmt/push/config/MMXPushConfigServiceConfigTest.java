package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
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
    public void createConfigMissingAppId_1_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = null;
        PushConfigTestUtil.createConfig(null, "cc", true, true, meta, channelIds);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_1_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = null;
        PushConfigTestUtil.createConfig("", "cc", true, true, meta, channelIds);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_1_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = null;
        PushConfigTestUtil.createConfig("   ", "cc", true, true, meta, channelIds);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_2_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2(null, "cc", true, true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_2_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("", "cc", true, true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_2_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("   ", "cc", true, true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_1_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = null;
        PushConfigTestUtil.createConfig("aa", null, true, true, meta, channelIds);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_1_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = null;
        PushConfigTestUtil.createConfig("aa", "", true, true, meta, channelIds);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_1_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = null;
        PushConfigTestUtil.createConfig("aa", "   ", true, true, meta, channelIds);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_2_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("aa", null, true, true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_2_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("aa", "", true, true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_2_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("aa", "   ", true, true, meta);
    }

    @Test
    public void createConfigMissingMeta() throws MMXException {
        Map<String,String> meta = null;
        Set<String> channelIds = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, true, meta, channelIds);
        Assert.assertNotNull(c);
        c = PushConfigTestUtil.createConfig2("aa", "cc", true, true, meta);
        Assert.assertNotNull(c);
    }

    @Test
    public void createAndRetrieveConfigNoChannels() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, false, meta, channelIds);
        assertConfig(c, "aa", "cc", true, false, meta, null);
        //retrieve
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, false, meta, null);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, false, meta, null);
    }
    @Test
    public void createAndRetrieveConfigWithChannels() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = new HashSet<>();
        channelIds.add("ch1");
        channelIds.add("ch2");

        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, true, meta, channelIds);
        assertConfig(c, "aa", "cc", true, true, meta, channelIds);

        //retrieve
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, true, meta, channelIds);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, true, meta, channelIds);
    }
    @Test
    public void createAndUpdateConfigNoChannels() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, true, meta, channelIds);
        assertConfig(c, "aa", "cc", true, true, meta, channelIds);
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, true, meta, channelIds);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, true, meta, channelIds);

        //do update
        Map<String,String> meta2 = new HashMap<>();
        meta2.put("kk1", "vv1");
        meta2.put("kk2", "vv2");
        c3.setConfigName("cc2");
        c3.setMeta(meta2);
        c3.setSilentPush(false);
        c3.setEnabled(false);
        MMXPushConfig c4 = MMXPushConfigService.getInstance().updateConfig(c3);
        assertConfig(c4, "aa", "cc2", false, false, meta2, channelIds);

        //retrieve after update
        MMXPushConfig c5 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c5, "aa", "cc2", false, false, meta2, channelIds);
        MMXPushConfig c6 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c5.getConfigName());
        assertConfig(c6, "aa", "cc2", false, false, meta2, channelIds);

        //make sure there is no old record
        assertConfigNotFound(c.getAppId(), "cc");
//        MMXPushConfig c7 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), "cc");
//        Assert.assertNull(c7);

    }
    @Test
    public void createAndDeleteConfigNoChannels() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelIds = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, true, meta, channelIds);
        assertConfig(c, "aa", "cc", true, true, meta, channelIds);
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, true, meta, channelIds);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, true, meta, channelIds);

        //do delete
        MMXPushConfigService.getInstance().deleteConfig(c3);

        //make sure there is no old record
        assertConfigNotFound(c.getConfigId());
        assertConfigNotFound(c.getAppId(), "cc");
//        MMXPushConfig c4 = MMXPushConfigService.getInstance().getConfig();
//        Assert.assertNull(c4);
//        MMXPushConfig c5 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), "cc");
//        Assert.assertNull(c5);

    }
    private static void assertConfigNotFound(int configId) {

        boolean found = false;
        try {
            MMXPushConfigService.getInstance().getConfig(configId);
            found = true;
        }
        catch (MMXException e) {
            e.printStackTrace();
        }
        if (found) {
            Assert.fail("expecting 'config not found' for id = " + configId);
        }
    }
    private static void assertConfigNotFound(String appId, String configName) {

        boolean found = false;
        try {
            MMXPushConfigService.getInstance().getConfig(appId, configName);
            found = true;
        }
        catch (MMXException e) {
            e.printStackTrace();
        }
        if (found) {
            Assert.fail("expecting 'config not found' for id = " + appId + "/" + configName);
        }
    }
    private static void assertConfig(MMXPushConfig c, String appId, String configName, boolean isSilentPush, boolean isEnabled, Map<String, String> meta, Set<String> channels) throws MMXException {

        Assert.assertNotNull(c);
        Assert.assertEquals(appId, c.getAppId());
        Assert.assertEquals(configName, c.getConfigName());
        Assert.assertEquals(isSilentPush, c.isSilentPush());
        Assert.assertEquals(isEnabled, c.isEnabled());
        assertMeta(meta, c.getMeta());
        if (channels == null || channels.size() == 0) {
            Assert.assertTrue(c.getChannelIds() == null || c.getChannelIds().size() == 0);
        } else {
            Assert.assertEquals(channels.size(), c.getChannelIds().size());
            for (String ch : c.getChannelIds()) {
                Assert.assertTrue(c.getChannelIds().contains(ch));
            }
        }
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

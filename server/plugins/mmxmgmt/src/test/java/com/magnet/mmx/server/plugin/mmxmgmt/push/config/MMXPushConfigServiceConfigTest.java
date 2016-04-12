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
        Set<String> channelNames = null;
        PushConfigTestUtil.createConfig(null, "cc", true, meta, channelNames);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_1_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelNames = null;
        PushConfigTestUtil.createConfig("", "cc", true, meta, channelNames);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_1_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelNames = null;
        PushConfigTestUtil.createConfig("   ", "cc", true, meta, channelNames);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_2_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2(null, "cc", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_2_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("", "cc", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingAppId_2_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("   ", "cc", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_1_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelNames = null;
        PushConfigTestUtil.createConfig("aa", null, true, meta, channelNames);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_1_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelNames = null;
        PushConfigTestUtil.createConfig("aa", "", true, meta, channelNames);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_1_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelNames = null;
        PushConfigTestUtil.createConfig("aa", "   ", true, meta, channelNames);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_2_null() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("aa", null, true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_2_empty() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("aa", "", true, meta);
    }
    @Test(expected = MMXException.class)
    public void createConfigMissingConfigName_2_space() throws MMXException {
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        PushConfigTestUtil.createConfig2("aa", "   ", true, meta);
    }

    @Test
    public void createConfigMissingMeta() throws MMXException {
        Map<String,String> meta = null;
        Set<String> channelNames = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, meta, channelNames);
        Assert.assertNotNull(c);
        c = PushConfigTestUtil.createConfig2("aa", "cc", true, meta);
        Assert.assertNotNull(c);
    }

    @Test
    public void createAndRetrieveConfigNoChannels() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelNames = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, meta, channelNames);
        assertConfig(c, "aa", "cc", true, meta, null);
        //retrieve
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, meta, null);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, meta, null);
    }
    @Test
    public void createAndRetrieveConfigWithChannels() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelNames = new HashSet<>();
        channelNames.add("ch1");
        channelNames.add("ch2");

        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, meta, channelNames);
        assertConfig(c, "aa", "cc", true, meta, channelNames);

        //retrieve
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, meta, channelNames);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, meta, channelNames);
    }
    @Test
    public void createAndUpdateConfigNoChannels() throws MMXException {
        //create
        Map<String,String> meta = new HashMap<>();
        meta.put("kk", "vv");
        Set<String> channelNames = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, meta, channelNames);
        assertConfig(c, "aa", "cc", true, meta, channelNames);
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, meta, channelNames);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, meta, channelNames);

        //do update
        Map<String,String> meta2 = new HashMap<>();
        meta2.put("kk1", "vv1");
        meta2.put("kk2", "vv2");
        c3.setConfigName("cc2");
        c3.setMeta(meta2);
        c3.setIsSilentPush(false);
        MMXPushConfig c4 = MMXPushConfigService.getInstance().updateConfig(c3);
        assertConfig(c4, "aa", "cc2", false, meta2, channelNames);

        //retrieve after update
        MMXPushConfig c5 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c5, "aa", "cc2", false, meta2, channelNames);
        MMXPushConfig c6 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c5.getConfigName());
        assertConfig(c6, "aa", "cc2", false, meta2, channelNames);

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
        Set<String> channelNames = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, meta, channelNames);
        assertConfig(c, "aa", "cc", true, meta, channelNames);
        MMXPushConfig c2 = MMXPushConfigService.getInstance().getConfig(c.getConfigId());
        assertConfig(c2, "aa", "cc", true, meta, channelNames);
        MMXPushConfig c3 = MMXPushConfigService.getInstance().getConfig(c.getAppId(), c.getConfigName());
        assertConfig(c3, "aa", "cc", true, meta, channelNames);

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
    private static void assertConfig(MMXPushConfig c, String appId, String configName, boolean isSilentPush, Map<String, String> meta, Set<String> channels) throws MMXException {

        Assert.assertNotNull(c);
        Assert.assertEquals(appId, c.getAppId());
        Assert.assertEquals(configName, c.getConfigName());
        Assert.assertEquals(isSilentPush, c.isSilentPush());
        assertMeta(meta, c.getMeta());
        if (channels == null || channels.size() == 0) {
            Assert.assertTrue(c.getChannelNames() == null || c.getChannelNames().size() == 0);
        } else {
            Assert.assertEquals(channels.size(), c.getChannelNames().size());
            for (String ch : c.getChannelNames()) {
                Assert.assertTrue(c.getChannelNames().contains(ch));
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

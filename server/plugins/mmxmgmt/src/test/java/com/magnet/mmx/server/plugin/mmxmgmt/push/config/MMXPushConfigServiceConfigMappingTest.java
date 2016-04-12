package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfigMapping;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;


/**
 * Created by mmicevic on 4/1/16.
 *
 */
public class MMXPushConfigServiceConfigMappingTest {

    //validation
    @Test(expected = MMXException.class)
    public void createMappingNull() throws MMXException {
        MMXPushConfigService.getInstance().createConfigMapping(null);
    }
    @Test(expected = MMXException.class)
    public void createMappingEmpty() throws MMXException {
        MMXPushConfigMapping t = new MMXPushConfigMapping();
        MMXPushConfigService.getInstance().createConfigMapping(t);
    }

    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_1_null() throws MMXException {
        PushConfigTestUtil.createMapping(null, 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_1_empty() throws MMXException {
        PushConfigTestUtil.createMapping("", 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_1_space() throws MMXException {
        PushConfigTestUtil.createMapping("   ", 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_2_null() throws MMXException {
        PushConfigTestUtil.createMapping2(null, 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_2_empty() throws MMXException {
        PushConfigTestUtil.createMapping2("", 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_2_space() throws MMXException {
        PushConfigTestUtil.createMapping2("   ", 1, "ch");
    }

    @Test
    public void createMappingNoChannel() throws MMXException {
        Set<String> channelNames = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("appId", "cc-1", true, null, channelNames);
        MMXPushConfigMapping m = PushConfigTestUtil.createMapping("appId", c.getConfigId(), null);
        assertMapping(m, "appId", c.getConfigId(), null);

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, "appId", c.getConfigId(), null);
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping("appId", null);
        assertMapping(m2, "appId", c.getConfigId(), null);
    }

    @Test
    public void createMappingWithChannel() throws MMXException {

        //create
        Set<String> channelNames = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, null, channelNames);

        MMXPushConfigMapping m = PushConfigTestUtil.createMapping("appId", c.getConfigId(), "c1");
        assertMapping(m, "appId", c.getConfigId(), "c1");

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, "appId", c.getConfigId(), "c1");
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping("appId", "c1");
        assertMapping(m2, "appId", c.getConfigId(), "c1");
    }
    @Test
    public void createAndUpdateMapping() throws MMXException {

        Set<String> channelNames = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, null, channelNames);
        MMXPushConfigMapping m = PushConfigTestUtil.createMapping("appId", c.getConfigId(), "c1");
        assertMapping(m, "appId", c.getConfigId(), "c1");

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, "appId", c.getConfigId(), "c1");
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping("appId", "c1");
        assertMapping(m2, "appId", c.getConfigId(), "c1");

        //do update
        MMXPushConfig c2 = PushConfigTestUtil.createConfig("aa", "cc", true, null, channelNames);
        m2.setAppId("app-2");
        m2.setConfigId(c2.getConfigId());
        m2.setChannelName("c2");
        MMXPushConfigMapping m3 = MMXPushConfigService.getInstance().updateConfigMapping(m2);
        assertMapping(m3, "app-2", c2.getConfigId(), "c2");

        //retrieve back from db
        MMXPushConfigMapping m4 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m4, "app-2", c2.getConfigId(), "c2");
        MMXPushConfigMapping m5 = MMXPushConfigService.getInstance().getConfigMapping("app-2", "c2");
        assertMapping(m5, "app-2", c2.getConfigId(), "c2");

        //make sure there is no old record
        assertMappingNotFound("appId", "c1");
//        MMXPushConfigMapping m6 = MMXPushConfigService.getInstance().getConfigMapping("appId", "c1");
//        Assert.assertNull(m6);

    }
    @Test
     public void createAndDeleteMapping() throws MMXException {

        Set<String> channelNames = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig("aa", "cc", true, null, channelNames);
        MMXPushConfigMapping m = PushConfigTestUtil.createMapping("app5",  c.getConfigId(), "c1");
        assertMapping(m, "app5", c.getConfigId(), "c1");

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, "app5", c.getConfigId(), "c1");
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping("app5", "c1");
        assertMapping(m2, "app5",  c.getConfigId(), "c1");

        //do delete
        MMXPushConfigService.getInstance().deleteConfigMapping(m2);

        //retrieve back from db
        assertMappingNotFound(m.getMappingId());
        assertMappingNotFound(m.getAppId(), m.getChannelName());
//        MMXPushConfigMapping m4 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
//        Assert.assertNull(m4);
//        MMXPushConfigMapping m5 = MMXPushConfigService.getInstance().getConfigMapping(m.getAppId(), m.getChannelName());
//        Assert.assertNull(m5);
    }
    private static void assertMappingNotFound(int mappingId) {

        boolean found = false;
        try {
            MMXPushConfigService.getInstance().getConfigMapping(mappingId);
            found = true;
        }
        catch (MMXException e) {
            e.printStackTrace();
        }
        if (found) {
            Assert.fail("expecting 'mapping not found' for id = " + mappingId);
        }
    }
    private static void assertMappingNotFound(String appId, String channelName) {

        boolean found = false;
        try {
            MMXPushConfigService.getInstance().getConfigMapping(appId, channelName);
            found = true;
        }
        catch (MMXException e) {
            e.printStackTrace();
        }
        if (found) {
            Assert.fail("expecting 'mapping not found' for id = " + appId + "/" + channelName);
        }
    }


//    @Test
//    public void createAndRetrieveMapping() throws MMXException {
//        MMXMapping t = PushConfigTestUtil.createMapping("aa", MMXMappingType.PUSH, "nn", "tt");
//        assertMapping(t, "aa", MMXMappingType.PUSH, "nn", "tt");
//        MMXMapping t2 = MMXPushConfigService.getInstance().getMapping(t.getMappingId());
//        assertMapping(t2, "aa", MMXMappingType.PUSH, "nn", "tt");
//        MMXMapping t3 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), t.getMappingName());
//        assertMapping(t3, "aa", MMXMappingType.PUSH, "nn", "tt");
//    }
//    @Test
//    public void createAndUpdateMappingName() throws MMXException {
//        MMXMapping t = PushConfigTestUtil.createMapping("aa", MMXMappingType.PUSH, "nn", "tt");
//        t.setMappingName("nn2");
//        //
//        t = MMXPushConfigService.getInstance().updateMapping(t);
//        assertMapping(t, "aa", MMXMappingType.PUSH, "nn2", "tt");
//        MMXMapping t2 = MMXPushConfigService.getInstance().getMapping(t.getMappingId());
//        assertMapping(t2, "aa", MMXMappingType.PUSH, "nn2", "tt");
//        MMXMapping t3 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), t.getMappingName());
//        assertMapping(t3, "aa", MMXMappingType.PUSH, "nn2", "tt");
//        //make sure there is no old record
//        MMXMapping t4 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), "nn");
//        Assert.assertNull(t4);
//    }
//    @Test
//    public void createAndUpdateMapping() throws MMXException {
//        MMXMapping t = PushConfigTestUtil.createMapping("aa", MMXMappingType.PUSH, "nn", "tt");
//        t.setMapping("tt2");
//        //
//        t = MMXPushConfigService.getInstance().updateMapping(t);
//        assertMapping(t, "aa", MMXMappingType.PUSH, "nn", "tt2");
//        MMXMapping t2 = MMXPushConfigService.getInstance().getMapping(t.getMappingId());
//        assertMapping(t2, "aa", MMXMappingType.PUSH, "nn", "tt2");
//        MMXMapping t3 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), t.getMappingName());
//        assertMapping(t3, "aa", MMXMappingType.PUSH, "nn", "tt2");
//    }
//    @Test
//    public void createAndDeleteMapping() throws MMXException {
//        MMXMapping t = PushConfigTestUtil.createMapping("aa", MMXMappingType.PUSH, "nn", "tt");
//        assertMapping(t, "aa", MMXMappingType.PUSH, "nn", "tt");
//        MMXMapping t2 = MMXPushConfigService.getInstance().getMapping(t.getMappingId());
//        assertMapping(t2, "aa", MMXMappingType.PUSH, "nn", "tt");
//
//        MMXPushConfigService.getInstance().deleteMapping(t.getMappingId());
//        MMXMapping t3 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), t.getMappingName());
//        Assert.assertNull(t3);
//    }
    private static void assertMapping(MMXPushConfigMapping m, String appId, int configId, String channelName) throws MMXException {
        Assert.assertNotNull(m);
        Assert.assertEquals(appId, m.getAppId());
        Assert.assertEquals(configId, m.getConfigId());
        Assert.assertEquals(channelName, m.getChannelName());
    }
}

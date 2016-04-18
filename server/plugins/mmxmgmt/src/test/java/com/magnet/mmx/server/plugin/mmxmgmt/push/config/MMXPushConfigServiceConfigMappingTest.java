package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfigMapping;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;


/**
 * Created by mmicevic on 4/1/16.
 *
 */
public class MMXPushConfigServiceConfigMappingTest {

    private static final String APP_ID = "test-app";

    @Before
    public void cleanUp() throws MMXException {
        PushConfigTestUtil.deleteAllDataForApp(APP_ID);
    }



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
        Set<String> channelIds = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig(APP_ID, "cc-1", true, true, null, channelIds);
        MMXPushConfigMapping m = PushConfigTestUtil.createMapping(APP_ID, c.getConfigId(), null);
        assertMapping(m, APP_ID, c.getConfigId(), null);

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, APP_ID, c.getConfigId(), null);
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping(APP_ID, null);
        assertMapping(m2, APP_ID, c.getConfigId(), null);
    }

    @Test
    public void createMappingWithChannel() throws MMXException {

        //create
        Set<String> channelIds = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig(APP_ID, "cc", true, true, null, channelIds);

        MMXPushConfigMapping m = PushConfigTestUtil.createMapping(APP_ID, c.getConfigId(), "c1");
        assertMapping(m, APP_ID, c.getConfigId(), "c1");

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, APP_ID, c.getConfigId(), "c1");
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping(APP_ID, "c1");
        assertMapping(m2, APP_ID, c.getConfigId(), "c1");
    }
    @Test
    public void createAndUpdateMapping() throws MMXException {

        Set<String> channelIds = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig(APP_ID, "cc", true, true, null, channelIds);
        MMXPushConfigMapping m = PushConfigTestUtil.createMapping(APP_ID, c.getConfigId(), "c1");
        assertMapping(m, APP_ID, c.getConfigId(), "c1");

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, APP_ID, c.getConfigId(), "c1");
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping(APP_ID, "c1");
        assertMapping(m2, APP_ID, c.getConfigId(), "c1");

        //do update
        MMXPushConfig c2 = PushConfigTestUtil.createConfig(APP_ID, "cc", true, true, null, channelIds);
        m2.setAppId(APP_ID);
        m2.setConfigId(c2.getConfigId());
        m2.setChannelId("c2");
        MMXPushConfigMapping m3 = MMXPushConfigService.getInstance().updateConfigMapping(m2);
        assertMapping(m3, APP_ID, c2.getConfigId(), "c2");

        //retrieve back from db
        MMXPushConfigMapping m4 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m4, APP_ID, c2.getConfigId(), "c2");
        MMXPushConfigMapping m5 = MMXPushConfigService.getInstance().getConfigMapping(APP_ID, "c2");
        assertMapping(m5, APP_ID, c2.getConfigId(), "c2");

        //make sure there is no old record
        assertMappingNotFound(APP_ID, "c1");
//        MMXPushConfigMapping m6 = MMXPushConfigService.getInstance().getConfigMapping(APP_ID, "c1");
//        Assert.assertNull(m6);

    }
    @Test
     public void createAndDeleteMapping() throws MMXException {

        Set<String> channelIds = null;
        MMXPushConfig c = PushConfigTestUtil.createConfig(APP_ID, "cc", true, true, null, channelIds);
        MMXPushConfigMapping m = PushConfigTestUtil.createMapping(APP_ID,  c.getConfigId(), "c1");
        assertMapping(m, APP_ID, c.getConfigId(), "c1");

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, APP_ID, c.getConfigId(), "c1");
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping(APP_ID, "c1");
        assertMapping(m2, APP_ID,  c.getConfigId(), "c1");

        //do delete
        MMXPushConfigService.getInstance().deleteConfigMapping(m2);

        //retrieve back from db
        assertMappingNotFound(m.getMappingId());
        assertMappingNotFound(m.getAppId(), m.getChannelId());
//        MMXPushConfigMapping m4 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
//        Assert.assertNull(m4);
//        MMXPushConfigMapping m5 = MMXPushConfigService.getInstance().getConfigMapping(m.getAppId(), m.getChannelId());
//        Assert.assertNull(m5);
    }
    private static void assertMappingNotFound(Integer mappingId) {

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
    private static void assertMappingNotFound(String appId, String channelId) {

        boolean found = false;
        try {
            MMXPushConfigService.getInstance().getConfigMapping(appId, channelId);
            found = true;
        }
        catch (MMXException e) {
            e.printStackTrace();
        }
        if (found) {
            Assert.fail("expecting 'mapping not found' for id = " + appId + "/" + channelId);
        }
    }


//    @Test
//    public void createAndRetrieveMapping() throws MMXException {
//        MMXMapping t = PushConfigTestUtil.createMapping(APP_ID, MMXMappingType.PUSH, "nn", "tt");
//        assertMapping(t, APP_ID, MMXMappingType.PUSH, "nn", "tt");
//        MMXMapping t2 = MMXPushConfigService.getInstance().getMapping(t.getMappingId());
//        assertMapping(t2, APP_ID, MMXMappingType.PUSH, "nn", "tt");
//        MMXMapping t3 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), t.getMappingName());
//        assertMapping(t3, APP_ID, MMXMappingType.PUSH, "nn", "tt");
//    }
//    @Test
//    public void createAndUpdateMappingName() throws MMXException {
//        MMXMapping t = PushConfigTestUtil.createMapping(APP_ID, MMXMappingType.PUSH, "nn", "tt");
//        t.setMappingName("nn2");
//        //
//        t = MMXPushConfigService.getInstance().updateMapping(t);
//        assertMapping(t, APP_ID, MMXMappingType.PUSH, "nn2", "tt");
//        MMXMapping t2 = MMXPushConfigService.getInstance().getMapping(t.getMappingId());
//        assertMapping(t2, APP_ID, MMXMappingType.PUSH, "nn2", "tt");
//        MMXMapping t3 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), t.getMappingName());
//        assertMapping(t3, APP_ID, MMXMappingType.PUSH, "nn2", "tt");
//        //make sure there is no old record
//        MMXMapping t4 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), "nn");
//        Assert.assertNull(t4);
//    }
//    @Test
//    public void createAndUpdateMapping() throws MMXException {
//        MMXMapping t = PushConfigTestUtil.createMapping(APP_ID, MMXMappingType.PUSH, "nn", "tt");
//        t.setMapping("tt2");
//        //
//        t = MMXPushConfigService.getInstance().updateMapping(t);
//        assertMapping(t, APP_ID, MMXMappingType.PUSH, "nn", "tt2");
//        MMXMapping t2 = MMXPushConfigService.getInstance().getMapping(t.getMappingId());
//        assertMapping(t2, APP_ID, MMXMappingType.PUSH, "nn", "tt2");
//        MMXMapping t3 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), t.getMappingName());
//        assertMapping(t3, APP_ID, MMXMappingType.PUSH, "nn", "tt2");
//    }
//    @Test
//    public void createAndDeleteMapping() throws MMXException {
//        MMXMapping t = PushConfigTestUtil.createMapping(APP_ID, MMXMappingType.PUSH, "nn", "tt");
//        assertMapping(t, APP_ID, MMXMappingType.PUSH, "nn", "tt");
//        MMXMapping t2 = MMXPushConfigService.getInstance().getMapping(t.getMappingId());
//        assertMapping(t2, APP_ID, MMXMappingType.PUSH, "nn", "tt");
//
//        MMXPushConfigService.getInstance().deleteMapping(t.getMappingId());
//        MMXMapping t3 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), t.getMappingName());
//        Assert.assertNull(t3);
//    }
    private static void assertMapping(MMXPushConfigMapping m, String appId, Integer configId, String channelId) throws MMXException {
        Assert.assertNotNull(m);
        Assert.assertEquals(appId, m.getAppId());
        Assert.assertEquals(configId, m.getConfigId());
        Assert.assertEquals(channelId, m.getChannelId());
    }
}

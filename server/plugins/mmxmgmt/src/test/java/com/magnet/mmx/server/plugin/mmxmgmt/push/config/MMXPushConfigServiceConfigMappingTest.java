package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfigMapping;
import org.junit.Assert;
import org.junit.Test;

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
        createMapping(null, 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_1_empty() throws MMXException {
        createMapping("", 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_1_space() throws MMXException {
        createMapping("   ", 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_2_null() throws MMXException {
        createMapping2(null, 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_2_empty() throws MMXException {
        createMapping2("", 1, "ch");
    }
    @Test(expected = MMXException.class)
    public void createMappingMissingAppId_2_space() throws MMXException {
        createMapping2("   ", 1, "ch");
    }

    @Test
    public void createMappingNoChannel() throws MMXException {
        MMXPushConfigMapping m = createMapping("appId", 1000, null);
        assertMapping(m, "appId", 1000, null);

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, "appId", 1000, null);
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping("appId", null);
        assertMapping(m2, "appId", 1000, null);
    }
    @Test
    public void createMappingWithChannel() throws MMXException {
        MMXPushConfigMapping m = createMapping("appId", 1000, "c1");
        assertMapping(m, "appId", 1000, "c1");

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, "appId", 1000, "c1");
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping("appId", "c1");
        assertMapping(m2, "appId", 1000, "c1");
    }
    @Test
    public void createAndUpdateMapping() throws MMXException {
        MMXPushConfigMapping m = createMapping("appId", 1000, "c1");
        assertMapping(m, "appId", 1000, "c1");

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, "appId", 1000, "c1");
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping("appId", "c1");
        assertMapping(m2, "appId", 1000, "c1");

        //do update
        m2.setAppId("app-2");
        m2.setConfigId(2000);
        m2.setChannelName("c2");
        MMXPushConfigMapping m3 = MMXPushConfigService.getInstance().updateConfigMapping(m2);
        assertMapping(m3, "app-2", 2000, "c2");

        //retrieve back from db
        MMXPushConfigMapping m4 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m4, "app-2", 2000, "c2");
        MMXPushConfigMapping m5 = MMXPushConfigService.getInstance().getConfigMapping("app-2", "c2");
        assertMapping(m5, "app-2", 2000, "c2");

        //make sure there is no old record
        MMXPushConfigMapping m6 = MMXPushConfigService.getInstance().getConfigMapping("appId", "c1");
        Assert.assertNull(m6);

    }
    @Test
     public void createAndDeleteMapping() throws MMXException {
        MMXPushConfigMapping m = createMapping("app5", 1000, "c1");
        assertMapping(m, "app5", 1000, "c1");

        //retrieve back from db
        MMXPushConfigMapping m1 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        assertMapping(m1, "app5", 1000, "c1");
        MMXPushConfigMapping m2 = MMXPushConfigService.getInstance().getConfigMapping("app5", "c1");
        assertMapping(m2, "app5", 1000, "c1");

        //do delete
        MMXPushConfigService.getInstance().deleteConfigMapping(m2);

        //retrieve back from db
        MMXPushConfigMapping m4 = MMXPushConfigService.getInstance().getConfigMapping(m.getMappingId());
        Assert.assertNull(m4);
        MMXPushConfigMapping m5 = MMXPushConfigService.getInstance().getConfigMapping(m.getAppId(), m.getChannelName());
        Assert.assertNull(m5);
    }

    private static MMXPushConfigMapping createMapping(String appId, int configId, String channelName) throws MMXException {
        MMXPushConfigMapping m = new MMXPushConfigMapping();
        m.setAppId(appId);
        m.setConfigId(configId);
        m.setChannelName(channelName);
        return MMXPushConfigService.getInstance().createConfigMapping(m);
    }

    private static MMXPushConfigMapping createMapping2(String appId, int configId, String channelName) throws MMXException {
        return MMXPushConfigService.getInstance().createConfigMapping(configId, appId, channelName);
    }

//    @Test
//    public void createAndRetrieveMapping() throws MMXException {
//        MMXMapping t = createMapping("aa", MMXMappingType.PUSH, "nn", "tt");
//        assertMapping(t, "aa", MMXMappingType.PUSH, "nn", "tt");
//        MMXMapping t2 = MMXPushConfigService.getInstance().getMapping(t.getMappingId());
//        assertMapping(t2, "aa", MMXMappingType.PUSH, "nn", "tt");
//        MMXMapping t3 = MMXPushConfigService.getInstance().getMapping(t.getAppId(), t.getMappingName());
//        assertMapping(t3, "aa", MMXMappingType.PUSH, "nn", "tt");
//    }
//    @Test
//    public void createAndUpdateMappingName() throws MMXException {
//        MMXMapping t = createMapping("aa", MMXMappingType.PUSH, "nn", "tt");
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
//        MMXMapping t = createMapping("aa", MMXMappingType.PUSH, "nn", "tt");
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
//        MMXMapping t = createMapping("aa", MMXMappingType.PUSH, "nn", "tt");
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

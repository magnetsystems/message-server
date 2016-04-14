package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushSuppress;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

/**
 * Created by mmicevic on 4/13/16.
 *
 */
public class MMXPushConfigSuppressTest {

    @Test(expected = MMXException.class)
    public void createSuppressNull() throws MMXException {
        MMXPushConfigService.getInstance().createPushSuppress(null);
    }

    @Test(expected = MMXException.class)
    public void createSuppressEmpty() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        MMXPushConfigService.getInstance().createPushSuppress(suppress);
    }

    @Test(expected = MMXException.class)
    public void createSuppressAppIdNull() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId(null);
        suppress.setUserId("u1");
        suppress.setChannelName("ch1");
        MMXPushConfigService.getInstance().createPushSuppress(suppress);
    }
    @Test(expected = MMXException.class)
    public void createSuppressAppIdEmpty() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId("");
        suppress.setUserId("u1");
        suppress.setChannelName("ch1");
        MMXPushConfigService.getInstance().createPushSuppress(suppress);
    }
    @Test(expected = MMXException.class)
    public void createSuppressAppIdSpace() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId("   ");
        suppress.setUserId("u1");
        suppress.setChannelName("ch1");
        MMXPushConfigService.getInstance().createPushSuppress(suppress);
    }

    @Test
    public void createSuppressAppIdCorrectAll() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId("app1");
        suppress.setUserId("u1");
        suppress.setChannelName("ch1");
        suppress = MMXPushConfigService.getInstance().createPushSuppress(suppress);
        Assert.assertEquals("app1", suppress.getAppId());
        Assert.assertEquals("u1", suppress.getUserId());
        Assert.assertEquals("ch1", suppress.getChannelName());
        Assert.assertTrue(suppress.getSuppressId() > 0);
    }
    @Test
    public void createSuppressAppIdCorrectAppId() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId("app1");
        suppress.setUserId(null);
        suppress.setChannelName(null);
        suppress = MMXPushConfigService.getInstance().createPushSuppress(suppress);
        Assert.assertEquals("app1", suppress.getAppId());
        Assert.assertNull(suppress.getUserId());
        Assert.assertNull(suppress.getChannelName());
        Assert.assertTrue(suppress.getSuppressId() > 0);
    }
    @Test
    public void createSuppressAndRetrieveWithUserAndChannel() throws MMXException {
        doCreateSuppressAndRetrieve("app1", "u1", "ch1");
    }
    @Test
    public void createSuppressAndRetrieveWithUserAndNoChannel() throws MMXException {
        doCreateSuppressAndRetrieve("app1", "u2", null);
    }
    @Test
    public void createSuppressAndRetrieveWNoUserAndNoChannel() throws MMXException {
        doCreateSuppressAndRetrieve("app2", null, null);
    }
    @Test
    public void createSuppressAndRetrieveNoUserAndWithChannel() throws MMXException {
        doCreateSuppressAndRetrieve("app3", null, "ch1");
    }
    private void doCreateSuppressAndRetrieve(String appId, String userId, String channelName) throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId(appId);
        suppress.setUserId(userId);
        suppress.setChannelName(channelName);
        suppress = MMXPushConfigService.getInstance().createPushSuppress(suppress);
        Assert.assertEquals(appId, suppress.getAppId());
        Assert.assertEquals(userId, suppress.getUserId());
        Assert.assertEquals(channelName, suppress.getChannelName());
        Assert.assertTrue(suppress.getSuppressId() > 0);

        MMXPushSuppress suppress2 = MMXPushConfigService.getInstance().getPushSuppress(suppress.getSuppressId());
        assertSuppress(suppress, suppress2);

        Collection<MMXPushSuppress> suppressList = MMXPushConfigService.getInstance().getPushSuppressForAppAndUser(suppress.getAppId(), suppress.getUserId());
        Assert.assertNotNull(suppressList);
        Assert.assertEquals(1, suppressList.size());
        for (MMXPushSuppress s : suppressList) {
            assertSuppress(suppress, s);
        }
    }
    @Test
    public void createSuppressAndDeleteNoUserNoChannel(String appId, String userId, String channelName) throws MMXException {
        doCreateSuppressAndDelete("app-d-1", null, null);
    }
    public void createSuppressAndDeleteWithUserNoChannel(String appId, String userId, String channelName) throws MMXException {
        doCreateSuppressAndDelete("app-d-2", "u1", null);
    }
    public void createSuppressAndDeleteNoUserWithChannel(String appId, String userId, String channelName) throws MMXException {
        doCreateSuppressAndDelete("app-d-3", null, "ch1");
    }
    public void createSuppressAndDelete(String appId, String userId, String channelName) throws MMXException {
        doCreateSuppressAndDelete("app-d-4", "u1", "ch1");
    }
    public void doCreateSuppressAndDelete(String appId, String userId, String channelName) throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId(appId);
        suppress.setUserId(userId);
        suppress.setChannelName(channelName);
        suppress = MMXPushConfigService.getInstance().createPushSuppress(suppress);
        Assert.assertEquals(appId, suppress.getAppId());
        Assert.assertEquals(userId, suppress.getUserId());
        Assert.assertEquals(channelName, suppress.getChannelName());
        Assert.assertTrue(suppress.getSuppressId() > 0);

        MMXPushSuppress suppress2 = MMXPushConfigService.getInstance().getPushSuppress(suppress.getSuppressId());
        assertSuppress(suppress, suppress2);

        MMXPushConfigService.getInstance().deletePushSuppress(suppress2);

        //make sure there is no record
        boolean recordFound = false;
        try {
            MMXPushConfigService.getInstance().getPushSuppress(suppress.getSuppressId());
            recordFound = true;
        } catch (MMXException e) {
            recordFound = false;
        }
        if (recordFound) {
            Assert.fail("suppress record found after delete");
        }


        Collection<MMXPushSuppress> suppressList = MMXPushConfigService.getInstance().getPushSuppressForAppAndUser(suppress.getAppId(), suppress.getUserId());
        Assert.assertTrue(suppressList == null || suppressList.size() == 0);
    }


    private static void assertSuppress(MMXPushSuppress expected, MMXPushSuppress actual) {

        if (expected == null && actual == null) {
            return;
        }
        if (expected == null) {
            Assert.fail("expected null object");
        }
        if (actual == null) {
            Assert.fail("expected not null but actual object is null");
        }

        Assert.assertEquals(expected.getSuppressId(), actual.getSuppressId());
        Assert.assertEquals(expected.getAppId(), actual.getAppId());
        Assert.assertEquals(expected.getAppId(), actual.getAppId());
        Assert.assertEquals(expected.getChannelName(), actual.getChannelName());
    }

}

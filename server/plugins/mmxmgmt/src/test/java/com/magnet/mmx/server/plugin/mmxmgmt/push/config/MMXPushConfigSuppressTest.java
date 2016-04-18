package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushSuppress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

/**
 * Created by mmicevic on 4/13/16.
 *
 */
public class MMXPushConfigSuppressTest {

    private static final String APP_ID = "test-app";

    @Before
    public void cleanUp() throws MMXException {
        PushConfigTestUtil.deleteAllDataForApp(APP_ID);
    }


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
        suppress.setChannelId("ch1");
        MMXPushConfigService.getInstance().createPushSuppress(suppress);
    }
    @Test(expected = MMXException.class)
    public void createSuppressAppIdEmpty() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId("");
        suppress.setUserId("u1");
        suppress.setChannelId("ch1");
        MMXPushConfigService.getInstance().createPushSuppress(suppress);
    }
    @Test(expected = MMXException.class)
    public void createSuppressAppIdSpace() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId("   ");
        suppress.setUserId("u1");
        suppress.setChannelId("ch1");
        MMXPushConfigService.getInstance().createPushSuppress(suppress);
    }

    @Test
    public void createSuppressAppIdCorrectAll() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId(APP_ID);
        suppress.setUserId("u1");
        suppress.setChannelId("ch1");
        suppress = MMXPushConfigService.getInstance().createPushSuppress(suppress);
        Assert.assertEquals(APP_ID, suppress.getAppId());
        Assert.assertEquals("u1", suppress.getUserId());
        Assert.assertEquals("ch1", suppress.getChannelId());
        Assert.assertTrue(suppress.getSuppressId() > 0);
    }
    @Test
    public void createSuppressAppIdCorrectAppId() throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId(APP_ID);
        suppress.setUserId(null);
        suppress.setChannelId(null);
        suppress = MMXPushConfigService.getInstance().createPushSuppress(suppress);
        Assert.assertEquals(APP_ID, suppress.getAppId());
        Assert.assertNull(suppress.getUserId());
        Assert.assertNull(suppress.getChannelId());
        Assert.assertTrue(suppress.getSuppressId() > 0);
    }
    @Test
    public void createSuppressAndRetrieveWithUserAndChannel() throws MMXException {
        doCreateSuppressAndRetrieve(APP_ID, "u1", "ch1");
    }
    @Test
    public void createSuppressAndRetrieveWithUserAndNoChannel() throws MMXException {
        doCreateSuppressAndRetrieve(APP_ID, "u2", null);
    }
    @Test
    public void createSuppressAndRetrieveWNoUserAndNoChannel() throws MMXException {
        doCreateSuppressAndRetrieve(APP_ID, null, null);
    }
    @Test
    public void createSuppressAndRetrieveNoUserAndWithChannel() throws MMXException {
        doCreateSuppressAndRetrieve(APP_ID, null, "ch1");
    }
    private void doCreateSuppressAndRetrieve(String appId, String userId, String channelName) throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId(appId);
        suppress.setUserId(userId);
        suppress.setChannelId(channelName);
        suppress = MMXPushConfigService.getInstance().createPushSuppress(suppress);
        Assert.assertEquals(appId, suppress.getAppId());
        Assert.assertEquals(userId, suppress.getUserId());
        Assert.assertEquals(channelName, suppress.getChannelId());
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
        doCreateSuppressAndDelete(APP_ID, null, null);
    }
    public void createSuppressAndDeleteWithUserNoChannel(String appId, String userId, String channelName) throws MMXException {
        doCreateSuppressAndDelete(APP_ID, "u1", null);
    }
    public void createSuppressAndDeleteNoUserWithChannel(String appId, String userId, String channelName) throws MMXException {
        doCreateSuppressAndDelete(APP_ID, null, "ch1");
    }
    public void createSuppressAndDelete(String appId, String userId, String channelName) throws MMXException {
        doCreateSuppressAndDelete(APP_ID, "u1", "ch1");
    }
    public void doCreateSuppressAndDelete(String appId, String userId, String channelName) throws MMXException {
        MMXPushSuppress suppress = new MMXPushSuppress();
        suppress.setAppId(appId);
        suppress.setUserId(userId);
        suppress.setChannelId(channelName);
        suppress = MMXPushConfigService.getInstance().createPushSuppress(suppress);
        Assert.assertEquals(appId, suppress.getAppId());
        Assert.assertEquals(userId, suppress.getUserId());
        Assert.assertEquals(channelName, suppress.getChannelId());
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
        Collection<MMXPushSuppress> suppressList = MMXPushConfigService.getInstance().getPushSuppressForAppAndUser(appId, userId);
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
        Assert.assertEquals(expected.getChannelId(), actual.getChannelId());
    }
}

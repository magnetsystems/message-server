package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplateType;
import org.junit.*;

/**
 * Created by mmicevic on 4/1/16.
 *
 */
@Ignore
public class MMXPushConfigServiceTempateTest {

    private static final String APP_ID = "test-app";

    @BeforeClass
    public static void init() {
        HibernateTestInitalizer.getInstance();
    }

    @Before
    public void cleanUp() throws MMXException {
        PushConfigTestUtil.deleteAllDataForApp(APP_ID);
    }

    //validation
    @Test(expected = MMXException.class)
    public void createTemplateNull() throws MMXException {
        MMXPushConfigService.getInstance().createTemplate(null);
    }
    @Test(expected = MMXException.class)
    public void createTemplateEmpty() throws MMXException {
        MMXTemplate t = new MMXTemplate();
        MMXPushConfigService.getInstance().createTemplate(t);
    }

    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_1_null() throws MMXException {
        createTemplate(null, MMXTemplateType.PUSH, "nn", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_1_empty() throws MMXException {
        createTemplate("", MMXTemplateType.PUSH, "nn", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_1_space() throws MMXException {
        createTemplate("   ", MMXTemplateType.PUSH, "nn", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_2_null() throws MMXException {
        createTemplate2(null, MMXTemplateType.PUSH, "nn", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_2_empty() throws MMXException {
        createTemplate2("", MMXTemplateType.PUSH, "nn", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingAppId_2_space() throws MMXException {
        createTemplate2("   ", MMXTemplateType.PUSH, "nn", "tt");
    }

    @Test(expected = MMXException.class)
    public void createTemplateMissingType_1() throws MMXException {
        createTemplate(APP_ID  , null, "nn", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingType_2() throws MMXException {
        createTemplate2(APP_ID  , null, "nn", "tt");
    }

    @Test(expected = MMXException.class)
    public void createTemplateMissingName_1_null() throws MMXException {
        createTemplate(APP_ID  , MMXTemplateType.PUSH, null, "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_1_empty() throws MMXException {
        createTemplate(APP_ID  , MMXTemplateType.PUSH, "", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_1_space() throws MMXException {
        createTemplate(APP_ID  , MMXTemplateType.PUSH, "  ", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_2_null() throws MMXException {
        createTemplate2(APP_ID  , MMXTemplateType.PUSH, null, "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_2_empty() throws MMXException {
        createTemplate2(APP_ID  , MMXTemplateType.PUSH, "", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_2_space() throws MMXException {
        createTemplate2(APP_ID  , MMXTemplateType.PUSH, "  ", "tt");
    }

    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_1_null() throws MMXException {
        createTemplate(APP_ID  , MMXTemplateType.PUSH, "nn", null);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_1_empty() throws MMXException {
        createTemplate(APP_ID  , MMXTemplateType.PUSH, "nn", "");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_1_space() throws MMXException {
        createTemplate(APP_ID  , MMXTemplateType.PUSH, "nn", "   ");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_2_null() throws MMXException {
        createTemplate2(APP_ID  , MMXTemplateType.PUSH, "nn", null);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_2_empty() throws MMXException {
        createTemplate2(APP_ID  , MMXTemplateType.PUSH, "nn", "");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_2_space() throws MMXException {
        createTemplate2(APP_ID  , MMXTemplateType.PUSH, "nn", "   ");
    }



    private static MMXTemplate createTemplate(String appId, MMXTemplateType type, String name, String template) throws MMXException {
        MMXTemplate t = new MMXTemplate();
        t.setAppId(appId);
        t.setTemplateType(type);
        t.setTemplateName(name);
        t.setTemplate(template);
        MMXPushConfigService.getInstance().createTemplate(t);
        return t;
    }

    private static MMXTemplate createTemplate2(String appId, MMXTemplateType type, String name, String template) throws MMXException {
        return MMXPushConfigService.getInstance().createTemplate(appId, name, type, template);
    }

    @Test
    public void createAndRetrieveTemplate() throws MMXException {
        MMXTemplate t = createTemplate(APP_ID  , MMXTemplateType.PUSH, "nn", "tt");
        assertTemplate(t, APP_ID  , MMXTemplateType.PUSH, "nn", "tt");
        MMXTemplate t2 = MMXPushConfigService.getInstance().getTemplate(t.getTemplateId());
        assertTemplate(t2, APP_ID  , MMXTemplateType.PUSH, "nn", "tt");
        MMXTemplate t3 = MMXPushConfigService.getInstance().getTemplate(t.getAppId(), t.getTemplateName());
        assertTemplate(t3, APP_ID  , MMXTemplateType.PUSH, "nn", "tt");
    }
    @Test
    public void createAndUpdateTemplateName() throws MMXException {
        MMXTemplate t = createTemplate(APP_ID  , MMXTemplateType.PUSH, "nn", "tt");
        t.setTemplateName("nn2");
        //
        MMXPushConfigService.getInstance().updateTemplate(t);
        assertTemplate(t, APP_ID  , MMXTemplateType.PUSH, "nn2", "tt");
        MMXTemplate t2 = MMXPushConfigService.getInstance().getTemplate(t.getTemplateId());
        assertTemplate(t2, APP_ID  , MMXTemplateType.PUSH, "nn2", "tt");
        MMXTemplate t3 = MMXPushConfigService.getInstance().getTemplate(t.getAppId(), t.getTemplateName());
        assertTemplate(t3, APP_ID  , MMXTemplateType.PUSH, "nn2", "tt");
        //make sure there is no old record
        assertTemplateNotFound(t.getAppId(), "nn");
//        MMXTemplate t4 = MMXPushConfigService.getInstance().getTemplate(t.getAppId(), "nn");
//        Assert.assertNull(t4);
    }
    @Test
    public void createAndUpdateTemplate() throws MMXException {
        MMXTemplate t = createTemplate(APP_ID  , MMXTemplateType.PUSH, "nn", "tt");
        t.setTemplate("tt2");
        //
        MMXPushConfigService.getInstance().updateTemplate(t);
        assertTemplate(t, APP_ID  , MMXTemplateType.PUSH, "nn", "tt2");
        MMXTemplate t2 = MMXPushConfigService.getInstance().getTemplate(t.getTemplateId());
        assertTemplate(t2, APP_ID  , MMXTemplateType.PUSH, "nn", "tt2");
        MMXTemplate t3 = MMXPushConfigService.getInstance().getTemplate(t.getAppId(), t.getTemplateName());
        assertTemplate(t3, APP_ID  , MMXTemplateType.PUSH, "nn", "tt2");
    }
    @Test
    public void createAndDeleteTemplate() throws MMXException {
        MMXTemplate t = createTemplate(APP_ID  , MMXTemplateType.PUSH, "nn", "tt");
        assertTemplate(t, APP_ID  , MMXTemplateType.PUSH, "nn", "tt");
        MMXTemplate t2 = MMXPushConfigService.getInstance().getTemplate(t.getTemplateId());
        assertTemplate(t2, APP_ID  , MMXTemplateType.PUSH, "nn", "tt");

        MMXPushConfigService.getInstance().deleteTemplate(t.getTemplateId());
        assertTemplateNotFound(t.getAppId(), t.getTemplateName());
//        MMXTemplate t3 = MMXPushConfigService.getInstance().getTemplate(t.getAppId(), t.getTemplateName());
//        Assert.assertNull(t3);
    }
    private void assertTemplate(MMXTemplate t, String appId, MMXTemplateType type, String name, String template) throws MMXException {
        Assert.assertNotNull(t);
        Assert.assertEquals(appId, t.getAppId());
        Assert.assertEquals(type, t.getTemplateType());
        Assert.assertEquals(name, t.getTemplateName());
        Assert.assertEquals(template, t.getTemplate());
    }
    private static void assertTemplateNotFound(Integer templateId) {

        boolean found = false;
        try {
            MMXPushConfigService.getInstance().getTemplate(templateId);
            found = true;
        }
        catch (MMXException e) {
            e.printStackTrace();
        }
        if (found) {
            Assert.fail("expecting 'template not found' for id = " + templateId);
        }
    }
    private static void assertTemplateNotFound(String appId, String tempateName) {

        boolean found = false;
        try {
            MMXPushConfigService.getInstance().getTemplate(appId, tempateName);
            found = true;
        }
        catch (MMXException e) {
            e.printStackTrace();
        }
        if (found) {
            Assert.fail("expecting 'template not found' for id = " + appId + "/" + tempateName);
        }
    }}

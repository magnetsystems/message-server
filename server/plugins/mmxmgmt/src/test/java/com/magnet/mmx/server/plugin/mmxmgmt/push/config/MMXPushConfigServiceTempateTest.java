package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplateType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by mmicevic on 4/1/16.
 *
 */
public class MMXPushConfigServiceTempateTest {

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
        createTemplate("aa", null, "nn", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingType_2() throws MMXException {
        createTemplate2("aa", null, "nn", "tt");
    }

    @Test(expected = MMXException.class)
    public void createTemplateMissingName_1_null() throws MMXException {
        createTemplate("aa", MMXTemplateType.PUSH, null, "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_1_empty() throws MMXException {
        createTemplate("aa", MMXTemplateType.PUSH, "", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_1_space() throws MMXException {
        createTemplate("aa", MMXTemplateType.PUSH, "  ", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_2_null() throws MMXException {
        createTemplate2("aa", MMXTemplateType.PUSH, null, "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_2_empty() throws MMXException {
        createTemplate2("aa", MMXTemplateType.PUSH, "", "tt");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingName_2_space() throws MMXException {
        createTemplate2("aa", MMXTemplateType.PUSH, "  ", "tt");
    }

    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_1_null() throws MMXException {
        createTemplate("aa", MMXTemplateType.PUSH, "nn", null);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_1_empty() throws MMXException {
        createTemplate("aa", MMXTemplateType.PUSH, "nn", "");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_1_space() throws MMXException {
        createTemplate("aa", MMXTemplateType.PUSH, "nn", "   ");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_2_null() throws MMXException {
        createTemplate2("aa", MMXTemplateType.PUSH, "nn", null);
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_2_empty() throws MMXException {
        createTemplate2("aa", MMXTemplateType.PUSH, "nn", "");
    }
    @Test(expected = MMXException.class)
    public void createTemplateMissingTemplate_2_space() throws MMXException {
        createTemplate2("aa", MMXTemplateType.PUSH, "nn", "   ");
    }



    private static MMXTemplate createTemplate(String appId, MMXTemplateType type, String name, String template) throws MMXException {
        MMXTemplate t = new MMXTemplate();
        t.setAppId(appId);
        t.setTemplateType(type);
        t.setTemplateName(name);
        t.setTemplate(template);
        return MMXPushConfigService.getInstance().createTemplate(t);
    }

    private static MMXTemplate createTemplate2(String appId, MMXTemplateType type, String name, String template) throws MMXException {
        return MMXPushConfigService.getInstance().createTemplate(appId, name, type, template);
    }

    @Test
    public void createAndRetrieveTemplate() throws MMXException {
        MMXTemplate t = createTemplate("aa", MMXTemplateType.PUSH, "nn", "tt");
        assertTemplate(t, "aa", MMXTemplateType.PUSH, "nn", "tt");
        MMXTemplate t2 = MMXPushConfigService.getInstance().getTemplate(t.getTemplateId());
        assertTemplate(t2, "aa", MMXTemplateType.PUSH, "nn", "tt");
        MMXTemplate t3 = MMXPushConfigService.getInstance().getTemplate(t.getAppId(), t.getTemplateName());
        assertTemplate(t3, "aa", MMXTemplateType.PUSH, "nn", "tt");
    }
    @Test
    public void createAndUpdateTemplateName() throws MMXException {
        MMXTemplate t = createTemplate("aa", MMXTemplateType.PUSH, "nn", "tt");
        t.setTemplateName("nn2");
        //
        t = MMXPushConfigService.getInstance().updateTemplate(t);
        assertTemplate(t, "aa", MMXTemplateType.PUSH, "nn2", "tt");
        MMXTemplate t2 = MMXPushConfigService.getInstance().getTemplate(t.getTemplateId());
        assertTemplate(t2, "aa", MMXTemplateType.PUSH, "nn2", "tt");
        MMXTemplate t3 = MMXPushConfigService.getInstance().getTemplate(t.getAppId(), t.getTemplateName());
        assertTemplate(t3, "aa", MMXTemplateType.PUSH, "nn2", "tt");
        //make sure there is no old record
        assertTemplateNotFound(t.getAppId(), "nn");
//        MMXTemplate t4 = MMXPushConfigService.getInstance().getTemplate(t.getAppId(), "nn");
//        Assert.assertNull(t4);
    }
    @Test
    public void createAndUpdateTemplate() throws MMXException {
        MMXTemplate t = createTemplate("aa", MMXTemplateType.PUSH, "nn", "tt");
        t.setTemplate("tt2");
        //
        t = MMXPushConfigService.getInstance().updateTemplate(t);
        assertTemplate(t, "aa", MMXTemplateType.PUSH, "nn", "tt2");
        MMXTemplate t2 = MMXPushConfigService.getInstance().getTemplate(t.getTemplateId());
        assertTemplate(t2, "aa", MMXTemplateType.PUSH, "nn", "tt2");
        MMXTemplate t3 = MMXPushConfigService.getInstance().getTemplate(t.getAppId(), t.getTemplateName());
        assertTemplate(t3, "aa", MMXTemplateType.PUSH, "nn", "tt2");
    }
    @Test
    public void createAndDeleteTemplate() throws MMXException {
        MMXTemplate t = createTemplate("aa", MMXTemplateType.PUSH, "nn", "tt");
        assertTemplate(t, "aa", MMXTemplateType.PUSH, "nn", "tt");
        MMXTemplate t2 = MMXPushConfigService.getInstance().getTemplate(t.getTemplateId());
        assertTemplate(t2, "aa", MMXTemplateType.PUSH, "nn", "tt");

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
    private static void assertTemplateNotFound(int templateId) {

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

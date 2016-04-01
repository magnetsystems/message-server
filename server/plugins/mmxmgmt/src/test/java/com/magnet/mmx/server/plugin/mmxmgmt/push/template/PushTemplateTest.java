package com.magnet.mmx.server.plugin.mmxmgmt.push.template;

import com.magnet.mmx.server.plugin.mmxmgmt.push.template.model.MMXTemplate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by mmicevic on 4/1/16.
 *
 */
public class PushTemplateTest {

    @Test
    public void checkDefultTemplate() {

        MMXTemplate t = MMXPushConfigService.getInstance().getTemplate(MMXPushConfigService.SYSTEM_APP, MMXPushConfigService.DEFAULT_TEMPLATE);
        Assert.assertNotNull(t);
        Assert.assertEquals(MMXPushConfigService.SYSTEM_APP, t.getAppId());
        Assert.assertEquals(MMXPushConfigService.DEFAULT_TEMPLATE, t.getTemplateName());
        Assert.assertNotNull(t.getTemplate());
    }
}

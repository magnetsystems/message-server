package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao;


import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXTemplateDo;

import java.util.Collection;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public interface MMXTemplateDao {

    public MMXTemplateDo createTemplate(MMXTemplateDo template);
    public Collection<MMXTemplateDo> getAllTemplates(String appId);
    public MMXTemplateDo getTemplate(String appId, String templateName);
    public MMXTemplateDo getTemplate(int templateId);
    public MMXTemplateDo updateTemplate(MMXTemplateDo template);
    public void deleteTemplate(MMXTemplateDo template);
}
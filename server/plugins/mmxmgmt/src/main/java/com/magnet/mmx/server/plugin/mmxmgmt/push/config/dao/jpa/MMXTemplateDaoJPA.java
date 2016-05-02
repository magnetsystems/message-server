package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.jpa;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXTemplateDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXTemplateDo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXTemplateDaoJPA extends JPABase<MMXTemplateDo> implements MMXTemplateDao {

    public MMXTemplateDaoJPA() {
        super(MMXTemplateDo.class);
    }

    @Override
    public void createTemplate(MMXTemplateDo template) {
        save(template);
    }

    @Override
    public Collection<MMXTemplateDo> getAllTemplates(String appId) {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        return findManyByCriteria(params);
    }

    @Override
    public MMXTemplateDo getTemplate(String appId, String templateName) {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        params.put("templateName", templateName);
        return findSingleByCriteria(params);
    }

    @Override
    public MMXTemplateDo getTemplate(Integer templateId) {
        return findById(templateId);
    }

    @Override
    public void updateTemplate(MMXTemplateDo template) {
        if (template.getTemplateId() != null) {
            update(template);
        } else {
            save(template);
        }
    }

    @Override
    public void deleteTemplate(MMXTemplateDo template) {
        delete(template);
    }
}

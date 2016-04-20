package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXTemplateDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXTemplateDo;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXTemplateDaoHbn extends HibernateBase<MMXTemplateDo> implements MMXTemplateDao {

    public MMXTemplateDaoHbn() {
        super(MMXTemplateDo.class);
    }

    @Override
    public MMXTemplateDo createTemplate(MMXTemplateDo template) {
        save(template);
        return template;
    }

    @Override
    public Collection<MMXTemplateDo> getAllTemplates(String appId) {
        return findManyByCriteria(Restrictions.eq("appId", appId));
    }

    @Override
    public MMXTemplateDo getTemplate(String appId, String templateName) {
        return findSingleByCriteria(Restrictions.eq("appId", appId), Restrictions.eq("templateName", templateName));
    }

    @Override
    public MMXTemplateDo getTemplate(Integer templateId) {
        return findById(templateId);
    }

    @Override
    public MMXTemplateDo updateTemplate(MMXTemplateDo template) {
        update(template);
        return template;
    }

    @Override
    public void deleteTemplate(MMXTemplateDo template) {
        delete(template);
    }
}

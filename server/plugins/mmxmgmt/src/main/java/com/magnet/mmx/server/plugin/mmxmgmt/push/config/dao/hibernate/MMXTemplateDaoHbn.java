package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.MMXTemplateDao;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXTemplateDo;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class MMXTemplateDaoHbn extends HibernateBase implements MMXTemplateDao {

    @Override
    public MMXTemplateDo createTemplate(MMXTemplateDo template) {
        openCurrentSessionwithTransaction();
        getCurrentSession().save(template);
        closeCurrentSessionwithTransaction();
        return template;
    }

    @Override
    public Collection<MMXTemplateDo> getAllTemplates(String appId) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXTemplateDo.class);
        Collection<MMXTemplateDo> list = (Collection<MMXTemplateDo>) criteria
                .add(Restrictions.eq("appId", appId))
                .list();
        closeCurrentSession();
        return list;
    }

    @Override
    public MMXTemplateDo getTemplate(String appId, String templateName) {
        openCurrentSession();
        Criteria criteria = getCurrentSession().createCriteria(MMXTemplateDo.class);
        MMXTemplateDo template = (MMXTemplateDo) criteria
                .add(Restrictions.eq("appId", appId))
                .add(Restrictions.eq("templateName", templateName))
                .uniqueResult();
        closeCurrentSession();
        return template;
    }

    @Override
    public MMXTemplateDo getTemplate(Integer templateId) {
        openCurrentSession();
        MMXTemplateDo template = (MMXTemplateDo) getCurrentSession().get(MMXTemplateDo.class, templateId);
        closeCurrentSession();
        return template;
    }

    @Override
    public MMXTemplateDo updateTemplate(MMXTemplateDo template) {
        openCurrentSessionwithTransaction();
        getCurrentSession().saveOrUpdate(template);
        closeCurrentSessionwithTransaction();
        return template;
    }

    @Override
    public void deleteTemplate(MMXTemplateDo template) {
        openCurrentSessionwithTransaction();
        getCurrentSession().delete(template);
        closeCurrentSessionwithTransaction();
    }
}

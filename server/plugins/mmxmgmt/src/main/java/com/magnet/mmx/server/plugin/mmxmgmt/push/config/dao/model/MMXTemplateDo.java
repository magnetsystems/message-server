package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by mmicevic on 3/31/16.
 *
 */

@Entity
@Table(name = "mmxTemplate")
public class MMXTemplateDo implements Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer templateId;
    private String appId;
    private String templateType;
    private String templateName;
    private String template;

    public Integer getTemplateId() {
        return templateId;
    }
    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public String getAppId() {
        return appId;
    }
    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getTemplateType() {
        return templateType;
    }
    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getTemplateName() {
        return templateName;
    }
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplate() {
        return template;
    }
    public void setTemplate(String template) {
        this.template = template;
    }
}

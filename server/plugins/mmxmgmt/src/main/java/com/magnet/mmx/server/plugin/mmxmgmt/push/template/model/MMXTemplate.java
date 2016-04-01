package com.magnet.mmx.server.plugin.mmxmgmt.push.template.model;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXTemplate {

    private int templateId;
    private String appId;
    private String templateType;
    private String templateName;
    private String template;

    public int getTemplateId() {
        return templateId;
    }
    public void setTemplateId(int templateId) {
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

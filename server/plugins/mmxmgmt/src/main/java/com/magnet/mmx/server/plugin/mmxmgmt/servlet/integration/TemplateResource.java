package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplateType;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mmicevic on 4/6/16.
 *
 */
@Path("/integration/templates")
public class TemplateResource {

    @POST
//    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTemplate(TemplateRequest request) {

        RestMethod<TemplateRequest, TemplateResponse> method = new RestMethod<TemplateRequest, TemplateResponse>() {
            @Override
            public TemplateResponse execute(TemplateRequest request) throws MMXException {
                //convert request
                MMXTemplate t = convertRequest(request);

                //do job
                t = MMXPushConfigService.getInstance().createTemplate(t);

                //convert and return response
                return convertResponse(t);
            }
        };
        return method.doMethod(request);

    }
    @GET
    @Path("/{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrieveTemplateById(@PathParam("templateId") Integer templateId) {

        RestMethod<Integer, TemplateResponse> method = new RestMethod<Integer, TemplateResponse>() {
            @Override
            public TemplateResponse execute(Integer templateId) throws MMXException {

                //convert request
                //do job
                MMXTemplate t = MMXPushConfigService.getInstance().getTemplate(templateId);

                //convert and return response
                return convertResponse(t);
            }
        };
        return method.doMethod(templateId);
    }
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrieveAllTemplatesForApp(@QueryParam("appId") String appId) {

        RestMethod<String, Collection<TemplateResponse>> method = new RestMethod<String,Collection<TemplateResponse>>() {
            @Override
            public Collection<TemplateResponse> execute(String appId) throws MMXException {

                //convert request
                //do job
                Collection<MMXTemplate> t = MMXPushConfigService.getInstance().getAllTemplates(appId);

                //convert and return response
                return convertResponse(t);
            }
        };
        return method.doMethod(appId);
    }
    @PUT
    @Path("/{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTemplate(@PathParam("templateId") final Integer templateId, TemplateRequest request) {

        RestMethod<TemplateRequest, TemplateResponse> method = new RestMethod<TemplateRequest, TemplateResponse>() {
            @Override
            public TemplateResponse execute(TemplateRequest request) throws MMXException {
                //convert request
                MMXTemplate t = convertRequest(request);
                t.setTemplateId(templateId);

                //do job
                t = MMXPushConfigService.getInstance().updateTemplate(t);

                //convert and return response
                return convertResponse(t);
            }
        };
        return method.doMethod(request);
    }
    @DELETE
    @Path("/{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteTemplate(@PathParam("templateId") Integer templateId) {

        RestMethod<Integer, RestMethod.SimpleMessage> method = new RestMethod<Integer,RestMethod.SimpleMessage>() {
            @Override
            public SimpleMessage execute(Integer templateId) throws MMXException {
                //convert request

                //do job
                MMXPushConfigService.getInstance().deleteTemplate(templateId);

                //convert and return response
                return new RestMethod.SimpleMessage("deleted");
            }
        };
        return method.doMethod(templateId);
    }

    private static MMXTemplate convertRequest(TemplateRequest request) {

        MMXTemplate t = new MMXTemplate();
        t.setAppId(StringUtils.isBlank(request.appId) ? null : request.appId);
        t.setTemplateName(StringUtils.isBlank(request.templateName) ? null : request.templateName);
        t.setTemplate(StringUtils.isBlank(request.template) ? null : request.template);
        t.setTemplateType(MMXTemplateType.PUSH);
        return t;
    }
    private static TemplateResponse convertResponse(MMXTemplate response) {

        TemplateResponse t = new TemplateResponse();
        t.templateId = response.getTemplateId();
        t.appId = response.getAppId();
        t.templateName = response.getTemplateName();
        t.template = response.getTemplate();
        t.templateType = response.getTemplateType().name();
        return t;
    }
    private static Collection<TemplateResponse> convertResponse(Collection<MMXTemplate> response) {

        if (response == null) {
            return null;
        }
        List<TemplateResponse> list = new ArrayList<>();
        for (MMXTemplate t : response) {
            list.add(convertResponse(t));
        }
        return list;
    }

    // REQUEST / RESPONSE
    public static class TemplateRequest {

        String appId;
        String templateName;
        String template;

        public String getAppId() {
            return appId;
        }
        public void setAppId(String appId) {
            this.appId = appId;
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

    public static class TemplateResponse {

        Integer templateId;
        String appId;
        String templateType;
        String templateName;
        String template;

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
}

package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplateType;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by mmicevic on 4/6/16.
 *
 */

@Path("/integration/templates")
public class TemplateResource {

    @POST
    @Path("/template")
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
    @Path("/template/{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrieveTemplateById(@PathParam("templateId") int templateId) {

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
    @PUT
    @Path("/template/{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTemplate(@PathParam("templateId") final int templateId, TemplateRequest request) {

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
    @Path("/template/{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteTemplate(@PathParam("templateId") final int templateId) {

        RestMethod<Integer, Object> method = new RestMethod<Integer, Object>() {
            @Override
            public Object execute(Integer templateId) throws MMXException {
                //convert request

                //do job
                MMXPushConfigService.getInstance().deleteTemplate(templateId);

                //convert and return response
                return new Object();
            }
        };
        return method.doMethod(templateId);
    }



    private static MMXTemplate convertRequest(TemplateRequest request) {

        MMXTemplate t = new MMXTemplate();
        t.setAppId(request.appId);
        t.setTemplateName(request.templateName);
        t.setTemplate(request.template);
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

    public static class TemplateRequest {

        String appId;
        String templateName;
        String template;
    }

    public static class TemplateResponse {

        int templateId;
        String appId;
        String templateType;
        String templateName;
        String template;
    }
}

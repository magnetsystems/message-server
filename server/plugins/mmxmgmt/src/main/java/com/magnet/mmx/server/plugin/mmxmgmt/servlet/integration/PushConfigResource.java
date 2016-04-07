package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmicevic on 4/6/16.
 *
 */
@Path("/integration/push/configs")
public class PushConfigResource {

    @POST
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPushConfig(PushConfigRequest request) {

        RestMethod<PushConfigRequest, PushConfigResponse> method = new RestMethod<PushConfigRequest, PushConfigResponse>() {
            @Override
            public PushConfigResponse execute(PushConfigRequest request) throws MMXException {
                //convert request
                MMXPushConfig c = convertRequest(request);

                //do job
                c = MMXPushConfigService.getInstance().createConfig(c);

                //convert and return response
                return convertResponse(c);
            }
        };
        return method.doMethod(request);
    }
    @GET
    @Path("/config/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrievePushConfig(@PathParam("configId") int configId) {

        RestMethod<Integer, PushConfigResponse> method = new RestMethod<Integer, PushConfigResponse>() {
            @Override
            public PushConfigResponse execute(Integer configId) throws MMXException {
                //convert request

                //do job
                MMXPushConfig c = MMXPushConfigService.getInstance().getConfig(configId);

                //convert and return response
                return convertResponse(c);
            }
        };
        return method.doMethod(configId);
    }
    @PUT
    @Path("/config/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePushConfig(@PathParam("configId") final int configId, PushConfigRequest request) {

        RestMethod<PushConfigRequest, PushConfigResponse> method = new RestMethod<PushConfigRequest, PushConfigResponse>() {
            @Override
            public PushConfigResponse execute(PushConfigRequest request) throws MMXException {
                //convert request
                MMXPushConfig c = convertRequest(request);
                c.setConfigId(configId);

                //do job
                c = MMXPushConfigService.getInstance().updateConfig(c);

                //convert and return response
                return convertResponse(c);
            }
        };
        return method.doMethod(request);
    }
    @DELETE
    @Path("/config/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePushConfig(@PathParam("configId") int configId) {

        RestMethod<Integer, Object> method = new RestMethod<Integer, Object>() {
            @Override
            public Object execute(Integer configId) throws MMXException {
                //convert request

                //do job
                MMXPushConfigService.getInstance().deleteConfig(configId);

                //convert and return response
                return new Object();
            }
        };
        return method.doMethod(configId);
    }


    private static MMXPushConfig convertRequest(PushConfigRequest request) throws MMXException {

        MMXPushConfig c = new MMXPushConfig();
        c.setAppId(request.appId);
        c.setConfigName(request.configName);
        c.setIsSilentPush(request.isSilentPush);
        MMXTemplate t =  MMXPushConfigService.getInstance().getTemplate(request.appId, request.templateName);
        c.setTemplate(t);
        c.setMeta(request.meta);
        return c;
    }
    private static PushConfigResponse convertResponse(MMXPushConfig c) {

        PushConfigResponse response = new PushConfigResponse();
        response.configId = c.getConfigId();
        response.appId = c.getAppId();
        response.configName = c.getConfigName();
        response.isSilentPush = c.isSilentPush();
        response.templateName = c.getTemplate().getTemplateName();
        response.meta = c.getMeta();
        return response;
    }

    public static class PushConfigRequest {

        String appId;
        String configName;
        String templateName;
        boolean isSilentPush;
        Map<String, String> meta = new HashMap<>();
    }

    public static class PushConfigResponse {

        int configId;
        String appId;
        String configName;
        String templateName;
        boolean isSilentPush;
        Map<String, String> meta = new HashMap<>();
    }
}

package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Created by mmicevic on 4/6/16.
 *
 */
@Path("/integration/pushconfigs")
public class PushConfigResource {

    @POST
    //@Path("/config")
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
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrieveAllPushConfigsForApp(@QueryParam("appId") String appId) {

        RestMethod<String, Collection<PushConfigResponse>> method = new RestMethod<String,Collection<PushConfigResponse>>() {
            @Override
            public Collection<PushConfigResponse> execute(String appId) throws MMXException {
                //convert request

                //do job
                Collection<MMXPushConfig> c = MMXPushConfigService.getInstance().getAllConfigs(appId);

                //convert and return response
                return convertResponse(c);
            }
        };
        return method.doMethod(appId);
    }
    @GET
    @Path("/{configId}")
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
    @Path("/{configId}")
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
//                transaction
                c = MMXPushConfigService.getInstance().updateConfig(c);
                //convert and return response
                return convertResponse(c);
            }
        };
        return method.doMethod(request);
    }
    @DELETE
    @Path("/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePushConfig(@PathParam("configId") int configId) {

        RestMethod<Integer, RestMethod.SimpleMessage> method = new RestMethod<Integer, RestMethod.SimpleMessage>() {
            @Override
            public RestMethod.SimpleMessage execute(Integer configId) throws MMXException {
                //convert request

                //do job
                MMXPushConfigService.getInstance().deleteConfig(configId);

                //convert and return response
                return new RestMethod.SimpleMessage("deleted");
            }
        };
        return method.doMethod(configId);
    }


    private static MMXPushConfig convertRequest(PushConfigRequest request) throws MMXException {

        MMXPushConfig c = new MMXPushConfig();
        c.setAppId(request.appId);
        c.setConfigName(request.configName);
        c.setSilentPush(request.silentPush);
        c.setEnabled(request.enabled);
        MMXTemplate t =  MMXPushConfigService.getInstance().getTemplate(request.templateId);
        c.setTemplate(t);
        c.setMeta(request.meta);
        if (request.getChannelNames() != null) {
            c.getChannelNames().addAll(request.getChannelNames());
        }
        return c;
    }
    private static PushConfigResponse convertResponse(MMXPushConfig c) {

        PushConfigResponse response = new PushConfigResponse();
        response.configId = c.getConfigId();
        response.appId = c.getAppId();
        response.configName = c.getConfigName();
        response.silentPush = c.isSilentPush();
        response.enabled = c.isEnabled();
        response.templateId = c.getTemplate().getTemplateId();
        response.meta = c.getMeta();
        response.channelNames = c.getChannelNames();
        return response;
    }
    private static Collection<PushConfigResponse> convertResponse(Collection<MMXPushConfig> c) {

        if (c == null) {
            return null;
        }
        List<PushConfigResponse> list = new ArrayList<>();
        for (MMXPushConfig config : c) {
            list.add(convertResponse(config));
        }
        return list;
    }

    public static class PushConfigRequest {

        String appId;
        String configName;
        int templateId;
        boolean silentPush;
        boolean enabled;
        Map<String, String> meta;
        List<String> channelNames;

        public String getAppId() {
            return appId;
        }
        public void setAppId(String appId) {
            this.appId = appId;
        }
        public String getConfigName() {
            return configName;
        }
        public void setConfigName(String configName) {
            this.configName = configName;
        }
        public int getTemplateId() {
            return templateId;
        }
        public void setTemplateId(int templateId) {
            this.templateId = templateId;
        }
        public boolean getIsSilentPush() {
            return silentPush;
        }
        public void setIsSilentPush(boolean silentPush) {
            this.silentPush = silentPush;
        }
        public boolean isSilentPush() {
            return silentPush;
        }
        public void setSilentPush(boolean silentPush) {
            this.silentPush = silentPush;
        }
        public boolean getIsEnabled() {
            return enabled;
        }
        public void setIsEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        public Map<String, String> getMeta() {
            return meta;
        }
        public void setMeta(Map<String, String> meta) {
            this.meta = meta;
        }
        public List<String> getChannelNames() {
            return channelNames;
        }
        public void setChannelNames(List<String> channelNames) {
            this.channelNames = channelNames;
        }
    }

    public static class PushConfigResponse {

        int configId;
        String appId;
        String configName;
        int templateId;
        boolean silentPush;
        boolean enabled;
        Map<String, String> meta = new HashMap<>();
        Set<String> channelNames;

        public int getConfigId() {
            return configId;
        }
        public void setConfigId(int configId) {
            this.configId = configId;
        }
        public String getAppId() {
            return appId;
        }
        public void setAppId(String appId) {
            this.appId = appId;
        }
        public String getConfigName() {
            return configName;
        }
        public void setConfigName(String configName) {
            this.configName = configName;
        }
        public int getTemplateId() {
            return templateId;
        }
        public void setTemplateId(int templateId) {
            this.templateId = templateId;
        }
        public boolean getIsSilentPush() {
            return silentPush;
        }
        public void setIsSilentPush(boolean silentPush) {
            this.silentPush = silentPush;
        }
        public boolean getIsEnabled() {
            return enabled;
        }
        public void setIsEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        public Map<String, String> getMeta() {
            return meta;
        }
        public void setMeta(Map<String, String> meta) {
            this.meta = meta;
        }
        public Set<String> getChannelNames() {
            return channelNames;
        }
        public void setChannelNames(Set<String> channelNames) {
            this.channelNames = channelNames;
        }
    }
}

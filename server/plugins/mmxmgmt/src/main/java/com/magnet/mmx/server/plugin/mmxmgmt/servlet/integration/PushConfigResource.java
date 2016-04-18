package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import org.apache.commons.lang3.StringUtils;

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
    public Response retrievePushConfig(@PathParam("configId") Integer configId) {

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
    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrieveActivePushConfig(@QueryParam("userId") String userId, @QueryParam("appId") String appId, @QueryParam("channelId") String channelId, @QueryParam("configName") String configName) {

        ActiveConfigRequest req = new ActiveConfigRequest();
        req.userId = userId;
        req.appId = appId;
        req.channelId = channelId;
        req.configName = configName;

        RestMethod<ActiveConfigRequest, PushConfigResponse> method = new RestMethod<ActiveConfigRequest, PushConfigResponse>() {
            @Override
            public PushConfigResponse execute(ActiveConfigRequest req) throws MMXException {
                //convert request

                //do job
                MMXPushConfig c = MMXPushConfigService.getInstance().getPushConfig(req.userId, req.appId, req.channelId, req.configName);

                //convert and return response
                return convertResponse(c);
            }
        };
        return method.doMethod(req);
    }
    public static class ActiveConfigRequest {
        String userId;
        String appId;
        String channelId;
        String configName;
    }
    @PUT
    @Path("/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePushConfig(@PathParam("configId") final Integer configId, PushConfigRequest request) {

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
    public Response deletePushConfig(@PathParam("configId") Integer configId) {

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
        c.setAppId(StringUtils.isBlank(request.appId) ? null : request.appId);
        c.setConfigName(StringUtils.isBlank(request.configName) ? null : request.configName);
        c.setSilentPush(request.silentPush);
        c.setEnabled(request.enabled);
        c.setTemplateId(request.templateId);
        c.setMeta(request.meta);
        if (request.getChannelIds() != null) {
            c.getChannelIds().addAll(request.getChannelIds());
        }
        return c;
    }
    private static PushConfigResponse convertResponse(MMXPushConfig c) {

        if (c == null) {
            return null;
        }
        PushConfigResponse response = new PushConfigResponse();
        response.configId = c.getConfigId();
        response.appId = c.getAppId();
        response.configName = c.getConfigName();
        response.silentPush = c.isSilentPush();
        response.enabled = c.isEnabled();
        response.templateId = c.getTemplateId();
        response.meta = c.getMeta();
        response.channelIds = c.getChannelIds();
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
        Integer templateId;
        boolean silentPush;
        boolean enabled;
        Map<String, String> meta;
        List<String> channelIds;

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
        public Integer getTemplateId() {
            return templateId;
        }
        public void setTemplateId(Integer templateId) {
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
        public boolean isEnabled() {
            return enabled;
        }
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        public List<String> getChannelIds() {
            return channelIds;
        }
        public void setChannelIds(List<String> channelIds) {
            this.channelIds = channelIds;
        }
    }

    public static class PushConfigResponse {

        Integer configId;
        String appId;
        String configName;
        Integer templateId;
        boolean silentPush;
        boolean enabled;
        Map<String, String> meta = new HashMap<>();
        Set<String> channelIds;

        public Integer getConfigId() {
            return configId;
        }
        public void setConfigId(Integer configId) {
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
        public Integer getTemplateId() {
            return templateId;
        }
        public void setTemplateId(Integer templateId) {
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
        public Set<String> getChannelIds() {
            return channelIds;
        }
        public void setChannelIds(Set<String> channelIds) {
            this.channelIds = channelIds;
        }
    }
}

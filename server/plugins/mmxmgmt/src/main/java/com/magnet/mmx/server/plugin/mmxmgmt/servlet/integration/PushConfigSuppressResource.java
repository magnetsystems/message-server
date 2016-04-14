package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushSuppress;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Created by mmicevic on 4/6/16.
 *
 */
@Path("/integration/pushconfigs/suppress")
public class PushConfigSuppressResource {

    @POST
//    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response suppress(PushConfigSuppressRequest request) {

        RestMethod<PushConfigSuppressRequest, PushConfigSuppressResponse> method = new RestMethod<PushConfigSuppressRequest, PushConfigSuppressResponse>() {
            @Override
            public PushConfigSuppressResponse execute(PushConfigSuppressRequest request) throws MMXException {
                //convert request
                MMXPushSuppress s = convertRequest(request);

                //do job
                s = MMXPushConfigService.getInstance().createPushSuppress(s);

                //convert and return response
                return convertResponse(s);
            }
        };
        return method.doMethod(request);
    }
    @GET
    @Path("/{suppressId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getSuppress(@PathParam("suppressId") int suppressId) {

        RestMethod<Integer, PushConfigSuppressResponse> method = new RestMethod<Integer,PushConfigSuppressResponse>() {
            @Override
            public PushConfigSuppressResponse execute(Integer suppressId) throws MMXException {
                //convert request

                //do job
                MMXPushSuppress s = MMXPushConfigService.getInstance().getPushSuppress(suppressId);

                //convert and return response
                return convertResponse(s);
            }
        };
        return method.doMethod(suppressId);
    }
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response geAlltSuppress(@PathParam("appId") final String appId) {

        RestMethod<String, Collection<PushConfigSuppressResponse>> method = new RestMethod<String, Collection<PushConfigSuppressResponse>>() {
            @Override
            public Collection<PushConfigSuppressResponse> execute(String suppressId) throws MMXException {
                //convert request

                //do job
                Collection<MMXPushSuppress> s = MMXPushConfigService.getInstance().getPushSuppressForAppAndUser(appId, null);

                //convert and return response
                return convertResponse(s);
            }
        };
        return method.doMethod(appId);
    }
    @DELETE
    @Path("/{suppressId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response unSuppress(@PathParam("suppressId") int suppressId) {

        RestMethod<Integer, RestMethod.SimpleMessage> method = new RestMethod<Integer, RestMethod.SimpleMessage>() {
            @Override
            public SimpleMessage execute(Integer suppressId) throws MMXException {
                //convert request

                //do job
                MMXPushConfigService.getInstance().deletePushSuppress(suppressId);

                //convert and return response
                return new SimpleMessage("deleted");
            }
        };
        return method.doMethod(suppressId);
    }
    private static MMXPushSuppress convertRequest(PushConfigSuppressRequest request) {

        MMXPushSuppress s = new MMXPushSuppress();
        s.setAppId(request.getAppId());
        s.setUserId(request.getUserId());
        s.setChannelName(request.getChannelName());
        return s;
    }
    private static Collection<PushConfigSuppressResponse> convertResponse(Collection<MMXPushSuppress> list) {

        if (list == null) {
            return null;
        }

        List<PushConfigSuppressResponse> respList = new ArrayList<>();
        for (MMXPushSuppress s : list) {
            respList.add(convertResponse(s));
        }
        return respList;
    }
    private static PushConfigSuppressResponse convertResponse(MMXPushSuppress s) {

        PushConfigSuppressResponse response = new PushConfigSuppressResponse();
        response.setSuppressId(s.getSuppressId());
        response.setAppId(s.getAppId());
        response.setUserId(s.getUserId());
        response.setChannelName(s.getChannelName());
        return response;
    }
    private static class PushConfigSuppressRequest {

        String appId;
        String userId;
        String channelName;

        public String getAppId() {
            return appId;
        }
        public void setAppId(String appId) {
            this.appId = appId;
        }
        public String getUserId() {
            return userId;
        }
        public void setUserId(String userId) {
            this.userId = userId;
        }
        public String getChannelName() {
            return channelName;
        }
        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }
    }
    private static class PushConfigSuppressResponse {

        int suppressId;
        String appId;
        String userId;
        String channelName;

        public int getSuppressId() {
            return suppressId;
        }
        public void setSuppressId(int suppressId) {
            this.suppressId = suppressId;
        }
        public String getAppId() {
            return appId;
        }
        public void setAppId(String appId) {
            this.appId = appId;
        }
        public String getUserId() {
            return userId;
        }
        public void setUserId(String userId) {
            this.userId = userId;
        }
        public String getChannelName() {
            return channelName;
        }
        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }
    }
}

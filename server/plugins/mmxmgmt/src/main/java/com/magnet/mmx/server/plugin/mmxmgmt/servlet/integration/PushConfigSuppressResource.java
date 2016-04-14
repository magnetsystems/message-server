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
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response geAlltSuppress(@QueryParam("appId") String appId, @QueryParam("userId") String userId) {

        PushConfigSuppressRequest request = new PushConfigSuppressRequest();
        request.setUserId(userId);
        request.setAppId(appId);
        RestMethod<PushConfigSuppressRequest, Collection<PushConfigSuppressResponse>> method = new RestMethod<PushConfigSuppressRequest, Collection<PushConfigSuppressResponse>>() {
            @Override
            public Collection<PushConfigSuppressResponse> execute(PushConfigSuppressRequest request) throws MMXException {
                //convert request

                //do job
                Collection<MMXPushSuppress> s = MMXPushConfigService.getInstance().getPushSuppressForAppAndUser(request.getAppId(), request.getUserId());

                //convert and return response
                return convertResponse(s);
            }
        };
        return method.doMethod(request);
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
        s.setUserId(request.getUserId());
        s.setAppId(request.getAppId());
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
        response.setUserId(s.getUserId());
        response.setAppId(s.getAppId());
        response.setChannelName(s.getChannelName());
        return response;
    }
    private static class PushConfigSuppressRequest {

        String userId;
        String appId;
        String channelName;

        public String getUserId() {
            return userId;
        }
        public void setUserId(String userId) {
            this.userId = userId;
        }
        public String getAppId() {
            return appId;
        }
        public void setAppId(String appId) {
            this.appId = appId;
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
        String userId;
        String appId;
        String channelName;

        public int getSuppressId() {
            return suppressId;
        }
        public void setSuppressId(int suppressId) {
            this.suppressId = suppressId;
        }
        public String getUserId() {
            return userId;
        }
        public void setUserId(String userId) {
            this.userId = userId;
        }
        public String getAppId() {
            return appId;
        }
        public void setAppId(String appId) {
            this.appId = appId;
        }
        public String getChannelName() {
            return channelName;
        }
        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }
    }
}

package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushSuppress;
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
public class PushConfigSuppressResource {

    @POST
    @Path("/suppress")
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
    @POST
    @Path("/unsuppress")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response unSuppress(PushConfigUnSuppressRequest request) {

        RestMethod<PushConfigUnSuppressRequest, RestMethod.SimpleMessage> method = new RestMethod<PushConfigUnSuppressRequest, RestMethod.SimpleMessage>() {
            @Override
            public RestMethod.SimpleMessage execute(PushConfigUnSuppressRequest request) throws MMXException {
                //convert request
                MMXPushSuppress s = convertRequest(request);

                //do job
                MMXPushConfigService.getInstance().createPushUnSuppress(s);

                //convert and return response
                return new RestMethod.SimpleMessage("deleted");
            }
        };
        return method.doMethod(request);
    }


    @GET
    @Path("/suppress/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response geAllSuppress(@QueryParam("appId") String appId, @QueryParam("userId") String userId) {

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
    @Path("/suppress/{suppressId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteRecotd(@PathParam("suppressId") int suppressId) {

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
    private static MMXPushSuppress convertRequest(PushConfigUnSuppressRequest request) {

        MMXPushSuppress s = new MMXPushSuppress();
        s.setUserId(StringUtils.isBlank(request.getUserId()) ? null : request.getUserId());
        s.setAppId(StringUtils.isBlank(request.getAppId()) ? null : request.getAppId());
        s.setChannelId(StringUtils.isBlank(request.getChannelId()) ? null : request.getChannelId());
        return s;
    }
    private static MMXPushSuppress convertRequest(PushConfigSuppressRequest request) {

        MMXPushSuppress s = new MMXPushSuppress();
        s.setUserId(StringUtils.isBlank(request.getUserId()) ? null : request.getUserId());
        s.setAppId(StringUtils.isBlank(request.getAppId()) ? null : request.getAppId());
        s.setChannelId(StringUtils.isBlank(request.getChannelId()) ? null : request.getChannelId());
        s.setUntilDate(request.getUntilDate());
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
        response.setChannelId(s.getChannelId());
        response.setUntilDate(s.getUntilDate());
        return response;
    }
    private static class PushConfigSuppressRequest {

        String userId;
        String appId;
        String channelId;
        Long untilDate;

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
        public String getChannelId() {
            return channelId;
        }
        public void setChannelId(String channelId) {
            this.channelId = channelId;
        }
        public Long getUntilDate() {
            return untilDate;
        }
        public void setUntilDate(Long untilDate) {
            this.untilDate = untilDate;
        }
    }
    private static class PushConfigUnSuppressRequest {

        String userId;
        String appId;
        String channelId;

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
        public String getChannelId() {
            return channelId;
        }
        public void setChannelId(String channelId) {
            this.channelId = channelId;
        }
    }
    private static class PushConfigSuppressResponse {

        int suppressId;
        String userId;
        String appId;
        String channelId;
        Long untilDate;

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
        public String getChannelId() {
            return channelId;
        }
        public void setChannelId(String channelId) {
            this.channelId = channelId;
        }
        public Long getUntilDate() {
            return untilDate;
        }
        public void setUntilDate(Long untilDate) {
            this.untilDate = untilDate;
        }
    }
}

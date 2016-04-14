package com.magnet.mmx.server.plugin.mmxmgmt.api.push;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.PublicRestMethod;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushSuppress;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mmicevic on 4/13/16.
 *
 */

@Path("/pushconfigs/suppress")
public class PushSuppressResource {  //extends AbstractBaseResource {

    @POST
    //    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response suppress(@Context HttpHeaders headers, PushConfigSuppressRequest request) {

        PublicRestMethod<PushConfigSuppressRequest, PushConfigSuppressResponse> method = new PublicRestMethod<PushConfigSuppressRequest, PushConfigSuppressResponse>() {
            @Override
            public PushConfigSuppressResponse execute(PushConfigSuppressRequest request) throws MMXException {
                //convert request
                MMXPushSuppress s = convertRequest(request);
                s.setAppId(appId);
                s.setUserId(userId);

                //do job
                s = MMXPushConfigService.getInstance().createPushSuppress(s);

                //convert and return response
                return convertResponse(s);
            }
        };
        return method.doMethod(request, headers);
    }
//    @GET
//    @Path("/{suppressId}")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response getSuppress(@Context HttpHeaders headers, @PathParam("suppressId") int suppressId) {
//
//        PublicRestMethod<Integer, PushConfigSuppressResponse> method = new PublicRestMethod<Integer,PushConfigSuppressResponse>() {
//            @Override
//            public PushConfigSuppressResponse execute(Integer suppressId) throws MMXException {
//                //convert request
//
//                //do job
//                MMXPushSuppress s = MMXPushConfigService.getInstance().getPushSuppress(suppressId);
//
//                //convert and return response
//                return convertResponse(s);
//            }
//        };
//        return method.doMethod(suppressId, headers);
//    }
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response geAllSuppress(@Context HttpHeaders headers) {

        PublicRestMethod<String, Collection<PushConfigSuppressResponse>> method = new PublicRestMethod<String, Collection<PushConfigSuppressResponse>>() {
            @Override
            public Collection<PushConfigSuppressResponse> execute(String dummy) throws MMXException {
                //convert request

                //do job
                Collection<MMXPushSuppress> s = MMXPushConfigService.getInstance().getPushSuppressForAppAndUser(appId, userId);

                //convert and return response
                return convertResponse(s);
            }
        };
        return method.doMethod(null, headers);
    }
    @DELETE
    @Path("/{suppressId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response unSuppress(@Context HttpHeaders headers, @PathParam("suppressId") int suppressId) {

        PublicRestMethod<Integer, PublicRestMethod.SimpleMessage> method = new PublicRestMethod<Integer, PublicRestMethod.SimpleMessage>() {
            @Override
            public SimpleMessage execute(Integer suppressId) throws MMXException {
                //convert request

                //do job
                MMXPushConfigService.getInstance().deletePushSuppress(suppressId);

                //convert and return response
                return new SimpleMessage("deleted");
            }
        };
        return method.doMethod(suppressId, headers);
    }
    private static MMXPushSuppress convertRequest(PushConfigSuppressRequest request) {

        MMXPushSuppress s = new MMXPushSuppress();
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

        String channelName;

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
    }}

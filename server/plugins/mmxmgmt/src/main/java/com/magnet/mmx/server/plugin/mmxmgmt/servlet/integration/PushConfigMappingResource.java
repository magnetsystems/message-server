package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfigMapping;

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
@Path("/integration/pushconfigmappings")
public class PushConfigMappingResource {

    @POST
//    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPushConfigMapping(PushConfigMappingRequest request) {

        RestMethod<PushConfigMappingRequest, PushConfigMappingResponse> method = new RestMethod<PushConfigMappingRequest, PushConfigMappingResponse>() {
            @Override
            public PushConfigMappingResponse execute(PushConfigMappingRequest request) throws MMXException {
                //convert request
                MMXPushConfigMapping m = convertRequest(request);

                //do job
                m = MMXPushConfigService.getInstance().createConfigMapping(m);

                //convert and return response
                return convertResponse(m);
            }
        };
        return method.doMethod(request);
    }
    @GET
    @Path("/{mappingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrievePushConfigMapping(@PathParam("mappingId") int mappingId) {

        RestMethod<Integer, PushConfigMappingResponse> method = new RestMethod<Integer, PushConfigMappingResponse>() {
            @Override
            public PushConfigMappingResponse execute(Integer mappingId) throws MMXException {
                //convert request

                //do job
                MMXPushConfigMapping c = MMXPushConfigService.getInstance().getConfigMapping(mappingId);

                //convert and return response
                return convertResponse(c);
            }
        };
        return method.doMethod(mappingId);
    }
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrieveAllPushConfigMappingsForApp(@QueryParam("appId") String appId) {

        RestMethod<String, Collection<PushConfigMappingResponse>> method = new RestMethod<String,Collection<PushConfigMappingResponse>>() {
            @Override
            public Collection<PushConfigMappingResponse> execute(String appId) throws MMXException {
                //convert request

                //do job
                Collection<MMXPushConfigMapping> c = MMXPushConfigService.getInstance().getAllConfigMappings(appId);

                //convert and return response
                return convertResponse(c);
            }
        };
        return method.doMethod(appId);
    }
    @PUT
    @Path("/{mappingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePushConfigMapping(@PathParam("mappingId") final int mappingId, PushConfigMappingRequest request) {

        RestMethod<PushConfigMappingRequest, PushConfigMappingResponse> method = new RestMethod<PushConfigMappingRequest, PushConfigMappingResponse>() {
            @Override
            public PushConfigMappingResponse execute(PushConfigMappingRequest request) throws MMXException {
                //convert request
                MMXPushConfigMapping m = convertRequest(request);
                m.setMappingId(mappingId);

                //do job
                m = MMXPushConfigService.getInstance().updateConfigMapping(m);

                //convert and return response
                return convertResponse(m);
            }
        };
        return method.doMethod(request);
    }
    @DELETE
    @Path("/{mappingId}")
    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePushConfigMapping(@PathParam("mappingId") int mappingId) {

        RestMethod<Integer, RestMethod.SimpleMessage> method = new RestMethod<Integer, RestMethod.SimpleMessage>() {
            @Override
            public RestMethod.SimpleMessage execute(Integer mappingId) throws MMXException {
                //convert request

                //do job
                MMXPushConfigService.getInstance().deleteConfigMapping(mappingId);

                //convert and return response
                return new RestMethod.SimpleMessage("deleted");
            }
        };
        return method.doMethod(mappingId);
    }


    private static MMXPushConfigMapping convertRequest(PushConfigMappingRequest request) throws MMXException {

        MMXPushConfigMapping m = new MMXPushConfigMapping();
        m.setAppId(request.appId);
        m.setConfigId(request.configId);
        m.setChannelName(request.channelName);
        return m;
    }
    private static PushConfigMappingResponse convertResponse(MMXPushConfigMapping m) {

        PushConfigMappingResponse response = new PushConfigMappingResponse();
        response.mappingId = m.getMappingId();
        response.configId = m.getConfigId();
        response.appId = m.getAppId();
        response.channelName = m.getChannelName();
        return response;
    }
    private static Collection<PushConfigMappingResponse> convertResponse(Collection<MMXPushConfigMapping> c) {

        if (c == null) {
            return null;
        }
        List<PushConfigMappingResponse> list = new ArrayList<>();
        for (MMXPushConfigMapping mapping : c) {
            list.add(convertResponse(mapping));
        }
        return list;
    }

    public static class PushConfigMappingRequest {

        int configId;
        String appId;
        String channelName;

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

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }
    }

    public static class PushConfigMappingResponse {

        int mappingId;
        int configId;
        String appId;
        String channelName;

        public int getMappingId() {
            return mappingId;
        }

        public void setMappingId(int mappingId) {
            this.mappingId = mappingId;
        }

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

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }
    }
}

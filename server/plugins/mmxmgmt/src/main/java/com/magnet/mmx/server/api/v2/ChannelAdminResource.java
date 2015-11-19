/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.server.api.v2;

import com.magnet.mmx.protocol.*;

import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXChannelManager;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.ChannelHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Channel admin REST API using auth token.  Accessing this resource must be
 * restricted to MMS or admin user.
 */
@Path("channels")
public class ChannelAdminResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelAdminResource.class);
    static final String DESCRIPTION_KEY = "description";
    static final String TAGS_KEY = "tag";
    static final String ID_KEY = "id";
    static final String CHANNEL_NAME = "channelName";

    /**
     * Add subscribers to a public or private channel by the admin.  The
     * <code>channelName</code> must be in the form of "user-id#channel-name"
     * (e.g. "38473af95e116c0#myblogs") for private channel, or without a hash
     * sign as "channel-name" (e.g. "company-announcement") for public channel.
     * @param headers
     * @param channelName Private or public channel name.
     * @param subscribers A list of subscribers.
     * @return
     */
    @PUT
    @Path("{" + CHANNEL_NAME + "}/subscribers")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSubscribers(@Context HttpHeaders headers,
                                    @PathParam(CHANNEL_NAME) String channelName,
                                    List<MMXid> subscribers) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID admin = RestUtils.createJID(tokenInfo);
        String domain = admin.getDomain();
        String appId = tokenInfo.getMmxAppId();
        MMXChannelId channelId = nameToId(channelName);

        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        try {             
            ChannelAction.SubscribeRequest rqt = new ChannelAction.SubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            ChannelAction.SubscribeResponse resp = null;
            for (MMXid subscriber : subscribers) {
              JID sub = new JID(JIDUtil.makeNode(subscriber.getUserId(), appId),
                    domain, subscriber.getDeviceId());
              resp = channelManager.subscribeChannel(sub, appId, rqt,
                  Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));
            }
            return RestUtils.getOKJAXRSResp(resp);
        } catch (MMXException e) {
            Response.Status status;
            ErrorResponse response;
            if (e.getCode() == StatusCode.NOT_FOUND) {
                status = Response.Status.NOT_FOUND;
                response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST,
                        "Channel does not exist: " + channelName);
            } else if (e.getCode() == StatusCode.FORBIDDEN) {
                status = Response.Status.FORBIDDEN;
                response = new ErrorResponse(ErrorCode.TOPIC_SUBSCRIBE_FORBIDDEN,
                        "Channel cannot be subscribed: " + channelName);
            } else if (e.getCode() == StatusCode.CONFLICT) {
                status = Response.Status.CONFLICT;
                response = new ErrorResponse(ErrorCode.TOPIC_ALREADY_SUBSCRIBED,
                        "Channel is already subscribed: " + channelName);
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR;
                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
            }
            return RestUtils.getJAXRSResp(status, response);
        }
    }

    /**
     * Delete subscribers from a public or private channel by the admin.  The
     * <code>channelName</code> must be in the form of "user-id#channel-name"
     * (e.g. "38473af95e116c0#myblogs") for private channel, or without a hash
     * sign as "channel-name" (e.g. "company-announcement") for public channel.
     * @param headers
     * @param channelName Private or public channel name.
     * @param subscribers A list of subscribers.
     * @return
     */
    @DELETE
    @Path("{" + CHANNEL_NAME + "}/subscribers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSubscribers(@Context HttpHeaders headers,
                                       @PathParam(CHANNEL_NAME) String channelName,
                                       List<MMXid> subscribers) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID admin = RestUtils.createJID(tokenInfo);
        String domain = admin.getDomain();
        String appId = tokenInfo.getMmxAppId();
        MMXChannelId channelId = nameToId(channelName);

        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        try {
            ChannelAction.UnsubscribeRequest rqt = new ChannelAction.UnsubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            MMXStatus resp = null;
            for (MMXid subscriber : subscribers) {
              JID sub = new JID(JIDUtil.makeNode(subscriber.getUserId(), appId),
                    domain, subscriber.getDeviceId());
              resp = channelManager.unsubscribeChannel(sub, appId, rqt);
            }
            return RestUtils.getOKJAXRSResp(resp);
        } catch (MMXException e) {
            Response.Status status;
            ErrorResponse response;
            if (e.getCode() == StatusCode.NOT_FOUND) {
                status = Response.Status.NOT_FOUND;
                response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST,
                        "Channel does not exist: " + channelName);
            } else if (e.getCode() == StatusCode.GONE) {
                status = Response.Status.GONE;
                response = new ErrorResponse(ErrorCode.TOPIC_NOT_SUBSCRIBED,
                        "Channel is not subscribed: " + channelName);
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR;
                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
            }
            return RestUtils.getJAXRSResp(status, response);
        }
    }

    // The hack to fix MOB-2516 that allows the console to display user channels as
    // userID#channelName. This method parses the global channel or user channel
    // properly.
    public static MMXChannelId nameToId(String channelName) {
        int index = channelName.indexOf(ChannelHelper.CHANNEL_SEPARATOR);
        if (index < 0) {
            return new MMXChannelId(channelName);
        } else {
            return new MMXChannelId(channelName.substring(0, index), channelName
                    .substring(index + 1));
        }
    }

    // The hack to fix MOB-2516 to convert a user channel to userID#channelName
    public static String idToName(String userId, String channelName) {

        if (userId == null) {
            return channelName;
        } else {
            return userId + ChannelHelper.CHANNEL_SEPARATOR + channelName;
        }
    }
}
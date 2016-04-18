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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.magnet.mmx.protocol.*;
import com.magnet.mmx.protocol.ChannelInfo;
import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.api.v1.protocol.*;
import com.magnet.mmx.server.api.v2.ChannelResource;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.*;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXChannelManager;
import com.magnet.mmx.server.plugin.mmxmgmt.message.*;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.PubSubPersistenceManagerExt;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfigMapping;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.TopicPostResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.*;
import com.magnet.mmx.util.AppChannel;
import com.magnet.mmx.util.ChannelHelper;
import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.pubsub.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Channel admin REST API using auth token.  Accessing this resource must be
 * restricted to MMS or admin user.
 */
@Path("/integration/channels")
public class IntegrationChannelResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationChannelResource.class);
    static final String DESCRIPTION_KEY = "description";
    static final String TAGS_KEY = "tag";
    static final String ID_KEY = "id";
    static final String CHANNEL_NAME = "channelName";
    static final String MMX_INTERNAL_SALT = "Z00N13!!";
    private static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static int HASH_MIN_KEY_SIZE = 8;
    UserDAO userDAO = new UserDAOImpl(new OpenFireDBConnectionProvider());
    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/create")
    public Response createChannel(@Context HttpHeaders headers,
                                  CreateChannelRequest channelInfo) {

        ErrorResponse errorResponse = null;
        CreateChannelResponse chatChannelResponse = null;

        if (channelInfo == null) {
            chatChannelResponse = new CreateChannelResponse(ErrorCode.ILLEGAL_ARGUMENT.getCode(), "Channel information not set");
            return RestUtils.getOKJAXRSResp(chatChannelResponse);
        }

        if (!ChannelHelper.validateApplicationChannelName(channelInfo.getChannelName())) {
            chatChannelResponse = new CreateChannelResponse(ErrorCode.ILLEGAL_ARGUMENT.getCode(),
                    MMXChannelManager.StatusCode.INVALID_CHANNEL_NAME.getMessage());
            return RestUtils.getOKJAXRSResp(chatChannelResponse);
        }

        if (!Strings.isNullOrEmpty(channelInfo.getDescription())
                && channelInfo.getDescription().length() > MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN) {

            chatChannelResponse = new CreateChannelResponse(ErrorCode.ILLEGAL_ARGUMENT.getCode(),
                    "channel description too long, max length = " +
                            MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN);

            return RestUtils.getOKJAXRSResp(chatChannelResponse);
        }

        //validate push config name
        Integer pushConfigId = null;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(channelInfo.getPushConfigName())) {
            try {
                MMXPushConfig pushConfig = MMXPushConfigService.getInstance().getConfig(channelInfo.getMmxAppId(), channelInfo.getPushConfigName());
                pushConfigId = pushConfig.getConfigId();
            }
            catch (MMXException e) {
                errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                        "push config name '" + channelInfo.getPushConfigName() + "' does not exist ");
                return RestUtils.getBadReqJAXRSResp(errorResponse);
            }
        }
        // Attempt to create or Fetch Channel
        String channelName = channelInfo.getChannelName();

        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        channelInfo.setChannelName(channelName);
        channelInfo.setPrivateChannel(channelInfo.isPrivateChannel());
        JID from = RestUtils.createJID(channelInfo.getUserId(),
                channelInfo.getMmxAppId(),
                channelInfo.getDeviceId());
        try {
            ChannelAction.CreateRequest rqt = toCreateRequest(channelInfo);
            rqt.getOptions().setWhiteList(channelInfo.getSubscribers());
            channelManager.createChannel(from, channelInfo.getMmxAppId(), rqt);

        } catch (MMXException e) {
            if (e.getCode() == StatusCode.CONFLICT) {
                errorResponse = new ErrorResponse(ErrorCode.TOPIC_EXISTS,
                        "Channel already exists");
            } else if (e.getCode() == StatusCode.FORBIDDEN) {
                errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e
                        .getMessage());
            } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e
                        .getMessage());
            } else {
                errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e
                        .getMessage());
            }

            chatChannelResponse = new CreateChannelResponse(errorResponse.getCode(),
                    errorResponse.getMessage());
            return RestUtils.getOKJAXRSResp(chatChannelResponse);

        } catch (Throwable e) {

            chatChannelResponse = new CreateChannelResponse(ErrorCode.UNKNOWN_ERROR.getCode(),
                    e.getMessage());
            return RestUtils.getOKJAXRSResp(chatChannelResponse);


        }

        // auto subscribe recipients to this channel
        Map<String, ChannelAction.SubscribeResponse> subResponseMap = new HashMap<String, ChannelAction.SubscribeResponse>();

        try {
            //MMXChannelId channelId = nameToId(channelName);
            MMXChannelId channelId;
            if (channelInfo.isPrivateChannel()) {
                channelId = new MMXChannelId(channelInfo.getUserId(), channelInfo.getChannelName());
            } else {
                channelId = new MMXChannelId(channelInfo.getChannelName());
            }
            //ChannelInfo foundChannel =  channelManager.getChannel(channelInfo.getMmxAppId(),channelId );
            //errorResponse = new ErrorResponse(ErrorCode.NO_ERROR, "Send Message to Chat Success");

            ChannelAction.SubscribeRequest rqt = new ChannelAction.SubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            ChannelAction.SubscribeResponse resp = null;
            if (channelInfo.getSubscribers() != null) {
                for (String subscriber : channelInfo.getSubscribers()) {
                    JID sub = new JID(JIDUtil.makeNode(subscriber, channelInfo.getMmxAppId()),
                            from.getDomain(), null);
                    resp = channelManager.subscribeChannel(sub, channelInfo.getMmxAppId(), rqt,
                            Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));
                    if (resp.getCode() == 200) {
                        subResponseMap.put(subscriber, new ChannelAction.SubscribeResponse(resp.getSubId(), 0, resp.getSubId()));
                    } else {
                        subResponseMap.put(subscriber, resp);
                    }


                }
            }
            if (pushConfigId != null) {
                setConfigMapping(channelInfo.getMmxAppId(), channelInfo.getChannelName(), pushConfigId);
            }

        } catch (MMXException e) {
            LOGGER.error("Exception during createChannel request", e);
            chatChannelResponse = new CreateChannelResponse(ErrorCode.UNKNOWN_ERROR.getCode(),
                    e.getMessage());
            chatChannelResponse.setSubscribeResponse(subResponseMap);

            return RestUtils.getCreatedJAXRSResp(chatChannelResponse);

        }


        chatChannelResponse = new CreateChannelResponse(ErrorCode.NO_ERROR.getCode(),
                "Channel created");
        chatChannelResponse.setSubscribeResponse(subResponseMap);

        return RestUtils.getCreatedJAXRSResp(chatChannelResponse);


    }
    // config mapping
    private void setConfigMapping(String appId, String channelName, int pushConfigId) throws MMXException {

        MMXPushConfigMapping mapping = null;
        try {
            mapping = MMXPushConfigService.getInstance().getConfigMapping(appId, channelName);
            //if found ==> delete
            MMXPushConfigService.getInstance().deleteConfigMapping(mapping);
        }
        catch (MMXException e) {
            //does not exist
        }
        MMXPushConfigService.getInstance().createConfigMapping(pushConfigId, appId, channelName);
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/subscribers/add")
    public Response addSubscribersToChannel(@Context HttpHeaders headers,
                                            CreateChannelRequest channelInfo) {

        ErrorResponse errorResponse = null;
        CreateChannelResponse chatChannelResponse = null;

        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        JID from = RestUtils.createJID(channelInfo.getUserId(), channelInfo.getMmxAppId(), channelInfo.getDeviceId());

        MMXChannelId tid = null;
        if (channelInfo.isPrivateChannel()) {
            tid = new MMXChannelId(channelInfo.getUserId(), channelInfo.getChannelName());
        } else {
            tid = new MMXChannelId(channelInfo.getChannelName());
        }

        try {
            com.magnet.mmx.protocol.ChannelInfo info = channelManager.getChannel(from,
                    channelInfo.getMmxAppId(), tid);

            if (info.isUserChannel()) {
                if (!JIDUtil.getReadableUserId(info.getCreator()).equals(channelInfo.getUserId())) {
                    errorResponse = new ErrorResponse(ErrorCode.TOPIC_NOT_OWNER,
                            "Channel doesn't belong to this user: " + channelInfo.getChannelName());
                    return RestUtils.getJAXRSResp(Response.Status.OK, errorResponse);
                }
            } else {
                //Validate the subscriber
                String channelId = ChannelHelper.makeChannel(channelInfo.getMmxAppId(), info.getEscUserId(),
                        info.getName());
                List<NodeSubscription> subscriptions = channelManager
                        .listSubscriptionsForChannel(channelId);

                boolean isSubscribedUser = false;
                for (NodeSubscription subscription : subscriptions) {
                    if (subscription.getJID().equals(from)) {
                        isSubscribedUser = true;
                    }
                }
                if (!isSubscribedUser) {
                    errorResponse = new ErrorResponse(ErrorCode.TOPIC_NOT_SUBSCRIBED,
                            "User doesn't subscribed to this channel: " + channelInfo.getChannelName());
                    return RestUtils.getJAXRSResp(Response.Status.OK, errorResponse);
                }

            }

        } catch (MMXException e) {
            ErrorResponse response;
            if (e.getCode() == StatusCode.NOT_FOUND) {
                response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST.getCode(),
                        "Channel not found: " + channelInfo.getChannelName());
                return RestUtils.getJAXRSResp(Response.Status.OK, response);
            } else {
                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR.getCode(), e
                        .getMessage());
                return RestUtils.getInternalErrorJAXRSResp(response);
            }
        }

        // auto subscribe recipients to this channel
        Map<String, ChannelAction.SubscribeResponse> subResponseMap = new HashMap<String, ChannelAction.SubscribeResponse>();

        try {
            //MMXChannelId channelId = nameToId(channelName);
            MMXChannelId channelId;
            if (channelInfo.isPrivateChannel()) {
                channelId = new MMXChannelId(channelInfo.getUserId(), channelInfo.getChannelName());
            } else {
                channelId = new MMXChannelId(channelInfo.getChannelName());
            }


            ChannelAction.SubscribeRequest rqt = new ChannelAction.SubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            ChannelAction.SubscribeResponse resp = null;
            if (channelInfo.getSubscribers() != null) {
                for (String subscriber : channelInfo.getSubscribers()) {

                    //Validate the user
                    if (!isValidUser(channelInfo.getMmxAppId(), subscriber)) {
                        subResponseMap.put(subscriber, new ChannelAction.SubscribeResponse(null, ErrorCode.INVALID_USER_NAME.getCode(), "Invalid User"));
                        continue;
                    }
                    JID sub = new JID(JIDUtil.makeNode(subscriber, channelInfo.getMmxAppId()),
                            from.getDomain(), null);
                    //SET
                    if (channelInfo.isPrivateChannel()) {
                        MMXChannelUtil.addUserToChannelWhiteList(channelInfo.getChannelName(), channelInfo.getUserId(), channelInfo.getMmxAppId(), sub);
                    }
//                    setWhiteList(channelInfo.getChannelName(), channelInfo.getUserId(), channelInfo.getMmxAppId(), sub);
                    resp = channelManager.subscribeChannel(sub, channelInfo.getMmxAppId(), rqt,
                            Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));

                    if (resp.getCode() == 200) {
                        subResponseMap.put(subscriber, new ChannelAction.SubscribeResponse(resp.getSubId(), 0, resp.getSubId()));
                    } else {
                        subResponseMap.put(subscriber, resp);
                    }
                }
            }

        } catch (MMXException e) {
            LOGGER.error("Exception during addSubscribersToChannel request", e);
            chatChannelResponse = new CreateChannelResponse(ErrorCode.UNKNOWN_ERROR.getCode(),
                    e.getMessage());
            chatChannelResponse.setSubscribeResponse(subResponseMap);

            return RestUtils.getCreatedJAXRSResp(chatChannelResponse);

        }


        chatChannelResponse = new CreateChannelResponse(ErrorCode.NO_ERROR.getCode(),
                "Subscribers added");
        chatChannelResponse.setSubscribeResponse(subResponseMap);

        return RestUtils.getCreatedJAXRSResp(chatChannelResponse);


    }

//    private void setWhiteList(String channelName, String ownerId, String appId, JID subJID) {
//
//        String channel = ChannelHelper.normalizePath(channelName);
//        String realChannel = ChannelHelper.makeChannel(appId, ownerId, channel);
//        Node node = XMPPServer.getInstance().getPubSubModule().getNode(realChannel);
//        node.addMember(subJID);
//    }

    private boolean isValidUser(String appId, String subscriber) {
        String mmxUsername = Helper.getMMXUsername(subscriber, appId);
        UserEntity userEntity = userDAO.getUser(mmxUsername);
        return (userEntity != null);
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/subscribers/remove")
    public Response removeSubscribersToChannel(@Context HttpHeaders headers,
                                               CreateChannelRequest channelInfo) {

        ErrorResponse errorResponse = null;
        CreateChannelResponse chatChannelResponse = null;

        JID from = RestUtils.createJID(channelInfo.getUserId(),
                channelInfo.getMmxAppId(),
                channelInfo.getDeviceId());

        MMXChannelId tid = null;
        if (channelInfo.isPrivateChannel()) {
            tid = new MMXChannelId(channelInfo.getUserId(), channelInfo.getChannelName());
        } else {
            tid = new MMXChannelId(channelInfo.getChannelName());
        }

        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        try {
            com.magnet.mmx.protocol.ChannelInfo info = channelManager.getChannel(from,
                    channelInfo.getMmxAppId(), tid);

            if (!JIDUtil.getReadableUserId(info.getCreator()).equals(channelInfo.getUserId())) {
                errorResponse = new ErrorResponse(ErrorCode.TOPIC_NOT_OWNER,
                        "Channel doesn't belong to this user: " + channelInfo.getChannelName());
                return RestUtils.getJAXRSResp(Response.Status.OK, errorResponse);
            }

        } catch (MMXException e) {
            ErrorResponse response;
            if (e.getCode() == StatusCode.NOT_FOUND) {
                response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST.getCode(),
                        "Channel not found: " + channelInfo.getChannelName());
                return RestUtils.getJAXRSResp(Response.Status.OK, response);
            } else {
                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR.getCode(), e
                        .getMessage());
                return RestUtils.getInternalErrorJAXRSResp(response);
            }
        }

        // auto subscribe recipients to this channel
        Map<String, ChannelAction.SubscribeResponse> subResponseMap = new HashMap<String, ChannelAction.SubscribeResponse>();

        try {
            //MMXChannelId channelId = nameToId(channelName);
            MMXChannelId channelId;
            if (channelInfo.isPrivateChannel()) {
                channelId = new MMXChannelId(channelInfo.getUserId(), channelInfo.getChannelName());
            } else {
                channelId = new MMXChannelId(channelInfo.getChannelName());
            }
            //ChannelInfo foundChannel =  channelManager.getChannel(channelInfo.getMmxAppId(),channelId );
            //errorResponse = new ErrorResponse(ErrorCode.NO_ERROR, "Send Message to Chat Success");

            ChannelAction.UnsubscribeRequest rqt = new ChannelAction.UnsubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            MMXStatus resp = null;
            if (channelInfo.getSubscribers() != null) {
                for (String subscriber : channelInfo.getSubscribers()) {
                    try {

                        //Validate the user
                        if (!isValidUser(channelInfo.getMmxAppId(), subscriber)) {
                            subResponseMap.put(subscriber, new ChannelAction.SubscribeResponse(null, ErrorCode.INVALID_USER_NAME.getCode(), "Invalid User"));
                            continue;
                        }

                        JID sub = new JID(JIDUtil.makeNode(subscriber, channelInfo.getMmxAppId()),
                                from.getDomain(), null);
                        resp = channelManager.unsubscribeChannel(sub, channelInfo.getMmxAppId(), rqt);

                        if (resp.getCode() == 200) {
                            subResponseMap.put(subscriber, new ChannelAction.SubscribeResponse(null, 0, resp.getMessage()));
                        } else {
                            subResponseMap.put(subscriber, new ChannelAction.SubscribeResponse(null, resp.getCode(), resp.getMessage()));

                        }
                    } catch (Exception ex) {
                        subResponseMap.put(subscriber, new ChannelAction.SubscribeResponse(null, ErrorCode.UNKNOWN_ERROR.getCode(), ex.getMessage()));
                    }

                }
            }

        } catch (Exception e) {
            LOGGER.error("Exception during addSubscribersToChannel request", e);
            chatChannelResponse = new CreateChannelResponse(ErrorCode.UNKNOWN_ERROR.getCode(),
                    e.getMessage());
            chatChannelResponse.setSubscribeResponse(subResponseMap);

            return RestUtils.getCreatedJAXRSResp(chatChannelResponse);

        }


        chatChannelResponse = new CreateChannelResponse(ErrorCode.NO_ERROR.getCode(),
                "Subscribers removed");
        chatChannelResponse.setSubscribeResponse(subResponseMap);

        return RestUtils.getCreatedJAXRSResp(chatChannelResponse);


    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/query/sql")
    public Response findChannelsUsingSQL(@Context HttpHeaders headers, QueryChannelRequest queryChannelRequest) {

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Map<String, Integer> channelCountMap = new HashMap<String, Integer>(3);
        Map<String, Integer> channelSubscriptionMap = new HashMap<String, Integer>(3);
        List<ChannelInfo> channels = new ArrayList<ChannelInfo>(3);
        try {
            con = DbConnectionManager.getConnection();


            StringBuffer userInClause = new StringBuffer();
            for (String user : queryChannelRequest.getSubscribers()) {

                JID from = RestUtils.createJID(user,
                        queryChannelRequest.getMmxAppId(),
                        queryChannelRequest.getDeviceId());
                if (userInClause.length() == 0) {
                    userInClause.append("'").append(from).append("'");
                } else {
                    userInClause.append(",'").append(from).append("'");
                }
            }

            String sql = "SELECT nodeID,count(*) FROM ofPubsubSubscription where state = 'subscribed' AND nodeID " +
                    "like '" + "/" + queryChannelRequest.getMmxAppId() + "%' AND " +
                    "jid in (" + userInClause.toString() + ") group by nodeID";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                channelCountMap.put(rs.getString(1), rs.getInt(2));
            }

            StringBuffer channelInClause = new StringBuffer();
            for (String node : channelCountMap.keySet()) {
                if (channelInClause.length() == 0) {
                    channelInClause.append("'").append(node).append("'");
                } else {
                    channelInClause.append(",'").append(node).append("'");
                }
            }

            if(channelInClause.length() == 0){
                QueryChannelResponse response = new QueryChannelResponse(ErrorCode.NO_ERROR.getCode(),
                        "Success");
                response.setChannels(channels);
                return RestUtils.getCreatedJAXRSResp(response);

            }

            String countSql = "SELECT nodeID, count(*) FROM ofPubsubSubscription where state = 'subscribed' AND " +
                    "nodeID in (" + channelInClause + ") group by nodeID";

            pstmt = con.prepareStatement(countSql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                channelSubscriptionMap.put(rs.getString(1), rs.getInt(2));
            }

            List<String> filteredChannels = new ArrayList<String>(3);

            if (!queryChannelRequest.getMatchFilter().equals(QueryChannelRequest.MatchType.ANY_MATCH)) {
                //Populate the subscription count
                for (String channelName : channelCountMap.keySet()) {

                    int totalSubscriptionCount = channelSubscriptionMap.get(channelName);
                    int matchingSubscriptionCount = channelCountMap.get(channelName);
                    if (queryChannelRequest.getMatchFilter().equals(QueryChannelRequest.MatchType.EXACT_MATCH)) {

                        if (matchingSubscriptionCount == totalSubscriptionCount &&
                                matchingSubscriptionCount == queryChannelRequest.getSubscribers().size()) {
                            filteredChannels.add(channelName);
                        }

                    } else if (queryChannelRequest.getMatchFilter().equals(QueryChannelRequest.MatchType.SUBSET_MATCH)) {
                        if (totalSubscriptionCount >= matchingSubscriptionCount &&
                                matchingSubscriptionCount >= queryChannelRequest.getSubscribers().size()) {
                            filteredChannels.add(channelName);
                        }
                    } else {
                        filteredChannels.add(channelName);
                    }

                }
            } else {

                for (String channelName : channelCountMap.keySet()) {
                    filteredChannels.add(channelName);
                }

            }


            if (filteredChannels.size() > 0) {
                //convert from nodeId to name
                StringBuffer nodeIds = new StringBuffer();
                for (String nodeId : filteredChannels) {
                    AppChannel appChannel = ChannelHelper.parseChannel(nodeId);
                    MMXChannelId channelId = getChannelName(appChannel);
                    Node node = MMXChannelManager.getInstance().getChannelNode(queryChannelRequest.getMmxAppId(), channelId);
                    ChannelInfo channelInfo = null;
                    //MMXChannelId tid = MMXChannelUtil.parseNode(node.getNodeID());
                    if (appChannel.isUserChannel()) {
                        channelInfo = MMXChannelManager.getInstance().nodeToChannelInfo(appChannel.getUserId(), node);
                    } else {
                        channelInfo = MMXChannelManager.getInstance().nodeToChannelInfo(null, node);
                    }
                    channels.add(channelInfo);
                }
            }
            QueryChannelResponse response = new QueryChannelResponse(ErrorCode.NO_ERROR.getCode(),
                    "Success");
            response.setChannels(channels);
            return RestUtils.getCreatedJAXRSResp(response);

        } catch (Exception sqlex) {
            LOGGER.error(sqlex.getMessage(), sqlex);
            ErrorResponse errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "");
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        } finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
    }


    private MMXChannelId getChannelName(AppChannel appChannel) {
        MMXChannelId channelId;
        if (appChannel.isUserChannel()) {
            channelId = new MMXChannelId(appChannel.getUserId(), appChannel.getName());
        } else {
            channelId = new MMXChannelId(appChannel.getName());
        }
        return channelId;
    }
//    @POST
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Path("/query")
//    public Response findChannels(@Context HttpHeaders headers,QueryChannelRequest queryChannelRequest) {
//
//        Map<String,Integer> channelCountMap = new HashMap<String, Integer>(3);
//
//        MMXChannelManager channelManager = MMXChannelManager.getInstance();
//
//        try {
//            ChannelAction.ListRequest request = new ChannelAction.ListRequest();
//            request.setLimit(1000);
//            request.setType(queryChannelRequest.getChannelType());
//
//            for(String userId:queryChannelRequest.getSubscribers()) {
//                JID from = RestUtils.createJID(userId, queryChannelRequest.getMmxAppId(), null);
//                ChannelAction.ListResponse response = channelManager.listChannels(from, queryChannelRequest.getMmxAppId(), request, null);
//                for (ChannelInfo info : response) {
//                    Integer integer = channelCountMap.get(info.getName());
//                    if (integer != null) {
//                        channelCountMap.put(info.getName(), new Integer(integer.intValue() + 1));
//                    } else {
//                        channelCountMap.put(info.getName(), new Integer(1));
//                    }
//                }
//            }
//            List<String> filteredChannels = new ArrayList<String>(3);
//            for(String channelName:channelCountMap.keySet()) {
//
//                if(queryChannelRequest.getMatchFilter().equals(QueryChannelRequest.MatchType.EXACT_MATCH)) {
//                    if (channelCountMap.get(channelName).intValue() == queryChannelRequest.getSubscribers().size()) {
//                        //Get the subscriber count
//                        List<NodeSubscription> subs = channelManager.listSubscriptionsForChannel(channelName);
//
//                        //Ignore if the user is not subscribed to this channel
//
//
//                        if (queryChannelRequest.getSubscribers().size() == subs.size()) {
//                            filteredChannels.add(channelName);
//                        }
//                    }
//                }else if(queryChannelRequest.getMatchFilter().equals(QueryChannelRequest.MatchType.SUBSET_MATCH)) {
//                    if (channelCountMap.get(channelName).intValue() == queryChannelRequest.getSubscribers().size()) {
//                        filteredChannels.add(channelName);
//                    }
//
//                }
//            }
//            QueryChannelResponse response = new QueryChannelResponse(ErrorCode.NO_ERROR.getCode(),
//                    "Success");
//            response.setChannels(filteredChannels);
//            return RestUtils.getCreatedJAXRSResp(response);
//
//        } catch (MMXException e) {
//            LOGGER.error("Exception during queryChannel request", e);
//            return RestUtils.getCreatedJAXRSResp(e.toString());
//
//        }
//
//    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/chat/publish/")
    public Response publishToChannel(@Context HttpHeaders headers,
                                     PublishMessageToChatRequest channelInfo) {

        ErrorResponse errorResponse = null;

        if (channelInfo == null) {
            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                    "Channel information not set");
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }

//        if (!MMXChannelUtil.validateApplicationChannelName(channelInfo.getChannelName())) {
//            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
//                    MMXChannelManager.StatusCode.INVALID_CHANNEL_NAME.getMessage());
//            return RestUtils.getBadReqJAXRSResp(errorResponse);
//        }

        if (!Strings.isNullOrEmpty(channelInfo.getDescription())
                && channelInfo.getDescription().length() > MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN) {
            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                    "channel description too long, max length = " +
                            MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN);
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }

        // Attempt to create or Fetch Channel
        String channelHash = generateHash(channelInfo);
        String channelName = constructChannelName(channelInfo.getChannelName(), channelHash);
        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        channelInfo.setChannelName(channelName);
        channelInfo.setPrivateChannel(true);
        JID from = RestUtils.createJID(channelInfo.getUserId(), channelInfo.getMmxAppId(), channelInfo.getDeviceId());
        try {
            ChannelAction.CreateRequest rqt = toCreateRequest(channelInfo);
            channelManager.createChannel(from, channelInfo.getMmxAppId(), rqt);


        } catch (MMXException e) {
            if (e.getCode() != StatusCode.CONFLICT) {
                if (e.getCode() == StatusCode.FORBIDDEN) {
                    errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e
                            .getMessage());
                } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                    errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e
                            .getMessage());
                } else {
                    errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e
                            .getMessage());
                    return RestUtils.getInternalErrorJAXRSResp(errorResponse);
                }
            }

        } catch (Throwable e) {
            LOGGER.error("Throwable during publishToChatChannel request", e);
            errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
            return RestUtils.getInternalErrorJAXRSResp(errorResponse);
        }

        List<SentMessageId> sentList = new ArrayList<SentMessageId>();
        // auto subscribe recipients to this channel
        try {
            MMXChannelId channelId = nameToId(channelName);
            ChannelInfo foundChannel = channelManager.getChannel(channelInfo.getMmxAppId(), channelId);
            errorResponse = new ErrorResponse(ErrorCode.NO_ERROR, "Send Message to Chat Success");

            ChannelAction.SubscribeRequest rqt = new ChannelAction.SubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            ChannelAction.SubscribeResponse resp = null;

            for (String subscriber : channelInfo.getRecipients()) {
                JID sub = new JID(JIDUtil.makeNode(subscriber, channelInfo.getMmxAppId()),
                        from.getDomain(), null);
                resp = channelManager.subscribeChannel(sub, channelInfo.getMmxAppId(), rqt,
                        Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));
                sentList.add(new SentMessageId(subscriber, "", null));


            }

        } catch (MMXException e) {
            // e.printStackTrace();
            //ignore this exception?
        }

        // send Message
        try {
            MessageSender sender = new MessageSenderImpl();

            TopicPostMessageRequest tpm = new TopicPostMessageRequest();
            tpm.setContent(channelInfo.getContent());
            tpm.setContentType(channelInfo.getContentType());
            tpm.setMessageType(channelInfo.getMessageType());

            TopicPostResult result = sender.postMessage(channelInfo.getUserId(), channelInfo.getUserId() + "#" + channelName,
                    channelInfo.getMmxAppId(), tpm);

            TopicPostResponse sendResponse = new TopicPostResponse();
            if (result.isError()) {
                LOGGER.info("Problem posting message:" + result.getErrorMessage());
                sendResponse.setMessage(result.getErrorMessage());
                sendResponse.setStatus(result.getStatus());
                throw new WebApplicationException(
                        Response
                                .status(
                                        result.getErrorCode() == ErrorCode.TOPIC_PUBLISH_FORBIDDEN
                                                .getCode() ?
                                                Response.Status.FORBIDDEN : Response.Status.BAD_REQUEST)
                                .entity(sendResponse)
                                .build());
            } else {
                sendResponse.setMessageId(result.getMessageId());
                sendResponse.setMessage("Successfully published message");
                sendResponse.setStatus(result.getStatus());
                for (SentMessageId messageId : sentList) {
                    messageId.setMessageId(result.getMessageId());
                }
            }

            SendMessageResponse messageResponse = new SendMessageResponse();
            messageResponse.setCount(new Count(channelInfo.getRecipients().length, sentList.size(), 0));
            messageResponse.setSentList(sentList);
            return RestUtils.getOKJAXRSResp(messageResponse);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Throwable t) {
            LOGGER.warn("Throwable during processing request", t);
            ErrorResponse error = new ErrorResponse(ErrorCode.UNKNOWN_ERROR,
                    "Error processing request");
            return RestUtils.getInternalErrorJAXRSResp(error);
        }

    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/summary")
    public Response getChannelSummary(@Context HttpHeaders headers,
                                      ChannelSummaryRequest request) {

        List<ChannelSummaryResponse> summaryResponses = new ArrayList<ChannelSummaryResponse>();

        JID userRequestingSummary = RestUtils.createJID(request.getRequestingUserId(),
                request.getAppId(),
                request.getDeviceId());

        List<ChannelSummaryResponse> channelSummaries = getChannelsSummary(userRequestingSummary, request);


//        //for each Channel
//        for(String channelId:request.getChannelIds()) {
//
//            //ChannelInfo channelInfo = MMXChannelManager.getInstance().getChannel(null,request.getAppId(),)
//            Node topicNode = MMXChannelManager.getInstance().getTopicNode(channelId);
//            ChannelInfo channelInfo = MMXChannelManager.getInstance().nodeToChannelInfo(request.getAppId(), topicNode);
//            MMXChannelManager.getInstance().getSummary(from,)
//            List<NodeSubscription> subscriptions = MMXChannelManager.getInstance().listSubscriptionsForChannel(channelId);
//            Date fromDate = topicNode.getCreationDate();
//            Date toDate = new Date(System.currentTimeMillis());
//            List<PublishedItem> messages = new ArrayList<PublishedItem>();
//
//            if(!topicNode.isCollectionNode())
//                messages = PubSubPersistenceManagerExt.getPublishedItems((LeafNode)topicNode,0,request.getNumOfMessages(),fromDate,toDate,true);
//
//            summaryResponses.add(new ChannelSummaryResponse(channelInfo,))
//            //MMXTopicManager.listSubscriptionsForTopic
//        }


        return RestUtils.getCreatedJAXRSResp(channelSummaries);
    }


    protected List<ChannelSummaryResponse> getChannelsSummary(JID userRequestingSummary, ChannelSummaryRequest channelSummaryRequest) {


        List<ChannelSummaryResponse> summaryList = new ArrayList<ChannelSummaryResponse>();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            List<MMXChannelId> channelIds = new ArrayList<MMXChannelId>();
            for (ChannelLookupKey channelLookupKey : channelSummaryRequest.getChannelIds()) {

                if (channelLookupKey.isPrivateChannel()) {
                    channelIds.add(new MMXChannelId(channelLookupKey.getUserId(), channelLookupKey.getChannelName()));
                } else {
                    channelIds.add(new MMXChannelId(channelLookupKey.getChannelName()));
                }
                //channelIds.add(nameToId(channelLookupKey.getChannelName()));
            }

            ChannelAction.SummaryRequest rqt = new ChannelAction.SummaryRequest(channelIds);
            ChannelAction.SummaryResponse resp = channelManager.getSummary(userRequestingSummary, channelSummaryRequest.getAppId(), rqt);

            for (ChannelSummary s : resp) {
                int count = s.getCount();
                Date lastPublishedDate = s.getLastPubTime();
                String userId = s.getChannelNode().getUserId();
                String name = s.getChannelNode().getName();


                // get Summary
//                MMXChannelSummary mts = new MMXChannelSummary(userId, name, count,
//                        lastPublishedDate);
                // get ChannelInfo
                MMXChannelId channelId;
                if (s.getChannelNode().isUserChannel()) {
                    channelId = new MMXChannelId(userId, name);
                } else {
                    channelId = new MMXChannelId(name);
                }

                String realChannel = ChannelHelper.makeChannel(channelSummaryRequest.getAppId(), channelId.getEscUserId(),
                        ChannelHelper.normalizePath(channelId.getName()));
                LOGGER.info("realChannel " + realChannel);
                PubSubPersistenceManager.loadNode(XMPPServer.getInstance().getPubSubModule(), realChannel);

                Node node = MMXChannelManager.getInstance().getChannelNode(channelSummaryRequest.getAppId(), channelId);

                ChannelInfo channelInfo = MMXChannelManager.getInstance().nodeToChannelInfo(userId, node);
                // get Subscribers
                //ChannelAction.SubscribersRequest subscribersRequest = new ChannelAction.SubscribersRequest(JIDUtil.getUserId(userRequestingSummary),node.getNodeID(), 0, channelSummaryRequest.getNumOfSubcribers());
                ChannelAction.SubscribersResponse subscribersResponse = MMXChannelManager.getInstance().getSubscribersFromNode(null, channelSummaryRequest.getAppId(), 0, channelSummaryRequest.getNumOfSubcribers(), node);


                Date sinceDate = null;
                if (channelSummaryRequest.getMessagesSince() == 0) {
                    Calendar oneyearDeafult = Calendar.getInstance();
                    oneyearDeafult.add(Calendar.YEAR, -1);
                    sinceDate = oneyearDeafult.getTime();
                } else {
                    sinceDate = new Date(channelSummaryRequest.getMessagesSince());
                }

                JID channelOwner = getChannelOwner(channelSummaryRequest.getAppId(),node);

                int messageOffset = 0;
                List<ChannelResource.MMXPubSubItemChannel2> messages =
                        this.fetchItemsForChannel(
                                userRequestingSummary,
                                channelOwner,
                                channelSummaryRequest.getAppId(),
                                channelId,
                                sinceDate,
                                new Date(),
                                channelSummaryRequest.getNumOfMessages(),
                                messageOffset,
                                MMXServerConstants.SORT_ORDER_ASC);

                UserEntity ownerInfo = userDAO.getUser(channelOwner.getNode());
                //if (ownerInfo != null) {
                  ChannelSummaryResponse channelSummaryResponse = new ChannelSummaryResponse(
                        userId, name,
                        ownerInfo,
                        count, lastPublishedDate,
                        subscribersResponse.getTotal(),
                        subscribersResponse.getSubscribers(),
                        messages);
                  summaryList.add(channelSummaryResponse);
                //}
            }

        } catch (Exception exc) {
            exc.printStackTrace();

        }

        return summaryList;

    }

    private JID getChannelOwner(String appId, Node node) {
        JID channelOwner = null;
        if(node.getOwners() != null) {
            JID serverUser = MMXChannelManager.getInstance().getServerUser(appId);
            Iterator<JID> list = node.getOwners().iterator();
            while(list.hasNext()){
                JID owner = list.next();
                if(owner != null) {
                    if (owner.equals(serverUser)){
                        continue;
                    }else{
                        channelOwner = owner;
                        break;
                    }
                }
            }
            if (channelOwner == null ) {
               LOGGER.info("Channel Owner is null. Setting server user as channel owner");
               channelOwner = serverUser;
            }
        }
        return channelOwner;
    }

    private List<ChannelResource.MMXPubSubItemChannel2> fetchItemsForChannel(JID userRequestingSummary,
                                                                             JID channelOwner,
                                                                             String appId,
                                                                             MMXChannelId channelId,
                                                                             Date since,
                                                                             Date until,
                                                                             int size,
                                                                             int offset,
                                                                             String sortOrder) throws MMXException {
        MMXChannelManager channelManager = MMXChannelManager.getInstance();

        ChannelAction.FetchOptions opt = new ChannelAction.FetchOptions()
                .setMaxItems(size)
                .setOffset(offset)
                .setAscending(
                        sortOrder.equalsIgnoreCase(MMXServerConstants.SORT_ORDER_ASC))
                .setSince(since == null ? null : new DateTime(since).toDate())
                .setUntil(until == null ? null : new DateTime(until).toDate());
        ChannelAction.FetchRequest rqt = new ChannelAction.FetchRequest(channelId.getEscUserId(), channelId
                .getName(),
                opt);
        //ChannelAction.FetchResponse resp = channelManager.fetchItems(channelOwner, appId, rqt);
        ChannelAction.FetchResponse resp = channelManager.fetchItems(userRequestingSummary, appId, rqt);

        String nodeId = ChannelHelper.makeChannel(appId, channelId.getEscUserId(), channelId.getName());
        List<TopicItemEntity> channelItemEntities = toTopicItemEntity(nodeId, resp.getItems());
        List<ChannelResource.MMXPubSubItemChannel2> items = toPubSubItems(channelId, channelItemEntities, appId);

        return items;

    }


//    /**
//     * Add subscribers to a public or private channel by the admin.  The
//     * <code>channelName</code> must be in the form of "user-id#channel-name"
//     * (e.g. "38473af95e116c0#myblogs") for private channel, or without a hash
//     * sign as "channel-name" (e.g. "company-announcement") for public channel.
//     * @param headers
//     * @param channelName Private or public channel name.
//     * @param subscribers A list of subscribers.
//     * @return
//     */
//    @PUT
//    @Path("{" + CHANNEL_NAME + "}/subscribers")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response addSubscribers(@Context HttpHeaders headers,
//                                    @PathParam(CHANNEL_NAME) String channelName,
//                                    List<MMXid> subscribers) {
//        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
//        if (tokenInfo == null) {
//            return RestUtils.getUnauthJAXRSResp();
//        }
//
    //      JID admin = RestUtils.createJID(tokenInfo);
//        String domain = admin.getDomain();
//        String appId = tokenInfo.getMmxAppId();
//        MMXChannelId channelId = nameToId(channelName);
//
//        MMXChannelManager channelManager = MMXChannelManager.getInstance();
//        try {
//            ChannelAction.SubscribeRequest rqt = new ChannelAction.SubscribeRequest(
//                    channelId.getEscUserId(), channelId.getName(), null);
//            ChannelAction.SubscribeResponse resp = null;
//            for (MMXid subscriber : subscribers) {
//              JID sub = new JID(JIDUtil.makeNode(subscriber.getUserId(), appId),
//                    domain, subscriber.getDeviceId());
//              resp = channelManager.subscribeChannel(sub, appId, rqt,
//                  Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));
//            }
//            return RestUtils.getOKJAXRSResp(resp);
//        } catch (MMXException e) {
//            Response.Status status;
//            ErrorResponse response;
//            if (e.getCode() == StatusCode.NOT_FOUND) {
//                status = Response.Status.NOT_FOUND;
//                response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST,
//                        "Channel does not exist: " + channelName);
//            } else if (e.getCode() == StatusCode.FORBIDDEN) {
//                status = Response.Status.FORBIDDEN;
//                response = new ErrorResponse(ErrorCode.TOPIC_SUBSCRIBE_FORBIDDEN,
//                        "Channel cannot be subscribed: " + channelName);
//            } else if (e.getCode() == StatusCode.CONFLICT) {
//                status = Response.Status.CONFLICT;
//                response = new ErrorResponse(ErrorCode.TOPIC_ALREADY_SUBSCRIBED,
//                        "Channel is already subscribed: " + channelName);
//            } else {
//                status = Response.Status.INTERNAL_SERVER_ERROR;
//                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
//            }
//            return RestUtils.getJAXRSResp(status, response);
//        }
//    }
//
//    /**
//     * Delete subscribers from a public or private channel by the admin.  The
//     * <code>channelName</code> must be in the form of "user-id#channel-name"
//     * (e.g. "38473af95e116c0#myblogs") for private channel, or without a hash
//     * sign as "channel-name" (e.g. "company-announcement") for public channel.
//     * @param headers
//     * @param channelName Private or public channel name.
//     * @param subscribers A list of subscribers.
//     * @return
//     */
//    @DELETE
//    @Path("{" + CHANNEL_NAME + "}/subscribers")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response deleteSubscribers(@Context HttpHeaders headers,
//                                       @PathParam(CHANNEL_NAME) String channelName,
//                                       List<MMXid> subscribers) {
//        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
//        if (tokenInfo == null) {
//            return RestUtils.getUnauthJAXRSResp();
//        }
//
//        JID admin = RestUtils.createJID(tokenInfo);
//        String domain = admin.getDomain();
//        String appId = tokenInfo.getMmxAppId();
//        MMXChannelId channelId = nameToId(channelName);
//
//        MMXChannelManager channelManager = MMXChannelManager.getInstance();
//        try {
//            ChannelAction.UnsubscribeRequest rqt = new ChannelAction.UnsubscribeRequest(
//                    channelId.getEscUserId(), channelId.getName(), null);
//            MMXStatus resp = null;
//            for (MMXid subscriber : subscribers) {
//              JID sub = new JID(JIDUtil.makeNode(subscriber.getUserId(), appId),
//                    domain, subscriber.getDeviceId());
//              resp = channelManager.unsubscribeChannel(sub, appId, rqt);
//            }
//            return RestUtils.getOKJAXRSResp(resp);
//        } catch (MMXException e) {
//            Response.Status status;
//            ErrorResponse response;
//            if (e.getCode() == StatusCode.NOT_FOUND) {
//                status = Response.Status.NOT_FOUND;
//                response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST,
//                        "Channel does not exist: " + channelName);
//            } else if (e.getCode() == StatusCode.GONE) {
//                status = Response.Status.GONE;
//                response = new ErrorResponse(ErrorCode.TOPIC_NOT_SUBSCRIBED,
//                        "Channel is not subscribed: " + channelName);
//            } else {
//                status = Response.Status.INTERNAL_SERVER_ERROR;
//                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
//            }
//            return RestUtils.getJAXRSResp(status, response);
//        }
//    }

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


    private ChannelAction.CreateRequest toCreateRequest(ChannelCreateInfo createInfo) {
        MMXTopicOptions options = new MMXTopicOptions()
                .setDescription(createInfo.getDescription())
                .setMaxItems(createInfo.getMaxItems())
                .setSubscribeOnCreate(createInfo.isSubscribeOnCreate())
                .setSubscriptionEnabled(createInfo.isSubscriptionEnabled())
                .setPublisherType(createInfo.getPublishPermission())
                ;
        ChannelAction.CreateRequest rqt = new ChannelAction.CreateRequest(createInfo.getChannelName(),
                createInfo.isPrivateChannel(), options);
        return rqt;
    }


    private static String generateHash(PublishMessageToChatRequest request) {
        List<String> recipients = new ArrayList<String>();
        recipients.addAll(Arrays.asList(request.getRecipients()));
        recipients.add(request.getUserId());

        if (recipients != null && recipients.size() > 0) {
            //order recipients
            Collections.sort(recipients);
            String signature = StringUtils.join(recipients, ",");
            MessageDigest md5 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");
                md5.update((MMX_INTERNAL_SALT + signature).getBytes());
                byte byteData[] = md5.digest();

                String channelHash = encode2Hex(byteData);
                return channelHash;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return null;

    }


    private static String constructChannelName(String channelName, String hash) {
        //return "chat/"+ hash;
        return hash;
    }


    public static String encode2Hex(byte[] bytes) {

        //shouldnt be using this to hexify a huge binary array
        if (bytes == null || bytes.length < 1 || bytes.length > (Integer.MAX_VALUE / 2)) {
          return null;
        }

        char[] hex = new char[bytes.length * 2];
        for (int indx = 0; indx < bytes.length; indx++) {
            byte lsb = (byte) (bytes[indx] & 0x0f);
            byte msb = (byte) ((bytes[indx] & 0xf0) >> 4);
            hex[indx * 2] = hexArray[msb];
            hex[indx * 2 + 1] = hexArray[lsb];
        }
        return new String(hex);
    }


    private List<TopicItemEntity> toTopicItemEntity(String nodeId,
                                                    List<ChannelAction.MMXPublishedItem> items) {
        List<TopicItemEntity> list = new ArrayList<TopicItemEntity>(items.size());
        for (ChannelAction.MMXPublishedItem item : items) {
            TopicItemEntity itemEntity = new TopicItemEntity();
            itemEntity.setNodeId(nodeId);
            itemEntity.setJid(item.getPublisher());
            itemEntity.setId(item.getItemId());
            itemEntity.setPayload(item.getPayloadXml());
            itemEntity.setCreationDate(org.jivesoftware.util.StringUtils.dateToMillis(item.getCreationDate()));
            itemEntity.setServiceId("pubsub");
            list.add(itemEntity);
        }
        return list;
    }

    private List<ChannelResource.MMXPubSubItemChannel2> toPubSubItems(final MMXChannelId channelId, List<TopicItemEntity> entityList, String appId) {
        Function<TopicItemEntity, MMXPubSubItemChannel> entityToItem =
                new Function<TopicItemEntity, MMXPubSubItemChannel>() {
                    @Override
                    public MMXPubSubItemChannel apply(TopicItemEntity i) {
                        return new MMXPubSubItemChannel(idToName(channelId.getEscUserId(),
                                channelId.getName()),
                                i.getId(), new JID(i.getJid()), i.getPayload());
                    }

                    ;
                };

        List<MMXPubSubItemChannel> items = Lists.transform(entityList, entityToItem);
        List<ChannelResource.MMXPubSubItemChannel2> items2 = new ArrayList<ChannelResource.MMXPubSubItemChannel2>(items.size());
        for (MMXPubSubItemChannel item : items) {
            items2.add(new ChannelResource.MMXPubSubItemChannel2Ext(item, appId));
        }
        return items2;
    }



	@PUT
    @Path("/message/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteMessage(@Context HttpHeaders headers,
                                  DeleteMessageRequest request) {

        DeleteMessageResponse response = new DeleteMessageResponse();

        if (request.getChannelId() != null) {
          // New version uses OF Pubsub Module for deletion.
          int code;
          try {
            String appId = request.getAppId();
            String userId = request.getUserId();

            // Pubsub Module does not support roles, pretend to be an owner.
            if ((request.getRoles() != null && request.getRoles().contains("DEVELOPER"))) {
              Node node = MMXChannelManager.getInstance().getChannelNode(appId,
                  new MMXChannelId(request.getOwnerId(), request.getChannelId()));
              userId = JIDUtil.getUserId(node.getOwners().iterator().next());
            }

            JID from = JIDUtil.makeJID(userId, appId, null);
            ChannelAction.RetractRequest rqt = new ChannelAction.RetractRequest(
                request.getOwnerId(), request.getChannelId(),
                Arrays.asList(request.getMessageId()));

            Map<String, Integer> res = MMXChannelManager.getInstance()
                .retractFromChannel(from, appId, rqt);
            code = res.get(request.getMessageId());

            if (code == MMXChannelManager.StatusCode.GONE.getCode()) {
              response.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
              response.setMessage("Message id is not found");
            } else if (code == MMXChannelManager.StatusCode.FORBIDDEN.getCode()) {
              response.setCode(ErrorCode.INSUFFICIENT_PRIVILEGES.getCode());
              response.setMessage("Insufficient privilege to delete the message");
            } else {
              response.setCode(200);
              response.setMessage("message has been deleted successfully");
            }
          } catch (MMXException e) {
            code = e.getCode();
            if (code == MMXChannelManager.StatusCode.CHANNEL_NOT_FOUND.getCode()) {
              response.setCode(ErrorCode.TOPIC_NOT_EXIST.getCode());
              response.setMessage("Channel is not found");
            } else {
              response.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
              response.setMessage(e.getMessage());
            }
          }
          return RestUtils.getOKJAXRSResp(response);
        } else {
          // The following codes are for backward compatible; developer should
          // not use it anymore.
          TopicItemEntity entity = DBUtil.getTopicItemDAO().findById(request.getMessageId());

          if(entity == null) {
              response.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
              response.setMessage("Message id is not found");
              return RestUtils.getOKJAXRSResp(response);
          }

          if(!allowDelete(request,entity)) {
              response.setCode(ErrorCode.INSUFFICIENT_PRIVILEGES.getCode());
              response.setMessage("Insufficient privilege to delete the message");
              return RestUtils.getOKJAXRSResp(response);
          }

  //        if(!JIDUtil.getAppId(entity.getNodeId()).equals(request.getAppId())){
  //            response.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
  //            response.setMessage("Message id and app id mismatch");
  //            return RestUtils.getOKJAXRSResp(response);
  //        }

          int result = DBUtil.getTopicItemDAO().deleteTopicItem(request.getMessageId());
          if(result != 1) {
              response.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
              response.setMessage("Message id is not found");
              return RestUtils.getOKJAXRSResp(response);

          }else {
              response.setCode(200);
              response.setMessage("message has been deleted successfully");
              return RestUtils.getOKJAXRSResp(response);
          }
      }

    }

    private boolean allowDelete(DeleteMessageRequest request, TopicItemEntity entity) {

        if((request.getRoles() != null && request.getRoles().contains("DEVELOPER"))){
            return true;
        }

        Node node = MMXChannelManager.getInstance().getTopicNode(entity.getNodeId());

        if(node == null) {
            LOGGER.error("Node is null for the id " + entity.getNodeId());
            return false;
        }
        JID from = JIDUtil.makeJID(request.getUserId(), request.getAppId(), null);
        if (from.equals(node.getCreator())) {
          return true;
        }
        return (node.getOwners().contains(from));
    }
}


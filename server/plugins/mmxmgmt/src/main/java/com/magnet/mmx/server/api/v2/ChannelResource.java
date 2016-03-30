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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.magnet.mmx.protocol.*;
import com.magnet.mmx.protocol.ChannelAction.*;
import com.magnet.mmx.protocol.SearchAction.Operator;
import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.api.v1.protocol.ChannelCreateInfo;
import com.magnet.mmx.server.api.v1.protocol.ChannelSubscription;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.ChannelTagInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXChannelManager;
import com.magnet.mmx.server.plugin.mmxmgmt.message.*;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.TopicPostResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.ChannelHelper;
import com.magnet.mmx.util.TimeUtil;

import com.magnet.mmx.util.TopicHelper;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.pubsub.CollectionNode;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.jivesoftware.openfire.pubsub.PubSubService;
import org.jivesoftware.util.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import javax.ws.rs.*;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Channel REST API for web client using auth token.
 */
@Path("channels")
public class ChannelResource {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ChannelResource.class);
    static final String DESCRIPTION_KEY = "description";
    static final String TAGS_KEY = "tag";
    static final String ID_KEY = "id";
    static final String CHANNEL_NAME = "channelName";
    private static final String DEFAULT_MAX_ITEMS = "200";
    private final static Integer DEFAULT_OFFSET = Integer.valueOf(0);


    public static class AddTagRequest {
        private boolean personal;
        private List<String> tags;

        public boolean isPersonal() {
            return personal;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChannel(@Context HttpHeaders headers,
                                  ChannelCreateInfo channelInfo) {
        ErrorResponse errorResponse = null;

        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        if (channelInfo == null) {
            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                    "Channel information not set");
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }

        if (!ChannelHelper.validateApplicationChannelName(channelInfo.getChannelName())) {
            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                    MMXChannelManager.StatusCode.INVALID_CHANNEL_NAME.getMessage());
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }

        if (!Strings.isNullOrEmpty(channelInfo.getDescription())
                && channelInfo.getDescription().length() > MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN) {
            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                    "channel description too long, max length = " +
                            MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN);
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }

        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            JID from = RestUtils.createJID(tokenInfo);
            CreateRequest rqt = toCreateRequest(channelInfo);
            channelManager.createChannel(from, tokenInfo.getMmxAppId(), rqt);
            errorResponse = new ErrorResponse(ErrorCode.NO_ERROR, "Channel created");
            return RestUtils.getCreatedJAXRSResp(errorResponse);
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
                return RestUtils.getInternalErrorJAXRSResp(errorResponse);
            }
            return RestUtils.getJAXRSResp(
                    Response.Status.fromStatusCode(e.getCode()), errorResponse);
        } catch (Throwable e) {
            errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
            return RestUtils.getInternalErrorJAXRSResp(errorResponse);
        }
    }

    private CreateRequest toCreateRequest(ChannelCreateInfo createInfo) {
        MMXTopicOptions options = new MMXTopicOptions()
                .setDescription(createInfo.getDescription())
                .setMaxItems(createInfo.getMaxItems())
                .setSubscribeOnCreate(createInfo.isSubscribeOnCreate())
                .setSubscriptionEnabled(createInfo.isSubscriptionEnabled())
                .setPublisherType(createInfo.getPublishPermission());
        CreateRequest rqt = new CreateRequest(createInfo.getChannelName(),
                createInfo.isPrivateChannel(), options);
        return rqt;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{" + CHANNEL_NAME + "}/items")
    public Response publishMessage(@Context HttpHeaders headers,
                                   @PathParam(CHANNEL_NAME) String channel,
                                   TopicPostMessageRequest request) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        try {
            MessageSender sender = new MessageSenderImpl();
            TopicPostResult result = sender.postMessage(tokenInfo.getUserId(), channel,
                    tokenInfo.getMmxAppId(), request);
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
            }
            return RestUtils.getOKJAXRSResp(sendResponse);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Throwable t) {
            LOGGER.warn("Throwable during processing request", t);
            ErrorResponse error = new ErrorResponse(ErrorCode.UNKNOWN_ERROR,
                    "Error processing request");
            return RestUtils.getInternalErrorJAXRSResp(error);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchChannels(@Context HttpHeaders headers,
                                   @QueryParam(CHANNEL_NAME) String channelName,
                                   @QueryParam(DESCRIPTION_KEY) String description,
                                   @QueryParam(TAGS_KEY) List<String> tags,
                                   @QueryParam(MMXServerConstants.OFFSET_PARAM) Integer offset,
                                   @QueryParam(MMXServerConstants.SIZE_PARAM)
                                   @DefaultValue(DEFAULT_MAX_ITEMS) int size) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }
        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();

        if (offset == null) {
          offset = DEFAULT_OFFSET;
        }

        TopicAction.TopicSearchRequest rqt = toSearchRequest(channelName,
                description, tags, size, offset);
        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        try {
            long startTime = System.nanoTime();
            ChannelAction.ChannelQueryResponse resp = channelManager.searchChannel(from,
                    appId, rqt, Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));
            long endTime = System.nanoTime();
            LOGGER.info("Completed processing searchChannels in {} milliseconds",
                    TimeUnit.MILLISECONDS.convert((endTime - startTime),
                            TimeUnit.NANOSECONDS));
            return RestUtils.getOKJAXRSResp(resp);
        } catch (MMXException e) {
            LOGGER.warn("Throwable during searchChannels", e);
            ErrorResponse response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR
                    .getCode(),
                    "Search channel failed: " + e.getMessage());
            return RestUtils.getInternalErrorJAXRSResp(response);
        }
    }

    private TopicAction.TopicSearchRequest toSearchRequest(String channelName,
                                                           String description, List<String> tags, Integer size, Integer offset) {
        TopicAction.TopicSearch attr = new TopicAction.TopicSearch()
                .setTopicName(channelName)
                .setDescription(description)
                .setTags(tags);
        TopicAction.TopicSearchRequest rqt = new TopicAction.TopicSearchRequest(
                Operator.AND, attr, offset, size);
        return rqt;
    }

    @PUT
    @Path("{" + CHANNEL_NAME + "}/subscribe")
    @Produces(MediaType.APPLICATION_JSON)
    public Response subscribeChannel(@Context HttpHeaders headers,
                                     @PathParam(CHANNEL_NAME) String channelName) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }
        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        MMXChannelId channelId = nameToId(channelName);

        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        try {
            ChannelAction.SubscribeRequest rqt = new ChannelAction.SubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            ChannelAction.SubscribeResponse resp = channelManager.subscribeChannel(from,
                    appId, rqt, Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));
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

    @PUT
    @Path("{" + CHANNEL_NAME + "}/unsubscribe")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unsubscribeChannel(@Context HttpHeaders headers,
                                       @PathParam(CHANNEL_NAME) String channelName) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }
        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        MMXChannelId channelId = nameToId(channelName);

        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        try {
            ChannelAction.UnsubscribeRequest rqt = new ChannelAction.UnsubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            MMXStatus resp = channelManager.unsubscribeChannel(from, appId, rqt);
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

    @GET
    @Path("{" + CHANNEL_NAME + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannel(@Context HttpHeaders headers,
                               @PathParam(CHANNEL_NAME) String channelName) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        MMXChannelId tid = nameToId(channelName);
        String channelId = ChannelHelper.makeChannel(appId, tid.getEscUserId(), tid
                .getName());
        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        try {
            com.magnet.mmx.protocol.ChannelInfo info = channelManager.getChannel(from,
                    appId, tid);
            return RestUtils.getOKJAXRSResp(info);
        } catch (MMXException e) {
            ErrorResponse response;
            if (e.getCode() == StatusCode.NOT_FOUND) {
                response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST.getCode(),
                        "Channel not found: " + channelName);
                return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, response);
            } else {
                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR.getCode(), e
                        .getMessage());
                return RestUtils.getInternalErrorJAXRSResp(response);
            }
        }
    }

    @DELETE
    @Path("{" + CHANNEL_NAME + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteChannel(@Context HttpHeaders headers,
                                  @PathParam(CHANNEL_NAME) String channelName,
                                  @QueryParam("personal") String isPersonalChannel) {

        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        ErrorResponse errorResponse = null;
        boolean myChannel = Boolean.parseBoolean(isPersonalChannel);
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            JID from = RestUtils.createJID(tokenInfo);
            DeleteRequest rqt = new DeleteRequest(channelName, myChannel);
            channelManager.deleteChannel(from, tokenInfo.getMmxAppId(), rqt);
            return RestUtils.getOKJAXRSResp();
        } catch (MMXException e) {
            errorResponse = new ErrorResponse();
            if (e.getCode() == StatusCode.NOT_FOUND) {
                String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND,
                        channelName);
                errorResponse.setMessage(message);
                return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(e
                                .getCode()),
                        errorResponse);
            } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getBadReqJAXRSResp(errorResponse);
            } else {
                errorResponse.setCode(ErrorCode.UNKNOWN_ERROR.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getInternalErrorJAXRSResp(errorResponse);
            }
        }
    }

    @GET
    @Path("{" + CHANNEL_NAME + "}/subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscriptionsForChannel(@Context HttpHeaders headers,
                                               @PathParam(CHANNEL_NAME) String channelName) {

        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();

        try {
            long startTime = System.nanoTime();
            MMXChannelId channelId = nameToId(channelName);
            List<ChannelSubscription> infoList = getChannelSubscriptions(appId,
                                                                    channelId);
            long endTime = System.nanoTime();
            LOGGER.info(
                    "Completed processing getSubscriptionsForChannels in {} milliseconds",
                    TimeUnit.MILLISECONDS.convert((endTime - startTime),
                            TimeUnit.NANOSECONDS));
            return RestUtils.getOKJAXRSResp(infoList);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Throwable t) {
            LOGGER.warn("Throwable during getSubscriptionsForChannels", t);
            ErrorResponse response = new ErrorResponse(ErrorCode.SEARCH_TOPIC_ISE, t
                    .getMessage());
            return RestUtils.getInternalErrorJAXRSResp(response);
        }
    }

    @GET
    @Path("my_subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMySubscriptions(@Context HttpHeaders headers) {
      TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
      if (tokenInfo == null) {
          return RestUtils.getUnauthJAXRSResp();
      }

      JID from = RestUtils.createJID(tokenInfo);
      JID owner = from.asBareJID();
      String appId = tokenInfo.getMmxAppId();

      try {
        long startTime = System.nanoTime();
        PubSubService service = XMPPServer.getInstance().getPubSubModule();
        List<ChannelSubscription> subList = new ArrayList<ChannelSubscription>();
        CollectionNode appNode = (CollectionNode) service.getNode(appId);
        if (appNode != null) {
          for (Node node : appNode.getNodes()) {
            Collection<NodeSubscription> subscriptions = node.getSubscriptions(owner);
            for (NodeSubscription sub : subscriptions) {
              ChannelSubscription info = ChannelSubscription.build(sub);
              subList.add(info);
            }
          }
        }
        long endTime = System.nanoTime();
        LOGGER.info("Completed processing getSubscriptions in {} milliseconds",
            TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
        return RestUtils.getOKJAXRSResp(subList);
      } catch (WebApplicationException e) {
        throw e;
      } catch (Throwable t) {
        LOGGER.warn("Throwable during getSubscriptionsForChannels", t);
        ErrorResponse response = new ErrorResponse(ErrorCode.SEARCH_TOPIC_ISE, t
                .getMessage());
        return RestUtils.getInternalErrorJAXRSResp(response);
      }
    }

    @GET
    @Path("{" + CHANNEL_NAME + "}/tags")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTags(@Context HttpHeaders headers,
                            @PathParam(CHANNEL_NAME) String channelName) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            MMXChannelId channelId = nameToId(channelName);
            ChannelTags tags = channelManager.getTags(from, appId, channelId);
            ChannelTagInfo tagInfo = new ChannelTagInfo(channelName, tags.getTags());
            return RestUtils.getOKJAXRSResp(tagInfo);
        } catch (MMXException e) {
            ErrorResponse errorResponse = new ErrorResponse();
            if (e.getCode() == StatusCode.NOT_FOUND) {
                String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND,
                        channelName);
                errorResponse.setMessage(message);
                return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(e
                                .getCode()),
                        errorResponse);
            } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getBadReqJAXRSResp(errorResponse);
            } else {
                errorResponse.setCode(ErrorCode.UNKNOWN_ERROR.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getInternalErrorJAXRSResp(errorResponse);
            }
        }
    }

    @POST
    @Path("{" + CHANNEL_NAME + "}/tags")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addTags(@Context HttpHeaders headers,
                            @PathParam(CHANNEL_NAME) String channelName, AddTagRequest request) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            ChannelTags tags = new ChannelTags(request.isPersonal() ?
                    tokenInfo.getUserId() : null, channelName, request.getTags());
            MMXStatus status = channelManager.addTags(from, appId, tags);
            return RestUtils.getOKJAXRSResp(status);
        } catch (MMXException e) {
            ErrorResponse errorResponse = new ErrorResponse();
            if (e.getCode() == StatusCode.NOT_FOUND) {
                String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND,
                        channelName);
                errorResponse.setMessage(message);
                return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(e
                                .getCode()),
                        errorResponse);
            } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getBadReqJAXRSResp(errorResponse);
            } else {
                errorResponse.setCode(ErrorCode.UNKNOWN_ERROR.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getInternalErrorJAXRSResp(errorResponse);
            }
        }
    }

    @DELETE
    @Path("{" + CHANNEL_NAME + "}/tags/" + "{"
            + MMXServerConstants.TAGNAME_PATH_PARAM + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTags(@Context HttpHeaders headers,
                               @PathParam(CHANNEL_NAME) String channelName,
                               @PathParam(MMXServerConstants.TAGNAME_PATH_PARAM) String tag,
                               @QueryParam("personal") String isPersonalChannel) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            boolean myChannel = Boolean.parseBoolean(isPersonalChannel);
            ChannelTags tags = new ChannelTags(myChannel ? tokenInfo.getUserId() : null,
                    channelName, Arrays.asList(tag));
            channelManager.removeTags(from, appId, tags);
            return RestUtils.getOKJAXRSResp();
        } catch (MMXException e) {
            ErrorResponse errorResponse = new ErrorResponse();
            if (e.getCode() == StatusCode.NOT_FOUND) {
                String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND,
                        channelName);
                errorResponse.setMessage(message);
                return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(e
                                .getCode()),
                        errorResponse);
            } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getBadReqJAXRSResp(errorResponse);
            } else {
                errorResponse.setCode(ErrorCode.UNKNOWN_ERROR.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getInternalErrorJAXRSResp(errorResponse);
            }
        }
    }

    @DELETE
    @Path("{" + CHANNEL_NAME + "}/tags")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllTags(@Context HttpHeaders headers,
                                  @PathParam(CHANNEL_NAME) String channelName,
                                  @QueryParam("personal") String isPersonalChannel) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        boolean myChannel = Boolean.parseBoolean(isPersonalChannel);
        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            ChannelTags tags = new ChannelTags(myChannel ? tokenInfo.getUserId() : null,
                    channelName, null);
            channelManager.setTags(from, appId, tags);
            return RestUtils.getOKJAXRSResp();
        } catch (MMXException e) {
            ErrorResponse errorResponse = new ErrorResponse();
            if (e.getCode() == StatusCode.NOT_FOUND) {
                String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND,
                        channelName);
                errorResponse.setMessage(message);
                return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(e
                                .getCode()),
                        errorResponse);
            } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getBadReqJAXRSResp(errorResponse);
            } else {
                errorResponse.setCode(ErrorCode.UNKNOWN_ERROR.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getInternalErrorJAXRSResp(errorResponse);
            }
        }
    }

    @GET
    @Path("{" + CHANNEL_NAME + "}/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSummary(@Context HttpHeaders headers,
                               @PathParam(CHANNEL_NAME) String channelName) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            MMXChannelId channelId = nameToId(channelName);
            SummaryRequest rqt = new SummaryRequest(Arrays.asList(channelId));
            SummaryResponse resp = channelManager.getSummary(from, appId, rqt);
//            List<MMXChannelSummary> summaryList = new ArrayList<MMXChannelSummary>();
            MMXChannelSummary singleResult = null;
            for (ChannelSummary s : resp) {
                int count = s.getCount();
                Date lastPublishedDate = s.getLastPubTime();
                String userId = s.getChannelNode().getUserId();
                String name = s.getChannelNode().getName();
                MMXChannelSummary mts = new MMXChannelSummary(userId, name, count,
                        lastPublishedDate);
                singleResult = mts;
                break;
//                summaryList.add(mts);
            }
//            MMXChannelSummaryResult summaryResult = new MMXChannelSummaryResult(summaryList);
//            return RestUtils.getOKJAXRSResp(summaryResult);
            return RestUtils.getOKJAXRSResp(singleResult);
//            return RestUtils.getOKJAXRSResp(summaryList);
        } catch (MMXException e) {
            ErrorResponse errorResponse = new ErrorResponse();
            if (e.getCode() == StatusCode.NOT_FOUND) {
                String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND,
                        channelName);
                errorResponse.setMessage(message);
                return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(e
                                .getCode()),
                        errorResponse);
            } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getBadReqJAXRSResp(errorResponse);
            } else {
                errorResponse.setCode(ErrorCode.UNKNOWN_ERROR.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getInternalErrorJAXRSResp(errorResponse);
            }
        }
    }

    public static class InviteInfo {
      private String invitationText;
      private List<String> inviteeUserIds;

      public void setInvitationText(String invitationText) {
        this.invitationText = invitationText;
      }
      /**
       * Set the user ID's of the invitees
       * @param inviteeUserIds
       */
      public void setInviteeUserIds(List<String> inviteeUserIds) {
        this.inviteeUserIds = inviteeUserIds;
      }
    }

    @POST
    @Path("{" + CHANNEL_NAME + "}/invite")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response inviteUsers(@Context HttpHeaders headers,
                              @PathParam(CHANNEL_NAME) String channelName,
                              InviteInfo inviteInfo) {
      TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
      if (tokenInfo == null) {
          return RestUtils.getUnauthJAXRSResp();
      }
      if (inviteInfo.inviteeUserIds == null || inviteInfo.inviteeUserIds.isEmpty()) {
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.INVALID_USER_NAME.getCode(),
            ErrorMessages.ERROR_INVALID_USERNAME_VALUE);
        return RestUtils.getBadReqJAXRSResp(errorResponse);
      }

      try {
        String appId = tokenInfo.getMmxAppId();
        JID from = RestUtils.createJID(tokenInfo);
        MMXChannelId channelId = nameToId(channelName);
        MMXChannelManager channelMgr = MMXChannelManager.getInstance();
        com.magnet.mmx.protocol.ChannelInfo channelInfo = channelMgr.getChannel(
            from, appId, channelId);

        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientUserIds(inviteInfo.inviteeUserIds);
        request.setReceipt(false);
        request.setMessageType(MSG_TYPE_INVITATION);
        request.setContent(buildInviteContent(channelInfo, inviteInfo));

          //add to whitelist TODO validate invitee
          if (channelId.isUserChannel() && inviteInfo.inviteeUserIds != null) {
              String topic = TopicHelper.normalizePath(channelId.getName());

              String nodeId = TopicHelper.makeTopic(appId, channelId.getUserId(), topic);
              Node node = XMPPServer.getInstance().getPubSubModule().getNode(nodeId);
              for (String inviteeId : inviteInfo.inviteeUserIds) {
                  JID jid = XMPPServer.getInstance().createJID(JIDUtil.makeNode(inviteeId, appId), null, true);
                  node.addMember(jid);
              }
          }

        MessageSender sender = new MessageSenderImpl();
        SendMessageResult result = sender.send(tokenInfo.getUserId(), appId, request);

        if (result.isError()) {
          ErrorResponse response = new ErrorResponse(result.getErrorCode(),
                                                     result.getErrorMessage());
          return RestUtils.getBadReqJAXRSResp(response);
        } else {
          SendMessageResponse response = new SendMessageResponse();
          response.setCount(result.getCount());
          response.setSentList(result.getSentList());
          response.setUnsentList(result.getUnsentList());
          return RestUtils.getOKJAXRSResp(response);
        }
      } catch (MMXException e) {
        ErrorResponse errorResponse = new ErrorResponse();
        if (e.getCode() == StatusCode.NOT_FOUND) {
          String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND,
                  channelName);
          errorResponse.setMessage(message);
          return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(
              e.getCode()), errorResponse);
        } else if (e.getCode() == StatusCode.BAD_REQUEST) {
          errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
          errorResponse.setMessage(e.getMessage());
          return RestUtils.getBadReqJAXRSResp(errorResponse);
        } else {
          errorResponse.setCode(ErrorCode.UNKNOWN_ERROR.getCode());
          errorResponse.setMessage(e.getMessage());
          return RestUtils.getInternalErrorJAXRSResp(errorResponse);
        }
      }
    }

    private static final String MSG_TYPE_INVITATION = "invitation";
    private static final String KEY_TEXT = "text";
    private static final String KEY_CHANNEL_NAME = "channelName";
    private static final String KEY_CHANNEL_SUMMARY = "channelSummary";
    private static final String KEY_CHANNEL_IS_PUBLIC = "channelIsPublic";
    private static final String KEY_CHANNEL_OWNER_ID = "channelOwnerId";
    private static final String KEY_CHANNEL_CREATION_DATE = "channelCreationDate";
    private static final String KEY_CHANNEL_PUBLISH_PERMISSIONS = "channelPublishPermissions";

    protected final HashMap<String, String> buildInviteContent(
                                    ChannelInfo channel, InviteInfo invInfo) {
      HashMap<String,String> content = new HashMap<String, String>();
      if (invInfo.invitationText != null) {
        content.put(KEY_TEXT, invInfo.invitationText);
      }
      content.put(KEY_CHANNEL_NAME, channel.getName());
      if (channel.getDescription() != null) {
        content.put(KEY_CHANNEL_SUMMARY, channel.getDescription());
      }
      content.put(KEY_CHANNEL_IS_PUBLIC, String.valueOf(!channel.isUserChannel()));
      content.put(KEY_CHANNEL_OWNER_ID, JIDUtil.getReadableUserId(channel.getCreator()));
      if (channel.getCreationDate() != null) {
        content.put(KEY_CHANNEL_CREATION_DATE, TimeUtil.toString(channel.getCreationDate()));
      }
      content.put(KEY_CHANNEL_PUBLISH_PERMISSIONS, channel.getPublishPermission().name());
      return content;
    }

    /**
     * Fetch the published items using a date range filter with pagination.  The
     * URL looks like:<br>
     * .../channels/{channelName}/items/fetch?offset={offset}&amp;since={datetime}
     * &amp;until={datetime}&amp;size={itemsPerPage}&smp;sort_order={ASC|DESC}
     * @param headers
     * @param channelName A public channel or private channel (userID#channelName)
     * @param sortOrder
     * @param since
     * @param until
     * @param offset
     * @param size
     * @return
     */
    @GET
    @Path("{" + CHANNEL_NAME + "}/items/fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response fetchItems(@Context HttpHeaders headers,
                               @PathParam(CHANNEL_NAME) String channelName,
                               @QueryParam(MMXServerConstants.SORT_ORDER_PARAM) String sortOrder,
                               @QueryParam(MMXServerConstants.SINCE_PARAM) String since,
                               @QueryParam(MMXServerConstants.UNTIL_PARAM) String until,
                               @QueryParam(MMXServerConstants.OFFSET_PARAM) Integer offset,
                               @QueryParam(MMXServerConstants.SIZE_PARAM)
                               @DefaultValue(DEFAULT_MAX_ITEMS) int size) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        if (offset == null) {
          offset = DEFAULT_OFFSET;
        }
        if (sortOrder == null) {
          sortOrder = MMXServerConstants.SORT_ORDER_ASCENDING;
        }

        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            MMXChannelId channelId = nameToId(channelName);
            FetchOptions opt = new FetchOptions()
                    .setMaxItems(size)
                    .setOffset(offset)
                    .setAscending(
                            sortOrder.equalsIgnoreCase(MMXServerConstants.SORT_ORDER_ASC))
                    .setSince(since == null ? null : new DateTime(since).toDate())
                    .setUntil(until == null ? null : new DateTime(until).toDate());
            FetchRequest rqt = new FetchRequest(channelId.getEscUserId(), channelId
                    .getName(),
                    opt);
            FetchResponse resp = channelManager.fetchItems(from, appId, rqt);

            String nodeId = ChannelHelper.makeChannel(appId, channelId.getEscUserId(), channelId.getName());
            List<TopicItemEntity> channelItemEntities = toTopicItemEntity(nodeId, resp.getItems());
            List<MMXPubSubItemChannel2> items = toPubSubItems(channelId, channelItemEntities);
            PubSubItemResultChannels result = new PubSubItemResultChannels(resp.getTotal(), items);
            return RestUtils.getOKJAXRSResp(result);
        } catch (IllegalArgumentException e) {
          String message = String.format(ErrorMessages.ERROR_DATE_BAD_FORMAT,
                  e.getMessage());
          ErrorResponse errorResponse = new ErrorResponse();
          errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
          errorResponse.setMessage(message);
          return RestUtils.getBadReqJAXRSResp(errorResponse);
        } catch (MMXException e) {
            ErrorResponse errorResponse = new ErrorResponse();
            if (e.getCode() == StatusCode.NOT_FOUND) {
                String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND,
                        channelName);
                errorResponse.setMessage(message);
                return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(e
                                .getCode()),
                        errorResponse);
            } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getBadReqJAXRSResp(errorResponse);
            } else {
                errorResponse.setCode(ErrorCode.UNKNOWN_ERROR.getCode());
                errorResponse.setMessage(e.getMessage());
                return RestUtils.getInternalErrorJAXRSResp(errorResponse);
            }
        }
    }

    public static class MMXPubSubItemChannel2 {
      private final String itemId;
      private final String channelName;
      private final MMXItemPublisher publisher;
      private final Map<String, String> content;
      private final MMXPubSubPayload metaData;

      public MMXPubSubItemChannel2(MMXPubSubItemChannel item) {
        itemId = item.getItemId();
        channelName = item.getChannelName();
        publisher = item.getPublisher();
        content = item.getMeta();
        metaData = item.getPayload();
      }

      public String getItemId() {
        return itemId;
      }

      public String getChannelName() {
        return channelName;
      }

      public MMXItemPublisher getPublisher() {
        return publisher;
      }

      public Map<String, String> getContent() {
        return content;
      }

      public MMXPubSubPayload getMetaData() {
        return metaData;
      }
    }

    /**
     * Get the published items by their ID's.  The channel name can be a public
     * channel or private channel.  The URL may look like:<br>
     * .../channels/{channelName}/items?id={itemID1}&amp;id={itemID2}
     * @param headers
     * @param channelName Public channel or private channel (userID#channelName).
     * @param idList
     * @return
     */
    @GET
    @Path("{" + CHANNEL_NAME + "}/items")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response getItemsByIds(@Context HttpHeaders headers,
                                  @PathParam(CHANNEL_NAME) String channelName,
                                  @QueryParam(ID_KEY) List<String> idList) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            MMXChannelId channelId = nameToId(channelName);
            ItemsByIdsRequest rqt = new ItemsByIdsRequest(channelId.getEscUserId(),
                    channelId.getName(), idList);
            FetchResponse resp = channelManager.getItems(from, appId, rqt);

            String nodeId = ChannelHelper.makeChannel(appId, channelId.getEscUserId(), channelId.getName());
            List<TopicItemEntity> channelItemEntities = toTopicItemEntity(nodeId, resp.getItems());
            List<MMXPubSubItemChannel2> items = toPubSubItems(channelId, channelItemEntities);
            PubSubItemResultChannels result = new PubSubItemResultChannels(resp.getTotal(), items);

            return RestUtils.getOKJAXRSResp(result);
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
                        "Channel items cannot be retracted: " + channelName);
            } else if (e.getCode() == StatusCode.GONE) {
                status = Response.Status.GONE;
                response = new ErrorResponse(ErrorCode.TOPIC_SUBSCRIBE_FORBIDDEN,
                        "Channel items cannot be retracted: " + channelName);
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR;
                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
            }
            return RestUtils.getJAXRSResp(status, response);
        }
    }

    /**
     * Delete all published items from a channel owned by the current user.
     * The URL may look like:<br>
     * .../channels/{channelName}/items/all?personal=boolean
     * @param headers
     * @param channelName The channel name.
     * @param isPersonalChannel true for private channel, false for public channel
     * @return
     */
    @DELETE
    @Path("{" + CHANNEL_NAME + "}/items/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllItems(@Context HttpHeaders headers,
                                   @PathParam(CHANNEL_NAME) String channelName,
                                   @QueryParam("personal") String isPersonalChannel) {
        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        boolean myChannel = Boolean.parseBoolean(isPersonalChannel);
        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            MMXChannelId channelId = nameToId(channelName);
            RetractAllRequest rqt = new RetractAllRequest(channelName, myChannel);
            MMXStatus resp = channelManager.retractAllFromChannel(from, appId, rqt);
            return RestUtils.getOKJAXRSResp();
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
                        "Channel items cannot be retracted: " + channelName);
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR;
                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
            }
            return RestUtils.getJAXRSResp(status, response);
        }
    }

    /**
     * Delete the items published by the current user from a public channel or
     * private channel.  The URL may look like:<br>
     * .../channels/{channelName}/items?id={itemID1}&amp;id={itemID2}
     * @param headers
     * @param channelName A public channel name or private channel name (userID#channelName)
     * @param idList
     * @return
     */
    @DELETE
    @Path("{" + CHANNEL_NAME + "}/items")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteItemsByIds(@Context HttpHeaders headers,
                                     @PathParam(CHANNEL_NAME) String channelName,
                                     @QueryParam(ID_KEY) List<String> idList) {
        ErrorResponse errorResponse;
        if (idList == null || idList.isEmpty()) {
            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Item ids are not specified");
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }

        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
        if (tokenInfo == null) {
            return RestUtils.getUnauthJAXRSResp();
        }

        JID from = RestUtils.createJID(tokenInfo);
        String appId = tokenInfo.getMmxAppId();
        try {
            MMXChannelManager channelManager = MMXChannelManager.getInstance();
            MMXChannelId channelId = nameToId(channelName);
            RetractRequest rqt = new RetractRequest(channelId.getEscUserId(),
                    channelId.getName(), idList);
            Map<String, Integer> resp = channelManager.retractFromChannel(from, appId, rqt);
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
                        "Channel items cannot be retracted: " + channelName);
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR;
                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
            }
            return RestUtils.getJAXRSResp(status, response);
        }
    }

    private List<TopicItemEntity> toTopicItemEntity(String nodeId,
                                                    List<MMXPublishedItem> items) {
        List<TopicItemEntity> list = new ArrayList<TopicItemEntity>(items.size());
        for (MMXPublishedItem item : items) {
            TopicItemEntity itemEntity = new TopicItemEntity();
            itemEntity.setNodeId(nodeId);
            itemEntity.setJid(item.getPublisher());
            itemEntity.setId(item.getItemId());
            itemEntity.setPayload(item.getPayloadXml());
            itemEntity.setCreationDate(StringUtils.dateToMillis(item.getCreationDate()));
            itemEntity.setServiceId("pubsub");
            list.add(itemEntity);
        }
        return list;
    }

    private List<MMXPubSubItemChannel2> toPubSubItems(final MMXChannelId channelId, List<TopicItemEntity> entityList) {
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
        List<MMXPubSubItemChannel2> items2 = new ArrayList<MMXPubSubItemChannel2>(items.size());
        for (MMXPubSubItemChannel item : items) {
          items2.add(new MMXPubSubItemChannel2(item));
        }
        return items2;
    }

    protected ConnectionProvider getConnectionProvider() {
        return new OpenFireDBConnectionProvider();
    }

//    protected SearchResult<ChannelNode> transform(
//            String appId, SearchResult<ChannelInfoWithSubscriptionCount> results) {
//        SearchResult<ChannelNode> nodes = new SearchResult<ChannelNode>();
//        nodes.setOffset(results.getOffset());
//        nodes.setSize(results.getSize());
//        nodes.setTotal(results.getTotal());
//
//        List<ChannelInfoWithSubscriptionCount> objects = results.getResults();
//        List<ChannelNode> nodeList = new LinkedList<ChannelNode>();
//
//        for (ChannelInfoWithSubscriptionCount object : objects) {
//            ChannelNode node = new ChannelNode();
//            // TODO: hack to fix MOB-2516;display a user channel as userId#channelName.
//            node.setChannelName(idToName(object.getUserId(), object.getName()));
//            node.setUserId(object.getUserId());
//            node.setCollection(object.isCollection());
//            node.setDescription(object.getDescription());
//            node.setPersistent(object.isPersistent());
//            node.setMaxItems(object.getMaxItems());
//            node.setMaxPayloadSize(object.getMaxPayloadSize());
//            node.setPublishPermission(object.getPublishPermission().name());
//            Date creationDate = object.getCreationDate();
//            Date modified = object.getModifiedDate();
//            DateFormat isoFormatter = Utils.buildISO8601DateFormat();
//            node.setCreationDate(isoFormatter.format(creationDate));
//            node.setModificationDate(isoFormatter.format(modified));
//            node.setSubscriptionEnabled(object.isSubscriptionEnabled());
//            node.setSubscriptionCount(object.getSubscriptionCount());
//            nodeList.add(node);
//        }
//        nodes.setResults(nodeList);
//        return nodes;
//    }

    protected List<ChannelSubscription> getChannelSubscriptions(String appId,
                                                         MMXChannelId cid) {
        String channelId = ChannelHelper.makeChannel(appId, cid.getEscUserId(),
            cid.getName());
        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        List<NodeSubscription> subscriptions = channelManager
                .listSubscriptionsForChannel(channelId);
        List<ChannelSubscription> infoList = new ArrayList<ChannelSubscription>(
                subscriptions.size());
        for (NodeSubscription sub : subscriptions) {
            ChannelSubscription info = ChannelSubscription.build(sub);
            infoList.add(info);
        }
        return infoList;
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

    public static class MMXPubSubItemChannel2Ext extends MMXPubSubItemChannel2 {
        private UserEntity publisherInfo;
        UserDAO userDAO = new UserDAOImpl( new OpenFireDBConnectionProvider());
        public MMXPubSubItemChannel2Ext(MMXPubSubItemChannel item, String appId) {
            super(item);
            publisherInfo = userDAO.getUser(JIDUtil.makeNode(item.getPublisher().getUserId(), appId));
        }

        public UserEntity getPublisherInfo() {
            return publisherInfo;
        }

        public MMXPubSubItemChannel2Ext setPublisherInfo(UserEntity publisherInfo) {
            this.publisherInfo = publisherInfo;
            return this;
        }
    }



}

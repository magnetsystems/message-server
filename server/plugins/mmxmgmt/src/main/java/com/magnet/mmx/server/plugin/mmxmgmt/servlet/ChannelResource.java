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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.SearchAction.Operator;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicAction.CreateRequest;
import com.magnet.mmx.protocol.TopicAction.DeleteRequest;
import com.magnet.mmx.protocol.TopicAction.TopicInfoWithSubscriptionCount;
import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.api.v1.protocol.TopicCreateInfo;
import com.magnet.mmx.server.api.v1.protocol.TopicSubscription;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXTopicManager;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSenderImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.message.TopicPostResult;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicNode;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.TopicHelper;
import com.magnet.mmx.util.Utils;

@Path("channels/")
public class ChannelResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelResource.class);
  static final String DESCRIPTION_KEY = "description";
  static final String TAGS_KEY = "tag";
  static final String CHANNEL_NAME = "channelName";

  @Context
  private HttpServletRequest servletRequest;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("create")
  public Response createChannel(@Context HttpHeaders headers, /* @HeaderParam("X-mmx-app-id") String mmxappid, */ TopicCreateInfo topicInfo) {
    ErrorResponse errorResponse = null;

    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    
    if(topicInfo == null) {
     errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Channel information not set");
     return RestUtils.getBadReqJAXRSResp(errorResponse);
    }

    if(!TopicHelper.validateApplicationTopicName(topicInfo.getTopicName())) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, MMXTopicManager.StatusCode.INVALID_TOPIC_NAME.getMessage());
      return RestUtils.getBadReqJAXRSResp(errorResponse);
    }

    if(!Strings.isNullOrEmpty(topicInfo.getDescription()) && topicInfo.getDescription().length() > MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "channel description too long, max length = " +
              MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN);
      return RestUtils.getBadReqJAXRSResp(errorResponse);
    }

    try {
      MMXTopicManager topicManager = MMXTopicManager.getInstance();
      JID from = RestUtils.createJID(tokenInfo);
      CreateRequest rqt = toCreateRequest(topicInfo);
      topicManager.createTopic(from, tokenInfo.getMmxAppId(), rqt);
      return RestUtils.getCreatedJAXRSResp();
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.CONFLICT) {
        errorResponse = new ErrorResponse(ErrorCode.TOPIC_EXISTS, "Channel already exists");
      } else if (e.getCode() == StatusCode.FORBIDDEN) {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e.getMessage());
      } else if (e.getCode() == StatusCode.BAD_REQUEST) {
        errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e.getMessage());
      } else {
        errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
        return RestUtils.getInternalErrorJAXRSResp(errorResponse);
      }
      return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(e.getCode()), errorResponse);
    } catch (Throwable e) {
      errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
  }
  
  private CreateRequest toCreateRequest(TopicCreateInfo createInfo) {
    MMXTopicOptions options = new MMXTopicOptions()
      .setDescription(createInfo.getDescription())
      .setMaxItems(createInfo.getMaxItems())
      .setSubscribeOnCreate(createInfo.isSubscribeOnCreate())
      .setSubscriptionEnabled(createInfo.isSubscriptionEnabled())
      .setPublisherType(createInfo.getPublishPermission());
    CreateRequest rqt = new CreateRequest(createInfo.getTopicName(),
        createInfo.isPersonalTopic(), options);
    return rqt;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{" + CHANNEL_NAME + "}/publish")
  public Response publishMessage(@Context HttpHeaders headers,
                                 @PathParam(CHANNEL_NAME) String topic,
                                 TopicPostMessageRequest request) {
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    
    try {
      MessageSender sender = new MessageSenderImpl();
      TopicPostResult result = sender.postMessage(topic, tokenInfo.getMmxAppId(), request);
      TopicPostResponse sendResponse = new TopicPostResponse();
      if (result.isError()) {
        LOGGER.info("Problem posting message:" + result.getErrorMessage());
        sendResponse.setMessage(result.getErrorMessage());
        sendResponse.setStatus(result.getStatus());
        throw new WebApplicationException(
            Response
                .status(result.getErrorCode() == ErrorCode.TOPIC_PUBLISH_FORBIDDEN.getCode() ? Response.Status.FORBIDDEN :Response.Status.BAD_REQUEST)
                .entity(sendResponse)
                .build()
        );
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
      ErrorResponse error = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "Error processing request");
      return RestUtils.getInternalErrorJAXRSResp(error);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchChannels(@Context HttpHeaders headers,
                                @QueryParam(CHANNEL_NAME) String topicName,
                                @QueryParam(DESCRIPTION_KEY) String description,
                                @QueryParam(TAGS_KEY) List<String> tags,
                                @QueryParam(MMXServerConstants.SIZE_PARAM) Integer size,
                                @QueryParam(MMXServerConstants.OFFSET_PARAM) Integer offset) {
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();
    TopicAction.TopicSearchRequest rqt = toSearchRequest(topicName, description,
                                          tags, size, offset);
    MMXTopicManager topicManager = MMXTopicManager.getInstance();
    try {
      long startTime = System.nanoTime();
      TopicAction.TopicQueryResponse resp = topicManager.searchTopic(from,
                                                            appId, rqt, null);
      long endTime = System.nanoTime();
      LOGGER.info("Completed processing searchTopics in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return RestUtils.getOKJAXRSResp(resp);
    } catch (MMXException e) {
      LOGGER.warn("Throwable during searchTopics", e);
      ErrorResponse response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR.getCode(),
          "Search channel failed: "+e.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(response);
    }
  }

  private TopicAction.TopicSearchRequest toSearchRequest(String topicName,
      String description, List<String> tags, Integer size, Integer offset) {
    TopicAction.TopicSearch attr = new TopicAction.TopicSearch()
      .setTopicName(topicName)
      .setDescription(description)
      .setTags(tags);
    TopicAction.TopicSearchRequest rqt = new TopicAction.TopicSearchRequest(
        Operator.AND, attr, offset, size);
    return rqt;
  }
  
  @GET
  @Path("{" + CHANNEL_NAME + "}/subscribe")
  @Produces(MediaType.APPLICATION_JSON)
  public Response subscribeChannel(@Context HttpHeaders headers, 
                                    @PathParam(CHANNEL_NAME) String topicName) {
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();
    MMXTopicId topicId = nameToId(topicName);
    
    MMXTopicManager topicManager = MMXTopicManager.getInstance();
    try {
      TopicAction.SubscribeRequest rqt = new TopicAction.SubscribeRequest(
          topicId.getEscUserId(), topicId.getName(), null);
      TopicAction.SubscribeResponse resp = topicManager.subscribeTopic(from,
          appId, rqt, null);
      return RestUtils.getOKJAXRSResp(resp);
    } catch (MMXException e) {
      Response.Status status;
      ErrorResponse response;
      if (e.getCode() == StatusCode.NOT_FOUND) {
        status = Response.Status.NOT_FOUND;
        response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST,
            "Channel does not exist: "+topicName);
      } else if (e.getCode() == StatusCode.FORBIDDEN) {
        status = Response.Status.FORBIDDEN;
        response = new ErrorResponse(ErrorCode.TOPIC_SUBSCRIBE_FORBIDDEN,
            "Channel cannot be subscribed: "+topicName);
      } else if (e.getCode() == StatusCode.CONFLICT) {
        status = Response.Status.CONFLICT;
        response = new ErrorResponse(ErrorCode.TOPIC_ALREADY_SUBSCRIBED,
            "Channel is already subscribed: "+topicName);
      } else {
        status = Response.Status.INTERNAL_SERVER_ERROR;
        response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
      }
      return RestUtils.getJAXRSResp(status, response);      
    }
  }
  
  @GET
  @Path("{" + CHANNEL_NAME + "}/unsubscribe")
  @Produces(MediaType.APPLICATION_JSON)
  public Response unsubscribeChannel(@Context HttpHeaders headers,
                                    @PathParam(CHANNEL_NAME) String topicName) {
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();
    MMXTopicId topicId = nameToId(topicName);
    
    MMXTopicManager topicManager = MMXTopicManager.getInstance();
    try {
      TopicAction.UnsubscribeRequest rqt = new TopicAction.UnsubscribeRequest(
          topicId.getEscUserId(), topicId.getName(), null);
      MMXStatus resp = topicManager.unsubscribeTopic(from, appId, rqt);
      return RestUtils.getOKJAXRSResp(resp);
    } catch (MMXException e) {
      Response.Status status;
      ErrorResponse response;
      if (e.getCode() == StatusCode.NOT_FOUND) {
        status = Response.Status.NOT_FOUND;
        response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST,
            "Channel does not exist: "+topicName);
      } else if (e.getCode() == StatusCode.GONE) {
        status = Response.Status.GONE;
        response = new ErrorResponse(ErrorCode.TOPIC_NOT_SUBSCRIBED,
            "Channel is not subscribed: "+topicName);
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
  public Response getChannel(@Context HttpHeaders headers, @PathParam(CHANNEL_NAME) String topicName) {
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    
    JID from = RestUtils.createJID(tokenInfo);
    String appId = RestUtils.getAppEntity(servletRequest).getAppId();
    MMXTopicId tid = nameToId(topicName);
    String topicId = TopicHelper.makeTopic(appId, tid.getEscUserId(), tid.getName());
    MMXTopicManager topicManager = MMXTopicManager.getInstance();
    try {
      com.magnet.mmx.protocol.TopicInfo info = topicManager.getTopic(from, appId, tid);
      return RestUtils.getOKJAXRSResp(info);
    } catch (MMXException e) {
      ErrorResponse response;
      if (e.getCode() == StatusCode.NOT_FOUND) {
        response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST.getCode(),
            "Channel not found: "+topicName);
        return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, response);
      } else {
        response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR.getCode(), e.getMessage());
        return RestUtils.getInternalErrorJAXRSResp(response);
      }
    }
  }

  @DELETE
  @Path("{" + CHANNEL_NAME + "}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteChannel(@Context HttpHeaders headers,
      @PathParam(CHANNEL_NAME) String topicName,
      @QueryParam("personal") boolean isPersonalTopic) {

    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    
    ErrorResponse errorResponse = null;
    try {
      MMXTopicManager topicManager = MMXTopicManager.getInstance();
      JID from = RestUtils.createJID(tokenInfo);
      DeleteRequest rqt = new DeleteRequest(topicName, isPersonalTopic);
      topicManager.deleteTopic(from, tokenInfo.getMmxAppId(), rqt);
      return RestUtils.getOKJAXRSResp();
    } catch (MMXException e) {
      errorResponse = new ErrorResponse();
      if (e.getCode() == StatusCode.NOT_FOUND) {
        String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND, topicName);
        errorResponse.setMessage(message);
        return RestUtils.getJAXRSResp(Response.Status.fromStatusCode(e.getCode()),
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
      @PathParam(CHANNEL_NAME) String topicName) {
    
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }

    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    try {
      long startTime = System.nanoTime();
      List<TopicSubscription> infoList = getTopicSubscriptions(appId, topicName);
      long endTime = System.nanoTime();
      LOGGER.info("Completed processing getSubscriptionsForTopics in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return RestUtils.getOKJAXRSResp(infoList);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.warn("Throwable during getSubscriptionsForTopics", t);
      ErrorResponse response = new ErrorResponse(ErrorCode.SEARCH_TOPIC_ISE, t.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(response);
    }
  }
  
  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }


  protected SearchResult<com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicNode> transform (String appId, SearchResult<TopicInfoWithSubscriptionCount> results) {
    SearchResult<com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicNode> nodes = new SearchResult<TopicNode>();
    nodes.setOffset(results.getOffset());
    nodes.setSize(results.getSize());
    nodes.setTotal(results.getTotal());

    List<TopicInfoWithSubscriptionCount> objects = results.getResults();
    List<TopicNode> nodeList = new LinkedList<TopicNode>();

    for (TopicInfoWithSubscriptionCount object : objects) {
      TopicNode node = new TopicNode();
      // TODO: hack to fix MOB-2516;display a user topic as userId#topicName.
      node.setTopicName(idToName(object.getUserId(), object.getName()));
      node.setUserId(object.getUserId());
      node.setCollection(object.isCollection());
      node.setDescription(object.getDescription());
      node.setPersistent(object.isPersistent());
      node.setMaxItems(object.getMaxItems());
      node.setMaxPayloadSize(object.getMaxPayloadSize());
      node.setPublisherType(object.getPublisherType().name());
      Date creationDate = object.getCreationDate();
      Date modified = object.getModifiedDate();
      DateFormat isoFormatter = Utils.buildISO8601DateFormat();
      node.setCreationDate(isoFormatter.format(creationDate));
      node.setModificationDate(isoFormatter.format(modified));
      node.setSubscriptionEnabled(object.isSubscriptionEnabled());
      node.setSubscriptionCount(object.getSubscriptionCount());
      nodeList.add(node);
    }
    nodes.setResults(nodeList);
    return nodes;
  }


  protected List<TopicSubscription> getTopicSubscriptions (String appId, String topicName) {
    MMXTopicId tid = nameToId(topicName);
    String topicId = TopicHelper.makeTopic(appId, tid.getEscUserId(), tid.getName());
    MMXTopicManager topicManager = MMXTopicManager.getInstance();
    List<NodeSubscription> subscriptions = topicManager.listSubscriptionsForTopic(topicId);
    List<TopicSubscription> infoList = new ArrayList<TopicSubscription>(subscriptions.size());
    for (NodeSubscription sub : subscriptions) {
      TopicSubscription info = TopicSubscription.build(sub);
      infoList.add(info);
    }
    return infoList;
  }
  
  // The hack to fix MOB-2516 that allows the console to display user topics as
  // userID#topicName.  This method parses the global topic or user topic properly.
  public static MMXTopicId nameToId(String topicName) {
    int index = topicName.indexOf(TopicHelper.TOPIC_SEPARATOR);
    if (index < 0) {
      return new MMXTopicId(topicName);
    } else {
      return new MMXTopicId(topicName.substring(0, index), topicName.substring(index+1));
    }
  }
  
  // The hack to fix MOB-2516 to convert a user topic to userId#topicName
  public static String idToName(String userId, String topicName) {
    if (userId == null) {
      return topicName;
    } else {
      return userId + TopicHelper.TOPIC_SEPARATOR + topicName;
    }
  }
}

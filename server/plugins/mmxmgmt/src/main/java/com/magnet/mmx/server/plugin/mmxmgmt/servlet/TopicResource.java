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
import java.util.Collections;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jivesoftware.openfire.pubsub.LeafNode;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.TopicAction.TopicInfoWithSubscriptionCount;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.api.v1.protocol.TopicCreateInfo;
import com.magnet.mmx.server.api.v1.protocol.TopicInfo;
import com.magnet.mmx.server.api.v1.protocol.TopicSubscription;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.TopicQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TagDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXTopicManager;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSenderImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.message.TopicPostResult;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.PubSubPersistenceManagerExt;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.TopicQueryBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicNode;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.TopicHelper;
import com.magnet.mmx.util.Utils;

@Path("topics/")
public class TopicResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopicResource.class);
  private final static Integer DEFAULT_PAGE_SIZE = Integer.valueOf(100);
  private final static Integer DEFAULT_OFFSET = Integer.valueOf(0);
  static final String  APP_ID_KEY = "appId";
  static final String  DESCRIPTION_KEY = "description";
  static final String  TAGS_KEY = "tag";
  static final String TOPIC_NAME = "topicName";

  @Context
  private HttpServletRequest servletRequest;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response createTopic(TopicCreateInfo topicInfo) {
    ErrorResponse errorResponse = null;

    if(topicInfo == null) {
     errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Topic information not set");
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

    AppEntity appEntity;
    Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);
    if (o instanceof AppEntity) {
      appEntity = (AppEntity) o;
    } else {
      LOGGER.error("createTopics : appEntity is not set");
      return Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .build();
    }

    try {
      MMXTopicManager topicManager = MMXTopicManager.getInstance();
      MMXTopicManager.TopicActionResult result = topicManager.createTopic(appEntity, topicInfo);
      if(result.isSuccess()) {
        return RestUtils.getCreatedJAXRSResp();
      } else {
        if(result.getCode().equals(MMXTopicManager.TopicFailureCode.DUPLICATE)) {
          errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Channel already exists");
          return RestUtils.getBadReqJAXRSResp(errorResponse);
        }
        if(result.getCode().equals(MMXTopicManager.TopicFailureCode.INVALID_TOPIC_ID)) {
          errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Invalid channel id");
          return RestUtils.getBadReqJAXRSResp(errorResponse);
        }
      }
    } catch (Exception e) {
      errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "Unknown error");
      return RestUtils.getInternalErrorJAXRSResp(errorResponse);
    }
    return null;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{" + TOPIC_NAME + "}/publish")
  public Response postMessage(@PathParam(TOPIC_NAME) String topic,
                                                            TopicPostMessageRequest request) {
    try {
      AppEntity appEntity;
      Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);
      if (o instanceof AppEntity) {
        appEntity = (AppEntity) o;
      } else {
        LOGGER.error("postMessage : appEntity is not set");
        throw new WebApplicationException(Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .build());
      }

      MessageSender sender = new MessageSenderImpl();
      TopicPostResult result = sender.postMessage(topic, appEntity.getAppId(), request);
      TopicPostResponse sendResponse = new TopicPostResponse();
      if (result.isError()) {
        LOGGER.info("Problem posting message:" + result.getErrorMessage());
        sendResponse.setMessage(result.getErrorMessage());
        sendResponse.setStatus(result.getStatus());
        throw new WebApplicationException(
            Response.status(
                result.getErrorCode() == ErrorCode.TOPIC_PUBLISH_FORBIDDEN.getCode() ?
                    Response.Status.FORBIDDEN : Response.Status.BAD_REQUEST)
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
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(error)
              .build()
      );
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchTopics(@QueryParam(TOPIC_NAME) String topicName,
                               @QueryParam(DESCRIPTION_KEY) String description,
                               @QueryParam(TAGS_KEY) List<String> tags,
                               @QueryParam(MMXServerConstants.SIZE_PARAM) Integer size,
                               @QueryParam(MMXServerConstants.OFFSET_PARAM) Integer offset
                              ) {
    try {
      long startTime = System.nanoTime();
      AppEntity appEntity;
      Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);
      if (o instanceof AppEntity) {
        appEntity = (AppEntity) o;
      } else {
        LOGGER.error("searchTopics : appEntity is not set");
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .build();
      }
      String appId = appEntity.getAppId();
      TopicQuery query = new TopicQuery();

      if (topicName != null && !topicName.isEmpty()) {
        query.setTopicName(topicName);
      }
      if (description != null && !description.isEmpty()) {
        query.setDescription(description);
      }

      if (tags != null && !tags.isEmpty()) {
        query.setTags(tags);
      }

      if (size == null) {
        size = DEFAULT_PAGE_SIZE;
      }
      if (offset == null) {
        offset = DEFAULT_OFFSET;
      }
      PaginationInfo paginationInfo = PaginationInfo.build(size, offset);

      TopicQueryBuilder queryBuilder = new TopicQueryBuilder();
      QueryBuilderResult builtQuery = queryBuilder.buildPaginationQueryWithOrder(
          query, appId, paginationInfo, null,
          Collections.singletonList(MMXServerConstants.TOPIC_ROLE_PUBLIC));

      SearchResult<TopicInfoWithSubscriptionCount> topicList = 
          PubSubPersistenceManagerExt.getTopicWithPagination(getConnectionProvider(), 
              builtQuery, paginationInfo);

      SearchResult<TopicNode> nodes = transform(appId, topicList);

      TagDAO tagDAO = DBUtil.getTagDAO();

      for (TopicNode node : nodes.getResults()) {
        String userId = node.getUserId();
        String topic = node.getTopicName();
        // TODO: hack for MOB-2516 that topic may have the format as userID/topicName.
        // When the console UI is fixed, don't call nametoId().
        MMXTopicId tid = nameToId(topic);
        List<String> topicTags = tagDAO.getTagsForTopic(appId, "pubsub",
            TopicHelper.makeTopic(appId, tid.getEscUserId(), tid.getName()));
        node.setTags(topicTags);
      }
      Response response = Response
          .status(Response.Status.OK)
          .entity(nodes)
          .build();

      long endTime = System.nanoTime();
      LOGGER.info("Completed processing searchTopics in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return response;

    } catch (WebApplicationException e) {
      throw e;

    } catch (Throwable t) {
      LOGGER.warn("Throwable during searchTopics", t);
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse(ErrorCode.SEARCH_TOPIC_ISE, t.getMessage()))
              .build()
      );
    }
  }

  @GET
  @Path("{" + TOPIC_NAME + "}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTopic(@PathParam(TOPIC_NAME) String topicName) {
    String appId = RestUtils.getAppEntity(servletRequest).getAppId();
    MMXTopicId tid = nameToId(topicName);
    String topicId = TopicHelper.makeTopic(appId, tid.getEscUserId(), tid.getName());
    MMXTopicManager topicManager = MMXTopicManager.getInstance();
    Node node = topicManager.getTopicNode(topicId);
    if(node instanceof LeafNode) {
      LeafNode leafNode = (LeafNode) node;
      TopicInfo info = new TopicInfo();
      info.setTopicName(node.getName());
      info.setPublisherType(leafNode.getPublisherModel().getName());
      info.setSubscriptionEnabled(leafNode.isSubscriptionEnabled());
      info.setMaxItems(leafNode.getMaxPublishedItems());
      info.setDescription(leafNode.getDescription());
      return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(info).build();
    } else {
      ErrorResponse errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Channel not found");
      return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, errorResponse);
    }
  }

  @DELETE
  @Path("{" + TOPIC_NAME + "}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteTopics(@PathParam(TOPIC_NAME) String topicName) {
    AppEntity appEntity;
    Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);
    if (o instanceof AppEntity) {
      appEntity = (AppEntity) o;
    } else {
      LOGGER.error("deleteTopics : appEntity is not set");
      return Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .build();
    }

    String appId = appEntity.getAppId();
    MMXTopicId tid = nameToId(topicName);
    String topicId = TopicHelper.makeTopic(appId, tid.getEscUserId(), tid.getName());

    MMXTopicManager topicManager = MMXTopicManager.getInstance();
    MMXTopicManager.TopicActionResult result = topicManager.deleteTopic(appId, topicId);
    if (result.isSuccess()) {
      return RestUtils.getOKJAXRSResp();
    } else {
      MMXTopicManager.TopicFailureCode code = result.getCode();
      ErrorResponse errorResponse = new ErrorResponse();
      errorResponse.setCode(ErrorCode.ILLEGAL_ARGUMENT.getCode());
      Response response;
      switch(code) {
        case INVALID_TOPIC_ID:
          errorResponse.setMessage("Invalid channel name");
          response = RestUtils.getBadReqJAXRSResp(errorResponse);
          break;
        case NOTFOUND:
          String message = String.format(ErrorMessages.ERROR_TOPIC_NOT_FOUND, topicName);
          errorResponse.setMessage(message);
          response = RestUtils.getBadReqJAXRSResp(errorResponse);
          break;
        default:
          errorResponse.setMessage("Unknown error processing topic id");
          response = RestUtils.getInternalErrorJAXRSResp(errorResponse);
      }
      return response;
    }
  }

  @GET
  @Path("{" + TOPIC_NAME + "}/subscriptions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscriptionsForTopics(@PathParam(TOPIC_NAME) String topicName) {
    try {
      long startTime = System.nanoTime();
      AppEntity appEntity;
      Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);
      if (o instanceof AppEntity) {
        appEntity = (AppEntity) o;
      } else {
        LOGGER.error("getSubscriptionsForTopics : appEntity is not set");
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .build();
      }
      List<TopicSubscription> infoList = getTopicSubscriptions(appEntity.getAppId(), topicName);
      Response response = Response
          .status(Response.Status.OK)
          .entity(infoList)
          .build();
      long endTime = System.nanoTime();
      LOGGER.info("Completed processing getSubscriptionsForTopics in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return response;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.warn("Throwable during getSubscriptionsForTopics", t);
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse(ErrorCode.SEARCH_TOPIC_ISE, t.getMessage()))
              .build()
      );
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

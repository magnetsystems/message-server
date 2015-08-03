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
package com.magnet.mmx.server.plugin.mmxmgmt.api.tags;

import com.magnet.mmx.server.plugin.mmxmgmt.api.AbstractBaseResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TagDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TopicEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.TopicHelper;
import com.magnet.mmx.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 */
@Path("/topics/{" + MMXServerConstants.TOPICNAME_PATH_PARAM +"}/tags")
public class MMXTopicTagsResource extends AbstractBaseResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXTopicTagsResource.class);

  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response getTagsForTopics(@Context HttpHeaders headers,
                                   @PathParam(MMXServerConstants.TOPICNAME_PATH_PARAM) String topicId) {
    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if(errorResponse != null) {
      return errorResponse.toJaxRSResponse();
    }

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    LOGGER.trace("getTagsForTopics : appId={}, topicId={}", appId, topicId);

    String nodeId = TopicHelper.makeTopic(appId, null, topicId);
    TopicEntity entity = DBUtil.getTopicDAO().getTopic(MMXServerConstants.DEFAULT_PUBSUB_SERVICE_ID,
            nodeId);
    if(entity == null ) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Topic not found");
      return Response.status(Response.Status.NOT_FOUND)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }

    try {
      List<String> tagList = DBUtil.getTagDAO().getTagsForTopic(appId,
                              MMXServerConstants.DEFAULT_PUBSUB_SERVICE_ID, nodeId);
      TopicTagInfo tagInfo = new TopicTagInfo(topicId, tagList);
      return Response.status(Response.Status.OK)
                     .type(MediaType.APPLICATION_JSON).entity(tagInfo)
                     .build();
    } catch (Exception e) {
      LOGGER.error("getTagsForTopics : error getting tags for topic={}", topicId);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error getting tags for topic");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response addTopicTag(@Context HttpHeaders headers,
                              @PathParam(MMXServerConstants.TOPICNAME_PATH_PARAM) String topicId,
                              TagList tagList) {
    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

    if(tagList == null || Utils.isNullOrEmpty(tagList.getTags())) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Tag list is not set");
      return Response
              .status(Response.Status.BAD_REQUEST)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    LOGGER.trace("addTopicTag : appId={}, topicId={}", appId, topicId);

    String nodeId = TopicHelper.makeTopic(appId, null, topicId);
    TopicEntity entity = DBUtil.getTopicDAO().getTopic(MMXServerConstants.DEFAULT_PUBSUB_SERVICE_ID,
            nodeId);
    if(entity == null ) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Topic not found");
      return Response.status(Response.Status.NOT_FOUND)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }

    if(!Helper.validateTags(tagList.getTags())) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Invalid tag : tag cannot be empty and can have a max length of : " + MMXServerConstants.MAX_TAG_LENGTH);
      return Response.status(Response.Status.BAD_REQUEST)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }

    try {
      TagDAO tagDao = DBUtil.getTagDAO();
      for(String tag : tagList.getTags()) {
        tagDao.createTopicTag(tag, appId,  MMXServerConstants.DEFAULT_PUBSUB_SERVICE_ID, nodeId);
      }
      return Response
              .status(Response.Status.CREATED)
              .type(MediaType.TEXT_PLAIN)
              .entity("Successfully Created Tags")
              .build();
    } catch (Exception e) {
      LOGGER.error("addTopicTag : error adding tags for topic={}", topicId, e);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error creating tags");
      return Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }
  }

  @DELETE
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public Response deleteAllTopicTags(@Context HttpHeaders headers,
                                     @PathParam(MMXServerConstants.TOPICNAME_PATH_PARAM) String topicId) {
    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);

    LOGGER.trace("deleteAllTopicTags : deleting all topic tags for topicId={}", topicId);

    String nodeId = TopicHelper.makeTopic(appId, null, topicId);
    TopicEntity entity = DBUtil.getTopicDAO().getTopic(MMXServerConstants.DEFAULT_PUBSUB_SERVICE_ID,
            nodeId);
    if(entity == null ) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Topic not found");
      return Response.status(Response.Status.NOT_FOUND)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }

    try {
      DBUtil.getTagDAO().deleteAllTagsForTopic(appId, MMXServerConstants.DEFAULT_PUBSUB_SERVICE_ID, nodeId);
      return Response.status(Response.Status.OK).build();
    } catch (Exception e) {
      LOGGER.error("deleteAllTopicTags : error deleting all topic tags for topicId={}", topicId);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error deleting tags");
      return Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }
  }

  @DELETE
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  @Path("/{" + MMXServerConstants.TAGNAME_PATH_PARAM + "}")
  public Response deleteTopicTags(@Context HttpHeaders headers,
                                  @PathParam(MMXServerConstants.TOPICNAME_PATH_PARAM) String topicId,
                                  @PathParam(MMXServerConstants.TAGNAME_PATH_PARAM) String tag) {
    ErrorResponse errorResponse = isAuthenticated(headers, DBUtil.getAppDAO());

    if (errorResponse != null)
      return errorResponse.toJaxRSResponse();

    String appId = headers.getRequestHeaders().getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);
    LOGGER.trace("deleteTopicTags : detelting tag={} for topicId{} with appId={}", tag, topicId, appId);

    String nodeId = TopicHelper.makeTopic(appId, null, topicId);
    TopicEntity entity = DBUtil.getTopicDAO().getTopic(MMXServerConstants.DEFAULT_PUBSUB_SERVICE_ID,
            nodeId);
    if(entity == null ) {
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Topic not found");
      return Response.status(Response.Status.NOT_FOUND)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse)
              .build();
    }

    try {
      DBUtil.getTagDAO().deleteTagsForTopic(Arrays.asList(tag), appId,
              MMXServerConstants.DEFAULT_PUBSUB_SERVICE_ID, nodeId);
      return Response.status(Response.Status.OK).build();
    } catch (Exception e) {
      LOGGER.error("deleteTopicTags : caught exception deleting tags for topicId={}", topicId, e);
      errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, "Error deleting tags for device");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.APPLICATION_JSON)
              .entity(errorResponse).build();
    }
  }
}

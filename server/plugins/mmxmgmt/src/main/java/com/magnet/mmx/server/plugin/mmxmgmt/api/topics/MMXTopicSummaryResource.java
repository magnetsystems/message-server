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
package com.magnet.mmx.server.plugin.mmxmgmt.api.topics;

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicSummary;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXTopicManager;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXTopicSummary;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXTopicSummaryResult;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 */
@Path("/topicssummary")
public class MMXTopicSummaryResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXTopicSummaryResource.class);
  
  @Context
  private HttpServletRequest servletRequest;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSummary(@QueryParam("topicName") List<String> topicNames) throws MMXException {
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
    LOGGER.trace("getSummary :appId={}, topicNames={}", appId, topicNames);

    if(topicNames == null || topicNames.isEmpty()) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).
              entity(new ErrorResponse(ErrorCode.TOPIC_SUMMARY_INVALID_TOPIC_NAME.getCode(), "No topic name specified")).type(MediaType.APPLICATION_JSON).build());
    }

    List<MMXTopicId> topicList = new ArrayList<MMXTopicId>();

    for(String name : topicNames) {
      if(!Strings.isNullOrEmpty(name))
        topicList.add(new MMXTopicId(name));
    }

    if(topicList.isEmpty()) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).
              entity(new ErrorResponse(ErrorCode.TOPIC_SUMMARY_INVALID_TOPIC_NAME.getCode(), "No topic name specified")).type(MediaType.APPLICATION_JSON).build());
    }

    TopicAction.SummaryRequest summaryRequest = new TopicAction.SummaryRequest(topicList);
    TopicAction.SummaryResponse response = MMXTopicManager.getInstance().getSummary(null, appId, summaryRequest);
    List<MMXTopicSummary> summaryList = new ArrayList<MMXTopicSummary>();

    for(TopicSummary s : response) {
      int count = s.getCount();
      Date lastPublishedDate = s.getLastPubTime();
      String topicName = s.getTopicNode().getName();
      MMXTopicSummary mts = new MMXTopicSummary(topicName, count, lastPublishedDate);
      summaryList.add(mts);
    }
    MMXTopicSummaryResult summaryResult = new MMXTopicSummaryResult(appId,summaryList);
    Response summary = Response
            .status(Response.Status.OK)
            .entity(summaryResult)
            .build();

    return summary;
  }

}

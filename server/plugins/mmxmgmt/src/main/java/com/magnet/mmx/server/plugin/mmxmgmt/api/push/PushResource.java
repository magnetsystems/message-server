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
package com.magnet.mmx.server.plugin.mmxmgmt.api.push;

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.AbstractBaseResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.PushMessageQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageQueryBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 */
@Path("push")
public class PushResource extends AbstractBaseResource {
  private final static Logger LOGGER = LoggerFactory.getLogger(PushResource.class);
  private final static String ESCAPED_COMMA = Pattern.quote(",");
  private final static Integer DEFAULT_PAGE_SIZE = Integer.valueOf(100);
  private final static Integer DEFAULT_OFFSET = Integer.valueOf(0);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  public Response getPushMessageForId(@Context HttpHeaders headers, @PathParam("id") String pushId) {
    long startTime = System.nanoTime();
    AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
    AppEntityHolder holder = new AppEntityHolderImpl();

    ErrorResponse authCheck = isAuthenticated(headers, appDAO, holder);
    if (authCheck != null) {
      return Response
          .status(Response.Status.UNAUTHORIZED)
          .entity(authCheck)
          .build();
    }
    AppEntity appEntity = holder.getAppEntity();

    PushMessageDAO pushMessageDAO = new PushMessageDAOImpl(new OpenFireDBConnectionProvider());
    PushMessageEntity message = pushMessageDAO.getPushMessage(pushId);
    if (message != null) {
      if (!message.getAppId().equals(appEntity.getAppId())) {
        message = null;
      }
    }
    Response response = null;
    if (message == null) {
      response = Response
          .status(Response.Status.NOT_FOUND)
          .entity(message)
          .build();
    } else {
      response = Response
          .status(Response.Status.OK)
          .entity(message)
          .build();
    }
    long endTime = System.nanoTime();
    LOGGER.info("Completed processing getPushMessageForId in {} milliseconds",
        TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
    return response;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchPushMessages(@Context HttpHeaders headers, @QueryParam("state") String state,
                                     @QueryParam("deviceId") String deviceId,
                                     @QueryParam("type") String type,
                                     @QueryParam("sentSince") Integer sentSince,
                                     @QueryParam("sentUntil") Integer sentUntil,
                                     @QueryParam("sortby") String sortBy,
                                     @QueryParam("sortorder") String sortOrder,
                                     @QueryParam("size") Integer size,
                                     @QueryParam("offset") Integer offset) {
    try {
      long startTime = System.nanoTime();
      AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
      AppEntityHolder holder = new AppEntityHolderImpl();

      ErrorResponse authCheck = isAuthenticated(headers, appDAO, holder);
      if (authCheck != null) {
        return Response
            .status(Response.Status.UNAUTHORIZED)
            .entity(authCheck)
            .build();
      }
      AppEntity appEntity = holder.getAppEntity();
      PushMessageQuery query = new PushMessageQuery();

      if (deviceId != null && !deviceId.isEmpty()) {
        query.setDeviceIds(parseDeviceIds(deviceId));
      }
      if (state != null) {
        query.setState(state);
      }
      if (type != null) {
        query.setType(type);
      }
      if (sentSince != null || sentUntil != null) {
        query.setDateSent(new DateRange(sentSince, sentUntil));
      }
      SortInfo sortInfo = SortInfo.build(sortBy, sortOrder);

      if (size == null) {
        size = DEFAULT_PAGE_SIZE;
      }
      if (offset == null) {
        offset = DEFAULT_OFFSET;
      }
      PaginationInfo paginationInfo = PaginationInfo.build(size, offset);

      PushMessageQueryBuilder queryBuilder = new PushMessageQueryBuilder();
      QueryBuilderResult builtQuery = queryBuilder.buildPaginationQueryWithOrder(query, appEntity.getAppId(), paginationInfo, sortInfo);

      PushMessageDAO dao = new PushMessageDAOImpl(new OpenFireDBConnectionProvider());

      SearchResult<PushMessageEntity> result = dao.getPushMessagesWithPagination(builtQuery, paginationInfo);

      Response response = Response
          .status(Response.Status.OK)
          .entity(result)
          .build();

      long endTime = System.nanoTime();
      LOGGER.info("Completed processing searchPushMessages in {} milliseconds",
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return response;

    } catch (WebApplicationException e) {
      throw e;

    } catch (Throwable t) {
      LOGGER.warn("Throwable during sendPushMessage", t);
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse(ErrorCode.SEND_PUSH_MESSAGE_ISE, t.getMessage()))
              .build()
      );
    }
  }

  private static  List<String> parseDeviceIds (String list) {
    List<String> rv = null;
    if (list != null) {
      String[] ids = list.split(ESCAPED_COMMA);
      rv = Arrays.asList(ids);
    }
    return rv;
  }

}

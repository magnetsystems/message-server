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
package com.magnet.mmx.server.plugin.mmxmgmt.db;

import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.Operator;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.PushMessageQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.AND;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.CLOSE_BRACKET;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.COUNT_STAR;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.DISTINCT;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.DOT;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.EQUAL;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.FROM;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.GREATER_THAN_EQUAL_TO;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.IN;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.LESS_THAN;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.LIMIT;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.OFFSET;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.OPEN_BRACKET;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.OR;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.QUESTION;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.SELECT;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.SPACE;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.STAR;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.WHERE;

/**
 * QueryBuilder for building PushMessage search queries.
 */
public class PushMessageQueryBuilder {
  private static Logger LOGGER = LoggerFactory.getLogger(PushMessageQueryBuilder.class);
  private final String BASE_TABLE_NAME = "mmxPushMessage";
  private final String COL_DEVICE_ID = "deviceId";
  private final String COL_APP_ID = "appId";
  private final String COL_STATE = "state";
  private final String COL_TYPE = "type";
  private final String COL_DATE_SENT = "dateSentUTC";
  private final String COL_DATE_ACK = "dateAcknowledgedUTC";
  private List<String> tableList = new LinkedList<String>();
  private StringBuilder queryBuilder = new StringBuilder();
  private StringBuilder countQueryBuilder = new StringBuilder();
  private StringBuilder whereClauseBuilder = new StringBuilder(100);

  private List<QueryParam> paramList = new LinkedList<QueryParam>();

  private static EnumMap<PushMessageSearchProperty, String> columnMap = new EnumMap<PushMessageSearchProperty, String>(PushMessageSearchProperty.class);

  {
    columnMap.put(PushMessageSearchProperty.ACK, COL_DATE_ACK);
    columnMap.put(PushMessageSearchProperty.DEVICEID, COL_DEVICE_ID);
    columnMap.put(PushMessageSearchProperty.SENT, COL_DATE_SENT);
    columnMap.put(PushMessageSearchProperty.STATE, COL_STATE);
    columnMap.put(PushMessageSearchProperty.TYPE, COL_TYPE);
  }

  public QueryBuilderResult buildQuery(PushMessageQuery query, String appId) {
    return this.buildPaginationQueryWithOrder(query, appId, null, null);
  }

  public QueryBuilderResult buildPaginationQuery(PushMessageQuery query, String appId, PaginationInfo paginationInfo) {
    return this.buildPaginationQueryWithOrder(query, appId, paginationInfo, null);
  }

  public QueryBuilderResult buildPaginationQueryWithOrder(PushMessageQuery query, String appId, PaginationInfo info, SortInfo sortInfo) throws
      ResolutionException {
    Operator operator = query.getOperator();
    if (operator == null) {
      operator = Operator.OR;
    }
    String sqlOperator;
    if (operator == Operator.OR) {
      sqlOperator = SPACE + OR + SPACE;
    } else {
      sqlOperator = SPACE + AND + SPACE;
    }
    List<String> deviceIds = query.getDeviceIds();
    boolean added = false;
    if (deviceIds != null && !deviceIds.isEmpty()) {
      String fragment = processDeviceIds(deviceIds);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    PushMessageEntity.PushMessageState state = query.getState();
    if (state != null) {
      String fragment = processState(state);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    DateRange dateSent = query.getDateSent();
    if (dateSent != null) {
      String fragment = processDateSent(dateSent);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    DateRange dateAck = query.getDateAcknowledged();
    if (dateAck != null) {
      String fragment = processDateAck(dateAck);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    String tableList = buildTableList();
    String appIdFragment = processAppId(appId);
    queryBuilder.append(SELECT)
        .append(SPACE)
        .append(DISTINCT)
        .append(SPACE)
        .append(BASE_TABLE_NAME).append(DOT)
        .append(STAR)
        .append(SPACE)
        .append(FROM)
        .append(SPACE)
        .append(tableList).append(SPACE)
        .append(WHERE)
        .append(SPACE);
    if (added) {
      queryBuilder.append(OPEN_BRACKET)
          .append(whereClauseBuilder)
          .append(CLOSE_BRACKET)
          .append(SPACE)
          .append(AND)
          .append(SPACE);
    }
    queryBuilder
        .append(OPEN_BRACKET)
        .append(appIdFragment)
        .append(CLOSE_BRACKET);

    if (sortInfo != null) {
      String sort = processSortInfo(sortInfo);
      queryBuilder.append(SPACE).append(sort);
    }

    if (info != null) {
      String pagination = processPaginationFragment(info);
      queryBuilder.append(SPACE).append(pagination);
    }

    countQueryBuilder.append(SELECT)
        .append(SPACE)
        .append(COUNT_STAR)
        .append(SPACE)
        .append(FROM)
        .append(SPACE)
        .append(tableList).append(SPACE)
        .append(WHERE)
        .append(SPACE);

    if (added) {
      countQueryBuilder.append(OPEN_BRACKET)
          .append(whereClauseBuilder)
          .append(CLOSE_BRACKET)
          .append(SPACE)
          .append(AND)
          .append(SPACE);
    }
    countQueryBuilder.append(OPEN_BRACKET)
        .append(appIdFragment)
        .append(CLOSE_BRACKET);

    return new QueryBuilderResult(queryBuilder.toString(), countQueryBuilder.toString(), paramList);
  }

  protected String buildTableList() {
    StringBuilder builder = new StringBuilder();
    builder.append(BASE_TABLE_NAME);
    for (String t : tableList) {
      builder.append(",");
      builder.append(t);
    }
    return builder.toString();
  }

  protected String processAppId(String appId) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(COL_APP_ID).append(EQUAL);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, appId, true));
    return fragmentBuilder.toString();
  }

  private String processDeviceIds(List<String> ids) {
    int size = ids.size();
    StringBuilder fragmentBuilder = new StringBuilder();
    if (size == 1) {
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(COL_DEVICE_ID).append(EQUAL);
      fragmentBuilder.append(QUESTION);
      paramList.add(new QueryParam(Types.VARCHAR, ids.get(0), true));
    } else {
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(COL_DEVICE_ID).append(SPACE).append(IN).append(SPACE)
          .append(OPEN_BRACKET);
      String placeHolder = Helper.getSQLPlaceHolders(size);
      fragmentBuilder.append(placeHolder);
      fragmentBuilder.append(CLOSE_BRACKET);
      for (String id : ids) {
        paramList.add(new QueryParam(Types.VARCHAR, id, true));
      }
    }
    return fragmentBuilder.toString();
  }


  private String processState(PushMessageEntity.PushMessageState state) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(COL_STATE).append(EQUAL);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, state.name(), true));
    return fragmentBuilder.toString();
  }

  protected String processDateSent(DateRange dateRange) {
    return processDateColumn(dateRange, COL_DATE_SENT);
  }

  protected String processDateAck(DateRange dateRange) {
    return processDateColumn(dateRange, COL_DATE_ACK);
  }

  protected String processDateColumn(DateRange dateRange, String column) {
    boolean hasStart = dateRange.getStart() != null;
    boolean hasEnd = dateRange.getEnd() != null;
    StringBuilder fragmentBuilder = new StringBuilder();
    if (!hasStart && !hasEnd) {
      //neither start nor end
      //this is bad
      throw new ResolutionException("Invalid date range specified");
    }
    if (hasStart && hasEnd) {
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(column)
          .append(GREATER_THAN_EQUAL_TO);
      fragmentBuilder.append(QUESTION);
      fragmentBuilder.append(SPACE).append(AND).append(SPACE);
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(column).append(LESS_THAN);
      fragmentBuilder.append(QUESTION);
      paramList.add(new QueryParam(Types.INTEGER, dateRange.getStart().intValue(), true));
      paramList.add(new QueryParam(Types.INTEGER, dateRange.getEnd().intValue(), true));
    } else if (hasStart) {
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(column)
          .append(GREATER_THAN_EQUAL_TO);
      fragmentBuilder.append(QUESTION);
      paramList.add(new QueryParam(Types.INTEGER, dateRange.getStart().intValue(), true));
    } else if (hasEnd) {
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(column).append(LESS_THAN);
      fragmentBuilder.append(QUESTION);
      paramList.add(new QueryParam(Types.INTEGER, dateRange.getEnd().intValue(), true));
    }
    return fragmentBuilder.toString();
  }

  private String processPaginationFragment(PaginationInfo info) {
    StringBuilder fragmentBuilder = new StringBuilder();
    if (info != null) {
      fragmentBuilder.append(SPACE).append(LIMIT).append(SPACE).append(QUESTION)
          .append(SPACE).append(OFFSET).append(SPACE).append(QUESTION);
      paramList.add(new QueryParam(Types.INTEGER, info.getTakeSize()));
      paramList.add(new QueryParam(Types.INTEGER, info.getSkipSize()));
    }
    return fragmentBuilder.toString();
  }

  private String processSortInfo(SortInfo info) {
    StringBuilder fragmentBuilder = new StringBuilder();
    boolean added = false;
    if (info != null) {
      List<SortInfo.SortPair> list = info.getSortPairs();
      for (SortInfo.SortPair pair : list) {
        String key = pair.getKey();
        String order = pair.getOrder();
        PushMessageSearchProperty property = PushMessageSearchProperty.find(key);
        if (property != null) {
          if (!added) {
            fragmentBuilder.append("ORDER BY");
            added = true;
          }
          fragmentBuilder.append(SPACE);
          fragmentBuilder.append(columnMap.get(property));
          SortOrder sorder = SortOrder.from(order);
          if (sorder == SortOrder.DESCENDING) {
            fragmentBuilder.append(SPACE);
            fragmentBuilder.append("DESC");
          }
        }
      }
    }
    return fragmentBuilder.toString();
  }
}

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

import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DeviceQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.Operator;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;

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
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.LESS_THAN;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.LIKE;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.LIMIT;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.OFFSET;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.OPEN_BRACKET;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.OR;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.PERCENTAGE;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.QUESTION;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.SELECT;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.SPACE;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.STAR;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.WHERE;

/**
 * DeviceQuery Builder that helps build SQL queries for resolving the DeviceQuery.
 */
public class DeviceQueryBuilder {

  private List<String> tableList = new LinkedList<String>();
  private StringBuilder queryBuilder = new StringBuilder();
  private final String BASE_TABLE_NAME = "mmxDevice";

  private StringBuilder whereClauseBuilder = new StringBuilder(100);

  private List<QueryParam> paramList = new LinkedList<QueryParam>();
  private boolean activeOnly = true;
  private boolean enableZeroCriterion;

  private final String COL_DEVICE_ID = "deviceId";
  private final String COL_STATUS = "status";
  private final String COL_OS_TYPE = "osType";
  private final String COL_DATE_REGISTERED = "dateCreated";
  private StringBuilder countQueryBuilder = new StringBuilder();

  private static EnumMap<DeviceSortBy, String> columnMap = new EnumMap<DeviceSortBy, String>(DeviceSortBy.class);

  {
    columnMap.put(DeviceSortBy.DEVICE_ID, COL_DEVICE_ID);
    columnMap.put(DeviceSortBy.OS_TYPE, COL_OS_TYPE);
    columnMap.put(DeviceSortBy.STATUS, COL_STATUS);
    columnMap.put(DeviceSortBy.REGISTRATION_DATE, COL_DATE_REGISTERED);
  }

  /**
   * No args constructor. This builds queries that will return active devices only. Using
   * this constructor sets the enableZeroCriterion flag to false.
   */
  public DeviceQueryBuilder() {
    this(true, false);
  }

  /**
   * Constructor.
   *
   * @param activeOnly flag indicating if the query should be built to include active devices only.
   *                   setting this to true builds a query for active devices only.
   * @param allowZeroCriterion
   */
  public DeviceQueryBuilder(boolean activeOnly, boolean allowZeroCriterion) {
    this.activeOnly = activeOnly;
    this.enableZeroCriterion = allowZeroCriterion;
  }

  public QueryBuilderResult buildQuery(DeviceQuery query, String appId) {
    return buildQuery(query, appId, null, null);
  }

  public QueryBuilderResult buildQuery(DeviceQuery query, String appId, PaginationInfo paginationInfo, SortInfo sortInfo) {

    Operator operator = query.getOperator();
    if (operator == null) {
      operator = Operator.OR;
    }
    String sqlOperator = null;
    if (operator == Operator.OR) {
      sqlOperator = SPACE + OR + SPACE;
    } else {
      sqlOperator = SPACE + AND + SPACE;
    }
    boolean added = false;
    if (query.getOsType() != null) {
      String fragment = process(query.getOsType());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }

    }
    if (query.getRegistrationDate() != null) {
      String fragment = processDateCreated(query.getRegistrationDate());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (query.getTags() != null && !query.getTags().isEmpty()) {
      String fragment = processTags(query.getTags());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (query.getModelInfo() != null && !query.getModelInfo().isEmpty()) {
      String fragment = processModelInfo(query.getModelInfo());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (query.getPhoneNumber() != null && !query.getPhoneNumber().isEmpty()) {
      String fragment = processPhoneNumber(query.getPhoneNumber());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (query.getStatus() != null) {
      String fragment = processDeviceStatus(query.getStatus());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (!added && !enableZeroCriterion) {
      throw new ResolutionException("No criterion defined in the device query");
    }
    String tableList = buildTableList();
    String appIdFragement = processAppId(appId);
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
          .append(AND);
    }
    queryBuilder.append(SPACE)
        .append(OPEN_BRACKET)
        .append(appIdFragement)
        .append(CLOSE_BRACKET);
    //add the fragment for including only active devices.
    String activeFragment = null;
    if (activeOnly) {
      activeFragment = processActiveOnly();
      queryBuilder.append(SPACE).append(AND)
          .append(SPACE)
          .append(OPEN_BRACKET)
          .append(activeFragment)
          .append(CLOSE_BRACKET);
    }
    if (sortInfo != null) {
      String sort = processSortInfo(sortInfo);
      queryBuilder.append(SPACE).append(sort);
    }

    if (paginationInfo != null) {
      String pagination = processPaginationFragment(paginationInfo);
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
    //add the fragment for including only active devices.
    if (activeOnly) {
      countQueryBuilder.append(SPACE).append(AND)
          .append(SPACE)
          .append(OPEN_BRACKET)
          .append(activeFragment)
          .append(CLOSE_BRACKET);
    }
    countQueryBuilder.append(OPEN_BRACKET)
        .append(appIdFragement)
        .append(CLOSE_BRACKET);

    return new QueryBuilderResult(queryBuilder.toString(), countQueryBuilder.toString(), paramList);
  }



  protected String process(OSType type) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append("osType").append(EQUAL);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, type.name(), true));
    return fragmentBuilder.toString();
  }

  protected String processModelInfo(String info) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append("modelInfo")
        .append(SPACE).append(LIKE).append(SPACE);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, PERCENTAGE + info + PERCENTAGE, true));
    return fragmentBuilder.toString();
  }

  protected String processPhoneNumber(String phone) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append("phoneNumber").append(EQUAL);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, phone, true));
    return fragmentBuilder.toString();
  }

  protected String processTags(List<String> tags) {
    StringBuilder fragmentBuilder = new StringBuilder();
    String template = "mmxTag.tagname = ?";
    fragmentBuilder.append(OPEN_BRACKET).append(SPACE);
    fragmentBuilder.append(OPEN_BRACKET).append(SPACE);
    String separator = SPACE + OR + SPACE;
    for (String tag : tags) {
      fragmentBuilder.append(template);
      fragmentBuilder.append(separator);
      paramList.add(new QueryParam(Types.VARCHAR, tag, true));
    }
    //strip the trailing or
    fragmentBuilder.setLength(fragmentBuilder.length() - separator.length());
    fragmentBuilder.append(SPACE).append(CLOSE_BRACKET);
    fragmentBuilder.append(SPACE).append(AND).append(SPACE).append("mmxTag.deviceId=mmxDevice.id").append(SPACE).append(CLOSE_BRACKET);
    /*
    add stuff of the join table
     */
    tableList.add("mmxTag");
    return fragmentBuilder.toString();
  }

  protected String processDateCreated(DateRange dateCreated) {
    boolean hasStart = dateCreated.getStart() != null;
    boolean hasEnd = dateCreated.getEnd() != null;
    StringBuilder fragmentBuilder = new StringBuilder();

    if (!DateRange.isValid(dateCreated)) {
      throw new ResolutionException("Invalid date range specified");
    }

    if (hasStart && hasEnd) {
      fragmentBuilder.append("unix_timestamp(").append(BASE_TABLE_NAME).append(DOT).append("dateCreated")
          .append(CLOSE_BRACKET)
          .append(GREATER_THAN_EQUAL_TO);
      fragmentBuilder.append(QUESTION);
      fragmentBuilder.append(" AND ");
      fragmentBuilder.append("unix_timestamp(").append(BASE_TABLE_NAME).append(DOT).append("dateCreated").append(CLOSE_BRACKET).append(LESS_THAN);
      fragmentBuilder.append(QUESTION);
      paramList.add(new QueryParam(Types.INTEGER, dateCreated.getStart().intValue(), true));
      paramList.add(new QueryParam(Types.INTEGER, dateCreated.getEnd().intValue() , true));
    } else if (hasStart) {
      fragmentBuilder.append("unix_timestamp(").append(BASE_TABLE_NAME).append(DOT).append("dateCreated")
          .append(CLOSE_BRACKET)
          .append(GREATER_THAN_EQUAL_TO);
      fragmentBuilder.append(QUESTION);
      paramList.add(new QueryParam(Types.INTEGER, dateCreated.getStart().intValue(), true));
    } else if (hasEnd) {
      fragmentBuilder.append("unix_timestamp(").append(BASE_TABLE_NAME).append(DOT).append("dateCreated").append(CLOSE_BRACKET).append(LESS_THAN);
      fragmentBuilder.append(QUESTION);
      paramList.add(new QueryParam(Types.INTEGER, dateCreated.getEnd().intValue(), true));
    }
    return fragmentBuilder.toString();
  }

  protected String processAppId(String appId) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append("appId").append(EQUAL);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, appId, true));
    return fragmentBuilder.toString();
  }

  protected String processActiveOnly() {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append("status").append(EQUAL);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, DeviceStatus.ACTIVE.name(), true));
    return fragmentBuilder.toString();
  }

  protected String processDeviceStatus(DeviceStatus status) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append("status").append(EQUAL);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, status.name(), true));
    return fragmentBuilder.toString();
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

  private String processPaginationFragment(PaginationInfo info) {
    StringBuilder fragmentBuilder = new StringBuilder();
    if (info != null) {
      fragmentBuilder.append(SPACE).append(LIMIT).append(SPACE).append(QUESTION)
          .append(SPACE).append(OFFSET).append(SPACE).append(QUESTION);
      paramList.add(new QueryParam(Types.INTEGER, info.getTakeSize(), false));
      paramList.add(new QueryParam(Types.INTEGER, info.getSkipSize(), false));
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
        DeviceSortBy property = DeviceSortBy.find(key);
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

  public static enum DeviceSortBy {
    DEVICE_ID,
    OS_TYPE,
    STATUS,
    REGISTRATION_DATE,
    ;

    /**
     * Get DeviceSortBy for the given key. Key is matched in case insensitive manner.
     * @param key
     * @return
     */
    public static DeviceSortBy find(String key) {
      if (key == null) {
        return null;
      }
      DeviceSortBy rv = null;
      DeviceSortBy[] values = DeviceSortBy.values();
      int size = values.length;
      for (int i = 0; i < size && rv == null; i++) {
        DeviceSortBy prop = values[i];
        if (prop.name().equalsIgnoreCase(key)) {
          rv = prop;
        }
      }
      return rv;
    }
  }

}

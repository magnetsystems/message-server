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

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.Operator;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.UserQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import java.sql.Types;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.*;

/**
 * UserQueryBuilder that helps build SQL queries for resolving the UserQuery.
 */
public class UserQueryBuilder {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserQueryBuilder.class);

  private List<String> tableList = new LinkedList<String>();
  private StringBuilder queryBuilder = new StringBuilder();
  private final String BASE_TABLE_NAME = "ofUser";

  private StringBuilder whereClauseBuilder = new StringBuilder(100);
  private StringBuilder countQueryBuilder = new StringBuilder();
  private List<QueryParam> paramList = new LinkedList<QueryParam>();

  private static final String COL_EMAIL = "email";
  private static final String COL_NAME = "name";
  private static final String COL_USERNAME = "username";

  private static final String COUNT_FRAGMENT = "COUNT(DISTINCT ofUser.username)";

  private boolean enableZeroCriterion = false;

  public UserQueryBuilder() {

  }

  public UserQueryBuilder(boolean enableZeroCriterion) {
    this.enableZeroCriterion = enableZeroCriterion;
  }

  private static EnumMap<UserSortBy, String> columnMap = new EnumMap<UserSortBy, String>(UserSortBy.class);

  {
    columnMap.put(UserSortBy.EMAIL, COL_EMAIL);
    columnMap.put(UserSortBy.NAME, COL_NAME);
    columnMap.put(UserSortBy.USERNAME, COL_USERNAME);
  }

  public QueryBuilderResult buildQuery(UserQuery query, String appId) {
    return this.buildQuery(query, appId, null);
  }

  public QueryBuilderResult buildQuery(UserQuery query, String appId, PaginationInfo pinfo) {
    return buildQuery(query, appId, pinfo, null);
  }

  public QueryBuilderResult buildQuery(UserQuery query, String appId, PaginationInfo pinfo, SortInfo sortInfo) {
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
    if (query.getDisplayName() != null && !query.getDisplayName().isEmpty()) {
      String fragment = processDisplayName(query.getDisplayName());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (query.getEmail() != null && !query.getEmail().isEmpty()) {
      String fragment = processEmail(query.getEmail());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (query.getRegistrationDate() != null) {
      String fragment = processRegistrationDate(query.getRegistrationDate());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }

    if (!Strings.isNullOrEmpty(query.getUsername())) {
      String fragment = processUsername(query.getUsername());
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

    String appIdFragment = processAppId(appId);

    String tableList = buildTableList();
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

    if(added) {
      queryBuilder.append(OPEN_BRACKET)
              .append(whereClauseBuilder)
              .append(CLOSE_BRACKET)
              .append(SPACE)
              .append(AND);
    }

    queryBuilder.append(SPACE)
        .append(OPEN_BRACKET)
        .append(appIdFragment)
        .append(CLOSE_BRACKET);

    if (sortInfo != null) {
      String sort = processSortInfo(sortInfo);
      queryBuilder.append(SPACE).append(sort);
    }

    if (pinfo != null) {
      String pagination = processPaginationFragment(pinfo);
      queryBuilder.append(SPACE).append(pagination);
    }

    countQueryBuilder.append(SELECT)
        .append(SPACE)
        .append(COUNT_FRAGMENT)
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

  /**
   * Build search query
   * @param query
   * @param appId
   * @param pinfo
   * @param sortInfo
   * @return
   * Uses like matches by default.
   */
  public QueryBuilderResult buildSearchQuery(UserQuery query, String appId, PaginationInfo pinfo, SortInfo sortInfo) {
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
    if (query.getDisplayName() != null && !query.getDisplayName().isEmpty()) {
      String fragment = processDisplayName(query.getDisplayName(), null);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (query.getEmail() != null && !query.getEmail().isEmpty()) {
      String fragment = processEmail(query.getEmail(), null);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (query.getRegistrationDate() != null) {
      String fragment = processRegistrationDate(query.getRegistrationDate());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }

    if (!Strings.isNullOrEmpty(query.getUsername())) {
      //perform user name related preprocessing
      //replace @ sign
      //double escape @ sign
      //double escape the slash escape character.
      //refer: http://stackoverflow.com/questions/14926386/how-to-search-for-slash-in-mysql-and-why-escaping-not-required-for-wher
      StringBuilder escaped = new StringBuilder();
      for (char c : JID.escapeNode(query.getUsername()).toCharArray()) {
        if (c == '\\') {
          escaped.append(c);
        }
        escaped.append(c);
      }
      String fragment = processUsername(escaped.toString(), null);
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

    String appIdFragment = processAppId(appId);

    String tableList = buildTableList();
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

    if(added) {
      queryBuilder.append(OPEN_BRACKET)
              .append(whereClauseBuilder)
              .append(CLOSE_BRACKET)
              .append(SPACE)
              .append(AND);
    }

    queryBuilder.append(SPACE)
            .append(OPEN_BRACKET)
            .append(appIdFragment)
            .append(CLOSE_BRACKET);

    if (sortInfo != null) {
      String sort = processSortInfo(sortInfo);
      queryBuilder.append(SPACE).append(sort);
    }

    if (pinfo != null) {
      String pagination = processPaginationFragment(pinfo);
      queryBuilder.append(SPACE).append(pagination);
    }

    countQueryBuilder.append(SELECT)
            .append(SPACE)
            .append(COUNT_FRAGMENT)
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

  /**
   * Build using match query
   * @param matchQuery
   * @param appId
   * @param pinfo
   * @return
   */
  //Note: This implementation uses exact matches.
  public QueryBuilderResult buildQuery(com.magnet.mmx.protocol.UserQuery.SearchRequest matchQuery, String appId, PaginationInfo pinfo) {
    SearchAction.Operator operator = matchQuery.getOperator();
    if (operator == null) {
      operator = SearchAction.Operator.OR;
    }
    String sqlOperator = null;
    if (operator == SearchAction.Operator.OR) {
      sqlOperator = SPACE + OR + SPACE;
    } else {
      sqlOperator = SPACE + AND + SPACE;
    }
    boolean added = false;
    if (matchQuery.getTags() != null && !matchQuery.getTags().isEmpty()) {
      String fragment = processTags(matchQuery.getTags());
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (matchQuery.getDisplayName() != null && !matchQuery.getDisplayName().isEmpty()) {
      SearchAction.Match match = matchQuery.getDisplayNameMatch();
      String fragment = processDisplayName(matchQuery.getDisplayName(), match);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (matchQuery.getEmail() != null && !matchQuery.getEmail().isEmpty()) {
      SearchAction.Match match = matchQuery.getEmailMatch();
      String fragment = processEmail(matchQuery.getEmail(), match);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (matchQuery.getPhone() != null) {
      SearchAction.Match phoneMatch = matchQuery.getPhoneMatch();
      String fragment = processPhone(matchQuery.getPhone(), phoneMatch);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append("(").append(fragment).append(")");
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    if (!added) {
      throw new ResolutionException("No criterion defined in the device query");
    }

    String appIdFragment = processAppId(appId);

    String tableList = buildTableList();
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
        .append(SPACE)
        .append(OPEN_BRACKET)
        .append(whereClauseBuilder)
        .append(CLOSE_BRACKET)
        .append(SPACE)
        .append(AND)
        .append(SPACE)
        .append(OPEN_BRACKET)
        .append(appIdFragment)
        .append(CLOSE_BRACKET);

    if (pinfo != null) {
      String pagination = processPaginationFragment(pinfo);
      queryBuilder.append(SPACE).append(pagination);
    }

    countQueryBuilder.append(SELECT)
        .append(SPACE)
        .append(COUNT_FRAGMENT)
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
    //strip the trailing OR
    fragmentBuilder.setLength(fragmentBuilder.length() - separator.length());
    fragmentBuilder.append(SPACE).append(CLOSE_BRACKET);
    fragmentBuilder.append(SPACE).append(AND).append(SPACE).append("mmxTag.username=ofUser.username").append(SPACE).append(CLOSE_BRACKET);
    /*
    add stuff of the join table
     */
    tableList.add("mmxTag");
    return fragmentBuilder.toString();
  }

  protected String processDisplayName(String name) {
    return LikeClauseBuilder.processTextProperty(BASE_TABLE_NAME, "name", name, SearchAction.Match.EXACT, paramList);
  }

  protected String processDisplayName(String name, SearchAction.Match match) {
    return LikeClauseBuilder.processTextProperty(BASE_TABLE_NAME, "name", name, match, paramList);
  }

  protected String processEmail(String email) {
    return LikeClauseBuilder.processTextProperty(BASE_TABLE_NAME, "email", email, SearchAction.Match.EXACT, paramList);
  }

  protected String processEmail(String email, SearchAction.Match match) {
    return LikeClauseBuilder.processTextProperty(BASE_TABLE_NAME, "email", email, match, paramList);
  }

  /**
   * Uses exact match.
   * @param username
   * @return
   */
  protected String processUsername(String username) {
    return LikeClauseBuilder.processTextProperty(BASE_TABLE_NAME, "username", username, SearchAction.Match.EXACT, paramList);
  }

  /**
   * Uses the specified match for the match type (like, exact, prefix, etc.
   * @param username
   * @param match
   * @return
   */
  protected String processUsername(String username, SearchAction.Match match) {
    return LikeClauseBuilder.processTextProperty(BASE_TABLE_NAME, "username", username, match, paramList);
  }

  protected String processPhone(String phone, SearchAction.Match match) {
    StringBuilder fragmentBuilder = new StringBuilder();
    String column="ofUserProp.propValue";
    String paramValue = null;
    if (match == null) {
      fragmentBuilder.append(UPPER).append(OPEN_BRACKET).append(column)
          .append(CLOSE_BRACKET).append(SPACE);
      fragmentBuilder.append(LIKE).append(SPACE).append(QUESTION).append(SPACE);
      paramValue = PERCENTAGE + phone.toUpperCase() + PERCENTAGE;
    } else if (match == SearchAction.Match.EXACT) {
      fragmentBuilder.append(column)
          .append(SPACE);
      fragmentBuilder.append(EQUAL).append(QUESTION).append(SPACE);
      paramValue = phone;
    } else if (match == SearchAction.Match.PREFIX) {
      fragmentBuilder.append(DOT).append(UPPER).append(OPEN_BRACKET).append(column)
          .append(CLOSE_BRACKET).append(SPACE);
      fragmentBuilder.append(LIKE).append(SPACE).append(QUESTION).append(SPACE);
      paramValue = phone.toUpperCase() + PERCENTAGE;
    } else if (match == SearchAction.Match.SUFFIX) {
      fragmentBuilder.append(UPPER).append(OPEN_BRACKET).append(column)
          .append(CLOSE_BRACKET).append(SPACE);
      fragmentBuilder.append(LIKE).append(QUESTION).append(SPACE);
      paramValue = PERCENTAGE + phone.toUpperCase();
    }
    fragmentBuilder.append(SPACE);
    paramList.add(new QueryParam(Types.VARCHAR, paramValue, true));

    fragmentBuilder.append(AND).append(SPACE).append("ofUserProp.name = 'phone'");
    //fragmentBuilder.append(SPACE).append(CLOSE_BRACKET);
    fragmentBuilder.append(SPACE).append(AND).append(SPACE).append("ofUserProp.username=ofUser.username").append(SPACE);
    /*
    add stuff of the join table
     */
    tableList.add("ofUserProp");
    return fragmentBuilder.toString();
  }

  protected String processRegistrationDate(DateRange range) {
    Integer start = range.getStart();
    Integer end  = range.getEnd();
    if (start == null && end == null) {
      //this is bad
      throw new ResolutionException("Invalid date range specified");
    }
    boolean hasStart = start != null;
    boolean hasEnd = end != null;
    StringBuilder fragmentBuilder = new StringBuilder();
    final String dateColumn = "creationDate";

    if (hasStart && hasEnd) {
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(dateColumn)
          .append(GREATER_THAN_EQUAL_TO);
      fragmentBuilder.append(QUESTION);
      fragmentBuilder.append(SPACE).append(AND).append(SPACE);
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(dateColumn).append(LESS_THAN);
      fragmentBuilder.append(QUESTION);

      String startTimestamp = StringUtils.dateToMillis(new Date(start.intValue() * 1000L));
      String endTimestamp = StringUtils.dateToMillis(new Date(end.intValue() * 1000L));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Start time stamp:{} End time stamp:{}" , startTimestamp, endTimestamp);
      }
      paramList.add(new QueryParam(Types.VARCHAR, startTimestamp, true));
      paramList.add(new QueryParam(Types.VARCHAR, endTimestamp, true));
    } else if (hasStart) {
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(dateColumn)
          .append(GREATER_THAN_EQUAL_TO);
      fragmentBuilder.append(QUESTION);
      String startTimestamp = StringUtils.dateToMillis(new Date(start.intValue() * 1000L));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Start time stamp:{} " , startTimestamp);
      }
      paramList.add(new QueryParam(Types.VARCHAR, startTimestamp, true));
    } else if (hasEnd) {
      String endTimestamp = StringUtils.dateToMillis(new Date(end.intValue() * 1000L));
      fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(dateColumn).append(LESS_THAN);
      fragmentBuilder.append(QUESTION);
      paramList.add(new QueryParam(Types.VARCHAR, endTimestamp, true));
    }
    return fragmentBuilder.toString();
  }

  protected String processAppId(String appId) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append("username").append(SPACE).append(LIKE);
    fragmentBuilder.append(SPACE);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, PERCENTAGE + appId, true));
    return fragmentBuilder.toString();
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

  public static class LikeClauseBuilder {
    /**
     * Process text property
     * @param tableName
     * @param column
     * @param value
     * @param match
     * @param paramList
     * @return
     */
    public static String processTextProperty(String tableName, String column, String value,
                                                SearchAction.Match match, List<QueryParam> paramList) {
      StringBuilder fragmentBuilder = new StringBuilder();
      String paramValue = null;
      if (match == null) {
        fragmentBuilder.append(UPPER).append(OPEN_BRACKET).append(tableName).append(DOT).append(column)
            .append(CLOSE_BRACKET).append(SPACE);
        fragmentBuilder.append(LIKE).append(QUESTION).append(SPACE);
        paramValue = PERCENTAGE + value.toUpperCase() + PERCENTAGE;
      } else if (match == SearchAction.Match.EXACT) {
        fragmentBuilder.append(tableName).append(DOT).append(column);
        fragmentBuilder.append(EQUAL).append(QUESTION).append(SPACE);
        paramValue = value;
      } else if (match == SearchAction.Match.PREFIX) {
        fragmentBuilder.append(UPPER).append(OPEN_BRACKET).append(tableName).append(DOT).append(column)
            .append(CLOSE_BRACKET).append(SPACE);
        fragmentBuilder.append(LIKE).append(SPACE).append(QUESTION).append(SPACE);
        paramValue = value.toUpperCase() + PERCENTAGE;
      } else if (match == SearchAction.Match.SUFFIX) {
        fragmentBuilder.append(UPPER).append(OPEN_BRACKET).append(tableName).append(DOT).append(column)
            .append(CLOSE_BRACKET).append(SPACE);
        fragmentBuilder.append(LIKE).append(SPACE).append(QUESTION).append(SPACE);
        paramValue = PERCENTAGE + value.toUpperCase();
      }
      //fragmentBuilder.append(SPACE);
      paramList.add(new QueryParam(Types.VARCHAR, paramValue, true));
      return fragmentBuilder.toString();
    }
  }

  public static enum UserSortBy {
    NAME,
    EMAIL,
    USERNAME,
    ;

    /**
     * Get DeviceSortBy for the given key. Key is matched in case insensitive manner.
     * @param key
     * @return
     */
    public static UserSortBy find(String key) {
      if (key == null) {
        return null;
      }
      UserSortBy rv = null;
      UserSortBy[] values = UserSortBy.values();
      int size = values.length;
      for (int i = 0; i < size && rv == null; i++) {
        UserSortBy prop = values[i];
        if (prop.name().equalsIgnoreCase(key)) {
          rv = prop;
        }
      }
      return rv;
    }
  }

  private String processSortInfo(SortInfo info) {
    StringBuilder fragmentBuilder = new StringBuilder();
    boolean added = false;
    if (info != null) {
      List<SortInfo.SortPair> list = info.getSortPairs();
      for (SortInfo.SortPair pair : list) {
        String key = pair.getKey();
        String order = pair.getOrder();
        UserSortBy property = UserSortBy.find(key);
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

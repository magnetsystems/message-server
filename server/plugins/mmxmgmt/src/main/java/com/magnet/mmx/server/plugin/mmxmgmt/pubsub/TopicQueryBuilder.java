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
package com.magnet.mmx.server.plugin.mmxmgmt.pubsub;

import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.Operator;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.TopicQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryParam;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserQueryBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.util.TopicHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.AND;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.CLOSE_BRACKET;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.DISTINCT;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.DOT;
import static com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderConstants.FROM;
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
 * QueryBuilder for building Pubsub/Topic search queries.
 */
public class TopicQueryBuilder {
  private static Logger LOGGER = LoggerFactory.getLogger(TopicQueryBuilder.class);
  private final String BASE_TABLE_NAME = "ofPubsubNode";
  private final String COL_DISPLAY_NAME = "name";
  private final String COL_DESCRIPTION = "description";
  private final String COL_NODE_ID = "nodeID";
  private List<String> tableList = new LinkedList<String>();
  private StringBuilder queryBuilder = new StringBuilder();
  private StringBuilder countQueryBuilder = new StringBuilder();
  private StringBuilder whereClauseBuilder = new StringBuilder(100);
  private static final String SUBSCRIPTION_COUNT_FRAGMENT = ", (SELECT count(1) FROM ofPubsubSubscription s where s.serviceID = ofPubsubNode.serviceId AND s.nodeID = ofPubsubNode.nodeID GROUP by s.nodeID,s.serviceId ) as 'subcount'";
  private static final String COUNT_FRAGMENT = "COUNT(DISTINCT ofPubsubNode.nodeID, ofPubsubNode.serviceId)";

  private List<QueryParam> paramList = new LinkedList<QueryParam>();

  private static EnumMap<TopicSearchProperty, String> columnMap = new EnumMap<TopicSearchProperty, String>(TopicSearchProperty.class);
  {
    columnMap.put(TopicSearchProperty.DESCRIPTION, COL_DESCRIPTION);
    columnMap.put(TopicSearchProperty.DISPLAYNAME, COL_DISPLAY_NAME);
  }

//  public QueryBuilderResult buildQuery(TopicQuery query, String appId) {
//    return this.buildPaginationQueryWithOrder(query, appId, null, null);
//  }

  public QueryBuilderResult buildPaginationQuery(TopicQuery query, String appId, PaginationInfo paginationInfo) {
    return this.buildPaginationQueryWithOrder(query, appId, paginationInfo, null);
  }

  public QueryBuilderResult buildPaginationQuery(TopicAction.TopicSearchRequest searchRequest, String appId,
                                                 PaginationInfo paginationInfo, String userName) throws ResolutionException {

    com.magnet.mmx.protocol.SearchAction.Operator operator = searchRequest.getOperator();
    if (operator == null) {
      operator = SearchAction.Operator.OR;
    }
    String sqlOperator = null;
    if (operator == SearchAction.Operator.OR) {
      sqlOperator = SPACE + OR + SPACE;
    } else {
      sqlOperator = SPACE + AND + SPACE;
    }
    String topicName = searchRequest.getTopicName();
    boolean added = false;
    if (topicName != null && !topicName.isEmpty()) {
      SearchAction.Match match = searchRequest.getTopicNameMatch();
      String fragment = processTopicName(topicName, match);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }

    String description = searchRequest.getDescription();
    if (description != null && !description.isEmpty()) {
      SearchAction.Match match = searchRequest.getDescriptionMatch();
      String fragment = processDescription(description, match);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    List<String> tags = searchRequest.getTags();
    if (tags != null && !tags.isEmpty()) {
      String fragment = processTags(tags);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    String tableList = buildTableList();
    String appIdFragment = processAppId(appId, userName);

    queryBuilder.append(SELECT)
        .append(SPACE)
        .append(DISTINCT)
        .append(SPACE)
        .append(BASE_TABLE_NAME).append(DOT)
        .append(STAR)
        .append(SPACE)
        .append(SUBSCRIPTION_COUNT_FRAGMENT)
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

//    if (sortInfo != null) {
//      String sort = processSortInfo(sortInfo);
//      queryBuilder.append(SPACE).append(sort);
//    }

    if (paginationInfo != null) {
      String pagination = processPaginationFragment(paginationInfo);
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

  public QueryBuilderResult buildPaginationQueryWithOrder(TopicQuery query, String appId, PaginationInfo info, SortInfo sortInfo) throws
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
    String topicName = query.getTopicName();
    boolean added = false;
    if (topicName != null && !topicName.isEmpty()) {
      String fragment = processDisplayName(topicName);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    String description = query.getDescription();
    if (description != null && !description.isEmpty()) {
      String fragment = processDescription(description);
      if (added) {
        whereClauseBuilder.append(sqlOperator);
        whereClauseBuilder.append(OPEN_BRACKET).append(fragment).append(CLOSE_BRACKET);
      } else {
        added = true;
        whereClauseBuilder.append(fragment);
      }
    }
    List<String> tags = query.getTags();
    if (tags != null && !tags.isEmpty()) {
      String fragment = processTags(tags);
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
        .append(SUBSCRIPTION_COUNT_FRAGMENT)
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

  protected String processAppId(String appId, String userId) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(COL_NODE_ID).append(SPACE).append(LIKE).append(SPACE);
    fragmentBuilder.append(QUESTION).append(SPACE).append(OR).append(SPACE).append(BASE_TABLE_NAME)
        .append(DOT).append(COL_NODE_ID).append(SPACE).append(LIKE).append(SPACE);
    fragmentBuilder.append(QUESTION);
    String globalPrefix = TopicHelper.makeTopic(appId, null, "/") + PERCENTAGE;
    if (userId == null) {
      userId = PERCENTAGE;
    }
    String personalPrefix = TopicHelper.makeTopic(appId, userId, "/") + PERCENTAGE;
    paramList.add(new QueryParam(Types.VARCHAR, globalPrefix, true));
    paramList.add(new QueryParam(Types.VARCHAR, personalPrefix, true));
    return fragmentBuilder.toString();
  }

  protected String processAppId(String appId) {
    return processAppId(appId, null);
  }

  protected String processDisplayName(String displayName) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append("UPPER").append(OPEN_BRACKET);
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(COL_DISPLAY_NAME).append(CLOSE_BRACKET).append(SPACE).append(LIKE).append(SPACE);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, PERCENTAGE + displayName.toUpperCase() + PERCENTAGE, true));
    return fragmentBuilder.toString();
  }

  protected String processTopicName(String topicName, SearchAction.Match match) {
    //if mach is null use a prefix match
    return UserQueryBuilder.LikeClauseBuilder.processTextProperty(BASE_TABLE_NAME, COL_DISPLAY_NAME, topicName,
        match == null ? SearchAction.Match.PREFIX : match, paramList);
  }

  protected String processDescription(String description, SearchAction.Match match) {
    //if match is null use a PREFIX match
    return UserQueryBuilder.LikeClauseBuilder.processTextProperty(BASE_TABLE_NAME, COL_DESCRIPTION, description,
        match == null ? SearchAction.Match.PREFIX : match, paramList);
  }

  protected String processDescription(String description) {
    StringBuilder fragmentBuilder = new StringBuilder();
    fragmentBuilder.append("UPPER").append(OPEN_BRACKET);
    fragmentBuilder.append(BASE_TABLE_NAME).append(DOT).append(COL_DESCRIPTION).append(CLOSE_BRACKET).append(SPACE)
        .append(LIKE).append(SPACE);
    fragmentBuilder.append(QUESTION);
    paramList.add(new QueryParam(Types.VARCHAR, PERCENTAGE + description.toUpperCase() + PERCENTAGE, true));
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
        TopicSearchProperty property = TopicSearchProperty.find(key);
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
    fragmentBuilder.append(SPACE).append(AND).append(SPACE).append("mmxTag.serviceID=ofPubsubNode.serviceID AND mmxTag.nodeID=ofPubsubNode.nodeID").append(SPACE).append(CLOSE_BRACKET);
    /*
    add stuff of the join table
     */
    tableList.add("mmxTag");
    return fragmentBuilder.toString();
  }
}

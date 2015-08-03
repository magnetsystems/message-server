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

import com.magnet.mmx.protocol.MMXAttribute;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicAction.TopicAttr;
import com.magnet.mmx.protocol.TopicAction.TopicQueryResponse;
import com.magnet.mmx.protocol.TopicInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.db.CloseUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DbInteractionException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryParam;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.ConfigureForm;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.util.TopicHelper;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.pubsub.LeafNode;
import org.jivesoftware.openfire.pubsub.PubSubPersistenceManager;
import org.jivesoftware.openfire.pubsub.PublishedItem;
import org.jivesoftware.openfire.pubsub.models.PublisherModel;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Extra functionality to the PubSubPersistenceManager.
 *
 */
public class PubSubPersistenceManagerExt {
  private static Logger LOGGER = LoggerFactory.getLogger(PubSubPersistenceManagerExt.class);
  public static final int MAX_ROWS_RETURN = JiveGlobals.getIntProperty(
      "mmx.topic.query.max", 5000);
  private static final int MAX_ROWS_FETCH = JiveGlobals.getIntProperty(
      "xmpp.pubsub.fetch.max", 2000);
  private static final String LOAD_ITEMS_PREDICATE = 
      "WHERE serviceID=? AND nodeID=? AND creationDate >= ? ORDER BY creationDate DESC";
  private static final String LOAD_ITEMS_BTWN_PREDICATE =
      "WHERE serviceID=? AND nodeID=? AND creationDate BETWEEN ? AND ? ORDER BY creationDate DESC";
  private static final String LOAD_ITEMS_COUNT = "SELECT count(*) FROM ofPubsubItem "
      + LOAD_ITEMS_PREDICATE;
  private static final String LOAD_ITEMS = "SELECT id,jid,creationDate,payload FROM ofPubsubItem "
      + LOAD_ITEMS_PREDICATE;
  private static final String LOAD_ITEMS_BTWN_COUNT = "SELECT count(*) FROM ofPubsubItem "
      + LOAD_ITEMS_BTWN_PREDICATE;
  private static final String LOAD_ITEMS_BTWN = "SELECT id,jid,creationDate,payload FROM ofPubsubItem "
      + LOAD_ITEMS_BTWN_PREDICATE;
  private static final String GET_ITEM_COUNT = "SELECT count(*) from ofPubsubItem WHERE nodeID=? AND serviceID=?";
  private static final String SEARCH_PROJECTION = 
      "nodeID,leaf,name,description,persistItems,maxItems,maxPayloadSize,publisherModel,creationDate,modificationDate,creator,subscriptionEnabled";
  private static final String SEARCH_BY_NAME = 
      "SELECT "+SEARCH_PROJECTION+" FROM ofPubsubNode "+
      "WHERE name LIKE ? AND serviceID=? AND (nodeID LIKE ? OR nodeID LIKE ?) ORDER BY serviceID,nodeID";
  private static final String SEARCH_BY_DESC =
      "SELECT "+SEARCH_PROJECTION+" FROM ofPubsubNode "+
      "WHERE description LIKE ? AND serviceID=? AND (nodeID LIKE ? OR nodeID LIKE ?) ORDER BY serviceID,nodeID";
  
  private static final int INDEX_SRCH_NODEID = 1;
  private static final int INDEX_SRCH_ISLEAF = 2;
  private static final int INDEX_SRCH_NAME = 3;
  private static final int INDEX_SRCH_DESC = 4;
  private static final int INDEX_SRCH_PERSISTED = 5;
  private static final int INDEX_SRCH_MAXITEMS = 6;
  private static final int INDEX_SRCH_MAXPAYLOADSIZE = 7;
  private static final int INDEX_SRCH_PUBMODEL = 8;
  private static final int INDEX_SRCH_CREATEDATE = 9;
  private static final int INDEX_SRCH_MODDATE = 10;
  private static final int INDEX_SRCH_CREATOR = 11;
  private static final int INDEX_SRCH_SUB_ENABLED = 12;

  public static List<PublishedItem> getPublishedItems(LeafNode node,
                                                    int maxRows, Date since) {

//    LOGGER.trace("getPublishedItems : nodeId={}, maxRows={}, since={}", node, maxRows, since);

    PubSubPersistenceManager.flushPendingItems();

    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    int max = MAX_ROWS_FETCH;
    int maxPublished = node.getMaxPublishedItems();

    // Limit the max rows until a solution is in place with Result Set
    // Management
    if (maxRows != -1) {
      max = (maxPublished == -1) ?
          Math.min(maxRows, MAX_ROWS_FETCH) : Math.min(maxRows, maxPublished);
    } else if (maxPublished != -1) {
      max = Math.min(MAX_ROWS_FETCH, maxPublished);
    }

    // We don't know how many items are in the db, so we will start with an
    // allocation of 500
    LinkedList<PublishedItem> results = new LinkedList<PublishedItem>();
    boolean descending = JiveGlobals.getBooleanProperty(
        "xmpp.pubsub.order.descending", false);
    try {
      con = DbConnectionManager.getConnection();
      // Get published items of the specified node
      pstmt = con.prepareStatement(LOAD_ITEMS);
      pstmt.setMaxRows(max);
      pstmt.setString(1, node.getService().getServiceID());
      pstmt.setString(2, encodeNodeID(node.getNodeID()));
      String dateString = StringUtils.dateToMillis(since);
      LOGGER.trace("getPublishedItems : dateString={}", dateString);
      pstmt.setString(3, dateString);
      LOGGER.trace("getPublishedItems : executing statement={}", pstmt);
      rs = pstmt.executeQuery();
      int counter = 0;
      // Rebuild loaded published items
      while (rs.next() && (counter < max)) {
        String itemID = rs.getString(1);
        JID publisher = new JID(rs.getString(2));

        Date creationDate = new Date(Long.parseLong(rs.getString(3).trim()));
        // Create the item
        PublishedItem item = new PublishedItem(node, publisher, itemID,
            creationDate);
        // Add the extra fields to the published item
        if (rs.getString(4) != null) {
          item.setPayloadXML(rs.getString(4));
        }
        // Add the published item to the node
        if (descending)
          results.add(item);
        else
          results.addFirst(item);
        counter++;
      }
    } catch (Exception sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
    } finally {
      DbConnectionManager.closeConnection(rs, pstmt, con);
    }

    if (results.size() == 0)
      return Collections.emptyList();

    LOGGER.trace("getPublishedItems : returning list : {}", results);
    return results;
  }

  public static int getPublishedItemCount(LeafNode node, Date since) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    int count = 0;
    try {
      con = DbConnectionManager.getConnection();
      pstmt = con.prepareStatement(LOAD_ITEMS_COUNT);
      pstmt.setString(1, node.getService().getServiceID());
      pstmt.setString(2, encodeNodeID(node.getNodeID()));
      pstmt.setString(3, StringUtils.dateToMillis(since));
      rs = pstmt.executeQuery();
      while (rs.next()) {
        count = rs.getInt(1);
      }
    } catch (Exception sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
    } finally {
      DbConnectionManager.closeConnection(rs, pstmt, con);
    }
    return count;
  }

  public static List<PublishedItem> getPublishedItems(LeafNode node,
      int maxRows, Date since, Date until, boolean asc) {

//    LOGGER.trace(
//        "getPublishedItems : nodeId={}, maxRows={}, since={}, until={}", node,
//        maxRows, since, until);

    PubSubPersistenceManager.flushPendingItems();

    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    int max = MAX_ROWS_FETCH;
    int maxPublished = node.getMaxPublishedItems();

    // Limit the max rows until a solution is in place with Result Set
    // Management
    if (maxRows != -1) {
      max = (maxPublished == -1) ? Math.min(maxRows, MAX_ROWS_FETCH) : Math
          .min(maxRows, maxPublished);
    } else if (maxPublished != -1) {
      max = Math.min(MAX_ROWS_FETCH, maxPublished);
    }

    // We don't know how many items are in the db, so we will start with an
    // allocation of 2000
    LinkedList<PublishedItem> results = new LinkedList<PublishedItem>();
    boolean descending = !asc;
    try {
      con = DbConnectionManager.getConnection();
      // Get published items of the specified node
      pstmt = con.prepareStatement(LOAD_ITEMS_BTWN);
      pstmt.setMaxRows(max);
      pstmt.setString(1, node.getService().getServiceID());
      pstmt.setString(2, encodeNodeID(node.getNodeID()));
      String sinceString = StringUtils.dateToMillis(since);
      pstmt.setString(3, sinceString);
      String untilString = StringUtils.dateToMillis(until);
      pstmt.setString(4, untilString);
      rs = pstmt.executeQuery();
      int counter = 0;
      // Rebuild loaded published items
      while (rs.next() && (counter < max)) {
        String itemID = rs.getString(1);
        JID publisher = new JID(rs.getString(2));

        Date creationDate = new Date(Long.parseLong(rs.getString(3).trim()));
        // Create the item
        PublishedItem item = new PublishedItem(node, publisher, itemID,
            creationDate);
        // Add the extra fields to the published item
        if (rs.getString(4) != null) {
          item.setPayloadXML(rs.getString(4));
        }
        // Add the published item to the node
        if (descending)
          results.add(item);
        else
          results.addFirst(item);
        counter++;
      }
    } catch (Exception sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
    } finally {
      DbConnectionManager.closeConnection(rs, pstmt, con);
    }

    if (results.size() == 0)
      return Collections.emptyList();

    LOGGER.trace("getPublishedItems : returning list : {}", results);
    return results;
  }
  
  public static int getPublishedItemCount(LeafNode node, Date since, Date until) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    int count = 0;
    try {
      con = DbConnectionManager.getConnection();
      pstmt = con.prepareStatement(LOAD_ITEMS_BTWN_COUNT);
      pstmt.setString(1, node.getService().getServiceID());
      pstmt.setString(2, encodeNodeID(node.getNodeID()));
      pstmt.setString(3, StringUtils.dateToMillis(since));
      pstmt.setString(4, StringUtils.dateToMillis(until));
      rs = pstmt.executeQuery();
      while (rs.next()) {
        count = rs.getInt(1);
      }
    } catch (Exception sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
    } finally {
      DbConnectionManager.closeConnection(rs, pstmt, con);
    }
    return count;
  }
  
  public static PublishedItemsResult getPublishedItemsResult(LeafNode node,
                                                      int maxRows, Date since) {
//    LOGGER.trace("getPublishedItems : nodeId={}, maxRows={}, since={}", node, maxRows, since);
    List<PublishedItem> items = getPublishedItems(node, maxRows, since);
    int count = getPublishedItemCount(node.getNodeID(), 
                    XMPPServer.getInstance().getPubSubModule().getServiceID());
    return new PublishedItemsResult(count, items);
  }

  public static class PublishedItemsResult {
    int totalCount = 0;
    List<PublishedItem> itemList;

    public PublishedItemsResult(int totalCount, List<PublishedItem> itemList) {
      this.totalCount = totalCount;
      this.itemList = itemList;
    }

    public int getTotalCount() {
      return totalCount;
    }

    public List<PublishedItem> getItemList() {
      return itemList;
    }
  }

  public static int getPublishedItemCount(String nodeID, String serviceID) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    int count = 0;
    try {
      con = DbConnectionManager.getConnection();
      pstmt = con.prepareStatement(GET_ITEM_COUNT);
      pstmt.setString(1, nodeID);
      pstmt.setString(2, serviceID);
      LOGGER.trace("getPublishedItemCount : executing statement={}", pstmt);
      rs = pstmt.executeQuery();
      if(rs.next())
        count = rs.getInt(1);
    } catch (SQLException e) {
      LOGGER.error("getPublishedItemCount : caught Exception", e);
    } finally {
      DbConnectionManager.closeConnection(rs, pstmt, con);
    }
    return count;
  }

  private static String encodeNodeID(String nodeID) {
    if (DbConnectionManager.getDatabaseType() == DbConnectionManager.DatabaseType.oracle
        && "".equals(nodeID)) {
      // Oracle stores empty strings as null so return a string with a space
      return " ";
    }
    return nodeID;
  }
  
  // Generalized search for topics by various attributes.
//  public static TopicQueryResponse searchTopic(String userId, String appId,
//                                    TopicSearchRequest search) {
//    Connection con = null;
//    PreparedStatement pstmt = null;
//    ResultSet rs = null;
//
//    String globalPrefix = TopicHelper.makeTopic(appId, null, "/") + "%";
//    String personalPrefix = TopicHelper.makeTopic(appId, userId, "/") + "%";
//
//    // Limit the max rows until a solution is in place with Result Set Management
//    int maxRows = search.getLimit();
//    if (maxRows == -1) {
//      maxRows = MAX_ROWS_FETCH;
//    } else {
//      maxRows = Math.min(maxRows, MAX_ROWS_FETCH);
//    }
//    int total = -1;
//    List<TopicInfo> results = new ArrayList<TopicInfo>();
//    // TODO:
//    return new TopicQueryResponse(total, results);
//  }
  
  // Search topic by name or description.
  public static TopicQueryResponse searchTopic(String userId, String appId, 
                                    int offset, int maxRows, 
                                    List<MMXAttribute<TopicAttr>> criteria) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String query = (criteria.get(0).getType() == TopicAttr.topicName) ?
        SEARCH_BY_NAME : SEARCH_BY_DESC;
    String value = "%" + criteria.get(0).getValue() + "%";
    String globalPrefix = TopicHelper.makeTopic(appId, null, "/") + "%";
    String personalPrefix = TopicHelper.makeTopic(appId, userId, "/") + "%";
    
    // Limit the max rows until a solution is in place with Result Set Management
    if (maxRows == -1) {
      maxRows = MAX_ROWS_FETCH;
    } else {
      maxRows = Math.min(maxRows, MAX_ROWS_FETCH);
    }
    // TODO: the pagination offset is not implemented, total is not available.
    int total = -1;
    List<TopicInfo> results = new ArrayList<TopicInfo>();
    try {
      con = DbConnectionManager.getConnection();
      pstmt = con.prepareStatement(query);
      pstmt.setString(1, value);
      pstmt.setString(2, XMPPServer.getInstance().getPubSubModule().getServiceID());
      pstmt.setString(3, globalPrefix);
      pstmt.setString(4, personalPrefix);
      pstmt.setMaxRows(maxRows);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        String nodeId = rs.getString(INDEX_SRCH_NODEID);
        MMXTopicId topic = TopicHelper.parseNode(nodeId);
        TopicInfo result = new TopicInfo(topic.getUserId(), topic.getName(),
            !rs.getBoolean(INDEX_SRCH_ISLEAF))
          .setDescription(rs.getString(INDEX_SRCH_DESC))
          .setCreationDate(new Date(Long.valueOf(rs.getString(INDEX_SRCH_CREATEDATE))))
          .setModifiedDate(new Date(Long.valueOf(rs.getString(INDEX_SRCH_MODDATE))))
          .setMaxItems(rs.getInt(INDEX_SRCH_MAXITEMS))
          .setMaxPayloadSize(rs.getInt(INDEX_SRCH_MAXPAYLOADSIZE))
          .setPublisherType(ConfigureForm.convert(
            PublisherModel.valueOf(rs.getString(INDEX_SRCH_PUBMODEL))))
          .setPersistent(rs.getBoolean(INDEX_SRCH_PERSISTED))
          .setCreator(rs.getString(INDEX_SRCH_CREATOR))
          .setSubscriptionEnabled(rs.getBoolean(INDEX_SRCH_SUB_ENABLED));
        results.add(result);
      }
    } catch (SQLException e) {
      LOGGER.error("searchTopic: caught Exception", e);
    } finally {
      DbConnectionManager.closeConnection(rs, pstmt, con);
    }
    return new TopicQueryResponse(total, results);
  }

  public static SearchResult<TopicAction.TopicInfoWithSubscriptionCount> getTopicWithPagination(ConnectionProvider provider, QueryBuilderResult query, PaginationInfo info) {
    Connection con = null;
    int totalCount = 0;
    List<TopicAction.TopicInfoWithSubscriptionCount> returnList = new ArrayList<TopicAction.TopicInfoWithSubscriptionCount>(info.getTakeSize());
    try {
      con = provider.getConnection();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Executing built query:{} \n count query:{}", query.getQuery(), query.getCountQuery());
      }
      {
        //do the count first
        PreparedStatement countPS = con.prepareStatement(query.getCountQuery());
        //set the values
        int index = 1;
        for (QueryParam param : query.getParamList()) {
          if (param.isForCount()) {
            QueryParam.setParameterValue(param, index++, countPS);
          }
        }
        ResultSet countRS = countPS.executeQuery();
        if (countRS.next()) {
          totalCount = countRS.getInt(1);
        }
        CloseUtil.close(LOGGER, countRS);
        CloseUtil.close(LOGGER, countPS);
      }
      {
        //do the actual query
        PreparedStatement resultPS = con.prepareStatement(query.getQuery());
        int rindex = 1;
        for (QueryParam param : query.getParamList()) {
          QueryParam.setParameterValue(param, rindex++, resultPS);
        }
        ResultSet resultSet = resultPS.executeQuery();

        while (resultSet.next()) {
          String nodeId = resultSet.getString("nodeId");
          boolean isLeaf = resultSet.getInt("leaf") == 1;
          String name = resultSet.getString("name");
          String description = resultSet.getString("description");
          int subscriptionCount = resultSet.getInt("subcount");
          if (resultSet.wasNull()) {
            subscriptionCount = 0;
          }
          MMXTopicId topic = TopicHelper.parseNode(nodeId);
          TopicAction.TopicInfoWithSubscriptionCount result = new TopicAction.TopicInfoWithSubscriptionCount(topic.getUserId(),
              name, !isLeaf);
          result.setSubscriptionCount(subscriptionCount);
          result.setDescription(description);
          result.setCreationDate(new Date(Long.valueOf(resultSet.getString("creationDate"))));
          result.setModifiedDate(new Date(Long.valueOf(resultSet.getString("modificationDate"))));
          int maxItems = resultSet.getInt("maxItems");
          if (!resultSet.wasNull()){
            result.setMaxItems(maxItems);
          }
          int maxPayloadSize = resultSet.getInt("maxPayloadSize");
          if (!resultSet.wasNull()) {
            result.setMaxPayloadSize(maxPayloadSize);
          }
          result.setPublisherType(ConfigureForm.convert(
              PublisherModel.valueOf(resultSet.getString("publisherModel"))));
          result.setPersistent(resultSet.getBoolean("persistItems"));
          result.setCreator(resultSet.getString("creator"));
          int subscriptionEnabled  = resultSet.getInt("subscriptionEnabled");

          if (resultSet.wasNull()) {
            result.setSubscriptionEnabled(false);
          } else if (subscriptionEnabled == 1) {
            result.setSubscriptionEnabled(true);
          }

          returnList.add(result);
        }
        resultSet.close();
        resultPS.close();
        CloseUtil.close(LOGGER, resultSet);
        CloseUtil.close(LOGGER, resultPS);
      }
      con.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, con);
    }
    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> results = new SearchResult<TopicAction.TopicInfoWithSubscriptionCount>();
    results.setResults(returnList);
    results.setTotal(totalCount);
    results.setSize(info.getTakeSize());
    results.setOffset(info.getSkipSize());
    return results;
  }
}

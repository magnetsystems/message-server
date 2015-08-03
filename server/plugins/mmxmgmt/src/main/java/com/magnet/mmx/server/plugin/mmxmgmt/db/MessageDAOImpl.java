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

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MMXInAppMessageStats;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MMXInAppTuple;
import com.magnet.mmx.server.plugin.mmxmgmt.util.SqlUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.web.MessageSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.MessageSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Implementation of the MessageDAO
 */
public class MessageDAOImpl implements MessageDAO {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageDAOImpl.class);

  private static final String ME_INSERT_QUERY = "INSERT INTO mmxMessage (messageId, fromJID, toJID, dateQueuedUTC, " +
      "state, appId, deviceId, sourceMessageId, messageType) VALUES (?,?,?,?,?,?,?,?,?)";

  private static final String ME_UPDATE_QUERY = "UPDATE mmxMessage SET state = ? WHERE messageId = ? AND deviceId = ?";

  private static final String ME_UPDATE_MARK_AS_RECEIVED = "UPDATE mmxMessage SET state = ?, dateAcknowledgedUTC = ? WHERE messageId = ? AND deviceId = ?";

  private static final String ME_UPDATE_MARK_AS_DELIVERED = "UPDATE mmxMessage SET state = ? WHERE messageId = ? AND deviceId = ? AND appId = ? " +
      "AND state != ?";

  private static final String ME_UPDATE_MARK_AS_WAKEUP_SENT = "UPDATE mmxMessage SET state = ? WHERE messageId = ? AND deviceId = ? AND state = ? ";

  private static final String ME_QUERY_BY_STATE = "SELECT id, messageId, deviceId, fromJID, toJID, dateQueuedUTC, state, " +
      "appId, dateAcknowledgedUTC, sourceMessageId, messageType FROM mmxMessage WHERE state = ? ORDER BY dateQueuedUTC ";

  private static final String ME_FOR_RETRY_QUERY = "SELECT  m.id, m.messageId, m.deviceId, m.fromJID, m.toJID, " +
      "m.dateQueuedUTC, m.state, m.appId, m.dateAcknowledgedUTC, max(w.dateSentUTC), m.sourceMessageId, m.messageType FROM mmxWakeupQueue w, mmxMessage m WHERE " +
      "m.state = ? AND w.messageId = m.messageId " +
      "AND w.deviceId = m.deviceId " +
      "AND w.dateSentUTC IS NOT NULL  GROUP BY w.messageId, w.deviceId HAVING (? - MAX(dateSentUTC) > ?) " +
      "AND count(w.messageId) < ?";

  private static final String ME_QUERY_FOR_TIMEOUT = "UPDATE mmxMessage set state = ? WHERE state = ? AND dateAcknowledgedUTC IS NULL AND " +
      "            (? - dateQueuedUTC > ?)";

  private static final String ME_QUERY_BY_MESSAGE_ID_AND_DEVICE_ID = "SELECT id, messageId, deviceId, fromJID, toJID, dateQueuedUTC, state, " +
      "appId, dateAcknowledgedUTC, sourceMessageId, messageType FROM mmxMessage WHERE messageId = ? AND deviceId = ? ";

  private static final String ME_QUERY_BY_MESSAGE_ID_AND_APP_ID = "SELECT id, messageId, deviceId, fromJID, toJID, dateQueuedUTC, state, " +
      "appId, dateAcknowledgedUTC, sourceMessageId, messageType FROM mmxMessage WHERE appId = ? AND messageId = ? ORDER BY deviceId";

  private static final String ME_UPDATE_STATE_AFTER_TOKEN_INVALIDATION_QUERY = " UPDATE mmxMessage m, mmxWakeupQueue w " +
      "SET m.state = ? WHERE m.messageId = w.messageId AND m.deviceId = w.deviceId AND " +
      "(m.state = 'WAKEUP_REQUIRED' OR m.state = 'WAKEUP_SENT') AND w.appId = ? AND  w.tokenType = ? AND " +
      "w.clientToken = ?";

  private static final String ME_UPDATE_STATE_FOR_BAD_API_KEY_QUERY = " UPDATE mmxMessage m " +
      "SET m.state = ? WHERE m.appId = ? AND m.messageId = ? AND m.deviceId = ? AND " +
      "(m.state = 'WAKEUP_REQUIRED' OR m.state = 'WAKEUP_SENT')  ";

  private ConnectionProvider provider;

  private final static String ESCAPED_COMMA = Pattern.quote(",");

  public MessageDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public void persist(MessageEntity entity) throws DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Integer rv = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_INSERT_QUERY, PreparedStatement.RETURN_GENERATED_KEYS);
      pstmt.setString(1, entity.getMessageId());
      pstmt.setString(2, entity.getFrom());
      pstmt.setString(3, entity.getTo());
      long dateQueuedUTC = new Date().getTime()/1000L;
      pstmt.setLong(4, dateQueuedUTC);
      pstmt.setString(5, entity.getState().toString());
      pstmt.setString(6, entity.getAppId());
      pstmt.setString(7, entity.getDeviceId());
      String sourceMessageId = entity.getSourceMessageId();
      if (sourceMessageId != null) {
        pstmt.setString(8, sourceMessageId);
      } else {
        pstmt.setNull(8, Types.VARCHAR);
      }
      MessageEntity.MessageType type = entity.getType();
      if (type == null) {
        type = MessageEntity.MessageType.REGULAR;
      }
      pstmt.setString(9, type.name());
      pstmt.executeUpdate();
      rs = pstmt.getGeneratedKeys();

      if (rs.next()) {
        rv = Integer.valueOf(rs.getInt(1));
      }
      rs.close();
      pstmt.close();
      con.close();
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in creating the device record", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
  }

  @Override
  public List<MessageEntity> getMessages(MessageEntity.MessageState state) throws DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<MessageEntity> returnList = new ArrayList<MessageEntity>(20);
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_QUERY_BY_STATE);
      pstmt.setString(1, state.toString());

      rs = pstmt.executeQuery();
      while (rs.next()) {
        MessageEntity pae = new MessageEntity.MessageEntityBuilder().build(rs);
        returnList.add(pae);
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return returnList;
  }

  @Override
  public void messageReceived(String messageId, String receivedByDeviceId) throws
      DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_UPDATE_MARK_AS_RECEIVED);
      pstmt.setString(1, MessageEntity.MessageState.RECEIVED.name());
      pstmt.setLong(2, new Date().getTime()/1000L);
      pstmt.setString(3, messageId);
      pstmt.setString(4, receivedByDeviceId);
      pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in updating message state", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public void updateMessageState(String messageId, String deviceId, MessageEntity.MessageState state) throws
      DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_UPDATE_QUERY);
      pstmt.setString(1, state.toString());
      pstmt.setString(2, messageId);
      pstmt.setString(3, deviceId);
      pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in updating message state", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public void wakeupSent(String messageId, String deviceId) throws DbInteractionException {
    //private static final String ME_UPDATE_MARK_AS_WAKEUP_SENT = "UPDATE mmxMessage SET state = ? WHERE messageId = ? AND deviceId = ? AND state = ? ";
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_UPDATE_MARK_AS_WAKEUP_SENT);
      pstmt.setString(1, MessageEntity.MessageState.WAKEUP_SENT.name());
      pstmt.setString(2, messageId);
      pstmt.setString(3, deviceId);
      pstmt.setString(4, MessageEntity.MessageState.WAKEUP_REQUIRED.name());
      pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in wakeupSent", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }

  }

  @Override
  public MessageEntity get(String messageId, String deviceId) throws DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    MessageEntity rv = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_QUERY_BY_MESSAGE_ID_AND_DEVICE_ID);
      pstmt.setString(1, messageId);
      pstmt.setString(2, deviceId);
      rs = pstmt.executeQuery();
      if (rs.next()) {
        rv = new MessageEntity.MessageEntityBuilder().build(rs);
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return rv;
  }

  @Override
  public int messageDelivered(String appId, String deviceId, String messageId) throws DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_UPDATE_MARK_AS_DELIVERED);
      pstmt.setString(1, MessageEntity.MessageState.DELIVERED.name());
      pstmt.setString(2, messageId);
      pstmt.setString(3, deviceId);
      pstmt.setString(4, appId);
      pstmt.setString(5, MessageEntity.MessageState.RECEIVED.name());
      LOGGER.trace("messageDelivered : appId={}, deviceId={}, messageId={}, statement={}", new Object[]{appId, deviceId, messageId, pstmt});
      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
      return count;
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in messageDelivered", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public int purgeDeliveredMessages(Date start, Date end) throws DbInteractionException {
    return 0;
  }

  @Override
  public List<MessageEntity> getMessagesForRetryProcessing(int timeSinceLastWakeup, long utcTime, int maxRetryCount) throws
      DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<MessageEntity> returnList = new ArrayList<MessageEntity>(20);
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_FOR_RETRY_QUERY);
      pstmt.setString(1, MessageEntity.MessageState.WAKEUP_SENT.toString());
      pstmt.setLong(2, utcTime);
      pstmt.setInt(3, timeSinceLastWakeup);
      pstmt.setInt(4, maxRetryCount);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        MessageEntity pae = new MessageEntity.MessageEntityBuilder().build(rs);
        returnList.add(pae);
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return returnList;
  }

  @Override
  public List<MessageEntity> getMessages(String appId, String messageId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<MessageEntity> returnList = new ArrayList<MessageEntity>(20);
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_QUERY_BY_MESSAGE_ID_AND_APP_ID);
      pstmt.setString(1, appId);
      pstmt.setString(2, messageId);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        MessageEntity pae = new MessageEntity.MessageEntityBuilder().build(rs);
        returnList.add(pae);
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return returnList;
  }

  @Override
  public SearchResult<MessageEntity> searchMessages(String appId, MessageSearchOption searchOption, String searchValue, MessageSortOption sortOption, PaginationInfo info) {
    ValueHolder holder = new ValueHolder();
    holder.setValue1(searchValue);
    return searchMessages(appId, searchOption, holder, sortOption, info, true);
  }

  @Override
  public SearchResult<MessageEntity> searchMessages(String appId, MessageSearchOption searchOption,
                                                    ValueHolder valueHolder, MessageSortOption sortOption,
                                                    PaginationInfo info, boolean filterReceiptMessages) {
    Connection con = null;
    int totalCount = 0;
    List<MessageEntity> returnList = new ArrayList<MessageEntity>(info.getTakeSize());
    try {
      con = provider.getConnection();

      MessageSearchQueryBuilder queryBuilder = new MessageSearchQueryBuilder();
      QueryHolder holder = queryBuilder.buildQuery(con, searchOption, valueHolder, sortOption, info, appId,
          filterReceiptMessages);
      List<QueryParam> paramList = holder.getParamList();
      //count query
      {
        PreparedStatement countPS = holder.getCountQuery();
        //set the parameters
        int index = 1;
        for (QueryParam param : paramList) {
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
      //result query
      {
        PreparedStatement resultStatement = holder.getResultQuery();
        int index = 1;
        for (QueryParam param : paramList) {
          QueryParam.setParameterValue(param, index++, resultStatement);
        }
        ResultSet resultSet = resultStatement.executeQuery();
        while (resultSet.next()) {
          MessageEntity pae = new MessageEntity.MessageEntityBuilder().build(resultSet);
          returnList.add(pae);
        }
        resultSet.close();
        resultStatement.close();
        CloseUtil.close(LOGGER, resultSet);
        CloseUtil.close(LOGGER, resultStatement);
      }
      con.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, con);
    }
    SearchResult<MessageEntity> results = new SearchResult<MessageEntity>();
    results.setResults(returnList);
    results.setTotal(totalCount);
    results.setSize(info.getTakeSize());
    results.setOffset(info.getSkipSize());

    return results;
  }

  @Override
  public Map<String, MMXInAppMessageStats> getMessageStats(List<String> appIdList) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String statmentStr = "SELECT appId, state, COUNT(*) FROM mmxMessage WHERE appId IN ( " + SqlUtil.getQs(appIdList.size()) + " ) AND dateQueuedUTC > ?" + " AND messageType = 'REGULAR' " + " GROUP BY appId, state";
    Map<String, MMXInAppMessageStats> statsMap = new HashMap<String, MMXInAppMessageStats>();
    try {

      con = provider.getConnection();
      pstmt = con.prepareStatement(statmentStr);
      for (int i = 1; i <= appIdList.size(); i++) {
        String appId = appIdList.get(i - 1);
        pstmt.setString(i, appId);
        statsMap.put(appId, new MMXInAppMessageStats(appId));
      }
      long weekBeforeTS = new DateTime().minusDays(7).getMillis()/1000L;
      pstmt.setLong(appIdList.size() + 1, weekBeforeTS);
      rs = pstmt.executeQuery();
      LOGGER.trace("getMessageCount : executedQuery = {}, results={}", pstmt, rs);
      while (rs.next()) {
        String appId = rs.getString(1);
        String state = rs.getString(2);
        int count = rs.getInt(3);
        MMXInAppMessageStats appStats = statsMap.get(appId);
        appStats.addStats(new MMXInAppTuple(state, count));
        LOGGER.trace("getMessageCount : appId={}, state={}, count={}", new Object[]{appId, state, count});
      }
    } catch (SQLException e) {
      LOGGER.error("getMessageCount : exception caught appIdList={}", appIdList);
      e.printStackTrace();
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }

    return statsMap;
  }

  @Override
  public int messageTimeout(long utcTime, int timeoutMinutes) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_QUERY_FOR_TIMEOUT);
      pstmt.setString(1, MessageEntity.MessageState.WAKEUP_TIMEDOUT.name());
      pstmt.setString(2, MessageEntity.MessageState.WAKEUP_SENT.name());
      pstmt.setLong(3, utcTime);
      pstmt.setInt(4, timeoutMinutes * 60);
      int rowCount = pstmt.executeUpdate();
      pstmt.close();
      con.close();
      return rowCount;
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in messageTimeout", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  public static class MessageSearchQueryBuilder {

    private static final String BASE_QUERY = "SELECT id, messageId, deviceId, fromJID, toJID, dateQueuedUTC, state, " +
        "appId, dateAcknowledgedUTC,sourceMessageId,messageType FROM mmxMessage WHERE appId = ?";

    private static final String BASE_COUNT_QUERY = "SELECT count(1) FROM mmxMessage WHERE appId = ? ";


    public QueryHolder buildQuery(Connection conn, MessageSearchOption searchOption, ValueHolder searchValue,
                                  MessageSortOption sortOption, PaginationInfo info, String appId,
                                  boolean excludeReceiptMessage) throws SQLException {
      List<QueryParam> paramList = new ArrayList<QueryParam>(10);

      //for appId
      QueryParam appIdParam = new QueryParam(Types.VARCHAR, appId, true);
      paramList.add(appIdParam);

      StringBuilder queryBuilder = new StringBuilder();
      if (excludeReceiptMessage) {
        // exclude the receipt messages
        queryBuilder.append(" AND messageType != ? ");
        QueryParam param = new QueryParam(Types.VARCHAR, MessageEntity.MessageType.RECEIPT.name(), true);
        paramList.add(param);
      }
      /*
        build the WHERE parts first
       */
      if (searchOption == MessageSearchOption.MESSAGE_ID) {
        /*
         * check is the value is a comma separated list.
         */
        String[] values = searchValue.getValue1().split(ESCAPED_COMMA);
        if (values.length > 1) {
          queryBuilder.append(" AND messageId IN (");
          for (int i = 0; i < values.length; i++) {
            if (i > 0) {
              queryBuilder.append(",");
            }
            queryBuilder.append("?");
            QueryParam param = new QueryParam(Types.VARCHAR, values[i], true);
            paramList.add(param);
          }
          queryBuilder.append(") ");
        } else {
          queryBuilder.append(" AND messageId = ? ");
          QueryParam param = new QueryParam(Types.VARCHAR, searchValue.getValue1(), true);
          paramList.add(param);
        }
      }

      if (searchOption == MessageSearchOption.TARGET_DEVICE_ID) {
        queryBuilder.append((" AND deviceId = ? "));
        QueryParam param = new QueryParam(Types.VARCHAR, searchValue.getValue1(), true);
        paramList.add(param);
      }

      if (searchOption == MessageSearchOption.MESSAGE_STATE) {
        queryBuilder.append((" AND state = ? "));
        QueryParam param = new QueryParam(Types.VARCHAR, searchValue.getValue1(), true);
        paramList.add(param);
      }

      if (searchOption == MessageSearchOption.DATE_SENT) {
        if (searchValue.getValue1() != null && searchValue.getValue2() != null) {
          //we have start and end values
          queryBuilder.append((" AND (dateQueuedUTC >= ? AND dateQueuedUTC < ?) "));
          QueryParam start = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue1()), true);
          QueryParam end = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue2()), true);
          paramList.add(start);
          paramList.add(end);
        } else if (searchValue.getValue1() != null) {
          //we have start value
          queryBuilder.append((" AND dateQueuedUTC >= ?  "));
          QueryParam start = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue1()), true);
          paramList.add(start);
        } else if (searchValue.getValue2() != null) {
          //we have end value
          queryBuilder.append((" AND dateQueuedUTC < ?  "));
          QueryParam start = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue2()), true);
          paramList.add(start);
        }
      }
      if (searchOption == MessageSearchOption.DATE_ACKNOWLEDGED) {
        if (searchValue.getValue1() != null && searchValue.getValue2() != null) {
          //we have start and end values
          queryBuilder.append((" AND (dateAcknowledgedUTC >= ? AND dateAcknowledgedUTC < ?) "));
          QueryParam start = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue1()), true);
          QueryParam end = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue2()), true);
          paramList.add(start);
          paramList.add(end);
        } else if (searchValue.getValue1() != null) {
          //we have start value
          queryBuilder.append((" AND dateAcknowledgedUTC >= ?  "));
          QueryParam start = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue1()), true);
          paramList.add(start);
        } else if (searchValue.getValue2() != null) {
          //we have end value
          queryBuilder.append((" AND dateAcknowledgedUTC < ?  "));
          QueryParam start = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue2()), true);
          paramList.add(start);
        }
      }
      /**
       * now the ordering by
       */
      MessageSearchOption column = sortOption.getColumn();
      SortOrder sort = sortOption.getOrder();
      if (column == MessageSearchOption.DATE_SENT) {
        queryBuilder.append(" ORDER BY dateQueuedUTC ");
      } else if (column == MessageSearchOption.DATE_ACKNOWLEDGED) {
        queryBuilder.append(" ORDER BY dateAcknowledgedUTC ");
      } else if (column == MessageSearchOption.TARGET_DEVICE_ID) {
        queryBuilder.append(" ORDER BY deviceId ");
      } else if (column == MessageSearchOption.MESSAGE_ID) {
        queryBuilder.append(" ORDER BY messageId ");
      } else if (column == MessageSearchOption.MESSAGE_STATE) {
        queryBuilder.append(" ORDER BY state ");
      }


      if (sort == SortOrder.DESCENDING) {
        queryBuilder.append(" DESC ");
      }

      StringBuilder countQueryBuilder = new StringBuilder();
      countQueryBuilder.append(BASE_COUNT_QUERY).append(queryBuilder.toString());


      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Count query:" + countQueryBuilder.toString());
      }

      StringBuilder resultQueryBuilder = new StringBuilder();
      resultQueryBuilder.append(BASE_QUERY).append(queryBuilder);

      if (info != null) {
        int chunk = info.getTakeSize();
        int offset = info.getSkipSize();
        resultQueryBuilder.append(" LIMIT ? OFFSET ?");
        QueryParam chunkp = new QueryParam(Types.INTEGER, chunk);
        QueryParam offsetp = new QueryParam(Types.INTEGER, offset);
        paramList.add(chunkp);
        paramList.add(offsetp);
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("result query:" + resultQueryBuilder.toString());
      }

      PreparedStatement countPS = conn.prepareStatement(countQueryBuilder.toString());

      PreparedStatement resultPS = conn.prepareStatement(resultQueryBuilder.toString());

      QueryHolder holder = new QueryHolder(countPS, resultPS);
      holder.setParamList(paramList);

      return holder;
    }
  }

  @Override
  public int changeStateToPending(String appId, PushType type, String token) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_UPDATE_STATE_AFTER_TOKEN_INVALIDATION_QUERY);
      pstmt.setString(1, MessageEntity.MessageState.PENDING.name());
      pstmt.setString(2, appId);
      pstmt.setString(3, type.name());
      pstmt.setString(4, token);
      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
      return count;
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in changeStateToPending", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }


  @Override
  public int changeStateToPending(String appId, String messageId, String deviceId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ME_UPDATE_STATE_AFTER_TOKEN_INVALIDATION_QUERY);
      pstmt.setString(1, MessageEntity.MessageState.PENDING.name());
      pstmt.setString(2, appId);
      pstmt.setString(3, messageId);
      pstmt.setString(4, appId);
      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
      return count;
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in changeStateToPending", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }
}

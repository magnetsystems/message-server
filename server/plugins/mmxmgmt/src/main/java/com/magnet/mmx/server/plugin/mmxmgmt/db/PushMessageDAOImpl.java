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

import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MMXPushMessageStats;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MMXPushTuple;
import com.magnet.mmx.server.plugin.mmxmgmt.util.SqlUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


/**
 */
public class PushMessageDAOImpl implements PushMessageDAO {

  private static final Logger LOGGER = LoggerFactory.getLogger(PushMessageDAOImpl.class);

  private static final String INSERT_SQL = "INSERT INTO mmxPushMessage (messageId, deviceId, appId, dateSentUTC, type, state) " +
      "VALUES (?, ?, ? ,?, ?, ?)";

  private static final String SELECT_BY_ID_SQL = "SELECT messageId, deviceId, appId, dateSentUTC, type, state, dateAcknowledgedUTC FROM mmxPushMessage WHERE messageId = ? ";

  private static final String SELECT_BY_APPID_DEVICEID_SQL = "SELECT messageId, deviceId, appId, dateSentUTC, type, state, dateAcknowledgedUTC FROM mmxPushMessage WHERE appId = ? AND deviceId = ? ORDER BY dateSentUTC DESC";

  private static final String UPDATE_BY_ID_SQL = "UPDATE mmxPushMessage SET state = ?, dateAcknowledgedUTC = ? WHERE messageId = ? ";

  private ConnectionProvider provider;

  /**
   * Constructor.
   * @param provider
   */
  public PushMessageDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public void add(PushMessageEntity entity) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(INSERT_SQL);
      pstmt.setString(1, entity.getMessageId());
      pstmt.setString(2, entity.getDeviceId());
      pstmt.setString(3, entity.getAppId());
      pstmt.setLong(4, new Date().getTime()/1000L);
      pstmt.setString(5, entity.getType().name());
      pstmt.setString(6, entity.getState().name());
      pstmt.executeUpdate();
      pstmt.close();
      con.close();
      LOGGER.debug("add : added pushMessageEntity={}", entity);
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in creating the push message record", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public List<PushMessageEntity> getPushMessages(String appId, String deviceId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<PushMessageEntity> returnList = new ArrayList<PushMessageEntity>(10);
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(SELECT_BY_APPID_DEVICEID_SQL);
      pstmt.setString(1, appId);
      pstmt.setString(2, deviceId);

      rs = pstmt.executeQuery();
      while (rs.next()) {
        PushMessageEntity pae = new PushMessageEntity.PushMessageEntityBuilder().build(rs);
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
  public PushMessageEntity getPushMessage(String messageId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    PushMessageEntity rv = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(SELECT_BY_ID_SQL);
      pstmt.setString(1, messageId);

      rs = pstmt.executeQuery();
      if (rs.first()) {
        rv = new PushMessageEntity.PushMessageEntityBuilder().build(rs);
      } else {
        return null;
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

  public Map<String, MMXPushMessageStats> getPushMessageStats(List<String> appIdList) {
    final String statementStr = "SELECT appId, type, state, COUNT(*) FROM mmxPushMessage WHERE appId IN ("+ SqlUtil.getQs(appIdList.size())+") AND dateSentUTC > ?" +  " GROUP by appId, type, state";
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Map<String, MMXPushMessageStats> statsMap = new HashMap<String, MMXPushMessageStats>();
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(statementStr);
      for(int i=1; i <= appIdList.size(); i++) {
        String appId = appIdList.get(i - 1);
        pstmt.setString(i,appId);
        statsMap.put(appId, new MMXPushMessageStats(appId));
      }
      long wkBeforeTS = new DateTime().minusDays(7).getMillis()/1000L;
      pstmt.setLong(appIdList.size() + 1, wkBeforeTS);
      rs = pstmt.executeQuery();
      LOGGER.trace("getPushMessageStats : executedQuery = {}, results={}", pstmt, rs);
      while(rs.next()) {
        String appId = rs.getString(1);
        MMXPushMessageStats stats = statsMap.get(appId);
        String type = rs.getString(2);
        String state = rs.getString(3);
        int count = rs.getInt(4);
        stats.addStats(new MMXPushTuple(type, state, count));
      }
    } catch (SQLException e) {
      LOGGER.error("getPushMessageStats : exception caught appIdList={}", appIdList);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return statsMap;
  }

  @Override
  public int acknowledgePushMessage(String messageId, Date dateAcknowledged) {
    Connection con = null;
    PreparedStatement pstmt = null;
    PushMessageEntity rv = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(UPDATE_BY_ID_SQL);
      pstmt.setString(1, PushMessageEntity.PushMessageState.ACKNOWLEDGED.name());
      pstmt.setLong(2, new Date().getTime()/1000L);
      pstmt.setString(3, messageId);

      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
      return count;
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public SearchResult<PushMessageEntity> getPushMessagesWithPagination(QueryBuilderResult query, PaginationInfo info) {
    Connection con = null;
    int totalCount = 0;
    List<PushMessageEntity> returnList = new ArrayList<PushMessageEntity>(info.getTakeSize());
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
          returnList.add(new PushMessageEntity.PushMessageEntityBuilder().build(resultSet));
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
    SearchResult<PushMessageEntity> results = new SearchResult<PushMessageEntity>();
    results.setResults(returnList);
    results.setTotal(totalCount);
    results.setSize(info.getTakeSize());
    results.setOffset(info.getSkipSize());
    return results;
  }
}

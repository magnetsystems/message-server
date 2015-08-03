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
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.server.plugin.mmxmgmt.search.user.UserSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.user.UserSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import com.magnet.mmx.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


/**
 */
public class UserDAOImpl implements UserDAO {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserDAOImpl.class);
  private static final String PERCENTAGE = "%";
  private ConnectionProvider provider;

  public UserDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public UserSearchResult searchUsers(String appId, UserSearchOption searchOption, ValueHolder valueHolder, UserSortOption sortOption, PaginationInfo info) {
    Connection con = null;
    int totalCount = 0;
    List<UserEntity> returnList = new ArrayList<UserEntity>((info != null) ? info.getTakeSize() : 100);
    try {
      con = provider.getConnection();

      UserSearchQueryBuilder queryBuilder = new UserSearchQueryBuilder();
      QueryHolder holder = queryBuilder.buildQuery(con, searchOption, valueHolder, sortOption, info, appId);
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
        LOGGER.trace("searchUsers : executing query : {}", countPS);
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
        LOGGER.trace("searchUsers : executing query : {}", resultStatement);
        ResultSet resultSet = resultStatement.executeQuery();
        while (resultSet.next()) {
          UserEntity pae = new UserEntity.UserEntityBuilder().build(resultSet);
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
    UserSearchResult results = new UserSearchResult();
    results.setResults(returnList);
    results.setTotal(totalCount);
    results.setSize(info != null ? info.getTakeSize() : null);
    results.setOffset(info != null ? info.getSkipSize() : null);

    return results;
  }

  public static class UserSearchQueryBuilder {

    private static final String BASE_QUERY = "SELECT username,name,email,creationDate,modificationDate " +
        " FROM ofUser WHERE username LIKE ? ";

    private static final String BASE_COUNT_QUERY = "SELECT count(1) FROM ofUser WHERE username LIKE ? ";


    public QueryHolder buildQuery(Connection conn, UserSearchOption searchOption, ValueHolder searchValue,
                                  UserSortOption sortOption, PaginationInfo info, String appId) throws SQLException {
      List<QueryParam> paramList = new ArrayList<QueryParam>(10);

      //for appId
      QueryParam appIdParam = new QueryParam(Types.VARCHAR, PERCENTAGE + appId, true);
      paramList.add(appIdParam);

      StringBuilder queryBuilder = new StringBuilder();
      /*
        build the WHERE parts first
       */
      if (searchOption == UserSearchOption.NAME) {
        queryBuilder.append(" AND name LIKE ? ");
        QueryParam param = new QueryParam(Types.VARCHAR, PERCENTAGE + searchValue.getValue1() + PERCENTAGE, true);
        paramList.add(param);
      }

      if (searchOption == UserSearchOption.EMAIL) {
        queryBuilder.append((" AND email LIKE ? "));
        QueryParam param = new QueryParam(Types.VARCHAR, PERCENTAGE + searchValue.getValue1() + PERCENTAGE, true);
        paramList.add(param);
      }

      if (searchOption == UserSearchOption.PHONE) {
        queryBuilder.append((" AND username IN (SELECT concat(ownerJid, ? ) FROM mmxDevice WHERE phoneNumberRev LIKE ?) "));
        QueryParam param1 = new QueryParam(Types.VARCHAR,
            (JIDUtil.APP_ID_DELIMITER + appId), true);
        QueryParam param2 = new QueryParam(Types.VARCHAR, Helper.reverse(
            Utils.normalizePhone(searchValue.getValue1(), 0)) + PERCENTAGE, true);
        paramList.add(param1);
        paramList.add(param2);
      }

      if (searchOption == UserSearchOption.USERNAME) {
        queryBuilder.append((" AND SUBSTRING(username,1,(LOCATE(?,username)-1)) LIKE ? "));
        // TODO: "%userId%" will be slow! it is a table scan.
        QueryParam param = new QueryParam(Types.VARCHAR, PERCENTAGE +
            searchValue.getValue1() + PERCENTAGE, true);
        paramList.add(appIdParam);
        paramList.add(param);
      }
      /**
       * now the ordering by
       */
      if (sortOption != null) {
        UserSearchOption column = sortOption.getColumn();
        SortOrder sort = sortOption.getOrder();
        if (column == UserSearchOption.USERNAME) {
          queryBuilder.append(" ORDER BY username ");
        } else if (column == UserSearchOption.EMAIL) {
          queryBuilder.append(" ORDER BY email ");
        } else if (column == UserSearchOption.NAME) {
          queryBuilder.append(" ORDER BY name ");
        }

        if (sort == SortOrder.DESCENDING) {
          queryBuilder.append(" DESC ");
        }
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
  public SearchResult<UserEntity> getUsersWithPagination(QueryBuilderResult queryBuilderResult, PaginationInfo info) {
    Connection con = null;
    int totalCount = 0;
    List<UserEntity> returnList = new ArrayList<UserEntity>(info.getTakeSize());
    LOGGER.debug("Executing built query:{}, count query:{}", queryBuilderResult.getQuery(), queryBuilderResult.getCountQuery());
    try {
      con = provider.getConnection();
      {
        //do the count first
        PreparedStatement countPS = con.prepareStatement(queryBuilderResult.getCountQuery());
        //set the values
        int index = 1;
        for (QueryParam param : queryBuilderResult.getParamList()) {
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
        PreparedStatement resultPS = con.prepareStatement(queryBuilderResult.getQuery());
        int rindex = 1;
        for (QueryParam param : queryBuilderResult.getParamList()) {
          QueryParam.setParameterValue(param, rindex++, resultPS);
        }
        ResultSet resultSet = resultPS.executeQuery();

        while (resultSet.next()) {
          UserEntity pae = new UserEntity.UserEntityBuilder().build(resultSet);
          returnList.add(pae);
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
    SearchResult<UserEntity> results = new SearchResult<UserEntity>();
    results.setResults(returnList);
    results.setTotal(totalCount);
    results.setSize(info.getTakeSize());
    results.setOffset(info.getSkipSize());
    return results;
  }

  @Override
  public List<UserEntity> getUsers(QueryBuilderResult query) throws DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<UserEntity> userList = new ArrayList<UserEntity>();

    try {
      con = provider.getConnection();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Executing built query:{}", query.getQuery());
      }
      pstmt = con.prepareStatement(query.getQuery());
      int index = 1;
      for (QueryParam param : query.getParamList()) {
        QueryParam.setParameterValue(param, index++, pstmt);
      }
      rs = pstmt.executeQuery();
      while(rs.next()) {
        userList.add(new UserEntity.UserEntityBuilder().build(rs));
      }
    } catch (Exception e){
      LOGGER.error("Exception in retrieving devices using query builder result:{}", query, e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER,rs, pstmt, con);
    }
    return userList;
  }




  /**
   * Inner class for holding the prepared statements
   */
  private static class QueryHolder {
    private PreparedStatement countQuery;
    private PreparedStatement resultQuery;
    private List<QueryParam> paramList;

    public PreparedStatement getCountQuery() {
      return countQuery;
    }

    public PreparedStatement getResultQuery() {
      return resultQuery;
    }

    private QueryHolder(PreparedStatement countQuery, PreparedStatement resultQuery) {
      this.countQuery = countQuery;
      this.resultQuery = resultQuery;
    }

    public List<QueryParam> getParamList() {
      return paramList;
    }

    public void setParamList(List<QueryParam> paramList) {
      this.paramList = paramList;
    }
  }


  public UserEntity getUser(String username) throws DbInteractionException {
    final String statementStr = "SELECT * FROM ofUser WHERE username = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, username);
      LOGGER.trace("getUser : executing query : {}", pstmt);
      rs = pstmt.executeQuery();
      if(rs.next())
        return new UserEntity.UserEntityBuilder().build(rs);
      else
        return null;
    } catch (SQLException e) {
      LOGGER.error("getUser : caught exception : username={}", username, e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }
}

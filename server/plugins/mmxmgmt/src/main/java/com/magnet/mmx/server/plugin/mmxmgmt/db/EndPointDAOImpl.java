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
import com.magnet.mmx.server.plugin.mmxmgmt.search.endpoint.EndPointSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.endpoint.EndPointSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation that allows for retrieving EndPointEntity objects.
 */
public class EndPointDAOImpl implements EndPointDAO {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndPointDAOImpl.class);
  private static final String PERCENTAGE = "%";

  private ConnectionProvider provider;

  /**
   * Constructor
   *
   * @param provider
   */
  public EndPointDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public SearchResult<EndPointEntity> search(String appId, EndPointSearchOption searchOption, ValueHolder valueHolder, EndPointSortOption sortOption, PaginationInfo info) {
    Connection con = null;
    int totalCount = 0;
    List<EndPointEntity> returnList = new ArrayList<EndPointEntity>(info != null ? info.getTakeSize() : 10);
    try {
      con = provider.getConnection();

      QueryBuilder queryBuilder = new QueryBuilder();
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
          EndPointEntity pae = new EndPointEntity.EndPointEntityBuilder().build(resultSet, "u.", "d.");
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
    SearchResult<EndPointEntity> results = new SearchResult<EndPointEntity>();
    results.setResults(returnList);
    results.setTotal(totalCount);
    if (info != null) {
      results.setSize(info.getTakeSize());
      results.setOffset(info.getSkipSize());
    } else {
      results.setSize(totalCount);
      results.setOffset(0);
    }
    return results;
  }


  /**
   *
   */
  public static class QueryBuilder {

    private static final String BASE_QUERY = "SELECT d.id 'd.id', d.name 'd.name', d.ownerJid 'd.ownerJid', " +
        "d.deviceId 'd.deviceId', d.tokenType 'd.tokenType' , d.clientToken 'd.clientToken', d.versionInfo 'd.versionInfo'," +
        "d.modelInfo 'd.modelInfo', d.status 'd.status', d.dateCreated 'd.dateCreated', d.dateUpdated 'd.dateUpdated', " +
        "phoneNumber 'd.phoneNumber', carrierInfo 'd.carrierInfo', d.osType 'd.osType', d.appId 'd.appId',d.protocolVersionMajor,d.protocolVersionMinor," +
        "d.pushStatus," +
        "u.username 'u.username', u.name 'u.name', " +
        "u.email 'u.email', u.creationDate 'u.creationDate', u.modificationDate 'u.modificationDate' FROM " +
        "ofUser u, mmxDevice d where d.ownerJid = SUBSTRING(u.username,1,(LOCATE(?,u.username)-2)) and d.appId = ?";

    private static final String BASE_COUNT_QUERY = "SELECT count(1) FROM " +
        "ofUser u, mmxDevice d where d.ownerJid = SUBSTRING(u.username,1,(LOCATE(?,u.username)-2)) and d.appId = ?";


    public QueryHolder buildQuery(Connection conn, EndPointSearchOption searchOption, ValueHolder searchValue, EndPointSortOption sortOption, PaginationInfo info, String appId) throws
        SQLException {
      List<QueryParam> paramList = new ArrayList<QueryParam>(10);

      //for appId
      QueryParam appIdParam = new QueryParam(Types.VARCHAR, appId, true);
      paramList.add(appIdParam);
      paramList.add(appIdParam);
      StringBuilder queryBuilder = new StringBuilder();
      /*
        build the WHERE parts first
       */
      if (searchOption == EndPointSearchOption.ENDPOINT_NAME) {
        queryBuilder.append(" AND d.name LIKE ? ");
        QueryParam param = new QueryParam(Types.VARCHAR, PERCENTAGE + searchValue.getValue1() + PERCENTAGE, true);
        paramList.add(param);
      } else if (searchOption == EndPointSearchOption.ENDPOINT_STATUS) {
        queryBuilder.append(" AND d.status = ? ");
        QueryParam param = new QueryParam(Types.VARCHAR, searchValue.getValue1().toUpperCase(), true);
        paramList.add(param);
      } else if (searchOption == EndPointSearchOption.ENDPOINT_OSTYPE) {
        queryBuilder.append(" AND d.osType = ? ");
        QueryParam param = new QueryParam(Types.VARCHAR, searchValue.getValue1().toUpperCase(), true);
        paramList.add(param);
      } else if (searchOption == EndPointSearchOption.ENDPOINT_DATE_CREATED) {
        //we have start and end values
        queryBuilder.append((" AND (UNIX_TIMESTAMP(d.dateCreated) >= ? AND UNIX_TIMESTAMP(d.dateCreated) < ?) "));
        QueryParam start = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue1()), true);
        QueryParam end = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue2()), true);
        paramList.add(start);
        paramList.add(end);
      }
      /**
       * now the ordering by
       */
      EndPointSearchOption column = sortOption.getColumn();
      SortOrder sort = sortOption.getOrder();
//      USERNAME ("username", new MinThreeCharValueValidator() ),
//          ENDPOINT_NAME("epname", new MinThreeCharValueValidator() ),
//          ENDPOINT_DATE_CREATED("epdatecreated", new NotEmptyValueValidator()),
//          ENDPOINT_STATUS("epstatus", new NotEmptyValueValidator()),
//          ENDPOINT_OSTYPE("epostype", new NotEmptyValueValidator()),
      if (column == EndPointSearchOption.ENDPOINT_NAME) {
        queryBuilder.append(" ORDER BY d.name ");
      } else if (column == EndPointSearchOption.ENDPOINT_DATE_CREATED) {
        queryBuilder.append(" ORDER BY d.dateCreated ");
      } else if (column == EndPointSearchOption.ENDPOINT_STATUS) {
        queryBuilder.append(" ORDER BY d.status ");
      } else if (column == EndPointSearchOption.ENDPOINT_OSTYPE) {
        queryBuilder.append(" ORDER BY d.osType ");
      } else if (column == EndPointSearchOption.ENDPOINT_DATE_CREATED) {
        //we have start and end values
        queryBuilder.append((" AND (UNIX_TIMESTAMP(d.dateCreated) >= ? AND UNIX_TIMESTAMP(d.dateCreated) < ?) "));
        QueryParam start = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue1()), true);
        QueryParam end = new QueryParam(Types.BIGINT, Long.parseLong(searchValue.getValue2()), true);
        paramList.add(start);
        paramList.add(end);
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
}

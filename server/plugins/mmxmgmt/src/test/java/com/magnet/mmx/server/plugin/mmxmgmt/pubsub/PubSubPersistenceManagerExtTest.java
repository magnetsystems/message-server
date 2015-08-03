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
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.TopicQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class PubSubPersistenceManagerExtTest {

  private static BasicDataSource ds;
  private QueryBuilderResult builtQueries;

  @BeforeClass
  public static void setUp() throws Exception {
    ds = UnitTestDSProvider.getDataSource();

    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/pubsub-node-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/pubsub-tag-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
  }

  @AfterClass
  public static void teardown() {
    try {
      ds.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testSearchTopic() throws Exception {
    TopicQuery query = new TopicQuery();
    query.setTopicName("sport");

    String appId = "7wmi73wxin9";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    PaginationInfo paginationInfo = PaginationInfo.build(100, 0);
    QueryBuilderResult builtQueries = builder.buildPaginationQuery(query, appId, paginationInfo);

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(new BasicDataSourceConnectionProvider(ds), builtQueries, paginationInfo);

    assertNotNull(topicList);

    int size = topicList.getSize();
    int total = topicList.getTotal();

    int expectedSize = 100;
    int expectedTotal = 1;

    assertEquals("Non matching size", expectedSize, size);
    assertEquals("Non matching total", expectedTotal, total);

    List<TopicAction.TopicInfoWithSubscriptionCount> results = topicList.getResults();
    assertNotNull(results);
  }

  @Test
  public void testSearchTopic1() throws Exception {
    TopicQuery query = new TopicQuery();
    query.setTopicName("%sport''"); //with mysql reserved character.

    String appId = "7wmi73wxin9";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    PaginationInfo paginationInfo = PaginationInfo.build(100, 0);
    QueryBuilderResult builtQueries = builder.buildPaginationQuery(query, appId, paginationInfo);

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(new BasicDataSourceConnectionProvider(ds), builtQueries, paginationInfo);

    assertNotNull(topicList);

    int size = topicList.getSize();
    int total = topicList.getTotal();

    int expectedSize = 100;
    int expectedTotal = 0;

    assertEquals("Non matching size", expectedSize, size);
    assertEquals("Non matching total", expectedTotal, total);

    List<TopicAction.TopicInfoWithSubscriptionCount> results = topicList.getResults();
    assertNotNull(results);
  }

  @Test
  public void testSearchTopicsUsingTags() throws Exception {
    TopicQuery query = new TopicQuery();
    String[] tags = {"food", "fitness", "cricket", "sport"};
    query.setTags(Arrays.asList(tags));
    String appId = "7wmi73wxin9";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    PaginationInfo paginationInfo = PaginationInfo.build(100, 0);
    QueryBuilderResult builtQueries = builder.buildPaginationQuery(query, appId, paginationInfo);

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(new BasicDataSourceConnectionProvider(ds), builtQueries, paginationInfo);

    assertNotNull(topicList);

    int size = topicList.getSize();
    int total = topicList.getTotal();

    int expectedSize = 100;
    int expectedTotal = 2;

    assertEquals("Non matching size", expectedSize, size);
    assertEquals("Non matching total", expectedTotal, total);

    List<TopicAction.TopicInfoWithSubscriptionCount> results = topicList.getResults();
    assertNotNull(results);
  }

  @Test
  public void testSearchTopicsUsingTags2() throws Exception {
    TopicQuery query = new TopicQuery();
    String[] tags = {"dance"};
    query.setTags(Arrays.asList(tags));
    String appId = "7wmi73wxin9";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    PaginationInfo paginationInfo = PaginationInfo.build(100, 0);
    QueryBuilderResult builtQueries = builder.buildPaginationQuery(query, appId, paginationInfo);

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(new BasicDataSourceConnectionProvider(ds), builtQueries, paginationInfo);

    assertNotNull(topicList);

    int size = topicList.getSize();
    int total = topicList.getTotal();

    int expectedSize = 100;
    int expectedTotal = 0;

    assertEquals("Non matching size", expectedSize, size);
    assertEquals("Non matching total", expectedTotal, total);

    List<TopicAction.TopicInfoWithSubscriptionCount> results = topicList.getResults();
    assertNotNull(results);
  }


  @Test
  public void testSearchTopicWithDisplayNameAndPrefixMatch() throws Exception {
//    TopicQuery query = new TopicQuery();
//    query.setTopicName("sport");
    TopicAction.TopicSearch search = new TopicAction.TopicSearch();
    int expectedSize = 10;
    int offset = 0;
    TopicAction.TopicSearchRequest searchRequest = new TopicAction.TopicSearchRequest(SearchAction.Operator.OR, search, offset, expectedSize);
    searchRequest.setTopicName("spo", SearchAction.Match.PREFIX);

    String appId = "7wmi73wxin9";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    PaginationInfo paginationInfo = PaginationInfo.build(expectedSize, 0);
    QueryBuilderResult builtQueries = builder.buildPaginationQuery(searchRequest, appId, paginationInfo, null);

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(new BasicDataSourceConnectionProvider(ds), builtQueries, paginationInfo);

    assertNotNull(topicList);

    int size = topicList.getSize();
    int total = topicList.getTotal();


    int expectedTotal = 1;

    assertEquals("Non matching size", expectedSize, size);
    assertEquals("Non matching total", expectedTotal, total);

    List<TopicAction.TopicInfoWithSubscriptionCount> results = topicList.getResults();
    assertNotNull(results);
  }
}

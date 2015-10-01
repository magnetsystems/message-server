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
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.BaseDbTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.TestDataSource;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class PubSubPersistenceManagerExtTest {

  @ClassRule
  public static BaseDbTest.DataSourceResource dataSourceRule = new BaseDbTest.DataSourceResource(TestDataSource.PUBSUB_NODE_DATA_1, TestDataSource.PUBSUB_TAG_DATA_1);

  private static ConnectionProvider connectionProvider = new BasicDataSourceConnectionProvider(dataSourceRule.getDataSource());


  private QueryBuilderResult builtQueries;

  @Test
  public void testSearchTopic() throws Exception {
    TopicQuery query = new TopicQuery();
    query.setTopicName("sport");

    String appId = "7wmi73wxin9";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    PaginationInfo paginationInfo = PaginationInfo.build(100, 0);
    QueryBuilderResult builtQueries = builder.buildPaginationQuery(query, appId, paginationInfo);

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(connectionProvider, builtQueries, paginationInfo);

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

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(connectionProvider, builtQueries, paginationInfo);

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

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(connectionProvider, builtQueries, paginationInfo);

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

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(connectionProvider, builtQueries, paginationInfo);

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

    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> topicList = PubSubPersistenceManagerExt.getTopicWithPagination(connectionProvider, builtQueries, paginationInfo);

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

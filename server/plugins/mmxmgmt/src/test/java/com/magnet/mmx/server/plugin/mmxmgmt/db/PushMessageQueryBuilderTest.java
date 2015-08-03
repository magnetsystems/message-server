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

import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.PushMessageQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 */
public class PushMessageQueryBuilderTest {
  @Test
  public void testBuildQuery() throws Exception {
    String[] deviceIds = {"d1", "d2"};
    PushMessageQuery query = new PushMessageQuery();
    query.setDeviceIds(Arrays.asList(deviceIds));
    PushMessageQueryBuilder builder = new PushMessageQueryBuilder();
    QueryBuilderResult builtQuery = builder.buildQuery(query, "app1");

    Assert.assertNotNull("built query is null", builtQuery);

    String sqlQuery =  builtQuery.getQuery();
    String expected = "SELECT DISTINCT mmxPushMessage.* FROM mmxPushMessage WHERE (mmxPushMessage.deviceId IN (?,?)) AND (mmxPushMessage.appId=?)";
    Assert.assertEquals("Non matching sql query", expected, sqlQuery);

    String countQuery = builtQuery.getCountQuery();
    String expectedc = "SELECT COUNT(*) FROM mmxPushMessage WHERE (mmxPushMessage.deviceId IN (?,?)) AND (mmxPushMessage.appId=?)";
    Assert.assertEquals("Non matching sql query", expectedc, countQuery);

  }

  @Test
  public void testBuildQuerySingleDeviceId() throws Exception {
    String[] deviceIds = {"d1"};
    PushMessageQuery query = new PushMessageQuery();
    query.setDeviceIds(Arrays.asList(deviceIds));
    PushMessageQueryBuilder builder = new PushMessageQueryBuilder();
    QueryBuilderResult builtQuery = builder.buildQuery(query, "app1");

    Assert.assertNotNull("built query is null", builtQuery);

    String sqlQuery =  builtQuery.getQuery();
    String expected = "SELECT DISTINCT mmxPushMessage.* FROM mmxPushMessage WHERE (mmxPushMessage.deviceId=?) AND (mmxPushMessage.appId=?)";
    Assert.assertEquals("Non matching sql query", expected, sqlQuery);

    String countQuery = builtQuery.getCountQuery();
    String expectedc = "SELECT COUNT(*) FROM mmxPushMessage WHERE (mmxPushMessage.deviceId=?) AND (mmxPushMessage.appId=?)";
    Assert.assertEquals("Non matching sql query", expectedc, countQuery);
  }

  @Test
  public void testBuildQueryNoCriterion() throws Exception {
    PushMessageQuery query = new PushMessageQuery();
    PushMessageQueryBuilder builder = new PushMessageQueryBuilder();
    QueryBuilderResult builtQuery = builder.buildQuery(query, "app1");

    Assert.assertNotNull("built query is null", builtQuery);

    String sqlQuery =  builtQuery.getQuery();
    String expected = "SELECT DISTINCT mmxPushMessage.* FROM mmxPushMessage WHERE (mmxPushMessage.appId=?)";
    Assert.assertEquals("Non matching sql query", expected, sqlQuery);

    String countQuery = builtQuery.getCountQuery();
    String expectedc = "SELECT COUNT(*) FROM mmxPushMessage WHERE (mmxPushMessage.appId=?)";
    Assert.assertEquals("Non matching sql query", expectedc, countQuery);
  }

  @Test
  public void testBuildQueryWithEverything() throws Exception {
    String[] deviceIds = {"d1", "d2"};
    PushMessageQuery query = new PushMessageQuery();
    query.setDeviceIds(Arrays.asList(deviceIds));
    PushMessageQueryBuilder builder = new PushMessageQueryBuilder();
    SortInfo info = SortInfo.build("deviceId", "ascending");
    PaginationInfo pinfo = PaginationInfo.build(50, 0);
    QueryBuilderResult builtQuery = builder.buildPaginationQueryWithOrder(query, "app1", pinfo, info);

    Assert.assertNotNull("built query is null", builtQuery);

    String sqlQuery =  builtQuery.getQuery();
    String expected = "SELECT DISTINCT mmxPushMessage.* FROM mmxPushMessage WHERE (mmxPushMessage.deviceId IN (?,?)) " +
        "AND (mmxPushMessage.appId=?) ORDER BY deviceId  LIMIT ? OFFSET ?";
    Assert.assertEquals("Non matching sql query", expected, sqlQuery);

    String countQuery = builtQuery.getCountQuery();
    String expectedc = "SELECT COUNT(*) FROM mmxPushMessage WHERE (mmxPushMessage.deviceId IN (?,?)) AND (mmxPushMessage.appId=?)";
    Assert.assertEquals("Non matching sql query", expectedc, countQuery);

  }

  @Test
  public void testBuildQueryWithDateSentRange() throws Exception {
    PushMessageQuery query = new PushMessageQuery();
    DateRange sentRange = new DateRange();
    sentRange.setStart(1425981600);
    sentRange.setEnd(1425985200);
    query.setDateSent(sentRange);
    PushMessageQueryBuilder builder = new PushMessageQueryBuilder();
    SortInfo sortInfo = SortInfo.build("dateSent", "descending");
    int expectedSize = 10;
    PaginationInfo paginationInfo = PaginationInfo.build(expectedSize, 0);
    QueryBuilderResult builtQuery = builder.buildPaginationQuery(query, "app1", paginationInfo);

    Assert.assertNotNull("built query is null", builtQuery);

    String sqlQuery =  builtQuery.getQuery();
    String expected = "SELECT DISTINCT mmxPushMessage.* FROM mmxPushMessage WHERE (mmxPushMessage.dateSentUTC>=? AND mmxPushMessage.dateSentUTC<?) AND (mmxPushMessage.appId=?)  LIMIT ? OFFSET ?";
    Assert.assertEquals("Non matching sql query", expected, sqlQuery);
    int paramCount = builtQuery.getParamList().size();
    Assert.assertEquals("Non matching param count", 5, paramCount);
  }
}

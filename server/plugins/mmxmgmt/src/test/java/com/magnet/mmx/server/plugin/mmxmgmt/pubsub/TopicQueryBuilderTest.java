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
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryParam;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class TopicQueryBuilderTest {
  @Test
  public void testBuildQuery() throws Exception {
    TopicQuery topicQuery = new TopicQuery();
    topicQuery.setTopicName("fastfood");
    String appId = "i1s3wtx3m0d";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    QueryBuilderResult result = builder.buildPaginationQuery(topicQuery, appId, PaginationInfo.build(100, 0));
    assertNotNull(result);

    String resultQuery = result.getQuery();
    String expected =
        "SELECT DISTINCT ofPubsubNode.* , (SELECT count(1) FROM ofPubsubSubscription s where " +
        "s.serviceID = ofPubsubNode.serviceId AND s.nodeID = ofPubsubNode.nodeID GROUP by s.nodeID,s.serviceId ) " +
        "as 'subcount' FROM ofPubsubNode WHERE (UPPER(ofPubsubNode.name) LIKE ?) AND (ofPubsubNode.nodeID LIKE ? " +
        "OR ofPubsubNode.nodeID LIKE ?)  LIMIT ? OFFSET ?";

    assertEquals("Non matching generated query", expected, resultQuery);
    int paramCount = result.getParamList().size();
    assertEquals("Non matching parameter count", 5, paramCount);
  }

  @Test
  public void testBuildQueryUsingDescription() throws Exception {
    TopicQuery topicQuery = new TopicQuery();
    topicQuery.setDescription("fast food joint");
    String appId = "i1s3wtx3m0d";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    QueryBuilderResult result = builder.buildPaginationQuery(topicQuery, appId, PaginationInfo.build(100, 0));
    assertNotNull(result);

    String resultQuery = result.getQuery();
    String expected = "SELECT DISTINCT ofPubsubNode.* , (SELECT count(1) FROM ofPubsubSubscription s where s.serviceID = ofPubsubNode.serviceId AND s.nodeID = ofPubsubNode.nodeID GROUP by s.nodeID,s.serviceId ) as 'subcount' FROM ofPubsubNode WHERE (UPPER(ofPubsubNode.description) LIKE ?) AND (ofPubsubNode.nodeID LIKE ? OR ofPubsubNode.nodeID LIKE ?)  LIMIT ? OFFSET ?";

    assertEquals("Non matching generated query", expected, resultQuery);
    int paramCount = result.getParamList().size();
    assertEquals("Non matching parameter count", 5, paramCount);
  }

  @Test
  public void testBuildQueryUsingTags() throws Exception {
    TopicQuery topicQuery = new TopicQuery();
    String[] tags = {"local", "city", "food"};
    topicQuery.setTags(Arrays.asList(tags));
    String appId = "i1s3wtx3m0d";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    QueryBuilderResult result = builder.buildPaginationQuery(topicQuery, appId, PaginationInfo.build(100, 0));
    assertNotNull(result);

    String resultQuery = result.getQuery();
    String expected = "SELECT DISTINCT ofPubsubNode.* , (SELECT count(1) FROM ofPubsubSubscription s where s.serviceID = " +
        "ofPubsubNode.serviceId AND s.nodeID = ofPubsubNode.nodeID GROUP by s.nodeID,s.serviceId ) as 'subcount' FROM " +
        "ofPubsubNode,mmxTag WHERE (( ( mmxTag.tagname = ? OR mmxTag.tagname = ? OR mmxTag.tagname = ? ) AND " +
        "mmxTag.serviceID=ofPubsubNode.serviceID AND mmxTag.nodeID=ofPubsubNode.nodeID )) AND " +
        "(ofPubsubNode.nodeID LIKE ? OR ofPubsubNode.nodeID LIKE ?)  LIMIT ? OFFSET ?";
    assertEquals("Non matching generated query", expected, resultQuery);
    int paramCount = result.getParamList().size();
    assertEquals("Non matching parameter count", 7, paramCount);
  }


  @Test
  public void testBuildQueryUsingDescriptionWithMatch() {
    TopicAction.TopicSearch search = new TopicAction.TopicSearch();
    int size = 10;
    int offset = 0;
    TopicAction.TopicSearchRequest searchRequest = new TopicAction.TopicSearchRequest(SearchAction.Operator.OR, search,offset, size);
    searchRequest.setDescription("fast food", SearchAction.Match.SUFFIX);

    String appId = "i1s3wtx3m0d";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    QueryBuilderResult result = builder.buildPaginationQuery(searchRequest, appId, PaginationInfo.build(10, 0),null);
    assertNotNull(result);

    String resultQuery = result.getQuery();
    String expected = "SELECT DISTINCT ofPubsubNode.* , (SELECT count(1) FROM ofPubsubSubscription s where s.serviceID " +
        "= ofPubsubNode.serviceId AND s.nodeID = ofPubsubNode.nodeID GROUP by s.nodeID,s.serviceId ) as 'subcount' FROM " +
        "ofPubsubNode WHERE (UPPER(ofPubsubNode.description) LIKE ? ) AND (ofPubsubNode.nodeID LIKE ? " +
        "OR ofPubsubNode.nodeID LIKE ?)  LIMIT ? OFFSET ?";

    assertEquals("Non matching generated query", expected, resultQuery);
    int paramCount = result.getParamList().size();
    assertEquals("Non matching parameter count", 5, paramCount);
  }

  @Test
  public void testBuildQueryUsingDisplayNameWithPrefixMatch() {
    TopicAction.TopicSearch search = new TopicAction.TopicSearch();
    int size = 10;
    int offset = 0;
    TopicAction.TopicSearchRequest searchRequest = new TopicAction.TopicSearchRequest(SearchAction.Operator.OR, search,offset, size);
    searchRequest.setTopicName("food", SearchAction.Match.PREFIX);

    String appId = "i1s3wtx3m0d";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    QueryBuilderResult result = builder.buildPaginationQuery(searchRequest, appId, PaginationInfo.build(10, 0), null);
    assertNotNull(result);

    String resultQuery = result.getQuery();
    String expected = "SELECT DISTINCT ofPubsubNode.* , (SELECT count(1) FROM ofPubsubSubscription s where s.serviceID " +
        "= ofPubsubNode.serviceId AND s.nodeID = ofPubsubNode.nodeID GROUP by s.nodeID,s.serviceId ) as 'subcount' FROM " +
        "ofPubsubNode WHERE (UPPER(ofPubsubNode.name) LIKE ? ) AND (ofPubsubNode.nodeID LIKE ? " +
        "OR ofPubsubNode.nodeID LIKE ?)  LIMIT ? OFFSET ?";

    assertEquals("Non matching generated query", expected, resultQuery);
    int paramCount = result.getParamList().size();
    assertEquals("Non matching parameter count", 5, paramCount);

    boolean foundPrefix = false;
    Iterator<QueryParam> paramIterator = result.getParamList().iterator();
    while (paramIterator.hasNext() && !foundPrefix) {
      QueryParam param = paramIterator.next();
      Object value = param.getValue();
      if (value instanceof String) {
        String strValue = (String) value;
        if (strValue.equals("FOOD%")) {
          foundPrefix = true;
        }
      }
    }
    assertTrue("Didn't find parameter with wildcard", foundPrefix);
  }


  @Test
  public void testBuildQueryUsingDisplayNameWithSuffixMatch() {
    TopicAction.TopicSearch search = new TopicAction.TopicSearch();
    int size = 10;
    int offset = 0;
    TopicAction.TopicSearchRequest searchRequest = new TopicAction.TopicSearchRequest(SearchAction.Operator.OR, search,offset, size);
    searchRequest.setTopicName("food", SearchAction.Match.SUFFIX);

    String appId = "i1s3wtx3m0d";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    QueryBuilderResult result = builder.buildPaginationQuery(searchRequest, appId, PaginationInfo.build(10, 0), null);
    assertNotNull(result);

    String resultQuery = result.getQuery();
    String expected = "SELECT DISTINCT ofPubsubNode.* , (SELECT count(1) FROM ofPubsubSubscription s where s.serviceID " +
        "= ofPubsubNode.serviceId AND s.nodeID = ofPubsubNode.nodeID GROUP by s.nodeID,s.serviceId ) as 'subcount' FROM " +
        "ofPubsubNode WHERE (UPPER(ofPubsubNode.name) LIKE ? ) AND (ofPubsubNode.nodeID LIKE ? " +
        "OR ofPubsubNode.nodeID LIKE ?)  LIMIT ? OFFSET ?";

    assertEquals("Non matching generated query", expected, resultQuery);
    int paramCount = result.getParamList().size();
    assertEquals("Non matching parameter count", 5, paramCount);

    boolean foundPrefix = false;
    Iterator<QueryParam> paramIterator = result.getParamList().iterator();
    while (paramIterator.hasNext() && !foundPrefix) {
      QueryParam param = paramIterator.next();
      Object value = param.getValue();
      if (value instanceof String) {
        String strValue = (String) value;
        if (strValue.equals("%FOOD")) {
          foundPrefix = true;
        }
      }
    }
    assertTrue("Didn't find parameter with wildcard", foundPrefix);
  }

  @Test
  public void testBuildQueryUsingDisplayNameWithExactMatch() {
    TopicAction.TopicSearch search = new TopicAction.TopicSearch();
    int size = 10;
    int offset = 0;
    TopicAction.TopicSearchRequest searchRequest = new TopicAction.TopicSearchRequest(SearchAction.Operator.OR, search,offset, size);
    searchRequest.setTopicName("food", SearchAction.Match.EXACT);

    String appId = "i1s3wtx3m0d";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    QueryBuilderResult result = builder.buildPaginationQuery(searchRequest, appId, PaginationInfo.build(10, 0), null);
    assertNotNull(result);

    String resultQuery = result.getQuery();
    String expected = "SELECT DISTINCT ofPubsubNode.* , (SELECT count(1) FROM ofPubsubSubscription s where s.serviceID " +
        "= ofPubsubNode.serviceId AND s.nodeID = ofPubsubNode.nodeID GROUP by s.nodeID,s.serviceId ) as 'subcount' FROM " +
        "ofPubsubNode WHERE (ofPubsubNode.name=? ) AND (ofPubsubNode.nodeID LIKE ? " +
        "OR ofPubsubNode.nodeID LIKE ?)  LIMIT ? OFFSET ?";

    assertEquals("Non matching generated query", expected, resultQuery);
    int paramCount = result.getParamList().size();
    assertEquals("Non matching parameter count", 5, paramCount);

    boolean foundPrefix = false;
    Iterator<QueryParam> paramIterator = result.getParamList().iterator();
    while (paramIterator.hasNext() && !foundPrefix) {
      QueryParam param = paramIterator.next();
      Object value = param.getValue();
      if (value instanceof String) {
        String strValue = (String) value;
        if (strValue.equals("food")) {
          foundPrefix = true;
        }
      }
    }
    assertTrue("Didn't find parameter with wildcard", foundPrefix);
  }

  @Test
  public void testBuildQueryUsingDisplayNameWithNullMatch() {
    TopicAction.TopicSearch search = new TopicAction.TopicSearch();
    int size = 10;
    int offset = 0;
    TopicAction.TopicSearchRequest searchRequest = new TopicAction.TopicSearchRequest(SearchAction.Operator.OR, search,offset, size);
    searchRequest.setTopicName("food", null);

    String appId = "i1s3wtx3m0d";

    TopicQueryBuilder builder = new TopicQueryBuilder();
    QueryBuilderResult result = builder.buildPaginationQuery(searchRequest, appId, PaginationInfo.build(10, 0), null);
    assertNotNull(result);

    String resultQuery = result.getQuery();
    String expected = "SELECT DISTINCT ofPubsubNode.* , (SELECT count(1) FROM ofPubsubSubscription s where s.serviceID " +
        "= ofPubsubNode.serviceId AND s.nodeID = ofPubsubNode.nodeID GROUP by s.nodeID,s.serviceId ) as 'subcount' FROM " +
        "ofPubsubNode WHERE (UPPER(ofPubsubNode.name) LIKE ? ) AND (ofPubsubNode.nodeID LIKE ? " +
        "OR ofPubsubNode.nodeID LIKE ?)  LIMIT ? OFFSET ?";

    assertEquals("Non matching generated query", expected, resultQuery);
    int paramCount = result.getParamList().size();
    assertEquals("Non matching parameter count", 5, paramCount);

    boolean foundPrefix = false;
    Iterator<QueryParam> paramIterator = result.getParamList().iterator();
    while (paramIterator.hasNext() && !foundPrefix) {
      QueryParam param = paramIterator.next();
      Object value = param.getValue();
      if (value instanceof String) {
        String strValue = (String) value;
        if (strValue.equals("FOOD%")) {
          foundPrefix = true;
        }
      }
    }
    assertTrue("Didn't find parameter with wildcard", foundPrefix);
  }
}

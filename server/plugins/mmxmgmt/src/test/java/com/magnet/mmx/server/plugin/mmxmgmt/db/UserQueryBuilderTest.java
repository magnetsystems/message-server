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

import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.UserQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 */
public class UserQueryBuilderTest {

  @Test
  public void testBuildQueryWithTags() throws Exception {
    UserQuery query = new UserQuery();
    String[] tags = {"student","elementary"};
    query.setTags(Arrays.asList(tags));

    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(query, "app2");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT ofUser.* FROM ofUser,mmxTag WHERE (( ( mmxTag.tagname = ? OR mmxTag.tagname = ? )" +
        " AND mmxTag.username=ofUser.username )) AND (ofUser.username LIKE ?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 3;
    assertEquals("Non matching param count", expectedSize, size);
  }

  @Test
  public void testBuildQueryWithUserName() throws Exception {
    UserQuery query = new UserQuery();
    query.setDisplayName("login3.wacky");

    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(query, "app2");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT ofUser.* FROM ofUser WHERE (ofUser.name=? ) AND (ofUser.username LIKE ?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 2;
    assertEquals("Non matching param count", expectedSize, size);
  }

  @Test
  public void testBuildQueryWithDateCreated() throws Exception {
    UserQuery query = new UserQuery();
    query.setRegistrationDate(new DateRange(1410302195, 1410303000));

    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(query, "app2");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT ofUser.* FROM ofUser WHERE (ofUser.creationDate>=? AND ofUser.creationDate<?) AND (ofUser.username LIKE ?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 3;
    assertEquals("Non matching param count", expectedSize, size);
  }


  @Test
  public void testBuildQueryWithDateCreatedWithOnlyStart() throws Exception {
    UserQuery query = new UserQuery();
    query.setRegistrationDate(new DateRange(1410302195, null));

    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(query, "app2");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT ofUser.* FROM ofUser WHERE (ofUser.creationDate>=?) AND (ofUser.username LIKE ?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 2;
    assertEquals("Non matching param count", expectedSize, size);
  }

  @Test
  public void testBuildQueryWithEmailAndMatch() throws Exception {
    com.magnet.mmx.protocol.UserQuery.Search search = new com.magnet.mmx.protocol.UserQuery.Search();
    com.magnet.mmx.protocol.UserQuery.SearchRequest searchRequest = new com.magnet.mmx.protocol.UserQuery.SearchRequest(SearchAction.Operator.OR, search, 0, 10);
    searchRequest.setEmail("firstname3.lastname3@magnet.com", SearchAction.Match.EXACT);

    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(searchRequest, "app2", PaginationInfo.build(10,0));
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT ofUser.* FROM ofUser WHERE (ofUser.email=? ) AND (ofUser.username LIKE ?)  LIMIT ? OFFSET ?";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 4;
    assertEquals("Non matching param count", expectedSize, size);
  }

  @Test
  public void testBuildQueryWithEmailAndPrefixMatch() throws Exception {
    com.magnet.mmx.protocol.UserQuery.Search search = new com.magnet.mmx.protocol.UserQuery.Search();
    com.magnet.mmx.protocol.UserQuery.SearchRequest searchRequest = new com.magnet.mmx.protocol.UserQuery.SearchRequest(SearchAction.Operator.OR, search, 0, 10);
    searchRequest.setEmail("firstname3.lastname3", SearchAction.Match.PREFIX);

    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(searchRequest, "app2", PaginationInfo.build(10,0));
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT ofUser.* FROM ofUser WHERE (UPPER(ofUser.email) LIKE ? ) AND (ofUser.username LIKE ?)  LIMIT ? OFFSET ?";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 4;
    assertEquals("Non matching param count", expectedSize, size);
    boolean foundPrefix = false;
    Iterator<QueryParam> paramIterator = paramList.iterator();
    while (paramIterator.hasNext() && !foundPrefix) {
      QueryParam param = paramIterator.next();
      Object value = param.getValue();
      if (value instanceof String) {
        String strValue = (String) value;
        if (strValue.endsWith("%")) {
          foundPrefix = true;
        }
      }
    }
    assertTrue("Didn't find parameter with wildcard", foundPrefix);

  }

  @Test
  public void testBuildQueryWithEmailAndSuffixMatch() throws Exception {
    com.magnet.mmx.protocol.UserQuery.Search search = new com.magnet.mmx.protocol.UserQuery.Search();
    com.magnet.mmx.protocol.UserQuery.SearchRequest searchRequest = new com.magnet.mmx.protocol.UserQuery.SearchRequest(SearchAction.Operator.OR, search, 0, 10);
    searchRequest.setEmail("firstname3.lastname3", SearchAction.Match.SUFFIX);

    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(searchRequest, "app2", PaginationInfo.build(10,0));
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT ofUser.* FROM ofUser WHERE (UPPER(ofUser.email) LIKE ? ) AND (ofUser.username LIKE ?)  LIMIT ? OFFSET ?";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 4;
    assertEquals("Non matching param count", expectedSize, size);
    boolean found = false;
    Iterator<QueryParam> paramIterator = paramList.iterator();
    while (paramIterator.hasNext() && !found) {
      QueryParam param = paramIterator.next();
      Object value = param.getValue();
      if (value instanceof String) {
        String strValue = (String) value;
        if (strValue.startsWith("%")) {
          found = true;
        }
      }
    }
    assertTrue("Didn't find parameter with wildcard", found);
  }

}

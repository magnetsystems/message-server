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
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.Operator;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.UserQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.user.UserSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.user.UserSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
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
import static org.junit.Assert.assertTrue;

/**
 */
public class UserDAOImplSearchTest {
  private static BasicDataSource ds;

  @BeforeClass
  public static void setup() throws Exception {
    ds = UnitTestDSProvider.getDataSource();
    DBTestUtil.cleanTables(new String[] {"mmxTag"}, new BasicDataSourceConnectionProvider(ds));

    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/user-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/device-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/user-tags-1.xml");
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
  public void testSearch1() {
    UserDAO messageDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "i0sq7ddvi17";
    UserSearchOption option = UserSearchOption.NAME;

    int chunk = 5;
    int offset = 0;
    ValueHolder holder = new ValueHolder();
    holder.setValue1("john");

    UserSearchResult results = messageDAO.searchUsers(appId, option, holder, UserSortOption.defaultSortOption(), null);
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 1;
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  @Test
  public void testSearch2() {
    UserDAO messageDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "app1";
    UserSearchOption option = UserSearchOption.EMAIL;

    int chunk = 5;
    int offset = 0;
    ValueHolder holder = new ValueHolder();
    holder.setValue1("newuser");

    UserSearchResult results = messageDAO.searchUsers(appId, option, holder, UserSortOption.defaultSortOption(), null);
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 1;
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  @Test
  public void testSearch3() {
    UserDAO messageDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "AAABSNIBKOstQST7";
    UserSearchOption option = UserSearchOption.PHONE;

    int chunk = 5;
    int offset = 0;
    ValueHolder holder = new ValueHolder();
    //4083084001
    holder.setValue1("84001");

    UserSearchResult results = messageDAO.searchUsers(appId, option, holder, UserSortOption.defaultSortOption(), null);
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 1;
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);

    List<UserEntity> list = results.getResults();

    UserEntity entity = list.get(0);
    assertNotNull(entity);
  }

  @Test
  public void testSearch4() {
    UserDAO messageDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "i0sq7ddvi17";
    UserSearchOption option = UserSearchOption.USERNAME;

    int chunk = 5;
    int offset = 0;
    ValueHolder holder = new ValueHolder();
    //4083084001
    holder.setValue1("rpp");

    UserSearchResult results = messageDAO.searchUsers(appId, option, holder, UserSortOption.defaultSortOption(), null);
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 3;
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);

    List<UserEntity> list = results.getResults();

    UserEntity entity = list.get(0);
    assertNotNull(entity);
  }


  @Test
  public void testSearchUsingTags() {
    UserDAO userDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    UserQuery userQuery = new UserQuery();
    userQuery.setTags(Arrays.asList("student"));
    String appId = "AAABSNIBKOstQST7";
    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult query = builder.buildQuery(userQuery, appId);
    List<UserEntity> userList = userDAO.getUsers(query);
    assertNotNull(userList);
    int size = userList.size();
    assertEquals("Non matching user list size", 2, size);
  }

  @Test
  public void testSearchUsingNameOrTags() {
    UserDAO userDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    UserQuery userQuery = new UserQuery();
    userQuery.setOperator(Operator.OR);
    userQuery.setDisplayName("Blah2-148d334586a%i0sq7ddvi17");
    userQuery.setTags(Arrays.asList("student", "admin"));
    String appId = "i0sq7ddvi17";
    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult query = builder.buildQuery(userQuery, appId);
    List<UserEntity> userList = userDAO.getUsers(query);
    assertNotNull(userList);
    int size = userList.size();
    assertEquals("Non matching user list size", 1, size);
  }

  @Test
  public void testSearchUsingEmail() {
    UserDAO userDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    UserQuery userQuery = new UserQuery();
    userQuery.setEmail("m@magnet.com");
    userQuery.setOperator(Operator.OR);
    userQuery.setDisplayName("Blah2-148d334586a%i0sq7ddvi17");
    String appId = "i0sq7ddvi17";
    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult query = builder.buildQuery(userQuery, appId);
    List<UserEntity> userList = userDAO.getUsers(query);
    assertNotNull(userList);
    int size = userList.size();
    assertEquals("Non matching user list size", 2, size);
  }


  @Test
  public void testSearchUsingEmailWithPagination() {
    UserDAO userDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    UserQuery userQuery = new UserQuery();
    userQuery.setEmail("m@magnet.com");
    userQuery.setOperator(Operator.OR);
    userQuery.setDisplayName("Blah2-148d334586a%i0sq7ddvi17");
    String appId = "i0sq7ddvi17";
    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult query = builder.buildQuery(userQuery, appId);
    int size = 10;
    SearchResult<UserEntity> userList = userDAO.getUsersWithPagination(query, PaginationInfo.build(size, 0));
    assertNotNull(userList);
    int rsize = userList.getResults().size();
    assertEquals("Non matching user list size", 2, rsize);
  }


  @Test
  public void testSearchUsingEmailWithSuffixMatch() {
    com.magnet.mmx.protocol.UserQuery.Search search = new com.magnet.mmx.protocol.UserQuery.Search();
    com.magnet.mmx.protocol.UserQuery.SearchRequest searchRequest = new com.magnet.mmx.protocol.UserQuery.SearchRequest(SearchAction.Operator.OR, search, 0, 10);
    searchRequest.setEmail("net.com", SearchAction.Match.SUFFIX);

    UserDAO userDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "i0sq7ddvi17";
    PaginationInfo pinfo = PaginationInfo.build(10, 0);
    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult query = builder.buildQuery(searchRequest, appId, pinfo);
    List<UserEntity> userList = userDAO.getUsers(query);
    assertNotNull(userList);
    int size = userList.size();
    assertEquals("Non matching user list size", 5, size);
  }

  @Test
  public void testSearchUsingDisplayName() {
    com.magnet.mmx.protocol.UserQuery.Search search = new com.magnet.mmx.protocol.UserQuery.Search();
    com.magnet.mmx.protocol.UserQuery.SearchRequest searchRequest = new com.magnet.mmx.protocol.UserQuery.SearchRequest(SearchAction.Operator.OR, search, 0, 10);
    searchRequest.setDisplayName("login3");

    UserDAO userDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "i0sq7ddvi17";
    PaginationInfo pinfo = PaginationInfo.build(10, 0);
    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult query = builder.buildQuery(searchRequest, appId, pinfo);
    SearchResult<UserEntity> userList = userDAO.getUsersWithPagination(query, pinfo);
    assertNotNull(userList);
    int size = userList.getResults().size();
    assertEquals("Non matching user list size", 1, size);
  }

  @Test
  public void testSearchUsingEmailWithPrefixMatch() {
    com.magnet.mmx.protocol.UserQuery.Search search = new com.magnet.mmx.protocol.UserQuery.Search();
    com.magnet.mmx.protocol.UserQuery.SearchRequest searchRequest = new com.magnet.mmx.protocol.UserQuery.SearchRequest(SearchAction.Operator.OR, search, 0, 10);
    searchRequest.setEmail("login3", SearchAction.Match.PREFIX);

    UserDAO userDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "i0sq7ddvi17";
    PaginationInfo pinfo = PaginationInfo.build(10, 0);
    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult query = builder.buildQuery(searchRequest, appId, pinfo);
    List<UserEntity> userList = userDAO.getUsers(query);
    assertNotNull(userList);
    int size = userList.size();
    assertEquals("Non matching user list size", 1, size);
  }

  @Test
  public void testSearchUsingEmailWithPrefixMatchAndTags() {
    com.magnet.mmx.protocol.UserQuery.Search search = new com.magnet.mmx.protocol.UserQuery.Search();
    com.magnet.mmx.protocol.UserQuery.SearchRequest searchRequest = new com.magnet.mmx.protocol.UserQuery.SearchRequest(SearchAction.Operator.OR, search, 0, 10);
    searchRequest.setEmail("login3", SearchAction.Match.PREFIX);
    searchRequest.setTags(Arrays.asList("student", "admin"));

    UserDAO userDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "i0sq7ddvi17";
    PaginationInfo pinfo = PaginationInfo.build(10, 0);
    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult query = builder.buildQuery(searchRequest, appId, pinfo);
    SearchResult<UserEntity> userList = userDAO.getUsersWithPagination(query, pinfo);
    assertNotNull(userList);
    int size = userList.getResults().size();
    assertEquals("Non matching user list size", 2, size);
  }


  @Test
  public void testSearchUsingPhoneWithPrefixMatch() {
    com.magnet.mmx.protocol.UserQuery.Search search = new com.magnet.mmx.protocol.UserQuery.Search();
    com.magnet.mmx.protocol.UserQuery.SearchRequest searchRequest = new com.magnet.mmx.protocol.UserQuery.SearchRequest(SearchAction.Operator.OR, search, 0, 10);
    searchRequest.setPhone("4086366678");

    UserDAO userDAO = new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "i0sq7ddvi17";
    PaginationInfo pinfo = PaginationInfo.build(10, 0);
    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult query = builder.buildQuery(searchRequest, appId, pinfo);
    SearchResult<UserEntity> userList = userDAO.getUsersWithPagination(query, pinfo);
    assertNotNull(userList);
    int size = userList.getResults().size();
    assertEquals("Non matching user list size", 0, size);
  }
}

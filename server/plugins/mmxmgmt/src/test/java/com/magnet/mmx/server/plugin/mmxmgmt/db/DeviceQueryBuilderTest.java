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

import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DeviceQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 */
public class DeviceQueryBuilderTest {


  @Test
  public void testBuildQuery() throws Exception {
    DeviceQuery query = new DeviceQuery();
    query.setOsType(OSType.ANDROID);

    DeviceQueryBuilder builder = new DeviceQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(query, "app1");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT mmxDevice.* FROM mmxDevice WHERE (mmxDevice.osType=?) AND (mmxDevice.appId=?) AND (mmxDevice.status=?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 3;
    assertEquals("Non matching param count", expectedSize, size);
  }


  @Test
  public void testBuildQueryWithOSTypeAndDateCreated() throws Exception {
    DeviceQuery query = new DeviceQuery();
    query.setOsType(OSType.ANDROID);
    query.setRegistrationDate(new DateRange(1410302195, 1410303000));

    DeviceQueryBuilder builder = new DeviceQueryBuilder(false, false);
    QueryBuilderResult result = builder.buildQuery(query, "app2");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT mmxDevice.* FROM mmxDevice WHERE (mmxDevice.osType=? OR " +
        "(unix_timestamp(mmxDevice.dateCreated)>=? AND unix_timestamp(mmxDevice.dateCreated)<?)) AND (mmxDevice.appId=?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 4;
    assertEquals("Non matching param count", expectedSize, size);
  }


  @Test
  public void testBuildQueryWithTags() throws Exception {
    DeviceQuery query = new DeviceQuery();
    String[] tags = {"home", "security", "old"};
    query.setTags(Arrays.asList(tags));

    DeviceQueryBuilder builder = new DeviceQueryBuilder(true, false);
    QueryBuilderResult result = builder.buildQuery(query, "app2");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT mmxDevice.* FROM mmxDevice,mmxTag WHERE (( ( mmxTag.tagname = ? OR mmxTag.tagname = ? " +
        "OR mmxTag.tagname = ? ) AND mmxTag.deviceId=mmxDevice.id )) AND (mmxDevice.appId=?) AND (mmxDevice.status=?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 5;
    assertEquals("Non matching param count", expectedSize, size);
  }

  @Test
  public void testBuildQueryWithTagsActiveOnly() throws Exception {
    DeviceQuery query = new DeviceQuery();
    String[] tags = {"home", "security", "old"};
    query.setTags(Arrays.asList(tags));

    DeviceQueryBuilder builder = new DeviceQueryBuilder(false, false);
    QueryBuilderResult result = builder.buildQuery(query, "app2");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT mmxDevice.* FROM mmxDevice,mmxTag WHERE (( ( mmxTag.tagname = ? OR mmxTag.tagname = ? " +
        "OR mmxTag.tagname = ? ) AND mmxTag.deviceId=mmxDevice.id )) AND (mmxDevice.appId=?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 4;
    assertEquals("Non matching param count", expectedSize, size);
  }


  @Test
  public void testBuildWithBadQueryDefinition() {
    DeviceQuery query = new DeviceQuery();

    DeviceQueryBuilder builder = new DeviceQueryBuilder();
    boolean gotException = false;
    try {
      builder.buildQuery(query, "app2");
    } catch (ResolutionException e) {
      gotException = true;
    }
    assertTrue("Didn't get the exception", gotException);
  }

  @Test
  public void testBuildQueryWithModelInfo() throws Exception {
    DeviceQuery query = new DeviceQuery();
    query.setModelInfo("samsung_galaxy");

    DeviceQueryBuilder builder = new DeviceQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(query, "app1");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT mmxDevice.* FROM mmxDevice WHERE (mmxDevice.modelInfo LIKE ?) AND (mmxDevice.appId=?) AND (mmxDevice.status=?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 3;
    assertEquals("Non matching param count", expectedSize, size);
  }

  @Test
  public void testBuildQueryWithPhoneNumber() throws Exception {
    DeviceQuery query = new DeviceQuery();
    query.setPhoneNumber("4082345678");

    DeviceQueryBuilder builder = new DeviceQueryBuilder();
    QueryBuilderResult result = builder.buildQuery(query, "app1");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT mmxDevice.* FROM mmxDevice WHERE (mmxDevice.phoneNumber=?) AND (mmxDevice.appId=?) AND (mmxDevice.status=?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 3;
    assertEquals("Non matching param count", expectedSize, size);
  }

  @Test
  public void testBuildQueryWithTagsAndPaginationAndSortInfo() throws Exception {
    DeviceQuery query = new DeviceQuery();
    String[] tags = {"home", "security", "old"};
    query.setTags(Arrays.asList(tags));

    DeviceQueryBuilder builder = new DeviceQueryBuilder(false, false );
    PaginationInfo paginationInfo = PaginationInfo.build(10,0);
    SortInfo sortInfo = SortInfo.build(DeviceQueryBuilder.DeviceSortBy.DEVICE_ID.name(), SortOrder.ASCENDING.name());
    QueryBuilderResult result = builder.buildQuery(query, "app2", paginationInfo, sortInfo);
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT mmxDevice.* FROM mmxDevice,mmxTag WHERE (( ( mmxTag.tagname = ? OR mmxTag.tagname = ? " +
        "OR mmxTag.tagname = ? ) AND mmxTag.deviceId=mmxDevice.id )) AND (mmxDevice.appId=?) ORDER BY deviceId  LIMIT ? OFFSET ?";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 6;
    assertEquals("Non matching param count", expectedSize, size);

    String countQuery = result.getCountQuery();
    String expectedCountQuery = "SELECT COUNT(*) FROM mmxDevice,mmxTag WHERE (( ( mmxTag.tagname = ? OR mmxTag.tagname = ? OR mmxTag.tagname = ? ) AND mmxTag.deviceId=mmxDevice.id )) AND (mmxDevice.appId=?)";
    assertEquals("Got non matching count query", expectedCountQuery, countQuery);
  }

  @Test
  public void testBuildQueryWithStatus() throws Exception {
    DeviceQuery query = new DeviceQuery();
    query.setStatus(Helper.enumerateDeviceStatus("active"));
    DeviceQueryBuilder builder = new DeviceQueryBuilder(false, false);
    PaginationInfo paginationInfo = PaginationInfo.build(10,0);
    SortInfo sortInfo = SortInfo.build(DeviceQueryBuilder.DeviceSortBy.DEVICE_ID.name(), SortOrder.ASCENDING.name());
    QueryBuilderResult result = builder.buildQuery(query, "app2", paginationInfo, sortInfo);
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT mmxDevice.* FROM mmxDevice WHERE (mmxDevice.status=?) AND (mmxDevice.appId=?) ORDER BY deviceId  LIMIT ? OFFSET ?";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 4;
    assertEquals("Non matching param count", expectedSize, size);

    String countQuery = result.getCountQuery();
    String expectedCountQuery = "SELECT COUNT(*) FROM mmxDevice WHERE (mmxDevice.status=?) AND (mmxDevice.appId=?)";
    assertEquals("Got non matching count query", expectedCountQuery, countQuery);

  }

  @Test
  public void testBuildQueryWithOnlyDateRegistered() throws Exception {
    DeviceQuery query = new DeviceQuery();
    query.setOsType(OSType.ANDROID);
    query.setRegistrationDate(new DateRange(null, 1410303000));

    DeviceQueryBuilder builder = new DeviceQueryBuilder(false, false);
    QueryBuilderResult result = builder.buildQuery(query, "app2");
    assertNotNull("Got null query builder result", result);

    String sqlQuery = result.getQuery();
    assertNotNull("Got null sql query string", sqlQuery);
    String expected = "SELECT DISTINCT mmxDevice.* FROM mmxDevice WHERE (mmxDevice.osType=? OR (unix_timestamp(mmxDevice.dateCreated)<?)) AND (mmxDevice.appId=?)";
    assertEquals("Got non matching query", expected, sqlQuery);

    List<QueryParam> paramList = result.getParamList();
    assertTrue("Query param list is empty", !paramList.isEmpty());
    int size = paramList.size();
    int expectedSize = 3;
    assertEquals("Non matching param count", expectedSize, size);
  }

}

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

package com.magnet.mmx.server.plugin.mmxmgmt.handler;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Constants.UserCreateMode;
import com.magnet.mmx.protocol.MMXAttribute;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.UserCreate;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.UserQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXUserHandler.UserOperationStatusCode;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.user.User;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class MMXUserHandlerTest {
  private static BasicDataSource ds;
  private static String sAppID = "i0sq7ddvi17";
  private static JID sFrom = new JID("login3%"+sAppID+"@localhost",true);

  @BeforeClass
  public static void setup() throws Exception {
    ds = UnitTestDSProvider.getDataSource();
    DBTestUtil.cleanTables(new String[] {"mmxTag"}, new BasicDataSourceConnectionProvider(ds));
    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    builder.setColumnSensing(true);
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/app-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
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
  }
  @Test
  public void testAddingUserWithNonExistingApp() throws UnauthorizedException {
    UserCreate request = new UserCreate();
    request.setDisplayName("Tommy");
    request.setApiKey("appnotexisting");
    request.setAppId(sAppID);
    request.setUserId("tom");
    request.setEmail("tom@tommy.com");

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, "create");
    element.setText(request.toJson());

    IQ userCreateIQ = new IQ();
    userCreateIQ.setChildElement(element);
    userCreateIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleCreateUser(userCreateIQ, element.getText());

    assertNotNull(response);

    IQ.Type type = response.getType();
    assertEquals("Non matching type", IQ.Type.error, type);

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    MMXStatus status = MMXStatus.fromJson(payload);

    int code = status.getCode();
    String message = status.getMessage();

    assertEquals("Non matching message", MMXUserHandler.UserOperationStatusCode.INVALID_API_KEY.getMessage(), message);

  }

  @Test
  public void testAddingExistingUser() throws UnauthorizedException {
    UserCreate request = new UserCreate();
    request.setDisplayName("Tommy");
    request.setApiKey("3171f18a-9fcc-4e84-8cb9-aad6ea7bf017");
    request.setAppId(sAppID);
    request.setUserId("tom");
    request.setEmail("tom@tommy.com");
    request.setPriKey("guestsecretcode");

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, "create");
    element.setText(request.toJson());

    IQ userCreateIQ = new IQ();
    userCreateIQ.setChildElement(element);
    userCreateIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleCreateUser(userCreateIQ, element.getText());

    assertNotNull(response);
    assertNotNull(response);

    IQ.Type type = response.getType();
    assertEquals("Non matching type", IQ.Type.error, type);

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    MMXStatus status = MMXStatus.fromJson(payload);

    String message = status.getMessage();

    assertEquals("Non matching message", MMXUserHandler.UserOperationStatusCode.INVALID_USER_ID_TAKEN.getMessage(), message);
  }

  @Test
  public void testAddingUserWithVeryLongUserId() throws UnauthorizedException {
    UserCreate request = new UserCreate();
    request.setDisplayName("Tommy");
    request.setApiKey("3171f18a-9fcc-4e84-8cb9-aad6ea7bf017");
    request.setAppId(sAppID);
    request.setUserId("SuperLongAndVeryAmazingIsThisUsersUserNameButIWantItLongSoThatIt");
    request.setEmail("tom@tommy.com");
    request.setPriKey("guestsecretcode");

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, "create");
    element.setText(request.toJson());

    IQ userCreateIQ = new IQ();
    userCreateIQ.setChildElement(element);
    userCreateIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleCreateUser(userCreateIQ, element.getText());

    assertNotNull(response);
    assertNotNull(response);

    IQ.Type type = response.getType();
    assertEquals("Non matching type", IQ.Type.error, type);

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    MMXStatus status = MMXStatus.fromJson(payload);

    String message = status.getMessage();

    assertEquals("Non matching message", MMXUserHandler.UserOperationStatusCode.INVALID_USER_ID_TOO_LONG.getMessage(), message);
  }

  /**
   * Running this test causes Openfire to spit out a message about home directory not
   * being configured. Hence disabling it for now.
   * @throws UnauthorizedException
   */
  @Ignore
  @Test
  public void testCreatingAValidUser() throws UnauthorizedException {
    UserCreate request = new UserCreate();
    request.setDisplayName("Tommy");
    request.setApiKey("app1");
    request.setAppId(sAppID);
    request.setUserId("login3");
    request.setEmail("login3@mmx.com");

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, "create");
    element.setText(request.toJson());

    IQ userCreateIQ = new IQ();
    userCreateIQ.setChildElement(element);
    userCreateIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleCreateUser(userCreateIQ, element.getText());

    assertNotNull(response);
    assertNotNull(response);

    IQ.Type type = response.getType();
    assertEquals("Non matching type", IQ.Type.result, type);

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    MMXStatus status = MMXStatus.fromJson(payload);

    String message = status.getMessage();

    assertEquals("Non matching message", MMXUserHandler.UserOperationStatusCode.USER_CREATED.getMessage(), message);
  }

  @Ignore
  //@Test
  public void testDeletingAValidUser() throws UnauthorizedException {
    UserCreate request = new UserCreate();
    request.setDisplayName("Tommy");
    request.setApiKey("app1");
    request.setAppId(sAppID);
    request.setUserId("login3");
    request.setEmail("login3@mmx.com");

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, Constants.UserCommand.delete.name());
    element.setText(request.toJson());

    IQ userCreateIQ = new IQ();
    JID from = new JID("login3%i0sqlunda4q@localhost",true);
    userCreateIQ.setFrom(from);
    userCreateIQ.setChildElement(element);
    userCreateIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleDeleteUser(userCreateIQ, from, "i0sqlunda4q", element.getText());

    assertNotNull(response);
    assertNotNull(response);

    IQ.Type type = response.getType();
    assertEquals("Non matching type", IQ.Type.result, type);

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    MMXStatus status = MMXStatus.fromJson(payload);

    String message = status.getMessage();

    assertEquals("Non matching message", MMXUserHandler.UserOperationStatusCode.USER_DELETED.getMessage(), message);
  }
  @Test
  public void testAddingAnonymousUser() throws UnauthorizedException {
    UserCreate request = new UserCreate();
    request.setDisplayName("AnonUser1");
    request.setApiKey("3171f18a-9fcc-4e84-8cb9-aad6ea7bf017");
    request.setAppId(sAppID);
    request.setUserId("deviceid-1");
    request.setEmail("unknown");
    request.setCreateMode(UserCreateMode.GUEST);
    request.setPriKey("guestsecretcode");

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, "create");
    element.setText(request.toJson());

    IQ userCreateIQ = new IQ();
    userCreateIQ.setChildElement(element);
    userCreateIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleCreateUser(userCreateIQ, element.getText());

    assertNotNull(response);

    IQ.Type type = response.getType();

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    MMXStatus status = MMXStatus.fromJson(payload);

    int code = status.getCode();
    String message = status.getMessage();

    assertTrue(message.length() > 0);
    assertEquals(UserOperationStatusCode.USER_CREATED.getCode(), code);
  }
  @Test
  public void testUpgradeToRealUser() throws UnauthorizedException {
    UserCreate request = new UserCreate();
    request.setDisplayName("RealUser1");
    request.setApiKey("3171f18a-9fcc-4e84-8cb9-aad6ea7bf017");
    request.setAppId(sAppID);
    request.setUserId("RealUser1");
    request.setEmail("RealUser1@yahoo.com");
    request.setCreateMode(UserCreateMode.UPGRADE_USER);
    request.setPriKey("guestsecretcode");

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, "create");
    element.setText(request.toJson());

    IQ userCreateIQ = new IQ();
    JID from = new JID("login3%i0sqlunda4q@localhost",true);
    userCreateIQ.setFrom(from);
    userCreateIQ.setChildElement(element);
    userCreateIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleCreateUser(userCreateIQ, element.getText());

    assertNotNull(response);

    IQ.Type type = response.getType();

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    MMXStatus status = MMXStatus.fromJson(payload);

    int code = status.getCode();
    String message = status.getMessage();

    assertTrue(message.length() > 0);
    assertEquals(UserOperationStatusCode.USER_CREATED.getCode(), code);
  }
  @Test
  public void testQueryUsers1() throws UnauthorizedException {

    List<MMXAttribute<UserQuery.Type>> clist = new ArrayList<MMXAttribute<UserQuery.Type>>(4);
    {
      MMXAttribute<UserQuery.Type> c = new MMXAttribute<UserQuery.Type>(UserQuery.Type.displayName, "login3");
      clist.add(c);
    }
    UserQuery.BulkSearchRequest query = new UserQuery.BulkSearchRequest(clist, null);

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, Constants.UserCommand.query.name());
    element.setText(query.toJson());

    IQ queryIQ = new IQ();
    queryIQ.setFrom(sFrom);
    queryIQ.setChildElement(element);
    queryIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleQueryUser(queryIQ, sFrom, sAppID, element.getText());

    assertNotNull(response);

    IQ.Type type = response.getType();
    assertEquals("Non matching type", IQ.Type.result, type);

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    UserQuery.Response responseList = UserQuery.Response.fromJson(payload);

    List<UserInfo> userList = responseList.getUsers();

    assertTrue("User list is empty", !userList.isEmpty());

  }

  @Test
  public void testQueryUsers2() throws UnauthorizedException {

    List<MMXAttribute<UserQuery.Type>> clist = new ArrayList<MMXAttribute<UserQuery.Type>>(4);
    {
      MMXAttribute<UserQuery.Type> c = new MMXAttribute<UserQuery.Type>(
          UserQuery.Type.email, "net.com");
      clist.add(c);
    }
    UserQuery.BulkSearchRequest query = new UserQuery.BulkSearchRequest(
        clist, Integer.valueOf(3));

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, Constants.UserCommand.query.name());
    element.setText(query.toJson());

    IQ queryIQ = new IQ();
    queryIQ.setFrom(sFrom);
    queryIQ.setChildElement(element);
    queryIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleQueryUser(queryIQ, sFrom, sAppID, element.getText());

    assertNotNull(response);

    IQ.Type type = response.getType();
    assertEquals("Non matching type", IQ.Type.result, type);

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    UserQuery.Response responseList = UserQuery.Response.fromJson(payload);

    List<UserInfo> userList = responseList.getUsers();

    assertTrue("User list is empty", !userList.isEmpty());

    int size = userList.size();
    assertEquals("Didn't get matching user list", 3, size);

    int total = responseList.getTotalCount();
    assertEquals("Didn't get matching user list total size", 5, total);
  }

  @Test
  public void testQueryUsers3() throws UnauthorizedException {

    List<MMXAttribute<UserQuery.Type>> clist = new ArrayList<MMXAttribute<UserQuery.Type>>(4);
    {
      MMXAttribute<UserQuery.Type> c = new MMXAttribute<UserQuery.Type>(
          UserQuery.Type.email, "net.com");
      clist.add(c);
    }
    UserQuery.BulkSearchRequest query = new UserQuery.BulkSearchRequest(
        clist, Integer.valueOf(10));

    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX, Constants.MMX_NS_USER);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, Constants.UserCommand.query.name());
    element.setText(query.toJson());

    IQ queryIQ = new IQ();
    queryIQ.setFrom(sFrom);
    queryIQ.setChildElement(element);
    queryIQ.setType(IQ.Type.set);

    MMXUserHandler userHandler = new StubMMXUserHandler("stubbed mmx user handler");
    IQ response = userHandler.handleQueryUser(queryIQ, sFrom, sAppID, element.getText());

    assertNotNull(response);

    IQ.Type type = response.getType();
    assertEquals("Non matching type", IQ.Type.result, type);

    String payload = response.getChildElement().getStringValue();
    assertNotNull("Null payload", payload);

    UserQuery.Response responseList = UserQuery.Response.fromJson(payload);

    List<UserInfo> userList = responseList.getUsers();

    assertTrue("User list is empty", !userList.isEmpty());

    int size = userList.size();
    assertEquals("Didn't get matching user list", 5, size);

    int total = responseList.getTotalCount();
    assertEquals("Didn't get matching user list total size", 5, total);
  }

  private static class StubMMXUserHandler extends MMXUserHandler {
    private StubMMXUserHandler(String name) {
      super(name);
    }

    @Override
    protected MMXUserManager getUserManager() {
      return new StubMMXUserManagerImpl();
    }

    @Override
    protected ConnectionProvider getConnectionProvider() {
      return new BasicDataSourceConnectionProvider(ds);
    }
  }


  /**
   * Stubbed implementation of the MMXUserManager for testing
   */
  private static class StubMMXUserManagerImpl implements MMXUserManager {
    List<String> userList = Arrays.asList("john%i0sq7ddvi17", "tom%i0sq7ddvi17", "tom%i0sq7ddvi17");

    @Override
    public boolean isUserIdTaken(String userId) {
      return userList.contains(userId);
    }

    /**
     * This dumb implementation just returns a UserObject created with userId etc
     *
     * @param userId   userId for the user
     * @param password
     * @param name
     * @param email
     * @param metadata name value pairs for the user
     * @return
     */
    @Override
    public User createUser(String userId, String password, String name, String email, Map<String, String> metadata) {
      return new User(userId, name, email, new Date(), new Date());
    }

    @Override
    public void deleteUser(String userId) {
      System.out.println("Deleting userId");
    }

    @Override
    public void markRemoveCurrentGuestUser(String bareJid) {
      System.out.println("Marking current user removed:" + bareJid);
    }

  }
}

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

import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
import org.jivesoftware.util.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.assertEquals;

/**
 */
public class UserDAOImplTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserDAOImplTest.class);
  private static final int TOTAL_USERS = 10 ;
  private static final String USER_PREFIX = "tagtestuser";
  private static BasicDataSource ds;

  private static List<TestUserDao.UserEntity> userList = new ArrayList<TestUserDao.UserEntity>();

  @BeforeClass
  public static void setupDatabase() throws Exception {
    InputStream inputStream = DeviceDAOImplTest.class.getResourceAsStream("/test.properties");

    Properties testProperties = new Properties();
    testProperties.load(inputStream);

    String host = testProperties.getProperty("db.host");
    String port = testProperties.getProperty("db.port");
    String user = testProperties.getProperty("db.user");
    String password = testProperties.getProperty("db.password");
    String driver = testProperties.getProperty("db.driver");
    String schema = testProperties.getProperty("db.schema");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + schema;

    ds = new BasicDataSource();
    ds.setDriverClassName(driver);
    ds.setUsername(user);
    ds.setPassword(password);
    ds.setUrl(url);

    DBTestUtil.setBasicDataSource(ds);

    generateRandomUserData();
  }

  private static void generateRandomUserData() throws Exception {
    for(int i=0; i < TOTAL_USERS; i++) {
      TestUserDao.UserEntity ue = new TestUserDao.UserEntity();
      ue.setEmail(RandomStringUtils.randomAlphabetic(5) + "@" + RandomStringUtils.randomAlphabetic(8) + ".com");
      ue.setName(RandomStringUtils.randomAlphabetic(5) + " " + RandomStringUtils.randomAlphabetic(5));
      ue.setPlainPassword(RandomStringUtils.randomAlphabetic(10));
      ue.setEncryptedPassword(RandomStringUtils.randomAlphabetic(80));
      ue.setCreationDate(StringUtils.dateToMillis(new Date()));
      ue.setModificationDate(StringUtils.dateToMillis(new Date()));
      ue.setUsername(USER_PREFIX + "_" + RandomStringUtils.randomAlphabetic(10));
      userList.add(ue);
      DBTestUtil.getTestUserDao().persist(ue);
    }
  }

  @Test
  public void testGetUser() throws Exception {
    for(int i=0; i < userList.size(); i++) {
      TestUserDao.UserEntity tue = userList.get(i);
      UserEntity ue = DBTestUtil.getUserDao().getUser(tue.getUsername());
      assertEquals(ue.getEmail(), tue.getEmail());
      assertEquals(ue.getUsername(), tue.getUsername());
      assertEquals(ue.getName(), tue.getName());
      assertEquals(ue.getCreationDate().toString(), new Date(Long.parseLong(tue.getCreationDate())).toString());
      assertEquals(ue.getModificationDate().toString(), new Date(Long.parseLong(tue.getModificationDate())).toString());
    }
  }

  @AfterClass
  public static void cleanupDatabase() throws Exception {
    final String statementStr = "DELETE FROM ofUser where username like '%" + USER_PREFIX + "%'";
    Connection conn =  UnitTestDSProvider.getDataSource().getConnection();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    
    try {
      pstmt = conn.prepareStatement(statementStr);
      pstmt.execute();
    } catch (SQLException e) {
      LOGGER.error("cleanupDatabase : {}");
    } finally {
       CloseUtil.close(LOGGER, pstmt, conn);
    }
  }
}

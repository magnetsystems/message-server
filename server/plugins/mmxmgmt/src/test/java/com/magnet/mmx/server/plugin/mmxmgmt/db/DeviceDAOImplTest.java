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

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.PushType;
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
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for the DeviceDAOImpl
 */
public class DeviceDAOImplTest {

  private static BasicDataSource ds;

  @BeforeClass
  public static void setup() throws Exception {
    InputStream inputStream = DeviceDAOImplTest.class.getResourceAsStream("/test.properties");

    ds = UnitTestDSProvider.getDataSource();
    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/app-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/device-data-1.xml");
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
  public void testConnection() throws Exception {
    Connection conn = ds.getConnection();
    assertNotNull(conn);
  }

  @Test
  public void testAddDevice() {

    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));

    DevReg request = new DevReg();
    request.setDevId("433536df7038e1b7");
    request.setDisplayName("Rahul's android");
    request.setOsType(OSType.ANDROID.toString());
    request.setPushType(PushType.GCM.toString());
    request.setPushToken("APA91bHaCculnOoolX0HV3f3CLHBY52C-H0lDS_m-lXXg5MbT9-EJiE6ooe0dUWURLuTQmVOttBS18cQwX5Pe-k9JDI2o8bq" +
        "Rhi3UZ0McTNs9JADvguH63vihIbVAgAjUm4K8mOZcRG4MC-edQBiiZ87l-GnQKpZ4ejBRP3j72oVQI6ooDavac4");
    request.setPhoneNumber("4086007000");
    request.setCarrierInfo("VERIZON");
    request.setOsVersion("macron");

    String appId = "AAABSNIBKOstQST7";

    int id = dao.addDevice("login3", appId, request);
    assertTrue("Got a zero id", id > 0);
  }

  /**
   * Use the same deviceId but different appid
   */
  @Test
  public void testAddDeviceWithDifferentAppId() throws DeviceNotFoundException {

    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));

    DevReg request = new DevReg();
    String deviceId = "433536df7038e1b7";
    request.setDevId(deviceId);
    request.setDisplayName("Rahul's android");
    request.setOsType(OSType.ANDROID.toString());
    request.setPushType(PushType.GCM.toString());
    request.setPushToken("APA91bHaCculnOoolX0HV3f3CLHBY52C-H0lDS_m-lXXg5MbT9-EJiE6ooe0dUWURLuTQmVOttBS18cQwX5Pe-k9JDI2o8bq" +
        "Rhi3UZ0McTNs9JADvguH63vihIbVAgAjUm4K8mOZcRG4MC-edQBiiZ87l-GnQKpZ4ejBRP3j72oVQI6ooDavac4");
    request.setOsVersion("macron");
    request.setVersionMajor(Constants.MMX_VERSION_MAJOR);
    request.setVersionMinor(Constants.MMX_VERSION_MINOR);
    String appId = "AAABSNIBKOstQST8";
    int id = dao.addDevice("login3", appId, request);
    assertTrue("Got a zero id", id > 0);
    try {
      DeviceEntity entity = dao.getDevice(appId, deviceId);
      assertNotNull(entity);
      DeviceEntity.Version version = entity.getProtocolVersion();
      DeviceEntity.Version expected = new DeviceEntity.Version(Constants.MMX_VERSION_MAJOR, Constants.MMX_VERSION_MINOR);
      assertEquals("Non matching version", expected, version);
    } catch (SQLException e) {
      e.printStackTrace();
      fail("testAddDeviceWithDifferentAppId failed");
    }

  }

  @Test
  public void testRetrieveDevice1() {
    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));
    String deviceId = "8933536df7038e1b7";
    OSType android = OSType.ANDROID;
    String appId = "AAABSNIBKOstQST7";
    DeviceEntity entity = dao.getDevice(deviceId, android, appId);
    assertNotNull(entity);
    int expectedId = 1;
    assertEquals("non matching id", expectedId, entity.getId());
    assertEquals("non matching deviceids", deviceId, entity.getDeviceId());
    DeviceStatus expected = DeviceStatus.ACTIVE;
    assertEquals("non matching device status", expected, entity.getStatus());
    String phoneNumber = "4083084001";
    assertEquals("non phone number", phoneNumber, entity.getPhoneNumber());
    String carrierInfo = "ATT";
    assertEquals("carrier info not matching", carrierInfo, entity.getCarrierInfo());
  }


  @Test
  public void testUpdateDevice1() {
    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));
    
    String deviceId = "10101010101";
    String appId = "AAABSNIBKOstQST7";
    String ownerId = "superUser";
    // create a device first
    DevReg createRequest = new DevReg();
    createRequest.setDevId(deviceId);
    createRequest.setDisplayName("Test android");
    createRequest.setOsType(OSType.ANDROID.toString());
    createRequest.setPushType(PushType.GCM.toString());
    createRequest.setPushToken("bogus token");
    createRequest.setOsVersion("macron");

    int id = dao.addDevice(ownerId, appId, createRequest);
    assertTrue("Got a zero id", id > 0);
    // update the device
    {
      String updatedName = "Cool phone";
      DevReg update = new DevReg();
      update.setDisplayName(updatedName);
      update.setPushToken(null);
      update.setOsVersion("sandwich 1.0.8");
      DeviceEntity.Version version = new DeviceEntity.Version(1,2);
      update.setVersionMajor(version.getMajor());
      update.setVersionMinor(version.getMinor());
      int count = dao.updateDevice(deviceId, OSType.ANDROID, appId, update, ownerId, DeviceStatus.ACTIVE);
      assertTrue("non matching update count", count == 1);
      // retrieve and assert the updates
      DeviceEntity updated = dao.getDevice(deviceId, OSType.ANDROID, appId);
      assertEquals("Non matching names", updatedName, updated.getName());
      assertNull("Token is not null", updated.getClientToken());
      assertEquals("Non matching update version", version, updated.getProtocolVersion());
    }
  }

  @Test
  public void testDeactivateDevice() {
    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String deviceId = "mydeviceid" + new Random().nextLong();
    String appId = "AAABSNIBKOstQST7";
    String ownerId = "superUser";
    // create a device first
    DevReg createRequest = new DevReg();
    createRequest.setDevId(deviceId);
    createRequest.setDisplayName("Test android");
    createRequest.setOsType(OSType.ANDROID.toString());
    createRequest.setPushType(PushType.GCM.toString());
    createRequest.setPushToken("bogus token");
    createRequest.setOsVersion("macron");

    int id = dao.addDevice(ownerId, appId, createRequest);
    assertTrue("Got a zero id", id > 0);
    // update the device
    {

      int count = dao.deactivateDevice(deviceId);
      assertTrue("non matching update count", count == 1);
      // retrieve and assert the updates
      DeviceEntity updated = dao.getDevice(deviceId, OSType.ANDROID, appId);
      assertEquals("Non matching status", DeviceStatus.INACTIVE, updated.getStatus());
    }
  }

  @Test
  public void testListingDevicesForAppkeyAndUser() {
    String userId = "magnet.way";
    String appKey = "AAABSNIBKOstQST7";
    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));
    List<DeviceEntity> devices = dao.getDevices(appKey, userId, DeviceStatus.ACTIVE);

    assertTrue("devices list is empty", !devices.isEmpty());
  }


  @Test
  public void testInvalidateToken() {
    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));
    String deviceId = "mydeviceid" + new Random().nextLong();
    String appId = "AAABSNIBKOstQST7";
    String ownerId = "superUser";
    String token = "a5b8f7b51acad15f96";

    // create a device first
    DevReg createRequest = new DevReg();
    createRequest.setDevId(deviceId);
    createRequest.setDisplayName("Test android");
    createRequest.setOsType(OSType.ANDROID.toString());
    createRequest.setPushType(PushType.GCM.toString());
    createRequest.setPushToken(token);
    createRequest.setOsVersion("macron");

    int id = dao.addDevice(ownerId, appId, createRequest);
    assertTrue("Got a zero id", id > 0);
    // update the device
    {

      dao.invalidateToken(appId, PushType.GCM,token);
      //assertTrue("non matching update count", count == 1);
      // retrieve and assert the updates
      DeviceEntity updated = dao.getDevice(deviceId, OSType.ANDROID, appId);
      assertEquals("Non matching push status", PushStatus.INVALID, updated.getPushStatus());
    }
    //change the push status to null
    {
      dao.updatePushStatus(deviceId, OSType.ANDROID, appId, null);
      DeviceEntity updated = dao.getDevice(deviceId, OSType.ANDROID, appId);
      assertNull("Non matching push status",  updated.getPushStatus());
    }
    {
      dao.updatePushStatus(deviceId, OSType.ANDROID, appId, PushStatus.VALID);
      DeviceEntity updated = dao.getDevice(deviceId, OSType.ANDROID, appId);
      assertEquals("Non matching push status", PushStatus.VALID, updated.getPushStatus());
    }
  }
}

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

import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.AbstractDataType;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.dataset.datatype.TypeCastException;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmpp.packet.JID;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class MessageDAOImplTest {
  private static BasicDataSource ds;

  @BeforeClass
  public static void setup() throws Exception {
    ds = UnitTestDSProvider.getDataSource();
    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    //IDatabaseConnection con = new DatabaseConnection(setup);
    DatabaseConnection con = new DatabaseConnection(setup);
    DatabaseConfig dbConfig = con.getConfig();
    dbConfig.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new CustomDataTypeFactory());
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/message-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/wakeup-queue-1.xml");
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
  public void testInsert1() {
    MessageEntity me = new MessageEntity();
    me.setMessageId(Long.toHexString(System.nanoTime()));
    me.setState(MessageEntity.MessageState.PENDING);
    //public JID(java.lang.String node, java.lang.String domain, java.lang.String resource)
    String appkey = "PrivateApp1";
    String targetDeviceId = "device2";
    JID from = new JID("login3" + JIDUtil.APP_ID_DELIMITER + appkey, "localhost", "device1");
    JID to = new JID("otheruser" + JIDUtil.APP_ID_DELIMITER + appkey, "localhost", targetDeviceId);

    me.setAppId(appkey);
    me.setTo(to.toString());
    me.setFrom(from.toString());
    me.setDeviceId(targetDeviceId);
    me.setState(MessageEntity.MessageState.DELIVERY_ATTEMPTED);
    MessageDAO dao = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    dao.persist(me);

    assertTrue(true);
  }


  @Test
  public void testUpdateState() {
    String messageId = "1396563a44077708";
    String targetDeviceId = "device2";
    MessageDAO dao = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    dao.updateMessageState(messageId, targetDeviceId, MessageEntity.MessageState.WAKEUP_SENT);
    //next query for messages with state set to  wakeup required
    MessageEntity entity = dao.get(messageId, targetDeviceId);
    assertEquals("Not expected message state", MessageEntity.MessageState.WAKEUP_SENT, entity.getState());
  }

  @Test
  public void testListingMessagesForRetryProcessing() {

    long currentUTCTime = 1411605900L; // September 24, 2014 at 5:45:00 PM PDT
   // long currentUTCTime = 1411666200L;  // September 25, 2014 at 10:30:00 AM PDT
    MessageDAO dao = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    //elapsedTime in seconds
    int elapsedTime = 30 * 60;

    List<MessageEntity> retryList = dao.getMessagesForRetryProcessing(elapsedTime, currentUTCTime, 2);

    assertTrue("List is empty", !retryList.isEmpty());
    int expectedSize = 1;
    assertEquals("Non matching list size", expectedSize, retryList.size());
  }

  @Test
  public void testListingMessagesForRetryProcessingWithDifferentRetryCount() {

    long currentUTCTime = 1411605900L; // September 24, 2014 at 5:45:00 PM PDT
    // long currentUTCTime = 1411666200L;  // September 25, 2014 at 10:30:00 AM PDT
    MessageDAO dao = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    //elapsedTime in seconds
    int elapsedTime = 30 * 60;

    List<MessageEntity> retryList = dao.getMessagesForRetryProcessing(elapsedTime, currentUTCTime, 1);

    assertTrue("List is empty", retryList.isEmpty());
  }

  @Test
  public void testTimeoutMessage() {

    long currentUTCTime = 1411605900L; // September 24, 2014 at 5:45:00 PM PDT
    // long currentUTCTime = 1411666200L;  // September 25, 2014 at 10:30:00 AM PDT
    MessageDAO dao = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    //timeoutperiod in minutes (300 minutes)
    int timeoutperiod = 300;

    int count = dao.messageTimeout(currentUTCTime, timeoutperiod);
    //assertTrue("List is empty", !retryList.isEmpty());
    int expectedSize = 1;
    assertEquals("Non matching list size", expectedSize, count);
  }

  @Test
  public void testMessageDelivered() {

    String messageId = "zWoNarzoTGOZEL0UTwDB2w-6";
    String appId = "i26u1lmv7uc";
    String deviceId = "8D2F9E5595E9989FEF3D1D3A5BA0FBE0BB318ED0";
    MessageDAO dao = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    int count = dao.messageDelivered(appId, deviceId, messageId);
    assertEquals("Non matching message count", 1, count);
  }

  @Test
  public void testMessageWakeupSent() {
    String messageId = "c126eb1ebb61126042f252b25c593c0b";
    String deviceId = "17BCE336-FBBD-44D5-AB69-D773900990B1";
    MessageDAO dao = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    dao.wakeupSent(messageId, deviceId);
    MessageEntity entity = dao.get(messageId, deviceId);
    MessageEntity.MessageState state = entity.getState();
    assertEquals("Not expected message state", MessageEntity.MessageState.WAKEUP_SENT, state);
  }

  /**
   * Attempt changing the state of a delivered message to wakeup_sent. Shouldn't be updated.
   */
  @Test
  public void testMessageWakeupSent2() {
    String messageId = "e4c86b2e16b64c64c953de1789fdaf6d";
    String deviceId = "8D2F9E5595E9989FEF3D1D3A5BA0FBE0BB318ED0";
    MessageDAO dao = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    dao.wakeupSent(messageId, deviceId);
    MessageEntity entity = dao.get(messageId, deviceId);
    MessageEntity.MessageState state = entity.getState();
    assertEquals("Not expected message state", MessageEntity.MessageState.DELIVERED, state);
  }



  public static class CustomTimestampDataType extends AbstractDataType {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CustomTimestampDataType.class);

    private final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.S";//"2014-09-22 17:42:10.0";

    public CustomTimestampDataType() {
      super("TIMESTAMP", Types.TIMESTAMP, Timestamp.class, false);
    }


    public Object typeCast(Object value) throws TypeCastException
    {
      logger.debug("typeCast(value={}) - start", value);

      if (value == null || value == ITable.NO_VALUE)
      {
        return null;
      }

      if (value instanceof java.sql.Timestamp)
      {
        return value;
      }

      if (value instanceof java.util.Date)
      {
        java.util.Date date = (java.util.Date)value;
        return new java.sql.Timestamp(date.getTime());
      }

      if (value instanceof Long)
      {
        Long date = (Long)value;
        return new java.sql.Timestamp(date.longValue());
      }

      if (value instanceof String)
      {
        String stringValue = value.toString();
        String zoneValue = null;

        Timestamp ts = null;
        if (stringValue.length() == 10)
        {
          try
          {
            long time = java.sql.Date.valueOf(stringValue).getTime();
            ts = new java.sql.Timestamp(time);
          }
          catch (IllegalArgumentException e)
          {
            // Was not a java.sql.Date, let Timestamp handle this value
          }
        }
        if (ts == null)
        {
          try
          {
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("US/Pacific"));
            formatter.setCalendar(calendar);
//            formatter.setTimeZone(TimeZone.getTimeZone("PDT"));
            java.util.Date parsed = formatter.parse(stringValue);
            logger.info("{}->{}",stringValue, parsed.getTime()/1000L);
            ts = new Timestamp(parsed.getTime());
          }
          catch (IllegalArgumentException e)
          {
            throw new TypeCastException(value, this, e);
          } catch (ParseException e) {
            throw new TypeCastException(value, this, e);
          }
        }
        //ts.set
        return ts;
      }

      throw new TypeCastException(value, this);
    }

    public boolean isDateTime()
    {
      logger.debug("isDateTime() - start");

      return true;
    }

    public Object getSqlValue(int column, ResultSet resultSet)
        throws SQLException, TypeCastException
    {
      if(logger.isDebugEnabled())
        logger.debug("getSqlValue(column={}, resultSet={}) - start", new Integer(column), resultSet);

      Timestamp value = resultSet.getTimestamp(column);
      if (value == null || resultSet.wasNull())
      {
        return null;
      }
      return value;
    }

    public void setSqlValue(Object value, int column, PreparedStatement statement)
        throws SQLException, TypeCastException
    {
      if(logger.isDebugEnabled())
        logger.debug("setSqlValue(value={}, column={}, statement={}) - start",
            new Object[]{value, new Integer(column), statement} );
      Timestamp ts = (java.sql.Timestamp)typeCast(value);
      //statement.setTimestamp(column, (java.sql.Timestamp)typeCast(value));
      statement.setTimestamp(column,  ts, Calendar.getInstance(TimeZone.getTimeZone("GMT")) );
      //Calendar calutc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      //statement.setTimestamp(column, new java.sql.Timestamp(0), calutc);
    }
  }

  public static class CustomDataTypeFactory extends MySqlDataTypeFactory {
    public DataType createDataType(int sqlType, String sqlTypeName)
        throws DataTypeException {
      if (sqlType == Types.TIMESTAMP) {
        return new CustomTimestampDataType();
      }
      else {
        return super.createDataType(sqlType, sqlTypeName);
      }
    }
  }

}

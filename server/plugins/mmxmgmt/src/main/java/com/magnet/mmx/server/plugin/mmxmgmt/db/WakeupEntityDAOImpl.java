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

import com.magnet.mmx.protocol.PushType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
public class WakeupEntityDAOImpl implements WakeupEntityDAO {
  private Logger LOGGER = LoggerFactory.getLogger(WakeupEntityDAOImpl.class);

  private static final String WAKEUP_INSERT = "INSERT INTO mmxWakeupQueue(deviceId,clientToken,tokenType,googleApiKey," +
      "payload,messageId,dateCreatedUTC,appId) VALUES (?,?,?,?,?,?,?,?)";

  private static final String WAKEUP_SELECT_BY_LIMIT = "SELECT id, deviceId,clientToken,tokenType,googleApiKey," +
      "payload,messageId,dateCreatedUTC, appId FROM mmxWakeupQueue WHERE dateSentUTC IS NULL AND clientToken IS NOT NULL ORDER BY dateCreatedUTC LIMIT ?";

  private static final String WAKEUP_UPDATE = "UPDATE mmxWakeupQueue SET dateSentUTC = ? WHERE dateSentUTC IS NULL AND id = ?";

  private static final String DELETE_INVALID_WAKEUP_RECORDS = "DELETE FROM mmxWakeupQueue " +
      "     WHERE  appId = ? AND tokenType = ? AND clientToken = ?  AND  dateSentUTC IS NULL";

  private static final String DELETE_BAD_API_KEY_WAKEUP_RECORDS = "DELETE FROM mmxWakeupQueue " +
      "     WHERE  id = ? AND  dateSentUTC IS NULL";

  private static final String WAKEUP_SELECT_FOR_DEVICE_FOR_MUTE = "SELECT id, deviceId, clientToken, tokenType, googleApiKey," +
      "payload,messageId,dateCreatedUTC, appId FROM mmxWakeupQueue WHERE appId = ? AND deviceId = ? AND " +
      "(? - dateCreatedUTC < ?) ";


  private ConnectionProvider provider;

  /**
   * Constructor
   * @param provider
   */
  public WakeupEntityDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public void offer(WakeupEntity entity) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(WAKEUP_INSERT, PreparedStatement.RETURN_GENERATED_KEYS);
      pstmt.setString(1, entity.getDeviceId());
      pstmt.setString(2, entity.getToken());
      pstmt.setString(3, entity.getType().name());
      pstmt.setString(4, entity.getSenderIdentifier());
      pstmt.setString(5, entity.getPayload());
      pstmt.setString(6, entity.getMessageId());
      pstmt.setLong(7, Long.valueOf((new Date().getTime())/1000L));
      pstmt.setString(8, entity.getAppId());
      pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in creating the device record", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public List<WakeupEntity> poll(int maxCount) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<WakeupEntity> returnList = new ArrayList<WakeupEntity>(10);
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(WAKEUP_SELECT_BY_LIMIT);
      pstmt.setInt(1, maxCount);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        WakeupEntity pae = new WakeupEntity.WakeupEntityBuilder().build(rs);
        returnList.add(pae);
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return returnList;
  }

  @Override
  public void complete(WakeupEntity entity) {
    if (entity.getDateSent() == null) {
      throw new IllegalArgumentException("Invalid date sent");
    }
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(WAKEUP_UPDATE);
      pstmt.setLong(1, entity.getDateSent());
      pstmt.setInt(2, entity.getId());
      pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in creating the device record", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public int remove(String appId, PushType type, String token) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(DELETE_INVALID_WAKEUP_RECORDS);
      pstmt.setString(1, appId);
      pstmt.setString(2, type.name());
      pstmt.setString(3, token);
      int deleteCount = pstmt.executeUpdate();
      pstmt.close();
      con.close();
      return deleteCount;
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in creating the device record", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public void remove(int wakeupEntityId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(DELETE_BAD_API_KEY_WAKEUP_RECORDS);
      pstmt.setInt(1, wakeupEntityId);
      int deleteCount = pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in remove wakeup entity record", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public List<WakeupEntity> retrieveOpenOrSentWakeup(String appId, DeviceEntity deviceEntity, int mutePeriod) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<WakeupEntity> returnList = new ArrayList<WakeupEntity>(10);
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(WAKEUP_SELECT_FOR_DEVICE_FOR_MUTE);
      pstmt.setString(1, appId);
      pstmt.setString(2, deviceEntity.getDeviceId());
      pstmt.setInt(3, (int) (currentTime().getTime()/1000L));
      pstmt.setInt(4, (int) TimeUnit.SECONDS.convert(mutePeriod, TimeUnit.MINUTES));
      rs = pstmt.executeQuery();
      while (rs.next()) {
        WakeupEntity pae = new WakeupEntity.WakeupEntityBuilder().build(rs);
        returnList.add(pae);
      }
      rs.close();
      pstmt.close();
      con.close();
      rs = null;
      pstmt = null;
      con = null;
    } catch (SQLException e) {
      LOGGER.error("SQLException in retrieveOpenOrSentWakeup", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return returnList;
  }


  protected Date currentTime() {
    return new Date();
  }
}

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

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.server.plugin.mmxmgmt.search.device.DeviceSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.device.DeviceSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MMXDeviceStats;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXDeviceCountResult;
import com.magnet.mmx.server.plugin.mmxmgmt.util.SqlUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Implement the DeviceDAO
 */
public class DeviceDAOImpl implements DeviceDAO {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeviceDAOImpl.class);
  private static final String PERCENTAGE = "%";

  private static final String DEVICE_COLUMN_STRING = "id,name,ownerJid,appId,osType,deviceId," +
            "tokenType,clientToken,versionInfo,modelInfo,status,dateCreated,dateUpdated, phoneNumber, carrierInfo," +
            "phoneNumberRev,protocolVersionMajor,protocolVersionMinor,pushStatus";

  private static final String INSERT_DEVICE = "INSERT INTO mmxDevice (name,ownerJid,appId,osType,deviceId," +
      "tokenType,clientToken,versionInfo, " +
      "modelInfo,status, " +
      "dateCreated, phoneNumber, carrierInfo, phoneNumberRev,protocolVersionMajor,protocolVersionMinor,pushStatus) " +
      "VALUES (?,?,?,?,?,?,?,?,?,?,now(),?,?,?,?,?,?)";

  private static final String QUERY_USING_DEVICEID_AND_OS_TYPE = "SELECT " + DEVICE_COLUMN_STRING + " FROM mmxDevice " +
      "WHERE deviceId=? AND osType=? AND appId=?";

  private static final String UPDATE_DEVICE = "UPDATE mmxDevice SET name=?, ownerJid=? , clientToken=?, tokenType=?, " +
      "versionInfo=?, modelInfo=?, status=?, dateUpdated=?, phoneNumber=?, phoneNumberRev=?, carrierInfo=?, " +
      "protocolVersionMajor=?, protocolVersionMinor=? "+
      "WHERE appId=? AND deviceId=? AND osType=?";
  
  private static final String UPDATE_DEVICE_STATUS = "UPDATE mmxDevice SET status = ?, dateUpdated =? " +
      "WHERE deviceId = ? ";

  private static final String UPDATE_DEVICE_PUSH_STATUS = "UPDATE mmxDevice SET pushStatus = ?, dateUpdated = ? " +
      "WHERE appId = ? AND clientToken = ? AND tokenType = ?";

  private static final String UPDATE_DEVICE_PUSH_STATUS_OTHER = "UPDATE mmxDevice SET pushStatus = ?, dateUpdated = ? " +
      "WHERE appId = ? AND deviceId = ? AND osType = ?";

  private static final String QUERY_USING_APPKEY_USERID_AND_STATUS = "SELECT " + DEVICE_COLUMN_STRING + " FROM mmxDevice " +
      "WHERE ownerJid=? AND appId=? AND status = ?";

  private static final String QUERY_USING_APPID_DEVICEID = "SELECT " + DEVICE_COLUMN_STRING  + " FROM mmxDevice " +
          "WHERE appId = ? AND deviceId = ?";

  private static final String QUERY_USING_APPKEY_DEVICE_ID_USERID_AND_STATUS = "SELECT " + DEVICE_COLUMN_STRING  + " FROM mmxDevice " +
      "WHERE ownerJid=? AND appId=?  AND status = ? AND deviceId = ?";

  private static final String QUERY_USING_APPKEY_DEVICE_ID_AND_STATUS = "SELECT " + DEVICE_COLUMN_STRING  + " FROM mmxDevice " +
      "WHERE appId=? AND status = ? AND deviceId = ?";

  private static final String QUERY_USING_APPID_AND_ID_LIST = "SELECT " + DEVICE_COLUMN_STRING  + " FROM mmxDevice " +
      "WHERE appId=? AND status = ? AND deviceId IN (%s)";

  private ConnectionProvider provider;

  public DeviceDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public void persist(DeviceEntity entity) {
    addDevice(entity.getOwnerId(), entity.getAppId(), getRequestFromEntity(entity));
  }

  @Override
  public int addDevice(String ownerId, String appId, DevReg request) throws DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Integer rv = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(INSERT_DEVICE, PreparedStatement.RETURN_GENERATED_KEYS);
      pstmt.setString(1, request.getDisplayName());
      pstmt.setString(2, ownerId);
      pstmt.setString(3, appId);
      pstmt.setString(4, Helper.enumerateOSType(request.getOsType()).toString());
      pstmt.setString(5, request.getDevId());

      if (request.getPushType() != null) {
        pstmt.setString(6, Helper.enumeratePushType(request.getPushType()).toString());
      } else {
        pstmt.setNull(6, Types.VARCHAR);
      }
      boolean hasPushToken = request.getPushToken() != null;
      if (hasPushToken) {
        pstmt.setString(7, request.getPushToken());
      } else {
        pstmt.setNull(7, Types.VARCHAR);
      }
      if (request.getOsVersion() != null) {
        pstmt.setString(8, request.getOsVersion());
      } else {
        pstmt.setNull(8, Types.VARCHAR);
      }
      if (request.getModelInfo() != null) {
        pstmt.setString(9, request.getModelInfo());
      } else {
        pstmt.setNull(9, Types.VARCHAR);
      }

      pstmt.setString(10, DeviceStatus.ACTIVE.name());

      if (request.getPhoneNumber() != null) {
        pstmt.setString(11, request.getPhoneNumber());
      } else {
        pstmt.setNull(11, Types.VARCHAR);
      }

      if (request.getCarrierInfo() != null) {
        pstmt.setString(12, Helper.enumerateCarrierInfo(request.getCarrierInfo()).toString());
      } else {
        pstmt.setNull(12, Types.VARCHAR);
      }

      if (request.getPhoneNumber() != null) {
        pstmt.setString(13, Helper.reverse(request.getPhoneNumber()));
      } else {
        pstmt.setNull(13, Types.VARCHAR);
      }
      int major = request.getVersionMajor();
      int minor = request.getVersionMinor();

      pstmt.setInt(14, major);
      pstmt.setInt(15, minor);
      if (hasPushToken) {
        pstmt.setString(16, PushStatus.VALID.name());
      } else {
        pstmt.setNull(16, Types.VARCHAR);
      }


      LOGGER.trace("addDevice : ownerId={}, appId={}, deviceId={}, statement={}", new Object[]{ownerId, appId, request.getDevId(), pstmt});
      pstmt.executeUpdate();
      rs = pstmt.getGeneratedKeys();

      if (rs.next()) {
        rv = Integer.valueOf(rs.getInt(1));
      }
      rs.close();
      pstmt.close();
      con.close();
      return rv;
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in creating the device record", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
  }

  @Override
  public DeviceEntity getDevice(String deviceId, OSType type, String appId) throws DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    DeviceEntity entity = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(QUERY_USING_DEVICEID_AND_OS_TYPE);
      pstmt.setString(1, deviceId);
      pstmt.setString(2, type.name());
      pstmt.setString(3, appId);

      rs = pstmt.executeQuery();
      if (rs.next()) {
        entity = new DeviceEntity.DeviceEntityBuilder().build(rs);
      }
      rs.close();
      pstmt.close();
      return entity;
    } catch (SQLException sqle) {
      LOGGER.warn(sqle.getMessage(), sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
  }

  @Override
  public List<DeviceEntity> getDevices(String appId, List<String> deviceIds, DeviceStatus status) {
    if (deviceIds.isEmpty()) {
      return Collections.emptyList();
    }

    int count = deviceIds.size();
    String placeHolder = Helper.getSQLPlaceHolders(count);
    String sql = String.format(QUERY_USING_APPID_AND_ID_LIST, placeHolder);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Constructed SQL:" + sql);
    }
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<DeviceEntity> returnList = new ArrayList<DeviceEntity>(count);
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(sql);
      pstmt.setString(1, appId);
      pstmt.setString(2, status.name());
      int index = 3;
      for (String deviceId : deviceIds) {
        pstmt.setString(index++, deviceId);
      }
      rs = pstmt.executeQuery();
      while (rs.next()) {
        DeviceEntity pae = new DeviceEntity.DeviceEntityBuilder().build(rs);
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
  public DeviceEntity getDevice(String appId, String deviceId) throws SQLException, DeviceNotFoundException {
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(QUERY_USING_APPID_DEVICEID);
      pstmt.setString(1, appId);
      pstmt.setString(2, deviceId);
      rs = pstmt.executeQuery();
      if(rs.next()) {
        return new DeviceEntity.DeviceEntityBuilder().build(rs);
      } else {
        throw new DeviceNotFoundException(deviceId,
                                          String.format("Device does not exist : deviceId : %s, appId : %s",
                                                         deviceId, appId));
      }

    } catch (SQLException e) {
      LOGGER.error("getDevice : caught exception appId={}, deviceId={}", appId, deviceId);
      throw e;
    } finally {
       CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public int updateDevice(String deviceId, OSType type, String appId, DevReg request,
                          String ownerId, DeviceStatus status) throws DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(UPDATE_DEVICE);
      pstmt.setString(1, request.getDisplayName());
      pstmt.setString(2, ownerId);
      pstmt.setString(3, request.getPushToken());
      pstmt.setString(4, request.getPushType());
      pstmt.setString(5, request.getOsVersion());
      pstmt.setString(6, request.getModelInfo());
      pstmt.setString(7, status.name());
      pstmt.setTimestamp(8, new Timestamp(new Date().getTime()));
      if (request.getPhoneNumber() == null) {
        pstmt.setNull(9, Types.VARCHAR);
        pstmt.setNull(10, Types.VARCHAR);
      } else {
        pstmt.setString(9, request.getPhoneNumber());
        pstmt.setString(10, Helper.reverse(request.getPhoneNumber()));
      }
      if (request.getCarrierInfo() == null) {
        pstmt.setNull(11, Types.VARCHAR);
      } else {
        pstmt.setString(11, request.getCarrierInfo());
      }
      pstmt.setInt(12, request.getVersionMajor());
      pstmt.setInt(13, request.getVersionMinor());

      //where clause
      pstmt.setString(14, appId);
      pstmt.setString(15, deviceId);
      pstmt.setString(16, type.name());

      LOGGER.trace("updateDevice : deviceId={}, appId={}, statment={}", new Object[]{deviceId, appId, pstmt});
      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
      return count;
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in creating the device record", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public int deactivateDevice(String deviceId) throws DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(UPDATE_DEVICE_STATUS);
      pstmt.setString(1, DeviceStatus.INACTIVE.name());
      pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
      pstmt.setString(3, deviceId);
      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
      return count;
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in creating the device record", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public List<DeviceEntity> getDevices(String appId, String userId, DeviceStatus status) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<DeviceEntity> returnList = new ArrayList<DeviceEntity>(10);
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(QUERY_USING_APPKEY_USERID_AND_STATUS);
      pstmt.setString(1, userId);
      pstmt.setString(2, appId);
      pstmt.setString(3, status.toString());

      rs = pstmt.executeQuery();
      while (rs.next()) {
        DeviceEntity pae = new DeviceEntity.DeviceEntityBuilder().build(rs);
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
  public DeviceEntity getDeviceUsingId(String appId, String userId, String deviceId, DeviceStatus status) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    DeviceEntity entity = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(QUERY_USING_APPKEY_DEVICE_ID_USERID_AND_STATUS);
      pstmt.setString(1, userId);
      pstmt.setString(2, appId);
      pstmt.setString(3, status.toString());
      pstmt.setString(4, deviceId);

      rs = pstmt.executeQuery();
      if (rs.next()) {
        entity = new DeviceEntity.DeviceEntityBuilder().build(rs);
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return entity;
  }

  @Override
  public DeviceEntity getDeviceUsingId(String appId, String deviceId, DeviceStatus status) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    DeviceEntity entity = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(QUERY_USING_APPKEY_DEVICE_ID_AND_STATUS);
      pstmt.setString(1, appId);
      pstmt.setString(2, status.toString());
      pstmt.setString(3, deviceId);

      rs = pstmt.executeQuery();
      if (rs.next()) {
        entity = new DeviceEntity.DeviceEntityBuilder().build(rs);
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return entity;
  }

  private Map<String, MMXDeviceStats> getDeviceStats2(List<String> appIdList) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String statementStr = "SELECT appId, COUNT(*) FROM mmxDevice WHERE appId IN ( "+ SqlUtil.getQs(appIdList.size()) +" ) AND dateCreated > ? GROUP BY appId";
    Map<String, MMXDeviceStats> statsMap = new HashMap<String, MMXDeviceStats>();
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(statementStr);
      for (int i = 1; i <= appIdList.size(); i++) {
        String appId = appIdList.get(i - 1);
        pstmt.setString(i, appId);
        statsMap.put(appId, new MMXDeviceStats(appId));
      }
      pstmt.setDate(appIdList.size() + 1, new java.sql.Date(new DateTime().minusDays(7).getMillis()));
      rs = pstmt.executeQuery();
      while (rs.next()) {
        String appId = rs.getString(1);
        int count = rs.getInt(2);
        MMXDeviceStats deviceStats = statsMap.get(appId);
        deviceStats.setNumDevices(count);
        LOGGER.trace("getDeviceStats : appId={}, count={}", appId, count);
      }
    } catch (SQLException e) {
      LOGGER.error("getDeviceStats : exception caught appIdList={}", appIdList);
      e.printStackTrace();
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return statsMap;
  }

  @Override
  public Map<String, MMXDeviceStats> getDeviceStats(List<String> appIdList) {
    LOGGER.trace("getDeviceStats : appIdList={}", appIdList);
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String statementStr = "SELECT appId, status, dateCreated FROM mmxDevice WHERE appId IN ( "+ SqlUtil.getQs(appIdList.size()) +" )";
    Map<String, MMXDeviceStats> statsMap = new HashMap<String, MMXDeviceStats>();
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(statementStr);
      for (int i = 1; i <= appIdList.size(); i++) {
        String appId = appIdList.get(i - 1);
        pstmt.setString(i, appId);
        statsMap.put(appId, new MMXDeviceStats(appId));
      }
      LOGGER.trace("getDeviceStats : executing query : {}", pstmt);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        String appId = rs.getString(1);
        String status = rs.getString(2);

        MMXDeviceStats deviceStats = statsMap.get(appId);
        if(DeviceStatus.ACTIVE.name().equals(status)) {
          deviceStats.incrementActive();
        } else if(DeviceStatus.INACTIVE.name().equals(status)) {
          deviceStats.incrementInactive();
        }

        DateTime dateCreated = new DateTime(rs.getDate(3));
        DateTime now = new DateTime();
        if(dateCreated.isAfter(now.minusDays(7))) {
          deviceStats.incrementNumDevices();
        }
        LOGGER.trace("getDeviceStats : appId={}, numDevices={}, totalActive={}, totalInactive={}", new Object[]{appId, deviceStats.getNumDevices(), deviceStats.getTotalActive(), deviceStats.getTotalInActive()});
      }
    } catch (SQLException e) {
      LOGGER.error("getDeviceStats : exception caught appIdList={}", appIdList);
      e.printStackTrace();
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return statsMap;
  }


  @Override
  public List<String> getOwnerIdsByDeviceIdAndAppId(String appId, String deviceId) {
    final String statementStr = "SELECT ownerJid from mmxDevice where appId = ? AND deviceId = ?";
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<String> ownerIds = new ArrayList<String>();

    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      pstmt.setString(2, deviceId);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        ownerIds.add(rs.getString(1));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }

    return ownerIds;
  }

  @Override
  public MMXDeviceCountResult getMMXDeviceCountByAppId(String appId, String ownerJid) {
    MMXDeviceCountResult result = null;
    final String statementStr = "SELECT deviceId, ownerJid FROM mmxDevice where appId = ? and status = ?";
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    long ownerJidCount = 0;
    long deviceCount = 0;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      pstmt.setString(2, "ACTIVE");
      rs = pstmt.executeQuery();
      LOGGER.trace("getMMXDeviceCountByAppId : executing query : {}", pstmt);
      while(rs.next()) {
        String resultDeviceId = rs.getString(1);
        if(resultDeviceId != null)
         deviceCount++;
        String resultOwnerJid = rs.getString(2);
        if(ownerJid.equalsIgnoreCase(resultOwnerJid) && resultDeviceId != null) {
          ownerJidCount++;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return new MMXDeviceCountResult(deviceCount, ownerJidCount, appId, ownerJid);
  }

  @Override
  public List<DeviceEntity> getDevicesForTag(String tagName, String appId) {
    final String statementStr = "select distinct mmxDevice.* from mmxTag inner join mmxDevice on mmxDevice.id = mmxTag.deviceId where mmxTag.tagname=? and mmxTag.appId=?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<DeviceEntity> deviceEntityList = new ArrayList<DeviceEntity>();
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, tagName);
      pstmt.setString(2, appId);
      rs = pstmt.executeQuery();

      while(rs.next()) {
        DeviceEntity e = new DeviceEntity.DeviceEntityBuilder().build(rs);
        deviceEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getDeviceIdsForTag : {}");
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return deviceEntityList;
  }

  @Override
  public List<DeviceEntity> getDevicesForTagORed(List<String> tagNameList, String appId) {
    List<DeviceEntity> deviceEntityList = new ArrayList<DeviceEntity>();

    if(Strings.isNullOrEmpty(appId)) {
      LOGGER.error("getDevicedForTagORed : appIs is null or empty");
    }

    if(tagNameList == null || tagNameList.size() == 0) {
      LOGGER.error("getDevicedForTagORed : tagNames list is empty");
    }
    final String statementStr = "SELECT DISTINCT mmxDevice.* FROM mmxTag INNER JOIN mmxDevice ON mmxTag.deviceId = mmxDevice.id WHERE mmxTag.tagname IN ( " + SqlUtil.getQs(tagNameList.size()) + " ) AND mmxTag.appId= ?";

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      int index = 1;
      for(String tagName : tagNameList) {
        pstmt.setString(index++, tagName);
      }
      pstmt.setString(index, appId);
      LOGGER.trace("getDevicedForTagORed : executing : {}", pstmt);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        DeviceEntity e = new DeviceEntity.DeviceEntityBuilder().build(rs);
        deviceEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getDevicedForTagORed : caught exception tagNameList={}, appId={}", tagNameList, appId, e);
    } finally {
       CloseUtil.close(LOGGER, pstmt, conn);
    }
    return deviceEntityList;
  }

  @Override
  public List<DeviceEntity> getDevicesForTagANDed(List<String> tagNames, String appId) {
    List<DeviceEntity> deviceEntityList = new ArrayList<DeviceEntity>();
    final String statementStr = "SELECT DISTINCT * FROM ( SELECT DISTINCT tempresult.deviceId AS deviceIds FROM  mmxTag AS tempresult " + getInnerJoinStatements(tagNames, appId) +
            " ) AS result1 INNER JOIN ( SELECT * FROM mmxDevice) as result2 ON  result1.deviceIds = result2.id";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      LOGGER.trace("getDevicesForTagANDed : executing query={}", pstmt);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        DeviceEntity e = new DeviceEntity.DeviceEntityBuilder().build(rs);
        deviceEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getDevicesForTagANDed : caught exception tagNames={}, appId={}", tagNames, appId, e);
    } finally {
       CloseUtil.close(LOGGER, pstmt, conn);
    }
    return deviceEntityList;
  }

  private String getInnerJoinStatements(List<String >tagNames, String appId) {
    String innerJoinTemplate = " INNER JOIN (SELECT DISTINCT deviceId FROM mmxTag WHERE tagname='%s' and appId='%s') as temp%d USING (deviceId) " ;
    StringBuffer sb = new StringBuffer(30*tagNames.size());
    int size = tagNames.size();
    for(int i=0; i < size; i++) {
      sb.append(String.format(innerJoinTemplate, tagNames.get(i), appId, i) + " ");
    }
    return sb.toString();
  }

  @Override
  public List<DeviceEntity> getDevicesByAppId(String appId) {
    final String statementStr = "SELECT * from mmxDevice where appId = ?";
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    DeviceEntity entity = null;
    List<DeviceEntity> deviceList = new ArrayList<DeviceEntity>();

    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        deviceList.add(new DeviceEntity.DeviceEntityBuilder().build(rs));
      }
    } catch (Exception e){
      LOGGER.error("getDevicesUsingAppId : appId={}", appId, e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
    return deviceList;
  }

  @Override
  public List<DeviceEntity> getDevices(QueryBuilderResult query) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<DeviceEntity> deviceList = new ArrayList<DeviceEntity>();

    try {
      con = provider.getConnection();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Executing built query:{}", query.getQuery());
      }
      pstmt = con.prepareStatement(query.getQuery());
      int index = 1;
      for (QueryParam param : query.getParamList()) {
        QueryParam.setParameterValue(param, index++, pstmt);
      }
      rs = pstmt.executeQuery();
      while(rs.next()) {
        deviceList.add(new DeviceEntity.DeviceEntityBuilder().build(rs));
      }
    } catch (Exception e){
      LOGGER.error("Exception in retrieving devices using query builder result:{}", query, e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
    return deviceList;
  }

  @Override
  public int getActiveDevicesForApp(String appId) {
    final String statementStr = "select count(*) from mmxDevice where appId = ? AND status = ?";
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    int count = 0;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      pstmt.setString(2, "ACTIVE");
      LOGGER.trace("getActiveDevicesForApp : executing query : {}", pstmt);
      rs = pstmt.executeQuery();
      if(rs.next())
        count = rs.getInt(1);
      LOGGER.trace("getActiveDevicesForApp : count={}", count);
    } catch (SQLException e) {
      LOGGER.error("getActiveDevicesForApp exception caught", e);
      e.printStackTrace();
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return count;
  }

  @Override
  public void invalidateToken(String appId, PushType pushType, String token) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(UPDATE_DEVICE_PUSH_STATUS);

      pstmt.setString(1, PushStatus.INVALID.name());
      pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
      pstmt.setString(3, appId);
      pstmt.setString(4, token);
      pstmt.setString(5, pushType.name());
      pstmt.execute();
      pstmt.close();
      con.close();
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception invalidateToken", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  /**
   * private static final String UPDATE_DEVICE_PUSH_STATUS_OTHER = "UPDATE mmxDevice SET pushStatus = ?, dateUpdated = ? " +
   "WHERE appId = ? AND deviceId = ? AND osType = ?";
   * @param deviceId
   * @param type
   * @param appId
   * @param status
   * @throws DbInteractionException
   */
  @Override
  public void updatePushStatus(String deviceId, OSType type, String appId, PushStatus status) throws
      DbInteractionException {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(UPDATE_DEVICE_PUSH_STATUS_OTHER);
      if (status == null) {
        pstmt.setNull(1, Types.VARCHAR);
      } else {
        pstmt.setString(1, status.name());
      }
      pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
      pstmt.setString(3, appId);
      pstmt.setString(4, deviceId);
      pstmt.setString(5, type.name());
      pstmt.execute();
      pstmt.close();
      con.close();
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception invalidateToken", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public SearchResult<DeviceEntity> searchDevices(String appId, DeviceSearchOption searchOption, ValueHolder valueHolder, DeviceSortOption sortOption, PaginationInfo info) {
    Connection con = null;
    int totalCount =  0;
    List<DeviceEntity> returnList = new ArrayList<DeviceEntity>(info.getTakeSize());
    try {
      con = provider.getConnection();

      DeviceSearchQueryBuilder queryBuilder = new DeviceSearchQueryBuilder();
      QueryHolder holder = queryBuilder.buildQuery(con, searchOption, valueHolder, sortOption, info, appId);
      List<QueryParam> paramList = holder.getParamList();
      //count query
      {
        PreparedStatement countPS = holder.getCountQuery();
        //set the parameters
        int index = 1;
        for (QueryParam param : paramList) {
          if (param.isForCount()) {
            QueryParam.setParameterValue(param, index++, countPS);
          }
        }
        ResultSet countRS = countPS.executeQuery();
        if (countRS.next()) {
          totalCount = countRS.getInt(1);
        }
        CloseUtil.close(LOGGER, countRS);
        CloseUtil.close(LOGGER, countPS);
      }
      //result query
      {
        PreparedStatement resultStatement = holder.getResultQuery();
        int index = 1;
        for (QueryParam param : paramList) {
          QueryParam.setParameterValue(param, index++, resultStatement);
        }
        ResultSet resultSet = resultStatement.executeQuery();
        while (resultSet.next()) {
          DeviceEntity pae = new DeviceEntity.DeviceEntityBuilder().buildLimited(resultSet);
          returnList.add(pae);
        }
        resultSet.close();
        resultStatement.close();
        CloseUtil.close(LOGGER, resultSet);
        CloseUtil.close(LOGGER, resultStatement);
      }
      con.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, con);
    }
    SearchResult<DeviceEntity> results = new SearchResult<DeviceEntity>();
    results.setResults(returnList);
    results.setTotal(totalCount);
    results.setSize(info.getTakeSize());
    results.setOffset(info.getSkipSize());
    return results;
  }


  public static class DeviceSearchQueryBuilder {

    private static final String BASE_QUERY = "SELECT "+DEVICE_COLUMN_STRING + " FROM mmxDevice " +
        "WHERE appId=?";

    private static final String BASE_COUNT_QUERY = "SELECT count(1) FROM mmxDevice WHERE appId = ? ";


    public QueryHolder buildQuery(Connection conn, DeviceSearchOption searchOption, ValueHolder searchValue, DeviceSortOption sortOption, PaginationInfo info, String appId) throws SQLException {
      List<QueryParam> paramList = new ArrayList<QueryParam>(10);

      //for appId
      QueryParam appIdParam = new QueryParam(Types.VARCHAR, appId, true);
      paramList.add(appIdParam);

      StringBuilder queryBuilder = new StringBuilder();
      /*
        build the WHERE parts first
       */
      if (searchOption == DeviceSearchOption.USERNAME) {
        queryBuilder.append(" AND ownerJid = ? ");
        QueryParam param = new QueryParam(Types.VARCHAR, searchValue.getValue1(), true);
        paramList.add(param);
      } else if (searchOption == DeviceSearchOption.DEVICE_ID) {
        queryBuilder.append(" AND deviceId LIKE ? ");
        QueryParam param = new QueryParam(Types.VARCHAR, PERCENTAGE + searchValue.getValue1() + PERCENTAGE, true);
        paramList.add(param);
      }
      /**
       * now the ordering by
       */
      DeviceSearchOption column = sortOption.getColumn();
      SortOrder sort = sortOption.getOrder();
      if (column == DeviceSearchOption.PHONE) {
        queryBuilder.append(" ORDER BY phoneNumber ");
      } else if (column == DeviceSearchOption.USERNAME) {
        queryBuilder.append(" ORDER BY ownerJid ");
      } else if (column == DeviceSearchOption.DATE_CREATED) {
        queryBuilder.append(" ORDER BY dateCreated ");
      } else if (column == DeviceSearchOption.DEVICE_ID) {
        queryBuilder.append(" ORDER BY deviceId ");
      }

      if (sort == SortOrder.DESCENDING) {
        queryBuilder.append(" DESC ");
      }

      StringBuilder countQueryBuilder = new StringBuilder();
      countQueryBuilder.append(BASE_COUNT_QUERY).append(queryBuilder.toString());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Count query:" + countQueryBuilder.toString());
      }

      StringBuilder resultQueryBuilder = new StringBuilder();
      resultQueryBuilder.append(BASE_QUERY).append(queryBuilder);

      if (info != null) {
        int chunk = info.getTakeSize();
        int offset = info.getSkipSize();
        resultQueryBuilder.append(" LIMIT ? OFFSET ?");
        QueryParam chunkp = new QueryParam(Types.INTEGER, chunk);
        QueryParam offsetp = new QueryParam(Types.INTEGER, offset);
        paramList.add(chunkp);
        paramList.add(offsetp);
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("result query:" + resultQueryBuilder.toString());
      }

      PreparedStatement countPS = conn.prepareStatement(countQueryBuilder.toString());
      PreparedStatement resultPS = conn.prepareStatement(resultQueryBuilder.toString());

      QueryHolder holder = new QueryHolder(countPS, resultPS);
      holder.setParamList(paramList);

      return holder;
    }
  }

  @Override
  public SearchResult<DeviceEntity> searchDevices(QueryBuilderResult query, PaginationInfo paginationInfo) {
    Connection con = null;
    int totalCount = 0;
    List<DeviceEntity> returnList = new ArrayList<DeviceEntity>(paginationInfo != null ? paginationInfo.getTakeSize(): 100);
    try {
      con = provider.getConnection();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Executing built query:{} \n count query:{}", query.getQuery(), query.getCountQuery());
      }
      {
        //do the count first
        PreparedStatement countPS = con.prepareStatement(query.getCountQuery());
        //set the values
        int index = 1;
        for (QueryParam param : query.getParamList()) {
          if (param.isForCount()) {
            QueryParam.setParameterValue(param, index++, countPS);
          }
        }
        ResultSet countRS = countPS.executeQuery();
        if (countRS.next()) {
          totalCount = countRS.getInt(1);
        }
        CloseUtil.close(LOGGER, countRS);
        CloseUtil.close(LOGGER, countPS);
      }
      {
        //do the actual query
        PreparedStatement resultPS = con.prepareStatement(query.getQuery());
        int rindex = 1;
        for (QueryParam param : query.getParamList()) {
          QueryParam.setParameterValue(param, rindex++, resultPS);
        }
        ResultSet resultSet = resultPS.executeQuery();

        while (resultSet.next()) {
          returnList.add(new DeviceEntity.DeviceEntityBuilder().build(resultSet));
        }
        resultSet.close();
        resultPS.close();
        CloseUtil.close(LOGGER, resultSet);
        CloseUtil.close(LOGGER, resultPS);
      }
      con.close();
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, con);
    }
    SearchResult<DeviceEntity> results = new SearchResult<DeviceEntity>();
    results.setResults(returnList);
    results.setTotal(totalCount);
    if (paginationInfo != null) {
      results.setSize(paginationInfo.getTakeSize());
      results.setOffset(paginationInfo.getSkipSize());
    }
    return results;
  }

  public DevReg getRequestFromEntity(DeviceEntity entity) {
    DevReg request = new DevReg();
    request.setDisplayName(entity.getName());
    request.setPhoneNumber(entity.getPhoneNumber());
    request.setApiKey(entity.getClientToken());
    request.setCarrierInfo(entity.getCarrierInfo());
    request.setDevId(entity.getDeviceId());
    request.setOsType(entity.getOsType().name());
    request.setOsVersion(entity.getOsVersion());
    return request;
  }
}

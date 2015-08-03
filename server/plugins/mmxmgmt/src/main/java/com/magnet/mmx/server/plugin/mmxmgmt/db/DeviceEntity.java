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
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.api.v1.protocol.DeviceInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Object that represents rows in the mmxDevice table
 */
public class DeviceEntity {
  private int id;
  private String ownerId;
  private String deviceId;
  private OSType osType;
  private PushType tokenType;
  private String clientToken;
  private String name;
  private String osVersion;
  private String appId;
  private Date created;
  private Date updated;
  private DeviceStatus status;
  private String phoneNumber;
  private String carrierInfo;
  private String phoneNumberRev;
  private Version protocolVersion;
  private PushStatus pushStatus;

  public int getId() {
    return id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public OSType getOsType() {
    return osType;
  }

  public void setOsType(OSType osType) {
    this.osType = osType;
  }

  public PushType getTokenType() {
    return tokenType;
  }

  public void setTokenType(PushType tokenType) {
    this.tokenType = tokenType;
  }

  public String getClientToken() {
    return clientToken;
  }

  public void setClientToken(String clientToken) {
    this.clientToken = clientToken;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOsVersion() {
    return osVersion;
  }

  public void setOsVersion(String osVersion) {
    this.osVersion = osVersion;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getUpdated() {
    return updated;
  }

  public void setUpdated(Date updated) {
    this.updated = updated;
  }

  public DeviceStatus getStatus() {
    return status;
  }

  public void setStatus(DeviceStatus status) {
    this.status = status;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getCarrierInfo() {
    return carrierInfo;
  }

  public void setCarrierInfo(String carrierInfo) {
    this.carrierInfo = carrierInfo;
  }

  public String getPhoneNumberRev() {
    return phoneNumberRev;
  }

  public void setPhoneNumberRev(String phoneNumberRev) {
    this.phoneNumberRev = phoneNumberRev;
  }

  public Version getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(Version protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public PushStatus getPushStatus() {
    return pushStatus;
  }

  public void setPushStatus(PushStatus pushStatus) {
    this.pushStatus = pushStatus;
  }

  public static class DeviceEntityBuilder {

    /**
     * Build the DeviceEntity using the result set.
     *
     * @param rs not null result set. We expect all the device entity columns to be in the result set.
     * @return
     */
    public DeviceEntity build(ResultSet rs) throws SQLException {
      DeviceEntity deviceEntity = buildLimited(rs);
      String phoneNumberRev = rs.getString("phoneNumberRev");
      deviceEntity.setPhoneNumberRev(phoneNumberRev);
      return deviceEntity;
    }

    /**
     * Build device entity with out including the reversed phone number
     * @param rs
     * @return
     * @throws SQLException
     */
    public DeviceEntity buildLimited(ResultSet rs) throws SQLException {
      int id = rs.getInt("id");
      String name = rs.getString("name");
      String appId = rs.getString("appId");
      String owner = rs.getString("ownerJid");
      OSType osType = OSType.valueOf(rs.getString("osType"));
      String deviceId = rs.getString("deviceId");
      PushType tokenType = Helper.enumeratePushType(rs.getString("tokenType"));
      String token = rs.getString("clientToken");
      String version = rs.getString("versionInfo");
      String model = rs.getString("modelInfo");
      Date created = rs.getTimestamp("dateCreated");
      Date updated = rs.getTimestamp("dateUpdated");
      DeviceStatus status = DeviceStatus.valueOf(rs.getString("status"));
      String phoneNumber = rs.getString("phoneNumber");
      String carrierInfo = rs.getString("carrierInfo");

      int major = rs.getInt("protocolVersionMajor");
      int minor = rs.getInt("protocolVersionMinor");
      //for devices for which major and minor are not set
      //we will set version for those devices to major:0
      //minor:0
      Version protocolVersion = new Version(major, minor);
      String pushStatusValue = rs.getString("pushStatus");
      PushStatus pushStatus = null;

      if (!rs.wasNull()) {
        pushStatus = Helper.enumeratePushStatus(pushStatusValue);
      }

      DeviceEntity deviceEntity = new DeviceEntity();
      deviceEntity.setId(id);
      deviceEntity.setDeviceId(deviceId);
      deviceEntity.setOsVersion(version);
      deviceEntity.setTokenType(tokenType);
      deviceEntity.setOwnerId(owner);
      deviceEntity.setAppId(appId);
      deviceEntity.setName(name);
      deviceEntity.setClientToken(token);
      deviceEntity.setOsType(osType);
      deviceEntity.setCreated(created);
      deviceEntity.setUpdated(updated);
      deviceEntity.setStatus(status);
      deviceEntity.setPhoneNumber(phoneNumber);
      deviceEntity.setCarrierInfo(carrierInfo);
      deviceEntity.setProtocolVersion(protocolVersion);
      deviceEntity.setPushStatus(pushStatus);
      return deviceEntity;
    }

    public DeviceEntity buildLimited(ResultSet rs, String prefix) throws SQLException {
      int id = rs.getInt(prefix + "id");
      String name = rs.getString(prefix +"name");
      String appId = rs.getString(prefix +"appId");
      String owner = rs.getString(prefix +"ownerJid");
      OSType osType = OSType.valueOf(rs.getString(prefix +"osType"));
      String deviceId = rs.getString(prefix +"deviceId");
      PushType tokenType = Helper.enumeratePushType(rs.getString(prefix +"tokenType"));
      String token = rs.getString(prefix +"clientToken");
      String version = rs.getString(prefix +"versionInfo");
      String model = rs.getString(prefix +"modelInfo");
      Date created = rs.getTimestamp(prefix +"dateCreated");
      Date updated = rs.getTimestamp(prefix +"dateUpdated");
      DeviceStatus status = DeviceStatus.valueOf(rs.getString(prefix + "status"));
      String phoneNumber = rs.getString(prefix +"phoneNumber");
      String carrierInfo = rs.getString(prefix +"carrierInfo");

      int major = rs.getInt(prefix +"protocolVersionMajor");
      int minor = rs.getInt(prefix + "protocolVersionMinor");
      //for devices for which major and minor are not set
      //we will set version for those devices to major:0
      //minor:0
      Version protocolVersion = new Version(major, minor);
      String pushStatusValue = rs.getString(prefix + "pushStatus");
      PushStatus pushStatus = null;

      if (!rs.wasNull()) {
        pushStatus = Helper.enumeratePushStatus(pushStatusValue);
      }

      DeviceEntity deviceEntity = new DeviceEntity();
      deviceEntity.setId(id);
      deviceEntity.setDeviceId(deviceId);
      deviceEntity.setOsVersion(version);
      deviceEntity.setTokenType(tokenType);
      deviceEntity.setOwnerId(owner);
      deviceEntity.setAppId(appId);
      deviceEntity.setName(name);
      deviceEntity.setClientToken(token);
      deviceEntity.setOsType(osType);
      deviceEntity.setCreated(created);
      deviceEntity.setUpdated(updated);
      deviceEntity.setStatus(status);
      deviceEntity.setPhoneNumber(phoneNumber);
      deviceEntity.setCarrierInfo(carrierInfo);
      deviceEntity.setProtocolVersion(protocolVersion);
      deviceEntity.setPushStatus(pushStatus);

      return deviceEntity;
    }
  }

  @Override
  public String toString() {
    return "DeviceEntity{" +
            "id=" + id +
            ", ownerId='" + ownerId + '\'' +
            ", deviceId='" + deviceId + '\'' +
            ", osType=" + osType +
            ", tokenType=" + tokenType +
            ", clientToken='" + clientToken + '\'' +
            ", name='" + name + '\'' +
            ", osVersion='" + osVersion + '\'' +
            ", appId='" + appId + '\'' +
            ", created=" + created +
            ", updated=" + updated +
            ", status=" + status +
            ", phoneNumber='" + phoneNumber + '\'' +
            ", carrierInfo='" + carrierInfo + '\'' +
            ", phoneNumberRev='" + phoneNumberRev + '\'' +
            '}';
  }

  /**
   * Class representing the SDK Version.
   */
  public static class Version {
    private int major;
    private int minor;

    public Version(int major, int minor) {
      this.major = major;
      this.minor = minor;
    }

    public int getMajor() {
      return major;
    }

    public int getMinor() {
      return minor;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Version version = (Version) o;

      if (major != version.major) return false;
      if (minor != version.minor) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = major;
      result = 31 * result + minor;
      return result;
    }
  }

  public static DeviceInfo toDeviceInfo(DeviceEntity entity) {
    DeviceInfo devInfo = new DeviceInfo()
        .setCarrierInfo(entity.getCarrierInfo())
        .setDeviceId(entity.getDeviceId())
        .setDisplayName(entity.getName())
        .setOsType(entity.getOsType() == null ?
            null : entity.getOsType().toString())
        .setOsVersion(entity.getOsVersion())
        .setPhoneNumber(entity.getPhoneNumber())
        .setPushType(entity.getTokenType() == null ?
            null : entity.getTokenType().toString())
        .setTags(null)
        .setDateRegistered(entity.getCreated())
        .setStatus(entity.getStatus().name())
        .setVersionMajor(entity.getProtocolVersion().getMajor())
        .setVersionMinor(entity.getProtocolVersion().getMinor())
        .setPushStatus(entity.getPushStatus() != null ? entity.getPushStatus().name():null);
    return devInfo;
  }

}

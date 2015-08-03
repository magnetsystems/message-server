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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO implementation.
 */
public class AppConfigurationEntityDAOImpl implements AppConfigurationEntityDAO {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppConfigurationEntityDAOImpl.class);
  private ConnectionProvider provider;

  private final String INSERT_UPDATE_SQL = "INSERT INTO mmxAppConfiguration (appId,configKey,configValue) " +
      " VALUES (?,?,?) " +
      " ON DUPLICATE KEY UPDATE configValue=? ";

  private final String SELECT_SQL = "SELECT id, appId, configKey, configValue FROM mmxAppConfiguration WHERE appId = ? ORDER BY configKey ";

  private final String SELECT_USING_KEY_SQL = "SELECT id, appId, configKey, configValue FROM mmxAppConfiguration WHERE appId = ? AND configKey = ? ";

  private final String DELETE_SQL = "DELETE FROM mmxAppConfiguration WHERE appId = ? AND configKey = ? ";
  /**
   * Constructor with the connection provider
   * @param provider
   */
  public AppConfigurationEntityDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public AppConfigurationEntity getConfiguration(String appId, String key) {
    AppConfigurationEntity config =  null;
    Connection connection = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      connection = provider.getConnection();
      pstmt = connection.prepareStatement(SELECT_USING_KEY_SQL);
      pstmt.setString(1, appId);
      pstmt.setString(2, key);
      rs = pstmt.executeQuery();
      if (rs.next()) {
        config = AppConfigurationEntity.build(rs);
      }
      rs.close();
      pstmt.close();
      connection.close();
      rs = null;
      pstmt = null;
      connection = null;
      return config;
    } catch (SQLException e) {
      LOGGER.warn("SQLException in getConfiguration while retrieving config for appId:{} and key:{}", appId, key, e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, connection);
    }
  }

  @Override
  public List<AppConfigurationEntity> getConfigurations(String appId) {
    List<AppConfigurationEntity> configList = new ArrayList<AppConfigurationEntity>(10);
    Connection connection = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      connection = provider.getConnection();
      pstmt = connection.prepareStatement(SELECT_SQL);
      pstmt.setString(1, appId);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        AppConfigurationEntity config = AppConfigurationEntity.build(rs);
        configList.add(config);
      }
      rs.close();
      pstmt.close();
      connection.close();
      rs = null;
      pstmt = null;
      connection = null;
      return configList;
    } catch (SQLException e) {
      LOGGER.warn("SQLException in getConfigurations", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, connection);
    }
  }

  @Override
  public void updateConfiguration(String appId, String key, String value) {
    Connection connection = null;
    PreparedStatement pstmt = null;
    try {
      connection = provider.getConnection();
      pstmt = connection.prepareStatement(INSERT_UPDATE_SQL);
      pstmt.setString(1, appId);
      pstmt.setString(2, key);
      pstmt.setString(3, value);
      pstmt.setString(4, value);
      pstmt.executeUpdate();
      pstmt.close();
      connection.close();
      pstmt = null;
      connection = null;
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception when updating configuration for appId:{} key:{} value:{}", appId, key, value, sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, connection);
    }
  }


  @Override
  public void deleteConfiguration(String appId, String key) {
    Connection connection = null;
    PreparedStatement pstmt = null;
    try {
      connection = provider.getConnection();
      pstmt = connection.prepareStatement(DELETE_SQL);
      pstmt.setString(1, appId);
      pstmt.setString(2, key);
      pstmt.executeUpdate();
      pstmt.close();
      connection.close();
      pstmt = null;
      connection = null;
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception when deleting configuration for appId:{} key:{} ", appId, key, sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, connection);
    }
  }
}

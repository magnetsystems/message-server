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

public class TopicRoleDAOImpl implements TopicRoleDAO {
  private static Logger LOGGER = LoggerFactory.getLogger(TopicRoleDAOImpl.class);
  private ConnectionProvider provider;

  private static final String GET_ROLES_FOR_TOPIC = "SELECT id, serviceID, nodeID, role, creationDate FROM mmxTopicRole WHERE serviceID = ? AND nodeID = ? ORDER BY role";
  private static final String ADD_ROLE_FOR_TOPIC = "INSERT INTO mmxTopicRole(serviceID, nodeID, role, creationDate) VALUES (?, ?, ?, now()) ON DUPLICATE KEY UPDATE id=id";



  public TopicRoleDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public List<String> getTopicRoles(String serviceId, String nodeId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<String> returnList = new ArrayList<String>(10);
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(GET_ROLES_FOR_TOPIC);
      pstmt.setString(1, serviceId);
      pstmt.setString(2, nodeId);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        String role = rs.getString("role");
        returnList.add(role);
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
  public void addTopicRoles(String serviceId, String nodeId, List<String> roles) {
    if (roles == null || roles.isEmpty()) {
      LOGGER.warn("addTopicRoles called with null or empty roles list");
      throw new IllegalArgumentException("roles list can't be empty");
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Adding roles:{} for topicId:{}", roles, nodeId);
    }
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = provider.getConnection();
      pstmt = con.prepareStatement(ADD_ROLE_FOR_TOPIC, PreparedStatement.RETURN_GENERATED_KEYS);
      for (String role : roles)  {
        pstmt.setString(1, serviceId);
        pstmt.setString(2, nodeId);
        pstmt.setString(3, role.toUpperCase());
        pstmt.executeUpdate();
        pstmt.clearParameters();
      }
      pstmt.close();
      con.close();
      pstmt = null;
      con = null;
    } catch (SQLException sqle) {
      LOGGER.warn("SQL Exception in adding roles to topic", sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

}

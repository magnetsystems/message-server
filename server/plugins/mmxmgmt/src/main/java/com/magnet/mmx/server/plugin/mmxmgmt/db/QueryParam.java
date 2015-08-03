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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * For Query parameters
*/
public class QueryParam {
  private int sqlType;
  private Object value;
  private boolean forCount = false;

  public QueryParam(int sqlType, Object value, boolean forCount) {
    this.sqlType = sqlType;
    this.value = value;
    this.forCount = forCount;
  }

  public QueryParam(int sqlType, Object value) {
    this (sqlType, value, false);
  }

  public int getSqlType() {
    return sqlType;
  }

  public Object getValue() {
    return value;
  }

  public boolean isForCount() {
    return forCount;
  }

  public static void setParameterValue (QueryParam param, int index, PreparedStatement statement) throws SQLException {
    if (param.getSqlType() == Types.VARCHAR) {
      statement.setString(index, (String) param.getValue());
    } else if (param.getSqlType() == Types.INTEGER) {
      statement.setInt(index, ((Integer) param.getValue()).intValue());
    } else if (param.getSqlType() == Types.BIGINT) {
      statement.setLong(index, ((Long) param.getValue()).longValue());
    } else {
      throw new DbInteractionException("Unknown parameter type:" + param.getSqlType());
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("QueryParam{");
    sb.append("sqlType=").append(sqlType);
    sb.append(", value=").append(value);
    sb.append(", forCount=").append(forCount);
    sb.append('}');
    return sb.toString();
  }
}

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

/**
 */
public class TagDaoEntry {
  private String columnName;
  private Object columnValue;
  private boolean selectNeeded;
  private Class type;

  public TagDaoEntry(String columnName, Object columnValue, Class type, boolean selectNeeded) {
    this.columnName = columnName;
    this.columnValue = columnValue;
    this.type = type;
    this.selectNeeded = selectNeeded;
  }

  public String getColumnName() {
    return columnName;
  }

  public Object getColumnValue() {
    return columnValue;
  }

  public Class getType() {
    return type;
  }

  public boolean isSelectNeeded() {
    return selectNeeded;
  }
}

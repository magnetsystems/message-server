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
import java.util.List;

/**
 * Class for holding prepared statements
 */
public class QueryHolder {
  private PreparedStatement countQuery;
  private PreparedStatement resultQuery;
  private List<QueryParam> paramList;

  public PreparedStatement getCountQuery() {
    return countQuery;
  }

  public PreparedStatement getResultQuery() {
    return resultQuery;
  }

  QueryHolder(PreparedStatement countQuery, PreparedStatement resultQuery) {
    this.countQuery = countQuery;
    this.resultQuery = resultQuery;
  }

  public List<QueryParam> getParamList() {
    return paramList;
  }

  public void setParamList(List<QueryParam> paramList) {
    this.paramList = paramList;
  }
}

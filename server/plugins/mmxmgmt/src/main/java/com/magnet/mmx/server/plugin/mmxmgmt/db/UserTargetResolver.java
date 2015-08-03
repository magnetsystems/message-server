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

import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Target;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.UserQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;

import java.util.List;

/**
 */
public class UserTargetResolver implements TargetResolver<UserEntity> {

  @Override
  public List<UserEntity> resolve(String appId, Target target) throws ResolutionException {
    if (target == null) {
      throw new ResolutionException("no valid targets defined");
    }
    UserQuery userQuery = target.getUserQuery();
    if (userQuery != null) {
      UserDAO userDAO = getUserDAO();
      UserQueryBuilder builder = new UserQueryBuilder();
      QueryBuilderResult query = builder.buildQuery(userQuery, appId);
      List<UserEntity> entityList = userDAO.getUsers(query);
      return entityList;
    } else {
      throw new ResolutionException("no valid targets defined");
    }
  }

  protected ConnectionProvider getConnectionProvider() {
    ConnectionProvider provider = new OpenFireDBConnectionProvider();
    return provider;
  }

  protected UserDAO getUserDAO() {
    ConnectionProvider provider = getConnectionProvider();
    UserDAO dao = new UserDAOImpl(provider);
    return dao;
  }
}

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
package com.magnet.mmx.server.plugin.mmxmgmt.handler;

import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * TODO: Use the one from Openfire ?
 */
public class UserRoleCache {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleCache.class);

  private static final String CACHE_NAME = "mmx.user.roles";
  private static  Cache<String, List<String>> userRoleCache;

  /**
   *
   * @param userName
   * @param roles
   */
  public static void cacheRoles (String userName, List<String> roles) {
    Cache<String, List<String>> userRoleCache = CacheFactory.createCache(CACHE_NAME);
    LOGGER.trace("Adding roles:{} to user:{}", roles, userName);
    userRoleCache.put(userName, roles);
  }

  /**
   * Get the roles for a particular userName. Username is the XID with the following format:
   * userID%appID
   * @param userName
   * @return
   */
  public static List<String> getRoles (String userName) {
    Cache<String, List<String>> userRoleCache = CacheFactory.createCache(CACHE_NAME);
    List<String> roles = userRoleCache.get(userName);
    LOGGER.trace("Returning roles:{} for user:{}", roles, userName);
    return roles;
  }

}
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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;

import java.util.concurrent.ExecutionException;

/**
 */
public class AppEntityDBLoadingEntityCache  implements DBEntityCache<AppEntity> {

  private static AppEntityDBLoadingEntityCache INSTANCE;
  private static int CACHE_SIZE = 100;

  private LoadingCache<String,AppEntity> cache;

  public AppEntityDBLoadingEntityCache(int size, CacheLoader<String, AppEntity> appEntityCacheLoader) {
    cache = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE)
        .build(appEntityCacheLoader);
  }

  @Override
  public AppEntity get(String key) {
    try {
      return cache.get(key);
    } catch (ExecutionException e) {
      return null;
    }
  }

  @Override
  public void purge(String key) {
    cache.refresh(key);
  }


  private static class AppEntityNotFoundException extends Exception {
    private AppEntityNotFoundException(String message) {
      super(message);
    }
  }

  public static class AppEntityDBLoader extends CacheLoader<String, AppEntity> {
    private ConnectionProvider provider;

    public AppEntityDBLoader (ConnectionProvider provider) {
      this.provider = provider;
    }

    public AppEntityDBLoader() {
      this (new OpenFireDBConnectionProvider());
    }


    public AppEntity load(String key) throws AppEntityNotFoundException {
      AppDAO appDAO = new AppDAOImpl(this.provider);
      AppEntity entity = appDAO.getAppForAppKey(key);
      if (entity == null) {
        throw new AppEntityNotFoundException(String.format("AppEntity with key:%s not found", key));
      }
      return entity;
    }
  }

}

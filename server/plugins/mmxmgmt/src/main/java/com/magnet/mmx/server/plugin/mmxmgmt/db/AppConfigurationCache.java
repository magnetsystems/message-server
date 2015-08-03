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

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Cache for App configuration values.
 * This is backed by database.
 * When cache values are updated/inserted client should call clear to force a refresh
 * of the cache.
 */
public class AppConfigurationCache {
  private final static Logger LOGGER = LoggerFactory.getLogger(AppConfigurationCache.class);


  private LoadingCache<ConfigKey, Optional<String>> cache;

  private AppConfigurationCache() {
    cache = CacheBuilder.newBuilder()
        .build(new AppConfigurationLoader(new OpenFireDBConnectionProvider()));
  }

  /**
   * Cache holder
   */
  private static class CacheHolder {
    private static final AppConfigurationCache INSTANCE = new AppConfigurationCache();
  }

  /**
   * Get the singleton instance of the app configuration cache.
   * @return
   */
  public static AppConfigurationCache getInstance() {
    return CacheHolder.INSTANCE;
  }

  /**
   * Private cache key.
   */
  private final static class ConfigKey {
    private String appId;
    private String key;

    public ConfigKey(String appId, String key) {
      this.appId = appId;
      this.key = key;
    }

    public String getAppId() {
      return appId;
    }

    public String getKey() {
      return key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ConfigKey configKey = (ConfigKey) o;

      if (!appId.equals(configKey.appId)) return false;
      return key.equals(configKey.key);

    }

    @Override
    public int hashCode() {
      int result = appId.hashCode();
      result = 31 * result + key.hashCode();
      return result;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("ConfigKey{");
      sb.append("appId='").append(appId).append('\'');
      sb.append(", key='").append(key).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }

  public static class AppConfigurationLoader extends CacheLoader<ConfigKey, Optional<String>> {
    private ConnectionProvider provider;


    public AppConfigurationLoader (ConnectionProvider provider) {
      this.provider = provider;
    }

    public AppConfigurationLoader() {
      this(new OpenFireDBConnectionProvider());
    }

    /**
     * Load the App Configuration for the key
     * @param configKey
     * @return configuration value.
     */
    public Optional<String> load(ConfigKey configKey)  {
      AppConfigurationEntityDAO appDAO = new AppConfigurationEntityDAOImpl(this.provider);
      AppConfigurationEntity entity = appDAO.getConfiguration(configKey.getAppId(), configKey.getKey());
      if (entity == null || entity.getValue() == null) {
        return Optional.fromNullable(null);
      } else {
        return Optional.fromNullable(entity.getValue());
      }
    }
  }


  /**
   * Get configuration value for the specified appId and key combination.
   * @param appId
   * @param key
   * @return Configuration value appId and key combination. null if none exists
   * If the specified combination isn't in the cache, this will attempt to load
   * it from the database.
   */
  public String getString(String appId, String key) {
    ConfigKey configKey = new ConfigKey(appId, key);
    try {
      Optional<String> optionalValue =  cache.get(configKey);
      if (!optionalValue.isPresent()) {
        LOGGER.info("Value for configKey:{} is null", configKey);
        return null;
      } else {
        return optionalValue.get();
      }
    } catch (ExecutionException e) {
      LOGGER.error("Exception in getting cache value for:{}",configKey,e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Clear the cached value for the appId and key combination.
   * @param appId
   * @param key
   */
  public void clear(String appId, String key) {
    ConfigKey configKey = new ConfigKey(appId, key);
    LOGGER.info("Invalidating configKey:{} ", configKey);
    cache.invalidate(configKey);
  }

}

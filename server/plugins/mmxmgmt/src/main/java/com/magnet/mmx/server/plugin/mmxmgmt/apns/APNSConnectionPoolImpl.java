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
package com.magnet.mmx.server.plugin.mmxmgmt.apns;

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.ApnsServiceBuilder;
import com.notnoop.exceptions.InvalidSSLConfig;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Pool for maintaining APNS service instances.
 * Using a cache ensures we are not opening and closing connections to the APNS servers repeatedly.
 */
public class APNSConnectionPoolImpl implements APNSConnectionPool {
  private static Logger LOGGER = LoggerFactory.getLogger(APNSConnectionPoolImpl.class);

  private GenericKeyedObjectPool<APNSConnectionKey, APNSConnection> connectionPool;

  private static APNSConnectionPoolImpl instance = new APNSConnectionPoolImpl();
  private static AtomicBoolean initialized = new AtomicBoolean(false);

  APNSConnectionPoolImpl() {
  }

  /**
   * Get the singleton instance of the connection pool
   * @return
   */
  public static APNSConnectionPoolImpl getInstance() {
    return instance;
  }

  /**
   * Lifecycle method.
   * Initializes the connection pool with the default object factory and configuration.
   * @throws java.lang.IllegalStateException if the pool instance has already been initialized.
   */
  public static void initialize() {
    GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
    config.setMaxIdlePerKey(1);
    config.setMaxTotalPerKey(5);
    config.setMaxTotal(50);
    initialize(new APNSConnectionKeyedPooledObjectFactory(new OpenFireDBConnectionProvider()), config);
  }

  public static void initialize(GenericKeyedObjectPoolConfig configuration) {
    if (initialized.get()) {
      throw new IllegalStateException("Can't initialize multiple times");
    }
    initialize(new APNSConnectionKeyedPooledObjectFactory(new OpenFireDBConnectionProvider()), configuration);
  }

  /**
   * Lifecycle method. Initializes the pool using the supplied KeyedPooledObjectFactory and the configuration.
   * @param factory
   * @param configuration
   * @throws java.lang.IllegalStateException if the pool instance has already been initialized.
   */
  public static void initialize(KeyedPooledObjectFactory<APNSConnectionKey, APNSConnection> factory, GenericKeyedObjectPoolConfig configuration) {
    if (initialized.get()) {
      throw new IllegalStateException("Can't initialize multiple times");
    }
    instance.connectionPool = new GenericKeyedObjectPool<APNSConnectionKey, APNSConnection>(factory, configuration);
    initialized.set(true);
    LOGGER.info("APNS Connection pool is initialized");
  }

  /**
   * Tear down the pool.
   * Clears any idle objects in the pool.
   */
  public static void teardown() {
    if (!initialized.get()) {
      throw new IllegalStateException("Can't teardown a pool that hasn't be initialized");
    }
    LOGGER.info("APNS Connection pool closing...");
    instance.connectionPool.clear();
    instance.connectionPool.close();
    LOGGER.info("APNS Connection pool closed.");

  }
  @Override
  public APNSConnection getConnection(String appId, boolean productionCert)  {
    if (!initialized.get()) {
      throw new IllegalStateException("Pool not initialized");
    }
    APNSConnectionKey key = new APNSConnectionKey(appId, productionCert);
    try {
      return connectionPool.borrowObject(key);
    } catch (Exception e) {
      LOGGER.warn("Couldn't get connection for key:" + key , e);
      return null;
    }
  }

  @Override
  public void returnConnection (APNSConnection connection) {
    APNSConnectionKey key = new APNSConnectionKey(connection.getAppId(), connection.isApnsProductionCert());
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Returning object with code:{} and key:{} to pool", connection.hashCode(), key);
    }
    connectionPool.returnObject(key, connection);
  }

  @Override
  public void remove(String appId, boolean productionCert) {
    APNSConnectionKey key = new APNSConnectionKey(appId, productionCert);
    LOGGER.info("Removing all connections for key:{}", key);
    connectionPool.clear(key);
  }

  /**
   * Protected class for defining the keys in the object pool
   */
  protected static class APNSConnectionKey {
    private String appId;
    private boolean production;

    protected APNSConnectionKey(String appId, boolean production) {
      if (appId == null) {
        throw new IllegalArgumentException("Bad appId value");
      }
      this.appId = appId;
      this.production = production;
    }

    public String getAppId() {
      return appId;
    }

    public boolean isProduction() {
      return production;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      APNSConnectionKey that = (APNSConnectionKey) o;

      if (production != that.production) return false;
      if (!appId.equals(that.appId)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = appId.hashCode();
      result = 31 * result + (production ? 1 : 0);
      return result;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("APNSConnectionKey{");
      sb.append("appId='").append(appId).append('\'');
      sb.append(", production=").append(production);
      sb.append('}');
      return sb.toString();
    }
  }

  /**
   * Factory that can build the APNS connections using the APNS Library and the AppEntity definition.
   */
  protected static class APNSConnectionKeyedPooledObjectFactory extends BaseKeyedPooledObjectFactory <APNSConnectionKey, APNSConnection> {
    private static Logger LOGGER = LoggerFactory.getLogger(APNSConnectionKeyedPooledObjectFactory.class);
    private ConnectionProvider provider;

    public APNSConnectionKeyedPooledObjectFactory(ConnectionProvider provider) {
      this.provider = provider;
    }

    @Override
    public void destroyObject(APNSConnectionKey key, PooledObject<APNSConnection> connection) throws
        Exception {
      APNSConnectionImpl wrappedConnection = (APNSConnectionImpl) connection.getObject();
      super.destroyObject(key, connection);
      wrappedConnection.close();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Closed APNS Connection for appId:{} and production:{}", key.getAppId(), key.isProduction());
      }
    }

    @Override
    public APNSConnection create(APNSConnectionKey key) throws Exception {
      LOGGER.info("Building APNS Connection for appId:{} and production:{}", key.getAppId(), key.isProduction());
      AppEntity appEntity = getAppEntity(key.getAppId());
      boolean apnsCertProduction = key.isProduction();

      byte[] cert = appEntity.getApnsCert();
      String password = appEntity.getApnsCertPassword();

      if (cert == null || cert.length == 0) {
        String template = "Certificate for app with id:%s is null or empty";
        throw new APNSConnectionException(String.format(template, key.getAppId()));
      }
      String md5String = DigestUtils.md5Hex(cert);
      LOGGER.info("MD5 for apns cert for key:{} is {}", key, md5String);

      if (password == null || password.isEmpty()) {
        String template = "Certificate password for app with id:%s is null or empty";
        throw new APNSConnectionException(String.format(template, key.getAppId()));
      }

      ApnsServiceBuilder builder =  null;
      try {
        builder = APNS.newService()
            .withCert(new ByteArrayInputStream(cert), password);
      } catch (InvalidSSLConfig t) {
        LOGGER.warn("Exception in building APNS service for key:{}", key, t);
        String template = "Invalid SSL Config for key:%s with exception:%s";
        throw new APNSConnectionException(String.format(template, key.getAppId(), t.getMessage()));
      }

      if (apnsCertProduction) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Using production apns destination");
        }
        builder.withAppleDestination(apnsCertProduction);
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Using sandbox apns destination");
        }
        builder.withAppleDestination(apnsCertProduction);
      }
      builder.withDelegate(new MMXAPNSDelegate(key));

      ApnsService apnsService = builder.build();
      APNSConnectionImpl apnsConnection = new APNSConnectionImpl(apnsService, key);
      apnsConnection.open();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Opened APNS Connection for appId:{} and production:{}", key.getAppId(), key.isProduction());
      }
      return apnsConnection;
    }

    @Override
    public PooledObject<APNSConnection> wrap(APNSConnection connection) {
      return new DefaultPooledObject<APNSConnection>(connection);
    }

    AppEntity getAppEntity(String appId) {
      AppDAO appDAO = new AppDAOImpl(provider);
      AppEntity appEntity = appDAO.getAppForAppKey(appId);
      return appEntity;
    }

  }

}

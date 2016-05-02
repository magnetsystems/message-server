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

package com.magnet.mmx.server.plugin.mmxmgmt;

import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSConnectionPoolImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSFeedbackProcessExecutionManager;
import com.magnet.mmx.server.plugin.mmxmgmt.bot.BotStarter;
import com.magnet.mmx.server.plugin.mmxmgmt.context.ContextDispatcherFactory;
import com.magnet.mmx.server.plugin.mmxmgmt.context.GeoEventDispatcher;
import com.magnet.mmx.server.plugin.mmxmgmt.context.IContextDispatcher;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.AppHandler;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.DeviceHandler;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXPubSubHandler;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXPushNSHandler;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXUserHandler;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXWakeupNSHandler;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MessageStateHandler;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MsgAckIQHandler;
import com.magnet.mmx.server.plugin.mmxmgmt.interceptor.MMXMessageHandlingRule;
import com.magnet.mmx.server.plugin.mmxmgmt.interceptor.MMXPacketInterceptor;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate.Hibernate;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.jpa.JPA;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXManagedConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXManagedConfigurationMBean;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.TimeoutExecutionManager;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.TimeoutExecutionManagerImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.WakeupConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.WakeupExecutionManager;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.WakeupExecutionManagerImpl;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.cluster.ClusterEventListener;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

/**
 * Plugin for custom IQ stanza for application registration and device
 * registration for GCM, or APNS.
 * <pre>
 *  <iq type="set" from="sender\40appKey@jabber-domain/resource" ...>
 *    <reg xlms="com.magnet:mmx:dev" type="application/json" ...>
 *    {
 *      "type": "gcm"
 *      "gcmToken": "..."
 *    }
 *    </reg>
 *  </iq>
 *
 *  <iq type="result" ...>
 *    <reg xlms="com.magnet:mmx:dev" type="application/json" ...>
 *    {
 *      "status": 0
 *    }
 *    </reg>
 *  </iq>
 * </pre>
 *
 */
public class MMXPlugin implements Plugin, ClusterEventListener {
  private IQHandler mIQAppRegHandler;
  private IQHandler mIQDevRegHandler;
  private IQHandler mIQUserRegHandler;
  private IQHandler mIQMessageStateHandler;
  private IQHandler mIQPubSubHandler;
  private IQHandler mIQWakeupNSHandler;
  private IQHandler mIQPushNSHandler;
  private IQHandler mIQMsgAckNSHandler;

  private MMXPacketInterceptor mmxPacketInterceptor;
  private WakeupExecutionManager wakeupExecutionManager = null;
  private TimeoutExecutionManager timeoutExecutionManager = null;
  private APNSFeedbackProcessExecutionManager apnsFeedbackProcessExecutionManager = null;

  private MMXAdminAPIServer adminAPIServer = null;
  private MMXPublicAPIServer publicAPIServer = null;

  private static final Logger Log = LoggerFactory.getLogger(MMXPlugin.class);
  private IContextDispatcher contextDispatcher;

  @Override
  public void initializePlugin(PluginManager manager, File pluginDirectory) {
    /*
      Initialize properties so that all the properties added by the plugin schema upgrade are loaded
     */
    JiveProperties.getInstance().init();
    //log the java version
    String version = System.getProperty("java.version");
    String vendor = System.getProperty("java.vendor");
    String vmName = System.getProperty("java.vm.name");
    Log.warn("Java details vendor:{} version:{} vmName:{}", vendor, version, vmName);

    {
      try {
        JiveGlobals.setProperty(MMXConfigKeys.MMX_VERSION, MMXVersion.getVersion());
        Log.debug("mmx properties updated");
      } catch (Exception e) {
        Log.error("Problem in reading java properties", e);
      }
    }

    Log.info("MMX version:" + MMXVersion.getVersion());

    /**
     * register the config MBean
     */
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName mbeanName = new ObjectName(MMXServerConstants.MMX_MBEAN_NAME);
      MMXManagedConfigurationMBean mbean = new MMXManagedConfiguration(MMXConfiguration.getConfiguration());
      mbs.registerMBean(mbean, mbeanName);
    } catch (Throwable t) {
      Log.warn("Configuration MBean registration failed", t);
    }

    // Add Context event dispatcher
    try {
      Class.forName(GeoEventDispatcher.class.getName());
      contextDispatcher = ContextDispatcherFactory.getInstance().getDispatcher((GeoEventDispatcher.class.getName()));
    } catch (IllegalAccessException e) {
      Log.error("event dispatcher error", e);
    } catch (InstantiationException e) {
      Log.error("event dispatcher error", e);
    } catch (ClassNotFoundException e) {
      Log.error("event dispatcher error", e);
    }

    XMPPServer server = XMPPServer.getInstance();
    IQRouter iqRouter = server.getIQRouter();

    // Add IQHandler to register an application for push notification and
    // generate a pair of app-key and api-key for the client app.
    mIQAppRegHandler = new AppHandler("appreg");
    mIQDevRegHandler = new DeviceHandler("devreg");
    mIQUserRegHandler = new MMXUserHandler("userreg");
    mIQMessageStateHandler = new MessageStateHandler("msgstate");
    mIQPubSubHandler = new MMXPubSubHandler("pubsub");

    mIQWakeupNSHandler = new MMXWakeupNSHandler("wakeupns");
    mIQPushNSHandler = new MMXPushNSHandler("pushns");
    mIQMsgAckNSHandler = new MsgAckIQHandler("msgack");

    mmxPacketInterceptor = new MMXPacketInterceptor(new MMXMessageHandlingRule());

    iqRouter.addHandler(mIQAppRegHandler);
    iqRouter.addHandler(mIQDevRegHandler);
    iqRouter.addHandler(mIQUserRegHandler);
    iqRouter.addHandler(mIQMessageStateHandler);
    iqRouter.addHandler(mIQPubSubHandler);
    iqRouter.addHandler(mIQWakeupNSHandler);
    iqRouter.addHandler(mIQPushNSHandler);
    iqRouter.addHandler(mIQMsgAckNSHandler);

    Log.info("App Management Plugin is initialized");

    InterceptorManager im = InterceptorManager.getInstance();
    im.addInterceptor(mmxPacketInterceptor);
    Log.info("MMXPacketInterceptor is initialized");

    if(ClusterManager.isClusteringEnabled()) {
      Log.debug("initializePlugin : clustering is available, timer tasks will start on joining cluster");
      ClusterManager.addListener(this);
    } else {
      Log.debug("initializePlugin : Clustering is disabled, starting scheduled tasks");
      startSchedulededTasks();
    }

    publicAPIServer = new MMXPublicAPIServer();
    publicAPIServer.start();

    //initialize the APNS connection pool.
    initializeAPNSConnectionPool();

    adminAPIServer = new MMXAdminAPIServer();
    adminAPIServer.start();

    if(!ClusterManager.isClusteringEnabled()) {
      Log.info("clustering is disabled and hence initializing bots.");
      initializeBots();
    } else {
      Log.info("clustering is enabled. Bot will be initialized after node joins the cluster.");
    }
//    //initialize hibernate
//    initializeHibernate();
    //initialize JPA
    initializeJPA();

  }
  private void initializeJPA() {

    String driver = JiveGlobals.getXMLProperty("database.defaultProvider.driver");
    String url = JiveGlobals.getXMLProperty("database.defaultProvider.serverURL");
    String username = JiveGlobals.getXMLProperty("database.defaultProvider.username");
    String password = JiveGlobals.getXMLProperty("database.defaultProvider.password");
    System.out.println(">>>>>>>>>>>>> driver = " + driver);
    System.out.println(">>>>>>>>>>>>> url = " + url);
    System.out.println(">>>>>>>>>>>>> username = " + username);
    System.out.println(">>>>>>>>>>>>> password = " + password);
    JPA.initialize(driver, url, username, password);
  }
  private void initializeHibernate() {

    String driver = JiveGlobals.getXMLProperty("database.defaultProvider.driver");
    String url = JiveGlobals.getXMLProperty("database.defaultProvider.serverURL");
    String username = JiveGlobals.getXMLProperty("database.defaultProvider.username");
    String password = JiveGlobals.getXMLProperty("database.defaultProvider.password");
    System.out.println(">>>>>>>>>>>>> driver = " + driver);
    System.out.println(">>>>>>>>>>>>> url = " + url);
    System.out.println(">>>>>>>>>>>>> username = " + username);
    System.out.println(">>>>>>>>>>>>> password = " + password);
    Hibernate.initialize(driver, url, username, password);
  }

  @Override
  public void destroyPlugin() {
    XMPPServer server = XMPPServer.getInstance();
    IQRouter iqRouter = server.getIQRouter();
    iqRouter.removeHandler(mIQAppRegHandler);
    iqRouter.removeHandler(mIQDevRegHandler);
    iqRouter.removeHandler(mIQUserRegHandler);
    iqRouter.removeHandler(mIQMessageStateHandler);
    iqRouter.removeHandler(mIQPubSubHandler);
    iqRouter.removeHandler(mIQWakeupNSHandler);
    iqRouter.removeHandler(mIQPushNSHandler);
    iqRouter.removeHandler(mIQMsgAckNSHandler);
    InterceptorManager.getInstance().removeInterceptor(mmxPacketInterceptor);
    wakeupExecutionManager.stopWakeupExecution();
    timeoutExecutionManager.stopTimeoutCheck();

    // shutdown geo event dispatcher
    contextDispatcher.shutdown();


    //Teardown the APNS Connection pool
    APNSConnectionPoolImpl.teardown();
    adminAPIServer.stop();
    publicAPIServer.stop();

    try {
      ObjectName mbeanName = new ObjectName(MMXServerConstants.MMX_MBEAN_NAME);
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      mbs.unregisterMBean(mbeanName);
    } catch(Exception e) {
      Log.error("destroyPlugin : error unregistering mbean={}", MMXServerConstants.MMX_MBEAN_NAME);
    }
    apnsFeedbackProcessExecutionManager.stop();
    Log.info("App Management Plugin is destroyed");
  }

  public void startSchedulededTasks() {
    Log.debug("startSchedulededTasks : starting scheduled tasks");
    int frequency = MMXConfiguration.getConfiguration().getInt(MMXConfigKeys.WAKEUP_FREQUENCY_KEY, MMXServerConstants.DEFAULT_WAKEUP_FREQUENCY);
    // how many seconds should we wait before starting the wakeup check
    final int initialDelay = MMXConfiguration.getConfiguration().getInt(MMXConfigKeys.WAKEUP_INITIAL_WAIT_KEY, MMXServerConstants.DEFAULT_WAKEUP_INITIAL_WAIT);
    try {
      startWakeupTask(frequency, initialDelay);
      startTimeoutExecutionTask(frequency, initialDelay);
      startApnsFeedbackProcess();
    } catch (Exception e) {
      Log.error("startSchedulededTasks : caught exception starting scheduled tasks", e);
    }
  }

  private void startWakeupTask(int frequency, final int initialDelay) throws Exception {
    Log.trace("startWakeupTask : frequency={}, initialDelay={}", frequency, initialDelay);
    wakeupExecutionManager = new WakeupExecutionManagerImpl();
    /*
     * how often should we run the wakeup check. Value should be in seconds
     */
    if (frequency <= 0) {
      Log.warn("Wakeup frequency value:{} is invalid. Resetting it to default value of:{}", frequency, MMXServerConstants.DEFAULT_WAKEUP_FREQUENCY);
      frequency = MMXServerConstants.DEFAULT_WAKEUP_FREQUENCY;
    }
    final int wkFrequency = frequency;
    WakeupConfig config = new WakeupConfig() {
      @Override
      public long getPeriod() {
        return wkFrequency;
      }

      @Override
      public long getInitialDelay() {
        return initialDelay;
      }

      @Override
      public int threadCount() {
        return 1;
      }

      /**
       * toString for the config.
       * @return
       */
      @Override
      public String toString() {
        final StringBuilder sb = new StringBuilder("WakeupConfig{");
        sb.append("period(seconds)=").append(getPeriod());
        sb.append(", InitialDelay(seconds)=").append(getInitialDelay());
        sb.append("}");
        return sb.toString();
      }
    };
    try {
      wakeupExecutionManager.startWakeupExecution(config);
    } catch (Exception e) {
      Log.warn("Exception in starting wakeup task", e);
      throw e;
    }
  }

  private void startTimeoutExecutionTask(int frequency, int initialDelay) {
    Log.trace("startTimeoutExecutionTask : frequency={}, initialDelay={}", frequency, initialDelay);
    timeoutExecutionManager = new TimeoutExecutionManagerImpl();
    timeoutExecutionManager.startTimeoutCheck(initialDelay + 40, frequency);
  }

  private void startApnsFeedbackProcess() {
    apnsFeedbackProcessExecutionManager = new APNSFeedbackProcessExecutionManager();

    int apnsFeedBackProcessInitialDelayMinutes = MMXConfiguration.getConfiguration().
            getInt(MMXConfigKeys.APNS_FEEDBACK_PROCESS_INITIAL_DELAY_MINUTES, MMXServerConstants.DEFAULT_APNS_FEEDBACK_PROCESS_INITIAL_DELAY_MINUTES);
    int apnsFeedBackProcessFrequencyMinutes = MMXConfiguration.getConfiguration().
            getInt(MMXConfigKeys.APNS_FEEDBACK_PROCESS_FREQUENCY_MINUTES, MMXServerConstants.DEFAULT_APNS_FEEDBACK_PROCESS_FREQUENCY_MINUTES);
    Log.trace("startApnsFeedbackProcess starting task apnsFeedBackProcessInitialDelayMinutes={}, apnsFeedBackProcessFrequencyMinutes={}",
            apnsFeedBackProcessInitialDelayMinutes, apnsFeedBackProcessFrequencyMinutes);

    apnsFeedbackProcessExecutionManager.start(apnsFeedBackProcessInitialDelayMinutes, apnsFeedBackProcessFrequencyMinutes);
  }

  public void initializeAPNSConnectionPool() {

    MMXConfiguration configuration = MMXConfiguration.getConfiguration();
    int maxObjectsPerKey = configuration.getInt(MMXConfigKeys.APNS_POOL_MAX_CONNECTIONS_PER_APP, MMXServerConstants.APNS_POOL_MAX_CONNECTIONS_PER_APP);
    int maxIdleObjectsPerKey = configuration.getInt(MMXConfigKeys.APNS_POOL_MAX_IDLE_CONNECTIONS_PER_APP, MMXServerConstants.APNS_POOL_MAX_IDLE_CONNECTIONS_PER_APP);
    int ttlForIdleObjectsInMinutes = configuration.getInt(MMXConfigKeys.APNS_POOL_IDLE_TTL_MINUTES, MMXServerConstants.APNS_POOL_IDLE_TTL_MINUTES);
    int maxTotal = configuration.getInt(MMXConfigKeys.APNS_POOL_MAX_TOTAL_CONNECTIONS, MMXServerConstants.APNS_POOL_MAX_TOTAL_CONNECTIONS);

    Log.info("Configuring APNS Connection pool with following values: maxObjects:{}, maxObjectsPerKey:{}, " +
        "maxIdleObjectsPerKey:{}, ttlForIdleObjectsInMinutes:{}", maxTotal, maxObjectsPerKey, maxIdleObjectsPerKey, ttlForIdleObjectsInMinutes);

    GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
    config.setMaxTotalPerKey(maxObjectsPerKey);
    config.setMaxTotal(maxTotal);
    config.setMaxIdlePerKey(maxIdleObjectsPerKey);
    config.setMinEvictableIdleTimeMillis(ttlForIdleObjectsInMinutes * 60 * 1000L);

    APNSConnectionPoolImpl.initialize(config);
  }


  protected void initializeBots() {
    final String BOT_LOCK = "BotStarterLock";
    Lock clusterLock;
    try {
      Log.trace("initializeBots : getting lock");
      clusterLock = CacheFactory.getLock(BOT_LOCK);
      BotStarter starter = new BotStarter(clusterLock);
      Executors.newSingleThreadExecutor().execute(starter);
      Log.trace("initializeBots: submitted the bot starter");
    } catch (Exception e) {
      Log.error("initializeBots : caught exception", e);
    }
  }

  @Override
  public void joinedCluster() {
    Log.info("joinedCluster : node has joined the cluster");
    startSchedulededTasks();
    //initialize the bots
    Log.info("Initializing the bots after joining the cluster");
    initializeBots();
  }

  @Override
  public void joinedCluster(byte[] bytes) {
    Log.debug("joinedCluster : id={}", bytes.toString());
  }

  @Override
  public void leftCluster() {
    Log.debug("leftCluster : the node has left the cluster");
  }

  @Override
  public void leftCluster(byte[] bytes) {
    Log.debug("leftCluster : id={} has left the cluster", bytes.toString());
  }

  @Override
  public void markedAsSeniorClusterMember() {
    Log.debug("markedAsSeniorClusterMember : node is not a senior cluster member");
  }

}

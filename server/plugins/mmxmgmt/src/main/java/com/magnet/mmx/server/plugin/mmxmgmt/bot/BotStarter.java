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
package com.magnet.mmx.server.plugin.mmxmgmt.bot;

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXClusterableTask;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXExecutors;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Clusterable task that can start the Bots for
 */
public class BotStarter extends MMXClusterableTask implements Runnable {
  private Logger LOGGER = LoggerFactory.getLogger(BotStarter.class);
  static final String THREAD_POOL_NAME = "BotStarter";
  static final int THREAD_POOL_SIZE = 5;
  /**
   * Lower cased app names for which we need to start the bots.
   */
  private static String[] BOT_APP_NAME_LIST = { MMXServerConstants.QUICKSTART_APP, MMXServerConstants.RPSLS_APP};

  //sort the list so we can use binary search.
  static {
    Arrays.sort(BOT_APP_NAME_LIST);
  }

  public BotStarter(Lock lock) {
    super(lock);
  }

  @Override
  public void run() {
    if (!canExecute()) {
      LOGGER.warn("BotStarter.run() : Unable to acquire clustered lock, not running");
      return;
    }

    LOGGER.info("BotStarter.run() : Successfully acquired BotStarter lock");

    ExecutorService executorService = MMXExecutors.getOrCreate(THREAD_POOL_NAME, THREAD_POOL_SIZE);
    long startTime = System.nanoTime();
    AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
    List<AppEntity> apps = appDAO.getAllApps();

    for (AppEntity app : apps) {
      String appName = app.getName().toLowerCase();

      if (isBotEnabled(appName)) {
        LOGGER.debug("Creating bot for app name:{} and id:{}", appName, app.getAppId());
        startApplicableBots(appName, app.getAppId(), executorService);
      } else {
        LOGGER.debug("Not creating bot for app name:{} and id:{}", appName, app.getId());
      }
    }
    long endTime = System.nanoTime();
    LOGGER.info("Completed run execution in {} milliseconds",
        TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
  }

  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }

  /**
   * Start applicable bots for app with supplied app information. Passed in executor service is used to submit the
   * bot starting task.
   * @param appName
   * @param appId
   * @param service - not null.
   * @return Future that can be used to get the status of the bot start task completion. null if no bots are designed
   * to be started for this app.
   */
  public static Future<Boolean> startApplicableBots (String appName, String appId, ExecutorService service) {
    Callable<Boolean> perAppBotStarter = null;
    if (appName != null) {
      String lowerCased = appName.toLowerCase();
      if (MMXServerConstants.QUICKSTART_APP.equals(lowerCased)) {
        perAppBotStarter = new PerAppBotStarter(appId);
      } else if (MMXServerConstants.RPSLS_APP.equals(lowerCased)) {
        perAppBotStarter = new RPSLBotStarter(appId);
      }
      return perAppBotStarter != null ? service.submit(perAppBotStarter) : null;
    } else {
      return null;
    }
  }


  /**
   * Check if app with supplied name is bot enabled.
   *
   * @param appName
   * @return true if it is enabled and false otherwise.
   */
  public static boolean isBotEnabled(String appName) {
    if (appName == null) {
      return false;
    }
    int index = Arrays.binarySearch(BOT_APP_NAME_LIST, appName.toLowerCase());
    return index > -1;
  }


  public static class PerAppBotStarter implements Callable<Boolean> {
    private Logger LOGGER = LoggerFactory.getLogger(PerAppBotStarter.class);
    private String botAppId;

    public PerAppBotStarter(String appId) {
      this.botAppId = appId;
    }

    /**
     * Create the bots for the app and return true.
     *
     * @return
     * @throws Exception
     */
    @Override
    public Boolean call() {
      LOGGER.warn("Registering a bot for botAppId:{}", botAppId);
      BotRegistration registration = new BotRegistrationImpl();
      registration.registerBot(botAppId, MMXServerConstants.AMAZING_BOT_NAME, new BotRegistrationImpl.AmazingBotProcessor());
      registration.registerBot(botAppId, MMXServerConstants.ECHO_BOT_NAME, new BotRegistrationImpl.EchoBotProcessor());
      LOGGER.warn("Bot registered for botAppId:{}", botAppId);
      return Boolean.TRUE;
    }
  }

  /**
   * Specialized starter for RPSLS bot.
   */
  public static class RPSLBotStarter implements Callable<Boolean> {
    private Logger LOGGER = LoggerFactory.getLogger(RPSLBotStarter.class);
    private String botAppId;

    public RPSLBotStarter(String appId) {
      this.botAppId = appId;
    }

    /**
     * Create the bots for the app and return true.
     *
     * @return
     * @throws Exception
     */
    @Override
    public Boolean call() {
      LOGGER.warn("Registering a bot for botAppId:{}", botAppId);
      BotRegistration registration = new BotRegistrationImpl();
      registration.registerBot(botAppId, MMXServerConstants.PLAYER_BOT_NAME, new RPSLSPlayerBotProcessor());
      LOGGER.warn("Bot registered for botAppId:{}", botAppId);
      return Boolean.TRUE;
    }
  }
}

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
package com.magnet.mmx.server.plugin.mmxmgmt.wakeup;

import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXClusterableTask;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.locks.Lock;

/**
 * Processor that updates messages and marks them as timed out that have timed out.
 */
public class TimeoutProcessor extends MMXClusterableTask implements Runnable {
  private static Logger LOGGER = LoggerFactory.getLogger(TimeoutProcessor.class);

  private static final int DEFAULT_TIMEOUT_MINUTES = 180;

  public TimeoutProcessor(Lock lock) {
    super(lock);
  }

  @Override
  public void run() {
    if(!canExecute()) {
      LOGGER.trace("TimeoutProcessor.run() : Unable to acquire clustered lock, not running");
      return;
    }

    LOGGER.debug("TimeoutProcessor.run() : Successfully acquired TimeoutProcessor lock");

    Date now = new Date();
    int timeoutMinutes = MMXConfiguration.getConfiguration().getInt(MMXConfigKeys.MESSAGE_TIMEOUT_MINUTES, DEFAULT_TIMEOUT_MINUTES);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Processing timeout processor");
      LOGGER.debug("Timeout period in minutes:" + timeoutMinutes);
    }
    long startTime = System.nanoTime();
    MessageDAO messageDAO = getMessageDAO();
    long utcTimeInSeconds = now.getTime() / 1000L;
    int messageCount = messageDAO.messageTimeout(utcTimeInSeconds, timeoutMinutes);
    long endTime = System.nanoTime();
    long delta = endTime - startTime;
    LOGGER.info("Completed timeout processing");
    String template = "Processed [%d] timeout messages in [%d] milliseconds";
    LOGGER.info(String.format(template, messageCount, (delta / 1000)));
  }


  public MessageDAO getMessageDAO() {
    return new MessageDAOImpl(new OpenFireDBConnectionProvider());
  }

}

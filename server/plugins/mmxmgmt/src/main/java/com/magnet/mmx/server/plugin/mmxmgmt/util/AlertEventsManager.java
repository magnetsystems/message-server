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

import com.google.common.base.Strings;
import com.google.common.collect.EvictingQueue;
import com.magnet.mmx.server.plugin.mmxmgmt.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 */
public class AlertEventsManager {
  private static final ExecutorService service = Executors.newSingleThreadScheduledExecutor();
  private static Future<Boolean> emailTaskFuture = null;
  private static final Logger LOGGER = LoggerFactory.getLogger(AlertEventsManager.class);

  private static long lastEmailSent = 0L;
  
  private static Lock queuesLock = new ReentrantLock();

  public static EvictingQueue<MMXXmppRateExceededEvent> inAppEventQueue =  EvictingQueue.<MMXXmppRateExceededEvent>create(10);
  public static EvictingQueue<MMXHttpRateExceededEvent> pushEventQueue = EvictingQueue.<MMXHttpRateExceededEvent>create(10);
  public static EvictingQueue<MMXMaxAppLimitReachedEvent> appLimitEventQueue = EvictingQueue.<MMXMaxAppLimitReachedEvent>create(10);
  public static EvictingQueue<MMXMaxDevicesPerAppLimitReachedEvent> devicesLimitQueue = EvictingQueue.<MMXMaxDevicesPerAppLimitReachedEvent>create(10);

  public static void post(MMXEvent event) {
    LOGGER.trace("post : event={}", event);
    if (queuesLock.tryLock()) {
      try {
        LOGGER.trace("post : acquired lock event={}", event);
        if(event instanceof MMXXmppRateExceededEvent) {
          inAppEventQueue.add((MMXXmppRateExceededEvent)event);
        } else if(event instanceof MMXHttpRateExceededEvent) {
          pushEventQueue.add((MMXHttpRateExceededEvent) event);
        } else if (event instanceof MMXMaxAppLimitReachedEvent) {
          appLimitEventQueue.add((MMXMaxAppLimitReachedEvent) event);
        } else if(event instanceof MMXMaxDevicesPerAppLimitReachedEvent) {
          devicesLimitQueue.add((MMXMaxDevicesPerAppLimitReachedEvent) event);
        }
      } finally {
        queuesLock.unlock();
      }
    }

    boolean alertEnabled = Boolean.parseBoolean(MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_ENABLED));
    LOGGER.trace("post : alertEnabled={}", alertEnabled);
    if (alertEnabled) {
      if(emailTaskFuture == null) {
        LOGGER.trace("post : emailTaskFuture is null, launching task for first time");
        emailTaskFuture = service.submit(new EmailSenderTask());
      } else if(emailTaskFuture.isDone()) {
        if(interEmailTimeElapsed()) {
          LOGGER.trace("post : emailTaskFuture is not in progress and inter email time elapsed, scheduling email task");
          emailTaskFuture = service.submit(new EmailSenderTask());
        } else {
          LOGGER.trace("post : inter email time not elapsed, check after a minute");
        }
      } else {
        LOGGER.trace("post : emailTask has already been submitted, ignoring");
      }
    } else {
      LOGGER.trace("post : alerts are disabled alertsEnabled={}", alertEnabled);
    }
  }

  private static void drainEventsToList(List<MMXEvent> eventList) {
    drainQueueToList(eventList, inAppEventQueue);
    drainQueueToList(eventList, pushEventQueue);
    drainQueueToList(eventList, appLimitEventQueue);
    drainQueueToList(eventList, devicesLimitQueue);
  }

  private static void drainQueueToList(List<MMXEvent> eventList, EvictingQueue queue) {
    int size = queue.size();
    for(int i=0; i < size; i++) {
    MMXEvent event = (MMXEvent)queue.poll();
    if(event != null)
      eventList.add(event);
   }
  }

  private static class EmailSenderTask implements Callable<Boolean> {
    @Override
    public Boolean call() {
      try {
        List<MMXEvent> eventList = new ArrayList<MMXEvent>();
        AlertEventsManager.drainEventsToList(eventList);
        String body = EmailBodyCreationUtil.getBodyFromEvents(eventList);
        if(eventList.size() > 0 && !Strings.isNullOrEmpty(body)) {
          new MMXEmailSender().sendToBccOnly(body);
          lastEmailSent = System.currentTimeMillis();
        } else {
          LOGGER.trace("call : event list is empty");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return true;
    }
  }

  private static boolean interEmailTimeElapsed() {
    long period = MMXConfiguration.getConfiguration().getLong(MMXConfigKeys.ALERT_INTER_EMAIL_TIME_MINUTES, MMXServerConstants.DEFAULT_INTER_EMAIL_TIME_MINUTES);
    if((System.currentTimeMillis() - lastEmailSent >= TimeUnit.MINUTES.toMillis(period)) || lastEmailSent == 0L)
      return true;
    return false;
  }
}

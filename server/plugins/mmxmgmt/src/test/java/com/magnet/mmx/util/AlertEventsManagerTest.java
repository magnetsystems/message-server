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
package com.magnet.mmx.util;

import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXXmppRateExceededEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXMaxAppLimitReachedEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXMaxDevicesPerAppLimitReachedEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXHttpRateExceededEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AlertEventsManager;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXEmailSender;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class AlertEventsManagerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(AlertEventsManagerTest.class);
  private long lastSent = 0L;
  private List<Long> elapsedTimes = new ArrayList<Long>();


  public void setup() {
    setupMocks();
  }

  private void setupMocks() {
    new MockUp<MMXEmailSender>() {
      @Mock
      public void sendToBccOnly(String body) {
        LOGGER.trace("sendToBccOnly : body={}", body);
        if(lastSent != 0) {
          long elapsedTime = (System.currentTimeMillis() - lastSent) / TimeUnit.MINUTES.toMillis(1);
          elapsedTimes.add(elapsedTime);
          LOGGER.trace("sendToBccOnly : elapsedTime={} body={}", elapsedTime, body);
        }
        lastSent = System.currentTimeMillis();
      }
    };
  }


  public void testInterEmailTime() {
    setupMocks();
    long testDurationMinutes = 5;
    MMXConfiguration.getConfiguration().setValue(MMXConfigKeys.ALERT_EMAIL_ENABLED, "true");
    MMXConfiguration.getConfiguration().setValue(MMXConfigKeys.ALERT_INTER_EMAIL_TIME_MINUTES, "1");
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    ScheduledFuture<?> future = executorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        AlertEventsManager.post(getRandomEvent());
      }
    }, 0L, 500, TimeUnit.MILLISECONDS);
    List<ScheduledFuture<?>> list = new ArrayList<ScheduledFuture<?>>();
    list.add(future);
    executorService.schedule(new StopTestTask(list), testDurationMinutes, TimeUnit.MINUTES);
    while(!future.isDone());
    LOGGER.trace("testInterEmailTime : average inter email time = {}", getAvg(elapsedTimes));
  }

  private MMXEvent getRandomEvent() {
    int index = RandomUtils.nextInt(4);
    switch(index) {
      case 0:
        return new MMXHttpRateExceededEvent("aaaaa", 100);
      case 1:
        return new MMXXmppRateExceededEvent("bbbbb", 200);
      case 2:
        return new MMXMaxAppLimitReachedEvent(10, "johndoe");
      case 3:
      default:
        return new MMXMaxDevicesPerAppLimitReachedEvent("aaaaa", 1000);
    }
  }

  private class StopTestTask implements Runnable {
    private final List<ScheduledFuture<?>> futuresList;

    public StopTestTask(List<ScheduledFuture<?>> futuresList) {
      this.futuresList = futuresList;
    }

    @Override
    public void run() {
      for(ScheduledFuture<?> f : futuresList) {
        if(f != null)
          f.cancel(true);
      }
    }
  }

  private double getAvg(List<Long> elapsedTimes) {
    double avg = 0.0;
    for(Long l : elapsedTimes) {
        avg+=l.doubleValue();
    }
    return avg / elapsedTimes.size();
  }
}

package com.magnet.yak.functional.mob2740;

import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sdatar on 6/3/15.
 */
public class BaseRateTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseRateTest.class);
  private ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(3);
  private static String baseUri = "http://localhost:5220/mmxmgmt/api/v1/topicssummary?topicName=BLUESKY";
  private long testDuration = 5L;
  private ScheduledFuture<?> scheduledFuture1;
  private ScheduledFuture<?> scheduledFuture2;
  private AtomicInteger count1 = new AtomicInteger(0);
  private AtomicInteger count2 = new AtomicInteger(0);
  private AtomicInteger successCount1 = new AtomicInteger(0);
  private AtomicInteger successCount2 = new AtomicInteger(0);
  private AtomicInteger failureCount1 = new AtomicInteger(0);
  private AtomicInteger failureCount2 = new AtomicInteger(0);
  private Runnable runnable1;
  private Runnable runnable2;

  public BaseRateTest(Runnable runnable1, Runnable runnable2) {
    this.runnable1 = runnable1;
    this.runnable2 = runnable2;
  }

  public void start() {
    scheduledFuture1 = scheduledService.scheduleAtFixedRate(runnable1,
            0L, 10L, TimeUnit.SECONDS);

    scheduledFuture2 = scheduledService.scheduleAtFixedRate(runnable2,
            0L, 2L, TimeUnit.SECONDS);

    scheduledService.schedule(new TestTerminator(), testDuration, TimeUnit.MINUTES);
  }

  private class TestTerminator implements Runnable {
    @Override
    public void run() {
      scheduledFuture1.cancel(true);
      scheduledFuture2.cancel(true);
      logStats();
      try {
        Unirest.shutdown();
        scheduledService.shutdownNow();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void logStats() {
    int successPercentage1 = successCount1.get() * 100 / count1.get();
    int successPercentage2 = successCount2.get() * 100 / count2.get();
    LOGGER.debug("logStats : successPercentage1={}, successPercentage2={}",
            successPercentage1, successPercentage2);
  }
  
}

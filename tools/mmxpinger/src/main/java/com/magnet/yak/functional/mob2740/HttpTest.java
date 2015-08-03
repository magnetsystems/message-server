package com.magnet.yak.functional.mob2740;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sdatar on 6/3/15.
 */
public class HttpTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpTest.class);
  private ScheduledExecutorService service =Executors.newScheduledThreadPool(3);
  private static String baseUri = "http://localhost:5220/mmxmgmt/api/v1/topicssummary?topicName=BLUESKY";
  private long testDuration = 1L;
  private ScheduledFuture<?> scheduledFuture1;
  private ScheduledFuture<?> scheduledFuture2;
  private AtomicInteger count1 = new AtomicInteger(0);
  private AtomicInteger count2 = new AtomicInteger(0);
  private AtomicInteger successCount1 = new AtomicInteger(0);
  private AtomicInteger successCount2 = new AtomicInteger(0);
  private AtomicInteger failureCount1 = new AtomicInteger(0);
  private AtomicInteger failureCount2 = new AtomicInteger(0);

  public static void main(String[] args) {
    new HttpTest().start();
  }

  public void start() {
    scheduledFuture1 = service.scheduleAtFixedRate(new HttpRunner(
                    new AppConfig("d7iah7hilv", "69730ee6-a8af-4fec-af0f-49f8b4f287f0"), 10,
                    count1, successCount1, failureCount1),
            0L, 10L, TimeUnit.SECONDS);

    scheduledFuture2 = service.scheduleAtFixedRate(new HttpRunner(
                    new AppConfig("7d9iah7hbha", "2587935a-b764-4fc9-bb8f-2ef6e26f9be6"), 1,
                    count2, successCount2, failureCount2),
            0L, 2L, TimeUnit.SECONDS);

    service.schedule(new TestTerminator(), testDuration, TimeUnit.MINUTES);

  }

  private class TestTerminator implements Runnable {
    @Override
    public void run() {
      scheduledFuture1.cancel(true);
      scheduledFuture2.cancel(true);
      logStats();
    }
  }

  private void logStats() {
    int successPercentage1 = successCount1.get() * 100 / count1.get();
    int successPercentage2 = successCount2.get() * 100 / count2.get();
    LOGGER.debug("logStats : successPercentage1={}, successPercentage2={}",
            successPercentage1, successPercentage2);
  }
  private class HttpRunner implements Runnable {
    AppConfig config;
    int burst;
    Map<String, String> headers = new HashMap<String, String>();
    ExecutorService service;
    private AtomicInteger successCount;
    private AtomicInteger failureCount;
    private AtomicInteger count;
    public HttpRunner(AppConfig config, int burst,  AtomicInteger count, AtomicInteger successCount, AtomicInteger failureCount) {
      this.config = config;
      this.burst = burst;
      this.count = count;
      this.successCount = successCount;
      this.failureCount = failureCount;
      service = Executors.newFixedThreadPool(burst);
      setHeaders(config);
    }

    private void setHeaders(AppConfig config){
      headers.put("X-mmx-app-id", config.getAppId());
      headers.put("X-mmx-api-key", config.getApiKey());
    }

    @Override
    public void run() {
      try {
        for (int i=0; i < burst; i++) {
          service.submit(new Runnable() {
            public void run() {
              try {

                HttpResponse<JsonNode> jsonResponse = Unirest.get(baseUri).basicAuth("admin", "admin").headers(headers).asJson();
                int status = jsonResponse.getStatus();
                count.incrementAndGet();
                if(status >= 200 && status < 300) {
                  successCount.incrementAndGet();
                } else {
                  failureCount.incrementAndGet();
                }
                LOGGER.trace("run : jsonRespone={}", jsonResponse.getStatus());
              } catch (Exception e) {
                LOGGER.error("run : Exception", e);
              }
            }
          });
        }
      } catch (Exception e) {
        LOGGER.error("run : caught exception", e);
      }
    }
  }


  private class AppConfig {
    String appId;
    String apiKey;

    public AppConfig(String appId, String apiKey) {
      this.appId = appId;
      this.apiKey = apiKey;
    }

    public String getAppId() {
      return appId;
    }

    public String getApiKey() {
      return apiKey;
    }
  }
}

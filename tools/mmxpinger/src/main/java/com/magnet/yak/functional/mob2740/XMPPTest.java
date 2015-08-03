package com.magnet.yak.functional.mob2740;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Created by sdatar on 6/3/15.
 */
public class XMPPTest {
  private Client client;
  private long testDuration = 1L;
  private ScheduledExecutorService service =Executors.newScheduledThreadPool(3);
  private ScheduledFuture<?> scheduledFuture1;
  private ScheduledFuture<?> scheduledFuture2;

  public static void main(String[] args) throws Exception {
    new XMPPTest().start();
  }

  public void start() throws Exception {
    Client.ServerConfig serverConfig = new Client.ServerConfig("127.0.0.1", "5222");
    Client.AppConfig appConfig1 = new Client.AppConfig("7d9iah7hbha",
            "2587935a-b764-4fc9-bb8f-2ef6e26f9be6",
            "0b2b75a0-5fa2-4890-a193-4dee7a25ad49");
    Client.UserConfig userConfig1 = new Client.UserConfig("user1", "pass");

    Client.AppConfig appConfig2 = new Client.AppConfig("d7iah7hilv",
            "69730ee6-a8af-4fec-af0f-49f8b4f287f0",
            "e9d5050f-ee7e-420e-8a4b-e35073364f22");
    Client.UserConfig userConfig2 = new Client.UserConfig("user2", "pass");

    Client client1 = new Client(serverConfig, appConfig1, userConfig1);
    Client client2 = new Client(serverConfig, appConfig2, userConfig2);
    client1.sendMessage("Hello", userConfig2.getUsername());
    client2.sendMessage("Echo : Hello", userConfig2.getUsername());

    scheduledFuture1 = service.scheduleAtFixedRate(new
                    XMPPRunner(serverConfig,
                              appConfig1,
                              new Client.UserConfig("testsender1", "pass"), 10,
                    userConfig1.getUsername()),
            0L, 10L, TimeUnit.SECONDS);

    scheduledFuture2 = service.scheduleAtFixedRate(new
                    XMPPRunner(serverConfig,
                    appConfig2,
                    new Client.UserConfig("testsender2", "pass"), 4,
                    userConfig2.getUsername()),
            0L, 10L, TimeUnit.SECONDS);

    service.schedule(new TestTerminator(), testDuration, TimeUnit.MINUTES);
  }

  private class TestTerminator implements Runnable {
    @Override
    public void run() {
      scheduledFuture1.cancel(true);
      scheduledFuture2.cancel(true);
    }
  }

  private class XMPPRunner implements Runnable {
    private final Logger LOGGER = LoggerFactory.getLogger(XMPPRunner.class);
    Client.AppConfig appConfig;
    Client.ServerConfig serverConfig;
    Client.UserConfig userConfig;
    Client client;

    int burst;
    ExecutorService service;
    private String toUser;

    public XMPPRunner(Client.ServerConfig serverConfig, Client.AppConfig appConfig, Client.UserConfig userConfig, int burst, String toUser) {
      this.serverConfig = serverConfig;
      this.appConfig = appConfig;
      this.userConfig = userConfig;
      this.burst = burst;
      service = Executors.newFixedThreadPool(burst);
      client = new Client(serverConfig, appConfig, userConfig);
      this.toUser = toUser;
    }

    @Override
    public void run() {
      try {
        for (int i = 0; i < burst; i++) {
          service.submit(new Runnable() {
            public void run() {
              try {
                client.sendMessage("Hello", toUser);
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
}
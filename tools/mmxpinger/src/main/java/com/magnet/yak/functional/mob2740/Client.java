package com.magnet.yak.functional.mob2740;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXContext;
import com.magnet.mmx.client.MMXSettings;
import com.magnet.mmx.client.common.*;
import com.magnet.mmx.protocol.Headers;
import com.magnet.mmx.protocol.Payload;
import com.magnet.yak.MMXConnectionListenerImpl;
import com.magnet.yak.MMXMessageListenerImpl;
import org.slf4j.*;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by sdatar on 6/3/15.
 */
public class Client {
  private AppConfig appConfig;
  private UserConfig userConfig;
  private ServerConfig serverConfig;
  private MMXContext context;
  private MMXClient mmxClient;
  private MMXSettings mmxSettings;
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Client.class);

  public Client(ServerConfig serverConfig, AppConfig appConfig, UserConfig userConfig) {
    this.serverConfig = serverConfig;
    this.appConfig = appConfig;
    this.userConfig = userConfig;
    String version = "version-1";
    String deviceId = userConfig.getUsername();
    context =  new MMXContext(".", version, deviceId);
    mmxSettings = getSettingsFromConfig(serverConfig, appConfig);
    mmxClient = new MMXClient(context, mmxSettings);
    register(userConfig, mmxClient);
  }

  public void sendMessage(String body, String toUser) throws Exception {
    MMXPayload p =
            new MMXPayload(new Headers(0), constructMessage(body));
    MMXid id = new MMXid(toUser);
    mmxClient.getMessageManager().sendPayload(new MMXid[]{id}, p, null);

  }

  public static class ServerConfig  {
    private String host;
    private String port;

    public ServerConfig(String host, String port) {
      this.host = host;
      this.port = port;
    }

    public String getHost() {
      return host;
    }

    public String getPort() {
      return port;
    }
  }

  public static class UserConfig {
    private String username;
    private String password;

    public UserConfig(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
    }
  }

  public static class AppConfig {
    private String apiKey;
    private String appId;
    private String anonymousSecret;

    public AppConfig(String appId, String apiKey, String anonymousSecret) {
      this.apiKey = apiKey;
      this.appId = appId;
      this.anonymousSecret = anonymousSecret;
    }

    public String getApiKey() {
      return apiKey;
    }

    public String getAppId() {
      return appId;
    }

    public String getAnonymousSecret() {
      return anonymousSecret;
    }
  }

  private Payload constructMessage(String text) {
    return new Payload("simple-message", text);
  }

  private void register(UserConfig userConfig, MMXClient  mmxClient) {
    try {
      LinkedBlockingQueue<Boolean> notify = new LinkedBlockingQueue<Boolean>();
      MMXMessageListener messageListener = new MMXMessageListenerImpl(userConfig.getUsername(), mmxClient);
      MMXConnectionListener connectionListener = new MMXConnectionListenerImpl(userConfig.getUsername(), notify);
      mmxClient.connect(userConfig.getUsername(), userConfig.getPassword().getBytes(), connectionListener, messageListener, true);
      try {
        Boolean b = notify.take();
        if(b.booleanValue())
          LOGGER.debug("register : Succesfully registered={}", userConfig.getUsername());
        else
          LOGGER.error("register : cannot register={}", userConfig.getUsername());
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } catch (MMXException e) {
      e.printStackTrace();
    }
  }

  private MMXSettings getSettingsFromConfig(ServerConfig serverConfig, AppConfig appConfig) {
    MMXSettings mmxSettings = new MMXSettings(context, "mmx.properties");
    mmxSettings.setString(MMXSettings.PROP_APIKEY, appConfig.getApiKey());
    mmxSettings.setString(MMXSettings.PROP_APPID, appConfig.getAppId());
    mmxSettings.setString(MMXSettings.PROP_GUESTSECRET, appConfig.getAnonymousSecret());
    mmxSettings.setString(MMXSettings.PROP_HOST, serverConfig.getHost());
    mmxSettings.setInt(MMXSettings.PROP_PORT, Integer.parseInt(serverConfig.getPort()));
    mmxSettings.setBoolean(MMXSettings.PROP_ENABLE_COMPRESSION, false);
    mmxSettings.setBoolean(MMXSettings.PROP_ENABLE_TLS, false);
    return mmxSettings;
  }
}

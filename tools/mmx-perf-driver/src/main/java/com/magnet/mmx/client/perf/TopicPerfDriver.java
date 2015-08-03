/**
 * Copyright (c) 2014-2015 Magnet Systems, Inc.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.magnet.mmx.client.perf;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXContext;
import com.magnet.mmx.client.MMXSettings;
import com.magnet.mmx.client.common.AdminManager;
import com.magnet.mmx.client.common.Invitation;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXConnection;
import com.magnet.mmx.client.common.MMXConnectionListener;
import com.magnet.mmx.client.common.MMXErrorMessage;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXMessageListener;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.client.common.PubSubManager;
import com.magnet.mmx.client.common.TopicExistsException;
import com.magnet.mmx.client.common.TopicNotFoundException;
import com.magnet.mmx.protocol.APNS;
import com.magnet.mmx.protocol.AppCreate;
import com.magnet.mmx.protocol.AuthData;
import com.magnet.mmx.protocol.GCM;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.TopicAction.PublisherType;
import com.magnet.mmx.util.Base64;
import com.magnet.mmx.util.QueuePoolExecutor;

public class TopicPerfDriver {
  private final static String TAG = "TopicPerfDriver";
  private final static String SERVER_USER = "server-user";
  private final static String SERVER_PWD = "test435";
  private final static char DELIMITER = '|';
  private boolean mStarted;
  private Thread mThread;
  private TopicDriverConfig mConfig;
  private TopicPublisher mPublisher;
  private ArrayList<MMXTopic> mTopics;
  private ArrayList<TopicSubscriber> mConClients;
  private ArrayList<TopicSubscriber> mDiscClients;
  private MMXSettings mSettings;
  private Random mRand = new Random();
  
  private EventListener<TopicPublisher> mPubListener = new EventListener<TopicPublisher>() {
    public void onEvent(Event event, TopicPublisher publisher) {
      switch (event) {
      case CONNECTED:
        break;
      case NOTCONNECTED:
        break;
      case DONE:
        mThread.interrupt();
        break;
      case DISCONNECTED:
        break;
      }
    }
  };
  
  private EventListener<TopicSubscriber> mSubListener = new EventListener<TopicSubscriber>() {
    public void onEvent(Event event, TopicSubscriber subscriber) {
      synchronized(mConClients) {
        switch (event) {
        case CONNECTED:
          mConClients.add(subscriber);
          break;
        case NOTCONNECTED:
          mDiscClients.add(subscriber);
          break;
        case DONE:
          break;
        case DISCONNECTED:
          if (mConClients.remove(subscriber)) {
            mDiscClients.add(subscriber);
          }
          break;
        }
      }
    }
  };

  static class TopicDriverConfig extends DriverConfig {
    public int numTopics = 5;
    public boolean keepTopics = false;
    
    public TopicDriverConfig() {
      super();
      userPrefix = "s-";
    }
    
    public int parseExtraOption(String[] args, int index) {
      if (args[index].equals("-t")) {
        String arg = args[++index];
        numTopics = parseInt(arg);
        return index;
      } else if (args[index].equals("-k")) {
        keepTopics = true;
        return index;
      } else {
        throw new IllegalArgumentException("Invalid option: "+args[index]);
      }
    }
    
    public void printExtraUsage() {
      System.out.println("[-t #Topics] [-k]");
    }
    
    public void printExtraHint() {
      System.out.println("-k for keeping topics before run");
    }
    
    public void reportExtraConfig(StringBuilder sb) {
      sb.append(Utils.pad("# Topics", 15)).append(numTopics).append('\n');
    }
  }

  class TopicPublisher extends Thread implements MMXConnectionListener, MMXMessageListener {
    // Statistics
    public int mPayloadSize;
    public long mWaitTime;
    public long mPubTotal;
    public int mItemsPub;
    public int mPubError;
    public int mConError;
    
    private StringBuilder mHeader;
    private boolean mDone;
    private boolean mAbort;
    private CharBuffer mText;
    private PubSubManager mPubSubMgr;
    private TopicPerfDriver mDriver;
    private EventListener<TopicPublisher> mListener;
    private MMXContext mContext;
    private MMXid mClientId;
    private MMXClient mClient;
    private String mUserId;
    
    public TopicPublisher(String userId, String devId, TopicPerfDriver driver,
                          EventListener<TopicPublisher> listener) {
      super(userId);
      this.setName(userId);
      this.setDaemon(true);
      mDriver = driver;
      mUserId = userId;
      mListener = listener;
      mContext = new MMXContext(".", "0.9", devId);
      mWaitTime = mDriver.getRandomWaitTime();
      mPayloadSize = mDriver.getRandomPayloadSize();
      mText = CharBuffer.allocate(mPayloadSize);
      Arrays.fill(mText.array(), 'p');
      
      mHeader = new StringBuilder(128)
        .append('\n')
        .append(Utils.pad("Publisher", 20)).append(DELIMITER)
        .append(Utils.pad("Size", 8)).append(DELIMITER)
        .append(Utils.pad("ConErr", 8)).append(DELIMITER)
        .append(Utils.pad("#PubErr", 10)).append(DELIMITER)
        .append(Utils.pad("#Pub", 10)).append(DELIMITER)
        .append(Utils.pad("BytesPub", 15)).append(DELIMITER)
        .append(Utils.pad("#Rcvd", 10)).append(DELIMITER)
        .append(Utils.pad("BytesRcvd", 15)).append(DELIMITER)
        .append(Utils.pad("#Delay", 10)).append(DELIMITER)
        .append(Utils.pad("AvgDelryTm", 10)).append(DELIMITER)
        .append('\n');
    }
    
    public StringBuilder getHeader(String prefix, StringBuilder sb) {
      if (prefix != null && !prefix.isEmpty()) {
        sb.append('\n').append(prefix);
      }
      sb.append(mHeader);
      return sb;
    }
    
    public StringBuilder getReport(StringBuilder sb) {
      sb.append(Utils.pad(mClientId.toString(), 20)).append(DELIMITER)
        .append(Utils.pad(mPayloadSize, 8)).append(DELIMITER)
        .append(Utils.pad(mConError, 8)).append(DELIMITER)
        .append(Utils.pad(mPubError, 10)).append(DELIMITER)
        .append(Utils.pad(mItemsPub, 10)).append(DELIMITER)
        .append(Utils.pad(mPubTotal, 15)).append(DELIMITER)
        .append(Utils.pad(0, 10)).append(DELIMITER)
        .append(Utils.pad(0, 15)).append(DELIMITER)
        .append(Utils.pad(0, 10)).append(DELIMITER)
        .append(Utils.pad(0, 10)).append(DELIMITER)
        .append('\n');
      return sb;
    }
    
    public void connect() {
      // Create the client and connect
      mClient = new MMXClient(mContext, mDriver.getSettings());
      try {
        mClient.connect(mUserId, "test435".getBytes(), this, this, true);
        mClientId = mClient.getClientId();
        mListener.onEvent(Event.CONNECTED, this);
        
        mPubSubMgr = mClient.getPubSubManager();
        MMXTopicOptions options = new MMXTopicOptions()
          .setPublisherType(PublisherType.anyone)
          .setMaxItems(10000);
        for (MMXTopic topic : mTopics) {
          if (!mConfig.keepTopics) {
            try {
              mPubSubMgr.deleteTopic(topic);
            } catch (TopicNotFoundException e) {
              // Ignored.
            }
          }
          try {
            mPubSubMgr.createTopic(topic, options);
          } catch (TopicExistsException e) {
            if (!mConfig.keepTopics) {
              throw e;
            }
          }
        }
      } catch (Throwable e) {
        System.err.println("Client "+mUserId+" is not ready");
        e.printStackTrace();
        mClientId = new MMXid(mUserId);
        mListener.onEvent(Event.NOTCONNECTED, this);
        return;
      }
    }
    
    public void halt() {
      try {
        mClient.disconnect(false);
      } catch (MMXException e) {
        // Ignored.
      }
    }
    
    public boolean isDonePublishing() {
      return mDone;
    }
    
    public void run() {      
      // Start publishing messages.
      mDone = false;
      mAbort = false;
      try {
        mPubSubMgr = mClient.getPubSubManager();
        long endTime = System.currentTimeMillis() + mDriver.getConfig().duration;
        while (!mAbort && System.currentTimeMillis() < endTime) {
          Thread.sleep(mWaitTime);
          for (MMXTopic topic : mDriver.mTopics) {
            MMXPayload payload = new MMXPayload(mText);
            try {
              String itemId = mPubSubMgr.publish(topic, payload);
              mItemsPub++;
              mPubTotal += payload.getDataSize();
            } catch (Throwable e) {
              mPubError++;
              System.err.println("Publishing item failed: "+e.getMessage());
            }
          }
        }
        mDone = true;
        if (!mAbort) {
          mListener.onEvent(Event.DONE, this);
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onMessageReceived(MMXMessage message, String receiptId) {
      // Ignored.
    }

    @Override
    public void onMessageSent(String msgId) {
      // Ignored.
    }

    @Override
    public void onMessageFailed(String msgId) {
      // Ignored.
    }

    @Override
    public void onMessageDelivered(MMXid recipient, String msgId) {
      // Ignored.
    }

    @Override
    public void onInvitationReceived(Invitation invitation) {
      // Ignored.
    }

    @Override
    public void onAuthReceived(AuthData auth) {
      // Ignored.
    }

    @Override
    public void onItemReceived(MMXMessage message, MMXTopic topic) {
      // Ignored.
    }

    @Override
    public void onErrorMessageReceived(MMXErrorMessage message) {
      // Ignored.
    }

    @Override
    public void onConnectionEstablished() {
      System.out.println("onConnected");
    }

    @Override
    public void onReconnectingIn(int interval) {
      System.out.println("onReconnectingIn "+interval);
    }
    
    @Override
    public void onConnectionClosed() {
      mAbort = true;
      mListener.onEvent(Event.DONE, this);
    }

    @Override
    public void onConnectionFailed(Exception cause) {
      mConError++;
      mListener.onEvent(Event.DISCONNECTED, this);
    }

    @Override
    public void onAuthenticated(MMXid user) {
      System.out.println("onAuthenticated: "+user);
    }

    @Override
    public void onAuthFailed(MMXid user) {
      System.out.println("onAuthFailed: "+user);
    }

    @Override
    public void onAccountCreated(MMXid user) {
      System.out.println("onAccountCreated: "+user);      
    }
  }
  
  private Accessor<TopicSubscriber, Integer> mRcvdAccessor = new Accessor<TopicSubscriber, Integer>() {
    public Integer get(TopicSubscriber client) {
      return client.mItemsRcvd;
    }
  };
  
  private Accessor<TopicSubscriber, Integer> mDelayAccessor = new Accessor<TopicSubscriber, Integer>() {
    public Integer get(TopicSubscriber client) {
      return client.mItemsDelay;
    }
  };
  
  public Accessor<TopicSubscriber, Integer> mFetchErrAccessor = new Accessor<TopicSubscriber, Integer>() {
    public Integer get(TopicSubscriber client) {
      return client.mFetchErr;
    }
  };
  
  public Accessor<TopicSubscriber, Integer> mFetchAccessor = new Accessor<TopicSubscriber, Integer>() {
    public Integer get(TopicSubscriber client) {
      return client.mFetch;
    }
  };
  
  public Accessor<TopicSubscriber, Long> mFetchBytesAccessor = new Accessor<TopicSubscriber, Long>() {
    public Long get(TopicSubscriber client) {
      return client.mFetchTotal;
    }
  };
    
  class TopicSubscriber implements MMXConnectionListener, MMXMessageListener {
    // Statistics
    public long mDlvryTime;     // accumulative delivery time
    public long mRcvTotal;
    public long mFetchTotal;    // accumulative fetch total bytes
    public int mItemsRcvd;
    public int mItemsDelay;
    public int mFetchErr;
    public int mFetch;
    public int mSub;
    public int mConError;
    
    private StringBuilder mHeader;
    private PubSubManager mPubSubMgr;
    private TopicPerfDriver mDriver;
    private EventListener<TopicSubscriber> mListener;
    private MMXContext mContext;
    private MMXid mClientId;
    private MMXClient mClient;
    private String mUserId;
    
    public TopicSubscriber(String userId, String devId, TopicPerfDriver driver,
                           EventListener<TopicSubscriber> listener) {
      mDriver = driver;
      mListener = listener;
      mUserId = userId;
      mContext = new MMXContext(".", "0.9", devId);
    }
    
    public StringBuilder getHeader(String prefix, StringBuilder sb) {
      if (mHeader == null) {
        mHeader = new StringBuilder(128).append('\n')
            .append(Utils.pad("Subscriber", 20)).append(DELIMITER)
            .append(Utils.pad("Size", 8)).append(DELIMITER)
            .append(Utils.pad("ConErr", 8)).append(DELIMITER)
            .append(Utils.pad("#FetchErr", 10)).append(DELIMITER)
            .append(Utils.pad("#Fetch", 10)).append(DELIMITER)
            .append(Utils.pad("BytesFetch", 15)).append(DELIMITER)
            .append(Utils.pad("#Rcvd", 10)).append(DELIMITER)
            .append(Utils.pad("BytesRcvd", 15)).append(DELIMITER)
            .append(Utils.pad("#Delay", 10)).append(DELIMITER)
            .append(Utils.pad("AvgDelryTm", 10)).append(DELIMITER)
            .append('\n');
      }
      if (prefix != null && !prefix.isEmpty()) {
        sb.append('\n').append(prefix);
      }
      sb.append(mHeader);
      return sb;
    }
    
    public void getReport(StringBuilder sb) {
      long avgDelTime = ((mItemsRcvd - mItemsDelay) == 0) ? 0 : mDlvryTime / (mItemsRcvd - mItemsDelay);
      sb.append(Utils.pad(mClientId.toString(), 20)).append(DELIMITER)
        .append(Utils.pad(0, 8)).append(DELIMITER)
        .append(Utils.pad(mConError, 8)).append(DELIMITER)
        .append(Utils.pad(mFetchErr, 10)).append(DELIMITER)
        .append(Utils.pad(mFetch, 10)).append(DELIMITER)
        .append(Utils.pad(mFetchTotal, 15)).append(DELIMITER)
        .append(Utils.pad(mItemsRcvd, 10)).append(DELIMITER)
        .append(Utils.pad(mRcvTotal, 15)).append(DELIMITER)
        .append(Utils.pad(mItemsDelay, 10)).append(DELIMITER)
        .append(Utils.pad(avgDelTime, 10)).append(DELIMITER)
        .append('\n');
    }
    
    public void halt() {
      try {
        mClient.disconnect(false);
      } catch (MMXException e) {
        // Ignored
      }
    }
    
    public void connect() {
      // Create the client and connect
      mClient = new MMXClient(mContext, mDriver.getSettings());
      try {
        mClient.connect(mUserId, "test435".getBytes(), this, this, true);
        mClientId = mClient.getClientId();
        mListener.onEvent(Event.CONNECTED, this);
        
        mPubSubMgr = mClient.getPubSubManager();
        for (MMXTopic topic : mDriver.getTopics()) {
          String subId = mPubSubMgr.subscribe(topic, false);
          mSub++;
        }
      } catch (Throwable e) {
        System.err.println("Client "+mUserId+" is not ready");
        e.printStackTrace();
        mClientId = new MMXid(mUserId);
        mListener.onEvent(Event.NOTCONNECTED, this);
        return;
      }
    }

    @Override
    public void onMessageReceived(MMXMessage message, String receiptId) {
      // Ignored.
    }

    @Override
    public void onMessageSent(String msgId) {
      // Ignored.
    }

    @Override
    public void onMessageFailed(String msgId) {
      // Ignored.
    }

    @Override
    public void onMessageDelivered(MMXid recipient, String msgId) {
      // Ignored.
    }

    @Override
    public void onInvitationReceived(Invitation invitation) {
      // Ignored.
    }

    @Override
    public void onAuthReceived(AuthData auth) {
      // Ignored.
    }

    @Override
    public void onItemReceived(MMXMessage message, MMXTopic topic) {
      if (!mDriver.isStarted())
        return;
      
      mItemsRcvd++;
      Date sentTime = message.getPayload().getSentTime();
      long tod = System.currentTimeMillis();
      long elapsed = tod - sentTime.getTime();
      if (elapsed <= 10000)
        mDlvryTime += elapsed;
      else {
        mItemsDelay++;
      }
      mRcvTotal += message.getPayload().getDataSize();
    }

    @Override
    public void onErrorMessageReceived(MMXErrorMessage message) {
      // Ignored.
    }

    @Override
    public void onConnectionEstablished() {
      System.out.println("onConnected");
    }

    @Override
    public void onReconnectingIn(int interval) {
      System.out.println("onReconnectingIn: "+interval);
    }
    
    @Override
    public void onConnectionClosed() {
      mListener.onEvent(Event.DONE, this);
    }

    @Override
    public void onConnectionFailed(Exception cause) {
      mConError++;
      mListener.onEvent(Event.DISCONNECTED, this);
    }

    @Override
    public void onAuthenticated(MMXid user) {
      System.out.println("onAuthenticated: "+user);
    }

    @Override
    public void onAuthFailed(MMXid user) {
      System.out.println("onAuthFailed: "+user);
    }

    @Override
    public void onAccountCreated(MMXid user) {
      System.out.println("onAccountCreated: "+user);      
    }
  }

  public TopicPerfDriver(TopicDriverConfig config) throws MMXException {
    Log.setLoggable(TAG, config.logLevel);
    MMXContext appContext = new MMXContext(".", "0.9", "instance-1");
    if (config.registerApp) {
      mSettings = registerApp(appContext, config.host, "admin", "admin",
                              config.appName);
    } else {
      mSettings = new MMXSettings(appContext, config.appName+".props");
      if (!mSettings.load()) {
        throw new IllegalArgumentException(
            config.appName+" is not registered yet; please specify -r");
      }
    }
    mThread = Thread.currentThread();
    mConfig = config;
    mTopics = new ArrayList<MMXTopic>();
    for (int i = 0; i < mConfig.numTopics; i++) {
      mTopics.add(new MMXGlobalTopic("t-"+i));
    }
  }

  public void getTotalsReport(StringBuilder sb, List<TopicSubscriber> clients) {
    sb.append(Utils.pad("== TOTAL ==", 20)).append(DELIMITER)
      .append(Utils.pad("", 8)).append(DELIMITER)
      .append(Utils.pad("", 8)).append(DELIMITER)
      .append(Utils.pad(mPublisher.mPubError, 10)).append(DELIMITER)
      .append(Utils.pad(mPublisher.mItemsPub, 10)).append(DELIMITER)
      .append(Utils.pad("", 15)).append(DELIMITER)
      .append(Utils.pad(grandTotal(clients, mRcvdAccessor), 10)).append(DELIMITER)
      .append(Utils.pad("", 15)).append(DELIMITER)
      .append(Utils.pad(grandTotal(clients, mDelayAccessor), 10)).append(DELIMITER)
      .append(Utils.pad("", 10)).append(DELIMITER)
      .append('\n');
  }
  
  public DriverConfig getConfig() {
    return mConfig;
  }
  
  // Get the application settings.
  public MMXSettings getSettings() {
    return mSettings;
  }

  public List<MMXTopic> getTopics() {
    return mTopics;
  }
  
  // Register a new application with bogus APNS Certificate and GCM Project ID.
  // Assume that the service name is "mmx".
  public MMXSettings registerApp(MMXContext context, String host, 
                                String adminUser, String adminPwd, String appName)
                                throws MMXException {
    MMXSettings settings = new MMXSettings(context, appName+".props");
    settings.setString(MMXSettings.PROP_HOST, host);
    settings.setInt(MMXSettings.PROP_PORT, 5222);
    settings.setBoolean(MMXSettings.PROP_ENABLE_COMPRESSION, false);
    
    MMXConnectionListener conListener = new MMXConnectionListener() {
      @Override
      public void onConnectionEstablished() { }
      @Override
      public void onReconnectingIn(int interval) { }
      @Override
      public void onConnectionClosed() { }
      @Override
      public void onAuthenticated(MMXid user) { }
      @Override
      public void onAccountCreated(MMXid user) { }
      @Override
      public void onConnectionFailed(Exception cause) {
        System.err.print("onConnectionFailed: "+cause.getMessage());
      }
      @Override
      public void onAuthFailed(MMXid user) {
        System.err.println("onAuthFailed: "+user);
      }
    };
    MMXConnection con = new MMXConnection(context, settings);
    try {
      con.connect(conListener);
      con.authenticateRaw(adminUser, adminPwd, "smack", 0);
      byte[] apnsCert = { 0x1, 0x2, 0x3, 0x4, 0x0, (byte) 0xff, 
                          (byte) 0xfe, (byte) 0x80, 0x7f };
      byte[] paddedCert = new byte[1000];
      System.arraycopy(apnsCert, 0, paddedCert, 0, apnsCert.length);
      AdminManager appMgr = AdminManager.getInstance(con);
      AppCreate.Request rqt = new AppCreate.Request()
        .setAppName(appName)
        .setAuthByAppServer(false)
        .setServerUserId(SERVER_USER)
        .setServerUserKey(SERVER_PWD)
        .setGcm(new GCM("Google Project ID 999", "Google API Key 1234"))
        .setApns(new APNS(Base64.encodeBytes(paddedCert), "dummyPasscode"));
      AppCreate.Response result = appMgr.createApp(rqt);
      System.out.println("app registration: apiKey="+result.getApiKey()+
                         ", appId="+result.getAppId());
      
      settings.setString(MMXSettings.PROP_SERVERUSER, SERVER_USER);
      settings.setString(MMXSettings.PROP_APPID, result.getAppId());
      settings.setString(MMXSettings.PROP_APIKEY, result.getApiKey());
      settings.setString(MMXSettings.PROP_GUESTSECRET, result.getGuestSecret());
      settings.save();
      return settings;
    } finally {
      con.disconnect();
    }
  }
  
  // Create the clients and let them connect first.  Each client will then wait
  // for a signal to start sending messages among the clients.
  public TopicPerfDriver init() {
    mPublisher = new TopicPublisher("pub-100", "d-100", this, mPubListener);
    mPublisher.connect();
    
    mDiscClients = new ArrayList<TopicSubscriber>();
    mConClients = new ArrayList<TopicSubscriber>(mConfig.numClients);
    // Use a pool for client connections.
    QueuePoolExecutor executor = new QueuePoolExecutor("Thread Pool", true,
                                                    (mConfig.numClients+9)/10);
    for (int i = 0; i < mConfig.numClients; i++) {
      final int j = 1000 + i;
      executor.post(new Runnable() {
        public void run() {
          TopicSubscriber sub = new TopicSubscriber(mConfig.userPrefix+j,
              "d-"+j, TopicPerfDriver.this, mSubListener);
          sub.connect();
          int retry = 10;
          while (!sub.mClient.isConnected()) {
            try {
              Thread.sleep(250L);
              if (--retry < 0) {
                System.out.println("Gave up connection: "+(mConfig.userPrefix+j));
                break;
              }
            } catch (InterruptedException e) {
              // Ignored.
            }
          }
        }
      });
    }
    
    return this;
  }
  
  public boolean isStarted() {
    return mStarted;
  }
  
  // Wait for all clients connected
  public TopicPerfDriver waitForConnected() {
    if (mPublisher == null || mConClients == null) {
      throw new IllegalStateException("Not call init() yet");
    }
    System.out.println(mConfig.numClients+" clients: "+
        mDiscClients.size()+" not connected, "+mConClients.size()+" connected");
    while ((mDiscClients.size() + mConClients.size()) < mConfig.numClients) {
      try {
        Thread.sleep(1000L);
        System.out.print(mDiscClients.size()+" not connected, "+
            mConClients.size()+" connected\r");
        System.out.flush();
      } catch (InterruptedException e) {
        // Ignored.
      }
    }
    if (mConClients.size() == 0) {
      throw new RuntimeException("No clients are connected.  Aborted.");
    }
    return this;
  }
  
  // Send a signal to all connected clients to start sending messages.
  public TopicPerfDriver start() {
    if (mPublisher == null || mConClients == null) {
      throw new IllegalStateException("Not call init() yet");
    }
    if ((mDiscClients.size() + mConClients.size()) < mConfig.numClients) {
      throw new IllegalStateException("Not call waitForConnected() yet");
    }
//    System.out.println("start()");
    
    // Start publishing items.
    mStarted = true;
    mPublisher.start();
    return this;
  }

  public int grandTotal(List<TopicSubscriber> clients,
                         Accessor<TopicSubscriber, Integer> accessor) {
    int total = 0;
    for (TopicSubscriber client : clients) {
      total += accessor.get(client);
    }
    return total;
  }
    
  // Wait for all clients done.
  public TopicPerfDriver waitForDone() {
    if (mPublisher == null || mConClients == null) {
      throw new IllegalStateException("Not call start() yet");
    }
//    System.out.println("waitForDone()");

    while (!mPublisher.isDonePublishing()) {
      try {
        Thread.sleep(mConfig.refreshTime);
      } catch (InterruptedException e) {
        // The publisher is done.
      }
      report();
    }
    return this;
  }
  
  public TopicPerfDriver reportConfig() {
    StringBuilder sb = new StringBuilder(512);
    System.out.println(mConfig.reportConfig(sb));
    return this;
  }
  
  // Generate a report
  public TopicPerfDriver report() {
    StringBuilder sb = new StringBuilder(2048);
    if (!mConClients.isEmpty()) {
      mConClients.get(0).getHeader("Live", sb);
      for (TopicSubscriber client : mConClients) {
        client.getReport(sb);
      }
      getTotalsReport(sb, mConClients);
    }
    if (!mDiscClients.isEmpty()) {
      mDiscClients.get(0).getHeader("Dead", sb);
      for (TopicSubscriber client : mDiscClients) {
        client.getReport(sb);
      }
      getTotalsReport(sb, mDiscClients);
    }
    mPublisher.getHeader(null, sb);
    mPublisher.getReport(sb);
    System.out.println(sb);
    return this;
  }
  
  // Close down all clients.
  public TopicPerfDriver shutdown(long delay) {
    if (mPublisher == null || mConClients == null) {
      throw new IllegalStateException("Not call init() yet");
    }
    mPublisher.halt();
    
//    System.out.println("shutdown()");
    if (delay > 0) {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        // Ignored.
      }
    }
    synchronized(mConClients) {
      for (TopicSubscriber client : mConClients) {
        client.halt();
      }
    }
    mStarted = false;
    return this;
  }
  
  int getRandomPayloadSize() {
    int diff = mConfig.maxSize - mConfig.minSize;
    if (diff <= 0)
      return mConfig.minSize;
    return mRand.nextInt(diff) + mConfig.minSize;
  }
  
  MMXTopic getRandomTopic() {
    int index = mRand.nextInt(mTopics.size());
    return mTopics.get(index);
  }
  
  long getRandomWaitTime() {
    long diff = mConfig.maxWaitTime - mConfig.minWaitTime;
    if (diff <= 0)
      return mConfig.minWaitTime;
    return mRand.nextInt((int) diff) + mConfig.minWaitTime;
  }
  
  /**
   * The main entry point.
   */
  public static void main(String[] args) {
    TopicDriverConfig config = new TopicDriverConfig();
    config.parseOptions(args);
    try {
      new TopicPerfDriver(config)
          .init()
          .waitForConnected()
          .start()
          .waitForDone()
          .shutdown(5000L)
          .reportConfig()
          .report();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}


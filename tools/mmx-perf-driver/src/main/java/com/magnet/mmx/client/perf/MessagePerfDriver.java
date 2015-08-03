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
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXMessageListener;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.client.common.MessageManager;
import com.magnet.mmx.client.common.Options;
import com.magnet.mmx.protocol.APNS;
import com.magnet.mmx.protocol.AppCreate;
import com.magnet.mmx.protocol.AuthData;
import com.magnet.mmx.protocol.GCM;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.util.Base64;

public class MessagePerfDriver {
  private final static String TAG = "MessagePerfDriver";
  private final static String SERVER_USER = "server-user";
  private final static String SERVER_PWD = "test435";
  private final static char DELIMITER = '|';
  private boolean mStarted;
  private Thread mThread;
  private MessageDriverConfig mConfig;
  private ArrayList<MsgClient> mConClients;
  private ArrayList<MsgClient> mDiscClients;
  private MMXSettings mSettings;
  private Random mRand = new Random();
  private Options OPTIONS_RCPT_ENABLED = new Options().enableReceipt(true);

  private EventListener<MsgClient> mClientListener = new EventListener<MsgClient>() {
    @Override
    public void onEvent(Event event, MsgClient client) {
      synchronized(mConClients) {
        switch (event) {
        case CONNECTED:
          mConClients.add(client);
          break;
        case NOTCONNECTED:
          mDiscClients.add(client);
          break;
        case DONE:
          mThread.interrupt();
          break;
        case DISCONNECTED:
          if (mConClients.remove(client)) {
            mDiscClients.add(client);
          }
          break;
        }
      }
    }
  };
  
  private Accessor<MsgClient, Integer> mMsgErrAccessor = new Accessor<MsgClient, Integer>() {
    @Override
    public Integer get(MsgClient client) {
      return client.mMsgError;
    }
  };
  
  private Accessor<MsgClient, Integer> mMsgSentAccessor = new Accessor<MsgClient, Integer>() {
    @Override
    public Integer get(MsgClient client) {
      return client.mMsgSent;
    }
  };
  
  private Accessor<MsgClient, Integer> mMsgRcvdAccessor = new Accessor<MsgClient, Integer>() {
    public Integer get(MsgClient client) {
      return client.mMsgRcved;
    }
  };
  
  private Accessor<MsgClient, Integer> mMsgDelayAccessor = new Accessor<MsgClient, Integer>() {
    public Integer get(MsgClient client) {
      return client.mDelayMsg;
    }
  };
  
  private Accessor<MsgClient, Integer> mRcptSentAccessor = new Accessor<MsgClient, Integer>() {
    public Integer get(MsgClient client) {
      return client.mReceiptSent;
    }
  };
  
  private Accessor<MsgClient, Integer> mRcptErrAccessor = new Accessor<MsgClient, Integer>() {
    public Integer get(MsgClient client) {
      return client.mReceiptFailed;
    }
  };
  
  private Accessor<MsgClient, Integer> mRcptRtnAccessor = new Accessor<MsgClient, Integer>() {
    public Integer get(MsgClient client) {
      return client.mMsgDelivered;
    }
  };
  
  static class MessageDriverConfig extends DriverConfig {
    public int receiptPercent;
    
    public int parseExtraOption(String[] args, int index) {
      if (args[index].equals("-e")) {
        String arg = args[++index];
        receiptPercent = Math.abs(parseInt(arg)) % 101;
        return index;
      } else {
        throw new IllegalArgumentException("Invalid option: "+args[index]);
      }
    }
    
    public void printExtraUsage() {
      System.out.println("[-e 0..100]");
    }
    
    public void printExtraHint() {
      System.out.println("-e for percentage to send delivery receipt");
    }
    
    public void reportExtraConfig(StringBuilder sb) {
      // Ignored.
    }
  }
  
  class MsgClient extends Thread implements MMXConnectionListener, MMXMessageListener {
    // Statistics
    public int mPayloadSize;
    public long mWaitTime;
    public long mSentTime;
    public long mSentTotal;
    public long mRcvTotal;
    public int mItemRcved;
    public int mMsgRcved;
    public int mMsgSent;
    public int mMsgSendFailed;
    public int mReceiptSent;
    public int mReceiptFailed;
    public int mMsgDelivered;
    public int mMsgError;
    public int mConError;
    public int mDelayMsg;
    
    private StringBuilder mHeader;
    private boolean mDone;
    private boolean mAbort;
    private CharBuffer mText;
    private MessageManager mMsgMgr;
    private MessagePerfDriver mDriver;
    private EventListener<MsgClient> mListener;
    private MMXContext mContext;
    private MMXid mClientId;
    private MMXClient mClient;
    private String mUserId;
    
    public MsgClient(String userId, String devId, MessagePerfDriver driver,
                       EventListener<MsgClient> listener) {
      super(userId);
      this.setName(userId);
      this.setDaemon(true);
      mDriver = driver;
      mListener = listener;
      mUserId = userId;
      mContext = new MMXContext(".", "0.9", devId);
      mWaitTime = mDriver.getRandomWaitTime();
      mPayloadSize = mDriver.getRandomPayloadSize();
      mText = CharBuffer.allocate(mPayloadSize);
      Arrays.fill(mText.array(), 'x');
    }
    
    public StringBuilder getHeader(String prefix, StringBuilder sb) {
      if (mHeader == null) {
        mHeader = new StringBuilder(160).append('\n')
            .append(Utils.pad("Client", 20)).append(DELIMITER)
            .append(Utils.pad("Size", 8)).append(DELIMITER)
            .append(Utils.pad("ConErr", 8)).append(DELIMITER)
            .append(Utils.pad("#ErrMsg", 10)).append(DELIMITER)
            .append(Utils.pad("#MsgSent", 10)).append(DELIMITER)
            .append(Utils.pad("BytesSent", 15)).append(DELIMITER)
            .append(Utils.pad("#MsgRcvd", 10)).append(DELIMITER)
            .append(Utils.pad("BytesRcvd", 15)).append(DELIMITER)
            .append(Utils.pad("AvgSentTm", 10)).append(DELIMITER)
            .append(Utils.pad("#MsgDelay", 10)).append(DELIMITER)
            .append(Utils.pad("RcptSnt", 8)).append(DELIMITER)
            .append(Utils.pad("RcptErr", 8)).append(DELIMITER)
            .append(Utils.pad("RcptRtn", 8)).append(DELIMITER)
            .append('\n');
      }
      if (prefix != null && !prefix.isEmpty()) {
        sb.append('\n').append(prefix);
      }
      sb.append(mHeader);
      return sb;
    }
    
    public void getReport(StringBuilder sb) {
      long avgSentTime = ((mMsgRcved - mDelayMsg) == 0) ? 0 : mSentTime / (mMsgRcved - mDelayMsg);
      sb.append(Utils.pad(mClientId.toString(), 20)).append(DELIMITER)
        .append(Utils.pad(mPayloadSize, 8)).append(DELIMITER)
        .append(Utils.pad(mConError, 8)).append(DELIMITER)
        .append(Utils.pad(mMsgError, 10)).append(DELIMITER)
        .append(Utils.pad(mMsgSent, 10)).append(DELIMITER)
        .append(Utils.pad(mSentTotal, 15)).append(DELIMITER)
        .append(Utils.pad(mMsgRcved, 10)).append(DELIMITER)
        .append(Utils.pad(mRcvTotal, 15)).append(DELIMITER)
        .append(Utils.pad(avgSentTime, 10)).append(DELIMITER)
        .append(Utils.pad(mDelayMsg, 10)).append(DELIMITER)
        .append(Utils.pad(mReceiptSent, 8)).append(DELIMITER)
        .append(Utils.pad(mReceiptFailed, 8)).append(DELIMITER)
        .append(Utils.pad(mMsgDelivered, 8)).append(DELIMITER)
        .append('\n');
    }
    
    public void halt() {
      mAbort = true;
    }
    
    public boolean isDoneSending() {
      return mDone;
    }
    
    public void run() {
      // Create the client and connect
      mClient = new MMXClient(mContext, mDriver.getSettings());
      try {
        mClient.connect(mUserId, "test435".getBytes(), this, this, true);
        mClientId = mClient.getClientId();
        mListener.onEvent(Event.CONNECTED, this);
      } catch (Throwable e) {
        System.err.println("Client "+mUserId+" is not ready");
        e.printStackTrace();
        mClientId = new MMXid(mUserId);
        mListener.onEvent(Event.NOTCONNECTED, this);
        return;
      }
      
      // Wait for a signal to start sending messages
      synchronized(mDriver) {
        try {
          mDriver.wait();
        } catch (InterruptedException e) {
          // Ignored.
        }
      }
      
      // Start sending messages.
      mDone = false;
      mAbort = false;
      try {
        mMsgMgr = mClient.getMessageManager();
        long endTime = System.currentTimeMillis() + mDriver.getConfig().duration;
        while (!mAbort && System.currentTimeMillis() < endTime) {
          Thread.sleep(mWaitTime);
          MMXid to = mDriver.getRandomRecipient();
          if (to == null) {
            // No connected clients (including self) available.
            break;
          }
          MMXPayload payload = new MMXPayload(mText);
          Options options = mDriver.getRandomReceipt();
          mMsgMgr.sendPayload(new MMXid[] { to }, payload, options);
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
      if (!MessagePerfDriver.this.isStarted())
        return;
      
      mMsgRcved++;
      Date sentTime = message.getPayload().getSentTime();
      long tod = System.currentTimeMillis();
      long elapsed = tod - sentTime.getTime();
      if (elapsed <= 10000) {
        mSentTime += elapsed;
      } else {
        mDelayMsg++;
      }
      mRcvTotal += message.getPayload().getDataSize();
      
      if (receiptId != null) {
        try {
          mMsgMgr.sendDeliveryReceipt(receiptId);
          mReceiptSent++;
        } catch (MMXException e) {
          mReceiptFailed++;
        }
      }
    }

    @Override
    public void onMessageSent(String msgId) {
      if (!MessagePerfDriver.this.isStarted())
        return;
      
      mMsgSent++;
      mSentTotal += mPayloadSize;
    }

    @Override
    public void onMessageFailed(String msgId) {
      if (!MessagePerfDriver.this.isStarted())
        return;
      
      mMsgSendFailed++;
    }

    @Override
    public void onMessageDelivered(MMXid recipient, String msgId) {
      if (!MessagePerfDriver.this.isStarted())
        return;
      
      mMsgDelivered++;
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
    public void onItemReceived(MMXMessage msg, MMXTopic topic) {
      if (!MessagePerfDriver.this.isStarted())
        return;
      
      mItemRcved++;
    }

    @Override
    public void onErrorMessageReceived(MMXErrorMessage message) {
      if (!MessagePerfDriver.this.isStarted())
        return;
      
      mMsgError++;
    }

    @Override
    public void onConnectionEstablished() {
      System.out.println("onConnected");
    }

    @Override
    public void onConnectionClosed() {
      mAbort = true;
      mListener.onEvent(Event.DISCONNECTED, this);
    }
    
    @Override
    public void onReconnectingIn(int interval) {
      System.out.println("onReconnectingIn: "+interval);
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

  public MessagePerfDriver(MessageDriverConfig config) throws MMXException {
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
  }

  public void getTotalsReport(StringBuilder sb, List<MsgClient> clients) {
    sb.append(Utils.pad("== TOTAL ==", 20)).append(DELIMITER)
      .append(Utils.pad("", 8)).append(DELIMITER)
      .append(Utils.pad("", 8)).append(DELIMITER)
      .append(Utils.pad(grandTotal(clients, mMsgErrAccessor), 10)).append(DELIMITER)
      .append(Utils.pad(grandTotal(clients, mMsgSentAccessor), 10)).append(DELIMITER)
      .append(Utils.pad("", 15)).append(DELIMITER)
      .append(Utils.pad(grandTotal(clients, mMsgRcvdAccessor), 10)).append(DELIMITER)
      .append(Utils.pad("", 15)).append(DELIMITER)
      .append(Utils.pad("", 10)).append(DELIMITER)
      .append(Utils.pad(grandTotal(clients, mMsgDelayAccessor), 10)).append(DELIMITER)
      .append(Utils.pad(grandTotal(clients, mRcptSentAccessor), 8)).append(DELIMITER)
      .append(Utils.pad(grandTotal(clients, mRcptErrAccessor), 8)).append(DELIMITER)
      .append(Utils.pad(grandTotal(clients, mRcptRtnAccessor), 8)).append(DELIMITER)
      .append('\n');
  }

  
  public DriverConfig getConfig() {
    return mConfig;
  }
  
  // Get the application settings.
  public MMXSettings getSettings() {
    return mSettings;
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
  public MessagePerfDriver init() {
    mDiscClients = new ArrayList<MsgClient>();
    mConClients = new ArrayList<MsgClient>(mConfig.numClients);
    for (int i = 0; i < mConfig.numClients; i++) {
      int j = 1000 + i;
      new MsgClient(mConfig.userPrefix+j, "d-"+j, this, mClientListener).start();
    }
    return this;
  }
  
  public boolean isStarted() {
    return mStarted;
  }
  
  // Wait for all clients connected
  public MessagePerfDriver waitForConnected() {
    if (mConClients == null) {
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
  public MessagePerfDriver start() {
    if (mConClients == null) {
      throw new IllegalStateException("Not call init() yet");
    }
    if ((mDiscClients.size() + mConClients.size()) < mConfig.numClients) {
      throw new IllegalStateException("Not call waitForConnected() yet");
    }
//    System.out.println("start()");
    
    // Notify all connected clients to start sending messages.
    synchronized(this) {
      mStarted = true;
      notifyAll();
    }
    return this;
  }

  public int grandTotal(List<MsgClient> clients, 
                         Accessor<MsgClient, Integer> accessor) {
    int total = 0;
    for (MsgClient client : clients) {
      total += accessor.get(client);
    }
    return total;
  }
  
  // Wait for all clients done.
  public MessagePerfDriver waitForDone() {
    if (mConClients == null) {
      throw new IllegalStateException("Not call start() yet");
    }
//    System.out.println("waitForDone()");

    while (!isAllDone()) {
      try {
        Thread.sleep(mConfig.refreshTime);
      } catch (InterruptedException e) {
        // One client is done.
      }
      report();
    }
    return this;
  }
  
  public MessagePerfDriver reportConfig() {
    StringBuilder sb = new StringBuilder(512);
    System.out.println(mConfig.reportConfig(sb));
    return this;
  }
  
  // Generate a report
  public MessagePerfDriver report() {
    StringBuilder sb = new StringBuilder(2048);
    if (!mConClients.isEmpty()) {
      mConClients.get(0).getHeader("Live", sb);
      for (MsgClient client : mConClients) {
        client.getReport(sb);
      }
      getTotalsReport(sb, mConClients);
    }
    if (!mDiscClients.isEmpty()) {
      mDiscClients.get(0).getHeader("Dead", sb);
      for (MsgClient client : mDiscClients) {
        client.getReport(sb);
      }
      getTotalsReport(sb, mDiscClients);
    }
    System.out.println(sb);
    return this;
  }
  
  // Close down all clients.
  public MessagePerfDriver shutdown(long delay) {
    if (mConClients == null) {
      throw new IllegalStateException("Not call init() yet");
    }
//    System.out.println("shutdown()");
    synchronized(mConClients) {
      for (MsgClient client : mConClients) {
        client.halt();
      }
    }
    mStarted = false;
    if (delay > 0) {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        // Ignored.
      }
    }
    return this;
  }
  
  int getRandomPayloadSize() {
    int diff = mConfig.maxSize - mConfig.minSize;
    if (diff <= 0)
      return mConfig.minSize;
    return mRand.nextInt(diff) + mConfig.minSize;
  }
  
  // Even the client is done (sending), it still can receive messages.
  MMXid getRandomRecipient() {
    if (mConClients.size() == 0)
      return null;
    int index = mRand.nextInt(mConClients.size());
    return mConClients.get(index).mClientId;
  }
  
  Options getRandomReceipt() {
    if (mConfig.receiptPercent == 0) {
      return null;
    } else if (mConfig.receiptPercent == 100) {
      return OPTIONS_RCPT_ENABLED;
    } else {
      return (mRand.nextInt(100) <= mConfig.receiptPercent) ? OPTIONS_RCPT_ENABLED : null;
    }
  }
  
  long getRandomWaitTime() {
    long diff = mConfig.maxWaitTime - mConfig.minWaitTime;
    if (diff <= 0)
      return mConfig.minWaitTime;
    return mRand.nextInt((int) diff) + mConfig.minWaitTime;
  }
  
  private boolean isAllDone() {
    for (MsgClient client : mConClients) {
      if (!client.isDoneSending()) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * The main entry point.
   */
  public static void main(String[] args) {
    MessageDriverConfig config = new MessageDriverConfig();
    config.parseOptions(args);
    try {
      new MessagePerfDriver(config)
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


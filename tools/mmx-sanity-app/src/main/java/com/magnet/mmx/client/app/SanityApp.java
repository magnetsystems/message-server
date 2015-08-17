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

package com.magnet.mmx.client.app;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXContext;
import com.magnet.mmx.client.MMXSettings;
import com.magnet.mmx.client.common.AdminManager;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXConnection;
import com.magnet.mmx.client.common.MMXConnectionListener;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXMessageHandler;
import com.magnet.mmx.client.common.MMXMessageListener;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.client.common.MessageManager;
import com.magnet.mmx.client.common.Options;
import com.magnet.mmx.client.common.PubSubManager;
import com.magnet.mmx.protocol.APNS;
import com.magnet.mmx.protocol.AppCreate;
import com.magnet.mmx.protocol.GCM;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.TopicAction.PublisherType;
import com.magnet.mmx.util.Base64;
import com.magnet.mmx.util.GsonData;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Map;

/**
 * A sanity test for the MMX server.  It will register a new app in MMX server
 * if it does not exist.  Then it will run some sanity test cases.
 */
public class SanityApp {
  private final static String TAG = "SanityApp";
  private final static String SERVER_USER = "server-user";
  private final static String SERVER_PASSWD = "cat123";
  private final static String GOOGLE_PROJECT_ID = "Google Project ID 999";
  private final static String GOOGLE_API_KEY = "Google API Key 1234";
  private final static String APP_VERSION = "1.0";
  private final static String DEVICE_ID = "computer-0";
  private final static String DEF_ADMIN_ID = "admin";
  private final static String DEF_ADMIN_PWD = "admin";
  private final static String DEF_HOST = "localhost";
  private final static String DEF_DOMAIN = "mmx";
  private final static String DEF_USER_ID = "john.doe@magnet.com";
  private final static String DEF_USER_PWD = "password1";
  private final static String DEF_OWNER_ID = "app-owner";
  private final static String DEF_OWNER_EMAIL = "mmx-admin@localhost";
  private final static SimpleDateFormat sIsoFmt;
  private String mAppName;
  private MMXContext mContext;
  private MMXSettings mSettings;  // app settings

  static {
    sIsoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    sIsoFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
  }
  
  private static void println(Object msg) {
    System.out.println(System.currentTimeMillis()+": "+msg);
  }

  public SanityApp(String appName, String appOwner, String adminUserId,
                    String adminPwd, String host, String domain)
                        throws MMXException {
    mAppName = appName;
    mContext = new MMXContext(".", APP_VERSION, DEVICE_ID);
    
    // If the app settings file exists, just use it and assume that the app
    // has been registered.  Otherwise, register it as a new app.
    String propFile = getAppSettingsFile(mAppName);
    mSettings = new MMXSettings(mContext, propFile);
    if (mSettings.exists()) {
      mSettings.load();
    } else if ((mSettings = registerApp(mContext, host, domain, adminUserId, adminPwd,
                                         mAppName, appOwner)) == null) {
      throw new MMXException("App '"+appName+"' already exists, but missing "+
                              propFile);
    }
  }
  
  public SanityClient createClient(String userId, String password) {
    return new SanityClient(userId, password);
  }
  
  private String getAppSettingsFile(String appName) {
    return appName + ".props";
  }
  
  private static void waitForIO() {
    if (Log.isLoggable(null, Log.VERBOSE)) {
      try {
        System.out.print("Press RETURN to exit...");
        System.out.flush();
        System.in.read();
      } catch (IOException e) {
        // Ignored.
      }
    }
  }
  
  // Register a new application with bogus APNS Certificate and GCM Project ID.
  // Assume that the service name is "mmx".  If the registration is success, the
  // app settings will be saved and returned; otherwise, null will be returned.
  private MMXSettings registerApp(MMXContext context, String host, String domain,
                                String adminUser, String adminPwd, String appName,
                                String appOwner)
                                throws MMXException {
    MMXSettings settings = new MMXSettings(context, getAppSettingsFile(appName));
    settings.setString(MMXSettings.PROP_HOST, host);
    settings.setInt(MMXSettings.PROP_PORT, 5222);
    settings.setString(MMXSettings.PROP_SERVICE_NAME, domain);
    settings.setBoolean(MMXSettings.PROP_ENABLE_COMPRESSION, true);
    settings.setBoolean(MMXSettings.PROP_ENABLE_TLS, false);
    
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
        .setServerUserKey(SERVER_PASSWD)
        .setOwnerId(appOwner)
        .setOwnerEmail(DEF_OWNER_EMAIL)
        .setGcm(new GCM(GOOGLE_PROJECT_ID, GOOGLE_API_KEY))
        .setApns(new APNS(Base64.encodeBytes(paddedCert), "dummyPasscode"));
      AppCreate.Response result = appMgr.createApp(rqt);
      
      settings.setString(MMXSettings.PROP_SERVERUSER, SERVER_USER);
      settings.setString(MMXSettings.PROP_APPID, result.getAppId());
      settings.setString(MMXSettings.PROP_APIKEY, result.getApiKey());
      settings.setString(MMXSettings.PROP_GUESTSECRET, result.getGuestSecret());
      settings.save();
      return settings;
    } catch (MMXException e) {
      waitForIO();
      // app already registered, return null.
      if (e.getCode() == StatusCode.CONFLICT) {
        return null;
      }
      throw e;
    } finally {
      con.disconnect();
    }
  }
  
//  private void clone(String user, String pwd) throws MMXException {
//    MMXSettings settings = new MMXSettings(mContext, user+".props");
//    settings.setString(MMXSettings.PROP_USER, user);
//    settings.setString(MMXSettings.PROP_PASSWD, pwd);
//    settings.setString(MMXSettings.PROP_HOST,
//        mSettings.getString(MMXSettings.PROP_HOST, null));
//    settings.setInt(MMXSettings.PROP_PORT,
//        mSettings.getInt(MMXSettings.PROP_PORT, 5222));
//    settings.setString(MMXSettings.PROP_NAME, 
//        Character.toUpperCase(user.charAt(0))+user.substring(1).toLowerCase());
//    settings.setString(MMXSettings.PROP_EMAIL, (user.indexOf('@') < 0) ?
//        user+"@magnet.com" : user);
//    settings.setString(MMXSettings.PROP_APIKEY,
//        mSettings.getString(MMXSettings.PROP_APIKEY, null));
//    settings.setString(MMXSettings.PROP_APPID, 
//        mSettings.getString(MMXSettings.PROP_APPID, null));
//    settings.setString(MMXSettings.PROP_SERVERUSER,
//        mSettings.getString(MMXSettings.PROP_SERVERUSER, null));
//    settings.setString(MMXSettings.PROP_GUESTSECRET,
//        mSettings.getString(MMXSettings.PROP_GUESTSECRET, null));
//    settings.save();
//    
//    println("New settings is saved to "+settings.getName());
//  }
  
  private static void setLogLevel(String tag, String level) {
    if (level.equalsIgnoreCase("s")) {
      Log.setLoggable(tag, Log.SUPPRESS);
    } else if (level.equalsIgnoreCase("v")) {
      Log.setLoggable(tag, Log.VERBOSE);
    } else if (level.equalsIgnoreCase("d")) {
      Log.setLoggable(tag, Log.DEBUG);
    } else if (level.equalsIgnoreCase("i")) {
      Log.setLoggable(tag, Log.INFO);
    } else if (level.equalsIgnoreCase("w")) {
      Log.setLoggable(tag, Log.WARN);
    } else if (level.equalsIgnoreCase("e")) {
      Log.setLoggable(tag, Log.ERROR);
    } else if (level.equals("?")) {
      char c = " SVDIWE".charAt(Log.getLoggable(tag));
      System.out.println("Current log level for "+tag+"="+c);
    } else {
      System.err.println("Invalid log level.");
    }
  }

  /**
   * Application data.
   */
  static class VitalData {
    public String lastName;
    public String firstName;
    public int hb;        // heart beats
    public float bp;      // blood pressure
    public String remarks;
    
    public VitalData(String lname, String fname, int hb, float bp, String remarks) {
      this.lastName = lname;
      this.firstName = fname;
      this.hb = hb;
      this.bp = bp;
      this.remarks = remarks;
    }
    
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      
      if (obj == null || !(obj instanceof VitalData)) {
        return false;
      }
      VitalData po = (VitalData) obj;
      return lastName.equals(po.lastName) && firstName.equals(po.firstName) &&
              hb == po.hb && bp == po.bp && (remarks == po.remarks || 
              remarks != null && remarks.equals(po.remarks));
    }
    
    public String toString() {
      return "{ ln="+lastName+", fn="+firstName+", hb="+hb+", bp="+bp+", rk="+remarks+" }";
    }
  }
  
  static class AppMsg {
    public String mRcptId;
    public VitalData mPayload;
    
    public AppMsg(VitalData payload) {
      this.mPayload = payload;
    }
    
    public AppMsg(VitalData payload, String rcptId) {
      this.mPayload = payload;
      this.mRcptId = rcptId;
    }
  }

  private static final VitalData VITAL_POJO = new VitalData(
      "Doe", "John", 72, 87.2f, "if $bp > 108; call 911");
  private static final MMXGlobalTopic VITAL_TOPIC = new MMXGlobalTopic(
      "vital-topic");

  class SanityClient {
    private String mUserId;
    private String mPassword;
    private boolean mAuthenticated;
    private MMXClient mClient;
    private PubSubManager mPubMgr;
    private MessageManager mMsgMgr;
    private HashMap<String, AppMsg> mRcvMsgs = new HashMap<String, AppMsg>();
    private final MMXTopicOptions mTopicOpts = new MMXTopicOptions().setMaxItems(1).
                                            setDescription("User vital signs").
                                            setPublisherType(PublisherType.anyone);
    private final MMXPayload VITAL_PAYLOAD = new MMXPayload(GsonData.getGson().
                                            toJson(VITAL_POJO)).
                                            setContentType("application/json");
    private final Options mMsgOpts = new Options().enableReceipt(true);
    private final MMXConnectionListener mConListener = new MMXConnectionListener() {
      @Override
      public void onConnectionEstablished() { }

      @Override
      public void onReconnectingIn(int interval) { }
      
      @Override
      public void onConnectionClosed() { }

      @Override
      public void onConnectionFailed(Exception cause) {
        Log.e(TAG, "onConnectionFailed", cause);
      }

      @Override
      public void onAuthenticated(MMXid user) {
        mAuthenticated = true;
        Log.d(TAG, "onAuthenticated: "+user);
      }

      @Override
      public void onAuthFailed(MMXid user) {
        mAuthenticated = false;
        Log.e(TAG, "onAuthFailed: "+user);
      }
      
      @Override
      public void onAccountCreated(MMXid user) {
        Log.i(TAG,  "onAccountCreated: "+user);
      }
    };
    
    private final MMXMessageListener mMsgListener = new MMXMessageHandler() {
      @Override
      public void onMessageReceived(MMXMessage msg, String receiptId) {
        String msgId = msg.getId().intern();
        synchronized(mRcvMsgs) {
          mRcvMsgs.put(msgId, new AppMsg(msgToVitalPayload(msg), receiptId));
        }
        synchronized(msgId) {
          msgId.notify();
        }
      }
      
      @Override
      public void onMessageDelivered(MMXid recipient, String msgId) {
        msgId = msgId.intern();
        boolean delivered = false;
        synchronized(mRcvMsgs) {
          delivered = (mRcvMsgs.remove(msgId) != null);
        }
        if (delivered) {
          synchronized(msgId) {
            msgId.notify();
          }
        }
      }
      
      @Override
      public void onItemReceived(MMXMessage msg, MMXTopic topic) {
        String msgId = msg.getId().intern();
        synchronized(mRcvMsgs) {
          mRcvMsgs.put(msgId, new AppMsg(msgToVitalPayload(msg)));
        }
        synchronized(msgId) {
          msgId.notify();
        }
      }
    };
    
    private SanityClient(String userId, String password) {
      mUserId = userId;
      mPassword = password;
      mClient = new MMXClient(mContext, mSettings);
    }
    
    public void run() throws MMXException {
      try {
        mClient.connect(mUserId, mPassword.getBytes(), mConListener,
                        mMsgListener, true);
        if (!mClient.isConnected() || !mAuthenticated) {
          throw new MMXException("Unable to connect: "+mUserId);
        }
        
        mMsgMgr = mClient.getMessageManager();
        mPubMgr = mClient.getPubSubManager();
        
        // Sanity test 1: send a message with delivery receipt to self,
        // return the delivery receipt and wait for the delivery receipt.
        String msgId = mMsgMgr.sendPayload(new MMXid[] { new MMXid(mUserId) },
            VITAL_PAYLOAD, mMsgOpts);
        AppMsg appMsg = pollMessage(msgId, 2000L);
        if (appMsg == null) {
          throw new RuntimeException("Receiving msg timed out: "+msgId);
        }
        if (!VITAL_POJO.equals(appMsg.mPayload)) {
          throw new RuntimeException("Received msg mismatched: "+
                                      VITAL_POJO+"!="+appMsg.mPayload);
        }
        if (mMsgOpts.isReceiptEnabled() ^ (appMsg.mRcptId != null)) {
          // delivery receipt is enabled, but no receipt, or delivery receipt
          // is disabled, but there is a receipt.
          throw new RuntimeException("Unexpected delivery receipt");
        }
        if (appMsg.mRcptId != null) {
          mMsgMgr.sendDeliveryReceipt(appMsg.mRcptId);
          if (!pollDeliveryReceipt(msgId, 2000L)) {
            throw new RuntimeException("Delivery receipt timed out: "+msgId);
          }
        }
        
        // Sanity test 2: create a topic, subscribe the topic, publish to the
        // topic, fetch the item.
        try {
          mPubMgr.deleteTopic(VITAL_TOPIC);
        } catch (Throwable e) {
          // Ignored.
        }
        MMXTopic topic = mPubMgr.createTopic(VITAL_TOPIC, mTopicOpts);
        if (!topic.equals(VITAL_TOPIC)) {
          throw new RuntimeException("Mismatch topic: "+VITAL_TOPIC+" != "+topic);
        }
        String subId = mPubMgr.subscribe(VITAL_TOPIC, false);
        if (subId == null) {
          throw new RuntimeException("Subscription ID is null");
        }
        String itemId = mPubMgr.publish(VITAL_TOPIC, VITAL_PAYLOAD);
        appMsg = pollItem(itemId, 2000L);
        if (appMsg == null) {
          throw new RuntimeException("Subscribed item timed out: "+itemId);
        }
        if (!VITAL_POJO.equals(appMsg.mPayload)) {
          throw new RuntimeException("Subscribed item mismatched: "+
              VITAL_POJO+"!="+appMsg.mPayload);
        }
        
        Map<String, MMXMessage> items = mPubMgr.getItemsByIds(VITAL_TOPIC, 
            Arrays.asList(new String[] { itemId } ));
        VitalData payload = msgToVitalPayload(items.get(itemId));
        if (!VITAL_POJO.equals(payload)) {
          throw new RuntimeException("Fetched item mismatched: "+
              VITAL_POJO+"!="+payload);
        }
      } catch (Throwable e) {
        waitForIO();
        throw e;
      } finally {
        if (mPubMgr != null) {
          mPubMgr.unsubscribe(VITAL_TOPIC, null);
          mPubMgr.deleteTopic(VITAL_TOPIC);
        }
        mClient.disconnect(true);
      }
    }
    
    private VitalData msgToVitalPayload(MMXMessage msg) {
      return GsonData.getGson().fromJson(
          msg.getPayload().getDataAsText().toString(), VitalData.class);
    }
    
    private boolean pollDeliveryReceipt(String msgId, long waitTime) {
      msgId = msgId.intern();
      try {
        if (mRcvMsgs.get(msgId) == null) {
          return true;
        }
        synchronized(msgId) {
          msgId.wait(waitTime);
        }
        return mRcvMsgs.get(msgId) == null;
      } catch (InterruptedException e) {
        return false;
      }
    }
    
    private AppMsg pollMessage(String msgId, long waitTime) {
      msgId = msgId.intern();
      try {
        AppMsg appMsg = mRcvMsgs.get(msgId);
        if (appMsg != null) {
          return appMsg;
        }
        synchronized(msgId) {
          msgId.wait(waitTime);
        }
        return mRcvMsgs.get(msgId);
      } catch (InterruptedException e) {
        return null;
      }
    }
    
    private AppMsg pollItem(String itemId, long waitTime) {
      itemId = itemId.intern();
      try {
        AppMsg appMsg = mRcvMsgs.get(itemId);
        if (appMsg != null) {
          return appMsg;
        }
        synchronized(itemId) {
          itemId.wait(waitTime);
        }
        return mRcvMsgs.get(itemId);
      } catch (InterruptedException e) {
        return null;
      }
    }
  }
  
  private static void usage() {
    System.err.println(
        "java SanityApp [-a adminUid -p adminPwd -h host -u userId -o ownerId -l loglevel] appname");
  }
  
  public static void main(String[] args) {
    try {
      String adminUserId = DEF_ADMIN_ID;
      String adminPasswd = DEF_ADMIN_PWD;
      String host = DEF_HOST;
      String domain = DEF_DOMAIN;
      String userId = DEF_USER_ID;
      String userPwd = DEF_USER_PWD;
      String appOwner = DEF_OWNER_ID;
      String appName = null;
      
      // Set the default log level to DEBUG.
      Log.setLoggable(null, Log.DEBUG);
      
      int optIndex = -1;
      for (int i = 0; i < args.length; ++i) {
        String opt = args[i];
        if (opt.charAt(0) != '-') {
          optIndex = i;
          break;
        } else {
          switch (opt.charAt(1)) {
          case 'a': adminUserId = args[++i];      break;
          case 'd': domain = args[++i];           break;
          case 'h': host = args[++i];             break;
          case 'l': setLogLevel(null, args[++i]); break;
          case 'o': appOwner = args[++i];         break;
          case 'p': adminPasswd = args[++i];      break;
          case 'u': userId = args[++i];           break;
          default: usage(); System.exit(1);       break;
          }
        }
      }
      if (optIndex < 0 || (appName = args[optIndex]).isEmpty()) {
        usage();
        System.exit(1);
      }
      
      SanityApp app = new SanityApp(appName, appOwner, adminUserId, adminPasswd,
          host, domain);
      SanityClient client = app.createClient(userId, userPwd);
      client.run();
      
      println("Sanity tests are done");
      System.exit(0);
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}

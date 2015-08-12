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
import com.magnet.mmx.client.app.TopicTemplateManager.TopicTemplate;
import com.magnet.mmx.client.common.AccountManager;
import com.magnet.mmx.client.common.AdminManager;
import com.magnet.mmx.client.common.DeviceManager;
import com.magnet.mmx.client.common.Invitation;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXConnection;
import com.magnet.mmx.client.common.MMXConnectionListener;
import com.magnet.mmx.client.common.MMXErrorMessage;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGeoLogger;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXMessageListener;
import com.magnet.mmx.client.common.MMXMessageStatus;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXPersonalTopic;
import com.magnet.mmx.client.common.MMXSubscription;
import com.magnet.mmx.client.common.MMXTopicInfo;
import com.magnet.mmx.client.common.MMXTopicSearchResult;
import com.magnet.mmx.client.common.MMXUserTopic;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.client.common.MessageManager;
import com.magnet.mmx.client.common.Options;
import com.magnet.mmx.client.common.PubSubManager;
import com.magnet.mmx.client.common.PushManager;
import com.magnet.mmx.protocol.APNS;
import com.magnet.mmx.protocol.AppCreate;
import com.magnet.mmx.protocol.AuthData;
import com.magnet.mmx.protocol.CarrierEnum;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Constants.PingPongCommand;
import com.magnet.mmx.protocol.Constants.UserCreateMode;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.DevTags;
import com.magnet.mmx.protocol.DeviceInfo;
import com.magnet.mmx.protocol.GCM;
import com.magnet.mmx.protocol.GeoLoc;
import com.magnet.mmx.protocol.MMXAttribute;
import com.magnet.mmx.protocol.MMXError;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.MsgTags;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.PingPong;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicAction.FetchOptions;
import com.magnet.mmx.protocol.TopicAction.ListType;
import com.magnet.mmx.protocol.TopicAction.PublisherType;
import com.magnet.mmx.protocol.TopicAction.TopicTags;
import com.magnet.mmx.protocol.TopicSummary;
import com.magnet.mmx.protocol.UserCreate;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.UserQuery;
import com.magnet.mmx.protocol.UserTags;
import com.magnet.mmx.util.Base64;
import com.magnet.mmx.util.DisposableBinFile;
import com.magnet.mmx.util.DisposableFile;
import com.magnet.mmx.util.DisposableTextFile;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.Utils;
import com.magnet.mmx.util.XIDUtil;

import org.jivesoftware.smack.packet.XMPPError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.Map;

/**
 * A manual test app for lower layer API.  The user first enter the "config..."
 * command which saves a user profile in the current directory.  Then enter
 * "adminlogin" to login as "admin" (the password is "admin"), and enter
 * "appreg appName" to register an application.  Then enter "logout".
 * Enter "login ..." will use the profile information to login or create an
 * account.
 * 
 * To test the higher layer MMXClient API, use "connect..."  
 *
 */
public class TestApp {
  private final static String TAG = "TestApp";
  private final static String EC2 = "citest01.magneteng.com";
  private final static String LOCAL = "localhost";
  private final static String SERVER_USER = "server-user";
  private final static String SERVER_PASSWD = "cat";
  private final static SimpleDateFormat sIsoFmt;
  private Random mRandom = new Random();

  static {
    sIsoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    sIsoFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
  }
  
  private MMXConnectionListener mConListener = new MMXConnectionListener() {
    @Override
    public void onConnectionEstablished() {
      Log.d(TAG, "onConnectionEstablished");
    }

    @Override
    public void onReconnectingIn(int interval) {
      Log.d(TAG, "onReconnectingIn: "+interval);
    }
    
    @Override
    public void onConnectionClosed() {
      Log.d(TAG, "onConnectionClosed");
    }

    @Override
    public void onConnectionFailed(Exception cause) {
      Log.e(TAG, "onConnectionFailed", cause);
    }

    @Override
    public void onAuthenticated(MMXid user) {
      Log.d(TAG, "onAuthenticated: "+user);
    }

    @Override
    public void onAuthFailed(MMXid user) {
      Log.w(TAG, "onAuthFailed: "+user);
    }
    
    @Override
    public void onAccountCreated(MMXid user) {
      Log.i(TAG,  "onAccountCreated: "+user);
    }
  };
  
  private static void println(Object msg) {
    System.out.println(System.currentTimeMillis()+": "+msg);
  }
  
  private MMXMessageListener mMsgListener = new MMXMessageListener() {
    @Override
    public void onMessageReceived(MMXMessage message, String receiptId) {
      try {
        MMXPayload payload = message.getPayload();
        long elapsed = System.currentTimeMillis() - payload.getSentTime().getTime();
        if (mDisplayName) {
          accountGet(message.getFrom().getUserId());
        }
        if (payload.getMetaData("type", null).equals("LARGEMSG")) {
          println("onMessageReceived: LARGEMSG, hdrs="+message.getPayload().getAllMetaData()+
              ", size="+payload.getDataSize()+", elapsed="+elapsed);
          println(Utils.subSequenceHeadTail(payload.getDataAsText(), 1024));
        } else {
          println("onMessageReceived: "+message+", receiptId="+
                              receiptId+", elapsed="+elapsed);
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    
    public void onMessageDelivered(MMXid recipient, String msgId) {
      println("onMessageDelivered: to="+recipient+", msgId="+msgId);
    }

    @Override
    public void onMessageSent(String msgId) {
      println("onMessageSent: msgId="+msgId);
    }
    
    @Override
    public void onMessageFailed(String msgId) {
      System.err.println("onMessageFailed: msgId="+msgId);
    }

    @Override
    public void onInvitationReceived(Invitation invitation) {
      println("onInvitationReceived: "+invitation);
    }

    @Override
    public void onAuthReceived(AuthData auth) {
      println("onAuthReceived: apikey="+auth.getApiKey()+
                ", authToken="+auth.getAuthToken()+", user="+auth.getUserId()+
                ", pwd="+auth.getPassword());
    }

    @Override
    public void onItemReceived(MMXMessage msg, MMXTopic topic) {
      println("onItemReceived: topic="+topic+", itemId="+msg.getId()+
                  ", payload="+msg.getPayload()+", msg="+msg);
      try {
        TopicTemplate template = mTemplateMgr.getItemView(topic);
        if (template != null) {
          CharSequence view = mTemplateMgr.bindItemView(template, msg);
          println(view.toString());
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    
    @Override
    public void onErrorMessageReceived(MMXErrorMessage message) {
      if (message.isMMXError()) {
        MMXError error = message.getMMXError();
        println("onErrorMessageReceived: msgId="+message.getId()+
            ", MMXError="+error);
      } else if (message.isXMPPError()) {
        XMPPError error = message.getXMPPError();
        println("onErrorMessageReceived: msgId="+message.getId()+
            ", XMPPError="+error);
      } else if (message.isCustomError()) {
        MMXPayload error = message.getCustomError();
        println("onErrorMessageReceived: CustomError="+message);
      }
    }
  };
  
  private MMXContext mContext;
  private MMXClient mClient;
  private MMXConnection mCon;
  private MMXConnection mAdminCon;
  private TopicTemplateManager mTemplateMgr;
  private MMXSettings mSettings;
  private HashMap<String, MMXTopicOptions> mTopicOptions = new HashMap<String, MMXTopicOptions>();
  private HashMap<String, String> mUserSubs = new HashMap<String, String>();
  private boolean mDisplayName;
  
  // Use the settings
  public TestApp(String settingsName) throws MMXException {
    mContext = new MMXContext(".", "version-1", "computer-"+System.currentTimeMillis()%3);
    init(settingsName);
    initTopicOptions();
    mClient = new MMXClient(mContext, mSettings);
    mTemplateMgr = new TopicTemplateManager(mClient);
    
    // Set the default log level to DEBUG.
    Log.setLoggable(null, Log.DEBUG);
  }
  
  private void assertCon(MMXConnection con) {
    if (con == null)
      throw new RuntimeException("Connection is null");
    if (!con.isConnected())
      throw new RuntimeException("Not connected");
    if (!con.isAuthenticated())
      throw new RuntimeException("Not authenticated");
  }
  
  private void init(String settingsName) throws MMXException {
    mSettings = new MMXSettings(mContext, settingsName);
    if (!mSettings.exists()) {
      Log.e(TAG, "No settings.  Type: config host user pwd");
    } else {
      mSettings.load();
    }

   // We really don't need to create the connection object here; it is just to
    // prevent NullPointerException if someone forgets to call connect() first.
    mCon = new MMXConnection(mContext, mSettings);
    mCon.setMessageListener(mMsgListener);

    // Clear the cached subscriptions for the current user.
    mUserSubs.clear();
  }
  
  private void config(String host, String user, String pwd) throws MMXException {
    if ("ec2".equalsIgnoreCase(host)) {
      host = EC2;
    } else if ("local".equalsIgnoreCase(host)) {
      host = LOCAL;
    }

    mSettings.setString(MMXSettings.PROP_USER, user);
    mSettings.setString(MMXSettings.PROP_PASSWD, pwd);
    mSettings.setString(MMXSettings.PROP_HOST, host);
    mSettings.setInt(MMXSettings.PROP_PORT, 5222);
    mSettings.setString(MMXSettings.PROP_NAME, 
        Character.toUpperCase(user.charAt(0))+user.substring(1).toLowerCase());
    String email = (user.indexOf('@') < 0) ? user+"@magnet.com" : user;
    mSettings.setString(MMXSettings.PROP_EMAIL, email);
    mSettings.save();
    
    println("Property file "+mSettings.getName()+" is configured.");
  }
  
  private void clone(String user, String pwd) throws MMXException {
    MMXSettings settings = new MMXSettings(mContext, user+".props");
    settings.setString(MMXSettings.PROP_USER, user);
    settings.setString(MMXSettings.PROP_PASSWD, pwd);
    settings.setString(MMXSettings.PROP_HOST,
        mSettings.getString(MMXSettings.PROP_HOST, null));
    settings.setInt(MMXSettings.PROP_PORT,
        mSettings.getInt(MMXSettings.PROP_PORT, 5222));
    settings.setString(MMXSettings.PROP_NAME, 
        Character.toUpperCase(user.charAt(0))+user.substring(1).toLowerCase());
    settings.setString(MMXSettings.PROP_EMAIL, (user.indexOf('@') < 0) ?
        user+"@magnet.com" : user);
    settings.setString(MMXSettings.PROP_APIKEY,
        mSettings.getString(MMXSettings.PROP_APIKEY, null));
    settings.setString(MMXSettings.PROP_APPID, 
        mSettings.getString(MMXSettings.PROP_APPID, null));
    settings.setString(MMXSettings.PROP_SERVERUSER,
        mSettings.getString(MMXSettings.PROP_SERVERUSER, null));
    settings.setString(MMXSettings.PROP_GUESTSECRET,
        mSettings.getString(MMXSettings.PROP_GUESTSECRET, null));
    settings.save();
    
    println("New settings is saved to "+settings.getName());
  }
  
  // Use a different properties file for the settings.
  private void use(String settingsName) throws MMXException {
    if (mCon.isConnected()) {
      discon(false);
      mCon = null;
    }
    init(settingsName);
  }
  
  private void connect(boolean autoCreate) throws MMXException {
    String user = mSettings.getString(MMXSettings.PROP_USER, null);
    if (user == null || user.isEmpty()) {
      System.err.println("No config.  Type: config host user pwd");
      return;
    }
    mClient.connect(user,
                   mSettings.getString(MMXSettings.PROP_PASSWD, null).getBytes(),
                   mConListener, mMsgListener, autoCreate);
    // Get the lower layer connection for internal testing.
    // Application never uses MMXConnection, but MMXClient.
    try {
      mCon = (MMXConnection) Utils.invokeMethod(mClient, "getConnection");
    } catch (Throwable e) {
      throw new MMXException(e);
    }
  }
  
  private void connectAnonymously() throws MMXException {
    if (mCon.isConnected()) {
      mCon.disconnect();
    }
    mClient.connectAnonymously(mConListener, mMsgListener);
    // Get the lower layer connection for internal testing.
    // Application never uses MMXConnection, but MMXClient.
    try {
      mCon = (MMXConnection) Utils.invokeMethod(mClient, "getConnection");
    } catch (Throwable e) {
      throw new MMXException(e);
    }
  }
  
  private void getStatus() {
    if (mAdminCon != null) {
      println("Admin="+mAdminCon.getUser()+", Connected="+
          mAdminCon.isConnected()+", Auth="+mAdminCon.isAuthenticated()+
          ", Anony="+mAdminCon.isAnonymous()+", DevId="+mContext.getDeviceId()+
          ", ShowSender="+mDisplayName);
    }
    if (mCon != null && mCon != mAdminCon) {
      println("User="+mCon.getUser()+", Connected="+mCon.isConnected()+
        ", Auth="+mCon.isAuthenticated()+", Anony="+mCon.isAnonymous()+
        ", DevId="+mContext.getDeviceId()+", ShowSender="+mDisplayName);
    }
  }
 
  private void accountUpdate(HashMap<String, String> attrs) throws MMXException {
    String value;
    AccountManager acctMgr = AccountManager.getInstance(mCon);
    UserInfo info = new UserInfo();
    if ((value = attrs.get("email")) != null) {
      info.setEmail(value);
    }
    if ((value = attrs.get("name")) != null) {
      info.setDisplayName(value);
    }
    MMXStatus status = acctMgr.updateAccount(info);
    println("account update: status="+status.getCode());
  }
  
  private void accountSearch(boolean and, HashMap<String, String> attrs) 
                              throws MMXException {
    String value;
    SearchAction.Operator operator = and ?
        SearchAction.Operator.AND : SearchAction.Operator.OR;
    AccountManager acctMgr = AccountManager.getInstance(mCon);
    UserQuery.Search search = new UserQuery.Search();
    if ((value = attrs.get("email")) != null) {
      search.setEmail(value);
    }
    if ((value = attrs.get("name")) != null) {
      search.setDisplayName(value);
    }
    if ((value = attrs.get("phone")) != null) {
      search.setPhone(value);
    }
    if ((value = attrs.get("tags")) != null) {
      search.setTags(parseMultiValues(value));
    }
    UserQuery.Response resp = acctMgr.searchBy(operator, search, null);
    for (UserInfo user : resp.getUsers()) {
      println("search user="+user);
    }
  }
  
  private void accountGet(String userId) throws MMXException {
    UserInfo accountInfo;
    AccountManager acctMgr = AccountManager.getInstance(mCon);
    if (userId == null) {
      accountInfo = acctMgr.getUserInfo();
      println("account="+accountInfo);
    } else {
      try {
        accountInfo = (UserInfo) Utils.invokeMethod(acctMgr, "getUserInfo", userId);
        println("account ("+userId+")="+accountInfo);
      } catch (Throwable e) {
        if (e instanceof MMXException) {
          throw (MMXException) e;
        }
        throw new MMXException(e);
      }
    }
  }
  
  private void loginAdmin(String user, String password) throws MMXException {
    if (mAdminCon == null) {
      mAdminCon = new MMXConnection(mContext, mSettings);
    }
    if (!mAdminCon.isConnected()) {
      mAdminCon.connect(mConListener);
    }
    if (!mAdminCon.isAuthenticated()) {
      mAdminCon.authenticateRaw(user, password, "smack", 0);
    }
    if (mAdminCon.isAuthenticated()) {
      mCon = mAdminCon;
    }
  }
  
  private void logoutAdmin() {
    if (mAdminCon != null && mAdminCon.isConnected()) {
      mAdminCon.disconnect();
      mAdminCon = null;
    }
  }
  
//  private void login(boolean autoCreate) throws MMXException {
//    String user = mSettings.getString(MMXSettings.PROP_USER, null);
//    if (user == null) {
//      System.err.println("No config.  Type: config host user pwd");
//      return;
//    }
//    String pwd = mSettings.getString(MMXSettings.PROP_PASSWD, null);
//    if (mCon.isConnected()) {
//      mCon.disconnect();
//    }
//    mCon.connect(mSettings, mConListener);
//    mCon.authenticate(user, pwd, mContext.getDeviceId(),
//        autoCreate ? MMXConnection.AUTH_AUTO_CREATE : 0);
//  }

  private void discon(boolean unreg) throws MMXException {
    mClient.disconnect(unreg);
  }
  
  private final static byte[] sAPNSCert = new byte[1000];

  private void registerApp(String appName) {
    assertCon(mAdminCon);
    try {
      byte[] apnsCert = { 0x1, 0x2, 0x3, 0x4, 0x0, (byte) 0xff, 
                          (byte) 0xfe, (byte) 0x80, 0x7f };
      System.arraycopy(apnsCert, 0, sAPNSCert, 0, apnsCert.length);
      AdminManager appMgr = AdminManager.getInstance(mAdminCon);
      AppCreate.Request rqt = new AppCreate.Request();
      rqt.setAppName(appName);
      rqt.setAuthByAppServer(false);
      rqt.setServerUserId(SERVER_USER);
      rqt.setServerUserKey(SERVER_PASSWD);
      rqt.setGcm(new GCM("Google Project ID 123", "Google API Key abc"));
      rqt.setApns(new APNS(Base64.encodeBytes(sAPNSCert), "dummyPasscode"));
      rqt.setOwnerId("app-owner");
      rqt.setOwnerEmail("owner@acme.org");
      AppCreate.Response result = appMgr.createApp(rqt);
      println("app registration: apiKey="+result.getApiKey()+
                 ", appId="+result.getAppId());
      mSettings.setString(MMXSettings.PROP_SERVERUSER, SERVER_USER);
      mSettings.setString(MMXSettings.PROP_APPID, result.getAppId());
      mSettings.setString(MMXSettings.PROP_APIKEY, result.getApiKey());
      mSettings.setString(MMXSettings.PROP_GUESTSECRET, result.getGuestSecret());
      mSettings.save();
      
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  private void registerDev(String deviceId) throws MMXException {
    DeviceManager devMgr = DeviceManager.getInstance(mCon);
    DevReg devInfo = new DevReg();
    devInfo.setDevId(deviceId);
    devInfo.setOsType(OSType.UNIX.toString());
    devInfo.setOsVersion("12.04 LT");
    devInfo.setDisplayName("Linux Desktop");
    devInfo.setPhoneNumber("4085551212");
    devInfo.setCarrierInfo(CarrierEnum.TMOBILE.name());
    MMXStatus status = devMgr.register(devInfo);
    println("device registration: status="+status.getCode());
  }
  
  private void queryDev(String userId) throws MMXException {
    DeviceManager devMgr = DeviceManager.getInstance(mCon);
    List<DeviceInfo> list = null;
    try {
      if (userId == null) {
        list = devMgr.getDevices();
      } else {
        list = (List<DeviceInfo>) Utils.invokeMethod(devMgr, "getDevices", userId);
      }
      println("Found "+list.size()+" devices: "+list);
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e);
    }
  }
  

  private void sendMsg(String target, boolean receipt, boolean droppable,
                        String text) throws MMXException {
    MMXid[] to = parseTarget(target);
    String data = GsonData.getGson().toJson(text);
    MMXPayload payload = new MMXPayload(data)
                        .setContentType("application/json")
                        .setMetaData("type", "TESTMSG");
    Options options = (new Options()).enableReceipt(receipt)
                        .setDroppable(droppable);
    MessageManager.getInstance(mCon).sendPayload(to, payload, options);
  }
  
  private void sendBigMsg(String target, int size, char c) throws MMXException {
    MMXid[] to = parseTarget(target);
    char[] cs = new char[size];
    Arrays.fill(cs, c);
    MMXPayload payload = new MMXPayload(new String(cs))
                        .setContentType("text/plain")
                        .setMetaData("type", "LARGEMSG");
    MessageManager.getInstance(mCon).sendPayload(to, payload, null);
  }
  
  private File fillFile(String path, byte b, int size) throws IOException {
    byte[] buf = new byte[4096];
    Arrays.fill(buf, b);
    File file = new File(path);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      while (size > 0) {
        int len = Math.min(size, buf.length);
        fos.write(buf, 0, len);
        size -= len;
      }
      return file; 
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }
  
  private void sendTextFile(String target, int size, char c) throws MMXException {
    MMXid[] to = parseTarget(target);
    File tmpfile = null;
    try {
      String path = mContext.getFilePath("large.txt");
      tmpfile = fillFile(path, (byte) c, size);
      tmpfile.deleteOnExit();
      Log.d(TAG, "sending large msg '"+c+"'...size="+tmpfile.length());
      DisposableFile dfile = new DisposableTextFile(tmpfile.getPath(), true);
      MMXPayload payload = new MMXPayload(dfile)
                          .setContentType("text/plain")
                          .setMetaData("type", "LARGEMSG");
      String msgId = MessageManager.getInstance(mCon).sendPayload(to, payload, null);
      println("sendLargeMsg() msgId="+msgId);
    } catch (IOException e) {
      throw new MMXException(e);
    }
  }
  
  private void sendBinFile(String target, int size, int b) throws MMXException {
    MMXid[] to = parseTarget(target);
    File tmpfile = null;
    try {
      String path = mContext.getFilePath("large.bin");
      tmpfile = fillFile(path, (byte) b, size);
      tmpfile.deleteOnExit();
      Log.d(TAG, "sending large bin msg...size="+tmpfile.length());
      DisposableFile dfile = new DisposableBinFile(tmpfile.getPath(), true);
      MMXPayload payload = new MMXPayload(dfile)
                      .setContentType("application/octet-stream")
                      .setMetaData("type", "LARGEMSG");
      String msgId = MessageManager.getInstance(mCon).sendPayload(to, payload, null);
      println("sendLargeBinMsg() msgId="+msgId);
    } catch (IOException e) {
      throw new MMXException(e);
    }
  }
  
  private void sendReceipt(String receiptId) throws MMXException {
    MessageManager mgr = MessageManager.getInstance(mCon);
    mgr.sendDeliveryReceipt(receiptId);
  }
  
  private void listAllTopics() throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    try {
      List<MMXTopicId> list = (List<MMXTopicId>) Utils.invokeMethod(mgr, "SAT");
      for (MMXTopicId topic : list) {
        println("topic="+topic);
      };
    } catch (Throwable e) {
      if (e instanceof MMXException)
        throw (MMXException) e;
      throw new MMXException(e);
    }
  }
  
  private void listTopics(String listType) throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    ListType type = null;
    if (listType != null) {
      if (listType.equalsIgnoreCase("b"))
        type = ListType.both;
      else if (listType.equalsIgnoreCase("g"))
        type = ListType.global;
      else if (listType.equalsIgnoreCase("p"))
        type = ListType.personal;
    }
    
    try {
      List<MMXTopicInfo> list;
      if (type == null)
        list = mgr.listTopics();
      else
        list = mgr.listTopics(null, type, true);
      for (MMXTopicInfo info : list) {
        println("info="+info);
      }
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e);
    }
  }

  // Assume that HTML templates are used.
  private void setTopicWithTemplates(String topicName, String pubPath,
                            String subPath) throws MMXException, IOException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    MMXTopic topic = new MMXGlobalTopic(topicName);
    mTemplateMgr.setTemplates(topic, 
        new TopicTemplate("text/html", new File(pubPath)), 
        new TopicTemplate("text/html", new File(subPath)));
  }
  
  private void pubTopicWithTemplate(String topicName) throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    MMXTopic topic = new MMXGlobalTopic(topicName);
    println("template: "+mTemplateMgr.getPublishView(topic));
    HashMap<String, String> map = new HashMap<String, String>();
    long tod = System.currentTimeMillis();
    map.put("event", String.valueOf(tod/1000%80+1));
    map.put("heat", String.valueOf(tod%18+1));
    map.put("desc", "10&Under 50 Free");
    mgr.publish(topic, new MMXPayload(GsonData.getGson().toJson(map)));
  }
  
  // This is a system administrative command to wipe out ALL topics in MMX.
  // Since Openfire does not provide Web UI for manage pub/sub, this is a simple
  // way to clean up the system.
  private void clearAllTopics() throws MMXException {
    try {
      PubSubManager mgr = PubSubManager.getInstance(mCon);
      Utils.invokeMethod(mgr, "CAT");
    } catch (Throwable e) {
      throw new MMXException(e);
    }
  }
  
  // The SDK is configured to show all subscriptions in global topics only.
  private void listSubs(String topic) throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    List<MMXSubscription> list = mgr.listSubscriptions((topic == null) ?
        null : new MMXGlobalTopic(topic));
    for (MMXSubscription sub : list) {
      println("sub="+sub);
    }
  }

  private void initTopicOptions() {
    MMXTopicOptions option;
    
    // use default (persisted item, anyone)
    mTopicOptions.put("df", null);
    
    // persisted items, anyone
    option = new MMXTopicOptions()
      .setDescription("Persisted, anyone can publish")
      .setMaxItems(1)
      .setPublisherType(PublisherType.anyone);
    mTopicOptions.put("pa", option);
    
    // transient items, owner
    option = new MMXTopicOptions()
          .setDescription("Not persisted, owner can publish")
          .setMaxItems(0)
          .setPublisherType(PublisherType.owner);
    mTopicOptions.put("to", option);
    
    // persisted items, subscribers
    option = new MMXTopicOptions()
          .setDescription("Persisted, subscriber can publish")
          .setPublisherType(PublisherType.subscribers)
          .setMaxItems(1);
    mTopicOptions.put("ps", option);
    
    // persisted items, owner
    option = new MMXTopicOptions()
            .setDescription("Persisted, owner can publish")
            .setMaxItems(1)
            .setPublisherType(PublisherType.owner);
//          .setAccessList(accessList);
    mTopicOptions.put("po", option);
    
    // transient item, anyone
    option = new MMXTopicOptions()
            .setDescription("Not persisted, anyone can publish")
            .setMaxItems(0)
            .setPublisherType(PublisherType.anyone);
    mTopicOptions.put("ta", option);
  }
  
//  private List<String> parseList(String list) {
//    if (list == null)
//      return null;
//    String[] userIds = list.split(",");
//    return Arrays.asList(userIds);
//  }
  
//  private void setAccessList(String list) {
//    mTopicOptions[2].setAccessList(parseList(list));
//  }
  
  private void createTopic(String topic, String option, boolean personal,
                            int maxItems) throws MMXException {
    MMXTopicOptions options = mTopicOptions.get(option);
    if (options != null) {
      options.setMaxItems(maxItems);
    }
    
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    if (personal)
      mgr.createTopic(new MMXPersonalTopic(topic), options);
    else
      mgr.createTopic(new MMXGlobalTopic(topic), options);
    println("createTopic("+topic+")");
  }
  
  private void deleteTopic(String topic, boolean personal) throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    if (personal)
      mgr.deleteTopic(new MMXPersonalTopic(topic));
    else
      mgr.deleteTopic(new MMXGlobalTopic(topic));
    println("deleteTopic("+topic+") success");
  }
  
  private void publish(String topic, String userId, String text)
                        throws MMXException {
    String itemId;
    String data = GsonData.getGson().toJson(text);
    MMXPayload payload = new MMXPayload(data)
                        .setContentType("application/json")
                        .setMetaData("type", "TEXTMSG");
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    MMXTopic mmxtopic = (userId == null) ?
        new MMXGlobalTopic(topic) : new MMXUserTopic(userId, topic);
    itemId = mgr.publish(mmxtopic, payload);
    println("published to topic="+topic+", itemID="+itemId);
  }
  
  private void getItems(String topic, String[] ids) throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    Map<String, MMXMessage> map = mgr.getItemsByIds(new MMXGlobalTopic(topic),
        Arrays.asList(ids));
    for (MMXMessage msg : map.values()) {
      println("getItems topic="+topic+", itemID="+msg.getId()+", msg="+msg);
    }
  }
  
  private void subscribe(String userId, String topic, boolean deviceOnly)
                          throws MMXException {
    String subscriptionId;
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    subscriptionId = mgr.subscribe(new MMXTopicId(userId, topic), deviceOnly);
    println("subscribe("+topic+") id="+subscriptionId);
  }
  
  private void unsubscribe(String userId, String topic, String subscriptionId)
                            throws MMXException {
    boolean success;
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    success = mgr.unsubscribe(new MMXTopicId(userId, topic), subscriptionId);
    println("unsubscribe("+topic+","+subscriptionId+") ="+success);
  }
  
  private void setOnline(boolean online) throws MMXException {
    mCon.setMessageFlow(online ? 0 : -1);
  }
  
  private void pingpong(String userId, String devId, boolean pingpong) 
                          throws MMXException {
    PingPongCommand type;
    PingPong payload;
    String id = Integer.toString(mRandom.nextInt(), 36);
    PushManager mgr = PushManager.getInstance(mCon);
    if (pingpong) {
      type = Constants.PingPongCommand.pingpong;
      payload = new PingPong(mCon.getUser(), id, "OK");
    } else {
      type = Constants.PingPongCommand.ping;
      payload = new PingPong(mCon.getUser(), id,
          "http://"+mSettings.getString(MMXSettings.PROP_HOST, "unknown")+
          ":8080/pingack?id="+id, "OK");
    }
    mgr.wakeup(userId, devId, type, payload);
  }
  
  private void geo(float lat, float lng) throws MMXException {
    GeoLoc geoLoc = new GeoLoc()
        .setAccuracy(20)
        .setLat(lat)
        .setLng(lng);
    String itemId = MMXGeoLogger.updateGeoLocation(mClient, geoLoc);
    println("published geo-location item ID="+itemId);
  }

  private void stalk(String userId, boolean start, String subscriptionId) throws MMXException {
    try {
      if (start) {
        subscriptionId = (String) Utils.invokeMethod(MMXGeoLogger.class, 
            "startTracking", mClient, userId, false);
        mUserSubs.put(userId, subscriptionId);
      } else {
        Utils.invokeMethod(MMXGeoLogger.class, "stopTracking", mClient, userId,
            subscriptionId);
      }
    } catch (Throwable e) {
      if (e instanceof MMXException)
        throw (MMXException) e;
      throw new MMXException(e);
    }
  }
  
  private void fetchAll(String topic, String subscriptionId) throws MMXException {
    int i = 0;
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    FetchOptions options = new FetchOptions()
        .setSubId(subscriptionId)
        .setMaxItems(1000);
    List<MMXMessage> msgs = mgr.getItems(new MMXTopicId(topic), options);
    for (MMXMessage msg : msgs) {
      println("Published items["+(i++)+"]="+msg);
    }
  }
  
  private void devTagsOp(TagOp cmd, String[] tags) throws MMXException {
    MMXStatus status = null;
    DeviceManager mgr = DeviceManager.getInstance(mCon);
    if (cmd == TagOp.add)
      status = mgr.addTags(Arrays.asList(tags));
    else if (cmd == TagOp.remove)
      status = mgr.removeTags(Arrays.asList(tags));
    else if (cmd == TagOp.set)
      status = mgr.setAllTags(Arrays.asList(tags));
    println("status="+status);
  }
  
  private void msgTagsOp(TagOp cmd, String msgId, String[] tags) throws MMXException {
    MMXStatus status = null;
    MessageManager mgr = MessageManager.getInstance(mCon);
    if (cmd == TagOp.add)
      status = mgr.addEvents(msgId, Arrays.asList(tags));
    else if (cmd == TagOp.remove)
      status = mgr.removeEvents(msgId, Arrays.asList(tags));
    else if (cmd == TagOp.set)
      status = mgr.setEvents(msgId, Arrays.asList(tags));
    println("status="+status);
  }
  
  private void topicTagsOp(TagOp cmd, String userId, String topic, String[] tags)
                              throws MMXException {
    MMXStatus status = null;
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    if (cmd == TagOp.add)
      status = mgr.addTags(new MMXTopicId(userId, topic), Arrays.asList(tags));
    else if (cmd == TagOp.remove)
      status = mgr.removeTags(new MMXTopicId(userId, topic), Arrays.asList(tags));
    else if (cmd == TagOp.set)
      status = mgr.setAllTags(new MMXTopicId(userId, topic), Arrays.asList(tags));
    println("status="+status);
  }
  
  private void userTagsOp(TagOp cmd, String[] tags) throws MMXException {
    MMXStatus status = null;
    AccountManager mgr = AccountManager.getInstance(mCon);
    if (cmd == TagOp.add)
      status = mgr.addTags(Arrays.asList(tags));
    else if (cmd == TagOp.remove)
      status = mgr.removeTags(Arrays.asList(tags));
    else if (cmd == TagOp.set)
      status = mgr.setAllTags(Arrays.asList(tags));
    println("status="+status);
  }
  
  private void getDevTags() throws MMXException {
    DeviceManager mgr = DeviceManager.getInstance(mCon);
    DevTags tags = mgr.getAllTags();
    println("device tags="+tags);
  }
  
  private void getMsgTags(String msgId) throws MMXException {
    MessageManager mgr = MessageManager.getInstance(mCon);
    MsgTags tags = mgr.getAllTags(msgId);
    println("message tags="+tags);
  }
  
  private void getTopicTags(String userId, String topic) throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    TopicTags tags = mgr.getAllTags(new MMXTopicId(userId, topic));
    println("message tags="+tags);
  }
  
  private void getUserTags() throws MMXException {
    AccountManager mgr = AccountManager.getInstance(mCon);
    UserTags tags = mgr.getAllTags();
    println("user tags="+tags);
  }
  
  private void summary() throws MMXException {
    List<MMXTopic> topics = new ArrayList<MMXTopic>();
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    List<MMXTopicInfo> list = mgr.listTopics();
    for (MMXTopicInfo topic : list) {
      if (!topic.isCollection()) {
        topics.add(topic.getTopic());
      }
    }
    List<TopicSummary> summaries = mgr.getTopicSummary(topics, null, null);
    for (TopicSummary summary : summaries) {
      println(summary.getTopicNode()+
          ", count="+summary.getCount()+
          ", pubdate="+summary.getLastPubTime());
    }
  }
  
  private void lastPubItems(int maxItems, Date since) throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    MMXStatus status = mgr.requestLastPublishedItems(maxItems, since);
    println("msg="+status.getMessage()+", code="+status.getCode());
  }
  
  private void fetchItems(String topic, Date since, Date until, int maxItems)
            throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    FetchOptions options = new FetchOptions()
      .setMaxItems(maxItems)
      .setSince(since)
      .setUntil(until);
    List<MMXMessage> msgs = mgr.getItems(new MMXTopicId(topic), options);
    for (MMXMessage msg : msgs) {
      println("Published item: "+msg);
    }
  }
  
  private MMXid[] parseTarget(String addresses) {
    String[] tos = addresses.split(",");
    MMXid[] xids = new MMXid[tos.length];
    for (int i = 0; i < tos.length; i++) {
      xids[i] = MMXid.parse(tos[i]);
    }
    return xids;
  }
  
  private HashMap<String, String> parseMap(String line) {
    HashMap<String, String> attrs = new HashMap<String, String>();
    String[] tokens = line.split(" ");
    for (String token : tokens) {
      if (token.isEmpty())
        continue;
      int sep = token.indexOf('=');
      String name = token.substring(0, sep).trim();
      String value = token.substring(sep+1).trim();
      attrs.put(name, value);
    }
    return attrs;
  }
  
  private List<MMXAttribute<UserQuery.Type>> parseList(String line) {
    ArrayList<MMXAttribute<UserQuery.Type>> attrs = 
        new ArrayList<MMXAttribute<UserQuery.Type>>();
    String[] tokens = line.split(" ");
    for (String token : tokens) {
      if (token.isEmpty())
        continue;
      int sep = token.indexOf('=');
      String name = token.substring(0, sep).trim();
      String value = token.substring(sep+1).trim();
      attrs.add(new MMXAttribute<UserQuery.Type>(UserQuery.Type.valueOf(name), value));
    }
    return attrs;
  }
  
  private List<String> parseMultiValues(String line) {
    ArrayList<String> list = new ArrayList<String>();
    String[] tokens = line.split(",");
    for (String token : tokens) {
      token = token.trim();
      if (token.isEmpty())
        continue;
      list.add(token);
    }
    return list;
  }
  
  private void getTopic(String userId, String topicName) throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    MMXTopic topic;
    if (userId != null && !userId.isEmpty())
      topic = new MMXUserTopic(userId, topicName);
    else
      topic = new MMXGlobalTopic(topicName);
    MMXTopicInfo info = mgr.getTopic(topic);
    println("get info="+info);
  }
  
  private void searchTopic(boolean and, HashMap<String, String> attrs)
                            throws MMXException {
    String value;
    SearchAction.Operator operator = and ?
        SearchAction.Operator.AND : SearchAction.Operator.OR;
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    TopicAction.TopicSearch search = new TopicAction.TopicSearch();
    if (attrs != null) {
      if ((value = attrs.get("name")) != null) {
        search.setTopicName(value);
      }
      if ((value = attrs.get("desc")) != null) {
        search.setDescription(value);
      }
      if ((value = attrs.get("tags")) != null) {
        search.setTags(parseMultiValues(value));
      }
    }
    MMXTopicSearchResult resp = mgr.searchBy(operator, search, null);
    for (MMXTopicInfo info : resp.getResults()) {
      println("search result="+info);
    }
  }
  
  private void searchTopicByTags(boolean and, String[] tags) throws MMXException {
    PubSubManager mgr = PubSubManager.getInstance(mCon);
    List<MMXTopicInfo> results = mgr.searchByTags(Arrays.asList(tags), and);
    for (MMXTopicInfo info : results) {
      println("search result="+info);
    }
  }
  
  private void changePwd(String passwd) throws MMXException {
    AccountManager acctMgr = AccountManager.getInstance(mCon);
    acctMgr.changePassword(passwd);
  }
  
  private void registerUser(String userId, String passwd, String[] tags)
      throws MMXException {
    AccountManager mgr = AccountManager.getInstance(mCon);
    UserCreate account = new UserCreate();
    account.setPriKey(mSettings.getString(MMXSettings.PROP_GUESTSECRET, null));
    account.setApiKey(mSettings.getString(MMXSettings.PROP_APIKEY, null));
    account.setAppId(mSettings.getString(MMXSettings.PROP_APPID, null));
    account.setCreateMode(UserCreateMode.UPGRADE_USER);
    account.setUserId(userId);
    account.setPassword(passwd);
    account.setDisplayName(Character.toUpperCase(userId.charAt(0))+userId.substring(1));
    if (tags != null && tags.length > 0) {
      account.setTags(Arrays.asList(tags));
    }
    MMXStatus status = mgr.createAccount(account);
    println(userId+": "+status);
  }
  
  private void getMsgStat(String[] msgIds) throws MMXException {
    MessageManager mgr = MessageManager.getInstance(mCon);
    Map<String, List<MMXMessageStatus>> map = mgr.getMessagesState(Arrays.asList(msgIds));
    for (Map.Entry<String, List<MMXMessageStatus>> entry : map.entrySet()) {
      for (MMXMessageStatus stat : entry.getValue()) {
        println("MsgID="+entry.getKey()+", stat="+stat);
      }
    }
  }
  
  private void log(String tag, String level) {
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
  
  private void help() {
    System.out.println(
        "\nCmd: config ec2|local|host user pwd, clone newuser newpwd, use props-file\n"+
        "     adminlogin [user pwd], adminlogout, appreg appname, log s|v|d|i|w|e|? [tag]\n" +
        "     connect [autocreate], connectanon, discon [unreg], online, offline\n"+
        
        "     userReg user pwd [tags...], userPwd passwd, userSet [email=x name=x], userGet [user],\n"+
        "     userSrch and|or [email=x] [name=x] [phone=x] [tags=x,...],\n"+
        "     userGetTags, userAddTags|userDelTags|userSetTags [tag ...],\n" +
        
        "     devGetTags, devAddTags|devDelTags|devSetTags [tag ...], devReg [devid], devQuery [user]\n" +
        
        "     msgGetTags msgid, msgAddTags|msgDelTags|msgSetTags msgid [tag ...], msgStat msgid[ ...]\n" +
        "     msgSend [user,...] rcpt text, msgSendU users rcpt text, msgSendRcpt rcptid\n" +
        "     msgSendBig users size c, msgSendTxtFile users size c, msgSendBinFile users size b\n"+
        
        "     topicList [b|g|p], topicListSubs [topic], topicSummary, topicRqtLast max since\n" +
        "     topicGet topic, topicSrch and|or [desc=x] [name=x] [tags=x,...]\n"+
        "     topicFetch topic since until, topicFetchall topic [sid], topicItem topic ids...\n"+
        "     topicCrt topic df|pa|to|ps|po|ta [max], topicDel topic\n"+
        "     topicPub topic text, topicSub topic devonly, topicUnsub topic [sid],\n"+
        "     topicGetTags topic, topicAddTags|topicDelTags|topicSetTags topic [tag ...]\n" +
        "     topicSrchTags and|or [tag ...]\n" +

        "     utopicGet uid topic, utopicCrt topic df|pa|to|ps|po|ta, utopicDel topic,\n"+
        "     utopicPub topic text, utopicSub uid topic devonly, utopicUnsub uid topic [sid],\n"+
        "     utopicGetTags uid topic, utopicAddTags|utopicDelTags|utopicSetTags uid topic [tag ...]\n" +

        "     geo lat lng, stalk user, unstalk user [subid], showsender true|false\n" +
        "     pingpong user devid, ping user devid, setview topic pubfile subfile, pubview topic, exit, ?");
  }
  
  private enum Command {
    adminlogin, adminlogout, appreg, cat, clone, config, connect, connectanon,
    discon, geo, showsender, log, online, offline, ping, pingpong, sat,
    stalk, unstalk, use, quit, exit, help, status, setview, pubview,

    devquery,
    devreg, 
    devaddtags,
    devgettags,
    devsettags,
    devdeltags,
    
    msggettags,
    msgsettags,
    msgaddtags,
    msgdeltags,
    msgsendbinfile,
    msgsendtxtfile,
    msgsendbig,
    msgsend,
    msgsendu,
    msgsendrcpt,
    msgstat,
    
    userreg,
    userpwd,
    userget,
    userset, 
    usergettags,
    usersettags,
    useraddtags,
    userdeltags,
    usersrch,
    
    topiccrt,
    topicdel,
    topicget,
    topicfetch,
    topicfetchall,
    topicitem,
    topiclist, 
    topiclistsubs,
    topicpub,
    topicsub,
    topicsrch,
    topicunsub,
    topicsummary,
    topicsettags,
    topicgettags,
    topicaddtags,
    topicdeltags,
    topicsrchtags,
    topicrqtlast,
    topicquery,

    utopiccrt,
    utopicdel,
    utopicget,
    utopicpub,
    utopicsub,
    utopicunsub,
    utopicsettags,
    utopicgettags,
    utopicaddtags,
    utopicdeltags,
  }

  static enum TagOp {
    add,
    remove,
    set,
  }
  
  public void run() {
    boolean done = false;
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    do {
      try {
        help();
        System.out.print(XIDUtil.getUserId(mCon.getUser())+"> ");
        System.out.flush();
        String cmdline = reader.readLine().trim();
        if (cmdline.isEmpty())
          continue;
        String[] cmds = cmdline.split(" ");
        if (cmds[0].equals("?")) {
          getStatus();
          continue;
        }
        Command command = null;
        try {
          command = Command.valueOf(cmds[0].toLowerCase());
        } catch (IllegalArgumentException e) {
          System.err.println("Invalid command");
          continue;
        }
        switch (command) {
        case adminlogin:
          if (cmds.length == 1)
            loginAdmin("admin", "admin");
          else {
            assertNumOfArgs(cmds, 2);
            loginAdmin(cmds[1], cmds[2]);
          }
          break;
        case adminlogout:
          logoutAdmin();
          break;
        case appreg:
          assertNumOfArgs(cmds, 1);
          registerApp(cmds[1]);
          break;

        case clone:
          assertNumOfArgs(cmds, 2);
          clone(cmds[1], cmds[2]);
          break;
        case config:
          assertNumOfArgs(cmds, 3);
          config(cmds[1], cmds[2], cmds[3]);
          break;
        case connect:
          connect(getBoolean(cmds, 1, false));
          break;
        case connectanon:
          connectAnonymously();
          break;
        case discon:
          discon(getBoolean(cmds, 1, false));
          break;
        case online:
          setOnline(true);
          break;
        case offline:
          setOnline(false);
          break;
        case log:
          assertNumOfArgs(cmds, 1);
          log(getString(cmds, 2, null), cmds[1]);
          break;

        // Device operations
        case devaddtags:
          devTagsOp(TagOp.add, getVarArgs(cmds, 1));
          break;
        case devquery:
          queryDev(getString(cmds, 1, null));
          break;
        case devreg:
          registerDev(getString(cmds, 1, mContext.getDeviceId()));
          break;
        case devdeltags:
          devTagsOp(TagOp.remove, getVarArgs(cmds, 1));
          break;
        case devgettags:
          getDevTags();
          break;
        case devsettags:
          devTagsOp(TagOp.set, getVarArgs(cmds, 1));
          break;


        // Messaging operations
        case msgdeltags:
          assertNumOfArgs(cmds, 1);
          msgTagsOp(TagOp.remove, cmds[1], getVarArgs(cmds, 1));
          break;
        case msgsendbinfile:
          assertNumOfArgs(cmds, 3);
          sendBinFile(cmds[1], Integer.parseInt(cmds[2]), Integer.parseInt(cmds[3]));
          break;
        case msgsendbig:
          assertNumOfArgs(cmds, 3);
          sendBigMsg(cmds[1], Integer.parseInt(cmds[2]), cmds[3].charAt(0));
          break;
        case msgsend:   // send reliable msg
          assertNumOfArgs(cmds, 3);
          sendMsg(cmds[1], Boolean.parseBoolean(cmds[2]), false,
              cmdline.substring(cmds[0].length()+cmds[1].length()+cmds[2].length()+3));
          break;
        case msgsendu:  // send unreliable msg
          assertNumOfArgs(cmds, 3);
          sendMsg(cmds[1], Boolean.parseBoolean(cmds[2]), true,
              cmdline.substring(cmds[0].length()+cmds[1].length()+cmds[2].length()+3));
          break;
        case msgsendrcpt:
          assertNumOfArgs(cmds, 1);
          sendReceipt(cmds[1]);
          break;
        case msgsendtxtfile:
          assertNumOfArgs(cmds, 3);
          sendTextFile(cmds[1], Integer.parseInt(cmds[2]), cmds[3].charAt(0));
          break;
        case msgsettags:
          assertNumOfArgs(cmds, 1);
          msgTagsOp(TagOp.set, cmds[1], getVarArgs(cmds, 1));
          break;
        case msgstat:
          assertNumOfArgs(cmds, 1);
          getMsgStat(getVarArgs(cmds, 1));
          break;
        case msgaddtags:
          assertNumOfArgs(cmds, 1);
          msgTagsOp(TagOp.add, cmds[1], getVarArgs(cmds, 1));
          break;
        case msggettags:
          assertNumOfArgs(cmds, 1);
          getMsgTags(cmds[1]);
          break;

        // Topic operations
        case topicsettags:
          assertNumOfArgs(cmds, 1);
          topicTagsOp(TagOp.set, null, cmds[1], getVarArgs(cmds, 2));
          break;
        case topiclistsubs:
          listSubs(getString(cmds, 1, null));
          break;
        case topiclist:
          listTopics(getString(cmds, 1, null));
          break;
        case topicsrch:
          assertNumOfArgs(cmds, 1);
          HashMap<String, String> tAttrs = null;
          if (cmds.length > 2) {
            tAttrs = parseMap(cmdline.substring(cmds[0].length()+cmds[1].length()+1));
          }
          searchTopic(cmds[1].equals("and"), tAttrs);
          break;
        case topicsrchtags:
          assertNumOfArgs(cmds, 2);
          searchTopicByTags(cmds[1].equals("and"), getVarArgs(cmds, 2));
          break;
        case topicsummary:
          summary();
          break;
        case topicunsub:
          assertNumOfArgs(cmds, 1);
          unsubscribe(null, cmds[1], getString(cmds, 2, null));
          break;
        case topicsub:
          assertNumOfArgs(cmds, 2);
          subscribe(null, cmds[1], Boolean.parseBoolean(cmds[2]));
          break;
        case topicdeltags:
          assertNumOfArgs(cmds, 1);
          topicTagsOp(TagOp.remove, null, cmds[1], getVarArgs(cmds, 2));
          break;
        case topicfetch:
          assertNumOfArgs(cmds, 3);
          fetchItems(cmds[1], getDateTime(cmds, 2, null), getDateTime(cmds, 3, null), -1);
          break;
        case topicfetchall:
          assertNumOfArgs(cmds, 1);
          fetchAll(cmds[1], getString(cmds, 2, null));
          break;
        case topicdel:
          assertNumOfArgs(cmds, 1);
          deleteTopic(cmds[1], false);
          break;
        case topicget:
          assertNumOfArgs(cmds, 1);
          getTopic(null, cmds[1]);
          break;
        case topicpub:
          assertNumOfArgs(cmds, 2);
          publish(cmds[1], null, cmdline.substring(cmds[0].length()+cmds[1].length()+2));
          break;
        case topiccrt:
          assertNumOfArgs(cmds, 2);
          createTopic(cmds[1], cmds[2], false, getInt(cmds, 3, 10));
          break;
        case topicrqtlast:
          assertNumOfArgs(cmds, 2);
          lastPubItems(Integer.parseInt(cmds[1]), getDateTime(cmds, 2, null));
          break;
        case topicaddtags:
          assertNumOfArgs(cmds, 1);
          topicTagsOp(TagOp.add, null, cmds[1], getVarArgs(cmds, 2));
          break;
        case topicgettags:
          assertNumOfArgs(cmds, 1);
          getTopicTags(null, cmds[1]);
          break;
        case topicitem:
          assertNumOfArgs(cmds, 2);
          getItems(cmds[1], getVarArgs(cmds, 2));
          break;
//        case topicquery:
//          assertNumOfArgs(cmds, 2);
//          searchTopic(Boolean.parseBoolean(cmds[1]), cmds[2]);
//          break;

        // User operations
        case userset:
          HashMap<String, String> setAttrs = parseMap(cmdline.substring(cmds[0].length()));
          accountUpdate(setAttrs);
          break;
        case userget:
          accountGet(getString(cmds, 1, null));
          break;
        case usersrch:
          assertNumOfArgs(cmds, 2);
          HashMap<String, String> uAttrs = parseMap(cmdline.substring(cmds[0].length()+cmds[1].length()+1));
          accountSearch(cmds[1].equals("and"), uAttrs);
          break;
        case usersettags:
          userTagsOp(TagOp.set, getVarArgs(cmds, 1));
          break;
        case userdeltags:
          userTagsOp(TagOp.remove, getVarArgs(cmds, 1));
          break;
        case userreg:
          assertNumOfArgs(cmds, 2);
          registerUser(cmds[1], cmds[2], getVarArgs(cmds, 3));
          break;
        case userpwd:
          assertNumOfArgs(cmds, 1);
          changePwd(cmds[1]);
          break;
        case usergettags:
          getUserTags();
          break;
        case useraddtags:
          userTagsOp(TagOp.add, getVarArgs(cmds, 1));
          break;

        // User topic operations
        case utopicsub:
          assertNumOfArgs(cmds, 3);
          subscribe(cmds[1], cmds[2], Boolean.parseBoolean(cmds[3]));
          break;
        case utopicunsub:
          assertNumOfArgs(cmds, 2);
          unsubscribe(cmds[1], cmds[2], getString(cmds, 3, null));
          break;
        case utopicsettags:
          assertNumOfArgs(cmds, 2);
          topicTagsOp(TagOp.set, cmds[1], cmds[2], getVarArgs(cmds, 3));
          break;
        case utopicdel:
          assertNumOfArgs(cmds, 1);
          deleteTopic(cmds[1], true);
          break;
        case utopicdeltags:
          assertNumOfArgs(cmds, 2);
          topicTagsOp(TagOp.remove, cmds[1], cmds[2], getVarArgs(cmds, 3));
          break;
        case utopicget:
          assertNumOfArgs(cmds, 2);
          getTopic(cmds[1], cmds[2]);
          break;
        case utopiccrt:
          assertNumOfArgs(cmds, 2);
          createTopic(cmds[1], cmds[2], true, getInt(cmds, 3, 10));
          break;
        case utopicpub:
          assertNumOfArgs(cmds, 2);
          publish(cmds[1], mCon.getUserId(), cmdline.substring(cmds[0].length()+cmds[1].length()+2));
          break;
        case utopicgettags:
          assertNumOfArgs(cmds, 2);
          getTopicTags(cmds[1], cmds[2]);
          break;
        case utopicaddtags:
          assertNumOfArgs(cmds, 2);
          topicTagsOp(TagOp.add, cmds[1], cmds[2], getVarArgs(cmds, 3));
          break;

        case setview:
          assertNumOfArgs(cmds, 3);
          setTopicWithTemplates(cmds[1], cmds[2], cmds[3]);
          break;
        case pubview:
          assertNumOfArgs(cmds, 1);
          pubTopicWithTemplate(cmds[1]);
          break;
        case cat:
          clearAllTopics();
          break;
        case sat:
          listAllTopics();
          break;
        case ping:
          assertNumOfArgs(cmds, 2);
          pingpong(cmds[1], cmds[2], false);
          break;
        case pingpong:
          assertNumOfArgs(cmds, 2);
          pingpong(cmds[1], cmds[2], true);
          break;
        case geo:
          assertNumOfArgs(cmds, 2);
          geo(Float.parseFloat(cmds[1]), Float.parseFloat(cmds[2]));
          break;
        case stalk:
          assertNumOfArgs(cmds, 1);
          stalk(cmds[1], true, null); 
          break;
        case unstalk:
          assertNumOfArgs(cmds, 1);
          stalk(cmds[1], false, getString(cmds, 2, null));
          break;
        case use:
          assertNumOfArgs(cmds, 1);
          use(cmds[1]);
          break;
        case quit:
        case exit:
          done = true;
          break;
        case showsender:
          assertNumOfArgs(cmds, 1);
          mDisplayName = cmds[1].equalsIgnoreCase("true");
          break;
        case status:
          getStatus();
          break;
        default:
          System.err.println("Invalid command");
          break;
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    } while (!done);
    
    mCon.disconnect();
    System.out.println("@@@ App exits now.");
    System.exit(0);
  }

  private Date getDateTime(String[] cmds, int index, Date defVal) {
    if (cmds.length <= index)
      return defVal;
    // Parse ISO-8601 Date/Time to Date.
    try {
      return sIsoFmt.parse(cmds[index]);
    } catch (Throwable e) {
      return defVal;
    }
  }
  
  private boolean getBoolean(String[] cmds, int index, boolean defVal) {
    if (cmds.length <= index)
      return defVal;
    return Boolean.parseBoolean(cmds[index]);
  }
  
  private int getInt(String[] cmds, int index, int defVal) {
    if (cmds.length <= index)
      return defVal;
    return Integer.parseInt(cmds[index]);
  }
  
  private String getString(String[] cmds, int index, String defVal) {
    if (cmds.length <= index)
      return defVal;
    return cmds[index];
  }
  
  private void assertNumOfArgs(String[] cmds, int numOfArgs) {
    if (cmds.length <= numOfArgs) {
      throw new IllegalArgumentException(cmds[0]+" expects "+numOfArgs+" param(s)");
    }
  }
  
  private String[] getVarArgs(String[] cmds, int startIndex) {
    if (cmds.length <= startIndex)
      return null;
    String[] args = new String[cmds.length-startIndex];
    System.arraycopy(cmds, startIndex, args, 0, args.length);
    return args;
  }
  
  @SuppressWarnings("unused")
  public static void main(String[] args) {   
    if (false) {
      HashMap<String, String> map = new HashMap<String, String>();
      map.put("msg1", "SUBMITTED");
      map.put("msg2", "QUEUED");
      map.put("msg3", "DELIVERED");
      String json = GsonData.getGson().toJson(map);
      System.out.println("JSON of map="+json);
      ArrayList<String> list = new ArrayList<String>();
      list.add("msg1");
      list.add("msg2");
      list.add("msg3");
      json = GsonData.getGson().toJson(list);
      System.out.println("JSON of list="+json);
      json = GsonData.getGson().toJson("Xml <tags> & 5'8\" tall");
      System.out.println("JSON of xml string="+json);
      System.out.println("=> "+GsonData.getGson().fromJson(json, String.class));
    }
    
    try {
      TestApp testApp;
      
      if (args.length == 1) {
        String name = args[0];
        testApp = new TestApp(name);
        testApp.run();
      } else {
        System.err.println("java TestApp settings");
        System.exit(1);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}

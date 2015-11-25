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
package com.magnet.mmx.server.plugin.mmxmgmt.handler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Count;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.PushResult;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.protocol.PushMessage.Action;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSPushMessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DbInteractionException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceNotFoundException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.gcm.GCMPushMessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushAPNSPayloadBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushGCMPayloadBuilder;
import com.magnet.mmx.util.GsonData;

/**
 * The internal class that IQ or pubsub or REST can use to send a push/wake-up
 * message.
 *
 */
public class MMXPushManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXPushManager.class);
  private static MMXPushManager sInstance = new MMXPushManager();
  
  public static class ApsAlert {
    @SerializedName("title")
    public String mTitle;
    @SerializedName("body")
    public String mBody;
    @SerializedName("title-loc-key")
    public String mTitleLocKey;
    @SerializedName("title-loc-args")
    public List<String> mTitleLocArgs;
    @SerializedName("action-loc-key")
    public String mActionLocKey;
    @SerializedName("loc-key")
    public String mLocKey;
    @SerializedName("loc-args")
    public List<String> mLocArgs;
    @SerializedName("launch-image")
    public String mLaunchImage;
    
    public String toString() {
      return "{title="+mTitle+", body="+mBody+", launch-image="+mLaunchImage+"}";
    }
  }

  // Use with "aps" element.
  public static class ApsPayload {
    @SerializedName("alert")
    public Object mAlert;       // Either String or ApsAlert
    @SerializedName("badge")
    public Integer mBadge;
    @SerializedName("sound")
    public String mSound;
    @SerializedName("content-available")
    public Integer mContentAvailable;
    @SerializedName("category")
    public String mCategory;
    
    @Override
    public String toString() {
      return "{alert="+mAlert+", badge="+mBadge+", sound="+mSound+", category="
              +mCategory+", content-avaliable="+mContentAvailable+"}";
    }
  }

  public static class GcmPayload {
    @SerializedName("title")
    public String mTitle;
    @SerializedName("body")
    public String mBody;
    @SerializedName("icon")
    public String mIcon;
    @SerializedName("sound")
    public String mSound;
  }

  public static MMXPushManager getInstance() {
    return sInstance;
  }
  
  private MMXPushManager() {
  }
  
  public PushResult send(JID from, String appId, MMXid to, Action action,
                          String customType, String jsonPayload) {
    LOGGER.trace("@@@ send() from="+from+", to="+to+", action="+action+
                 ", customType="+customType+", payload="+jsonPayload);
    String userId = to.getUserId();
    String devId = to.getDeviceId();
    
    boolean abortOnError;
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    List<DeviceEntity> gcmDevices = new ArrayList<DeviceEntity>(1);
    List<DeviceEntity> apnsDevices = new ArrayList<DeviceEntity>(1);
    if (devId != null) {
      // Push message to an end-point which must be active.
      abortOnError = true;
      try {
        DeviceEntity de = deviceDAO.getDeviceUsingId(appId, devId, DeviceStatus.ACTIVE);
        if (de == null) {
          PushResult result = new PushResult();
          result.setCount(new Count(1, 0, 1));
          result.setUnsentList(Arrays.asList(new PushResult.Unsent(devId,
              ErrorCode.INVALID_DEVICE_ID.getCode(), "Device not found or not active")));
          return result;
        }
        if (de.getTokenType() == PushType.APNS) {
          apnsDevices.add(de);
        } else if (de.getTokenType() == PushType.GCM) {
          gcmDevices.add(de);
        }
      } catch (DbInteractionException e) {
        LOGGER.error("Cannot validate deviceID="+devId, e);
        PushResult result = new PushResult();
        result.setCount(new Count(1, 0, 1));
        result.setUnsentList(Arrays.asList(new PushResult.Unsent(devId,
            ErrorCode.ISE_DEVICES_GET_BY_ID.getCode(), e.getMessage())));
        return result;
      }
    } else {
      // Push message to a user; get all active devices registered to the user.
      abortOnError = false;
      List<DeviceEntity> deList = deviceDAO.getDevices(appId, userId,
          DeviceStatus.ACTIVE);
      for (DeviceEntity de : deList) {
        if (de.getTokenType() == PushType.APNS) {
          apnsDevices.add(de);
        } else if (de.getTokenType() == PushType.GCM) {
          gcmDevices.add(de);
        }
      }
    }
    
    PushResult result = null;
    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
    AppEntity appEntity = appDAO.getAppForAppKey(appId);
    if (apnsDevices.size() > 0) {
      Map<String, Object> map = GsonData.getGson().fromJson(
          jsonPayload, HashMap.class);
      MMXPushAPNSPayloadBuilder builder = new MMXPushAPNSPayloadBuilder(action);
      // Extract APNS specific elements from the request.
      ApsPayload apsPayload = GsonData.fromMap(map, ApsPayload.class);
      if (apsPayload.mAlert == null &&
          (map.get(Constants.PAYLOAD_PUSH_TITLE) != null ||
           map.get(Constants.PAYLOAD_PUSH_BODY) != null)) {
        ApsAlert alert = new ApsAlert();
        alert.mTitle = (String) map.get(Constants.PAYLOAD_PUSH_TITLE);
        alert.mBody = (String) map.get(Constants.PAYLOAD_PUSH_BODY);
        apsPayload.mAlert = alert;
      }
      builder.setAps(apsPayload);
      // Remove the APNS elements from the custom fields.
      if (apsPayload.mAlert != null) {
        map.remove(Constants.PAYLOAD_PUSH_TITLE);
        map.remove(Constants.PAYLOAD_PUSH_BODY);
      }
      if (apsPayload.mSound != null) {
        map.remove(Constants.PAYLOAD_PUSH_SOUND);
      }
      if (apsPayload.mBadge != null) {
        map.remove("badge");
      }
      if (apsPayload.mCategory != null) {
        map.remove("category");
      }
      builder.setCustomType(customType);
      builder.setCustomDictionary(map);
      APNSPushMessageSender sender = new APNSPushMessageSender(appEntity);
      result = sender.sendPush(apnsDevices, builder);
      if (result.getCount().getRequested() != result.getCount().getSent() &&
          abortOnError) {
        return result;
      }
    }
    if (gcmDevices.size() > 0) {
      Map<String, Object> map = GsonData.getGson().fromJson(
          jsonPayload, HashMap.class);
      MMXPushGCMPayloadBuilder builder = new MMXPushGCMPayloadBuilder(action, customType);
      GcmPayload gcmPayload = GsonData.fromMap(map, GcmPayload.class);
//      // Extract a set of Magnet pre-defined elements for GCM.
//      builder.setGcm(gcmPayload);
//      // TODO: leave the pre-defined elements in the custom payload?
//      if (gcmPayload.mTitle != null) {
//        map.remove(Constants.PAYLOAD_PUSH_TITLE);
//      }
//      if (gcmPayload.mBody != null) {
//        map.remove(Constants.PAYLOAD_PUSH_BODY);
//      }
//      if (gcmPayload.mIcon != null) {
//        map.remove(Constants.PAYLOAD_PUSH_ICON);
//      }
//      if (gcmPayload.mSound != null) {
//        map.remove(Constants.PAYLOAD_PUSH_SOUND);
//      }
      builder.setCustomType(customType);
      builder.setCustom(map);
      GCMPushMessageSender sender = new GCMPushMessageSender(appEntity);
      PushResult gcmRes = sender.sendPush(gcmDevices, builder);
      result = addResults(gcmRes, result);
      if (result.getCount().getRequested() != result.getCount().getSent() &&
          abortOnError) {
        return result;
      }
    }
    return result;
  }

  // Add two results.
  private PushResult addResults(PushResult r1, PushResult r2) {
    if (r1 == null)
      return r2;
    if (r2 == null)
      return r1;
    List<PushResult.PushIdTuple> sentList = new ArrayList<PushResult.PushIdTuple>(
        r1.getSentList().size() + r2.getSentList().size());
    sentList.addAll(r1.getSentList());
    sentList.addAll(r2.getSentList());
    List<PushResult.Unsent> unsentList = new ArrayList<PushResult.Unsent>(
        r1.getUnsentList().size() + r2.getUnsentList().size());
    unsentList.addAll(r1.getUnsentList());
    unsentList.addAll(r2.getUnsentList());
    Count count = new Count(
        r1.getCount().getRequested() + r2.getCount().getRequested(),
        r1.getCount().getSent() + r2.getCount().getSent(),
        r1.getCount().getUnsent() + r2.getCount().getUnsent());
    PushResult result = new PushResult();
    result.setCount(count);
    result.setSentList(sentList);
    result.setUnsentList(unsentList);
    return result;
  }
}
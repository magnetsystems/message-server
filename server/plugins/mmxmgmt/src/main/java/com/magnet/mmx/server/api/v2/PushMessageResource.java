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
package com.magnet.mmx.server.api.v2;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.PushMessage;
import com.magnet.mmx.protocol.PushResult;
import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXPushManager;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.util.GsonData;

/**
 * Push a payload to a user or a device using GCM or APNS.
 * 1. .../v2/push/user/{userID}
 * 2. .../v2/push/device/{deviceID}
 */
@Path("push")
public class PushMessageResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(
      PushMessageResource.class);

  /**
   * Push request payload.
   */
  public static class PushRequest {
    @SerializedName("action")
    private PushMessage.Action mAction;
    @SerializedName("name")
    private String mName;
    @SerializedName("payload")
    private Map<String, Object> mPayload;
    
    public PushMessage.Action getAction() {
      return mAction;
    }
    
    /**
     * Set the push (normal notification) or wakeup (silent notification) action.
     * @param action
     */
    public void setAction(PushMessage.Action action) {
      mAction = action;
    }
    
    public String getName() {
      return mName;
    }
    
    /**
     * Set the name of this push request.
     * @param name Name of this push request.
     */
    public void setName(String name) {
      mName = name;
    }
    
    public Map<String, Object> getPayload() {
      return mPayload;
    }
    
    /**
     * Set the custom payload for this push request.
     * @param payload A dictionary.
     */
    public void setPayload(Map<String, Object> payload) {
      mPayload = payload;
    }
  }
  
  /**
   * Push a payload to a user via GCM or APNS.  The payload will be pushed
   * to all active devices registered to the user.
   * @param headers
   * @param userId The user ID of the target user.
   * @param rqt The push request.
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("user/{userId}")
  public Response pushToUser(@Context HttpHeaders headers,
                              @PathParam("userId") String userId,
                              PushRequest rqt) {
    long startTime = System.nanoTime();

    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    LOGGER.trace("pushToUser : userId={}", userId);
    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    UserDAO userDAO = new UserDAOImpl(new OpenFireDBConnectionProvider());
    UserEntity ue = userDAO.getUser(JIDUtil.makeNode(userId, appId));
    if (ue == null) {
      ErrorResponse errorResponse = new ErrorResponse(
          ErrorCode.INVALID_USER_NAME.getCode(), "Invalid user ID");
      return RestUtils.getNotFoundJAXRSResp(errorResponse);
    }
    
    MMXPushManager pushMgr = MMXPushManager.getInstance();
    PushResult result = pushMgr.send(from, appId, new MMXid(userId, null),
        rqt.getAction(), rqt.getName(), ((rqt.getPayload() == null) ?
            null : GsonData.getGson().toJson(rqt.getPayload())));
    long endTime = System.nanoTime();
    LOGGER.info("Completed processing pushToUser in {} milliseconds",
        TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
    return RestUtils.getOKJAXRSResp(result);
  }

  /**
   * Push a payload to a device via GCM or APNS.  The device must be active.
   * @param headers
   * @param deviceId A unique registered device ID.
   * @return
   */
  @POST
  @Path("device/{devId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response pushToDevice(@Context HttpHeaders headers,
                                @PathParam("devId") String deviceId,
                                PushRequest rqt) {
    long startTime = System.nanoTime();

    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    LOGGER.trace("pushToDevice : devId={}", deviceId);
    JID from = RestUtils.createJID(tokenInfo);
    String appId = tokenInfo.getMmxAppId();

    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    DeviceEntity de = deviceDAO.getDeviceUsingId(appId, deviceId,
        DeviceStatus.ACTIVE);
    if (de == null) {
      ErrorResponse errorResponse = new ErrorResponse(
          ErrorCode.INVALID_DEVICE_ID, "Invalid device ID");
      return RestUtils.getNotFoundJAXRSResp(errorResponse);
    }
    
    MMXPushManager pushMgr = MMXPushManager.getInstance();
    PushResult result = pushMgr.send(from, appId,
        new MMXid(de.getOwnerId(), deviceId), rqt.getAction(),
        rqt.getName(), ((rqt.getPayload() == null) ?
            null : GsonData.getGson().toJson(rqt.getPayload())));
    long endTime = System.nanoTime();
    LOGGER.info("Completed processing pushToDevice in {} milliseconds",
        TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
    return RestUtils.getOKJAXRSResp(result);
  }
}

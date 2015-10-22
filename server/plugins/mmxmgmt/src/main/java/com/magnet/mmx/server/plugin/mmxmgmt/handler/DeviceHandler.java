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

import com.magnet.mmx.protocol.*;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXMaxDevicesPerAppLimitReachedEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.util.*;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.Utils;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Handler that handles the requests for managing devices
 */
public class DeviceHandler extends IQHandler {
  private static Logger LOGGER = LoggerFactory.getLogger(DeviceHandler.class);
  private static final String COMMAND_KEY = com.magnet.mmx.protocol.Constants.MMX_ATTR_COMMAND;
  /**
   * Constructor that takes the module name
   * @param moduleName
   */
  public DeviceHandler(String moduleName) {
    super(moduleName);
  }


  @Override
  public IQ handleIQ(IQ iq) throws UnauthorizedException {
    LOGGER.info("handleIQ is called");
    AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
    JID fromJID = iq.getFrom();
    String appId = JIDUtil.getAppId(fromJID);

    AppEntity appEntity = dao.getAppForAppKey(appId);
    if (appEntity == null) {
      return IQUtils.createErrorIQ(iq,
          DeviceOperationStatusCode.INVALID_APP_ID.getMessage(),
          DeviceOperationStatusCode.INVALID_APP_ID.getCode());
    }
    /*
      retrieve the command that we are processing
     */
    Element element = iq.getChildElement();
    String payload = element.getText();
    String command = element.attributeValue(COMMAND_KEY);
    if (command == null || command.isEmpty() || command.trim().isEmpty()) {
      return IQUtils.createErrorIQ(iq, 
          DeviceOperationStatusCode.INVALID_COMMAND_VALUE.getMessage(),
          DeviceOperationStatusCode.INVALID_COMMAND_VALUE.getCode());
    }

    Constants.DeviceCommand deviceCommand = null;
    try {
      deviceCommand = Constants.DeviceCommand.valueOf(command.toUpperCase());
    } catch (IllegalArgumentException e) {
      LOGGER.info("Invalid device command string:" + command, e);
    }
    if (deviceCommand == null) {
      return IQUtils.createErrorIQ(iq, 
          DeviceOperationStatusCode.INVALID_COMMAND_VALUE.getMessage(),
          DeviceOperationStatusCode.INVALID_COMMAND_VALUE.getCode());
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Processing command:" + deviceCommand.toString());
    }

    switch (deviceCommand) {
    case REGISTER:
      return processRegistration(iq, fromJID, appEntity, payload);
    case UNREGISTER:
      return processDeRegistration(iq, fromJID, appEntity, payload);
    case QUERY:
      return processQuery(iq, fromJID, appEntity, payload);
    case GETTAGS:
      return processGetTags(iq, fromJID, appEntity, payload);
    case SETTAGS:
      return processSetTags(iq, fromJID, appEntity, payload);
    case ADDTAGS:
      return processAddTags(iq, fromJID, appEntity, payload);
    case REMOVETAGS:
      return processRemoveTags(iq, fromJID, appEntity, payload);
    }
    return null;
  }

  @Override
  public IQHandlerInfo getInfo() {
    return new IQHandlerInfo(Constants.MMX, Constants.MMX_NS_DEV);
  }

  // Process device registration
  private IQ processRegistration(IQ input,JID fromJID, AppEntity appEntity, String payload) {
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    String appId = appEntity.getAppId();
    String userId = JIDUtil.getUserId(fromJID);

    DevReg deviceRequest = DevReg.fromJson(payload);

    IQ error = validateRequest(input, deviceRequest);
    if (error != null) {
      //validation failed
      return error;
    }

    DeviceEntity deviceEntity = deviceDAO.getDevice(deviceRequest.getDevId(), Helper.enumerateOSType(deviceRequest.getOsType()), appId);
    if (deviceEntity != null) {
      //this is an update
      String oldToken = deviceEntity.getClientToken();
      String newToken = deviceRequest.getPushToken();
      boolean changed = false;
      if (oldToken == null && newToken == null) {
        changed = false;
      } else if (oldToken == null && newToken != null) {
        changed = true;
      } else if (oldToken != null && newToken == null) {
        changed = true;
      } else if (!oldToken.equals(newToken)) {
        changed = true;
      }
      int rowCount = deviceDAO.updateDevice(deviceRequest.getDevId(), Helper.enumerateOSType(deviceRequest.getOsType()), appId, deviceRequest, userId, DeviceStatus.ACTIVE);

      if (changed) {
        PushStatus status = null;
        if (newToken != null) {
          status = PushStatus.VALID;
        }
        LOGGER.trace("processRegistration : updateDevice resultCount={}, deviceId={}", rowCount, deviceRequest.getDevId());
        deviceDAO.updatePushStatus(deviceRequest.getDevId(), Helper.enumerateOSType(deviceRequest.getOsType()), appId, status);
      }
    } else {
      int id =  deviceDAO.addDevice(userId, appId, deviceRequest);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(String.format("Device created with id:%d using request:%s for app id: %s", id, deviceRequest.toString(), appId));
      }
    }

    MMXStatus devResp = new MMXStatus()
      .setCode(DeviceOperationStatusCode.DEVICE_REGISTERED.getCode())
      .setMessage(DeviceOperationStatusCode.DEVICE_REGISTERED.getMessage());
    return IQUtils.createResultIQ(input, devResp.toJson());
  }

  private IQ processDeRegistration(IQ input,JID fromJID, AppEntity appEntity, String payload) {
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    int appId = appEntity.getId();
    DevId rqt = DevId.fromJson(payload);

    IQ error = validateRequest(input, rqt);
    if (error != null) {
      //validation failed
      return error;
    }
    int id = deviceDAO.deactivateDevice(rqt.getDevId());
    MMXStatus devResp = new MMXStatus()
      .setCode(DeviceOperationStatusCode.DEVICE_UNREGISTERED.getCode());
    return IQUtils.createResultIQ(input, devResp.toJson());
  }

  private DeviceInfo convert(DeviceEntity entity) {
    DeviceInfo devInfo = new DeviceInfo()
              .setCarrierInfo(entity.getCarrierInfo())
              .setDevId(entity.getDeviceId())
              .setDisplayName(entity.getName())
              .setModelInfo(null)
              .setOsType(entity.getOsType() == null ?
                  null : entity.getOsType().toString())
              .setOsVersion(entity.getOsVersion())
              .setPhoneNumber(entity.getPhoneNumber())
              .setExtras(null)
              .setPushType(entity.getTokenType() == null ?
                  null : entity.getTokenType().toString())
              .setPushToken(entity.getClientToken())
              .setTags(null);
    return devInfo;
  }
  
  // Process the query for device ID's owned by a user using user ID.
  private IQ processQuery(IQ input, JID fromJID, AppEntity appEntity, String payload) {
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    String appId = appEntity.getAppId();    
    String userId = GsonData.getGson().fromJson(payload, String.class);
    
    if (userId == null) {
      userId = JIDUtil.getUserId(fromJID);
    }
    IQ error = validateRequest(input, userId);
    if (error != null) {
      return error;
    }
    List<DeviceEntity> list = deviceDAO.getDevices(appId, userId, DeviceStatus.ACTIVE);
    DevList devList = new DevList(list.size());
    for (DeviceEntity entity : list) {
      devList.add(convert(entity));
    }
    return IQUtils.createResultIQ(input, GsonData.getGson().toJson(devList));
  }
  
  private IQ validateRequest(IQ input, DevReg deviceRequest) {
    String apiKey = deviceRequest.getApiKey();
    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
    AppEntity app = appDAO.getAppUsingAPIKey(apiKey);

    String ownerJid = JIDUtil.getUserId(input.getFrom());
    if (app == null) {
      return IQUtils.createErrorIQ(input,
          DeviceOperationStatusCode.INVALID_API_KEY.getMessage(),
          DeviceOperationStatusCode.INVALID_API_KEY.getCode());
    }
    String deviceId = deviceRequest.getDevId();
    if (deviceId == null || deviceId.isEmpty()) {
      return IQUtils.createErrorIQ(input,
          DeviceOperationStatusCode.INVALID_DEVICE_ID.getMessage(),
          DeviceOperationStatusCode.INVALID_DEVICE_ID.getCode());

    }
    String osTypeString = deviceRequest.getOsType();
    OSType osType = Helper.enumerateOSType(osTypeString);
    if (osType == null) {
      return IQUtils.createErrorIQ(input, 
          DeviceOperationStatusCode.INVALID_OS_TYPE.getMessage(),
          DeviceOperationStatusCode.INVALID_OS_TYPE.getCode());
    }

    String pushTypeString = deviceRequest.getPushType();
    if (pushTypeString != null) {
      PushType pushType = Helper.enumeratePushType(pushTypeString);
      if (pushType == null) {
        return IQUtils.createErrorIQ(input,
            DeviceOperationStatusCode.INVALID_PUSH_TYPE.getMessage(),
            DeviceOperationStatusCode.INVALID_PUSH_TYPE.getCode());
      }
    }

    //validate phone number
    String phoneNumber = deviceRequest.getPhoneNumber();
    if (phoneNumber != null) {
      boolean valid = Helper.checkPhoneNumber(phoneNumber);
      if (!valid) {
        return IQUtils.createErrorIQ(input,
            DeviceOperationStatusCode.INVALID_PHONE_NUMBER.getMessage(),
            DeviceOperationStatusCode.INVALID_PHONE_NUMBER.getCode());
      }
    }

    //validate the carrier information
    String carrierInfo = deviceRequest.getCarrierInfo();
    if (carrierInfo != null) {
      CarrierEnum carrierEnum = Helper.enumerateCarrierInfo(carrierInfo);
      if (carrierEnum == null) {
        return IQUtils.createErrorIQ(input,
            DeviceOperationStatusCode.INVALID_CARRIER.getMessage(),
            DeviceOperationStatusCode.INVALID_CARRIER.getCode());
      }
    }
    String appId = app.getAppId();
    if(AlertsUtil.maxDevicesPerAppLimitReached(appId, ownerJid)) {
      DeviceOperationStatusCode code = DeviceOperationStatusCode.DEVICES_PER_APP_EXCEEDED;
      code.setMessage("Reached devices per app limit : " + MMXConfiguration.getConfiguration().getLong(MMXConfigKeys.MAX_DEVICES_PER_APP, -1));
      int limit = AlertsUtil.getMaxDevicePerAppLimit();
      AlertEventsManager.post(new MMXMaxDevicesPerAppLimitReachedEvent(appId, limit));
      return IQUtils.createErrorIQ(input, code.getMessage(), code.getCode());
    }

    return null;
  }

  private IQ processGetTags(IQ iq, JID fromJID, AppEntity appEntity, String payload) {
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    String appId = appEntity.getAppId();
    String userId = JIDUtil.getUserId(fromJID);
    DevId rqt = GsonData.getGson().fromJson(payload, DevId.class);

    DeviceEntity devEntity = deviceDAO.getDeviceUsingId(appId, rqt.getDevId(),
        DeviceStatus.ACTIVE);
    if (devEntity == null) {
      return IQUtils.createErrorIQ(iq, "Device ID not found", StatusCode.NOT_FOUND);
    }
    if (!userId.equals(devEntity.getOwnerId())) {
      return IQUtils.createErrorIQ(iq, "Not a device owner", StatusCode.FORBIDDEN);
    }
    
    List<TagEntity> tagEntities = DBUtil.getTagDAO().getTagEntitiesForDevice(devEntity);
    List<String> tags = new ArrayList<String>();
    List<Date> dates = new ArrayList<Date>();
    for(TagEntity tagEntity : tagEntities) {
      tags.add(tagEntity.getTagname());
      dates.add(tagEntity.getCreationDate());
    }

    Collections.sort(dates, Collections.reverseOrder());

    DevTags devTags = new DevTags(rqt.getDevId(), tags, 
        Utils.isNullOrEmpty(dates) ? null : dates.get(0));
    return IQUtils.createResultIQ(iq, devTags.toJson());
  }
  
  private IQ processSetTags(IQ iq, JID fromJID, AppEntity appEntity, String payload) {
    String appId = appEntity.getAppId();
    String userId = JIDUtil.getUserId(fromJID);
    DevTags rqt = DevTags.fromJson(payload);
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());

    DeviceEntity devEntity = deviceDAO.getDeviceUsingId(appId, rqt.getDevId(),
            DeviceStatus.ACTIVE);
    if (devEntity == null) {
      return IQUtils.createErrorIQ(iq, "Device not found", StatusCode.NOT_FOUND);
    }

    if (!userId.equals(devEntity.getOwnerId())) {
      return IQUtils.createErrorIQ(iq, "Not a device owner", StatusCode.FORBIDDEN);
    }

    TagDAO tagDAO = DBUtil.getTagDAO();
    tagDAO.deleteAllTagsForDevice(devEntity);

    List<String> tagNames = rqt.getTags();

    if (!Utils.isNullOrEmpty(tagNames)) {
      for(String tag : tagNames) {
        try {
          tagDAO.createDeviceTag(tag, appId, devEntity.getId());
        } catch (DbInteractionException e) {
          LOGGER.error("processSetTags : unable to set tags for device={}", devEntity);
          return IQUtils.createErrorIQ(iq, "server error in creating tags", StatusCode.INTERNAL_ERROR);
        }
      }
    }

    MMXStatus status = new MMXStatus().setCode(StatusCode.SUCCESS).setMessage("successfully set tags");
    return IQUtils.createResultIQ(iq, status.toJson());
  }
  
  private IQ processAddTags(IQ iq, JID fromJID, AppEntity appEntity, String payload) {
    String appId = appEntity.getAppId();
    String userId = JIDUtil.getUserId(fromJID);
    DevTags rqt = DevTags.fromJson(payload);
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    
    DeviceEntity devEntity = deviceDAO.getDeviceUsingId(appId, rqt.getDevId(), 
        DeviceStatus.ACTIVE);
    if (devEntity == null) {
      return IQUtils.createErrorIQ(iq, "Device not found", StatusCode.NOT_FOUND);
    }
    if (!userId.equals(devEntity.getOwnerId())) {
      return IQUtils.createErrorIQ(iq, "Not a device owner", StatusCode.FORBIDDEN);
    }
    
    // Add the tags
    for(String tag : rqt.getTags()) {
      try {
        DBUtil.getTagDAO().createDeviceTag(tag, devEntity.getAppId(), devEntity.getId());
      } catch (IllegalArgumentException e) {
        return IQUtils.createErrorIQ(iq, e.getMessage(), StatusCode.BAD_REQUEST);
      } catch (Exception e) {
        return IQUtils.createErrorIQ(iq, "Device does not exist", StatusCode.FORBIDDEN);
      }
    }

    MMXStatus status = new MMXStatus().setCode(StatusCode.SUCCESS).setMessage("successfully added tags");
    return IQUtils.createResultIQ(iq, status.toJson());
  }

  private IQ processRemoveTags(IQ iq, JID fromJID, AppEntity appEntity, String payload) {
    String appId = appEntity.getAppId();
    String userId = JIDUtil.getUserId(fromJID);
    DevTags rqt = DevTags.fromJson(payload);
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    
    DeviceEntity devEntity = deviceDAO.getDeviceUsingId(appId, rqt.getDevId(), 
        DeviceStatus.ACTIVE);
    if (devEntity == null) {
      return IQUtils.createErrorIQ(iq, "Device not found", StatusCode.NOT_FOUND);
    }
    if (!userId.equals(devEntity.getOwnerId())) {
      return IQUtils.createErrorIQ(iq, "Not a device owner", StatusCode.FORBIDDEN);
    }
    
    TagDAO tagDAO = DBUtil.getTagDAO();
    if(!Utils.isNullOrEmpty(rqt.getTags()))
      tagDAO.deleteTagsForDevice(devEntity, rqt.getTags());

    MMXStatus status = new MMXStatus().setCode(StatusCode.SUCCESS).setMessage("successfully removed tags");
    return IQUtils.createResultIQ(iq, status.toJson());
  }
  
  /**
   * Validate the un reg request.
   * @param input
   * @param deviceRequest
   * @return a non null IQ if the validation fails
   */
  private IQ validateRequest(IQ input, DevId deviceRequest) {
    String deviceId = deviceRequest.getDevId();
    if (deviceId == null || deviceId.isEmpty()) {
      return IQUtils.createErrorIQ(input,
          DeviceOperationStatusCode.INVALID_DEVICE_ID.getMessage(),
          DeviceOperationStatusCode.INVALID_DEVICE_ID.getCode());
    }
    return null;
  }
  
  private IQ validateRequest(IQ input, String userId) {
    if (userId == null || userId.isEmpty()) {
      return IQUtils.createErrorIQ(input,
          DeviceOperationStatusCode.INVALID_USER_ID.getMessage(),
          DeviceOperationStatusCode.INVALID_USER_ID.getCode());
    }
    return null;
  }
  /**
   * Enum for the status codes
   */
  public static enum DeviceOperationStatusCode {
    DEVICE_REGISTERED(201, "Device registered"),
    DEVICE_UNREGISTERED(200, "Device unregistered"),
    INVALID_APP_ID (400, "Invalid app ID"),
    INVALID_API_KEY(400, "Invalid api key"),
    INVALID_USER_ID(400, "Invalid user ID"),
    INVALID_DEVICE_ID (400, "Invalid deviceId"),
    INVALID_OS_TYPE (400, "Invalid osType"),
    INVALID_PUSH_TYPE (400, "Invalid pushType"),
    INVALID_COMMAND_VALUE (400, "Invalid command value"),
    INVALID_PHONE_NUMBER (400, "Invalid phone number"),
    INVALID_CARRIER (400, "Invalid carrier"),
    INVALID_OWNER_ID(400, "Invalid Owner ID"),
    DEVICES_PER_APP_EXCEEDED(400, ""),
    DEVICES_PER_USER_EXCEEDED(400, "");

    private int code;
    private String message;
    DeviceOperationStatusCode(int c, String m) {
      code = c;
      message = m;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }
}

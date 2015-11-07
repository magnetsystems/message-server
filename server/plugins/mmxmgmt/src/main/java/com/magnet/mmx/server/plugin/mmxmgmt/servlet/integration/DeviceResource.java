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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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

import com.magnet.mmx.protocol.CarrierEnum;
import com.magnet.mmx.protocol.DeviceInfo;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceNotFoundException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXMaxDevicesPerAppLimitReachedEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.DeviceHandler;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AlertEventsManager;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AlertsUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;

/**
 * This API can be used by REST client as well as Blowfish server.  It uses a
 * more secured OAuth token to identify the requester.
 */
@Path("/mmx/devices")
public class DeviceResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationDeviceResource.class);

  @Path("{deviceId}")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateDevice(@Context HttpHeaders headers, 
                                @PathParam("deviceId") String deviceId,
                                com.magnet.mmx.protocol.DeviceInfo deviceInfo) {
    LOGGER.trace("updateDevice : getting device for device={}", deviceInfo);
    if (deviceInfo == null) {
      return RestUtils.getBadReqJAXRSResp(new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
              "device payload is illegal or empty"));
    }
    
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    String appId = tokenInfo.getMmxAppId();
    String userId = tokenInfo.getUserId();
    
    try {
      DeviceDAO dao = DBUtil.getDeviceDAO();
      DeviceEntity entity = dao.getDevice(tokenInfo.getMmxAppId(), deviceId);
      if (!userId.equalsIgnoreCase(entity.getOwnerId())) {
        ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_UPDATE_ERROR.getCode(),
            String.format("Device with id:%s is not owned by %s", deviceId, userId));
        return RestUtils.getJAXRSResp(Response.Status.FORBIDDEN, response);
      }
      int id = dao.addDevice(userId, appId, deviceInfo);
      if (id == 0) {
        ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_UPDATE_ERROR.getCode(),
            String.format("Unable to update device with id:%s", deviceId));
        return RestUtils.getInternalErrorJAXRSResp(response);
      }
      return RestUtils.getOKJAXRSResp();
    } catch (DeviceNotFoundException e) {
      ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_UPDATE_ERROR.getCode(),
          String.format("Device with id:%s not found", deviceId));
      return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, response);
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in create topic", e);
      ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_UPDATE_ERROR.getCode(),
          e.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(response);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/register")
  public Response registerDevice(@Context HttpHeaders headers, com.magnet.mmx.protocol.DeviceInfo device) {
    LOGGER.trace("createDevice : creating device for device={}", device);
    try {
      TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
      if (tokenInfo == null) {
        return RestUtils.getUnauthJAXRSResp();
      }
      String appId = tokenInfo.getMmxAppId();
      String userId = tokenInfo.getUserId();

      ErrorResponse error = validateRequest(appId, userId, device);
      if (error != null) {
        return RestUtils.getBadReqJAXRSResp(error);
      }
      
      DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
      DeviceEntity deviceEntity = deviceDAO.getDevice(device.getDevId(), Helper.enumerateOSType(device.getOsType()), appId);
      if (deviceEntity != null) {
        //this is an update
        String oldToken = deviceEntity.getClientToken();
        String newToken = device.getPushToken();
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
        int rowCount = deviceDAO.updateDevice(device.getDevId(),
            Helper.enumerateOSType(device.getOsType()), appId, device, userId,
            DeviceStatus.ACTIVE);
        if (changed) {
          PushStatus status = null;
          if (newToken != null) {
            status = PushStatus.VALID;
          }
          LOGGER.trace("createDevice : updateDevice resultCount={}, deviceId={}", rowCount, device.getDevId());
          deviceDAO.updatePushStatus(device.getDevId(), Helper.enumerateOSType(device.getOsType()), appId, status);
        }
      } else {
        int id = deviceDAO.addDevice(userId, appId, device);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(String.format("Device created with id:%d using request:%s for app id: %s", id, device.toString(), appId));
        }
      }
      deviceEntity = deviceDAO.getDevice(appId, device.getDevId());
      com.magnet.mmx.server.api.v1.protocol.DeviceInfo devInfo = DeviceEntity.toDeviceInfo(deviceEntity);
      return RestUtils.getOKJAXRSResp(devInfo);
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in create device", e);
      ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(), e.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(response);
    } catch (DeviceNotFoundException e) {
      LOGGER.warn("SQL Exception in create topic", e);
      ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(), e.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(response);
    } catch (Throwable t) {
      LOGGER.warn("Unknown exception in create device", t);
      ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(), t.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(response);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{deviceId}/unregister")
  public Response unregisterDevice(@Context HttpHeaders headers, @PathParam("deviceId") String deviceId) {
    try {
      LOGGER.trace("deleteDevice : deleting device={}", deviceId);
      TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
      if (tokenInfo == null) {
        return RestUtils.getUnauthJAXRSResp();
      }
      String appId = tokenInfo.getMmxAppId();
      String userId = tokenInfo.getUserId();
      
      ErrorResponse validation = validateDeleteRequest(appId, deviceId);
      if (validation != null) {
        return RestUtils.getBadReqJAXRSResp(validation);
      }
      DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
      DeviceEntity deviceEntity = deviceDAO.getDevice(appId, deviceId);
      if (!userId.equalsIgnoreCase(deviceEntity.getOwnerId())) {
        ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_DELETION_ERROR.getCode(),
            String.format("Device with id:%s is not owned by %s", deviceId, userId));
        return RestUtils.getJAXRSResp(Response.Status.FORBIDDEN, response);
      }
      deviceDAO.deactivateDevice(deviceId);
      return RestUtils.getOKJAXRSResp();
    } catch (DeviceNotFoundException e) {
      LOGGER.info("Device Not found exception", e);
      ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_DELETION_ERROR.getCode(),
          String.format("Device with id:%s not found", deviceId));
      return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, response);
    } catch (Throwable t) {
      LOGGER.warn("Unknown exception in device delete", t);
      ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_DELETION_ERROR.getCode(), t.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(response);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{deviceId}")
  public Response getDevice(@Context HttpHeaders headers, @PathParam("deviceId") String deviceId) {
    try {
      LOGGER.trace("getDevice : getting device for id={}", deviceId);
      TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
      if (tokenInfo == null) {
        return RestUtils.getUnauthJAXRSResp();
      }
      
      String appId = tokenInfo.getMmxAppId();
      String userId = tokenInfo.getUserName();

      ErrorResponse validation = validateGetRequest(appId, deviceId);
      if (validation != null) {
        return RestUtils.getBadReqJAXRSResp(validation);
      }
      DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
      DeviceEntity deviceEntity = deviceDAO.getDevice(appId, deviceId);
      
      // TODO: should we check if the requester is the owner of the device?
      
      com.magnet.mmx.server.api.v1.protocol.DeviceInfo deviceInfo = DeviceEntity.toDeviceInfo(deviceEntity);
      return RestUtils.getOKJAXRSResp(deviceInfo);
    } catch (DeviceNotFoundException e) {
      LOGGER.info("Device Not found exception", e);
      ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_RETRIEVAL_ERROR.getCode(),
          String.format("Device with id:%s not found", deviceId));
      return RestUtils.getJAXRSResp(Response.Status.NOT_FOUND, response);
    } catch (Throwable t) {
      LOGGER.warn("Unknown exception in device retrieval", t);
      ErrorResponse response = new ErrorResponse(ErrorCode.DEVICE_RETRIEVAL_ERROR.getCode(), t.getMessage());
      return RestUtils.getInternalErrorJAXRSResp(response);
    }
  }

  /**
   * Validate create request.
   * @param appId
   * @param apiKey
   * @param ownerJid
   * @param deviceRequest
   * @return
   */
  private ErrorResponse validateRequest(String appId, String ownerJid, DeviceInfo deviceRequest) {
    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
    AppEntity app = appDAO.getAppForAppKey(appId);

    if (ownerJid == null || ownerJid.isEmpty()) {
      return new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(),
          DeviceHandler.DeviceOperationStatusCode.INVALID_OWNER_ID.getMessage());
    }

    if (app == null) {
      return new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(),
          DeviceHandler.DeviceOperationStatusCode.INVALID_APP_ID.getMessage());
    }
    String deviceId = deviceRequest.getDevId();
    if (deviceId == null || deviceId.isEmpty()) {
      return new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(),
          DeviceHandler.DeviceOperationStatusCode.INVALID_DEVICE_ID.getMessage());

    }
    String osTypeString = deviceRequest.getOsType();
    OSType osType = Helper.enumerateOSType(osTypeString);
    if (osType == null) {
      return new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(),
          DeviceHandler.DeviceOperationStatusCode.INVALID_OS_TYPE.getMessage());
    }

    String pushTypeString = deviceRequest.getPushType();
    if (pushTypeString != null) {
      PushType pushType = Helper.enumeratePushType(pushTypeString);
      if (pushType == null) {
        return new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(),
            DeviceHandler.DeviceOperationStatusCode.INVALID_PUSH_TYPE.getMessage());
      }
    }

    //validate phone number
    String phoneNumber = deviceRequest.getPhoneNumber();
    if (phoneNumber != null) {
      boolean valid = Helper.checkPhoneNumber(phoneNumber);
      if (!valid) {
        return new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(),
            DeviceHandler.DeviceOperationStatusCode.INVALID_PHONE_NUMBER.getMessage());
      }
    }
    //validate the carrier information
    String carrierInfo = deviceRequest.getCarrierInfo();
    if (carrierInfo != null) {
      CarrierEnum carrierEnum = Helper.enumerateCarrierInfo(carrierInfo);
      if (carrierEnum == null) {
        return new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(),
            DeviceHandler.DeviceOperationStatusCode.INVALID_CARRIER.getMessage());
      }
    }
    if(AlertsUtil.maxDevicesPerAppLimitReached(appId, ownerJid)) {
      DeviceHandler.DeviceOperationStatusCode code = DeviceHandler.DeviceOperationStatusCode.DEVICES_PER_APP_EXCEEDED;
      code.setMessage("Reached devices per app limit : " + MMXConfiguration.getConfiguration().getLong(MMXConfigKeys.MAX_DEVICES_PER_APP, -1));
      int limit = AlertsUtil.getMaxDevicePerAppLimit();
      AlertEventsManager.post(new MMXMaxDevicesPerAppLimitReachedEvent(appId, limit));
      return new ErrorResponse(ErrorCode.DEVICE_CREATION_ERROR.getCode(),
          code.getMessage());
    }
    return null;
  }

  /**
   * Validate delete request.
   * @param appId
   * @param deviceId
   * @return
   */
  private ErrorResponse validateDeleteRequest(String appId, String deviceId) {
    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
    AppEntity app = appDAO.getAppForAppKey(appId);

    if (app == null) {
      return new ErrorResponse(ErrorCode.DEVICE_DELETION_ERROR.getCode(),
          DeviceHandler.DeviceOperationStatusCode.INVALID_APP_ID.getMessage());
    }
    if (deviceId == null || deviceId.isEmpty()) {
      return new ErrorResponse(ErrorCode.DEVICE_DELETION_ERROR.getCode(),
          DeviceHandler.DeviceOperationStatusCode.INVALID_DEVICE_ID.getMessage());

    }
    return null;
  }

  /**
   * Validate get request.
   * @param appId
   * @param deviceId
   * @return
   */
  private ErrorResponse validateGetRequest(String appId, String deviceId) {
    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
    AppEntity app = appDAO.getAppForAppKey(appId);

    if (app == null) {
      return new ErrorResponse(ErrorCode.DEVICE_RETRIEVAL_ERROR.getCode(),
          DeviceHandler.DeviceOperationStatusCode.INVALID_APP_ID.getMessage());
    }
    if (deviceId == null || deviceId.isEmpty()) {
      return new ErrorResponse(ErrorCode.DEVICE_RETRIEVAL_ERROR.getCode(),
          DeviceHandler.DeviceOperationStatusCode.INVALID_DEVICE_ID.getMessage());
    }
    return null;
  }
}

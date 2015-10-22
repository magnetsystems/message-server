package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;/*   Copyright (c) 2015 Magnet Systems, Inc.
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

import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.api.v1.protocol.DeviceInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/integration/devices")
public class DeviceResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResource.class);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{deviceId}")
  public DeviceResponse getDevice(@PathParam("deviceId")String deviceId) {
    LOGGER.trace("getDevice : getting device for deviceId={}", deviceId);
    return null;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public DeviceResponse updateDevice(DeviceInfo deviceInfo) {
    LOGGER.trace("updateDevice : getting device for device={}", deviceInfo);
    if(deviceInfo == null)
      RestUtils.getBadReqJAXRSResp(new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
          "device payload is illegal or empty"));
    DeviceEntity entity = new DeviceEntity();

    DeviceDAO dao = DBUtil.getDeviceDAO();
    dao.persist(entity);

    return null;
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public DeviceResponse createDevice(DeviceInfo device) {
    LOGGER.trace("createDevice : getting device for device={}", device);
    return null;
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public DeviceResponse deleteDevice(DeviceInfo device) {
    LOGGER.trace("deleteDevice : deleting device={}", device);
    return null;
  }
}

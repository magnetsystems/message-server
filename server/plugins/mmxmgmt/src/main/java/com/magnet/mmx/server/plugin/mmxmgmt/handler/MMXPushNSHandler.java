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

import java.util.ArrayList;
import java.util.List;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.push.*;
import com.magnet.mmx.server.plugin.mmxmgmt.util.*;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

/**
 */
public class MMXPushNSHandler extends IQHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXPushNSHandler.class);

  public MMXPushNSHandler(String moduleName) {
    super(moduleName);
  }

  @Override
  public IQ handleIQ(IQ iq) throws UnauthorizedException {
    LOGGER.trace("handleIQ : {}", iq);
    JID fromJID = iq.getFrom();
    String appId = JIDUtil.getAppId(fromJID);
    Element element = iq.getChildElement();
    String command = element.attributeValue(Constants.MMX_ATTR_COMMAND);
    String toJID = element.attributeValue(Constants.MMX_ATTR_DST);
    String userId = JIDUtil.getUserId(toJID);
    String devId = new JID(toJID).getResource();
    LOGGER.trace("handleIQ : toJID={}, deviceId={}", toJID, devId);

    boolean abortOnFailure;
    List<String> deviceIds = new ArrayList<String>();
    if (devId != null) {
      // Push message to an end-point.
      abortOnFailure = true;
      deviceIds.add(devId);
    } else {
      // Push message to a user; get all the devices registered to the user.
      abortOnFailure = false;
      DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
      List<DeviceEntity> deList = deviceDAO.getDevices(appId, userId, DeviceStatus.ACTIVE);
      for (DeviceEntity de : deList) {
        deviceIds.add(de.getDeviceId());
      }
    }

    int count = 0;
    MMXPushMessageValidator validator = new IQPushMessageValidator();
    for (String deviceId : deviceIds) {
      MMXPushValidationResult validationResult = validator.validate(appId,
          userId, deviceId);
      if (validationResult instanceof MMXPushValidationFailure) {
        if (abortOnFailure) {
          return getRespIQ(iq, validationResult.getCode());
        } else {
          continue;
        }
      }

      String pushMessageId = PushUtil.generateId(appId, deviceId);
      MMXPayload mmxPayload = new MMXPushPayload(command, element.getText());
      PushSender sender =  new PushMessageSender();
      PushResult pushResult = sender.push(appId, deviceId, mmxPayload,
          ((MMXPushValidationSuccess)validationResult).getContext());

      if (pushResult.isError()) {
        if (abortOnFailure) {
          return getRespIQ(iq, PushStatusCode.ERROR_SENDING_PUSH);
        } else {
          continue;
        }
      }

      storeMessage(pushMessageId, deviceId, appId);
      ++count;
    }
    return getRespIQ(iq, (count == 0) ?
        PushStatusCode.NO_DEVICES_FOUND : PushStatusCode.SUCCESSFUL);
  }

  private void storeMessage(String messageId, String deviceId, String appId) {
    PushMessageEntity message = new PushMessageEntity();
    message.setAppId(appId);
    message.setDeviceId(deviceId);
    message.setMessageId(messageId);
    message.setState(PushMessageEntity.PushMessageState.PUSHED);
    message.setType(PushMessageEntity.PushMessageType.IQ_PUSH);
    DBUtil.getPushMessageDAO().add(message);
  }

  private IQ getRespIQ(IQ iq, PushStatusCode code) {
    switch(code) {
      case SUCCESSFUL:
        MMXStatus iqStatus = new MMXStatus();
        iqStatus.setCode(PushStatusCode.SUCCESSFUL.getCode());
        return IQUtils.createResultIQ(iq, iqStatus.toJson());
      default:
        return IQUtils.createErrorIQ(iq, code.getMessage(), code.getCode());
    }
  }

  @Override
  public IQHandlerInfo getInfo() {
    return new IQHandlerInfo(Constants.MMX, Constants.MMX_NS_MSG_PUSH);
  }
}

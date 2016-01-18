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


import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.PushResult;
import com.magnet.mmx.protocol.PushMessage.Action;
import com.magnet.mmx.server.plugin.mmxmgmt.util.*;
import com.magnet.mmx.util.GsonData;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

/**
 * Handle the IQ request from client to issue Push Message (non-silent
 * notification) message to another client.
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
    String customType = element.attributeValue(Constants.MMX_ATTR_COMMAND);
    String dst = element.attributeValue(Constants.MMX_ATTR_DST);
    LOGGER.trace("handleIQ : dst={}, type={}", dst, customType);
    
    MMXPushManager pushMsgMgr = MMXPushManager.getInstance();
    MMXid to = new MMXid(JIDUtil.getUserId(dst), JIDUtil.getResource(dst), null);
    PushResult result = pushMsgMgr.send(fromJID, appId, to,
        Action.PUSH, customType, element.getText());
    return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(result));
  }

  @Override
  public IQHandlerInfo getInfo() {
    return new IQHandlerInfo(Constants.MMX, Constants.MMX_NS_MSG_PUSH);
  }
}
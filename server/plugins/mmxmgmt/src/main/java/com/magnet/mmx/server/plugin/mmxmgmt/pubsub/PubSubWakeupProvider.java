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
package com.magnet.mmx.server.plugin.mmxmgmt.pubsub;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.WakeupProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.PubSubNotification;
import com.magnet.mmx.protocol.PushMessage;
import com.magnet.mmx.protocol.PushResult;
import com.magnet.mmx.protocol.PushResult.Unsent;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXPushManager;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TopicHelper;

public class PubSubWakeupProvider implements WakeupProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(PubSubWakeupProvider.class);

  @Override
  public void wakeup(WakeupProvider.Scope scope, JID userOrDev, Node node, Date pubDate) {
    String appId = JIDUtil.getAppId(userOrDev);
    String userId = JIDUtil.getUserId(userOrDev);
    String userName = userOrDev.getNode();
    String domain = userOrDev.getDomain();
    SessionManager sessionMgr = SessionManager.getInstance();
    MMXPushManager pushMsgMgr = MMXPushManager.getInstance();

    MMXTopicId topic = TopicHelper.parseNode(node.getNodeID());
    LOGGER.debug("@@@ wakeup(): scope="+scope+", jid="+userOrDev+", topic="+topic);
    String text = "New message is available";
    PubSubNotification pubsubNtf = new PubSubNotification(topic, pubDate, text);
    String pubsubPayload = GsonData.getGson().toJson(pubsubNtf);

    if (scope == WakeupProvider.Scope.no_wakeup)
      return;
    
    if (userOrDev.getResource() != null) {
      if (sessionMgr.getSession(userOrDev) == null) {
        // Wake up this disconnected device only.
        AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
        AppEntity ae = appDAO.getAppForAppKey(appId);
        JID fromJID = new JID(JIDUtil.makeNode(ae.getServerUserId(), appId),
            domain, null);
        PushResult result = pushMsgMgr.send(fromJID, appId, new MMXid(userId,
            userOrDev.getResource(), null), PushMessage.Action.WAKEUP,
            PubSubNotification.getType(), pubsubPayload);
        if (result.getCount().getRequested() != result.getCount().getSent()) {
          Unsent unsent = result.getUnsentList().get(0);
          LOGGER.warn(
              "pubsub wake up failed; count={}, devId={}, code={}, msg={}",
              result.getCount(), unsent.getDeviceId(), unsent.getCode(),
              unsent.getMessage());
        }
//        // It is possible that a device is subscribed to a node, but the
//        // device is no longer owned by the user.
//        LOGGER.warn("Cannot wake up non-existing device; appId="+appId+", devId="+userOrDev.getResource());
//        return;
      }
    } else {
      AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
      DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
      AppEntity ae = appDAO.getAppForAppKey(appId);
      JID fromJID = new JID(JIDUtil.makeNode(ae.getServerUserId(), appId),
          domain, null);
      List<DeviceEntity> deList = deviceDAO.getDevices(appId, userId, DeviceStatus.ACTIVE);
      List<DeviceEntity> devices = new ArrayList<DeviceEntity>(deList.size());
      for (DeviceEntity de : deList) {
        JID devJID = new JID(userName, domain, de.getDeviceId(), false);
        if (sessionMgr.getSession(devJID) == null) {
          devices.add(de);
        }
      }
      if (devices.size() > 0) {
        // Wake up all disconnected devices
        PushResult result = pushMsgMgr.send(fromJID, appId, devices,
            PushMessage.Action.WAKEUP, PubSubNotification.getType(),
            pubsubPayload);
        if (result.getCount().getRequested() != result.getCount().getSent()) {
          Unsent unsent = result.getUnsentList().get(0);
          LOGGER.warn("pubsub wake up failed; count={}, devId={}, code={}, msg={}",
              result.getCount(), unsent.getDeviceId(), unsent.getCode(),
              unsent.getMessage());
        }
      }
    }
  }
}

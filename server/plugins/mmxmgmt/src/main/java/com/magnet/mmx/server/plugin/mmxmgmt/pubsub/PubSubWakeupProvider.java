/*   Copyright (c) 2015-2016 Magnet Systems, Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.WakeupProvider;
import org.jivesoftware.util.JiveProperties;
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
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TopicHelper;
import com.magnet.mmx.util.Utils;

/**
 * Wakeup provider for pubsub notification.  The notification can be push
 * notification (push) or silent notification (wakeup.)  The push notification
 * consists of a parameterized title and body.  Currently only ${application.name}
 * ${channel.name}, ${channel.desc} are available to title and body.  For
 * example, the body or title can have a template as
 * "New message is available in ${channel.name}."
 */
public class PubSubWakeupProvider implements WakeupProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(PubSubWakeupProvider.class);
  private static final String BODY = "New message is available";

  @Override
  public void wakeup(WakeupProvider.Scope scope, JID userOrDev, Node node, Date pubDate) {
    String appId = JIDUtil.getAppId(userOrDev);
    String userId = JIDUtil.getUserId(userOrDev);
    String userName = userOrDev.getNode();
    String domain = userOrDev.getDomain();
    SessionManager sessionMgr = SessionManager.getInstance();
    MMXPushManager pushMsgMgr = MMXPushManager.getInstance();

    MMXTopicId topic = TopicHelper.parseNode(node.getNodeID());
    PushMessage.Action action;
    try {
      // Default is "push".  Other options are: "wakeup" or "" (disabling.)
      String type = JiveProperties.getInstance().getProperty(
          MMXConfigKeys.PUBSUB_NOTIFICATION_TYPE, PushMessage.Action.PUSH.toString());
      action = PushMessage.Action.valueOf(type.toUpperCase());
    } catch (Throwable e) {
      action = null;
    }

    LOGGER.debug("@@@ wakeup(): action="+action+", scope="+scope+", jid="+
                userOrDev+", topic="+topic);
    if (action == null || scope == WakeupProvider.Scope.no_wakeup) {
      return;
    }

    if (userOrDev.getResource() != null) {
      if (sessionMgr.getSession(userOrDev) == null) {
        // Wake up this disconnected device only.
        AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
        AppEntity ae = appDAO.getAppForAppKey(appId);
        if (!isPushEnabled(ae)) {
          return;
        }
        String pubsubPayload = makePubsubPayload(action, ae, topic, pubDate, node);
        JID fromJID = new JID(JIDUtil.makeNode(ae.getServerUserId(), appId),
            domain, null);
        PushResult result = pushMsgMgr.send(fromJID, appId, new MMXid(userId,
            userOrDev.getResource(), null), action, PubSubNotification.getType(),
            pubsubPayload);
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
      if (!isPushEnabled(ae)) {
        return;
      }
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
        String pubsubPayload = makePubsubPayload(action, ae, topic, pubDate, node);
        // Wake up all disconnected devices
        PushResult result = pushMsgMgr.send(fromJID, appId, devices, action,
            PubSubNotification.getType(), pubsubPayload);
        if (result.getCount().getRequested() != result.getCount().getSent()) {
          Unsent unsent = result.getUnsentList().get(0);
          LOGGER.warn("pubsub wake up failed; count={}, devId={}, code={}, msg={}",
              result.getCount(), unsent.getDeviceId(), unsent.getCode(),
              unsent.getMessage());
        }
      } else {
        LOGGER.trace("@@@ wakeup(): no disconnected (active) devices for jid="+
              userOrDev+", topic="+topic);
      }
    }
  }

  // Build a context with ${application.name}, ${channel.name}, ${channel.desc}
  // for title and body.
  private Map<String, String> makeContext(AppEntity ae, MMXTopicId topic,
                                          Date pubDate, Node node) {
    HashMap<String, String> context = new HashMap<String, String>();
    context.put("application.name", ae.getName());
    context.put("channel.name", node.getName());      // just its name (no user id)
    context.put("channel.desc", node.getDescription());
    return context;
  }

  private String makePubsubPayload(PushMessage.Action action, AppEntity ae,
                                   MMXTopicId topic, Date pubDate, Node node) {
    String pubsubPayload;
    Map<String, String> context = makeContext(ae, topic, pubDate, node);
    String body = JiveProperties.getInstance().getProperty(
        MMXConfigKeys.PUBSUB_NOTIFICATION_BODY, BODY);
    if (body != null) {
      body = Utils.eval(body, context).toString();
    }
    if (action == PushMessage.Action.PUSH) {
      // Push notification payload
      String title = JiveProperties.getInstance().getProperty(
          MMXConfigKeys.PUBSUB_NOTIFICATION_TITLE, null);
      if (title != null) {
        title = Utils.eval(title, context).toString();
      }
      pubsubPayload = GsonData.getGson().toJson(new PubSubNotification(topic,
          pubDate, title, body));
    } else {
      // Wakeup (silent) notification payload
      pubsubPayload = GsonData.getGson().toJson(new PubSubNotification(topic,
          pubDate, body));
    }
    return pubsubPayload;
  }

  private boolean isPushEnabled(AppEntity ae) {
    if ((ae.getApnsCert() != null && ae.getApnsCert().length > 0) ||
        (ae.getGoogleAPIKey() != null && ae.getGoogleProjectId() != null)) {
      return true;
    }
    LOGGER.debug("Push configuration is not set; pubsub wakeup is ignored in app {}",
        ae.getAppId());
    return false;
  }
}

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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.MmxHeaders;
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
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXPacketExtension;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TopicHelper;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * Wakeup provider for pubsub notification.  The notification can be push
 * notification (push) or silent notification (wakeup.)  The push notification
 * consists of a parameterized title and body.  The notification is configurable
 * through the Freemarker template language with the following objects and
 * their properties:
 * <table border="1">
 * <tr>
 * <th>Object</th>
 * <th colspan="3">Properties</th>
 * </tr>
 * <tr>
 * <td>application</td>
 * <td>name</td><td/><td/>
 * </tr>
 * <tr>
 * <td>channel</td>
 * <td>name</td>
 * <td>desc</td>
 * <td>count (int)</td>
 * </tr>
 * <tr>
 * <td>msg</td>
 * <td>from</td>
 * <td>date (Date)</td>
 * <td>content (Map)</td>
 * </tr>
 * </table>
 * The default template consists of a set of key-value pairs (Properties):
 * <pre>
 * mmx.pubsub.notification.type=push             # other values: wakeup, EMPTY
 * mmx.pubsub.notification.title=${channel.name}
 * mmx.pubsub.notification.body=New message from ${msg.from}
 * mmx.pubsub.notification.sound=default
 * </pre>
 */
public class PubSubWakeupProvider implements WakeupProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(PubSubWakeupProvider.class);

  // Template loader from DB.
  public static class DBTemplateLoader implements TemplateLoader {
    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
      // No-op
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
      String[] tokens = name.split(":");
      String appId = (tokens.length > 0) ? tokens[0] : null;
      String channelName = (tokens.length > 1) ? tokens[1] : null;
      String configName = (tokens.length > 2) ? tokens[2] : null;
      return MMXPushConfigService.getInstance().getPushConfig(appId,
          channelName, configName);
    }

    @Override
    public long getLastModified(Object templateSource) {
      if (templateSource instanceof MMXPushConfig) {
        MMXPushConfig pushConfig = (MMXPushConfig) templateSource;
//        return pushConfig.getTemplate().getLastModified();
        return 0;
      }
      return 0;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
      if (templateSource instanceof MMXPushConfig) {
        MMXPushConfig pushConfig = (MMXPushConfig) templateSource;
        return new StringReader(pushConfig.getTemplate().getTemplate());
      }
      return null;
    }
  }

  public static class PushConfig {
    private final String mName;
    private Properties mProps;
    private PushMessage.Action mPushType;
    private static Configuration sFmCfg;

    static {
      sFmCfg = new Configuration(Configuration.VERSION_2_3_24);
      sFmCfg.setTemplateLoader(new DBTemplateLoader());
      sFmCfg.setDefaultEncoding("UTF-8");
      sFmCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      sFmCfg.setLogTemplateExceptions(false);
    }

    public PushConfig(String appId, String topicPath, String configName) {
      StringBuilder name = new StringBuilder();
      if (appId != null) {
        name.append(appId);
        if (topicPath != null) {
          name.append(':').append(topicPath);
          if (configName != null) {
            name.append(':').append(configName);
          }
        }
      }
      mName = name.toString();
    }

    // Build a context with ${application}, ${channel}, and ${msg}
    private Map<String, Object> buildContext(AppEntity ae, MMXTopicId topic,
                                            MMXPacketExtension mmxExt0,
                                            Node node, int count) {
      HashMap<String, Object> context = new HashMap<String, Object>();
      context.put("application", new NameDesc(ae.getName(), null, 0));
      // just its name (no user id for user topic)
      context.put("channel", new NameDesc(node.getName(), node.getDescription(), count));
      String displayName;
      try {
        MMXid from = MMXid.fromMap((Map<String, String>)
            mmxExt0.getMmxMeta().get(MmxHeaders.FROM));
        if (((displayName = from.getDisplayName()) == null) || displayName.isEmpty()) {
          String username = JIDUtil.makeNode(from.getUserId(), ae.getAppId());
          displayName = UserManager.getInstance().getUser(username).getName();
        }
      } catch (UserNotFoundException e) {
        displayName = null;
      }
      context.put("msg", new MsgData(displayName,
          mmxExt0.getPayload().getSentTime(), mmxExt0.getHeaders()));
      return context;
    }

    // Use the oldest item to build the push payload; it avoids to have multiple
    // notifications.  Otherwise, template needs the ${msgs} as a collection.
    public String buildPushPayload(AppEntity ae, MMXTopicId topic,
                            List<MMXPacketExtension> mmxItems, Node node) {
      MMXPacketExtension item0 = mmxItems.get(0);
      Map<String, Object> context = buildContext(ae, topic, item0, node,
                                                  mmxItems.size());
      try {
        // Merge the context and template to form the output properties.
        StringWriter writer = new StringWriter();
        Template template = sFmCfg.getTemplate(mName);
        template.process(context, writer);
        String pushConfig = writer.getBuffer().toString();
        LOGGER.trace("@@@ push config="+pushConfig);
        Reader reader = new StringReader(pushConfig);
        mProps = new Properties();
        mProps.load(reader);

        // Default is "push".  Other options are: "wakeup" or "" (disabling.)
        String type = mProps.getProperty(MMXConfigKeys.PUBSUB_NOTIFICATION_TYPE,
            PushMessage.Action.PUSH.toString());
        try {
          mPushType = PushMessage.Action.valueOf(type.toUpperCase());
        } catch (Throwable e) {
          mPushType = null;
        }
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      } catch (TemplateException e) {
        e.printStackTrace();
        return null;
      }

      if (getScope() == WakeupProvider.Scope.no_wakeup) {
        return null;
      }
      String pushPayload;
      MMXid from = MMXid.fromMap((Map<String, String>) item0.getMmxMeta().get(MmxHeaders.FROM));
      if (getPushType() == PushMessage.Action.PUSH) {
        // Push notification payload
        pushPayload = GsonData.getGson().toJson(new PubSubNotification(topic,
            item0.getPayload().getSentTime(), from, getTitle(), getBody(), getSound()));
      } else if (getPushType() == PushMessage.Action.WAKEUP) {
        // Wakeup (silent) notification payload
        pushPayload = GsonData.getGson().toJson(new PubSubNotification(topic,
            item0.getPayload().getSentTime(), from, getBody()));
      } else {
        pushPayload = null;
      }
      return pushPayload;
    }

    public PushMessage.Action getPushType() {
      return mPushType;
    }

    public WakeupProvider.Scope getScope() {
      return WakeupProvider.Scope.all_devices;
    }

    public String getTitle() {
      return mProps.getProperty(MMXConfigKeys.PUBSUB_NOTIFICATION_TITLE,
          MMXPushConfigService.TITLE);
    }

    public String getBody() {
      return mProps.getProperty(MMXConfigKeys.PUBSUB_NOTIFICATION_BODY,
          MMXPushConfigService.BODY);
    }

    public String getSound() {
      return mProps.getProperty(MMXConfigKeys.PUBSUB_NOTIFICATION_SOUND, null);
    }
  }

  @Override
  public void wakeup(JID jid, Node node, List<MMXPacketExtension> mmxItems) {
    String appId = JIDUtil.getAppId(jid);
    String userId = JIDUtil.getUserId(jid);
    SessionManager sessionMgr = SessionManager.getInstance();
    MMXPushManager pushMsgMgr = MMXPushManager.getInstance();

    // if at least one item having publisher and subscriber being same user, or
    // self-notification enabled for the publisher, do the wakeup.
    // MAX-339 is to disable self notification.
    boolean doWakeup = false;
    for (MMXPacketExtension ext : mmxItems) {
      Boolean selfNotify;
      MMXid from = MMXid.fromMap((Map<String, String>) ext.getMmxMeta().get(
          MmxHeaders.FROM));
      if (!userId.equals(from.getUserId()) ||
          (((selfNotify = (Boolean) ext.getMmxMeta().get(
              MmxHeaders.SELF_NOTIFICATION)) != null) && selfNotify)) {
        doWakeup = true;
        break;
      }
    }
    if (!doWakeup) {
      LOGGER.trace("@@@ wakeup(): skip waking up jid="+jid);
      return;
    }

    // Use the first (oldest item) to construct the push payload.
    MMXPacketExtension mmx = mmxItems.get(0);
    String configName = (String) mmx.getMmxMeta().get(MmxHeaders.PUSH_CONFIG);
    MMXTopicId topic = TopicHelper.parseNode(node.getNodeID());

    LOGGER.debug("@@@ wakeup(): jid="+jid+", topic="+topic);

    if (jid.getResource() != null) {
      if (sessionMgr.getSession(jid) == null) {
        // Wake up this disconnected device only.
        AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
        AppEntity ae = appDAO.getAppForAppKey(appId);
        if (!isPushEnabled(ae)) {
          return;
        }
        PushConfig pushConf = new PushConfig(appId, topic.toPath(), configName);
        String pushPayload = pushConf.buildPushPayload(ae, topic, mmxItems, node);
        if (pushPayload == null) {
          return;
        }
        JID fromJID = JIDUtil.makeJID(ae.getServerUserId(), appId, null);
        PushResult result = pushMsgMgr.send(fromJID, appId, new MMXid(userId,
            jid.getResource(), null), pushConf.getPushType(),
            PubSubNotification.getType(), pushPayload);
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
      JID fromJID = JIDUtil.makeJID(ae.getServerUserId(), appId, null);
      List<DeviceEntity> deList = deviceDAO.getDevices(appId, userId, DeviceStatus.ACTIVE);
      List<DeviceEntity> devices = new ArrayList<DeviceEntity>(deList.size());
      for (DeviceEntity de : deList) {
        JID devJID = JIDUtil.makeJID(userId, appId, de.getDeviceId());
        if (sessionMgr.getSession(devJID) == null) {
          devices.add(de);
        }
      }
      if (devices.size() > 0) {
        PushConfig pushConf = new PushConfig(appId, topic.toPath(), configName);
        String pushPayload = pushConf.buildPushPayload(ae, topic, mmxItems, node);
        if (pushPayload == null) {
          return;
        }
        // Wake up all disconnected devices
        PushResult result = pushMsgMgr.send(fromJID, appId, devices,
            pushConf.getPushType(), PubSubNotification.getType(), pushPayload);
        if (result.getCount().getRequested() != result.getCount().getSent()) {
          Unsent unsent = result.getUnsentList().get(0);
          LOGGER.warn("pubsub wake up failed; count={}, devId={}, code={}, msg={}",
              result.getCount(), unsent.getDeviceId(), unsent.getCode(),
              unsent.getMessage());
        }
      } else {
        LOGGER.trace("@@@ wakeup(): no disconnected devices for jid="+jid+
                      ", topic="+topic+"; registered devices="+deList.size());
      }
    }
  }

  public static class NameDesc {
    private final String name;
    private final String desc;
    private final int count;

    public NameDesc(String name, String desc, int count) {
      this.name = name;
      this.desc = desc;
      this.count = count;
    }

    public String getName() {
      return this.name;
    }

    public String getDesc() {
      return this.desc;
    }

    public int getCount() {
      return count;
    }
  }

  public static class MsgData {
    private final String from;
    private final Date date;
    private final Map<String, String> content;

    public MsgData(String from, Date pubDate, Map<String, String> content) {
      this.from = from;
      this.date = pubDate;
      this.content = content;
    }

    public String getFrom() {
      return this.from;
    }

    public Date getDate() {
      return this.date;
    }

    public Map<String, String> getContent() {
      return this.content;
    }
  }

  private boolean isPushEnabled(AppEntity ae) {
    if ((ae.getApnsCert() != null && ae.getApnsCert().length > 0) ||
        (ae.getGoogleAPIKey() != null && ae.getGoogleProjectId() != null)) {
      return true;
    }
    LOGGER.debug("Push settings are not set in app {}; pubsub wakeup is disabled",
        ae.getAppId());
    return false;
  }
}

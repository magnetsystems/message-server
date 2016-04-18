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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
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
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.TemplateDataModel;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
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
import freemarker.template.TemplateNotFoundException;

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
 * <td>config</td>
 * <td>silentPush (boolean)</td>
 * <td>meta (Map)</td>
 * </td>
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
 * mmx.pubsub.notification.title=
 * mmx.pubsub.notification.body=New message from ${msg.from}
 * mmx.pubsub.notification.sound=default
 * </pre>
 */
public class PubSubWakeupProvider implements WakeupProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(PubSubWakeupProvider.class);

  // Freemarker template loader using MMX Push Config Service.
  public static class FmMmxTemplateLoader implements TemplateLoader {
    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
      // No-op
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
      String[] tokens = FmPushConfig.parseName(name);
      MMXPushConfig config = MMXPushConfigService.getInstance().getPushConfig(
          tokens[0], tokens[1], tokens[2], tokens[3]);
      LOGGER.debug("findTemplateSource(name={}): config={}", name, config);
      return config;
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
        return new StringReader(getTemplate(pushConfig));
      }
      return null;
    }
    private String getTemplate(MMXPushConfig pushConfig) throws IOException  {
      try {
        MMXTemplate template = MMXPushConfigService.getInstance().getTemplate(pushConfig.getTemplateId());
        return template.getTemplate();
      }
      catch (MMXException e) {
        throw new IOException(e);
      }
    }
  }

  // Push Config wrapper for Freemarker.
  public static class FmPushConfig {
    private final static String DELIMITER = "::";
    private final String mName;
    private Properties mProps;
    private PushMessage.Action mPushType;
    private static Configuration sFmCfg;

    static {
      sFmCfg = new Configuration(Configuration.VERSION_2_3_24);
      sFmCfg.setLocalizedLookup(false);   // templateName_{locale} lookup disabled
      sFmCfg.setTemplateLoader(new FmMmxTemplateLoader());
      sFmCfg.setDefaultEncoding("UTF-8");
      sFmCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      sFmCfg.setLogTemplateExceptions(false);
    }

    // Get a Freemarker configuration to override the template loader for testing.
    static Configuration getFmConfig() {
      return sFmCfg;
    }

    static String makeName(String userId, String appId, String channelId,
                            String configName) {
      StringBuilder name = new StringBuilder();
      name.append(userId);
      if (appId != null) {
        name.append(DELIMITER).append(appId);
        if (channelId != null) {
          name.append(DELIMITER).append(channelId);
          if (configName != null) {
            name.append(DELIMITER).append(configName);
          }
        }
      }
      return name.toString();
    }

    static String[] parseName(String name) {
      String[] tokens = name.split(DELIMITER);
      String userId = (tokens.length > 0) ? tokens[0] : null;
      String appId = (tokens.length > 1) ? tokens[1] : null;
      String channelId = (tokens.length > 2) ? tokens[2] : null;
      String configName = (tokens.length > 3) ? tokens[3] : null;
      return new String[] { userId, appId, channelId, configName };
    }

    public FmPushConfig(String userId, String appId, String channelId,
                        String configName) {
      mName = makeName(userId, appId, channelId, configName);
    }

    /**
     * Get the unique name used by Freemarker for this configuration.
     * @return
     */
    public String getName() {
      return mName;
    }

    // Build a context with ${application}, ${channel}, ${msg} and ${config}
    // which can be null.
    private Map<String, Object> buildContext(AppEntity ae, Node node, int count,
                                            MMXPacketExtension mmxExt0) {
      HashMap<String, Object> context = new HashMap<String, Object>();
      try {
        context.put("application", new TemplateDataModel.NameDesc(
            ae.getName(), null, 0));
        // channel name is just its name (not nodeID)
        context.put("channel", new TemplateDataModel.NameDesc(
            node.getName(),  node.getDescription(), count));
        context.put("config", sFmCfg.getTemplateLoader().findTemplateSource(mName));
      } catch (IOException e) {
        LOGGER.error("Caught IOException while building context for template "+mName, e);
      }
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
      context.put("msg", new TemplateDataModel.MsgData(displayName,
          mmxExt0.getPayload().getSentTime(), mmxExt0.getHeaders()));
      return context;
    }

    /**
     * Evaluate the template with context for push configurations.  The push
     * configurations are stored in java.util.Properties name-value format.
     * @param context A non-null dictionary.
     * @return Push configurations, or null if config is not found.
     */
    public String eval(Map<String, Object> context) throws MMXException {
      try {
        // Merge the context and template to form the output properties.
        StringWriter writer = new StringWriter();
        Template template = sFmCfg.getTemplate(mName);
        template.process(context, writer);
        String pushConfigProps = writer.getBuffer().toString();
        Reader reader = new StringReader(pushConfigProps);
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
        return pushConfigProps;
      } catch (TemplateNotFoundException e) {
        // If the template is not found, no notification will be performed.
        return null;
      } catch (IOException e) {
        LOGGER.error("Cannot access template", e);
        throw new MMXException("Cannot access template", StatusCode.INTERNAL_ERROR, e);
      } catch (TemplateException e) {
        LOGGER.error("Template processing error", e);
        throw new MMXException("Template processing error", StatusCode.BAD_REQUEST, e);
      }
    }

    // Use the oldest item to build the push payload; it avoids to have multiple
    // notifications.  Otherwise, template needs the ${msgs} as a collection.
    private String buildPushPayload(AppEntity ae, Node node,
                          List<MMXPacketExtension> mmxItems, MMXTopicId topic) {
      MMXPacketExtension item0 = mmxItems.get(0);
      try {
        Map<String, Object> context = buildContext(ae, node, mmxItems.size(), item0);
        if (eval(context) == null) {
          // If the config is not found, no notification.
          return null;
        }
      } catch (MMXException e) {
        // Ignore all errors.
        return null;
      }

      if (getScope() == WakeupProvider.Scope.no_wakeup) {
        return null;
      }
      String pushPayload;
      MMXid from = MMXid.fromMap((Map<String, String>) item0.getMmxMeta().get(
          MmxHeaders.FROM));
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

    @Override
    public String toString() {
      try {
        StringWriter writer = new StringWriter();
        mProps.store(writer, null);
        return writer.getBuffer().toString();
      } catch (IOException e) {
        return null;
      }
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
    // Topic and channel are interchangeable.
    MMXTopicId topic = TopicHelper.parseNode(node.getNodeID());

    LOGGER.debug("@@@ wakeup(): jid="+jid+", nodeID="+node.getNodeID());

    if (jid.getResource() != null) {
      if (sessionMgr.getSession(jid) == null) {
        // Wake up this disconnected device only.
        AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
        AppEntity ae = appDAO.getAppForAppKey(appId);
        if (!isPushEnabled(ae)) {
          return;
        }
        FmPushConfig pushConf = new FmPushConfig(userId, appId, topic.getId(),
                                                  configName);
        String pushPayload = pushConf.buildPushPayload(ae, node, mmxItems, topic);
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
        FmPushConfig pushConf = new FmPushConfig(userId, appId, topic.getId(),
                                                  configName);
        String pushPayload = pushConf.buildPushPayload(ae, node, mmxItems, topic);
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
                      ", nodeID="+node.getNodeID()+"; registered devices="+deList.size());
      }
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

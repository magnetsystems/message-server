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
package com.magnet.mmx.server.plugin.mmxmgmt.interceptor;

import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXExecutors;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 */
public class MessageDistributorImpl implements MessageDistributor {
  private static final String POOL_NAME = "DistributionExecutor";
  private static final int POOL_SIZE = 10;
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageDistributorImpl.class);

  @Override
  public DistributionResult distribute(Message message, DistributionContext context) {
    DeviceDAO deviceDAO = getDeviceDAO();
    String bareUserId = context.getBareUserId();
    String appKey = context.getAppId();
    String domain = context.getServerHostname();
    List<DeviceEntity> devices = deviceDAO.getDevices(appKey, bareUserId, DeviceStatus.ACTIVE);
    LOGGER.info("distribute : Distributing messages for user={}, messageId={}, numDevices={} ", new Object[]{context.getBareUserId(), context.getMessageId(), devices.size()});

    List<JID> distributed = new ArrayList<JID>();
    List<JIDDevicePair> undistributed = new ArrayList<JIDDevicePair>();
    boolean noDevices = (devices.size() == 0);
    for (DeviceEntity entity : devices) {
      String deviceId = entity.getDeviceId();
      Message cloned = message.createCopy();
      JID deviceSpecificJID = buildToAddress(bareUserId, appKey, domain, deviceId);
      cloned.setTo(deviceSpecificJID);
      MMXPresenceFinder finder = getPresenceFinder();
      if(finder.isOnline(deviceSpecificJID)) {
       LOGGER.trace("distribute : found online deviceSpecificJID={}", deviceSpecificJID);
       route(cloned);
       distributed.add(deviceSpecificJID);
      } else {
        LOGGER.trace("distribute : not found online deviceSpecificJID={}", deviceSpecificJID);
        JIDDevicePairImpl pair = new JIDDevicePairImpl(deviceSpecificJID, entity);
        undistributed.add(pair);
      }
    }
    return new DistributionResultImpl(distributed, undistributed, noDevices);
  }

  private void route(final Message cloned) {
    ExecutorService service = MMXExecutors.getOrCreate(POOL_NAME, POOL_SIZE);
    service.submit(new Runnable() {
      @Override
      public void run() {
        getPacketRouter().route(cloned);
      }
    });
  }

  private JID buildToAddress (String userId, String appKey, String domain, String deviceId ) {
    return new JID(userId + JIDUtil.APP_ID_DELIMITER + appKey, domain, deviceId);
  }

  protected DeviceDAO getDeviceDAO() {
    DeviceDAO dao = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    return dao;
  }

  protected PacketRouter getPacketRouter() {
    PacketRouter router = XMPPServer.getInstance().getPacketRouter();
    return router;
  }

  protected MMXPresenceFinder getPresenceFinder() {
    return new MMXPresenceFinderImpl();
  }

  public static class DistributionResultImpl implements DistributionResult {
    List<JID> distributed;
    List<JIDDevicePair> undistributed;
    boolean noDevices;
    public DistributionResultImpl(List<JID> distributed, List<JIDDevicePair> undistributed, boolean noDevices) {
      this.distributed = distributed;
      this.undistributed = undistributed;
      this.noDevices = noDevices;
    }
    @Override
    public List<JIDDevicePair> getNotDistributed() {
      return undistributed;
    }

    @Override
    public List<JID> getDistributed() {
      return distributed;
    }

    @Override
    public boolean noDevices() {
      return noDevices;
    }
  }

  public static class JIDDevicePairImpl implements JIDDevicePair {
    JID userJID;
    DeviceEntity deviceEntity;

    public JIDDevicePairImpl(JID userJID, DeviceEntity deviceEntity) {
      this.userJID = userJID;
      this.deviceEntity = deviceEntity;
    }

    @Override
    public JID getJID() {
      return userJID;
    }

    @Override
    public DeviceEntity getDevice() {
      return deviceEntity;
    }
  }
}

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

import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.BaseDbTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.TestDataSource;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import org.jivesoftware.openfire.PacketRouter;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmpp.packet.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 */
public class MessageDistributorImplTest {

  @ClassRule
  public static BaseDbTest.DataSourceResource dataSourceRule = new BaseDbTest.DataSourceResource(TestDataSource.APP_DATA_1, TestDataSource.DEVICE_DATA_1);

  private static ConnectionProvider connectionProvider = new BasicDataSourceConnectionProvider(dataSourceRule.getDataSource());

  @Test
  public void testDistribute1() throws InterruptedException {
    MessageDistributor distributor = new StubMessageDistributorImpl();
    Message message = new Message();
    message.setBody("body");
    message.setSubject("subject");
    String toNode1 = "magnet.way";
    String fromNode = "thomas";
    String appKey = "AAABSNIBKOstQST7";
    String host = "mmx.magnet.com";
    String resource = "device1";
    message.setTo(new JID(toNode1 + JIDUtil.APP_ID_DELIMITER + appKey, host, resource));
    message.setFrom(new JID(fromNode + JIDUtil.APP_ID_DELIMITER + appKey, host, resource));

    MessageDistributor.DistributionContext context = new DistributionContextImpl(toNode1, appKey, host, resource);

    PacketRouter router = new CountingPacketRouter();
    MMXPresenceFinder finder = new AlwaysOnlinePresenceFinder();
    ((StubMessageDistributorImpl) distributor).setPacketRouter(router);
    ((StubMessageDistributorImpl) distributor).setPresenceFinder(finder);

    MessageDistributor.DistributionResult result = distributor.distribute(message, context);

    Thread.sleep(5000);

    CountingPacketRouter countingPacketRouter = (CountingPacketRouter) router;
    assertEquals("Non matching message packet count", 4, countingPacketRouter.messageCount.get());
    assertFalse("User has devices but we are reporting he doesn't", result.noDevices());
  }

  @Test
  public void testDistribute2ToUserWithNoDevices() throws InterruptedException {
    MessageDistributor distributor = new StubMessageDistributorImpl();
    Message message = new Message();
    message.setBody("body");
    message.setSubject("subject");
    String toNode1 = "bud.worker";
    String fromNode = "thomas";
    String appKey = "AAABSNIBKOstQST7";
    String host = "mmx.magnet.com";
    String resource = "device1";
    message.setTo(new JID(toNode1 + JIDUtil.APP_ID_DELIMITER + appKey, host, null));
    message.setFrom(new JID(fromNode + JIDUtil.APP_ID_DELIMITER + appKey, host, resource));

    MessageDistributor.DistributionContext context = new DistributionContextImpl(toNode1, appKey, host, resource);

    PacketRouter router = new CountingPacketRouter();
    MMXPresenceFinder finder = new AlwaysOnlinePresenceFinder();
    ((StubMessageDistributorImpl) distributor).setPacketRouter(router);
    ((StubMessageDistributorImpl) distributor).setPresenceFinder(finder);

    MessageDistributor.DistributionResult result = distributor.distribute(message, context);

    Thread.sleep(5000);

    CountingPacketRouter countingPacketRouter = (CountingPacketRouter) router;
    assertEquals("Non matching message packet count", 0, countingPacketRouter.messageCount.get());
    assertTrue("User has no devices but we are reporting he has", result.noDevices());
  }


  /**
   * Stub implementation of the MessageDistributor for unit testing
   */
  private static class StubMessageDistributorImpl extends MessageDistributorImpl {
    private PacketRouter router = null;
    private MMXPresenceFinder finder = null;

    @Override
    protected PacketRouter getPacketRouter() {
      return router;
    }

    @Override
    protected MMXPresenceFinder getPresenceFinder() { return finder; }

    @Override
    protected DeviceDAO getDeviceDAO() {
      return new DeviceDAOImpl(connectionProvider);
    }

    public void setPacketRouter(PacketRouter router) {
      this.router = router;
    }

    public void setPresenceFinder(MMXPresenceFinder finder) {this.finder = finder; }
  }

  /**
   * Packet counter that just counts the packets and doesn't do any real routing.
   * This is for unit testing.
   */
  private static class CountingPacketRouter implements PacketRouter {
    AtomicInteger iqCount = new AtomicInteger(0);
    AtomicInteger packetCount =  new AtomicInteger(0);
    AtomicInteger messageCount =  new AtomicInteger(0);
    AtomicInteger presenceCount = new AtomicInteger(0);;

    @Override
    public void route(Packet packet) {
      packetCount.incrementAndGet();
    }

    @Override
    public void route(IQ packet) {
      iqCount.incrementAndGet();
    }

    @Override
    public void route(Message packet) {
      messageCount.incrementAndGet();
    }

    @Override
    public void route(Presence packet) {
      presenceCount.incrementAndGet();
    }
  }

  private static class AlwaysOnlinePresenceFinder implements MMXPresenceFinder {
    @Override
    public boolean isOnline(JID user) {
      return true;
    }
  }
}

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
package com.magnet.mmx.server.plugin.mmxmgmt.context;

import com.magnet.ext.geo.GeoHashEncoderDecoder;
import com.magnet.ext.geo.GeoPoint;
import com.magnet.ext.geo.GeoPointDefaultImpl;
import com.magnet.ext.geo.GeohashEncoderDecoderFactory;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.GeoLoc;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXExecutors;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TopicHelper;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.component.ComponentEventListener;
import org.jivesoftware.openfire.component.InternalComponentManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class GeoEventDispatcher implements IContextDispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoEventDispatcher.class);
  private static final String NAME = GeoEventDispatcher.class.getName();

  static {
    // register to the factory
    ContextDispatcherFactory.getInstance().registerClass(NAME, GeoEventDispatcher.class);
  }

  private static final int POOL_SIZE = 5;
  private static final String POOL_NAME = "GeoContextDispatch";
  public static final String APP_ID = "appId";
  public static final String USER_ID = "userId";
  public static final String ACCURACY = "accuracy";
  public static final String LONG = "long";
  public static final String LAT = "lat";
  public static final String ALTITUDE = "altitude";
  public static final String DEVICE_ID = "deviceId";
  public static final String GEOHASH = "geohash";
  public static final String PROTOCOL_XMPP = "PROTOCOL_XMPP";


  // <iq id='5VJ3b-87' to='pubsub.login1s-macbook-pro-3.local' from='test%kv7i6cj1sd0@login1s-macbook-pro-3.local/5reb0zs53pkf1lx770vnlz60mokzknhtzq4g' type='set'><pubsub xmlns="http://jabber.org/protocol/pubsub"><publish node='/kv7i6cj1sd0/test/com.magnet.geoloc'><item id='lw3wY4Z8TaeyBWezAqNiCA-3'><mmx xmlns='com.magnet:msg:payload'><payload ctype='application/json' mtype='geoloc' chunk='0/50/50' stamp='2015-02-25T04:49:56.477Z'>{&quot;accuracy&quot;:2397,&quot;lat&quot;:37.034985,&quot;lng&quot;:-122.02842}</payload></mmx></item></publish></pubsub></iq>
  // TODO create a thread pool to take care of the dispatch
  private final ExecutorService executors = MMXExecutors.getOrCreate(POOL_NAME, POOL_SIZE);
  private final BlockingQueue<IQ> eventQueue = new LinkedBlockingDeque<IQ>();

  private static final ThreadLocal<GeoHashEncoderDecoder> localGeoEndocoder = new ThreadLocal<GeoHashEncoderDecoder>();

  private final ConcurrentLinkedQueue<JID> componentJids = new ConcurrentLinkedQueue<JID>();
  private final AtomicInteger hits = new AtomicInteger();

  public GeoEventDispatcher() {

    if (InternalComponentManager.getInstance() != null) {
      InternalComponentManager.getInstance().addListener(new ComponentEventListener() {
        @Override
        public void componentRegistered(JID jid) {
          registerExternalComponent(jid);
        }

        @Override
        public void componentUnregistered(JID jid) {
          unregisterExternalComponent(jid);
        }

        @Override
        public void componentInfoReceived(IQ iq) {
        }
      });
    }
    // start the consumer threads
    for (int i = 0; i < POOL_SIZE; i++) {
      executors.execute(new DispatchHandler());
    }
  }

  public void shutdown() {
    executors.shutdown();
    try {
      executors.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.error("unexpected GeoEventDispatcher thread interrupted waiting for shutdown", e);
    }
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getSupportedTypeName() {
    return Constants.MMX_MTYPE_GEOLOC;
  }

  @Override
  public String getSupportedProtocol() {
    return PROTOCOL_XMPP;
  }

  public void registerExternalComponent(final JID jid) {
    // add it to the map
    if (isRegisteredForGeo(jid.toString())) {
      synchronized (componentJids) {
        componentJids.add(jid);
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("external component added: " + jid.toString());
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("external component ignored: " +  jid.toString());
      }
    }
  }

  public void unregisterExternalComponent(final JID jid) {
    synchronized (componentJids) {
      componentJids.remove(jid);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("external component removed: " + jid.toString());
    }
  }

  public void dispatchToExternalService(final IQ iq) {
    try {
      eventQueue.put(iq);
    } catch (InterruptedException e) {
      LOGGER.error("unexpected thread interrupted while putting geo event into queue", e);
    }
  }

  private class DispatchHandler implements Runnable {
    @Override
    public void run() {
      // create geohash encoder for the thread.
      localGeoEndocoder.set(GeohashEncoderDecoderFactory.createEncoderDecoder());

      // wait for event to dispatch
      try {
        while (true) {
          final IQ iq = eventQueue.take();
          routeGeoEvent(iq);
          Thread.sleep(50);   // sleep sometimes
        }
      } catch (InterruptedException e) {
        LOGGER.error("unexpected thread interrupted while taking geo event from queue", e);
      }
    }
  }

  public void routeGeoEvent(final IQ geoIQ) {
    if (componentJids.isEmpty()) {
      // nothing to do
      LOGGER.debug("no external component registered for geo event");
      return;
    }
    String geoService = selectGeoService();
    if (geoService != null && geoService.length() > 0) {
      Message geoMessage = buildGeoMessageFromPubSubIQ(geoIQ);
      if (geoMessage != null) {
        geoMessage.setTo(geoService);
        // TODO create a new message ID?
        geoMessage.setID(geoIQ.getID());
        LOGGER.debug("Sending geo event to external component: " + geoService);
        // start attaching the pieces
        PacketRouter router = XMPPServer.getInstance().getPacketRouter();
        router.route(geoMessage);
      }
    }
  }

  static Message buildGeoMessageFromPubSubIQ(final IQ geoIQ) {
    Message geoMessage = null;
    if (IQ.Type.set == geoIQ.getType() && geoIQ.getTo().toString().startsWith("pubsub")) {
      // find 'geoloc'
      Element element = geoIQ.getChildElement();
      Element action = element.element("publish");
      if (action != null) {
        String nodeID = action.attributeValue("node");
        if (nodeID != null && nodeID.endsWith(TopicHelper.TOPIC_GEOLOC)) {
          // Entity publishes an item
          geoMessage = new Message();
          geoMessage.setType(Message.Type.chat);
          Map<String, String> geoValues = new HashMap<String, String>();
          JID from = geoIQ.getFrom();
          String appId = JIDUtil.getAppId(from);
          geoValues.put(APP_ID, appId);

          String userId = JIDUtil.getUserId(from);
          geoValues.put(USER_ID, userId);

          String deviceId = JIDUtil.getResource(from.toString());
          geoValues.put(DEVICE_ID, deviceId);

          Iterator<Element> items = action.elementIterator("item");
          if (items != null) {
            while (items.hasNext()) {
              Element item = (Element) items.next();
              Element mmx = item.element(Constants.MMX_ELEMENT);
              if (mmx == null) continue;
              Element payload = mmx.element(Constants.MMX_PAYLOAD);
              if (payload == null) return null;  // not a valid MMX payload, ignore it
              Attribute timestamp = payload.attribute(Constants.MMX_ATTR_STAMP);
              if (timestamp != null) {
                geoValues.put(Constants.MMX_ATTR_STAMP, timestamp.getValue());
              }
              String geoData = payload.getTextTrim();
              GeoLoc geoLoc = GsonData.getGson().fromJson(geoData, GeoLoc.class);
              if (geoLoc.getAccuracy() != null) {
                geoValues.put(ACCURACY, geoLoc.getAccuracy().toString());
              }
              if (geoLoc.getLng() != null && geoLoc.getLat() != null) {
                geoValues.put(LONG, geoLoc.getLng().toString());
                geoValues.put(LAT, geoLoc.getLat().toString());
                // calculate geohash
                GeoPoint point = new GeoPointDefaultImpl(geoLoc.getLat(), geoLoc.getLng());
                geoValues.put(GEOHASH, localGeoEndocoder.get().encodePoint(point));
              }
              if (geoLoc.getAlt() != null) {
                geoValues.put(ALTITUDE, Integer.toString((int)geoLoc.getAlt().floatValue()));
              }
              Element geoElement = geoMessage.addChildElement(Constants.MMX_ELEMENT, Constants.MMX_NS_CONTEXT);
              geoElement.addAttribute(Constants.MMX_ATTR_MTYPE, Constants.MMX_MTYPE_GEOLOC);
              JSONObject geoJson = new JSONObject(geoValues);
              geoElement.setText(geoJson.toString());
              geoMessage.setBody(geoJson.toString());

              return geoMessage;

            }
          }
        }
      }
    }
    return null;
  }

  private String selectGeoService() {
    // dequeue and add it to the end of the queue for round-robin every 30 hits or so
    synchronized (componentJids) {
      JID target = componentJids.peek();
        if (componentJids.size() > 1) {
        if (hits.incrementAndGet() % 30 == 0) {
          target = componentJids.poll();
          componentJids.offer(target);
          hits.set(0);
        }
      }
      return target.toString();
    }
  }

  public static boolean isRegisteredForGeo(String strJid) {
    //TODO look it up in a registry map
    String geoService = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.EXT_SERVICE_EVENT_GEO);

    // find all the connected component that starts with this name (actual name = <service-name>-instanceId)
    if (strJid != null &&
        geoService != null &&
        geoService.length() > 0 && strJid.toString().startsWith(geoService)) {
      return true;
    } else {
      return false;
    }
  }
}

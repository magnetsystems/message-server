/*   Copyright (c) 2016 Magnet Systems, Inc.
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

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.PubSubWakeupProvider.FmPushConfig;
import com.magnet.mmx.util.TimeUtil;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for push config template.
 */
public class PushConfigTemplateTest {

  private static final String APP_ID = "appid-1234-5678-9abcdef";
  private static final String APP_NAME = "myApp";
  private static final String PUBLISHER_USER_ID = "0123456789abcdef";
  private static final String SUBSCRIBER_USER_ID = "999999999999999";
  private static final String CHANNEL_CREATOR_USER_ID = "feedfacefeed";
  private static final String CHANNEL_ID = "pubchannel";
  private static final String CHANNEL_NAME = "PubChannel";
  private static final String CHANNEL_DESC = "Mock Public Channel";
  private static final String PUBLISHER_NAME = "John Doe";
  private static final String SENT_TIME = "2016-04-11T14:51:28.111Z";
  private static final String HELLO_MESG = "Hello World!  It is a long, long, long text that we don't expect to see entirely";
  private static final String GARP_MESG = "Hello Garp!  It is a long, long, long text that we don't expect to see entirely";


  public static final String CONFIG1 = "config1";
  public static final String CONFIG2 = "config2";
  public static final String CONFIG3 = "config3";
  public static final String CONFIG4 = "config4";
  public static final String CONFIG5 = "config5";
  public static final String TEMPLATE1 =
      "<#setting date_format=\"yyyy-MM-dd\">\n"+
      "<#setting time_zone=\"UTC\">\n"+
      "mmx.pubsub.notification.type=push\n"+
      "mmx.pubsub.notification.title=${application.name} - ${channel.name} - ${msg.date?date}\n"+
      "mmx.pubsub.notification.body=${msg.from}: ${msg.content.message[0..*11]}...\n"+
      "mmx.pubsub.notification.sound=default\n";
  public static final String TEMPLATE2 =
      "<#setting datetime_format=\"yyyy-MM-dd HH:mm:ss zzz\">\n"+
      "<#setting time_zone=\"UTC\">\n"+
      "mmx.pubsub.notification.type=wakeup\n"+
      "mmx.pubsub.notification.title=${application.name} - ${channel.name} - ${msg.date?datetime?string(\"yyyy-MM-dd HH:mm:ss zzz\")}\n"+
      "mmx.pubsub.notification.body=${msg.from}: ${msg.content.message[0..*11]}...\n"+
      "mmx.pubsub.notification.sound=default\n";
  public static final String TEMPLATE3 =
      "<#setting time_zone=\"UTC\">\n"+
      "mmx.pubsub.notification.type=\n"+
      "mmx.pubsub.notification.title=${channel.name}/${channel.desc}/${msg.date?time}\n"+
      "mmx.pubsub.notification.body=${msg.from}: ${msg.content.message}\n"+
      "mmx.pubsub.notification.sound=\n";
  public static final String TEMPLATE4 =
      "<#setting datetime_format=\"yyyy-MM-dd HH:mm:ss zzz\">\n"+
      "<#setting time_zone=\"UTC\">\n"+
      "<#if config.silentPush >\n"+
      "mmx.pubsub.notification.type=wakeup\n"+
      "<#else>\n"+
      "mmx.pubsub.notification.type=push\n"+
      "</#if>\n"+
      "mmx.pubsub.notification.title=${application.name} - ${channel.name} - ${msg.date?datetime?string(\"yyyy-MM-dd HH:mm:ss zzz\")}\n"+
      "mmx.pubsub.notification.body=${msg.from}: ${msg.content.message[0..*11]}...\n"+
      "mmx.pubsub.notification.sound=default\n";
  public static final String TEMPLATE5 =
      "<#if config.silentPush >\n"+
      "mmx.pubsub.notification.type=wakeup\n"+
      "<#else>\n"+
      "mmx.pubsub.notification.type=push\n"+
      "</#if>\n"+
      "mmx.pubsub.notification.title=${noSuchVar}\n"+
      "mmx.pubsub.notification.body=${Msg.content.message}\n"+
      "mmx.pubsub.notification.sound=default\n";

  // Must be public class for Freemarker to access.
  public static class MockPushConfig {
    private final Map<String, String> mMeta = new HashMap<String, String>();
    private final String mTemplate;
    private final Boolean mSilentPush;

    /**
     * Constructor with template content.
     * @param template The template content.
     */
    public MockPushConfig(String template, Boolean silentPush) {
      mTemplate = template;
      mSilentPush = silentPush;
    }

    /**
     * Get the template content.
     * @return
     */
    public String getTemplate() {
      return mTemplate;
    }

    public Map<String, String> getMeta() {
      return mMeta;
    }

    public boolean isSilentPush() {
      return (mSilentPush == null) ? false : mSilentPush;
    }
  }

  static class MockTemplateLoader implements TemplateLoader {
    private final Map<String, MockPushConfig> mMap;

    public MockTemplateLoader(Map<String, MockPushConfig> maps) {
      mMap = maps;
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
      // No-op.
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
      return mMap.get(name);
    }

    @Override
    public long getLastModified(Object templateSource) {
      return 0;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
      if (templateSource instanceof MockPushConfig) {
        MockPushConfig pushConfig = (MockPushConfig) templateSource;
        System.out.println("MockTemplateLoader: getReader() template="+pushConfig.getTemplate());
        return new StringReader(pushConfig.getTemplate());
      }
      throw new IOException("templateSource is "+templateSource.getClass().getSimpleName()+"; expect MockPushConfig");
    }
  }

  @BeforeClass
  public static void setUp() throws Exception {
    // Create 3 templates for testing
    Map<String, MockPushConfig> maps = new HashMap<String, MockPushConfig>() {{
      put(FmPushConfig.makeName(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG1), new MockPushConfig(TEMPLATE1, Boolean.FALSE));
      put(FmPushConfig.makeName(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG2), new MockPushConfig(TEMPLATE2, Boolean.TRUE));
      put(FmPushConfig.makeName(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG3), new MockPushConfig(TEMPLATE3, null));
      put(FmPushConfig.makeName(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG4), new MockPushConfig(TEMPLATE4, Boolean.TRUE));
      put(FmPushConfig.makeName(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG5), new MockPushConfig(TEMPLATE5, Boolean.TRUE));
    }};
    MockTemplateLoader loader = new MockTemplateLoader(maps);

    // Use a mock template loader for testing.
    Configuration fmConfig = FmPushConfig.getFmConfig();
    fmConfig.setTemplateLoader(loader);
  }

  @AfterClass
  public static void teardown() {
  }

  @Test
  public void test0Template() throws Exception {
    int count = 1;
    Map<String, String> meta = new HashMap<String, String>();
    meta.put("message", HELLO_MESG);

    // Too hard to create a mock LeafNode, so we just simulate the context.
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("application", new PubSubWakeupProvider.NameDesc(APP_NAME, null, 0));
    context.put("channel", new PubSubWakeupProvider.NameDesc(CHANNEL_NAME, CHANNEL_DESC, count));
    context.put("msg", new PubSubWakeupProvider.MsgData(PUBLISHER_NAME, TimeUtil.toDate(SENT_TIME), meta));

    // Use config1 template
    FmPushConfig fmPushConfig = new FmPushConfig(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG1);
    assertNotNull(FmPushConfig.getFmConfig().getTemplateLoader().findTemplateSource(fmPushConfig.getName()));
    context.put("config", FmPushConfig.getFmConfig().getTemplateLoader().findTemplateSource(fmPushConfig.getName()));
    String pushConfig = fmPushConfig.eval(context);
    System.out.println(pushConfig);
    assertNotNull(pushConfig);
    assertFalse(pushConfig.isEmpty());
    assertEquals(
        "mmx.pubsub.notification.type=push\n"+
        "mmx.pubsub.notification.title=myApp - PubChannel - 2016-04-11\n"+
        "mmx.pubsub.notification.body=John Doe: Hello World...\n"+
        "mmx.pubsub.notification.sound=default\n", pushConfig);

    // Use config2 template
    fmPushConfig = new FmPushConfig(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG2);
    assertNotNull(FmPushConfig.getFmConfig().getTemplateLoader().findTemplateSource(fmPushConfig.getName()));
    context.put("config", FmPushConfig.getFmConfig().getTemplateLoader().findTemplateSource(fmPushConfig.getName()));
    pushConfig = fmPushConfig.eval(context);
    System.out.println(pushConfig);
    assertNotNull(pushConfig);
    assertFalse(pushConfig.isEmpty());
    assertEquals(
        "mmx.pubsub.notification.type=wakeup\n"+
        "mmx.pubsub.notification.title=myApp - PubChannel - 2016-04-11 14:51:28 UTC\n"+
        "mmx.pubsub.notification.body=John Doe: Hello World...\n"+
        "mmx.pubsub.notification.sound=default\n", pushConfig);

    // Use config3 template
    fmPushConfig = new FmPushConfig(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG3);
    assertNotNull(FmPushConfig.getFmConfig().getTemplateLoader().findTemplateSource(fmPushConfig.getName()));
    context.put("config", FmPushConfig.getFmConfig().getTemplateLoader().findTemplateSource(fmPushConfig.getName()));
    pushConfig = fmPushConfig.eval(context);
    System.out.println(pushConfig);
    assertNotNull(pushConfig);
    assertFalse(pushConfig.isEmpty());
    assertEquals(
        "mmx.pubsub.notification.type=\n"+
        "mmx.pubsub.notification.title=PubChannel/Mock Public Channel/2:51:28 PM\n"+
        "mmx.pubsub.notification.body=John Doe: "+HELLO_MESG+"\n"+
        "mmx.pubsub.notification.sound=\n", pushConfig);
  }

  @Test
  public void test1TemplateWithConfig() throws Exception {
    int count = 1;
    Map<String, String> meta = new HashMap<String, String>();
    meta.put("message", GARP_MESG);

    FmPushConfig fmPushConfig = new FmPushConfig(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG4);

    // Too hard to create a mock LeafNode, so we just simulate the context.
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("application", new PubSubWakeupProvider.NameDesc(APP_NAME, null, 0));
    context.put("channel", new PubSubWakeupProvider.NameDesc(CHANNEL_NAME, CHANNEL_DESC, count));
    context.put("msg", new PubSubWakeupProvider.MsgData(PUBLISHER_NAME, TimeUtil.toDate(SENT_TIME), meta));
    context.put("config", FmPushConfig.getFmConfig().getTemplateLoader().findTemplateSource(fmPushConfig.getName()));

    MockPushConfig mpc = (MockPushConfig) context.get("config");
    assertNotNull(mpc);
    assertTrue(mpc.isSilentPush());

    // Use config4 template
    String pushConfig = fmPushConfig.eval(context);
    System.out.println(pushConfig);
    assertNotNull(pushConfig);
    assertFalse(pushConfig.isEmpty());
    assertEquals(
        "mmx.pubsub.notification.type=wakeup\n"+
        "mmx.pubsub.notification.title=myApp - PubChannel - 2016-04-11 14:51:28 UTC\n"+
        "mmx.pubsub.notification.body=John Doe: Hello Garp!...\n"+
        "mmx.pubsub.notification.sound=default\n", pushConfig);
  }
  @Test
  public void test2NoTemplate() throws Exception {
    int count = 1;
    Map<String, String> meta = new HashMap<String, String>();
    meta.put("message", GARP_MESG);

    FmPushConfig fmPushConfig = new FmPushConfig(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, "NoConfig");

    // Too hard to create a mock LeafNode, so we just simulate the context.
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("application", new PubSubWakeupProvider.NameDesc(APP_NAME, null, 0));
    context.put("channel", new PubSubWakeupProvider.NameDesc(CHANNEL_NAME, CHANNEL_DESC, count));
    context.put("msg", new PubSubWakeupProvider.MsgData(PUBLISHER_NAME, TimeUtil.toDate(SENT_TIME), meta));
    context.put("config", FmPushConfig.getFmConfig().getTemplateLoader().findTemplateSource(fmPushConfig.getName()));

    MockPushConfig mpc = (MockPushConfig) context.get("config");
    assertNull(mpc);

    // Test no template
    String pushConfig = fmPushConfig.eval(context);
    assertNull(pushConfig);
  }

  @Test
  public void test3ErrorTemplate() throws Exception {
    int count = 1;
    Map<String, String> meta = new HashMap<String, String>();
    meta.put("message", GARP_MESG);

    FmPushConfig fmPushConfig = new FmPushConfig(SUBSCRIBER_USER_ID, APP_ID, CHANNEL_ID, CONFIG5);

    // Too hard to create a mock LeafNode, so we just simulate the context.
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("application", new PubSubWakeupProvider.NameDesc(APP_NAME, null, 0));
    context.put("channel", new PubSubWakeupProvider.NameDesc(CHANNEL_NAME, CHANNEL_DESC, count));
    context.put("msg", new PubSubWakeupProvider.MsgData(PUBLISHER_NAME, TimeUtil.toDate(SENT_TIME), meta));
    context.put("config", FmPushConfig.getFmConfig().getTemplateLoader().findTemplateSource(fmPushConfig.getName()));

    MockPushConfig mpc = (MockPushConfig) context.get("config");
    assertNotNull(mpc);
    assertTrue(mpc.isSilentPush());

    // Use config5 template
    try {
      String pushConfig = fmPushConfig.eval(context);
      fail(pushConfig);
    } catch (MMXException e) {
      System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
      System.out.println(e.getCause().getMessage());
      System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
      assertTrue(e.getCause() instanceof TemplateException);
    }
  }
}
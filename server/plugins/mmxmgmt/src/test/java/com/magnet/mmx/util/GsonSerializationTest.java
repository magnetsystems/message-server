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
package com.magnet.mmx.util;

import com.magnet.mmx.server.plugin.mmxmgmt.servlet.AppRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertNotNull;

/**
 */
public class GsonSerializationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(GsonSerializationTest.class);

  @Test
  public void testAppRequestSerialization() {

    AppRequest request = new AppRequest();
    request.setName("Cool App");
    request.setOwnerId("superuser");
    request.setGoogleApiKey("gapikey");
    request.setGoogleProjectId(Integer.toString(1837483));
    request.setOwnerEmail("super@magnum.com");
    request.setGuestSecret("fet17171^$%");
    request.setApnsCertPassword("mag!cOp@n");

    String json = GsonData.getGson().toJson(request);

    assertNotNull("json is null", json);


  }

  @Test
  public void testCommaSeparatedValue() {
    MMXConfiguration.getConfiguration().setValue(MMXConfigKeys.ALERT_EMAIL_BCC_LIST, "a,b,c");

    SerializableConfig config = new SerializableConfig(MMXConfiguration.getConfiguration());

    GsonData.getGson().toJson(config, System.out);

  }

  /**
   * Config that can be serialized to JSON and de-serialized from JSON.
   */
  private static class SerializableConfig  {
    Map<String, String> configs = new TreeMap<String, String>();

    public SerializableConfig() {};

    public SerializableConfig(MMXConfiguration configuration) {
      Iterator<String> keys = configuration.getKeys();
      while (keys.hasNext()) {
        String key = keys.next();
        String value;
        if(key.equals(MMXConfigKeys.ALERT_EMAIL_BCC_LIST)) {
          List<String> list = configuration.getList(key);
          value = StringUtils.join(list, ",");
        } else {
          value = configuration.getString(key);
        }
        configs.put(key, value);
      }
    }
  }



}

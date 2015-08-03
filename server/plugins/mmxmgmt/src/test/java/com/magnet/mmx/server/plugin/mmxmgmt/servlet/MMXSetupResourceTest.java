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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AuthUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXSetupStatus;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;

//import com.sun.jersey.api.client.ClientResponse;
//import com.sun.jersey.api.client.WebResource;

/**
*/
@RunWith(JMockit.class)
public class MMXSetupResourceTest extends BaseJAXRSTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXSetupResourceTest.class);
  private static String baseUri = "http://localhost:8087/mmxadmin/rest/v1/status/setup";

  public MMXSetupResourceTest() {
    super(baseUri);
  }

  @Before
  public void setup() {
    setupMocks();
  }

  public void setupMocks() {
    new MockUp<AuthUtil>() {
      @Mock
      public boolean isAuthorized(HttpServletRequest headers) throws IOException {
        return true;
      }
    };
  }

  @Test
  public void testStatus() {
    WebTarget service = getClient().target(getBaseURI());
    String jsonString = service.request().get(String.class);
    LOGGER.trace("testStatus : jsonString : {}", jsonString);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    MMXSetupStatus status = gson.fromJson(jsonString, MMXSetupStatus.class);
    LOGGER.trace("testAllApps : receivedJson=\n{}", gson.toJson(status));
  }
}

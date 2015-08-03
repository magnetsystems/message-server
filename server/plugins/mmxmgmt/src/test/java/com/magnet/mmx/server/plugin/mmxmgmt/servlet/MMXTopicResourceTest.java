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
import com.magnet.mmx.server.api.v1.protocol.TopicSubscription;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXPubSubItem;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXPubSubPayload;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicNode;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AuthUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TopicHelper;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
*/
@RunWith(JMockit.class)
public class MMXTopicResourceTest extends BaseJAXRSTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXTopicResourceTest.class);
  //http://localhost:5220/mmxmgmt/api/v1/send_message
  private static final String baseUri = "http://localhost:8086/mmxmgmt/api/v1/topics";
  private static BasicDataSource ds;
  private static String appId = "7wmi73wxin9";
  private static String apiKey = "4111f18a-9fcc-4e84-8cb9-aad6ea7bf024";

  private static void setupMocks() {
    new MockUp<TopicResource>() {

      @Mock
      protected List<TopicSubscription> getTopicSubscriptions (String appId, String topicName) {
        LOGGER.trace("MOCKED getTopicSubscriptions : appid={}, topicName={}", appId, topicName);
        String topicId = TopicHelper.makeTopic(appId, null, topicName);
        List<TopicSubscription> list = new ArrayList<TopicSubscription>(10);
        for (int i=0; i < 100; i++) {
          StubTopicSubscription subscription = new StubTopicSubscription();
          subscription.setTopic(topicName);
          subscription.setUsername(RandomStringUtils.randomAlphanumeric(3));
          subscription.setSubscriptionId(RandomStringUtils.randomAlphanumeric(10));
          if (i%3 ==0 ) {
            subscription.setDeviceId(RandomStringUtils.randomNumeric(10));

          }
          list.add(subscription);
        }
        return list;
      }

    };
    //for allowing auth
    new MockUp<AuthUtil>() {
      @Mock
      public boolean isAuthorized(HttpServletRequest headers) throws IOException {
        return true;
      }
    };
    /**
     * For using the db unit datasource
     */
    new MockUp<TopicResource>() {
      @Mock
      public ConnectionProvider getConnectionProvider() {
        return new BasicDataSourceConnectionProvider(ds);
      }
    };
  }

  public MMXTopicResourceTest() {
    super(baseUri);
  }

  @BeforeClass
  public static void setup() throws Exception{
    ds = UnitTestDSProvider.getDataSource();

    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/app-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/pubsub-node-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    setupMocks();
  }

  @AfterClass
  public static void cleanup() throws Exception {
    DBTestUtil.cleanTables(new String[] {"mmxTag"}, new BasicDataSourceConnectionProvider(ds));
    ds.close();
  }


  @Test
  public void testSearchTopics() {

    String topicName = "sport";

    WebTarget target = getClient().target(baseUri)
                        .queryParam(TopicResource.TOPIC_NAME, topicName);

    Invocation.Builder invocationBuilder =
        target.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appId);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, apiKey);

    Response response = invocationBuilder.get();
    int statusCode = response.getStatus();
    assertEquals("Non matching response code",Response.Status.OK.getStatusCode(), statusCode);

    String json = response.readEntity(String.class);
    assertNotNull("Response is null", json);
    Gson gson = GsonData.getGson();
    SearchResult<TopicNode> result = gson.fromJson(json, SearchResult.class);
    assertNotNull(result);
    response.close();
  }



  @Test
  public void testListTopicSubscriptionsForATopic() {

    String topicName = "sport";

    WebTarget target = getClient().target(baseUri + "/" + topicName + "/" + "subscriptions")
        .queryParam(TopicResource.APP_ID_KEY, appId)
        .queryParam(TopicResource.TOPIC_NAME, topicName);

    Invocation.Builder invocationBuilder =
        target.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appId);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, apiKey);

    Response response = invocationBuilder.get();
    int statusCode = response.getStatus();
    assertEquals("Non matching response code",Response.Status.OK.getStatusCode(), statusCode);

    String json = response.readEntity(String.class);
    assertNotNull("Response is null", json);
    Gson gson = GsonData.getGson();
    List<TopicSubscription> result = gson.fromJson(json, List.class);
    assertNotNull(result);
    response.close();
  }


  private static  MMXPubSubItem getRandomPubSubItem() {
    MMXPubSubPayload payload = new MMXPubSubPayload("text_" + RandomStringUtils.randomAlphabetic(2),new Date().toString(), "Hello_World_" + RandomStringUtils.randomAlphabetic(2));

    Map<String, String> map = new HashMap<String, String>();

    map.put("key1", "value1");
    map.put("key2", "value2");

    MMXPubSubItem item = new MMXPubSubItem();


    item.setItemId(RandomStringUtils.randomAlphanumeric(10));
    item.setMeta(map);
    item.setAppId(RandomStringUtils.randomAlphanumeric(8));
    item.setTopicName(RandomStringUtils.randomAlphanumeric(10));
    item.setPayload(payload);
    return item;

  }


  private static class StubTopicSubscription extends TopicSubscription {
    private String username;            // a user ID for user topic, or null for global topic
    private String topic;             // the topic name
    private String subscriptionId;    // the subscription ID
    private String deviceId;          // device identifier associated with this subscription.

    public void setTopic(String topic) {
      this.topic = topic;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public void setDeviceId(String deviceId) {
      this.deviceId = deviceId;
    }

    public void setSubscriptionId(String subscriptionId) {
      this.subscriptionId = subscriptionId;
    }

    public String getUsername() {
      return username;
    }

    public String getTopicName() {
      return topic;
    }

    public String getSubscriptionId() {
      return subscriptionId;
    }

    public String getDeviceId() {
      return deviceId;
    }
  }
}

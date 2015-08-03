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
package com.magnet.mmx.protocol;

import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageEntity.MessageState;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * For testing serialization of MsgsState property
 */
public class MsgsStateTest {


  @Test
  public void test1SerializeRequest() {
    MsgsState.Request request = new MsgsState.Request();
    String[] msgIds = {"m1", "m2", "m3"};
    for (String m : msgIds) {
      request.add(m);
    }

    String json = request.toJson();
    String expected = "[\"m1\",\"m2\",\"m3\"]";
    assertEquals("No matching json", expected, json);

  }

  @Test
  public void test2DeSerializeRequest() {
    String json = "[\"m1\",\"m2\",\"m3\"]";
    MsgsState.Request request = MsgsState.Request.fromJson(json);
    String[] msgIds = {"m1", "m2", "m3"};
    for (String m : msgIds) {
      assertTrue("Request doesn't contain:" + m, request.contains(m));
    }
  }


  @Test
  public void test3SerializeResponse() {
    MsgsState.Response response = new MsgsState.Response();
    String[] msgIds = {"m1", "m2", "m3"};
    String[] userIds = {"user1", "user2", "user3"};
    String[] states = {MessageEntity.MessageState.DELIVERED.name(),
        MessageEntity.MessageState.WAKEUP_SENT.name(),
        MessageEntity.MessageState.DELIVERY_ATTEMPTED.name()};
    int index = 0;
    for (String m : msgIds) {
      MsgsState.MessageStatusList list = new MsgsState.MessageStatusList();
      for (String userId : userIds) {
        list.add(new MsgsState.MessageStatus()
              .setRecipient(userId)
              .setState(Constants.MessageState.valueOf(states[index])));
      }
      response.put(m, list);
      ++index;
    }
    String json = response.toJson();
    assertNotNull(json);
  }

}

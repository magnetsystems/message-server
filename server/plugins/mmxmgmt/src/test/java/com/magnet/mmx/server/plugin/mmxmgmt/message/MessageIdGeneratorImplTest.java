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
package com.magnet.mmx.server.plugin.mmxmgmt.message;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;

/**
 */
public class MessageIdGeneratorImplTest {
  @Test
  public void testGenerate() throws Exception {
    String clientId = "login3";
    String deviceId = "D009C747-6DF1-4A67-A2F6-F8AF210AB062";
    String appId = "ocei5yeerqr";
    Set<String> idSet = new HashSet<String>();

    int size = 100000;
    for (int i=0; i< size; i++) {
      String messageId = new StubMessageIdGenerator().generate(clientId, deviceId, appId);
      assertFalse ("Generated id is already contained", idSet.contains(messageId));
      idSet.add(messageId);
    }
  }


  static class StubMessageIdGenerator extends MessageIdGeneratorImpl {
    //return a fixed time
    @Override
    protected long getCurrentTimeMillis() {
      return 1424981217* 1000L;
    }
  }


}

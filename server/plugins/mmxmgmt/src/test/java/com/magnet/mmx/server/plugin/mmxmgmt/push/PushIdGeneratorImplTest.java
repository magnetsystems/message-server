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
package com.magnet.mmx.server.plugin.mmxmgmt.push;

import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 */
public class PushIdGeneratorImplTest {

  @Test
  public void testGenerateId() throws Exception {
    String appId = "i0sqlunda4q";
    String deviceId = "1399b38520170d08";
    PushIdGenerator generator = new PushIdGeneratorImpl();
    int count = 100;
    HashSet<String> ids = new HashSet<String>(count);
    for (int i=0; i<count; i++) {
      String pushMessageId = generator.generateId(appId, deviceId);
      assertNotNull(pushMessageId);
      assertFalse("Generated push message id exists", ids.contains(pushMessageId));
      ids.add(pushMessageId);
    }
  }

}




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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class AppIDGeneratorTest {
  private static Logger LOGGER = LoggerFactory.getLogger(AppIDGeneratorTest.class);

  /**
   * Test generating the ID using a known value of
   * timestamp and int value.
   * Use the generated ID and decode the bytes and then
   * assert that the timestamp value and int values are correct
   */
  @Test
  public void test1() throws Exception {
    long timestamp = 1412196717L;
    int randomValue = 32000;
    String generated = AppIDGenerator.generate(timestamp, randomValue);

    assertNotNull("Generated id is null", generated);

//    byte[] read = Base64.decodeBase64(generated.getBytes("utf-8"));
//
//    byte[] time = Arrays.copyOfRange(read, 0, 8);
//    long readValue = 0;
////    for (int i = 0; i < time.length; i++)
////    {
////      //readValue += ((long) time[i] & 0xffL) << (8 * i);
////      readValue = (readValue << 8) + (time[i] & 0xff);
////    }
//
//    ByteBuffer timeBuffer = ByteBuffer.wrap(time);
//
//    readValue = timeBuffer.getLong();
//
//    assertEquals("Got a bad time value", timestamp, readValue);
//
//
//    ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(read, 8, 12));
//    int readRandom = bb.getInt();
//
//    assertEquals("Got a bad integer value", readRandom, randomValue);
  }

  @Test
  public void testMultipleGenerate() {
    int count = 100;
    long sleep = 10; //milliseconds
    Set<String> generatedSet = new HashSet<String>(count);

    for (int i=0; i<count; i++) {
      String generated = AppIDGenerator.generate();

      assertTrue("Generated set already contains:" + generated, !generatedSet.contains(generated));
      generatedSet.add(generated);
      assertTrue("Generated contains /" + generated, generated.indexOf('/') == -1);
      LOGGER.debug("Generated:" + generated);
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException e) {
      }
    }







  }

}

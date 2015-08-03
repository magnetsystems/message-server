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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TopicHelperTest {


  @Test
  public void testIsAppTopic1() {
    String appId = "i1cglsw8dsa";
    String topicId = "/i1cglsw8dsa/*/com.magnet.os/ANDROID";

    boolean isAppTopic = TopicHelper.isAppTopic(topicId, appId);

    assertTrue("Wrong answer", isAppTopic);


  }

  @Test
  public void testIsAppTopic2() {
    String appId = "i1cglsw8dsb";
    String topicId = "/i1cglsw8dsa/*/com.magnet.os/ANDROID";

    boolean isAppTopic = TopicHelper.isAppTopic(topicId, appId);

    assertFalse("Wrong answer", isAppTopic);
  }

  @Test
  public void testIsUserTopic1() {
    String appId = "i1cglsw8dsa";
    String topicId = "/i1cglsw8dsa/*/com.magnet.os/ANDROID";
    boolean isUserTopic = TopicHelper.isUserTopic(topicId, appId);
    assertFalse("Wrong answer", isUserTopic);
  }

  @Test
  public void testIsUserTopic2() {
    String appId = "i1cglsw8dsa";
    String topicId = "/i1cglsw8dsa/login3.p/mycars";
    boolean isUserTopic = TopicHelper.isUserTopic(topicId, appId);
    assertTrue("Wrong answer", isUserTopic);
  }

  @Test
  public void testIsUserTopic3() {
    String appId = "i1cglsw8dsa";
    String topicId = "/i1cglsw8dsa/*/login3.p/mycars";
    boolean isUserTopic = TopicHelper.isUserTopic(topicId, appId);
    assertFalse("Wrong answer", isUserTopic);
  }

  @Test
  public void testValidateTopicName() {
    String name = "testTopic1";
    assertTrue(TopicHelper.validateApplicationTopicName(name));
  }
  @Test
  public void test1ValidateApplicationTopicName() {
    String topicName = "human";
    boolean valid = TopicHelper.validateApplicationTopicName(topicName);
    assertTrue("Topic name:" + topicName + " should be valid", valid);
  }

  @Test
  public void test2ValidateApplicationTopicName() {
    String topicName = "human.person";
    boolean valid = TopicHelper.validateApplicationTopicName(topicName);
    assertTrue("Topic name:" + topicName + " should be valid", valid);
  }

  @Test
  public void test3ValidateApplicationTopicName() {
    String topicName = "1human.person2";
    boolean valid = TopicHelper.validateApplicationTopicName(topicName);
    assertTrue("Topic name:" + topicName + " should be valid", valid);
  }

  @Test
  public void test4ValidateApplicationTopicName() {
    String topicName = "Educational";
    boolean valid = TopicHelper.validateApplicationTopicName(topicName);
    assertTrue("Topic name:" + topicName + " should be valid", valid);
  }

  @Test
  public void test5ValidateApplicationTopicName() {
    String topicName = "Transport_rides-shares";
    boolean valid = TopicHelper.validateApplicationTopicName(topicName);
    assertTrue("Topic name:" + topicName + " should be valid", valid);
  }

  @Test
  public void test1InvalidTopicNames() {
    String topicName = "Educational@SF";
    boolean valid = TopicHelper.validateApplicationTopicName(topicName);
    assertFalse("Topic name:" + topicName + " should be invalid", valid);
  }

  @Test
  public void test2InvalidTopicNames() {
    String topicName = "Educational#SF";
    boolean valid = TopicHelper.validateApplicationTopicName(topicName);
    assertFalse("Topic name:" + topicName + " should be invalid", valid);
  }

  @Test
  public void test3InvalidTopicNames() {
    String topicName = "Educational  SF";
    boolean valid = TopicHelper.validateApplicationTopicName(topicName);
    assertFalse("Topic name:" + topicName + " should be invalid", valid);
  }

  @Test
  public void test4InvalidTopicNames() {
    String topicName = "012345678901234567890123456789012345678901234567890123456789";
    boolean valid = TopicHelper.validateApplicationTopicName(topicName);
    assertFalse("Topic name:" + topicName + " should be invalid", valid);
  }
}

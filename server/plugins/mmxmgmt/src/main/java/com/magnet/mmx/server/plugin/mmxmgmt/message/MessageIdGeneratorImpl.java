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

import com.magnet.mmx.server.plugin.mmxmgmt.push.PushIdGeneratorImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Random;

/**
 */
public class MessageIdGeneratorImpl implements MessageIdGenerator {
  private static Logger LOGGER = LoggerFactory.getLogger(PushIdGeneratorImpl.class);
  private static Random randomGenerator;
  //statically initialize the random number generator
  {
    try {
      //try to use secure random based on this CERT advisory:
      //https://www.securecoding.cert.org/confluence/display/java/MSC02-J.+Generate+strong+random+numbers
      randomGenerator = SecureRandom.getInstance(MMXServerConstants.SECURE_RANDOM_ALGORITHM);
    } catch (Throwable t) {
      LOGGER.error("Problem in initializing the random number generator. Falling back to java.util.Random", t);
      randomGenerator = new Random();
    }
  }
  @Override
  public String generate(String clientId, String appId, String deviceId) {
    StringBuilder builder = new StringBuilder();
    builder.append(Long.toString(getCurrentTimeMillis()));
    builder.append(clientId);
    builder.append(appId);
    builder.append(randomGenerator.nextLong());
    if (deviceId != null) {
      builder.append(deviceId);
    }
    String generated;
    try {
      byte[] bytes = builder.toString().getBytes(MMXServerConstants.UTF8_ENCODING);
      generated = DigestUtils.md5Hex(bytes);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }
    return generated;
  }

  @Override
  public String generateTopicMessageId(String appId, String topicId) {
    StringBuilder builder = new StringBuilder();
    builder.append(Long.toString(getCurrentTimeMillis()));
    builder.append(appId);
    builder.append(topicId);
    builder.append(randomGenerator.nextLong());
    String generated;
    try {
      byte[] bytes = builder.toString().getBytes(MMXServerConstants.UTF8_ENCODING);
      generated = DigestUtils.md5Hex(bytes);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }
    return generated;
  }

  @Override
  public String generateItemIdentifier(String topicId) {
    StringBuilder builder = new StringBuilder();
    builder.append(Long.toString(getCurrentTimeMillis()));
    builder.append(topicId);
    builder.append(randomGenerator.nextLong());
    String generated;
    try {
      byte[] bytes = builder.toString().getBytes(MMXServerConstants.UTF8_ENCODING);
      generated = DigestUtils.md5Hex(bytes);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }
    return generated;
  }

  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }
}

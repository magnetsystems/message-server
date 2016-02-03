/*   Copyright (c) 2015-2016 Magnet Systems, Inc.
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
package com.magnet.mmx.server.plugin.mmxmgmt.apns;

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.util.Base64;

import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class APNSKeyStorePasswordTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(APNSKeyStorePasswordTest.class);
  private static boolean sPreJava_1_8;
  private final static String mP12CertWithEmptyPassword =
      "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";

  @BeforeClass
  public static void setUp() throws Exception {
    // version format is 1.x.xxxx
    String v = System.getProperty("java.version");
    sPreJava_1_8 = v.startsWith("1.") && v.charAt(3) == '.' && (v.charAt(2) - '0') < 8;
  }

  @AfterClass
  public static void tearDown() throws Exception {
  }

  @Test
  public void testEmptyPassword() {
    try {
      byte[] cert = Base64.decode(mP12CertWithEmptyPassword);
      KeyStore ks = KeyStore.getInstance("TLS");
      ks.load(new ByteArrayInputStream(cert), "".toCharArray());
      if (sPreJava_1_8) {
        fail("Expect pre-java 1.8 failure with empty password for keystore");
      }
    } catch (Exception e) {
      if (!sPreJava_1_8) {
        fail("Expect Java 1.8 or above having empty password for keystore fixed");
      }
    }
  }
}

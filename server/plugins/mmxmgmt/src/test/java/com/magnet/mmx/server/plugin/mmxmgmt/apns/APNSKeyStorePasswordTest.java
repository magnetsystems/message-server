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

import com.magnet.mmx.util.Base64;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;

import javax.crypto.BadPaddingException;

import static org.junit.Assert.fail;

/**
 * Test JDK 1.7 and older vs JDK 1.8 for the APNS keystore (p12 certificate)
 * with empty password and non-empty password.  According to Oracle, JDK 1.7 and
 * older should fail with empty password, but this test2 showed otherwise.
 *
 * mvn -s ~/.m2/settings.xml -Dtest=com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSKeyStorePasswordTest test
 */
public class APNSKeyStorePasswordTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(APNSKeyStorePasswordTest.class);
  private static boolean sPreJava_1_8;
  private final static String mP12CertWithEmptyPassword =
      "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";
  private final static String mP12CertWithPassword =
      "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";


  @BeforeClass
  public static void setUp() throws Exception {
    // version format is 1.8.xxxx
    String v = System.getProperty("java.version");
    sPreJava_1_8 = v.startsWith("1.") && v.charAt(3) == '.' && (v.charAt(2) - '0') < 8;
  }

  @AfterClass
  public static void tearDown() throws Exception {
  }

  @Test
  public void test1Password() {
    try {
      byte[] cert = Base64.decode(mP12CertWithPassword);
      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(new ByteArrayInputStream(cert), "magnetqe".toCharArray());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected failure in password keystore in all Java versions");
    }
  }

  @Test
  public void test2WrongPassword() {
    try {
      byte[] cert = Base64.decode(mP12CertWithPassword);
      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(new ByteArrayInputStream(cert), "bogus".toCharArray());
      fail("Expect failure with wrong password keystore in all Java versions");
    } catch (Throwable e) {
      if (e instanceof IOException) {
        if (sPreJava_1_8) {
          if (e.getCause() instanceof BadPaddingException) {
            return;
          }
        } else {
          if (e.getCause() instanceof UnrecoverableKeyException) {
            // Expected: keystore password was incorrect
            return;
          }
        }
      }
      e.printStackTrace();
      fail("Unexpected exception: "+e.getMessage());
    }
  }

  @Test
  public void test3NullPassword() {
    try {
      byte[] cert = Base64.decode(mP12CertWithEmptyPassword);
      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(new ByteArrayInputStream(cert), null);
      // No integrity check.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Expecting no integrity check for keystore for all Java versions");
    }
  }

  @Test
  public void test4EmptyPassword() {
    try {
      byte[] cert = Base64.decode(mP12CertWithEmptyPassword);
      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(new ByteArrayInputStream(cert), "".toCharArray());
      if (sPreJava_1_8) {
        // It didn't fail in 1.7 anymore.
//        fail("Expecting failure in Java 1.7 or older with empty password for keystore");
      }
    } catch (Exception e) {
      if (!sPreJava_1_8) {
        fail("Expecting Java 1.8 or newer having empty password for keystore fixed");
      }
    }
  }
}

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

import com.magnet.mmx.util.AppHelper;
import com.magnet.mmx.util.CryptoUtil;
import org.junit.Before;
import org.junit.Test;

import java.security.SignatureException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class CryptoUtilTest {

  private static String TEST_KEY = AppHelper.generateRandomKey();
  private static String TEST_DATA = AppHelper.generateApiKey();
  private static String baselineResult;

  @Before
  public void initData() throws SignatureException {
    baselineResult = CryptoUtil.generateHmacSha1(TEST_DATA, TEST_KEY);
  }
  @Test
  public void testBasicHmacSha1() throws SignatureException {
    String result = CryptoUtil.generateHmacSha1(TEST_DATA, TEST_KEY);
    assertEquals(baselineResult, result);
  }

  @Test
  public void testBasicHmacSha1WrongKey() throws SignatureException {
    // sign it with a diff key. should get diff. result
    String key = AppHelper.generateRandomKey();
    String result = CryptoUtil.generateHmacSha1(TEST_DATA, key);
    assertNotEquals(baselineResult, result);
  }
}

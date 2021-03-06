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
package com.magnet.mmx.server.plugin.mmxmgmt.db;

import com.magnet.mmx.server.common.spi.IEncryptor;
import com.magnet.mmx.server.common.utils.EncryptorFactory;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;


public class EncryptorForTest implements IEncryptor {

  static {
    EncryptorFactory.RegisterEncryptorClass("test-encryptor", EncryptorForTest.class, true);
  }
  public EncryptorForTest() {
  }

  @Override
  public String getName() {
    return "test-encryptor";
  }

  @Override
  public String getEncrypted(String value) {
    return Helper.reverse(value);
  }

  @Override
  public String getDecrypted(String value) {
    return Helper.reverse(value);
  }
}

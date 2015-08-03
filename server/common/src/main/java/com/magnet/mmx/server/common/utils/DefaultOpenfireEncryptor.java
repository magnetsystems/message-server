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
package com.magnet.mmx.server.common.utils;

import com.magnet.mmx.server.common.spi.IEncryptor;
import org.jivesoftware.openfire.auth.AuthFactory;


public class DefaultOpenfireEncryptor implements IEncryptor {

  public static String NAME = "of-authfactory-default";

  static {
    EncryptorFactory.RegisterEncryptorClass(NAME, DefaultOpenfireEncryptor.class, true);
  }

  public DefaultOpenfireEncryptor() {
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getEncrypted(String value) {
    String encrypted = null;
    try {
      encrypted = AuthFactory.encryptPassword(value);
      // Set password to null so that it's inserted that way.
    } catch (UnsupportedOperationException uoe) {
      // Encrypting the apiKey may have failed. Therefore,

    }
    return encrypted;
  }

  @Override
  public String getDecrypted(String value) {
    String decrypted = null;
    try {
      decrypted = AuthFactory.decryptPassword(value);
      // Set password to null so that it's inserted that way.
    } catch (UnsupportedOperationException uoe) {
      // Encrypting the apiKey may have failed. Therefore,

    }
    return decrypted;
  }
}

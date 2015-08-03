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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class EncryptorFactory {

  private static ConcurrentMap<String, IEncryptor> sIEncryptorMap = new ConcurrentHashMap<String, IEncryptor>();
  private static ConcurrentMap<String, Class<? extends IEncryptor>> sIEncryptorClassMap = new ConcurrentHashMap<String, Class<? extends IEncryptor>>();
  private static Class<? extends IEncryptor> defaultEncryptorClass;
  private static IEncryptor sDefaultEncryptor = new DefaultOpenfireEncryptor();  // this is the default

  /**
   * Register global instance of encryptor suitable for testing
   * @param name
   * @param encryptor
   */
  public static synchronized void RegisterEncryptor(String name, IEncryptor encryptor) {
    sIEncryptorMap.put(name, encryptor);

  }
  /**
   * Register global instance of encryptor class suitable for testing
   * @param name
   * @param encryptorClazz
   */
  public static synchronized void RegisterEncryptorClass(String name, Class<? extends IEncryptor> encryptorClazz, boolean isDefault) {
    sIEncryptorClassMap.put(name, encryptorClazz);
    if (isDefault) {
      defaultEncryptorClass = encryptorClazz;
    }
  }

  /**
   *   create new instance of encryptor by name
   */
  public IEncryptor createInstance(String name) throws IllegalAccessException, InstantiationException {
    if (sIEncryptorClassMap.containsKey(name)) {
      Class encryptorClass = sIEncryptorClassMap.get(name);
      return (IEncryptor) encryptorClass.newInstance();
    }
    throw new IllegalArgumentException("no such encryptor name registered:" + name);
  }

  /**
  /* create new instance of default encryptor
   */
  public IEncryptor createInstance() throws IllegalAccessException, InstantiationException {
    if (defaultEncryptorClass != null) {
      return (IEncryptor) defaultEncryptorClass.newInstance();
    } else {
      // default to openfire
      return sDefaultEncryptor;
    }
  }
}

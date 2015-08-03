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
package com.magnet.mmx.server.plugin.mmxmgmt;

import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import org.apache.commons.codec.digest.DigestUtils;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 */
class MMXSSLConfiguration {
  private final static Logger LOGGER = LoggerFactory.getLogger(MMXSSLConfiguration.class);

  private final static String KEYSTORE_PROP_KEY = "xmpp.socket.ssl.keystore";
  private final static String KEYSTORE_PASSWORD_PROP_KEY = "xmpp.socket.ssl.keypass";

  private String keyStoreFilePath;
  private String keyStorePassword;

  private MMXSSLConfiguration() {
  }

  /**
   * Fully qualified path to the keystore file.
   *
   * @return
   */
  public String getKeyStoreFilePath() {
    return keyStoreFilePath;
  }

  /**
   * Key store password.
   *
   * @return
   */
  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  @Override
  public String toString() {
    String md5Password = null;
    try {
      byte[] bytes = keyStorePassword.toString().getBytes(MMXServerConstants.UTF8_ENCODING);
      md5Password = DigestUtils.md5Hex(bytes);
    } catch (Throwable t) {
      LOGGER.info("Throwable in computing md5sum", t);
    }
    final StringBuilder sb = new StringBuilder("MMXSSLConfiguration{");
    sb.append("keyStoreFilePath='").append(keyStoreFilePath).append('\'');
    sb.append(", MD5sum of keyStorePassword='").append(md5Password);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Construct MMXSSLConfiguration using SSL certificate configured for SSL.
   * This is based on the code in SSLConfig class in openfire code base.
   *
   * @return MMXSSLConfiguration
   */
  public static MMXSSLConfiguration usingOpenFireCert() {
    // Get the keystore location. The default location is security/keystore
    String relativeKeyStoreLocation = JiveGlobals.getProperty(KEYSTORE_PROP_KEY,
        "resources" + File.separator + "security" + File.separator + "keystore");
    String keyStoreLocation = JiveGlobals.getHomeDirectory() + File.separator + relativeKeyStoreLocation;
    // Get the keystore password. The default password is "changeit".
    String password = JiveGlobals.getProperty(KEYSTORE_PASSWORD_PROP_KEY, "changeit");
    MMXSSLConfiguration sslConfig = new MMXSSLConfiguration();
    sslConfig.keyStoreFilePath = keyStoreLocation;
    sslConfig.keyStorePassword = password;
    return sslConfig;
  }

}

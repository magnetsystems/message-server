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

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 */
public class MMXVersion {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXVersion.class);
  private static final String VER_FILE_NAME = "version.properties";
  private static final String VER_PROPERTY = "mmx.version";
  private static String version = null;

  public static synchronized String getVersion() {
    if(version == null) {
      try {
        InputStream inputStream = MMXVersion.class.getResourceAsStream("/" + VER_FILE_NAME);
        if(inputStream != null) {
          Properties props = new Properties();
          props.load(inputStream);
          String s = props.getProperty(VER_PROPERTY);
          if(!Strings.isNullOrEmpty(s)) {
            version = s;
          }
        } else {
          LOGGER.trace("getVersion : file={} not found", VER_FILE_NAME);
        }
      } catch (Exception e) {
        LOGGER.error("getVersion : {}");
      }
    }
    return version == null ? "Version not available" : version;
  }
}

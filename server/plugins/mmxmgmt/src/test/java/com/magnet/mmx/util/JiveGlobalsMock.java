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

import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 */
public class JiveGlobalsMock {
  private static final Logger LOGGER = LoggerFactory.getLogger(JiveGlobalsMock.class);
  public static void setup() {
    URL home = JiveGlobalsMock.class.getResource("/");
    LOGGER.trace("setup : found path : {}", home.getPath());
    JiveGlobals.setHomeDirectory(home.getPath());
    JiveGlobals.setConfigName("openfire.xml");
  }
}

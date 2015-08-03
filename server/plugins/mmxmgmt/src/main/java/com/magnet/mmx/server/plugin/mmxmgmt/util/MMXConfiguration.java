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

import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * MMX Configuration Singleton.
 * 
 * A wrapper around openfire's JiveGlobals class. All the properties will be stored in  
 * ofProperty table.
 *
 */
public class MMXConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MMXConfiguration.class);

  private static class ConfigHolder {
    private static final MMXConfiguration INSTANCE = new MMXConfiguration();
  }

  public static final MMXConfiguration getConfiguration() {
    return ConfigHolder.INSTANCE;
  }

  /**
   * Get the value associated with the configuration key.
   * @param key
   * @return property value.
   */
  public String getString(String key) {
    return getString(key, null);
  }

  /**
   * Get value associated with the configuration key. If the value
   * is not found then return the supplied defaultValue
   * @param key
   * @param defaultValue
   * @return
   */
  public String getString(String key, String defaultValue) {
    return JiveGlobals.getProperty(key, defaultValue);
  }

  /**
   * Get int value associated with the configuration key. If the configuration
   * key is not found then return the supplied defaultValue.
   * @param key
   * @param defaultValue
   * @return int value
   * @throws java.lang.RuntimeException if the value can't be converted
   * to int
   */
  public int getInt(String key, int defaultValue) {
    return JiveGlobals.getIntProperty(key, defaultValue);
  }

  /**
   * Get long value associated with the configuration key
   * @param key
   * @return
   */
  public long getLong(String key, long defaultValue) {
    return JiveGlobals.getLongProperty(key, defaultValue);
  }

  /**
   * Get boolean value associated with the configuration key.
   * @param key
   * @param defaultValue
   * @return
   */
  public boolean getBoolean(String key, boolean defaultValue) {
    return JiveGlobals.getBooleanProperty(key, defaultValue);
  }

  /**
   * Get list of values associated with the configuration key
   * @param key
   * @return value
   */

  public List<String> getList(String key) {
    String listStr = JiveGlobals.getProperty(key);
    List<String> returnList = Helper.getListFromCommaDelimitedString(listStr);
    return returnList;
  }
  /**
   * Get all the keys in the configuration object.
   * @return Iterator<String>
   * Please note that you shouldn't use the Iterator to remove configuration
   * properties.
   * The iterator should be used only for ready the configuration values.
   */
  public Iterator<String> getKeys() {
    return JiveGlobals.getPropertyNames().iterator();
  }

  /**
   * Set a value for the specified key.
   * Note: This doesn't do any validation on the value.
   * @param key
   * @param value
   */
  public void setValue (String key, String value) {
    JiveGlobals.setProperty(key, value);
    PropertyChangeHandler.handle(key);
  }


  /**
   * Return whether the specified key is an XMPP system property
   * @param key Key value, such as "xmpp.client.tls.policy"
   * @return
   */
  public static boolean isXmppProperty(String key) {
    return key != null && key.startsWith("xmpp.");
  }
}


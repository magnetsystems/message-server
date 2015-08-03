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

/**
 * DB Entity cache interface.
 */
public interface DBEntityCache<E> {

  /**
   * Retrieve the cached object for the passed in key.
   * If the object for the passed in key is not found in the cache ,the cache implementation
   * should load it from the database. If it can't be loaded null should be returned.
   * @param key
   * @return
   */
  public E get(String key);

  /**
   * Remove the cached object for the passed in key from the cache.
   * @param key
   */
  public void purge(String key);

}

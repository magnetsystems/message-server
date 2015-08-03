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
package com.magnet.mmx.server.plugin.mmxmgmt.context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ContextDispatcherFactory {

  private static class SingletonHolder {
    private static final ContextDispatcherFactory INSTANCE = new ContextDispatcherFactory();
  }

  public static ContextDispatcherFactory getInstance() {
    return SingletonHolder.INSTANCE;
  }

  private volatile ConcurrentMap<String, IContextDispatcher> dispatcherInstanceMap =
      new ConcurrentHashMap<String, IContextDispatcher>();
  private volatile ConcurrentMap<String, Class<? extends IContextDispatcher>> dispatcherClassMap =
      new ConcurrentHashMap<String, Class<? extends IContextDispatcher>>();

  /**
   * Register existing global instance of dispatcher class by name
   * @param name
   * @param dispatcher
   */
  public void registerInstance(String name, IContextDispatcher dispatcher) {
    dispatcherInstanceMap.put(name, dispatcher);
  }
  /**
   * Register global instance of dispatcher class by name
   * @param name
   * @param clazz
   */
  public void registerClass(String name, Class<? extends IContextDispatcher> clazz) {
    dispatcherClassMap.put(name, clazz);
  }

  /**
   *  create or get Singleton instance of dispatcher by name
   */
  public IContextDispatcher getDispatcher(String name) throws IllegalAccessException, InstantiationException {
    IContextDispatcher result = dispatcherInstanceMap.get(name);

    if (result == null) {
      synchronized (ContextDispatcherFactory.class) {
        result = dispatcherInstanceMap.get(name);
        if (result == null) {
          Class<? extends IContextDispatcher> clazz = dispatcherClassMap.get(name);
          if (clazz != null) {
            result = (IContextDispatcher) clazz.newInstance();
            dispatcherInstanceMap.put(name, result);
          } else {
            throw new IllegalArgumentException("no such ContextDispatcher name registered:" + name);
          }
        }
      }
    }
    return result;
  }
}

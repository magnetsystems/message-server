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

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 */
public class MMXExecutors {
  private static ConcurrentHashMap<String, ExecutorService> executorMap = new ConcurrentHashMap<String, ExecutorService>();

  public static ExecutorService getOrCreate(String name, int size) {
    ExecutorService service = executorMap.get(name);
    if(service == null) {
      ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
              .setNameFormat(name + "-%d").build();
      service = Executors.newFixedThreadPool(size, namedThreadFactory);
      ExecutorService service1 = executorMap.putIfAbsent(name, service);
      if(service1 != null) {
        service = service1;
      }
    }
    return service;
  }

  public static void remove(String name) {
    executorMap.remove(name);
  }
}

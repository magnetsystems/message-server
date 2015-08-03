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
package com.magnet.mmx.server.plugin.mmxmgmt.monitoring;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

/**
 */
public class RateLimiterService {
  private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterService.class);
  private static LoadingCache<RateLimiterDescriptor, RateLimiter> rateLimiterMap = CacheBuilder.newBuilder()
          .maximumSize(100)
          .build(
                  new CacheLoader<RateLimiterDescriptor, RateLimiter>() {
                    public RateLimiter load(RateLimiterDescriptor descriptor) {
                      return RateLimiter.create(descriptor.getPermitsPerSecond());
                    }
                  });

  private static RateLimiter getOrCreate(RateLimiterDescriptor descriptor) throws ExecutionException {
    return rateLimiterMap.get(descriptor);
  }

  public static boolean isAllowed(RateLimiterDescriptor descriptor) {
    if(descriptor.getPermitsPerSecond() <= 0) {
      LOGGER.trace("isAllowed : rate limiting disabled");
      return true;
    }
    try {
      LOGGER.trace("isAllowed : getting ratelimiter for descriptor={}", descriptor);
      RateLimiter r = getOrCreate(descriptor);
      LOGGER.trace("isAllowed : retireved ratelimiter={}, rate={}", r, r.getRate());
      return r.tryAcquire();
    } catch (ExecutionException e) {
      LOGGER.error("isAllowed : Caught exception getting RateLimiter for descriptor : {}", descriptor, e);
      return false;
    }
  }

  public static void updateRates(String type, long rate) {
    LOGGER.debug("updateRates : type={}, rate={}", type, rate);
    Iterator<RateLimiterDescriptor> iterator = rateLimiterMap.asMap().keySet().iterator();
    while(iterator.hasNext()) {
      RateLimiterDescriptor oldRateDesc = iterator.next();
      if(type.equals(oldRateDesc.getType())) {
        LOGGER.trace("updateRates : invalidated rate={}", oldRateDesc);
        rateLimiterMap.invalidate(oldRateDesc);
        RateLimiterDescriptor newRateDesc = new RateLimiterDescriptor(oldRateDesc.getType(), oldRateDesc.getAppId(), rate);
        LOGGER.trace("updateRates : updating rate desc={}, rate={}", newRateDesc, rate);
        rateLimiterMap.put(newRateDesc, RateLimiter.create(rate));
        rateLimiterMap.refresh(newRateDesc);
      }
    }
  }
}

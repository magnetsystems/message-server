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
package com.magnet.mmx.server.api.v1;

import com.google.common.base.Strings;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXHttpRateExceededEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.monitoring.RateLimiterDescriptor;
import com.magnet.mmx.server.plugin.mmxmgmt.monitoring.RateLimiterService;
import com.magnet.mmx.server.plugin.mmxmgmt.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Created by sdatar on 6/2/15.
 */
@Provider
@Priority(MMXServerConstants.MMX_RATE_LIMIT_PRIORITY)
public class RateLimitFilter implements ContainerRequestFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilter.class);
  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    Object prop = containerRequestContext.getProperty(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);
    if(prop instanceof AppEntity) {
      AppEntity appEntity = (AppEntity) prop;
      String appId = appEntity.getAppId();
      if(!Strings.isNullOrEmpty(appEntity.getAppId())) {
        int rate = MMXConfiguration.getConfiguration().getInt(MMXConfigKeys.MAX_HTTP_RATE, MMXServerConstants.DEFAULT_MAX_HTTP_RATE);

        RateLimiterDescriptor descriptor = new RateLimiterDescriptor(MMXServerConstants.HTTP_RATE_TYPE, appId,
                                                                     rate);
        if(!RateLimiterService.isAllowed(descriptor)){
          LOGGER.error("filter : Rate limit exceeded for appId : {}", appId);
          ErrorResponse mmxErrorResponse = new ErrorResponse(ErrorCode.RATE_LIMIT_EXCEEDED,
                  "Exceeded rate limit for appiId :" + appId);
          Response httpErrorResponse = Response.status(429).entity(mmxErrorResponse).build();
          int limit = AlertsUtil.getMaxHttpRate();
          AlertEventsManager.post(new MMXHttpRateExceededEvent(appId, limit));
          containerRequestContext.abortWith(httpErrorResponse);
        }
      } else {
        sendErrorResponse(containerRequestContext);
      }
    } else {
      sendErrorResponse(containerRequestContext);
    }
  }

  private void sendErrorResponse(ContainerRequestContext containerRequestContext) {
    LOGGER.error("filter : appId is not set");
    Response response = RestUtils.buildMissingHeaderResponse(MMXServerConstants.HTTP_HEADER_APP_ID);
    containerRequestContext.abortWith(response);
  }
}
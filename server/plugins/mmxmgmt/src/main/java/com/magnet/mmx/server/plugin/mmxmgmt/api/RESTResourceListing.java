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
package com.magnet.mmx.server.plugin.mmxmgmt.api;

import com.magnet.mmx.server.api.v1.DevicesResource;
import com.magnet.mmx.server.api.v1.MMXAuthHeadersFilter;
import com.magnet.mmx.server.api.v1.RateLimitFilter;
import com.magnet.mmx.server.api.v2.ChannelResource;
import com.magnet.mmx.server.api.v2.DeviceTagsResource;
import com.magnet.mmx.server.api.v2.MMXVersionResource;
import com.magnet.mmx.server.api.v2.MessageResource;
import com.magnet.mmx.server.api.v2.PushMessageResource;
import com.magnet.mmx.server.api.v2.UserResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.message.MessageStatusResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PingMessageFunctionResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PushMessageFunctionResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PushResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PushSuppressResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.MMXDeviceTagsResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.MMXTopicTagsResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.MMXUserTagsResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.topics.MMXTopicSummaryResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.topics.MMXTopicsItemsResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.user.MMXUsersResource;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.JacksonJSONObjectMapperProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.TopicResource;

/**
 * Grouping MMX REST API into v1 and v2.  The v1 API uses appKey and appId
 * headers as its proprietary authorization mechanism.  The v2 API uses a
 * standard token based authorization mechanism.
 */
public final class RESTResourceListing {

  // Any new resources added should be added to this list at compile time.
  // we maintain this list statically because I am not able to get
  // resteasy to scan all classes and bind the resources automatically.
  private static final String[] v1ResourceClasses = {
      MMXDeviceTagsResource.class.getName(),
      MMXUserTagsResource.class.getName(),
      MMXTopicTagsResource.class.getName(),
      MMXTopicsItemsResource.class.getName(),
      MMXUsersResource.class.getName(),
      MessageFunctionResource.class.getName(),
      PingMessageFunctionResource.class.getName(),
      PushMessageFunctionResource.class.getName(),
      PushResource.class.getName(),
      /**
       * add the refactored APIs
       */
      TopicResource.class.getName(),
      MMXTopicSummaryResource.class.getName(),
      DevicesResource.class.getName(),
      MessageStatusResource.class.getName(),
  };

  // New v2 API
  private static final String[] v2ResourceClasses = {
    MMXVersionResource.class.getName(),
    ChannelResource.class.getName(),
    UserResource.class.getName(),
    MessageResource.class.getName(),
    DeviceTagsResource.class.getName(),
    PushMessageResource.class.getName(),
          PushSuppressResource.class.getName()
  };

  public static String[] getV1Resources() {
    return v1ResourceClasses;
  }

  public static String[] getV2Resources() {
    return v2ResourceClasses;
  }

  private static final String[] providers = {
          MMXAuthHeadersFilter.class.getName(),
          RateLimitFilter.class.getName(),
          JacksonJSONObjectMapperProvider.class.getName()
  };

  public static String[] getProviders() { return providers;}
}

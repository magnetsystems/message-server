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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet;

import com.magnet.mmx.server.api.v1.MMXAppIdFilter;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration.IntegrationAppResource;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration.IntegrationUserResource;
//import com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration.IntegrationDeviceResource;

/**
 */
public final class AdminRESTResourceListing {

  // Any new resources added should be added to this list at compile time.
  // we maintain this list statically because I am not able to get
  // resteasy to scan all classes and bind the resources automatically.
  private static final String[] resourceClasses = {
      MMXAppStatsResource.class.getName(),
      EndPointResource.class.getName(),
      MMXSetupStatusResource.class.getName(),
      AppResource.class.getName(),
      IntegrationAppResource.class.getName(),
      IntegrationUserResource.class.getName()
  };


  public static String[] getResources() {
    return resourceClasses;
  }

  private static final String[] providers = {
      MMXAppIdFilter.class.getName(),
      JacksonJSONObjectMapperProvider.class.getName()
  };

  public static String[] getProviders() { return providers;}

}

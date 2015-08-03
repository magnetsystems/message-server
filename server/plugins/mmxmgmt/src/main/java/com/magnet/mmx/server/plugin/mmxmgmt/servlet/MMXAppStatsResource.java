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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 */

@Path("stats/ownerId/")
public class MMXAppStatsResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXAppStatsResource.class);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{ownerId}/app/{appId}")
  public MMXAppStats getStats(@PathParam("ownerId") String ownerId, @PathParam("appId") String appId) {
    List<String> appIds = new ArrayList<String>();
    appIds.add(appId);
    MMXStats mmxStats = new MMXStats(appIds);
    return mmxStats.getFirst();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{ownerId}")
  public MMXAppStats[] getStats(@PathParam("ownerId") String ownerId) {

    MMXConfiguration mmxConfiguration = MMXConfiguration.getConfiguration();
    String accessControlMode = mmxConfiguration.getString(MMXConfigKeys.ADMIN_REST_API_ACCESS_CONTROL_MODE,
        MMXServerConstants.DEFAULT_ADMIN_REST_API_ACCESS_CONTROL_MODE);
    List<String> appIds;

    if (accessControlMode.equalsIgnoreCase(MMXServerConstants.ADMIN_REST_API_ACCESS_CONTROL_MODE_RELAXED)) {
      LOGGER.info("returning stats for all apps");
      List<AppEntity> entityList = getAppDao().getAllApps();
      appIds = new LinkedList<String>();
      for (AppEntity appEntity : entityList) {
        appIds.add(appEntity.getAppId());
      }
    } else {
      appIds = getAppDao().getAllAppIds(ownerId);
    }
    if(appIds != null && !appIds.isEmpty()) {
      MMXStats mmxStats = new MMXStats(appIds);
      return mmxStats.getAppStats();
    }
    return null;
  }

  private AppDAO getAppDao() {
    return new AppDAOImpl(new OpenFireDBConnectionProvider());
  }
}

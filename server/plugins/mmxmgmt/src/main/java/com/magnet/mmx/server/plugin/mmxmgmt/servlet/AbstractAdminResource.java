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
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Base AbstractAdminResource that provides methods to validate the ownerId to appId
 * co-relation.
 */
public abstract class AbstractAdminResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAdminResource.class);
  private static String ERROR_INVALID_AUTHORIZATION = "Authorization using required header:%s failed";
  private static String ERROR_INVALID_APPID = "Supplied appId is invalid";


  protected com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse isAuthorized(HttpHeaders headers, AppDAO appDAO, String appId) {
    MMXConfiguration mmxConfiguration = MMXConfiguration.getConfiguration();
    String accessControlMode = mmxConfiguration.getString(MMXConfigKeys.ADMIN_REST_API_ACCESS_CONTROL_MODE, MMXServerConstants.DEFAULT_ADMIN_REST_API_ACCESS_CONTROL_MODE);

    if (accessControlMode.equalsIgnoreCase(MMXServerConstants.ADMIN_REST_API_ACCESS_CONTROL_MODE_RELAXED)) {
      LOGGER.info("Using relaxed access control mode");
      return null;
    }
    MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
    String appOwner = requestHeaders.getFirst(MMXServerConstants.HTTP_HEADER_ADMIN_APP_OWNER_KEY);

    if (appOwner == null || appOwner.isEmpty()) {
      return buildAuthFailure(ErrorCode.APP_OWNER_ID_MISSING);
    }

    if (appId == null || appId.isEmpty()) {
      return buildBadAppIdFailure(ErrorCode.APP_OWNER_ID_MISSING);
    }

    AppEntity appEntity = appDAO.getAppForAppKey(appId);
    if (appEntity == null) {
      return buildBadAppIdFailure(ErrorCode.AUTH_BAD_APP_ID);
    }

    String owner = appEntity.getOwnerId();

    if (!owner.equals(appOwner)) {
      LOGGER.info("Request failed app and ownerId check");
      return buildAuthFailure(ErrorCode.AUTH_APPID_OWNERID_MISMATCH);
    }
    return null;
  }


  protected com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse buildAuthFailure(ErrorCode code) {
    return new com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse(code, String.format(ERROR_INVALID_AUTHORIZATION,
        MMXServerConstants.HTTP_HEADER_ADMIN_APP_OWNER_KEY));
  }

  protected com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse buildBadAppIdFailure(ErrorCode code) {
    return new com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse(code, ERROR_INVALID_APPID);
  }
}

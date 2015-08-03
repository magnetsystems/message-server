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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 */
public class AbstractBaseResource {
  //private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBaseResource.class);

  private static String ERROR_INVALID_AUTHENTICATION = "Authentication using required headers:%s and %s failed";


  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }

  protected ErrorResponse isAuthenticated(HttpHeaders headers, AppDAO appDAO) {
    return isAuthenticated(headers, appDAO, null);
  }

  protected ErrorResponse isAuthenticated(HttpHeaders headers, AppDAO appDAO, AppEntityHolder holder) {
    MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
    String appId = requestHeaders.getFirst(MMXServerConstants.HTTP_HEADER_APP_ID);
    String apiKey = requestHeaders.getFirst(MMXServerConstants.HTTP_HEADER_REST_API_KEY);

    if (appId == null || appId.isEmpty() || apiKey == null || apiKey.isEmpty()) {
      return buildAuthFailure(ErrorCode.AUTH_MISSING);
    }

    AppEntity appEntity = appDAO.getAppForAppKey(appId);
    if (appEntity == null) {
      return buildAuthFailure(ErrorCode.AUTH_BAD_APP_ID);
    }

    String appApiKey = appEntity.getAppAPIKey();
    if (!appApiKey.equals(apiKey)) {
      return buildAuthFailure(ErrorCode.AUTH_APPID_APIKEY_MISMATCH);
    }

    if (holder != null) {
      holder.setAppEntity(appEntity);
    }
    return null;
  }

  protected ErrorResponse buildAuthFailure(ErrorCode code) {
    return new ErrorResponse(code, String.format(ERROR_INVALID_AUTHENTICATION,
        MMXServerConstants.HTTP_HEADER_APP_ID, MMXServerConstants.HTTP_HEADER_REST_API_KEY));
  }


  public static interface AppEntityHolder {

    public AppEntity getAppEntity();

    public void setAppEntity(AppEntity appEntity);

  }

  public static class AppEntityHolderImpl implements  AppEntityHolder{

    private AppEntity appEntity;

    @Override
    public AppEntity getAppEntity() {
      return appEntity;
    }

    @Override
    public void setAppEntity(AppEntity appEntity) {
      this.appEntity = appEntity;
    }
  }

  /**
   * Exception to invalid validation problem
   */
  public static class ValidationException extends Exception {
    private ErrorResponse error;
    public ValidationException (ErrorResponse error) {
      super(error.getMessage());
      this.error = error;
    }

    public ErrorResponse getError() {
      return error;
    }
  }

}

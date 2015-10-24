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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.magnet.mmx.sasl.TokenInfo;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;

/**
 */
public class AbstractBaseResource {
  //private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBaseResource.class);

  private static String ERROR_INVALID_AUTHENTICATION = "Authentication using required headers:%s and %s failed";

  private static String ERROR_INVALID_USER_AUTHENTICATION = "Authentication using required headers:%s failed";

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
  
  protected Response isAuthenticatedUser(HttpHeaders headers, UserEntityHolder holder) {
    TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
    if (tokenInfo == null) {
      return RestUtils.getUnauthJAXRSResp();
    }
    if (holder != null) {
      UserEntity userEntity = new UserEntity();
      userEntity.setUsername(JIDUtil.makeNode(tokenInfo.getUserId(), tokenInfo.getMmxAppId()));
      userEntity.setName(toDisplayName(tokenInfo.getFirstName(), tokenInfo.getLastName()));
      holder.setAuthToken(null);
      holder.setUserEntity(userEntity);
    }
    return null;
  }
  
  private String toDisplayName(String firstName, String lastName) {
    if (firstName == null || firstName.isEmpty()) {
      return lastName;
    }
    if (lastName == null || lastName.isEmpty()) {
      return firstName;
    }
    return firstName + ' ' + lastName;
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

  public static interface UserEntityHolder {
    public String getAuthToken();
    public void setAuthToken(String authToken);
    public UserEntity getUserEntity();
    public void setUserEntity(UserEntity userEntity);
  }
  
  public static class UserEntityHolderImpl implements UserEntityHolder {
    private String authToken;
    private UserEntity userEntity;
    
    @Override
    public String getAuthToken() {
      return authToken;
    }
    
    @Override
    public void setAuthToken(String authToken) {
      this.authToken = authToken;
    }
    
    @Override
    public UserEntity getUserEntity() {
      return userEntity;
    }

    @Override
    public void setUserEntity(UserEntity userEntity) {
      this.userEntity = userEntity;
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

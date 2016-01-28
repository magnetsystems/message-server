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

import com.magnet.mmx.protocol.AppCreate;
import com.magnet.mmx.server.api.v1.protocol.AppConfig;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSCertificateValidator;
import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSConnectionPool;
import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSConnectionPoolImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.bot.BotStarter;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppAlreadyExistsException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppConfigurationCache;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppConfigurationEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppConfigurationEntityDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppConfigurationEntityDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppManagementException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.gcm.GCMAPIKeyValidator;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXAppManager;
import com.magnet.mmx.server.plugin.mmxmgmt.monitoring.MaxAppLimitExceededException;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.AppHelper;
import com.magnet.mmx.util.Utils;
import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * AppResource. Provides API for App related CRUD.
 */
@Path("apps")
public class AppResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppResource.class);
  private static final String APNS_CERT_SIZE_TOO_BIG = "Supplied APNS certificate exceeds the maximum allowed size: "+WebConstants.APNS_CERT_MAX_SIZE;
  private static final String INVALID_APP_NAME_MSG = "Supplied app name is invalid.";
  private static final String APP_NAME_TOO_LONG_MSG = "Supplied app name is too long.";
  private static final String INVALID_APP_OWNER_ID_MSG = "Supplied app owner is invalid.";
  private static final String APP_CREATION_FAILED = "App creation failed.";
  private static final String APP_LIMIT_REACHED = "You have the maximum number of apps. You will need to delete an app to create one.";
  private static final String APNS_CERT_SIZE_TOO_BIG_CODE = "CERT_TOO_BIG";
  private static final String APP_ID_EMPTY_OR_NULL = "Supplied app id is invalid.";
  private static final String APP_NOT_FOUND = "App with supplied id not found.";
  private static final String INVALID_APNS_CERTIFICATE = "APNS Certificate is invalid. Unable to read the certificate using APNS Password.";
  private static final String INVALID_GOOGLE_API_KEY_MSG = "Supplied Google API Key is invalid";

  static final DateFormat iso8601DateFormat = Utils.buildISO8601DateFormat();
  private static final String APNS_CERT_PASSWORD = "apnsCertPassword";
  private static final String APNS_CERT_FILE = "certfile";
  private static final String APP_ID_KEY = "appId";

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createApp(AppInfo request) {
    LOGGER.trace("createApp : request={}", request);
    try {
      String appName = request.getName();
      if (appName == null || appName.length() == 0) {
        throw buildForBadRequest(AppErrorCode.INVALID_APP_NAME.name(), INVALID_APP_NAME_MSG);
      }
      if (appName.length() > 100) {
        throw buildForBadRequest(AppErrorCode.INVALID_APP_NAME.name(), APP_NAME_TOO_LONG_MSG);
      }
      String owner = request.getOwnerId();
      if (owner == null || owner.length() == 0) {
        throw buildForBadRequest(AppErrorCode.INVALID_APP_OWNER.name(), INVALID_APP_OWNER_ID_MSG);
      }

      String googleApikey = request.getGoogleApiKey();
      if (googleApikey != null && !googleApikey.isEmpty()) {
        GCMAPIKeyValidator.validate(googleApikey);
      }
      MMXAppManager mmxAppManager = MMXAppManager.getInstance();

      AppCreate.Response response = mmxAppManager.createApp(appName, request.getServerUserId(), null,
          request.getGuestSecret(), googleApikey, request.getGoogleProjectId(),
          request.getApnsCertPassword(), request.getOwnerId(), request.getOwnerEmail(),
          request.isApnsCertProduction());

      String appId = response.getAppId();
      AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
      AppEntity appEntity = dao.getAppForAppKey(appId);

      /**
       * create default configuration for the mute period.
       */
      AppConfigurationEntityDAO configurationDAO = new AppConfigurationEntityDAOImpl(new OpenFireDBConnectionProvider());
      configurationDAO.updateConfiguration(appEntity.getAppId(), MMXConfigKeys.WAKEUP_MUTE_PERIOD_MINUTES,
          Integer.toString(MMXServerConstants.WAKEUP_MUTE_PERIOD_MINUTES_DEFAULT));

      // If we just created a bot enabled app, we want to create the bots for the app.
      if (BotStarter.isBotEnabled(appName)) {
        Future<Boolean> resultFuture = BotStarter.startApplicableBots(appName, appId, Executors.newSingleThreadExecutor());
        if (resultFuture != null && resultFuture.get()) {
          LOGGER.debug("Created/Started bots for appId:{} and name:{}", appId, appName);
        } else {
          LOGGER.warn("Failed to create bots for appId:{} and name:{}", appId, appName);
        }
      }
      Response createdResponse = Response.status(Response.Status.CREATED)
                                 .entity(new JSONFriendlyAppEntityDecorator(appEntity))
                                  .build();
      return createdResponse;
    } catch (WebApplicationException e) {
      throw e;
    } catch (AppAlreadyExistsException e) {
      LOGGER.warn("App with supplied name exists", e);
      throw buildForBadRequest(AppErrorCode.DUPLICATE_APP_NAME.name(), INVALID_APP_NAME_MSG);
    } catch (MaxAppLimitExceededException e) {
      LOGGER.warn("MaxAppLimitExceededException", e);
      throw buildForBadRequest(AppErrorCode.APP_LIMIT_EXCEEDED.name(), APP_LIMIT_REACHED);
    } catch (AppManagementException e) {
      throw build(AppErrorCode.UNKNOWN_APP_EXCEPTION.name(), APP_CREATION_FAILED, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (GCMAPIKeyValidator.GCMAPIKeyValidationException e) {
      LOGGER.warn("Google API key validation failed", e);
      throw buildForBadRequest(AppErrorCode.INVALID_GOOGLE_API_KEY.name(), INVALID_GOOGLE_API_KEY_MSG);
    } catch (Throwable t) {
      LOGGER.warn("Throwable when creating app", t);
      ErrorResponse error = new ErrorResponse();
      error.setCode(WebConstants.STATUS_ERROR);
      error.setMessage(t.getMessage());
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(error)
              .build()
      );
    }
  }

  @Path("{appId}")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONFriendlyAppEntityDecorator updateApp(@PathParam("appId")String appId, AppEntity request) {
    try {
      if (appId == null || appId.isEmpty()) {
        throw buildForBadRequest(AppErrorCode.INVALID_APP_ID.name(), APP_ID_EMPTY_OR_NULL);
      }
      AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
      AppEntity entity = dao.getAppForAppKey(appId);

      if (entity == null) {
        throw build(AppErrorCode.INVALID_APP_ID.name(), APP_NOT_FOUND, Response.Status.NOT_FOUND);
      }

      String appName = request.getName();
      if (appName != null  && !AppHelper.validateAppName(appName)) {
        throw buildForBadRequest(AppErrorCode.INVALID_APP_NAME.name(), APP_NAME_TOO_LONG_MSG);
      }
      AppEntity other = dao.getAppForName(appName, entity.getOwnerId());

      if (other != null && !other.getAppId().equals(appId))  {
        //we would be creating a duplicate
        LOGGER.warn("Supplied appName={} for appId={} will cause a duplicate", appName, appId);
        //there exists another app with the same name.
        throw buildForBadRequest(AppErrorCode.DUPLICATE_APP_NAME.name(), INVALID_APP_NAME_MSG);
      }

      String googleApikey = request.getGoogleAPIKey();
      if (googleApikey != null && !googleApikey.isEmpty()) {
        GCMAPIKeyValidator.validate(googleApikey);
      }

      dao.updateApp(appId, request.getName(), request.getGoogleAPIKey(),request.getGoogleProjectId(),
          request.getApnsCertPassword(), request.getOwnerEmail(), request.getGuestSecret(), request.isApnsCertProduction());
      AppEntity revised = dao.getAppForAppKey(appId);
      return new JSONFriendlyAppEntityDecorator(revised);
    } catch (WebApplicationException e) {
      throw e;
    } catch (GCMAPIKeyValidator.GCMAPIKeyValidationException e) {
      LOGGER.warn("Google API key validation failed", e);
      throw buildForBadRequest(AppErrorCode.INVALID_GOOGLE_API_KEY.name(), INVALID_GOOGLE_API_KEY_MSG);
    } catch (Throwable t) {
      LOGGER.warn("Throwable when updating app", t);
      throw build(WebConstants.STATUS_ERROR, t.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApp(@PathParam("appId") String appId) {
    try {
      if (appId == null || appId.isEmpty()) {
        throw buildForBadRequest(AppErrorCode.INVALID_APP_ID.name(), APP_ID_EMPTY_OR_NULL);
      }
      AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
      AppEntity entity = dao.getAppForAppKey(appId);

      if (entity == null) {
        throw build(AppErrorCode.INVALID_APP_ID.name(), APP_NOT_FOUND, Response.Status.NOT_FOUND);
      }
      Response createdResponse = Response.status(Response.Status.OK)
          .entity(new JSONFriendlyAppEntityDecorator((entity)))
          .build();
      return createdResponse;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      throw build(WebConstants.STATUS_ERROR, t.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteApp(@PathParam("appId") String appId) {
    try {
      if (appId == null || appId.isEmpty()) {
        throw buildForBadRequest(AppErrorCode.INVALID_APP_ID.name(), APP_ID_EMPTY_OR_NULL);
      }
      AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
      AppEntity entity = dao.getAppForAppKey(appId);

      if (entity == null) {
        throw build(AppErrorCode.INVALID_APP_ID.name(), APP_NOT_FOUND, Response.Status.NOT_FOUND);
      }
      MMXAppManager manager = MMXAppManager.getInstance();
      manager.deleteApp(appId);
      Response response = Response.status(Response.Status.OK).build();
      return response;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      throw build(WebConstants.STATUS_ERROR, t.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<JSONFriendlyAppEntityDecorator> getAppList(@QueryParam("appOwner") String appOwner) {
    try {
      MMXConfiguration mmxConfiguration = MMXConfiguration.getConfiguration();
      String accessControlMode = mmxConfiguration.getString(MMXConfigKeys.ADMIN_REST_API_ACCESS_CONTROL_MODE, MMXServerConstants.DEFAULT_ADMIN_REST_API_ACCESS_CONTROL_MODE);
      AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
      List<AppEntity> entityList;
      LOGGER.info("access control mode is set to:{}", accessControlMode);

      if (accessControlMode.equalsIgnoreCase(MMXServerConstants.ADMIN_REST_API_ACCESS_CONTROL_MODE_RELAXED)) {
        LOGGER.info("returning all apps");
        entityList = dao.getAllApps();
      } else {
        if (appOwner == null || appOwner.isEmpty()) {
          throw buildForBadRequest(AppErrorCode.INVALID_APP_OWNER.name(), INVALID_APP_OWNER_ID_MSG);
        }
        entityList = dao.getAppsForOwner(appOwner);
      }
      List<JSONFriendlyAppEntityDecorator> returnList = new ArrayList<JSONFriendlyAppEntityDecorator>(entityList.size());

      for (AppEntity entity : entityList) {
        JSONFriendlyAppEntityDecorator decorated = new JSONFriendlyAppEntityDecorator(entity);
        returnList.add(decorated);
      }
      return returnList;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      throw build(WebConstants.STATUS_ERROR, t.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Path("{appId}/apnscert")
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateAppAPNsCertificateWithPassword(@PathParam("appId") String appId, MultipartFormDataInput input) {
    LOGGER.debug("Executing certificate upload with password");
    LOGGER.info("App ID :" + appId);
    try {
      Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
      List<InputPart> inputParts = uploadForm.get(APNS_CERT_FILE);
      InputStream certificateFileStream = null;
      for (InputPart inputPart : inputParts) {
        certificateFileStream = inputPart.getBody(InputStream.class, null);
      }
      List<InputPart> passwordParts = uploadForm.get(APNS_CERT_PASSWORD);
      // It is OK to have empty password.
      if (passwordParts == null || passwordParts.isEmpty() || passwordParts.get(0)== null) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(ErrorMessages.ERROR_APNS_CERT_PASSWORD_MISSING);
        error.setCode(Integer.toString(ErrorCode.APNS_PASSWORD_MISSING.getCode()));
        throw new WebApplicationException(
            Response
                .status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build()
        );
      }
      String password = passwordParts.get(0).getBodyAsString();
      byte[] buffer = new byte[8*1024];
      int read = -1;
      int totalRead = 0;
      boolean tooBig = false;
      ByteArrayOutputStream bufferStream = new ByteArrayOutputStream(buffer.length);
      while ((read = certificateFileStream.read(buffer, 0, buffer.length)) != -1) {
        if ((totalRead + read) > WebConstants.APNS_CERT_MAX_SIZE) {
          tooBig = true;
          break;
        } else {
          totalRead += read;
          bufferStream.write(buffer, 0, read);
        }
      }
      bufferStream.flush();
      bufferStream.close();
      certificateFileStream.close();
      if (tooBig) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(APNS_CERT_SIZE_TOO_BIG);
        error.setCode(APNS_CERT_SIZE_TOO_BIG_CODE);
        throw new WebApplicationException(
            Response
                .status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build()
        );
      }
      AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
      AppEntity appEntity = dao.getAppForAppKey(appId);
      if (appEntity == null) {
        throw build(AppErrorCode.INVALID_APP_ID.name(), APP_NOT_FOUND, Response.Status.NOT_FOUND);
      }

      byte[] certificate = bufferStream.toByteArray();
      if (password != null) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Validating APNS certificate");
        }
        APNSCertificateValidator.validate(new ByteArrayInputStream(certificate), password.toCharArray());
      }
      String md5String = DigestUtils.md5Hex(certificate);
      LOGGER.info("MD5 for apns cert for is {}", md5String);
      dao.updateAPNsCertificateAndPassword(appId, certificate, password);
      /*
       * if there are any connections established we need to purge them
       */
      APNSConnectionPool connectionPool = APNSConnectionPoolImpl.getInstance();
      LOGGER.info("Clearing open APNS connections");
      connectionPool.remove(appId, appEntity.isApnsCertProduction());
      return Response.ok().status(Response.Status.OK).build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (APNSCertificateValidator.APNSCertificationValidationException e) {
      LOGGER.warn("APNS cert validation exception", e);
      ErrorResponse error = new ErrorResponse();
      error.setMessage(INVALID_APNS_CERTIFICATE);
      error.setCode(AppErrorCode.INVALID_APNS_CERT.name());
      throw new WebApplicationException(
          Response
              .status(Response.Status.BAD_REQUEST)
              .entity(error)
              .build());
    } catch (Throwable t) {
      LOGGER.warn("Bad stuff happened", t);
      ErrorResponse error = new ErrorResponse();
      error.setCode(WebConstants.STATUS_ERROR);
      error.setMessage(t.getMessage());
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(error)
              .build()
      );
    }
  }
  /**
   * Delete the apns certificate for an appId.
   * @return
   */
  @Path("{appId}/apnscert")
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteAPNsCertificate(@PathParam("appId") String appId) {
    if (appId == null || appId.length() == 0) {
      throw buildForBadRequest(AppErrorCode.INVALID_APP_ID.name(), APP_ID_EMPTY_OR_NULL);
    }
    AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
    AppEntity appEntity = dao.getAppForAppKey(appId);

    if (appEntity == null) {
      throw build(AppErrorCode.INVALID_APP_ID.name(), APP_NOT_FOUND, Response.Status.NOT_FOUND);
    }
    try {
      dao.clearAPNsCertificateAndPassword(appId);
      APNSConnectionPool connectionPool = APNSConnectionPoolImpl.getInstance();
      connectionPool.remove(appId, appEntity.isApnsCertProduction());
      return Response.ok().status(Response.Status.OK).build();
    } catch (Throwable t) {
      LOGGER.warn("Throwable when deleting APNs certificate", t);
      ErrorResponse error = new ErrorResponse();
      error.setCode(WebConstants.STATUS_ERROR);
      error.setMessage(t.getMessage());
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(error)
              .build()
      );
    }
  }

  @GET
  @Path("{"+ APP_ID_KEY +"}/configurations")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAppConfig(@PathParam(APP_ID_KEY) String appId) {
    try {
      if (appId == null || appId.isEmpty()) {
        throw buildForBadRequest(AppErrorCode.INVALID_APP_ID.name(), APP_ID_EMPTY_OR_NULL);
      }
      AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
      AppEntity entity = dao.getAppForAppKey(appId);

      if (entity == null) {
        throw build(AppErrorCode.INVALID_APP_ID.name(), APP_NOT_FOUND, Response.Status.NOT_FOUND);
      }
      AppConfigurationEntityDAO configurationEntityDAO = new AppConfigurationEntityDAOImpl(new OpenFireDBConnectionProvider());
      List<AppConfigurationEntity> configEntityList = configurationEntityDAO.getConfigurations(appId);

      List<AppConfig> configList = new LinkedList<AppConfig>();
      for (AppConfigurationEntity centity : configEntityList) {
        AppConfig config = AppConfig.from(centity);
        configList.add(config);
      }
      Response response = Response.status(Response.Status.OK)
          .entity(configList)
          .build();
      return response;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      throw build(WebConstants.STATUS_ERROR, t.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("{"+ APP_ID_KEY +"}/configurations")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateAppConfig(@PathParam(APP_ID_KEY) String appId, List<AppConfig> configs) {
    try {
      if (appId == null || appId.isEmpty()) {
        throw buildForBadRequest(AppErrorCode.INVALID_APP_ID.name(), APP_ID_EMPTY_OR_NULL);
      }
      AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
      AppEntity entity = dao.getAppForAppKey(appId);

      if (entity == null) {
        throw build(AppErrorCode.INVALID_APP_ID.name(), APP_NOT_FOUND, Response.Status.NOT_FOUND);
      }

      if (configs == null || configs.isEmpty()) {
        throw buildForBadRequest(AppErrorCode.NULL_OR_EMPTY_CONFIG_LIST.name(), ErrorMessages.ERROR_CONFIG_LIST_NULL_OR_EMPTY);
      }
      LOGGER.info("Update configs for appId:{}", appId);
      AppConfigurationCache cache = AppConfigurationCache.getInstance();
      AppConfigurationEntityDAO configurationEntityDAO = new AppConfigurationEntityDAOImpl(new OpenFireDBConnectionProvider());
      // validate configurations
      for (AppConfig config : configs) {
        String key = config.getKey();
        String value = config.getValue();
        if (key == null || key.isEmpty()) {
          throw buildForBadRequest(AppErrorCode.INVALID_CONFIG_KEY.name(), ErrorMessages.ERROR_CONFIG_BAD_KEY);
        }
        if (value == null) {
          throw buildForBadRequest(AppErrorCode.NULL_CONFIG_VALUE.name(), ErrorMessages.ERROR_CONFIG_BAD_VALUE);
        }

        if (key.equals(MMXConfigKeys.WAKEUP_MUTE_PERIOD_MINUTES)) {
          boolean valid = validateWakeupMutePeriod(value);
          if (!valid) {
            String template = "Value supplied for key:%s is invalid.";
            String message = String.format(template, key);
            throw buildForBadRequest(AppErrorCode.INVALID_CONFIG_VALUE.name(),message);
          }
        }
      }
      for (AppConfig config : configs) {
        String key = config.getKey();
        String value = config.getValue();
        configurationEntityDAO.updateConfiguration(appId, key, value);
        cache.clear(appId, key);
      }
      Response response = Response.status(Response.Status.OK)
          .build();
      return response;
    } catch (WebApplicationException e) {
      LOGGER.info("Web Application exception", e);
      throw e;
    } catch (Throwable t) {
      LOGGER.info("Throwable", t);
      throw build(WebConstants.STATUS_ERROR, t.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private WebApplicationException buildForBadRequest (String code, String message) {
    ErrorResponse error = new ErrorResponse();
    error.setCode(code);
    error.setMessage(message);
    return new WebApplicationException(
        Response
            .status(Response.Status.BAD_REQUEST)
            .entity(error)
            .build());
  }

  private WebApplicationException build (String code, String message, Response.Status status) {
    ErrorResponse error = new ErrorResponse();
    error.setCode(code);
    error.setMessage(message);
    return new WebApplicationException(
        Response
            .status(status)
            .entity(error)
            .build());
  }

  private enum AppErrorCode {
    INVALID_APP_NAME,
    INVALID_APP_OWNER,
    DUPLICATE_APP_NAME,
    APP_LIMIT_EXCEEDED,
    UNKNOWN_APP_EXCEPTION,
    INVALID_APP_ID,
    INVALID_APNS_CERT,
    INVALID_GOOGLE_API_KEY,
    NULL_OR_EMPTY_CONFIG_LIST,
    INVALID_CONFIG_KEY,
    NULL_CONFIG_VALUE,
    INVALID_CONFIG_VALUE
    ;
  }

  /**
   * This wraps AppCreate.Response and returns the dates as ISO8601 formatted strings.
   * This is to get around the JSON serializing issue where by default the json library
   * is converting dates to unix time stamps.
   */
  protected static class JSONFriendlyAppCreateResponseDecorator {
    private final AppCreate.Response response;

    public JSONFriendlyAppCreateResponseDecorator(AppCreate.Response response) {
      this.response = response;
    }
    public String getAppId() {
      return response.getAppId();
    }
    public String getApiKey() {
      return response.getApiKey();
    }

    public String getGuestSecret() {
      return response.getGuestSecret();
    }

    public String getCreationDate() {
      String rv = null;
      if (response.getCreationDate() != null) {
        rv = iso8601DateFormat.format(response.getCreationDate());
      }
      return rv;
    }
    public String getModificationDate() {
      String rv = null;
      if (response.getModificationDate() != null) {
        rv = iso8601DateFormat.format(response.getModificationDate());
      }
      return rv;
    }
  }

  private boolean validateWakeupMutePeriod (String value) {
    try {
      int minutes = Integer.parseInt(value);
      if (minutes < 0) {
        return false;
      }
      return true;
    } catch (NumberFormatException e) {
      LOGGER.info("Invalid wakeup mute period:{}" , value);
      return false;
    }
  }

}

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
package com.magnet.mmx.server.plugin.mmxmgmt.db;

import com.magnet.mmx.server.common.data.AppEntity;

import java.util.List;

/**
 * DAO interface for app records
 */
public interface AppDAO {

  /**
   * Get the AppEntity for the supplied appId.
   * @param appId
   * @return null if no app exists for this id
   */
  public AppEntity getAppForAppKey (String appId);

  /**
   * Get AppEntity for the supplied api key
   * @param apiKey
   * @return
   */
  public AppEntity getAppUsingAPIKey (String apiKey);

  public void persist(AppEntity appEntity) throws AppAlreadyExistsException;

  /**
   * Create the App in the database
   * @param serverUserId
   * @param appName
   * @param appId
   * @param apiKey
   * @param googleApiKey
   * @param googleProjectId
   * @param apnsPwd
   * @param ownerId
   * @param apnsProductionEnvironment
   * @return
   * @throws AppAlreadyExistsException
   * @throws DbInteractionException
   */
  public AppEntity createApp(String serverUserId, String appName,
                             String appId, String apiKey, String googleApiKey,
                             String googleProjectId,
                             String apnsPwd, String ownerId, String ownerEmail, String guestSecret, boolean apnsProductionEnvironment)
                                 throws AppAlreadyExistsException, DbInteractionException;

  /**
   * Delete an app with the specified id
   * @param appId
   */
  public void deleteApp (String appId);

  public List<String> getAllAppIds(String ownerId);

  /**
   * Get a list of Apps for a particular owner.
   * @param owner owner is matched exactly.
   * @return
   */
  public List<AppEntity> getAppsForOwner(String owner);

  /**
   * Get AppEntity for the given appName and the ownerId. The appName is compared in case insensitive manner.
   * @param appName
   * @param ownerId
   * @return AppEntity if one exists or null if no app with that name exists.
   */
  public AppEntity getAppForName(String appName, String ownerId);

  public String getOwnerEmailForApp(String appId);

  /**
   * Update the apns certificate for the supplied appId
   * @param appId
   * @param certificate
   */
  public void updateAPNsCertificate(String appId, byte[] certificate);

  /**
   * Update the apns certificate and password for the supplied appId
   * @param appId
   * @param certificate
   * @param password
   */
  public void updateAPNsCertificateAndPassword(String appId, byte[] certificate, String password);

  /**
   * Clear/remove the APNs certificate for the supplied appId.
   * @param appId
   */
  public void clearAPNsCertificate(String appId);

  /**
   * Clear/remove the APNs certificate and the password for the supplied appId.
   * @param appId
   */
  public void clearAPNsCertificateAndPassword(String appId);
  /**
   * Update app information.
   * @param appId
   * @param appName - name of the app. can be null if app name is not being updated
   * @param googleApiKey - google api key for the app. can be null if not being updated
   * @param googleProjectId - google project id for the app. can be null if not being updated.
   * @param apnsCertPwd - apns certificate password. can be null if not being updated.
   * @param productionApnsCert
   * @throws AppDoesntExistException
   */
  public void updateApp(String appId, String appName, String googleApiKey,
                        String googleProjectId, String apnsCertPwd, String ownerEmail, String guestSecret, boolean productionApnsCert)
      throws AppDoesntExistException;


  /**
   * Get a list of all apps currently defined in the database.
   * @return
   */
  public List<AppEntity> getAllApps();

}

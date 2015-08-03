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

import java.util.Date;

/**
*/
class JSONFriendlyAppEntityDecorator {

  private AppEntity entity;

  public JSONFriendlyAppEntityDecorator() {
  }

  public JSONFriendlyAppEntityDecorator(AppEntity entity) {
    this.entity = entity;
  }

  public String getISO8601CreationDate() {
    Date creationDate = entity.getCreationDate();
    String rv = null;
    if (creationDate != null) {
      rv = AppResource.iso8601DateFormat.format(creationDate);
    }
    return rv;
  }

  public String getISO8601Modificationdate() {
    Date modDate = entity.getModificationdate();
    String rv = null;
    if (modDate != null) {
      rv = AppResource.iso8601DateFormat.format(modDate);
    }
    return rv;
  }

  public String getGuestSecret() {
    return entity.getGuestSecret();
  }


  public int getId() {
    return entity.getId();
  }

  public String getName() {
    return entity.getName();
  }

  public String getAppId() {
    return entity.getAppId();
  }

  public String getAppAPIKey() {
    return entity.getAppAPIKey();
  }

  public String getGoogleAPIKey() {
    return entity.getGoogleAPIKey();
  }

  public String getGoogleProjectId() {
    return entity.getGoogleProjectId();
  }

  public String getApnsCertPassword() {
    return entity.getApnsCertPassword();
  }

  public Date getCreationDate() {
    return entity.getCreationDate();
  }

  public Date getModificationdate() {
    return entity.getModificationdate();
  }

  public String getServerUserId() {
    return entity.getServerUserId();
  }

  public String getOwnerId() {
    return entity.getOwnerId();
  }

  public String getOwnerEmail() {
    return entity.getOwnerEmail();
  }

  public boolean isApnsCertProduction() {
    return entity.isApnsCertProduction();
  }

  public boolean isApnsCertUploaded() {
    return entity.getApnsCert() != null;
  }
}

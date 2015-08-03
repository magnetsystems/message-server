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

package com.magnet.mmx.server.plugin.mmxmgmt.handler;

import com.magnet.mmx.protocol.APNS;
import com.magnet.mmx.protocol.AppCreate;
import com.magnet.mmx.protocol.AppDelete;
import com.magnet.mmx.protocol.AppRead;
import com.magnet.mmx.protocol.AppUpdate;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.GCM;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MyAppsRead;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppAlreadyExistsException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDoesntExistException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.IQUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.AuthFactory;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class AppHandler extends IQHandler {

  private static final Logger Log = LoggerFactory.getLogger(AppHandler.class);

  private MMXAppManager mmxAppManager = MMXAppManager.getInstance();

  public AppHandler(String moduleName) {
    super(moduleName);
  }

  @Override
  public IQHandlerInfo getInfo() {
    return new IQHandlerInfo(Constants.MMX_APP_REG, Constants.MMX_NS_APP);
  }

  @Override
  public IQ handleIQ(IQ iq) throws UnauthorizedException {
    Log.info("AppHandler.handleIQ called");
    String command = IQUtils.getCommand(iq);
    if ("create".equals(command)) {
      return handleCreateApp(iq);
    } else if ("read".equals(command)) {
      return handleReadApps(iq);
    } else if ("readMine".equals(command)) {
      return handleReadMyApps(iq);
    } else if ("update".equals(command)) {
      return handleUpdateApp(iq);
    } else if ("delete".equals(command)) {
      return handleDeleteApp(iq);
    }

    return IQUtils.createErrorIQ(iq, "Unsupported operation " + command, 1);
  }

  /**
   * <pre>
   *  <iq type="set" from="admin@jabber-domain/resource" ...>
   *    <reg xlms="com.magnet:mmx:app" command="create" type="application/json" ...>
   *    {
   *      "appName": "...",
   *      "gcm":
   *      {
   *        "googleApiKey" : "...",
   *        "googleProjectId" : "..."
   *      },
   *      "apns":
   *      {
   *        "cert" : "...",
   *        "pwd" : "..."
   *      }
   *    }
   *    </reg>
   *  </iq>
   *
   *  <iq type="result"...>
   *    <reg xlms="com.magnet:mmx:app" ctype="application/json" ...>
   *    {
   *      "appKey": "...",
   *      "apiKey": "...",
   *      "serverUser": "...",
   *      "serverKey": "..."
   *    }
   *    </reg>
   *  </iq>
   * </pre>
   */
  private IQ handleCreateApp(IQ iq) throws UnauthorizedException {
    Element element = iq.getChildElement();
    JID from = iq.getFrom();

    if (!Helper.isAppMgmtPermitted(from)) {
      Log.warn(from + " has no admin privilege to register application");
      throw new UnauthorizedException("No admin privilege to register application");
    }

    // Register for server push notification and generate app-key/api-key.
    String payload = element.getText();
    AppCreate.Request appRqt = AppCreate.Request.fromJson(payload);

    APNS apns = appRqt.getApns();
    String apnsCertInBase64 = null;
    String apnsPwd = null;
    String apnsEncryptedPwd = null;
    if (apns != null) {
      apnsCertInBase64 = apns.getCert();
      apnsPwd = apns.getPwd();
      apnsEncryptedPwd = AuthFactory.encryptPassword(apnsPwd);
    }

    GCM gcm = appRqt.getGcm();
    String googleApiKey = null;
    String googleProjectId = null;
    if (gcm != null) {
      googleApiKey = appRqt.getGcm().getGoogleApiKey();
      googleProjectId = appRqt.getGcm().getGoogleProjectId();
    }

    AppCreate.Response appResp = null;
    try {
      appResp = mmxAppManager.createApp(appRqt.getAppName(),
          appRqt.getServerUserId(), appRqt.getServerUserKey(),
          appRqt.getGuestSecret(), googleApiKey,
          googleProjectId, apnsPwd, (appRqt.getOwnerId() == null ?
              from.getNode() : appRqt.getOwnerId()),
          appRqt.getOwnerEmail(), false);
    } catch (AppAlreadyExistsException e) {
      Log.warn(e.getMessage(), e);
      return IQUtils.createErrorIQ(iq, e.getMessage(), StatusCode.CONFLICT);
    } catch (Exception e) {
      Log.error(e.getMessage(), e);
      return IQUtils.createErrorIQ(iq, e.getMessage(), StatusCode.INTERNAL_ERROR);
    }

    IQ response = IQUtils.createResultIQ(iq, appResp.toJson());

    Log.debug("Sending back: " + response);

    return response;
  }

  /**
   * <pre>
   *  <iq type="set" from="admin@jabber-domain/resource" ...>
   *    <reg xlms="com.magnet:mmx:app" command="read" type="application/json" ...>
   *    {
   *      "appKey": "...",
   *    }
   *    </reg>
   *  </iq>
   *
   *  <iq type="result"...>
   *    <reg xlms="com.magnet:mmx:app" ctype="application/json" ...>
   *    {
   *      "appName": "...",
   *      "apiKey": "...",
   *      "serverUser": "...",
   *      "serverKey": "...",
   *      "gcm":
   *      {
   *        "googleApiKey" : "...",
   *        "googleProjectId" : "..."
   *      },
   *      "apns":
   *      {
   *        "pwd" : "..."
   *      }
   *    }
   *    </reg>
   *  </iq>
   * </pre>
   */
  private IQ handleReadApps(IQ iq) throws UnauthorizedException {
    Element element = iq.getChildElement();

    JID from = iq.getFrom();
    if (Helper.isAppMgmtPermitted(from)) {
      throw new UnauthorizedException("No admin privilege to register application");
    }

    String payload = element.getText();
    AppRead.Request appRqt = AppRead.Request.fromJson(payload);

    AppRead.Response appResp = null;
    try {
      appResp = mmxAppManager.getApp(appRqt.getAppId());
    } catch (AppDoesntExistException e) {
      return IQUtils.createErrorIQ(iq, e.getMessage(), 1);
    }

    // TODO: How do we set the serverKey?

    IQ response = IQUtils.createResultIQ(iq, appResp.toJson());

    return response;
  }

  private IQ handleReadMyApps(IQ iq) throws UnauthorizedException {
    Element element = iq.getChildElement();

    JID from = iq.getFrom();
    if (!Helper.isAppMgmtPermitted(from)) {
      throw new UnauthorizedException("No admin privilege to register application");
    }

    String payload = element.getText();
    MyAppsRead.Request appRqt = MyAppsRead.Request.fromJson(payload);

    MyAppsRead.Response appResp = new MyAppsRead.Response();
    try {
      AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
      List<AppEntity> list = appDAO.getAppsForOwner(from.getNode());
      for (AppEntity e : list) {
        AppRead.Response response = new AppRead.Response();
        response.setAppName(e.getName());
        response.setAppId(e.getAppId());
        response.setOwnerId(e.getOwnerId());
        response.setOwnerEmail(e.getOwnerEmail());
        response.setApiKey(e.getAppAPIKey());
        response.setGuestUserSecret(e.getGuestSecret());
        response.setServerUserId(e.getServerUserId());
        appResp.addResponse(response);
      }
    } catch (Throwable t) {
      return IQUtils.createErrorIQ(iq, t.getMessage(), 1);
    }

    IQ response = IQUtils.createResultIQ(iq, appResp.toJson());

    return response;
  }

  /**
   * <pre>
   *  <iq type="set" from="admin@jabber-domain/resource" ...>
   *    <reg xlms="com.magnet:mmx:app" command="update" type="application/json" ...>
   *    {
   *      "appKey": "...",
   *      "appName": "...",
   *      "gcm":
   *      {
   *        "googleApiKey" : "...",
   *        "googleProjectId" : "..."
   *      },
   *      "apns":
   *      {
   *        "cert" : "...",
   *        "pwd" : "..."
   *      }
   *    }
   *    </reg>
   *  </iq>
   *
   *  <iq type="result"...>
   *    <reg xlms="com.magnet:mmx:app" ctype="application/json" ... />
   *  </iq>
   * </pre>
   */
  private IQ handleUpdateApp(IQ iq) throws UnauthorizedException {
    Element element = iq.getChildElement();

    JID from = iq.getFrom();
    if (!Helper.isAppMgmtPermitted(from)) {
      throw new UnauthorizedException("No admin privilege to register application");
    }

    String payload = element.getText();
    AppUpdate appUpdateRqt = AppUpdate.fromJson(payload);

    String apnsCertInBase64 = appUpdateRqt.getApns().getCert();
    /*String apnsCertPath = null;
		if (appUpdateRqt.getApns() != null && apnsCertInBase64 != null) {
			// Save APNS cert to file system.
			try {
				apnsCertPath = databaseHandler.getApnsCertPath(appUpdateRqt.getAppId());
			} catch (AppDoesntExistException e) {
				return IQUtils.createError(iq, Constants.MMX_APP_REG,
            Constants.MMX_NS_APP, e.getMessage(), 1);
			}
			if (apnsCertPath == null) {
				// No APNS cert was stored before. Create one.
				try {
					String userName = databaseHandler.getServerUser(appUpdateRqt.getAppId());
					XMPPServer server = XMPPServer.getInstance();
					User user = server.getUserManager().getUser(userName);
					apnsCertPath = saveAPNSCertForApp(user, apnsCertInBase64);
				} catch (AppDoesntExistException | UserNotFoundException e) {
					return IQUtils.createError(iq, Constants.MMX_APP_REG,
              Constants.MMX_NS_APP, e.getMessage(), 1);
				}
			} else {
				updateAPNSCertForApp(apnsCertPath, apnsCertInBase64);
			}
		}*/

    AppRead.Response appReadResp = null;
    MMXStatus appResp = null;
    try {
      mmxAppManager.updateApp(appUpdateRqt.getAppId(), appUpdateRqt.getAppName(),
          appUpdateRqt.getGcm().getGoogleApiKey(), appUpdateRqt.getGcm().getGoogleProjectId(),
          apnsCertInBase64, appUpdateRqt.getApns().getPwd());
    } catch (AppDoesntExistException e) {
      return IQUtils.createErrorIQ(iq, e.getMessage(), StatusCode.NOT_FOUND);
    }

    IQ response = IQUtils.createResultIQ(iq, appResp.toJson());

    return response;
  }


  /**
   * <pre>
   *  <iq type="set" from="admin@jabber-domain/resource" ...>
   *    <reg xlms="com.magnet:mmx:app" command="delete" type="application/json" ...>
   *    {
   *      "app-key": "..."
   *    }
   *    </reg>
   *  </iq>
   *
   *  <iq type="result"...>
   *    <reg xlms="com.magnet:mmx:app" ctype="application/json" ... />
   *  </iq>
   * </pre>
   */
  private IQ handleDeleteApp(IQ iq) throws UnauthorizedException {
    XMPPServer server = XMPPServer.getInstance();
    Element element = iq.getChildElement();

    JID from = iq.getFrom();
    if (!Helper.isAppMgmtPermitted(from)) {
      throw new UnauthorizedException("No admin privilege to register application");
    }

    String payload = element.getText();
    AppDelete.Request appDeleteRqt = AppDelete.Request.fromJson(payload);

    MMXStatus appResp = null;
    try {
      mmxAppManager.deleteApp(appDeleteRqt.getAppId());
      appResp = new MMXStatus();
      appResp.setCode(HttpServletResponse.SC_OK);
    } catch (AppDoesntExistException e) {
      return IQUtils.createErrorIQ(iq, e.getMessage(), StatusCode.NOT_FOUND);
    } catch(UserNotFoundException e) {
      return IQUtils.createErrorIQ(iq, e.getMessage(), StatusCode.INTERNAL_ERROR);
    }
    return IQUtils.createResultIQ(iq, appResp.toJson());
  }

}

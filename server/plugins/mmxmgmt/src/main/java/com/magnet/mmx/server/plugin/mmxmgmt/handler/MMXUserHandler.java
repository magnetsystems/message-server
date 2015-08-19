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

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Constants.UserCommand;
import com.magnet.mmx.protocol.Constants.UserCreateMode;
import com.magnet.mmx.protocol.MMXAttribute;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.TagSearch;
import com.magnet.mmx.protocol.UserCreate;
import com.magnet.mmx.protocol.UserId;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.UserQuery;
import com.magnet.mmx.protocol.UserReset;
import com.magnet.mmx.protocol.UserTags;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.UserEntityPostProcessor;
import com.magnet.mmx.server.plugin.mmxmgmt.search.user.UserSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.IQUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import com.magnet.mmx.util.AppHelper;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.Utils;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.session.LocalClientSession;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * IQHandler that creates an user account.
 * In the IQ message, the client provides an username of the format:
 *
 *         <shortUserName>@<AppKey>
 *
 * An account will be created in Openfire with the full username, if the app is found with the provided appKey.
 * If the app is not found, then it will respond with an error.
 *
 */
public class MMXUserHandler extends IQHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MMXUserHandler.class) ;
  private static final int MAX_USERID_SIZE = 64;

  //private DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
  //private MMXUserManager userManager = null;


  public MMXUserHandler(String name) {
    super(name);
  }

  @Override
  public IQ handleIQ(IQ packet) throws UnauthorizedException {
    LOGGER.info("MMXUserHandler.handleIQ called");
    JID from = packet.getFrom();
    String command = IQUtils.getCommand(packet);
    try {
      UserCommand cmd = UserCommand.valueOf(command);
      LocalClientSession session = (LocalClientSession) sessionManager.getSession(from);
      // Session must be authenticated for delete/query/update, and must not be
      // closed for create/reset.
      if (session == null || session.getStatus() == Session.STATUS_CLOSED ||
          (session.getStatus() == Session.STATUS_CONNECTED &&
           (cmd != UserCommand.create && cmd != UserCommand.reset))) {
        IQ reply = IQUtils.createErrorIQ(packet, PacketError.Condition.not_authorized);
        if (session == null || packet.getFrom() != null) {
          return reply;
        } else {
          reply.setTo(session.getAddress());
          reply.setFrom((JID) null);
          session.process(reply);
          return null;
        }
      }
      

      JID fromJID;
      String appId;
      Element element = packet.getChildElement();
      String payload = element.getText();
      // These two commands have no "from".
      if (cmd != UserCommand.create && cmd != UserCommand.reset) {
        fromJID = packet.getFrom();
        appId = JIDUtil.getAppId(fromJID);
      } else {
        fromJID = null;
        appId = null;
      }
      
      switch(cmd) {
      case list:
        return handleListUsers(packet, fromJID, appId, payload);
      case get:
        return handleGetUser(packet, fromJID, appId, payload);
      case search:
        return handleSearchUser(packet, fromJID, appId, payload);
      case query:
        return handleQueryUser(packet, fromJID, appId, payload);
      case update:
        return handleUpdateUser(packet, fromJID, appId, payload);
      case delete:
        return handleDeleteUser(packet, fromJID, appId, payload);
      case getTags:
        return handleGetTags(packet, fromJID, appId, payload);
      case setTags:
        return handleSetTags(packet, fromJID, appId, payload);
      case addTags:
        return handleAddTags(packet, fromJID, appId, payload);
      case removeTags:
        return handleRemoveTags(packet, fromJID, appId, payload);
      case searchByTags:
        return handleSearchByTags(packet, fromJID, appId, payload);
      case create:
        IQ reply = handleCreateUser(packet, payload);
        // There was no "from"; send the reply directly to the client session
        reply.setTo(session.getAddress());
        reply.setFrom((JID) null);
        session.process(reply);
        return null;
//    case reset:
//      IQ reply = handleResetPassword(packet, payload);
//      // there was no "from"; send the reply directly to the client session
//      reply.setTo(session.getAddress());
//      reply.setFrom((JID) null);
//      return null;
      }
    } catch (IllegalArgumentException e) {
      LOGGER.info("Invalid user command string:" + command, e);
      return IQUtils.createErrorIQ(packet,
          e.getMessage(), StatusCode.BAD_REQUEST);
    }
    return IQUtils.createErrorIQ(packet,
        UserOperationStatusCode.INVALID_COMMAND.getMessage(),
        UserOperationStatusCode.INVALID_COMMAND.getCode());
  }

  /*private*/ IQ handleCreateUser(IQ packet, String payload) throws UnauthorizedException {
    UserCreate userRqt = UserCreate.fromJson(payload);
    AppDAO appDAO = new AppDAOImpl(getConnectionProvider());

    IQ validationError = validateUserCreateRequest(packet, userRqt, appDAO);
    if (validationError != null) {
      return validationError;
    }
    String displayName = userRqt.getDisplayName();
    String userId = userRqt.getUserId();
    String apiKey = userRqt.getApiKey();
    AppEntity appEntity = appDAO.getAppUsingAPIKey(apiKey);

    UserCreateMode createMode = userRqt.getCreateMode();

    String userPassword = userRqt.getPassword();
    if (userPassword == null && UserCreateMode.GUEST == createMode) {
      // create the password for guess access
      userPassword = AppHelper.generateRandomPositiveKey();
    }
    // The userID must be escaped first.
    String userName = JIDUtil.makeNode(userId, appEntity.getAppId());
    String email = userRqt.getEmail();

    Map<String, String> extras = userRqt.getExtras();
    if (extras == null) {
      extras = new HashMap<String, String>();
    }
    // "anonymous" users get marked as "guest" in the creation mode.
    if (UserCreateMode.GUEST == createMode) {
      extras.put(Constants.MMX_PROP_NAME_USER_GUEST_MODE, Constants.MMX_PROP_VALUE_USER_GUEST_TRUE);
    }

    MMXUserManager userManager = getUserManager();
    User newUser = userManager.createUser(userName, userPassword, displayName, email, extras);

    if (UserCreateMode.UPGRADE_USER == createMode) { // mark the current user to remove it
      userManager.markRemoveCurrentGuestUser(packet.getFrom().toBareJID());
    }
    if (newUser != null) {
      // Add the tags (if any) for the new user.
      List<String> tags = userRqt.getTags();
      if (tags != null) {
        TagDAO tagDao = DBUtil.getTagDAO();
        for (String tag : tags) {
          if (!tag.isEmpty()) {
            try {
              tagDao.createUsernameTag(tag, appEntity.getAppId(), userName);
            } catch (Exception e) {
              LOGGER.error("Unable to set a tag to "+userName, e);
            }
          }
        }
      }
      
      LOGGER.info("Created a new user with username:" + userName);
      MMXStatus userResp = new MMXStatus();
      // return the generated password for guest mode
      if (UserCreateMode.GUEST == createMode) {
        userResp.setCode(UserOperationStatusCode.GUEST_USER_CREATED.getCode());
        userResp.setMessage(userPassword);
      } else {
        userResp.setCode(UserOperationStatusCode.USER_CREATED.getCode());
        userResp.setMessage(UserOperationStatusCode.USER_CREATED.getMessage());
      }
      IQ response = IQUtils.createResultIQ(packet, userResp.toJson());
      return response;
    } else {
      return IQUtils.createErrorIQ(packet, UserOperationStatusCode.USER_CREATION_FAILED.getMessage(),
          UserOperationStatusCode.USER_CREATION_FAILED.getCode());
    }
  }

  /**
   * Handle delete user.  The initiator (e.g. app-server user) must be from 
   * the same app.
   * @param packet
   * @return
   * @throws UnauthorizedException
   */
  IQ handleDeleteUser(IQ packet, JID from, String appId, String payload) 
                          throws UnauthorizedException {
    UserCreate userRqt = UserCreate.fromJson(payload);
    AppDAO appDAO = new AppDAOImpl(getConnectionProvider());

    IQ validationError = validateUserDeleteRequest(packet, userRqt, appDAO);
    if (validationError != null) {
      return validationError;
    }
    String userId = userRqt.getUserId();
    String constructedUserId = JIDUtil.makeNode(userId, appId);
    MMXUserManager userManager = getUserManager();
    userManager.deleteUser(constructedUserId);
    LOGGER.info("Deleted a user with userId:" + constructedUserId);
    MMXStatus userResp = new MMXStatus();
    userResp.setCode(UserOperationStatusCode.USER_DELETED.getCode());
    userResp.setMessage(UserOperationStatusCode.USER_DELETED.getMessage());
    IQ response = IQUtils.createResultIQ(packet, userResp.toJson());
    return response;
  }
  
  // TODO: The MMXUserManager.resetPassword() is not implemented.
//  IQ handleResetPassword(IQ packet, String payload)
//                         throws UnauthorizedException {
//    UserReset resetRqt = UserReset.fromJson(payload);
//    AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
//    IQ validationError = validateResetPwdRequest(packet, resetRqt, appDAO);
//    if (validationError != null) {
//      return validationError;
//    }
//    
//    String apiKey = resetRqt.getApiKey();
//    String userId = resetRqt.getUsername();
//    AppEntity appEntity = appDAO.getAppUsingAPIKey(apiKey);
//    if (appEntity == null) {
//      return IQUtils.createErrorIQ(packet,
//          UserOperationStatusCode.INVALID_API_KEY.getMessage(),
//          UserOperationStatusCode.INVALID_API_KEY.getCode());
//    }
//    
//    String xid = userId + JIDUtil.APP_ID_DELIMITER + appEntity.getAppId();
//    MMXUserManager userManager = getUserManager();
//    String sendTo = userManager.resetPassword(xid, null, null, null);
//    if (sendTo == null) {
//      return IQUtils.createErrorIQ(packet,
//          UserOperationStatusCode.UNAUTHORIZED.getMessage(),
//          UserOperationStatusCode.UNAUTHORIZED.getCode());
//    }
//    MMXStatus userResp = new MMXStatus();
//    userResp.setCode(UserOperationStatusCode.PASSWD_RESET.getCode());
//    userResp.setMessage(UserOperationStatusCode.PASSWD_RESET.getMessage()+sendTo);
//    IQ response = IQUtils.createResultIQ(packet, userResp.toJson());
//    return response;
//  }

  IQ handleUpdateUser(IQ packet, JID from, String appId, String payload)
      throws UnauthorizedException {
    UserInfo request = UserInfo.fromJson(payload);
    String userName = from.getNode();
    
    try {
      UserManager userManager = XMPPServer.getInstance().getUserManager();
      User user = userManager.getUser(userName);
      if (user == null) {
        throw new UserNotFoundException();
      }
      if (request.getEmail() != null) {
        user.setEmail(request.getEmail());
      }
      if (request.getDisplayName() != null) {
        user.setName(request.getDisplayName());
      }
    } catch (UserNotFoundException e) {
      return IQUtils.createErrorIQ(packet,
            UserOperationStatusCode.USER_NOT_FOUND.getMessage(),
            UserOperationStatusCode.USER_NOT_FOUND.getCode());
    }

    MMXStatus userResp = new MMXStatus();
    userResp.setCode(UserOperationStatusCode.USER_UPDATED.getCode());
    userResp.setMessage(UserOperationStatusCode.USER_UPDATED.getMessage());
    IQ response = IQUtils.createResultIQ(packet, userResp.toJson());
    return response;
  }
  
  private static class ListOfUserId extends ArrayList<UserId> {
    public ListOfUserId() {
      super();
    }
    
    public ListOfUserId(int size) {
      super(size);
    }
  }
  
  IQ handleListUsers(IQ packet, JID from, String appId, String payload)
      throws UnauthorizedException {
    List<UserId> userIds = GsonData.getGson().fromJson(payload, ListOfUserId.class);
    HashMap<String, UserInfo> map = new HashMap<String, UserInfo>(userIds.size());
    UserManager userManager = XMPPServer.getInstance().getUserManager();
    for (UserId userId : userIds) {
      try {
        String uid = userId.getUserId();
        String userName = JIDUtil.makeNode(uid, appId);
        User user = userManager.getUser(userName);
        map.put(uid, new UserInfo()
          .setUserId(uid))
          .setDisplayName(user.getName())
          .setEmail(user.getEmail());
      } catch (UserNotFoundException e) {
        // Ignored.
      }
    }
    IQ response = IQUtils.createResultIQ(packet, GsonData.getGson().toJson(map));
    return response;
  }
  
  IQ handleGetUser(IQ packet, JID from, String appId, String payload) 
      throws UnauthorizedException {
    String userName;
    UserId userId = UserId.fromJson(payload);
    if (userId == null || userId.getUserId() == null)
      userName = from.getNode();
    else
      userName = JIDUtil.makeNode(userId.getUserId(), appId);
    
    try {
      UserManager userManager = XMPPServer.getInstance().getUserManager();
      User user = userManager.getUser(userName);
      UserInfo accountInfo = new UserInfo()
        .setUserId(userId.getUserId())
        .setDisplayName(user.getName())
        .setEmail(user.getEmail());
      IQ response = IQUtils.createResultIQ(packet, accountInfo.toJson());
      return response;
    } catch (UserNotFoundException e) {
      return IQUtils.createErrorIQ(packet,
          UserOperationStatusCode.USER_NOT_FOUND.getMessage(),
          UserOperationStatusCode.USER_NOT_FOUND.getCode());
    }
  }
  
  IQ handleSearchUser(IQ packet, JID from, String appId, String payload)
                                throws UnauthorizedException {
    UserQuery.SearchRequest rqt = UserQuery.SearchRequest.fromJson(payload);
    IQ validationError = validateUserSearchRequest(packet, rqt);
    if (validationError != null) {
      return validationError;
    }

    UserDAO userDAO = new UserDAOImpl(getConnectionProvider());
    AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
    // get the server user for this app
    String serverUserId = appDAO.getAppForAppKey(appId).getServerUserId();
    String serverUserIdWithoutAppId = Helper.removeSuffix(serverUserId, JIDUtil.APP_ID_DELIMITER);

    int offset = rqt.getOffset();
    int limit = rqt.getLimit();
    UserQuery.Response response = new UserQuery.Response();

    PaginationInfo paginationInfo = PaginationInfo.build(limit, offset);

    UserQueryBuilder builder = new UserQueryBuilder();
    QueryBuilderResult queryBuilderResult = builder.buildQuery(rqt, appId, paginationInfo);

    SearchResult<UserEntity> searchResult = userDAO.getUsersWithPagination(queryBuilderResult, paginationInfo);

    userDAO.getUsers(queryBuilderResult);
    List<UserInfo> userList = new ArrayList<UserInfo>(searchResult.getResults().size());

    UserEntityPostProcessor processor = new UserEntityPostProcessor();
    for (UserEntity ue : searchResult.getResults()) {
      // Strip the app ID and not to filter out server user and anonymous user.
      processor.postProcess(ue);
      UserInfo user = UserBuilder.build(ue);
      userList.add(user);
    }
    response.setUsers(userList);
    response.setTotalCount(searchResult.getTotal());
    return IQUtils.createResultIQ(packet, response.toJson());
  }
  
  /**
   * Handle bulk search
   * @param packet
   * @return
   * @throws UnauthorizedException
   */
  IQ handleQueryUser(IQ packet, JID from, String appId, String payload)
      throws UnauthorizedException {
    UserQuery.BulkSearchRequest rqt = UserQuery.BulkSearchRequest.fromJson(payload);
    IQ validationError = validateUserQueryRequest(packet, rqt);
    if (validationError != null) {
      return validationError;
    }

    UserDAO userDAO = new UserDAOImpl(getConnectionProvider());
    AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
    // get the server user for this app
    String serverUserId = appDAO.getAppForAppKey(appId).getServerUserId();
    String serverUserIdWithoutAppId = Helper.removeSuffix(serverUserId, JIDUtil.APP_ID_DELIMITER);

    // get the bulk list.
    List<MMXAttribute<UserQuery.Type>> clist = rqt.getCriteria();
    UserQuery.Response response = new UserQuery.Response();

    Integer limit = rqt.getLimit();
    boolean unlimited = (limit == null);
    int total = 0;
    // Use the HashMap to avoid duplicated.
    Map<String, UserInfo> userMap = new HashMap<String, UserInfo>();
    // TODO: This looping is not efficient.  It should query by type, then 
    // aggregate the final user list.
    for (MMXAttribute<UserQuery.Type> attr : clist) {
      UserQuery.Type ctype = attr.getType();
      UserSearchOption userSearchOption = null;
      switch (ctype) {
        case email:
          userSearchOption = UserSearchOption.EMAIL;
          break;
        case userId:
          userSearchOption = UserSearchOption.USERNAME;
          break;
        case phone:
          userSearchOption = UserSearchOption.PHONE;
          break;
        case displayName:
          userSearchOption = UserSearchOption.NAME;
          break;
      }
      String value = attr.getValue();
      ValueHolder holder = new ValueHolder();
      holder.setValue1(value);
      PaginationInfo pinfo = null;
      if (!unlimited) {
        pinfo = PaginationInfo.build(limit, Integer.valueOf(0));
      }
      UserSearchResult results = userDAO.searchUsers(appId, userSearchOption, holder, null, pinfo);
      total = total + results.getTotal();
      int size = results.getResults().size();
      UserEntityPostProcessor processor = new UserEntityPostProcessor();
      for (int i = 0; i < size && (unlimited || userMap.size() < limit.intValue()); i++) {
        UserEntity entity = results.getResults().get(i);
        processor.postProcess(entity);
        String userId = entity.getUsername();
        //MOB-922 filter management account and guest account
        if (userId.equalsIgnoreCase(serverUserIdWithoutAppId) || 
            userId.equalsIgnoreCase(appId)) {
          continue;
        }
        userMap.put(userId, UserBuilder.build(entity));
      }
    }
    
    List<UserInfo> userList = new ArrayList<UserInfo>(userMap.values());
    response.setUsers(userList);
    response.setTotalCount(total);
    return IQUtils.createResultIQ(packet, response.toJson());
  }
  
  IQ handleSearchByTags(IQ packet, JID from, String appId, String payload) {    
    TagSearch rqt = TagSearch.fromJson(payload);
    
    // TODO; not implemented
    return IQUtils.createErrorIQ(packet, 
        UserOperationStatusCode.NOT_IMPLEMENTED.getMessage()+"User search by tags", 
        UserOperationStatusCode.NOT_IMPLEMENTED.getCode());
  }
  
  IQ handleGetTags(IQ packet, JID from, String appId, String payload) {
    try {
      User user = UserManager.getInstance().getUser(from.getNode());
      if(user == null) {
        return IQUtils.createErrorIQ(packet, "User does not exist", StatusCode.BAD_REQUEST);
      }
      TagDAO tagDao = DBUtil.getTagDAO();
      List<TagEntity> tagEntities = tagDao.getTagEntitiesForUsername(appId, user.getUsername());
      List<Date> dates = new ArrayList<Date>();
      List<String> tags = new ArrayList<String>();
      for(TagEntity te : tagEntities) {
        tags.add(te.getTagname());
        dates.add(te.getCreationDate());
      }
      Collections.sort(dates);
      UserTags userTags = new UserTags(tags, Utils.isNullOrEmpty(dates) ? null : dates.get(dates.size() - 1));
      return IQUtils.createResultIQ(packet, userTags.toJson());
    } catch (Exception e) {
      LOGGER.error("handleGetTags : caught Exception : {}", e);
      return IQUtils.createErrorIQ(packet, "Unknown Error", StatusCode.BAD_REQUEST);
    }
  }

  IQ handleSetTags(IQ packet, JID from, String appId, String payload) {
    String username = from.getNode();
    UserTags userTags = UserTags.fromJson(payload);

    try {
      User user = UserManager.getInstance().getUser(username);
      if (user != null) {
        TagDAO tagDao = DBUtil.getTagDAO();
        tagDao.deleteAllTagsForUsername(appId, username);
        if(!Utils.isNullOrEmpty(userTags.getTags())) {
          for (String tag : userTags.getTags()) {
            tagDao.createUsernameTag(tag, appId, username);
          }
        }
      }
    } catch (Exception e) {
      return IQUtils.createErrorIQ(packet, "User does not exist", StatusCode.BAD_REQUEST);
    }
    MMXStatus status = new MMXStatus();
    status.setCode(StatusCode.SUCCESS)
            .setMessage("Success");
    return IQUtils.createResultIQ(packet, status.toJson());
  }
  
  IQ handleAddTags(IQ packet, JID from, String appId, String payload) {
    String username = from.getNode();
    UserTags userTags = UserTags.fromJson(payload);

    try {
      User user = UserManager.getInstance().getUser(username);
      if (user != null && userTags != null && !Utils.isNullOrEmpty(userTags.getTags())) {
        for (String tag : userTags.getTags()) {
          TagDAO tagDao = DBUtil.getTagDAO();
          tagDao.createUsernameTag(tag, appId, username);
        }
      }
    } catch (IllegalArgumentException e) {
      return IQUtils.createErrorIQ(packet, e.getMessage(), StatusCode.BAD_REQUEST);
    } catch (Exception e) {
      return IQUtils.createErrorIQ(packet, "User does not exist", StatusCode.BAD_REQUEST);
    }

    MMXStatus status = new MMXStatus();
    status.setCode(StatusCode.SUCCESS)
          .setMessage("Success");
    return IQUtils.createResultIQ(packet, status.toJson());
  }
  
  IQ handleRemoveTags(IQ packet, JID from, String appId, String payload) {
    String username = from.getNode();
    UserTags userTags = UserTags.fromJson(payload);
    
    try {
      User user = UserManager.getInstance().getUser(username);
      if (user != null && userTags != null) {
        if(!Utils.isNullOrEmpty(userTags.getTags())) {
          TagDAO tagDao = DBUtil.getTagDAO();
          tagDao.deleteTagsForUsername(appId, username, userTags.getTags());
        }
      }
    } catch (Exception e) {
      return IQUtils.createErrorIQ(packet, "User does not exist", StatusCode.BAD_REQUEST);
    }

    MMXStatus status = new MMXStatus();
    status.setCode(StatusCode.SUCCESS)
          .setMessage("Success");
    return IQUtils.createResultIQ(packet, status.toJson());

  }

  @Override
  public IQHandlerInfo getInfo() {
    return new IQHandlerInfo("reg", Constants.MMX_NS_USER);
  }

  private IQ validateUserCreateRequest (IQ input, UserCreate request, AppDAO appDAO) {
    String apiKey = request.getApiKey();
    if (null == apiKey || apiKey.isEmpty()) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_API_KEY.getMessage(),
          UserOperationStatusCode.INVALID_API_KEY.getCode());
    }
    AppEntity appEntity = appDAO.getAppUsingAPIKey(apiKey);
    if (appEntity == null) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_API_KEY.getMessage(),
          UserOperationStatusCode.INVALID_API_KEY.getCode());
    }
    String priKey = appEntity.getGuestSecret();
    if (priKey == null || !priKey.equals(request.getPriKey())) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_PRI_KEY.getMessage(),
          UserOperationStatusCode.INVALID_PRI_KEY.getCode());
    }
    String appId = appEntity.getAppId();
    if (appId == null || !appId.equals(request.getAppId())) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_APP_ID.getMessage(),
          UserOperationStatusCode.INVALID_APP_ID.getCode());
    }
    //* validate the userId
    String userId = request.getUserId();
    if (userId == null || userId.isEmpty()) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_USER_ID_KEY.getMessage(),
          UserOperationStatusCode.INVALID_USER_ID_KEY.getCode());
    }
    //check if there are invalid chars in the user name
    boolean hasInvalidChars = JIDUtil.checkUsernameForInvalidCharacters(userId);
    if (hasInvalidChars) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_USER_ID_CHAR.getMessage(),
          UserOperationStatusCode.INVALID_USER_ID_CHAR.getCode());
    }

    //check userId length is not too long
    String constructedUserId = JIDUtil.makeNode(userId, appEntity.getAppId());
    int length = -1;
    try {
      length = constructedUserId.getBytes(Constants.UTF8_CHARSET).length;
    } catch (UnsupportedEncodingException e) {
      LOGGER.warn("UnsupportedEncodingException" , e);
    }
    if (length > MAX_USERID_SIZE) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_USER_ID_TOO_LONG.getMessage(),
          UserOperationStatusCode.INVALID_USER_ID_TOO_LONG.getCode());
    }
    MMXUserManager userManager = getUserManager();
    boolean isTaken = userManager.isUserIdTaken(constructedUserId);
    if (isTaken) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_USER_ID_TAKEN.getMessage(),
          UserOperationStatusCode.INVALID_USER_ID_TAKEN.getCode());
    }
    return null;
  }
  
  private IQ validateResetPwdRequest(IQ input, UserReset request, AppDAO appDAO) {
    String apiKey = request.getApiKey();
    if (null == apiKey || apiKey.isEmpty()) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_API_KEY.getMessage(),
          UserOperationStatusCode.INVALID_API_KEY.getCode());
    }
    AppEntity appEntity = appDAO.getAppUsingAPIKey(apiKey);
    if (appEntity == null) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_API_KEY.getMessage(),
          UserOperationStatusCode.INVALID_API_KEY.getCode());
    }
    String priKey = appEntity.getGuestSecret();
    if (priKey == null || !priKey.equals(request.getPriKey())) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_PRI_KEY.getMessage(),
          UserOperationStatusCode.INVALID_PRI_KEY.getCode());
    }
    //* validate the userId
    String userId = request.getUserId();
    if (userId == null || userId.isEmpty()) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_USER_ID_KEY.getMessage(),
          UserOperationStatusCode.INVALID_USER_ID_KEY.getCode());
    }
    return null;
  }
  
  private IQ validateUserDeleteRequest (IQ input, UserCreate request, AppDAO appDAO) {
    String apiKey = request.getApiKey();
    if (null == apiKey || apiKey.isEmpty()) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_API_KEY.getMessage(),
          UserOperationStatusCode.INVALID_API_KEY.getCode());
    }
    AppEntity appEntity = appDAO.getAppUsingAPIKey(apiKey);
    if (appEntity == null) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_API_KEY.getMessage(),
          UserOperationStatusCode.INVALID_API_KEY.getCode());
    }
    // validate the userId
    String userId = request.getUserId();
    if (userId == null || userId.isEmpty()) {
      return IQUtils.createErrorIQ(input, UserOperationStatusCode.INVALID_USER_ID_KEY.getMessage(),
          UserOperationStatusCode.INVALID_USER_ID_KEY.getCode());
    }
    return null;
  }

  private IQ validateUserQueryRequest (IQ source, UserQuery.BulkSearchRequest request) {
    JID fromJID = source.getFrom();
    String appId = JIDUtil.getAppId(fromJID);
    UserOperationStatusCode code = null;
    if (null == appId || appId.isEmpty()) {
      code = UserOperationStatusCode.INVALID_APP_ID;
      return IQUtils.createErrorIQ(source, code.getMessage(),
          code.getCode());
    }
    if (request == null) {
      code = UserOperationStatusCode.INVALID_REQUEST;
      return IQUtils.createErrorIQ(source, code.getMessage(),
          code.getCode());
    }

    List<MMXAttribute<UserQuery.Type>> criteria = request.getCriteria();
    if (criteria == null || criteria.isEmpty()) {
      code = UserOperationStatusCode.INVALID_QUERY_CRITERION;
      return IQUtils.createErrorIQ(source, code.getMessage(),
          code.getCode());
    }
    return null;
  }

  private IQ validateUserSearchRequest(IQ source, UserQuery.SearchRequest request) {
    JID fromJID = source.getFrom();
    String appId = JIDUtil.getAppId(fromJID);
    UserOperationStatusCode code = null;
    if (null == appId || appId.isEmpty()) {
      code = UserOperationStatusCode.INVALID_APP_ID;
      return IQUtils.createErrorIQ(source, code.getMessage(),
          code.getCode());
    }
    if (request == null) {
      code = UserOperationStatusCode.INVALID_REQUEST;
      return IQUtils.createErrorIQ(source, code.getMessage(),
          code.getCode());
    }

    SearchAction.Operator operator = request.getOperator();
    if (operator == null) {
      code = UserOperationStatusCode.INVALID_OPERATOR;
      return IQUtils.createErrorIQ(source, code.getMessage(),
          code.getCode());
    }
    if (request.getDisplayName() == null && request.getEmail() == null &&
        request.getPhone() == null && request.getTags() == null) {
      code = UserOperationStatusCode.NO_SEARCH_ATTR;
      return IQUtils.createErrorIQ(source, code.getMessage(),
          code.getCode());
    }
    return null;
  }
  /**
   * Enum for the status codes
   */
  public static enum UserOperationStatusCode {
    USER_CREATED (200, "User created"),
    USER_DELETED (200, "User deleted"),
    USER_UPDATED (200, "User updated"),
    GUEST_USER_CREATED (200, "" /* generated password goes here */),
    PASSWD_RESET (200, "Temporary password has been sent to "),
    INVALID_COMMAND (400, "Invalid command"),
    INVALID_APP_ID (400, "Invalid app ID"),
    INVALID_API_KEY (400, "Invalid api key"),
    INVALID_PRI_KEY (400, "Invalid privilege key"),
    INVALID_USER_ID_KEY (500, "Invalid userId"),
    INVALID_USER_ID_TOO_LONG (500, "userId too long"),
    INVALID_USER_ID_TAKEN (500, "userId is taken"),
    INVALID_EMAIL (400, "email is invalid"),
    INVALID_REQUEST (400, "Invalid request"),
    INVALID_QUERY_CRITERION (400, "Invalid query criterion"),
    INVALID_OPERATOR(400, "Invalid operator"),
    NO_SEARCH_ATTR(400, "No search attributes specified"),
    UNAUTHORIZED (400, "Unauthorized to reset password"),
    USER_CREATION_FAILED (500, "user creation failed"),
    USER_NOT_FOUND (403, "User not found"),
    NOT_IMPLEMENTED(501, "Feature not implemented: "),
    INVALID_USER_ID_CHAR(500, "Invalid character specified in userId"),
    ;

    private int code;
    private String message;
    UserOperationStatusCode(int c, String m) {
      code = c;
      message = m;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }
  }

  protected MMXUserManager getUserManager() {
    return new OFMMXUserManagerImpl();
  }

  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }


  private static class UserBuilder {

    public static UserInfo build (UserEntity entity) {
      UserInfo user = new UserInfo();
      user.setEmail(entity.getEmail());
      user.setUserId(entity.getUsername());
      user.setDisplayName(entity.getName());
      //Note: I am not setting the phone number here since that needs
      //additional lookup.
      return user;
    }

  }
}

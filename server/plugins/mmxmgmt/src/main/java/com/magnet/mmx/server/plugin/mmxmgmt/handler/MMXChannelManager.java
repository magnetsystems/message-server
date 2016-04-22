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

import com.magnet.mmx.protocol.*;
import com.magnet.mmx.protocol.ChannelAction.ListType;
import com.magnet.mmx.protocol.ChannelAction.MMXPublishedItem;
import com.magnet.mmx.server.api.v1.protocol.ChannelCreateInfo;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.PubSubPersistenceManagerExt;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.TopicQueryBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicNode;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXChannelUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.AppChannel;
import com.magnet.mmx.util.ChannelHelper;
import com.magnet.mmx.util.TopicHelper;
import com.magnet.mmx.util.Utils;
import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.pubsub.*;
import org.jivesoftware.openfire.pubsub.cluster.RefreshNodeTask;
import org.jivesoftware.openfire.pubsub.models.AccessModel;
import org.jivesoftware.openfire.pubsub.models.PublisherModel;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.forms.DataForm;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class MMXChannelManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(MMXAppManager.class);
  private static final boolean EXCLUDE_USER_CHANNELS = true;
  private static final int MAX_ENTRIES = 100;
  private final XMPPServer mServer = XMPPServer.getInstance();
  private final PubSubService mPubSubModule = mServer.getPubSubModule();
  private final Map<String, JID> mCachedServerUsers = Collections.synchronizedMap(
      new LinkedHashMap<String, JID>(MAX_ENTRIES+1, 0.75f, true) {
        private static final long serialVersionUID = 1L;
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, JID> eldest) {
          return size() > MAX_ENTRIES;
        }
       });

  private static MMXChannelManager sInstance = null;

  public static final String MISSING_APP_ROOT_CHANNEL = "Root channel not found for selected app.";
  public static final String INVALID_CHANNEL_ID = "Supplied channel id is invalid.";
  public static final String INVALID_CHANNEL_NAME = "Supplied channel name is invalid.";
  public static final String DUPLICATE_CHANNEL_ID = "Channel with supplied id exists.";

  protected MMXChannelManager() {
  }

  public static MMXChannelManager getInstance() {
    if (sInstance == null) {
      sInstance = new MMXChannelManager();
    }
    return sInstance;
  }

  List<String> listChannels(String parentNode) {
    Collection<Node> nodes = mPubSubModule.getNodes();
    ArrayList<String> result = new ArrayList<String>();
    for (Node node : nodes) {
      result.add(node.getNodeID());
    }
    return result;
  }

  /**
   * Get a list of Leaf Channel nodes for the passed in appId
   * (collection nodes aren't returned)
   * @param appId
   * @return
   */
  public List<TopicNode> listChannelsForAppId(String appId) {
    ArrayList<TopicNode> result = new ArrayList<TopicNode>();
    CollectionNode rootNode = getRootAppChannel(appId);
    if (rootNode != null) {
      Collection<Node> nodes = rootNode.getNodes();
      for (Node node : nodes) {
        //For fixing: https://magneteng.atlassian.net/browse/MOB-833
        if (node.isCollectionNode()) {
          continue;
        }
        String identifier = node.getNodeID();
        boolean isAppChannel = ChannelHelper.isAppChannel(identifier, appId);
        if (isAppChannel) {
          TopicNode tn = TopicNode.build(appId, node);
          result.add(tn);
        }
      }
    }
    return result;
  }

  /**
   * Get a list of Leaf Channel nodes for the passed in appId
   * (collection nodes aren't returned)
   * @param appId
   * @return
   */
  public List<com.magnet.mmx.server.api.v1.protocol.ChannelInfo> getChannelInfo(String appId) {
    ArrayList<com.magnet.mmx.server.api.v1.protocol.ChannelInfo> result =
        new ArrayList<com.magnet.mmx.server.api.v1.protocol.ChannelInfo>();
    CollectionNode rootNode = getRootAppChannel(appId);
    if (rootNode != null) {
      Collection<Node> nodes = rootNode.getNodes();
      for (Node node : nodes) {
        //For fixing: https://magneteng.atlassian.net/browse/MOB-833
        if (node.isCollectionNode()) {
          continue;
        }
        String identifier = node.getNodeID();
        boolean isAppChannel = ChannelHelper.isAppChannel(identifier, appId);
        if (isAppChannel) {
          com.magnet.mmx.server.api.v1.protocol.ChannelInfo info =
              getChannelInfoFromNode(appId, node);
          result.add(info);
        }
      }
    }
    return result;
  }

  public com.magnet.mmx.server.api.v1.protocol.ChannelInfo getChannelInfoFromNode(
                  String appId, Node node) {
    com.magnet.mmx.server.api.v1.protocol.ChannelInfo info = new
        com.magnet.mmx.server.api.v1.protocol.ChannelInfo();
    info.setChannelId(ChannelHelper.converToId(node.getNodeID()));
    info.setDescription(node.getDescription());
    info.setChannelName(node.getName());
    if(node instanceof  LeafNode) {
      LeafNode lnode = (LeafNode) node;
      info.setSubscriptionEnabled(lnode.isSubscriptionEnabled());
      info.setMaxItems(lnode.isPersistPublishedItems() ?
          lnode.getMaxPublishedItems() : 0);
    }
    info.setChannelId(ChannelHelper.converToId(node.getNodeID()));
    info.setPublisherType(node.getPublisherModel().getName());
    return info;
  }

  /**
   * Get a list of subscriptions for a specific channel
   * @param channelId
   * @return
   */
  public List<NodeSubscription> listSubscriptionsForChannel(String channelId) {
    String lowerCase = channelId.toLowerCase();
    LeafNode node = (LeafNode) mPubSubModule.getNode(lowerCase);
    Collection<NodeSubscription> rv = Collections.emptyList();
    if (node != null) {
      rv = node.getAllSubscriptions();
    }
    ArrayList<NodeSubscription> returnList = new ArrayList<NodeSubscription>(rv);
    return returnList;
  }

  public ChannelActionResult createChannel(AppEntity entity, ChannelCreateInfo channelInfo) {
    ChannelActionResult result = new ChannelActionResult();
    if (channelInfo.getChannelName() == null || channelInfo.getChannelName().isEmpty()) {
      result.setSuccess(false);
      result.setCode(ChannelFailureCode.INVALID_CHANNEL_NAME);
      result.setMessage("Channel id is either null or empty");
      return result;
    }
    if (channelInfo.getChannelName().length() > MMXServerConstants.MAX_TOPIC_NAME_LEN) {
      result.setSuccess(false);
      result.setCode(ChannelFailureCode.INVALID_CHANNEL_ID);
      result.setMessage("Supplied channel id exceeds the permitted maximum length of:" + MMXServerConstants.MAX_TOPIC_NAME_LEN);
      return result;
    }
    String channelName = channelInfo.getChannelName();
    channelName = ChannelHelper.normalizePath(channelName);

    String appId = entity.getAppId();
    String channelId = ChannelHelper.makeChannel(appId, channelInfo.isPrivateChannel() ?
            entity.getServerUserId() : null, channelName);

    /**
     * Get the appRootNode
     */
    CollectionNode appRootNode = getRootAppChannel(appId);
    if (appRootNode == null) {
      result = new ChannelActionResult();
      result.setSuccess(false);
      result.setMessage(MISSING_APP_ROOT_CHANNEL);
      result.setCode(ChannelFailureCode.NOTFOUND);
      return result;
    }
    // create the requested channel
    Node newAppChannel = null;

    try {
      newAppChannel = createLeafNode(JIDUtil.makeNode(entity.getServerUserId(), appId),
          channelId, appRootNode, channelInfo);
      result.setSuccess(true);
      result.setNode(TopicNode.build(appId, newAppChannel));
      // Add role mappings
      List<String> roles = channelInfo.getRoles();
      if (roles == null || roles.isEmpty()) {
        roles = Collections.singletonList(MMXServerConstants.TOPIC_ROLE_PUBLIC);
      }
      TopicRoleDAO roleDAO = getTopicRoleDAO();
      roleDAO.addTopicRoles("pubsub", newAppChannel.getNodeID(), roles);
    } catch (NotAcceptableException e) {
      result.setSuccess(false);
      result.setCode(ChannelFailureCode.UNKNOWN);
      result.setMessage(e.getMessage());
    } catch (ChannelExistsException e) {
      result.setSuccess(false);
      result.setCode(ChannelFailureCode.DUPLICATE);
      result.setMessage(DUPLICATE_CHANNEL_ID);
    }
    return result;
  }
  /**
   * Delete a channel identified by a channel id.
   * @param appId The app ID for error message.
   * @param channelId The node ID.
   * @return
   */
  public ChannelActionResult deleteChannel (String appId, String channelId) {

    ChannelActionResult result = new ChannelActionResult();

    Node gonner = mPubSubModule.getNode(channelId);

    if (gonner == null) {
      result.setSuccess(false);
      result.setCode(ChannelFailureCode.NOTFOUND);
      result.setMessage(INVALID_CHANNEL_ID);
    } else {
      LOGGER.trace("Deleting channel with id:" + channelId);
      gonner.delete();
      result.setSuccess(true);
      result.setNode(TopicNode.build(appId, gonner));
      result.setMessage(INVALID_CHANNEL_ID);
    }
    return result;
  }

  /**
   * Get the root collection node for an app
   *
   * @param appId
   * @return
   */
  public CollectionNode getRootAppChannel(String appId) {
    Node result = mPubSubModule.getNode(appId);
    if (result != null && result.isCollectionNode()) {
      return (CollectionNode) result;
    } else {
      return null;
    }
  }

  /**
   * Get a Node representing the channel with the specified channelId.
   * @param channelId
   * @return Node if one exists false other wise.
   */
  public Node getTopicNode (String channelId){
    Node result = mPubSubModule.getNode(channelId);
    return result;
  }

  // creatorUsername is "userID%appID"
  CollectionNode createCollectionNode(String creatorUsername, String nodeId, CollectionNode parentNode) {
    CollectionNode parent = (parentNode == null) ? mPubSubModule.getRootCollectionNode() : parentNode;
    Node result = mPubSubModule.getNode(nodeId);
    if (result != null && !result.isCollectionNode()) {
      result.delete();
      result = null;
      // TODO cleanup existing published Items for this node
    }

    if (result == null) {
      ConfigureForm form = new ConfigureForm(DataForm.Type.submit);
      form.setSendItemSubscribe(true);
      form.setDeliverPayloads(true);
      form.setNotifyRetract(false);
      form.setNotifyDelete(false);
      form.setNotifyConfig(false);
      form.setNodeType(ConfigureForm.NodeType.collection);
      form.setPublishModel(ConfigureForm.PublishModel.open);
      LOGGER.trace("Collection config form: " + form);

      JID jid = new JID(creatorUsername, mServer.getServerInfo().getXMPPDomain(), null);
      CollectionNode node = new CollectionNode(mPubSubModule, parent, nodeId, jid);
      node.addOwner(jid);
      try {
        node.configure(form);
      } catch (NotAcceptableException e) {
        LOGGER.warn("NotAcceptableException", e);
      }
      node.saveToDB();
      CacheFactory.doClusterTask(new RefreshNodeTask(node));
      result = node;
    }
    return (CollectionNode) result;
  }

  /**
   * Create a channel using the channel request.
   *
   * @param createUsername
   * @param channelId
   * @param parentNode
   * @throws ChannelExistsException   -- if channel already exists
   * @throws NotAcceptableException -- if an exception is thrown by openfire during channel creation.
   */
  private LeafNode createLeafNode(String createUsername, String channelId,
                    CollectionNode parentNode, ChannelCreateInfo channelInfo) //, MMXTopicOptions options, String appId)
                        throws ChannelExistsException, NotAcceptableException {

    String channelName = channelInfo.getChannelName();
    String channelDescription = channelInfo.getDescription();
    int maxItems = channelInfo.getMaxItems();

    Node result = mPubSubModule.getNode(channelId);
    if (result != null) {
      throw new ChannelExistsException("Channel with id: " + channelId + " exists");
    }
    LeafNode createdNode = null;
    if (result == null) {
      // This config form should use the same values from setOptions().
      ConfigureForm form = new ConfigureForm(DataForm.Type.submit);
      boolean isUserChannel = ChannelHelper.isUserChannel(channelId);
      form.setAccessModel(isUserChannel ? ConfigureForm.AccessModel.whitelist : ConfigureForm.AccessModel.open);
      form.setPersistentItems(maxItems != 0);
      form.setMaxItems(maxItems);
      form.setSendItemSubscribe(true);
      form.setMaxPayloadSize(Constants.MAX_PAYLOAD_SIZE);
      form.setDeliverPayloads(true);
      form.setNotifyRetract(false);
      form.setNotifyDelete(false);
      form.setNotifyConfig(false);
      form.setNodeType(ConfigureForm.NodeType.leaf);
      // TODO: default permission for channels created from console is subscribers
      TopicAction.PublisherType permission = channelInfo.getPublishPermission();
      if (permission == null) {
        permission = TopicAction.PublisherType.subscribers;
      }
      form.setPublishModel(ConfigureForm.convert(permission));
      form.setTitle(channelName);
      form.setSubscribe(channelInfo.isSubscriptionEnabled());
      if (channelDescription != null) {
        form.setDescription(channelDescription);
      }
//      LOGGER.trace("Leaf config form: "+form);

      JID jid = new JID(createUsername, mServer.getServerInfo().getXMPPDomain(), null);
      LeafNode node = new LeafNode(mPubSubModule, parentNode, channelId, jid);
      node.addOwner(jid);
      try {
        node.configure(form);
//        if (isUserChannel) {
//          setWhiteList(node, options.getWhiteList(), appId);
//        }
      } catch (NotAcceptableException e) {
        LOGGER.warn("NotAcceptableException", e);
        throw e;
      }

      if (channelInfo.isSubscribeOnCreate()) {
        NodeSubscription subscription = subscribeToNode(node, jid, jid);
        // TODO: not returning the subscription ID yet.
      }

      node.saveToDB();
      CacheFactory.doClusterTask(new RefreshNodeTask(node));
      createdNode = node;
    }
    return createdNode;
  }

  String getAppIdPrefix(String appId) {
    return ChannelHelper.CHANNEL_DELIM + appId + ChannelHelper.CHANNEL_DELIM + "*" + ChannelHelper.CHANNEL_DELIM;
  }


  /**
   * Class that represents a result of a channel related action.
   */
  public static class ChannelActionResult {
    boolean success;
    private TopicNode node;
    private String message;
    private ChannelFailureCode code;

    public boolean isSuccess() {
      return success;
    }

    public void setSuccess(boolean success) {
      this.success = success;
    }

    public TopicNode getNode() {
      return node;
    }

    public void setNode(TopicNode node) {
      this.node = node;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public ChannelFailureCode getCode() {
      return code;
    }

    public void setCode(ChannelFailureCode code) {
      this.code = code;
    }
  }

  public static enum ChannelFailureCode {
    DUPLICATE,
    NOTFOUND,
    UNKNOWN,
    INVALID_CHANNEL_ID,
    INVALID_CHANNEL_NAME
  }


  public static class ChannelExistsException extends RuntimeException {
    public ChannelExistsException(String message, Throwable cause) {
      super(message, cause);
    }

    public ChannelExistsException(String message) {
      super(message);
    }
  }

  private void setOptions(String channelName, Node node, MMXTopicOptions options, String appId) {

    ConfigureForm form = new ConfigureForm(DataForm.Type.submit);
    form.setSendItemSubscribe(true);
    form.setMaxPayloadSize(Constants.MAX_PAYLOAD_SIZE);
    form.setDeliverPayloads(true);
    form.setNotifyRetract(false);
    form.setNotifyDelete(false);
    form.setNotifyConfig(false);
    boolean isUserChannel = ChannelHelper.isUserChannel(node.getNodeID());
    form.setAccessModel(isUserChannel ? ConfigureForm.AccessModel.whitelist : ConfigureForm.AccessModel.open);
    form.setNodeType(node.isCollectionNode() ? ConfigureForm.NodeType.collection : ConfigureForm.NodeType.leaf);
    if (options == null) {
      form.setPublishModel(ConfigureForm.PublishModel.open);
      form.setPersistentItems(true);
      form.setMaxItems(-1);
      form.setSubscribe(true);
      form.setTitle(channelName);
    } else {
      // Set the default values if not specified.
      options.fillWithDefaults();

      form.setPublishModel(ConfigureForm.convert(options.getPublisherType()));
      form.setPersistentItems(options.getMaxItems() != 0);
      form.setMaxItems(options.getMaxItems());
      form.setSubscribe(options.isSubscriptionEnabled());
      form.setTitle((options.getDisplayName() == null) ?
          channelName : options.getDisplayName());
      form.setDescription(options.getDescription());
    }
    try {
      node.configure(form);
      if (isUserChannel) {
        setWhiteList(node, options.getWhiteList(), appId);
      }
    } catch (NotAcceptableException e) {
      e.printStackTrace();
    }
  }
  private void setWhiteList(Node node, List<String> whiteList, String appId) {

    if (whiteList != null) {
      for (String subId : whiteList) {
        MMXChannelUtil.addUserToChannelWhiteList(node, subId, appId);
      }
    }
  }

  // Add a list of owners to a node.  The owners should be unique.
  private void addOwnersToNode(Node node, JID[] owners) {
    for (JID owner : owners) {
      if (owner != null) {
        node.addOwner(owner);
      }
    }
  }

  public JID getServerUser(String appId) {
    // Use LRU cache for server user JID.
    JID jid = mCachedServerUsers.get(appId);
    if (jid == null) {
      AppDAO appDAO = DBUtil.getAppDAO();
      String userId = appDAO.getServerUserForApp(appId);
      if (userId != null) {
        jid = new JID(JIDUtil.makeNode(userId, appId),
                  mServer.getServerInfo().getXMPPDomain(), null);
        mCachedServerUsers.put(appId, jid);
      }
    }
    return jid;
  }

  // Create the collection node and its ancestors recursively.
  private CollectionNode createCollectionNode(int prefix, String nodeId,
                              JID creator, JID[] owners, String appId) throws IllegalArgumentException {
//    LOGGER.trace("createCollectionNode: prefix="+prefix+", nodeId="+nodeId);
    if (nodeId == null) {
      return null;
    }
    // Check if the collection node exists and is valid.
    Node node;
    if ((node = mPubSubModule.getNode(nodeId)) != null) {
      if (!node.isCollectionNode()) {
        throw new IllegalArgumentException(nodeId.substring(prefix)+
            " exists, but not a collection node.");
      }
//      LOGGER.trace("return existing collection node="+nodeId);
      return (CollectionNode) node;
    }
    String parentNodeId = ChannelHelper.getParent(prefix, nodeId);
    CollectionNode parentNode;
    if (parentNodeId == null) {
      parentNode = getRootAppChannel(ChannelHelper.getRootNodeId(nodeId));
    } else {
      parentNode = createCollectionNode(prefix, parentNodeId, creator, owners, appId);
    }

    synchronized(nodeId.intern()) {
      if ((node = mPubSubModule.getNode(nodeId)) == null) {
        LOGGER.trace("create collection node=" + nodeId + ", parent=" +
                ((parentNode == null) ? "" : parentNode.getNodeID()));
        node = new CollectionNode(mPubSubModule, parentNode, nodeId, creator);
        setOptions(null, node, null, appId);
        addOwnersToNode(node, owners);
        node.saveToDB();

        CacheFactory.doClusterTask(new RefreshNodeTask(node));
      }
    }
    LOGGER.trace("return new collection node=" + nodeId);
    return (CollectionNode) node;
  }

  /**
   * Create an application-wide or personal channel and create all parent nodes
   * if needed.
   * @param from
   * @param appId
   * @param rqt
   * @return
   * @throws MMXException
   */
  public MMXStatus createChannel(JID from, String appId, ChannelAction.CreateRequest rqt)
                          throws MMXException {
    String channel = rqt.getChannelName();
    try {
      // Note, MMXChannelUtil.checkPathAllowed(channel) should not be called here;
      // it is a restriction in the client SDK.
      channel = ChannelHelper.normalizePath(channel);
    } catch (IllegalArgumentException e) {
      throw new MMXException(e.getMessage(), StatusCode.BAD_REQUEST.getCode());
    }

    boolean isValid = ChannelHelper.validateApplicationChannelName(channel);
    if (!isValid) {
      throw new MMXException(StatusCode.INVALID_CHANNEL_NAME.getMessage(),
          StatusCode.INVALID_CHANNEL_NAME.getCode());
    }
    String userId = JIDUtil.getUserId(from);
    JID owner = from.asBareJID();
    JID serverUser = getServerUser(appId);
    // Don't add server user if the creator is the server user already.
    JID[] owners = { owner, owner.equals(serverUser) ? null : serverUser };
    String channelId = ChannelHelper.makeChannel(appId, rqt.isPersonal() ?
            userId : null, channel);
//    LOGGER.trace("createChannel realChannel="+realChannel+", channel="+channel);

    if (!mPubSubModule.canCreateNode(from)) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
          StatusCode.FORBIDDEN.getCode());
    }
    if (mPubSubModule.getNode(channelId) != null) {
      throw new MMXException(StatusCode.CHANNEL_EXISTS.getMessage(channel),
          StatusCode.CHANNEL_EXISTS.getCode());
    }
    if (!mPubSubModule.isInstantNodeSupported()) {
      throw new MMXException(StatusCode.NOT_ACCEPTABLE.getMessage(channel),
          StatusCode.NOT_ACCEPTABLE.getCode());
    }
    int prefix = ChannelHelper.getPrefixLength(channelId);
    String parentId = ChannelHelper.getParent(prefix, channelId);
    CollectionNode parent;
    if (parentId == null) {
      parent = getRootAppChannel(appId);
    } else {
      try {
        // Recursively create the parent nodes if they don't exist.
        parent = createCollectionNode(prefix, parentId, from, owners, appId);
      } catch (Throwable e) {
        throw new MMXException(e.getMessage(), StatusCode.BAD_REQUEST.getCode());
      }
    }
    if (parent != null && !rqt.isCollection()) {
      if (!parent.isAssociationAllowed(from)) {
        // Check if requester is allowed to add a new leaf node to the parent.
        throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
            StatusCode.FORBIDDEN.getCode());
      }
      if (parent.isMaxLeafNodeReached()) {
        throw new MMXException("Max nodes exceeded in the parent of "+channel,
            StatusCode.CONFLICT.getCode());
      }
    }

    synchronized(channelId.intern()) {
      if (mPubSubModule.getNode(channelId) != null) {
        throw new MMXException(StatusCode.CHANNEL_EXISTS.getMessage(channel),
            StatusCode.CHANNEL_EXISTS.getCode());
      }
//      LOGGER.trace("create node="+realChannel+", parent="+parent);
      Node node;
      if (rqt.isCollection()) {
        node = new CollectionNode(mPubSubModule, parent, channelId, from);
      } else {
        node = new LeafNode(mPubSubModule, parent, channelId, from);
      }
      // Add the creator as the owner.
      addOwnersToNode(node, owners);
      setOptions(channel, node, rqt.getOptions(), appId);

      // Do the auto-subscription for creator.
      if (rqt.getOptions() != null && rqt.getOptions().isSubscribeOnCreate()) {
        NodeSubscription subscription = subscribeToNode(node, owner, owner);
        // TODO: not returning the subscription ID yet.
      }

      node.saveToDB();
      CacheFactory.doClusterTask(new RefreshNodeTask(node));
      /**
       * Add the mapping for the roles.
       */
      TopicRoleDAO roleDAO = getTopicRoleDAO();
      List<String> roles = rqt.getRoles();
      if (roles == null) {
        roles = Collections.singletonList(MMXServerConstants.TOPIC_ROLE_PUBLIC);
      }
      roleDAO.addTopicRoles("pubsub", node.getNodeID(), roles);


    }
//    LOGGER.trace("create node="+realChannel+" success");

    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(StatusCode.SUCCESS.getMessage());
    return status;
  }

  private NodeSubscription subscribeToNode(Node node, JID owner, JID subscriber) {
    SubscribeForm optionsForm = new SubscribeForm(DataForm.Type.submit);
    // Receive notification of new items only.
    optionsForm.setSubscriptionType(NodeSubscription.Type.items);
    optionsForm.setSubscriptionDepth("all");
    // Don't set other options; it will change the subscription state to
    // pending because it will wait for the owner's approval.

    node.createSubscription(null, owner, subscriber, false, optionsForm);
    NodeSubscription subscription = node.getSubscription(subscriber);
    return subscription;
  }

  private int deleteNode(Node node, JID owner) throws MMXException {
    if (!node.getOwners().contains(owner)) {
      AppChannel channel = ChannelHelper.parseChannel(node.getNodeID());
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel.getName()),
          StatusCode.FORBIDDEN.getCode());
    }
    int count = 0;
    if (node.isCollectionNode()) {
      for (Node child : node.getNodes()) {
        if (child.isCollectionNode()) {
          count += deleteNode(child, owner);
        } else {
          if (!child.getOwners().contains(owner)) {
            AppChannel channel = ChannelHelper.parseChannel(child.getNodeID());
            throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel.getName()),
                StatusCode.FORBIDDEN.getCode());
          }

//          LOGGER.trace("delete leaf node=" + node.getNodeID());
          child.delete();
          ++count;
        }
      }
    }
//    LOGGER.trace("delete node="+node.getNodeID());
    node.delete();
    ++count;
    return count;
  }

  public MMXStatus deleteChannel(JID from, String appId,
                          ChannelAction.DeleteRequest rqt) throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannel());
    String userId = JIDUtil.getUserId(from);
    JID owner = from.asBareJID();
    String realChannel = ChannelHelper.makeChannel(appId, rqt.isPersonal() ?
            userId : null, channel);
    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    int count = deleteNode(node, owner);
    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(count+" channel"+((count==1)?" is":"s are")+" deleted");
    return status;
  }


  public ChannelInfo getChannel(String appId, MMXChannelId channel)
          throws MMXException {
        return getChannel( null,appId,channel);
  }

  public ChannelInfo getChannel(JID from, String appId, MMXChannelId channel)
                          throws MMXException {
    String realChannel = ChannelHelper.makeChannel(appId, channel.getEscUserId(),
            ChannelHelper.normalizePath(channel.getName()));
    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel.getName()),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
//    // A user can get the channel info if the channel is a global channel, or the owner
//    // of a user channel, or a subscriber to a user channel.
//    if (channel.isUserChannel() && !node.getOwners().contains(from.asBareJID()) &&
//        node.getSubscriptions(from.asBareJID()).size() > 0) {
//      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel.getName()),
//          StatusCode.FORBIDDEN.getCode());
//    }
    return nodeToInfo(channel.getUserId(), channel.getName(), node);
  }

  public Node getChannelNode(String appId, MMXChannelId channel)
          throws MMXException {
    String realChannel = ChannelHelper.makeChannel(appId, channel.getEscUserId(),
            ChannelHelper.normalizePath(channel.getName()));
    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel.getName()),
              StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    return node;
  }

  public List<ChannelInfo> getChannels(JID from, String appId, List<MMXChannelId> channels)
                            throws MMXException {
    List<ChannelInfo> infos = new ArrayList<ChannelInfo>(channels.size());
    for (MMXChannelId channel : channels) {
      try {
        infos.add(getChannel(from, appId, channel));
      } catch (Throwable e) {
        infos.add(null);
      }
    }
    return infos;
  }

  public MMXStatus retractAllFromChannel(JID from, String appId,
      ChannelAction.RetractAllRequest rqt) throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannel());
    String userId = JIDUtil.getUserId(from);
    JID owner = from.asBareJID();
    String realChannel = ChannelHelper.makeChannel(appId, rqt.isPersonal() ?
            userId : null, channel);
    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    if (!node.getOwners().contains(owner)) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
          StatusCode.FORBIDDEN.getCode());
    }
    LeafNode leafNode = (LeafNode) node;
    List<PublishedItem> pubItems = leafNode.getPublishedItems();
    leafNode.deleteItems(pubItems);

    int count = (pubItems == null) ? 0 : pubItems.size();
    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(count+" item"+((count==1)?" is":"s are")+" retracted");
    return status;
  }

  public Map<String, Integer> retractFromChannel(JID from, String appId,
      ChannelAction.RetractRequest rqt) throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannel());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getUserId(), channel);
    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    if (node.isCollectionNode()) {
      throw new MMXException("Cannot retract items from a collection channel",
          StatusCode.NOT_IMPLEMENTED.getCode());
    }

    LeafNode leafNode = (LeafNode) node;
    List<String> itemIds = rqt.getItemIds();
    if (itemIds == null || itemIds.size() == 0) {
      throw new MMXException(StatusCode.BAD_REQUEST.getMessage("no item ID's"),
          StatusCode.BAD_REQUEST.getCode());
    }
    if (!leafNode.isItemRequired()) {
      // Cannot delete items from a leaf node that doesn't handle itemIDs
      throw new MMXException("Items required in this channel",
          StatusCode.NOT_IMPLEMENTED.getCode());
    }

    List<PublishedItem> pubItems = new ArrayList<PublishedItem>(itemIds.size());
    Map<String, Integer> results = new HashMap<String, Integer>(itemIds.size());
    for (String itemId : itemIds) {
      if (itemId == null) {
        continue;
      }
      PublishedItem item = leafNode.getPublishedItem(itemId);
      if (item == null) {
        results.put(itemId, StatusCode.GONE.getCode());
      } else if (!item.canDelete(from)) {
        results.put(itemId, StatusCode.FORBIDDEN.getCode());
      } else {
        pubItems.add(item);
        results.put(itemId, StatusCode.SUCCESS.getCode());
      }
    }
    leafNode.deleteItems(pubItems);
    return results;
  }

  public ChannelAction.SubscribeResponse subscribeChannel(JID from, String appId,
              ChannelAction.SubscribeRequest rqt, List<String> userRoles) throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannel());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getUserId(), channel);
    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }

    JID owner = from.asBareJID();
    // The subscriber can specify a different resource or without resource.
    JID subscriber = new JID(from.getNode(), from.getDomain(), rqt.getDevId());

    AccessModel accessModel = node.getAccessModel();
    if (!accessModel.canSubscribe(node, owner, subscriber)) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
          StatusCode.FORBIDDEN.getCode());
    }

    // Check if the subscription owner is a user with outcast affiliation
    NodeAffiliate nodeAffiliate = node.getAffiliate(owner);
    if (nodeAffiliate != null &&
        nodeAffiliate.getAffiliation() == NodeAffiliate.Affiliation.outcast) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
          StatusCode.FORBIDDEN.getCode());
    }

    // Check that subscriptions to the node are enabled
    if (!node.isSubscriptionEnabled()) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
          StatusCode.FORBIDDEN.getCode());
    }
    /*
     * ensure user has the necessary role for subscribing to the channel.
     */
    boolean isSubScriptionAllowed = isAllowed(node.getNodeID(), userRoles);
    if (!isSubScriptionAllowed) {
      LOGGER.info("Subscription to Channel:{} not allowed for user with roles:{}", node.getNodeID(), userRoles);
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
          StatusCode.FORBIDDEN.getCode());
    }
    // Check for duplicated subscription; return error or existing subscription.
    NodeSubscription subscription;
    if ((subscription = node.getSubscription(subscriber)) != null) {
      if (rqt.isErrorOnDup()) {
        throw new MMXException(StatusCode.SUBSCRIPTION_EXISTS.getMessage(channel),
          StatusCode.SUBSCRIPTION_EXISTS.getCode());
      } else {
        ChannelAction.SubscribeResponse resp = new ChannelAction.SubscribeResponse(
            subscription.getID(), StatusCode.SUCCESS.getCode(),
            StatusCode.SUCCESS.getMessage());
        return resp;
      }
    }

    subscription = subscribeToNode(node, owner, subscriber);

    ChannelAction.SubscribeResponse resp = new ChannelAction.SubscribeResponse(
        subscription.getID(), StatusCode.SUCCESS.getCode(),
        StatusCode.SUCCESS.getMessage());
    return resp;
  }

  public MMXStatus unsubscribeChannel(JID from, String appId,
                  ChannelAction.UnsubscribeRequest rqt) throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannel());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getUserId(), channel);
    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    int count = 0;
    JID owner = from.asBareJID();
    String subId = rqt.getSubId();
    for (NodeSubscription subscription : node.getSubscriptions(owner)) {
      if (subId == null || subscription.getID().equals(subId)) {
        ++count;
        node.cancelSubscription(subscription);
      }
    }
    if (count == 0) {
      throw new MMXException(StatusCode.GONE.getMessage(),
          StatusCode.GONE.getCode());
    }

    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(count+" subscription"+((count==1)?" is":"s are")+" cancelled");
    return status;
  }

  public MMXStatus unsubscribeForDev(JID from, String appId,
                  ChannelAction.UnsubscribeForDevRequest rqt) throws MMXException {
    String prefix = ChannelHelper.CHANNEL_DELIM + appId + ChannelHelper.CHANNEL_DELIM;
    int count = 0;
    JID owner = from.asBareJID();
    String devId = rqt.getDevId();
    for (Node node : mPubSubModule.getNodes()) {
      if (!node.getNodeID().startsWith(prefix)) {
        continue;
      }
      for (NodeSubscription subscription : node.getSubscriptions(owner)) {
        if (devId.equals(subscription.getJID().getResource())) {
          ++count;
          node.cancelSubscription(subscription);
        }
      }
    }
    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(count+" subscription"+((count==1)?" is":"s are")+" cancelled");
    return status;
  }

  public ChannelAction.ListResponse listChannels(JID from, String appId,
                  ChannelAction.ListRequest rqt, List<String> userRoles) throws MMXException {
    ChannelAction.ListResponse resp = new ChannelAction.ListResponse();
    Integer maxLimit = rqt.getLimit();
    boolean recursive = rqt.isRecursive();
    String start = rqt.getStart();
    ListType type = rqt.getType();
    int limit = (maxLimit == null || maxLimit == -1) ? Integer.MAX_VALUE : maxLimit;
    String realChannel;
    if (start == null || start.isEmpty()) {
      // Get the g
      // lobal root node.
      Node node = mPubSubModule.getNode(appId);
      if (node == null) {
        throw new MMXException(StatusCode.APP_NODE_NOT_FOUND.getMessage(appId),
            StatusCode.APP_NODE_NOT_FOUND.getCode());
      }
      // Get its top level children nodes filtered by the search type.
      String userId = JIDUtil.getUserId(from);
      limit = getTopChildNodes(recursive, node, resp, limit, type, userId, userRoles);
    } else {
      // Filter by the global channels.
      if (type == ListType.global || type == ListType.both) {
        realChannel = ChannelHelper.makeChannel(appId, null, start);
        // Get the global channel node first.
        Node node = mPubSubModule.getNode(realChannel);
        if (node == null) {
          throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(start),
                StatusCode.CHANNEL_NOT_FOUND.getCode());
        }
        // Get its children nodes without search filter.
        limit = getChildNodes(recursive, node, resp, limit);
      }
      // Filter by the user channels.
      if (type == ListType.personal || type == ListType.both) {
        realChannel = ChannelHelper.makeChannel(appId, JIDUtil.getUserId(from), start);
        // Get the personal channel node first.
        Node node = mPubSubModule.getNode(realChannel);
        if (node == null) {
          throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(start),
                StatusCode.CHANNEL_NOT_FOUND.getCode());
        }
        // Get its children nodes without search filter.
        limit = getChildNodes(recursive, node, resp, limit);
      }
    }
    return resp;
  }

  public ChannelInfo nodeToChannelInfo(String userId,Node node){
    return nodeToInfo(userId,node.getName(),node);
  }

  private ChannelInfo nodeToInfo(String userId, String channel, Node node) {
    ChannelInfo info = new ChannelInfo(
        userId, node.getName() != null ? node.getName() : channel, node.isCollectionNode())
      .setId(TopicHelper.convertToId(node.getNodeID()))
      .setDisplayName(node.getName())
      .setCreationDate(node.getCreationDate())
      .setDescription(node.getDescription())
      .setModifiedDate(node.getModificationDate())
      .setCreator(node.getCreator().toString())
      .setSubscriptionEnabled(node.isSubscriptionEnabled());
    if (!node.isCollectionNode()) {
      LeafNode leafNode = (LeafNode) node;
      info.setMaxItems(leafNode.isPersistPublishedItems() ?
            leafNode.getMaxPublishedItems() : 0)
          .setMaxPayloadSize(leafNode.getMaxPayloadSize())
          .setPersistent(leafNode.isPersistPublishedItems())
          .setPublishPermission(ConfigureForm.convert(leafNode.getPublisherModel()));
    } else {
      info.setMaxItems(0)
          .setMaxPayloadSize(0)
          .setPersistent(false)
          .setPublishPermission(null);
    }
    return info;
  }

  // Get all top level children nodes which are either global channels or user
  // channels.  Filter out the nodes by the search type.
  private int getTopChildNodes(boolean recursive, Node root,
      ChannelAction.ListResponse resp, int limit, ListType type, String userId, List<String> roles) {
    boolean globalOnly = (type == ListType.global);
    if (roles == null || roles.isEmpty()) {
      roles = Collections.singletonList(MMXServerConstants.TOPIC_ROLE_PUBLIC);
    }
    for (Node child : root.getNodes()) {
      AppChannel channel = ChannelHelper.parseChannel(child.getNodeID());
      if (channel == null) {
        LOGGER.warn("Ignore malformed channel: " + child.getNodeID());
        continue;
      }
      // Brain teaser: the xor below is actually same as
      //    (type == ListType.global && !channel.isUserChannel()) ||
      //    (type == ListType.personal && channel.isUserChannel()))
      if ((type == ListType.both) || (globalOnly ^ channel.isUserChannel())) {
        if (channel.isUserChannel() && !channel.getEscUserId().equals(userId)) {
          continue;
        }
        if (--limit < 0) {
          return limit;
        }
        //resp.add(nodeToInfo(channel.getUserId(), channel.getName(), child));
        /**
         * Add a check to see if the channel role mapping allows current user's roles
         * to access this channel. The check should be done only for global channels
         */
        if (!channel.isUserChannel()) {
          boolean userHasRole = isAllowed(child.getNodeID(), roles);
          if (userHasRole) {
            resp.add(nodeToInfo(channel.getUserId(), channel.getName(), child));
          }
        } else {
          resp.add(nodeToInfo(channel.getUserId(), channel.getName(), child));
        }
        if (recursive && child.isCollectionNode()) {
          limit = getChildNodes(recursive, child, resp, limit);
        }
      }
    }
    return limit;
  }

  // Get all children nodes recursively below the top level.
  // @return < 0 if exceeding the limit, >= 0 if within the limit.
  private int getChildNodes(boolean recursive, Node node,
      ChannelAction.ListResponse resp, int limit) {
    if (!node.isCollectionNode()) {
      AppChannel channel = ChannelHelper.parseChannel(node.getNodeID());
      if (channel == null) {
        LOGGER.warn("Ignore malformed channel: " + node.getNodeID());
      } else {
        if (--limit < 0) {
          return limit;
        }
        resp.add(nodeToInfo(channel.getUserId(), channel.getName(), node));
      }
    } else {
      for (Node child : node.getNodes()) {
        AppChannel channel = ChannelHelper.parseChannel(child.getNodeID());
        if (channel == null) {
          LOGGER.warn("Ignore malformed channel: " + child.getNodeID());
          continue;
        }
        if (--limit < 0) {
          return limit;
        }
        resp.add(nodeToInfo(channel.getUserId(), channel.getName(), child));
        if (recursive && child.isCollectionNode()) {
              limit = getChildNodes(recursive, child, resp, limit);
        }
      }
    }
    return limit;
  }

  private String[] getChannels(String appId, List<MMXChannelId> list,
                              int begin, int size) {
    if (list == null) {
      return null;
    }
    size = Math.min(size, list.size() - begin);
    String[] channels = new String[size];
    for (int i = 0, index = begin; --size >= 0; index++, i++) {
      MMXChannelId channelNode = list.get(index);
      channels[i] = ChannelHelper.makeChannel(appId, channelNode.getEscUserId(),
              channelNode.getName());
    }
    return channels;
  }

  private static class SQLHelper {
    public static String generateArgList(int numOfArgs) {
      if (numOfArgs == 0) {
        return "";
      }
      StringBuilder sb = new StringBuilder((numOfArgs-1) * 2 + 1);
      while (--numOfArgs > 0) {
        sb.append("?,");
      }
      sb.append('?');
      return sb.toString();
    }

    public static void bindArgList(PreparedStatement pstmt, int index,
                                      String[] args) throws SQLException {
      for (String arg : args) {
        pstmt.setString(index++, arg);
      }
    }

    public static void bindArgList(PreparedStatement pstmt, int index,
                                      List<String> args) throws SQLException {
      for (String arg : args) {
        pstmt.setString(index++, arg);
      }
    }
  }

  public ChannelAction.SummaryResponse getSummary(JID from, String appId,
          ChannelAction.SummaryRequest rqt) throws MMXException {
    // Build a collection of channel ID's from the request; it contains channels
    // without any published items.
    HashSet<MMXChannelId> tpNoItems = new HashSet<MMXChannelId>(rqt.getChannelNodes().size());
    for (MMXChannelId channelId : rqt.getChannelNodes()) {
      tpNoItems.add(channelId);
    }
    ChannelAction.SummaryResponse resp = new ChannelAction.SummaryResponse(
        rqt.getChannelNodes().size());
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      con = DbConnectionManager.getConnection();
      int start = 0;
      int numOfChannels = rqt.getChannelNodes().size();
      StringBuilder dateRange = new StringBuilder();
      ArrayList<String> dates = new ArrayList<String>();
      if (rqt.getSince() != null) {
        dateRange.append("AND ofPubsubItem.creationDate >= ? ");
        dates.add(StringUtils.dateToMillis(rqt.getSince()));
      }
      if (rqt.getUntil() != null) {
        dateRange.append("AND ofPubsubItem.creationDate <= ? ");
        dates.add(StringUtils.dateToMillis(rqt.getUntil()));
      }
      do {
        // Limit to 128 channels per query because some DBMS cannot handle more
        // than 255 arguments in the IN clause.
        String[] channels = getChannels(appId, rqt.getChannelNodes(), start, 128);
        if (channels == null || channels.length == 0) {
          break;
        }
        String argList = SQLHelper.generateArgList(channels.length);
        String sql = "SELECT ofPubsubNode.maxItems,count(*),max(ofPubsubItem.creationDate),ofPubsubNode.nodeId, ofPubsubNode.name "+
                     "FROM ofPubsubItem, ofPubsubNode " +
                     "WHERE ofPubsubItem.serviceID=? AND " +
                     "      ofPubsubItem.nodeID = ofPubsubNode.nodeId AND " +
                     "      ofPubsubItem.nodeID IN ("+argList+") "+dateRange+
                     "GROUP BY ofPubsubItem.nodeID";
        pstmt = con.prepareStatement(sql);
        pstmt.setString(1, mPubSubModule.getServiceID());
        SQLHelper.bindArgList(pstmt, 2, channels);
        SQLHelper.bindArgList(pstmt, 2 + channels.length, dates);
        rs = pstmt.executeQuery();
        while (rs.next()) {
          int maxItems = rs.getInt(1);
          int count = rs.getInt(2);
          Date creationDate = new Date(Long.parseLong(rs.getString(3).trim()));
          String nodeId = rs.getString(4);
          MMXChannelId channelId = ChannelHelper.parseNode(nodeId);
          String channelName = rs.getString(5);
          MMXChannelId channelIdWithOriginalName = new MMXChannelId(channelId.getUserId(), channelName);
          resp.add(new ChannelSummary(channelIdWithOriginalName)
            .setCount((maxItems < 0) ? count : Math.min(maxItems, count))
            .setLastPubTime(creationDate));
          // This channel has published items; remove it from the collection.
          tpNoItems.remove(channelId);
        }
        start += channels.length;
      } while (start < numOfChannels);
      // Fill the response with the channels having no published items.
      Iterator<MMXChannelId> it = tpNoItems.iterator();
      while (it.hasNext()) {
        resp.add(new ChannelSummary(it.next()).setCount(0));
      }
      return resp;
    } catch (Exception sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new MMXException(sqle.getMessage(),
          StatusCode.SERVER_ERROR.getCode());
    } finally {
      DbConnectionManager.closeConnection(rs, pstmt, con);
    }
  }

  public List<ChannelInfo> searchByTags(JID from, String appId,
                      TagSearch rqt) throws MMXException {
    List<TopicEntity> entities;
    List<String> tags = rqt.getTags();
    TopicDAO channelDao = DBUtil.getTopicDAO();
    switch(rqt.getOperator()) {
    case AND:
      entities = channelDao.getTopicsForTagAND(tags, appId);
      break;
    case OR:
      entities = channelDao.getTopicsForTagOR(tags, appId);
      break;
    default:
      entities = new ArrayList<TopicEntity>(0);
      break;
    }
    // Convert the channel entity to channel info.
    List<ChannelInfo> res = new ArrayList<ChannelInfo>(
        entities.size());
    for (TopicEntity entity : entities) {
      MMXChannelId channel = ChannelHelper.parseNode(entity.getNodeId());
      ChannelInfo info =  new ChannelInfo(channel.getUserId(),
            channel.getName(), !entity.isLeaf())
        .setId(ChannelHelper.converToId(entity.getNodeId()))
        .setDisplayName(entity.getName())
        .setDescription(entity.getDescription())
        .setCreationDate(entity.getCreationDate())
        .setMaxItems(entity.isPersistItems() ? entity.getMaxItems() : 0)
        .setMaxPayloadSize(entity.getMaxPayloadSize())
        .setModifiedDate(entity.getModificationDate())
        .setPublishPermission(ConfigureForm.convert(
                PublisherModel.valueOf(entity.getPublisherModel())))
        .setPersistent(entity.isPersistItems())
        .setCreator(entity.getCreator())
        .setSubscriptionEnabled(entity.isSubscriptionEnabled());
      res.add(info);
    }
    return res;
  }

  public ChannelAction.ChannelTags getTags(JID from, String appId,
                      MMXChannelId rqt) throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getName());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getEscUserId(), channel);
    Node node = mPubSubModule.getNode(realChannel);
    // No need to check for permission; just check for existing.
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }

    TagDAO tagDao = DBUtil.getTagDAO();
    String serviceId = node.getService().getServiceID();
    String nodeId = node.getNodeID();
    List<String> tags = new ArrayList<String>();
    try {
      tags = tagDao.getTagsForTopic(appId, serviceId, nodeId);
    } catch (Exception e) {
      LOGGER.error("getTags : caught exception for getting tags appId={}, serviceId={}, nodeId={}",
              new Object[]{appId, serviceId, nodeId, e});

    }

    ChannelAction.ChannelTags channelTags = new ChannelAction.ChannelTags(rqt.getUserId(),
        rqt.getName(), tags, new Date());
    return channelTags;
  }

  public MMXStatus setTags(JID from, String appId, ChannelAction.ChannelTags rqt)
                      throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannelName());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getUserId(), channel);
    Node node = mPubSubModule.getNode(realChannel);
    // No need to check for permission; just check for existing.
    if (node == null) {
      LOGGER.trace("setTags : node not found appId={},  channel={}", appId, realChannel);
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    List<String> tags = rqt.getTags();
    String serviceId = node.getService().getServiceID();
    String nodeId = node.getNodeID();

    TagDAO tagDao = DBUtil.getTagDAO();
    tagDao.deleteAllTagsForTopic(appId, serviceId, nodeId);

    if(!Utils.isNullOrEmpty(tags)) {
      for(String tag : tags) {
        try {
          tagDao.createTopicTag(tag, appId, serviceId, nodeId);
        } catch (DbInteractionException e) {
          return (new MMXStatus())
                  .setCode(StatusCode.SERVER_ERROR.getCode())
                  .setMessage(e.getMessage());
        }
      }
    }

    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(StatusCode.SUCCESS.getMessage());
    return status;
  }

  public MMXStatus addTags(JID from, String appId, ChannelAction.ChannelTags rqt)
                      throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannelName());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getUserId(), channel);
    Node node = mPubSubModule.getNode(realChannel);
    // No need to check for permission; just check for existing.
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    List<String> tags = rqt.getTags();

    String serviceId = node.getService().getServiceID();
    String nodeId = node.getNodeID();

    if(!Utils.isNullOrEmpty(tags)) {
      TagDAO tagDao = DBUtil.getTagDAO();
      for(String tag : tags) {
        try {
          LOGGER.trace("addTags : creating channel setting tag={}, appId={}, serviceId={}, nodeId={}",
                  new Object[]{tag, appId, serviceId, nodeId});

          tagDao.createTopicTag(tag, appId, serviceId, nodeId);
        } catch (DbInteractionException e) {
          LOGGER.error("addTags : caught exception setting tag={}, appId={}, serviceId={}, nodeId={}"
                  , new Object[]{tag, appId, serviceId, nodeId, e});
          return (new MMXStatus())
              .setCode(StatusCode.SERVER_ERROR.getCode())
              .setMessage(e.getMessage());
        }
      }
    } else {
      LOGGER.trace("addTags : received an empty tag list appId={}, serviceId={}, nodeId={}", new Object[]{appId, serviceId, nodeId});
    }

    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(StatusCode.SUCCESS.getMessage());
    return status;
  }

  public MMXStatus removeTags(JID from, String appId, ChannelAction.ChannelTags rqt)
                      throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannelName());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getUserId(), channel);
    Node node = mPubSubModule.getNode(realChannel);
    // No need to check for permission; just check for existing.
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    List<String> tags = rqt.getTags();
    String serviceId = node.getService().getServiceID();
    String nodeId = node.getNodeID();

    if(!Utils.isNullOrEmpty(tags)) {
      TagDAO tagDao = DBUtil.getTagDAO();
      tagDao.deleteTagsForTopic(tags, appId, serviceId, nodeId);
    }

    MMXStatus status = (new MMXStatus())
            .setCode(StatusCode.SUCCESS.getCode())
            .setMessage(StatusCode.SUCCESS.getMessage());
    return status;
  }


  public ChannelAction.ChannelQueryResponse searchChannel(JID from, String appId,
                                                    TopicAction.TopicSearchRequest rqt, List<String> userRoles) throws MMXException {
    String userId = JIDUtil.getUserId(from);
    TopicQueryBuilder queryBuilder = new TopicQueryBuilder();
    int offset = rqt.getOffset();
    int size = rqt.getLimit();
    if (size > PubSubPersistenceManagerExt.MAX_ROWS_RETURN) {
      throw new MMXException(StatusCode.VALUE_TOO_LARGE.getMessage(
          "Max number of results cannot exceed "+PubSubPersistenceManagerExt.MAX_ROWS_RETURN),
          StatusCode.VALUE_TOO_LARGE.getCode());
    } else if (size < 0) {
      size = PubSubPersistenceManagerExt.MAX_ROWS_RETURN;
    }
    PaginationInfo pgInfo = PaginationInfo.build(size, offset);
    QueryBuilderResult queryBuilderResult = queryBuilder.buildPaginationQuery(rqt, appId, pgInfo, userId, userRoles);
    SearchResult<TopicAction.TopicInfoWithSubscriptionCount> results = PubSubPersistenceManagerExt.
        getTopicWithPagination(new OpenFireDBConnectionProvider(), queryBuilderResult, pgInfo);

    List<ChannelInfo> channelList = new ArrayList<ChannelInfo>(results.getResults().size());

    for (TopicAction.TopicInfoWithSubscriptionCount ti : results.getResults()) {
      ChannelInfo info = new ChannelInfo(ti.getUserId(), ti.getName(), ti.isCollection())
        .setId(ti.getId())
        .setDisplayName(ti.getDisplayName())
        .setDescription(ti.getDescription())
        .setCreationDate(ti.getCreationDate())
        .setModifiedDate(ti.getModifiedDate())
        .setPublishPermission(ti.getPublisherType())
        .setMaxPayloadSize(ti.getMaxPayloadSize())
        .setMaxItems(ti.isPersistent() ? ti.getMaxItems() : 0)
        .setPersistent(ti.isPersistent())
        .setCreator(ti.getCreator())
        .setSubscriptionEnabled(ti.isSubscriptionEnabled());
      channelList.add(info);
    }
    ChannelAction.ChannelQueryResponse resp = new ChannelAction.ChannelQueryResponse(results.getTotal(), channelList);
    return resp;
  }

  public MMXStatus processSendLastPublishedItems(JID from, String appId,
                          SendLastPublishedItems rqt) throws MMXException {
    JID fromUser = from.asBareJID();
    Date since = rqt.getSince();
    if (since == null) {
      // Missing "since" here
      throw new MMXException(StatusCode.INVALID_DATE.getMessage(),
          StatusCode.INVALID_DATE.getCode());
    }

    // Don't query the DB directly because some items are cached in memory.
    // Besides, it is faster looping through all cached nodes in memory than
    // query the ofPubsubSubscription table because # of nodes should be many
    // less than # of subscriptions.

    String prefix = ChannelHelper.makePrefix(appId);

    // Find all collection nodes subscribed by the user.
    TreeMap<String, Node> colNodes = new TreeMap<String, Node>();
    for (Node node : mPubSubModule.getNodes()) {
      if (!node.getNodeID().startsWith(prefix)) {
        // Skip channels that do not belong to this app.
        continue;
      }
      if (node.isCollectionNode()) {
        Collection<NodeSubscription> subs = node.getSubscriptions(fromUser);
        if (subs != null && subs.size() > 0) {
          colNodes.put(node.getNodeID(), node);
          LOGGER.trace("Collection node=" + node.getNodeID() + " is subscribed");
        }
      }
    }

    int numSent = 0, numSubs = 0;
    int maxItems = rqt.getMaxItems();
    for (Node node : mPubSubModule.getNodes()) {
      if (!node.getNodeID().startsWith(prefix)) {
        // Skip channels that do not belong to this app.
        continue;
      }
      if (maxItems == 1) {
        // Check the leaf node if its last published item should be sent.
        PublishedItem item = node.getLastPublishedItem();
//        if (item == null) {
//          LOGGER.trace("No published items in subscribed node="+node.getNodeID());
//        } else {
//          LOGGER.trace("since="+since.getTime()+", creatDate="+item.getCreationDate().getTime()+
//              ", node="+node.getNodeID()+", last pub item="+item.getID());
//        }
        if (item == null || item.getCreationDate().getTime() < since.getTime()) {
          // Skip all last published items older than the last delivery time.
          continue;
        }
        Collection<NodeSubscription> subs = node.getSubscriptions(fromUser);
        if (subs == null || subs.size() == 0) {
          // The leaf node has no subscriptions, check its ancestor for subscriptions.
          Node ancestor = findAncestor(colNodes, node);
          if (ancestor == null) {
            continue;
          }
          subs = ancestor.getSubscriptions(fromUser);
        }
        // Either the leaf node or ancestor node has subscriptions with the
        // latest published item.
        numSubs += subs.size();
        for (NodeSubscription sub : subs) {
          if (sendLastPublishedItem(item, sub, from)) {
            ++numSent;
//            LOGGER.trace("Sent last published item="+item.getID()+
//                        ", sub ID="+sub.getID());
          } else {
//            LOGGER.trace("cannot send last published item="+item.getID()+
//                        ", sub ID="+sub.getID());
          }
        }
      } else {
        if (node.isCollectionNode()) {
          continue;
        }
        Collection<NodeSubscription> subs = node.getSubscriptions(fromUser);
        if (subs == null || subs.size() == 0) {
          // The leaf node is not subscribed, check its ancestor for subscriptions.
          Node ancestor = findAncestor(colNodes, node);
          if (ancestor == null) {
//            LOGGER.trace("No ancestor node is subscribed, skip "+node.getNodeID());
            continue;
          }
          // Get the subscriptions from the ancestor.
          subs = ancestor.getSubscriptions(fromUser);
          if (subs.size() == 0) {
//            LOGGER.trace("No subscriptions in ancestor node "+ancestor.getNodeID());
            continue;
          }
        }

//        LOGGER.trace("Fetch published items from "+node.getNodeID()+", since="+since);
        List<PublishedItem> items = PubSubPersistenceManagerExt.getPublishedItems(from,
            (LeafNode) node, maxItems, since);
        if (items == null || items.size() == 0) {
//          LOGGER.trace("No published items in "+node.getNodeID()+", since="+since);
          continue;
        }

        // Either the leaf node or ancestor node has subscriptions with the
        // latest published item.
        numSubs += subs.size();
        for (NodeSubscription sub : subs) {
          if (sendLastPublishedItems(items, sub, from)) {
            numSent += items.size();
//            LOGGER.trace("Sent last published #items="+items.size()+
//                        ", sub ID="+sub.getID());
          } else {
//            LOGGER.warn("cannot send last published items="+items.size()+
//                        ", sub ID="+sub.getID());
          }
        }
      }
    }

    MMXStatus status = (new MMXStatus())
        .setCode(Constants.STATUS_CODE_200)
        .setMessage(numSubs+" subscriptions; "+numSent+" published items sent");
    return status;
  }

  private Node findAncestor(TreeMap<String, Node> map, Node node) {
    Entry<String, Node> entry = map.floorEntry(node.getNodeID());
    if ((entry != null) && node.getNodeID().startsWith(entry.getKey())) {
      return entry.getValue();
    } else {
      return null;
    }
  }

  private boolean sendLastPublishedItem(PublishedItem publishedItem,
                                          NodeSubscription subNode, JID to) {
    if (!subNode.canSendPublicationEvent(publishedItem.getNode(), publishedItem)) {
      return false;
    }
    Node node = subNode.getNode();
    Message notification = new Message();
    Element event = notification.getElement()
            .addElement("event", "http://jabber.org/protocol/pubsub#event");
    Element items = event.addElement("items");
    items.addAttribute("node", node.getNodeID());
    Element item = items.addElement("item");
    if (publishedItem.getNode().isItemRequired()) {
        item.addAttribute("id", publishedItem.getID());
    }
    if (node.isPayloadDelivered() && publishedItem.getPayload() != null) {
        item.add(publishedItem.getPayload().createCopy());
    }
    // Add a message body (if required)
    if (subNode.isIncludingBody()) {
        notification.setBody(LocaleUtils.getLocalizedString("pubsub.notification.message.body"));
    }
    // Include date when published item was created
    notification.getElement().addElement("delay", "urn:xmpp:delay")
            .addAttribute("stamp", XMPPDateTimeFormat.format(publishedItem.getCreationDate()));
    // Send the event notification to the subscriber
    node.getService().sendNotification(node, notification, to);
//    node.getService().sendNotification(node, notification, subNode.getJID());
    return true;
  }

  private boolean sendLastPublishedItems(List<PublishedItem> publishedItems,
                                           NodeSubscription subNode, JID to) {
    PublishedItem pubItem = publishedItems.get(0);
    if (!subNode.canSendPublicationEvent(pubItem.getNode(), pubItem)) {
      return false;
    }
    Node node = subNode.getNode();
    Message notification = new Message();
    Element event = notification.getElement().addElement("event",
            "http://jabber.org/protocol/pubsub#event");
    Element items = event.addElement("items");
    items.addAttribute("node", node.getNodeID());
    for (PublishedItem publishedItem : publishedItems) {
      Element item = items.addElement("item");
      if (publishedItem.getNode().isItemRequired()) {
        item.addAttribute("id", publishedItem.getID());
      }
      if (node.isPayloadDelivered() && publishedItem.getPayload() != null) {
        item.add(publishedItem.getPayload().createCopy());
      }
    }
    // Add a message body (if required)
    if (subNode.isIncludingBody()) {
      notification.setBody(LocaleUtils
          .getLocalizedString("pubsub.notification.message.body"));
    }
    // Include date when published item was created
    notification
        .getElement()
        .addElement("delay", "urn:xmpp:delay")
        .addAttribute("stamp",
                XMPPDateTimeFormat.format(pubItem.getCreationDate()));
    // Send the event notification to the subscriber
    node.getService().sendNotification(node, notification, to);
    // node.getService().sendNotification(node, notification, subNode.getJID());
    return true;
  }

  /**
   * Create the <code>all</code> version channel of an OS.  All its parent channels
   * will be created if they do not exist.
   * @param creatorUserId The app server user ID.
   * @param appId The app ID.
   * @param osType The non-null OS type.
   * @param displayName The display name of the "all" versions.
   * @return
   */
  public MMXStatus createOSChannel(String creatorUserId, String appId,
                                  OSType osType, String displayName) {
    if (osType == null) {
      throw new NullPointerException("OS type is null");
    }
    String allChannel = ChannelHelper.makeOSChannel(osType, ChannelHelper.CHANNEL_LEAF_ALL);
    MMXTopicOptions options = new MMXTopicOptions().setMaxItems(-1);
    ChannelAction.CreateRequest rqt = new ChannelAction.CreateRequest(allChannel, false,
                options);
    try {
      // create the OS and all version channels
      JID jid = new JID(creatorUserId, mServer.getServerInfo().getXMPPDomain(), null);
      MMXStatus status = createChannel(jid, appId, rqt);
      return status;
    } catch (MMXException e) {
      e.printStackTrace();
      return new MMXStatus().setCode(e.getCode()).setMessage(e.getMessage());
    }
  }

  /**
   * Delete the OS channel and its children, or a specific OS type channel.
   * @param creatorUserId The app server user ID.
   * @param appId The app ID.
   * @param osType null for all OS, or a specific OS type.
   * @return
   */
  public MMXStatus deleteOSChannel(String creatorUserId, String appId, OSType osType) {
    String channel = ChannelHelper.makeOSChannel(osType, null);
    ChannelAction.DeleteRequest rqt = new ChannelAction.DeleteRequest(channel, false);
    try {
      // delete the OS channel and its children.
      JID jid = new JID(creatorUserId, mServer.getServerInfo().getXMPPDomain(), null);
      MMXStatus status = deleteChannel(jid, appId, rqt);
      return status;
    } catch (MMXException e) {
      e.printStackTrace();
      return new MMXStatus().setCode(e.getCode()).setMessage(e.getMessage());
    }
  }

  public ChannelAction.FetchResponse fetchItems(JID from, String appId,
            ChannelAction.FetchRequest rqt) throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannel());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getUserId(), channel);
    ChannelAction.FetchOptions options = rqt.getOptions();
    String subId = null;
    Date since = null;
    Date until = null;
    boolean ascending = false;
    int maxItems = 0;
    int offset = 0;
    if (options != null) {
      subId = options.getSubId();
      since = options.getSince();
      until = options.getUntil();
      ascending = options.isAscending();
      maxItems = options.getMaxItems();
      offset = options.getOffset();
    }
    // If not defined, default to system property ("xmpp.pubsub.fetch.max")
    if (maxItems <= 0) {
      maxItems = -1;
    }
    // If not defined, default to the epoch.
    if (since == null) {
      since = new Date(0L);
    }
    // If not defined, default to current time.
    if (until == null) {
      until = new Date();
    }

    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    if (node.isCollectionNode()) {
      throw new MMXException("Cannot fetch items from a collection channel",
          StatusCode.NOT_IMPLEMENTED.getCode());
    }
    // Check if sender and subscriber JIDs match or if a valid "trusted proxy" is being used
    // Assumed that the owner of the subscription is the bare JID of the subscription JID.
    JID owner = from.asBareJID();
    if (!node.getAccessModel().canAccessItems(node, owner, from)) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
          StatusCode.FORBIDDEN.getCode());
    }
    // Check that the requester is not an outcast
    NodeAffiliate affiliate = node.getAffiliate(owner);
    if (affiliate != null && affiliate.getAffiliation() == NodeAffiliate.Affiliation.outcast) {
        throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
            StatusCode.FORBIDDEN.getCode());
    }
    // Check if the specified subId belongs to an existing node subscription
    NodeSubscription subscription = null;
    if (subId != null) {
      if ((((subscription = node.getSubscription(from)) == null) &&
           ((subscription = node.getSubscription(owner)) == null)) ||
          !subId.equals(subscription.getID())) {
        throw new MMXException(StatusCode.NOT_ACCEPTABLE.getMessage(
            "Invalid subscription ID"), StatusCode.NOT_ACCEPTABLE.getCode());
      }
      if (subscription != null && !subscription.isActive()) {
        throw new MMXException(StatusCode.NOT_AUTHORIZED.getMessage("channel not suscribed"),
            StatusCode.NOT_AUTHORIZED.getCode());
      }
    }

    List<PublishedItem> pubItems = PubSubPersistenceManagerExt.getPublishedItems(from,
        (LeafNode) node, offset, maxItems, since, until, ascending);
    List<MMXPublishedItem> mmxItems = new ArrayList<MMXPublishedItem>(pubItems.size());
    for (PublishedItem pubItem : pubItems) {
      MMXPublishedItem mmxItem = new MMXPublishedItem(pubItem.getID(),
          pubItem.getPublisher().toBareJID(),
          pubItem.getCreationDate(),
          pubItem.getPayloadXML());
      mmxItems.add(mmxItem);
    }
    int total = PubSubPersistenceManagerExt.getPublishedItemCount(from,
        (LeafNode) node, since, until);
    ChannelAction.FetchResponse resp = new ChannelAction.FetchResponse(
        rqt.getUserId(), channel, total, mmxItems);
    return resp;
  }

  public ChannelAction.FetchResponse getItems(JID from, String appId,
      ChannelAction.ItemsByIdsRequest rqt) throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannel());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getUserId(), channel);
    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }
    if (node.isCollectionNode()) {
      throw new MMXException("Cannot get items from a collection channel",
          StatusCode.NOT_IMPLEMENTED.getCode());
    }

    LeafNode leafNode = (LeafNode) node;
    List<String> itemIds = rqt.getItemIds();
    if (itemIds == null || itemIds.isEmpty()) {
      throw new MMXException(StatusCode.BAD_REQUEST.getMessage("no item ID's"),
          StatusCode.BAD_REQUEST.getCode());
    }
    // Check if sender and subscriber JIDs match or if a valid "trusted proxy" is being used
    // Assumed that the owner of the subscription is the bare JID of the subscription JID.
    JID owner = from.asBareJID();
    if (!node.getAccessModel().canAccessItems(node, owner, from)) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
          StatusCode.FORBIDDEN.getCode());
    }
    // Check that the requester is not an outcast
    NodeAffiliate affiliate = node.getAffiliate(owner);
    if (affiliate != null && affiliate.getAffiliation() == NodeAffiliate.Affiliation.outcast) {
        throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
            StatusCode.FORBIDDEN.getCode());
    }

    // TODO: do we need to check for subscription first?
    List<MMXPublishedItem> mmxItems = new ArrayList<MMXPublishedItem>(itemIds.size());
    for (String itemId : itemIds) {
      if (itemId == null) {
        throw new MMXException(StatusCode.BAD_REQUEST.getMessage("null item ID"),
            StatusCode.BAD_REQUEST.getCode());
      }
      PublishedItem pubItem = leafNode.getPublishedItem(itemId);
      if (pubItem == null) {
        // Ignored the invalid item ID.
        continue;
      }
      MMXPublishedItem mmxItem = new MMXPublishedItem(pubItem.getID(),
            pubItem.getPublisher().toBareJID(),
            pubItem.getCreationDate(),
            pubItem.getPayloadXML());
      mmxItems.add(mmxItem);
    }
    ChannelAction.FetchResponse resp = new ChannelAction.FetchResponse(
        rqt.getUserId(), channel, mmxItems.size(), mmxItems);
    return resp;
  }

  public ChannelAction.SubscribersResponse getSubscribers(JID from, String appId,
      ChannelAction.SubscribersRequest rqt) throws MMXException {
    String channel = ChannelHelper.normalizePath(rqt.getChannel());
    String realChannel = ChannelHelper.makeChannel(appId, rqt.getUserId(), channel);
    Node node = mPubSubModule.getNode(realChannel);
    if (node == null) {
      throw new MMXException(StatusCode.CHANNEL_NOT_FOUND.getMessage(channel),
          StatusCode.CHANNEL_NOT_FOUND.getCode());
    }

    if (rqt.getUserId() != null) {
      //do the affiliation check only for personal channels
      JID requester = from.asBareJID();
      // Check if the requester has any affiliations but not outcast affiliation.
      NodeAffiliate nodeAffiliate = node.getAffiliate(requester);
      if (nodeAffiliate == null ||
          nodeAffiliate.getAffiliation() == NodeAffiliate.Affiliation.outcast) {
        throw new MMXException(StatusCode.FORBIDDEN.getMessage(channel),
            StatusCode.FORBIDDEN.getCode());
      }
    }

    Collection<NodeSubscription> allSubscriptions = node.getAllSubscriptions();
    /**
     * all subscriptions has all subscriptions in all possible states. We need
     * cull out subscriptions in state == subscribed.
     */
    int count = 0;
    TreeSet<String> subscriberUserNameSet = new TreeSet<String>();
    for (NodeSubscription ns : allSubscriptions) {
      if ((ns.getState() != null) && (ns.getState() == NodeSubscription.State.subscribed)) {
        JID subscriberJID = ns.getJID();
        String subscriberJIDNode = subscriberJID.getNode();
        String username = subscriberJIDNode;
        subscriberUserNameSet.add(username);
        count++;
      }
    }
    List<com.magnet.mmx.protocol.UserInfo> userInfoList = new LinkedList<com.magnet.mmx.protocol.UserInfo>();
    if(count > rqt.getOffset()) {
      UserDAO userDAO = new UserDAOImpl(getConnectionProvider());
      int addedCount = 0; //for applying the limit
      int index = 0;
      for (String username : subscriberUserNameSet) {
        if(index++ < rqt.getOffset()) {
          continue;
        }

        if (rqt.getLimit() > 0 && addedCount >= rqt.getLimit()) {
          break;
        }
        //TODO: Improve this
        UserEntity userEntity = userDAO.getUser(username);
        if (userEntity != null) {
          com.magnet.mmx.protocol.UserInfo userInfo = UserEntity.toUserInfo(userEntity);
          userInfoList.add(userInfo);
          addedCount++;
        }
      }
    }

    ChannelAction.SubscribersResponse resp = new ChannelAction.SubscribersResponse()
      .setTotal(count).setSubscribers(userInfoList);
    resp.setCode(StatusCode.SUCCESS.getCode())
      .setMessage(StatusCode.SUCCESS.getMessage());
    return resp;
  }


  public ChannelAction.SubscribersResponse getSubscribersFromNode(JID from,
                                                                  String appId,
                                                                  int offset,
                                                                  int size,
                                                          Node node) throws MMXException {
    Collection<NodeSubscription> allSubscriptions = node.getAllSubscriptions();
    /**
     * all subscriptions has all subscriptions in all possible states. We need
     * cull out subscriptions in state == subscribed.
     */
    int count = 0;
    TreeSet<String> subscriberUserNameSet = new TreeSet<String>();
    for (NodeSubscription ns : allSubscriptions) {
      if ((ns.getState() != null) && (ns.getState() == NodeSubscription.State.subscribed)) {
        JID subscriberJID = ns.getJID();
        String subscriberJIDNode = subscriberJID.getNode();
        String username = subscriberJIDNode;
        subscriberUserNameSet.add(username);
        count++;
      }
    }
    List<com.magnet.mmx.protocol.UserInfo> userInfoList = new LinkedList<com.magnet.mmx.protocol.UserInfo>();
    if(count > offset) {
      UserDAO userDAO = new UserDAOImpl(getConnectionProvider());
      int addedCount = 0; //for applying the limit
      int index = 0;
      for (String username : subscriberUserNameSet) {
        if(index++ < offset) {
          continue;
        }

        if (size > 0 && addedCount >= size) {
          break;
        }
        //TODO: Improve this
        UserEntity userEntity = userDAO.getUser(username);
        if (userEntity != null) {
          com.magnet.mmx.protocol.UserInfo userInfo = UserEntity.toUserInfo(userEntity);
          userInfoList.add(userInfo);
          addedCount++;
        }
      }
    }

    ChannelAction.SubscribersResponse resp = new ChannelAction.SubscribersResponse()
            .setTotal(count).setSubscribers(userInfoList);
    resp.setCode(StatusCode.SUCCESS.getCode())
            .setMessage(StatusCode.SUCCESS.getMessage());
    return resp;
  }

  public ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }

  private TopicRoleDAO getTopicRoleDAO() {
    return new TopicRoleDAOImpl(new OpenFireDBConnectionProvider());
  }

  /**
   * Check if user with supplied roles has access to the channel identified by nodeId.
   * This API shouldn't be called for personal channel. It works with global channels only.
   * @param nodeId
   * @param userRoles
   * @return
   */
  private boolean isAllowed (String nodeId, List<String> userRoles) {
    TopicRoleDAO roleDAO = getTopicRoleDAO();
    List<String> channelRoles  = roleDAO.getTopicRoles("pubsub", nodeId);
    boolean userHasRole = false;
    Iterator<String> userRoleIterator = userRoles.iterator();

    while (userRoleIterator.hasNext() && !userHasRole) {
      String userRole = userRoleIterator.next();
      int index = Collections.binarySearch(channelRoles, userRole);
      userHasRole = index > -1;
    }
    return userHasRole;
  }

  /**
   * Enum for the status codes
   */
  public static enum StatusCode {
    SUCCESS(200, "Success"),
    INVALID_COMMAND(400, "Invalid command: "),
    INVALID_DATE(400, "Invalid date"),
    CHANNEL_EXISTS(409, "Channel already exists: "),
    CHANNEL_NOT_FOUND(404, "Channel not found: "),
    APP_NODE_NOT_FOUND(500, "Internal error; application root node is missing: "),
    SUBSCRIPTION_EXISTS(409, "Subscription already exists: "),
    SUBSCRIPTION_NOT_FOUND(404, "Subscription not found: "),
    ITEM_NOT_FOUND(404, "Item not found: "),
    GONE(410, "Subscription is no longer available"),
    CONFLICT(409, "Conflict in the request"),
    FORBIDDEN(403, "Request is denied: "),
    BAD_REQUEST(400, "Bad Request"),
    VALUE_TOO_LARGE(400, "Bad Request: "),
    NOT_AUTHORIZED(401, "Not authorized: "),
    NOT_ACCEPTABLE(406, "Instant channel creation is disabled"),
    SERVER_ERROR(500, "Server error; please check the server log"),
    NOT_IMPLEMENTED(501, "Feature not implemented: "),
    INVALID_CHANNEL_NAME(400, "Channel name should be less than 50 characters long and can only have numbers, letters, hyphen, underscores, and dashes")
    ;

    private int code;
    private String message;

    StatusCode(int c, String m) {
      code = c;
      message = m;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public String getMessage(String arg) {
      return message + arg;
    }
  }
}

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXChannelUtil;
import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.privacy.PrivacyListManager;
import org.jivesoftware.openfire.pubsub.CollectionNode;
import org.jivesoftware.openfire.pubsub.LeafNode;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeAffiliate;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.jivesoftware.openfire.pubsub.NotAcceptableException;
import org.jivesoftware.openfire.pubsub.PubSubService;
import org.jivesoftware.openfire.pubsub.PublishedItem;
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

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXAttribute;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.SendLastPublishedItems;
import com.magnet.mmx.protocol.TagSearch;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicAction.ListType;
import com.magnet.mmx.protocol.TopicAction.MMXPublishedItem;
import com.magnet.mmx.protocol.TopicInfo;
import com.magnet.mmx.protocol.TopicSummary;
import com.magnet.mmx.server.api.v1.protocol.TopicCreateInfo;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DbInteractionException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TagDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TopicDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TopicEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TopicRoleDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TopicRoleDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.PubSubPersistenceManagerExt;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.TopicQueryBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicNode;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.AppTopic;
import com.magnet.mmx.util.TopicHelper;
import com.magnet.mmx.util.Utils;

public class MMXTopicManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(MMXAppManager.class);
  private static final boolean EXCLUDE_USER_TOPICS = true;
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

  private static MMXTopicManager sInstance = null;

  public static final String MISSING_APP_ROOT_TOPIC = "Root topic not found for selected app.";
  public static final String INVALID_TOPIC_ID = "Supplied topic id is invalid.";
  public static final String INVALID_TOPIC_NAME = "Supplied topic name is invalid.";
  public static final String DUPLICATE_TOPIC_ID = "Topic with supplied id exists.";

  protected MMXTopicManager() {
  }

  public static MMXTopicManager getInstance() {
    if (sInstance == null) {
      sInstance = new MMXTopicManager();
    }
    return sInstance;
  }

  List<String> listTopics(String parentNode) {
    Collection<Node> nodes = mPubSubModule.getNodes();
    ArrayList<String> result = new ArrayList<String>();
    for (Node node : nodes) {
      result.add(node.getNodeID());
    }
    return result;
  }

  /**
   * Get a list of Leaf Topic nodes for the passed in appId
   * (collection nodes aren't returned)
   * @param appId
   * @return
   */
  public List<TopicNode> listTopicsForAppId(String appId) {
    ArrayList<TopicNode> result = new ArrayList<TopicNode>();
    CollectionNode rootNode = getRootAppTopic(appId);
    if (rootNode != null) {
      Collection<Node> nodes = rootNode.getNodes();
      for (Node node : nodes) {
        //For fixing: https://magneteng.atlassian.net/browse/MOB-833
        if (node.isCollectionNode()) {
          continue;
        }
        String identifier = node.getNodeID();
        boolean isAppTopic = TopicHelper.isAppTopic(identifier, appId);
        if (isAppTopic) {
          TopicNode tn = TopicNode.build(appId, node);
          result.add(tn);
        }
      }
    }
    return result;
  }

  /**
   * Get a list of Leaf Topic nodes for the passed in appId
   * (collection nodes aren't returned)
   * @param appId
   * @return
   */
  public List<com.magnet.mmx.server.api.v1.protocol.TopicInfo> getTopicInfo(String appId) {
    ArrayList<com.magnet.mmx.server.api.v1.protocol.TopicInfo> result =
        new ArrayList<com.magnet.mmx.server.api.v1.protocol.TopicInfo>();
    CollectionNode rootNode = getRootAppTopic(appId);
    if (rootNode != null) {
      Collection<Node> nodes = rootNode.getNodes();
      for (Node node : nodes) {
        //For fixing: https://magneteng.atlassian.net/browse/MOB-833
        if (node.isCollectionNode()) {
          continue;
        }
        String identifier = node.getNodeID();
        boolean isAppTopic = TopicHelper.isAppTopic(identifier, appId);
        if (isAppTopic) {
          com.magnet.mmx.server.api.v1.protocol.TopicInfo info =
              getTopicInfoFromNode(appId, node);
          result.add(info);
        }
      }
    }
    return result;
  }

  public com.magnet.mmx.server.api.v1.protocol.TopicInfo getTopicInfoFromNode(
                  String appId, Node node) {
    com.magnet.mmx.server.api.v1.protocol.TopicInfo info = new
        com.magnet.mmx.server.api.v1.protocol.TopicInfo();
    info.setDescription(node.getDescription());
    info.setTopicName(node.getName());
    if(node instanceof  LeafNode) {
      LeafNode lnode = (LeafNode) node;
      info.setSubscriptionEnabled(lnode.isSubscriptionEnabled());
      info.setMaxItems(lnode.isPersistPublishedItems() ?
          lnode.getMaxPublishedItems() : 0);
    }
    info.setTopicId(TopicHelper.convertToId(node.getNodeID()));
    info.setPublisherType(node.getPublisherModel().getName());
    return info;
  }

  /**
   * Get a list of subscriptions for a specific topic
   * @param topicId
   * @return
   */
  public List<NodeSubscription> listSubscriptionsForTopic(String topicId) {
    String lowerCase = topicId.toLowerCase();
    LeafNode node = (LeafNode) mPubSubModule.getNode(lowerCase);
    Collection<NodeSubscription> rv = Collections.emptyList();
    if (node != null) {
      rv = node.getAllSubscriptions();
    }
    ArrayList<NodeSubscription> returnList = new ArrayList<NodeSubscription>(rv);
    return returnList;
  }

  public TopicActionResult createTopic(AppEntity entity, TopicCreateInfo topicInfo) {
    TopicActionResult result = new TopicActionResult();
    if (topicInfo.getTopicName() == null || topicInfo.getTopicName().isEmpty()) {
      result.setSuccess(false);
      result.setCode(TopicFailureCode.INVALID_TOPIC_NAME);
      result.setMessage("Topic id is either null or empty");
      return result;
    }
    if (topicInfo.getTopicName().length() > MMXServerConstants.MAX_TOPIC_NAME_LEN) {
      result.setSuccess(false);
      result.setCode(TopicFailureCode.INVALID_TOPIC_ID);
      result.setMessage("Supplied topic id exceeds the permitted maximum length of:" + MMXServerConstants.MAX_TOPIC_NAME_LEN);
      return result;
    }
    String topicName = topicInfo.getTopicName();
    topicName = TopicHelper.normalizePath(topicName);

    String appId = entity.getAppId();
    String topicId = TopicHelper.makeTopic(appId, topicInfo.isPersonalTopic() ?
        entity.getServerUserId() : null, topicName);

    /**
     * Get the appRootNode
     */
    CollectionNode appRootNode = getRootAppTopic(appId);
    if (appRootNode == null) {
      result = new TopicActionResult();
      result.setSuccess(false);
      result.setMessage(MISSING_APP_ROOT_TOPIC);
      result.setCode(TopicFailureCode.NOTFOUND);
      return result;
    }
    // create the requested topic
    Node newAppTopic = null;

    try {
      newAppTopic = createLeafNode(JIDUtil.makeNode(entity.getServerUserId(), appId),
          topicId, appRootNode, topicInfo);
      result.setSuccess(true);
      result.setNode(TopicNode.build(appId, newAppTopic));
      // Add role mappings
      List<String> roles = topicInfo.getRoles();
      if (roles == null || roles.isEmpty()) {
        roles = Collections.singletonList(MMXServerConstants.TOPIC_ROLE_PUBLIC);
      }
      TopicRoleDAO roleDAO = getTopicRoleDAO();
      roleDAO.addTopicRoles("pubsub", newAppTopic.getNodeID(), roles);
    } catch (NotAcceptableException e) {
      result.setSuccess(false);
      result.setCode(TopicFailureCode.UNKNOWN);
      result.setMessage(e.getMessage());
    } catch (TopicExistsException e) {
      result.setSuccess(false);
      result.setCode(TopicFailureCode.DUPLICATE);
      result.setMessage(DUPLICATE_TOPIC_ID);
    }
    return result;
  }
  /**
   * Delete a topic identified by a topic id.
   * @param appId The app ID for error message.
   * @param topicId The node ID.
   * @return
   */
  public TopicActionResult deleteTopic (String appId, String topicId) {

    TopicActionResult result = new TopicActionResult();

    Node gonner = mPubSubModule.getNode(topicId);

    if (gonner == null) {
      result.setSuccess(false);
      result.setCode(TopicFailureCode.NOTFOUND);
      result.setMessage(INVALID_TOPIC_ID);
    } else {
      LOGGER.trace("Deleting topic with id:" + topicId);
      gonner.delete();
      result.setSuccess(true);
      result.setNode(TopicNode.build(appId, gonner));
      result.setMessage(INVALID_TOPIC_ID);
    }
    return result;
  }

  /**
   * Get the root collection node for an app
   *
   * @param appId
   * @return
   */
  public CollectionNode getRootAppTopic(String appId) {
    Node result = mPubSubModule.getNode(appId);
    if (result != null && result.isCollectionNode()) {
      return (CollectionNode) result;
    } else {
      return null;
    }
  }

  /**
   * Get a Node representing the topic with the specified topicId.
   * @param topicId
   * @return Node if one exists false other wise.
   */
  public Node getTopicNode (String topicId){
    Node result = mPubSubModule.getNode(topicId);
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
   * Create a topic using the topic request.
   *
   * @param createUsername
   * @param topicId
   * @param parentNode
   * @throws TopicExistsException   -- if topic already exists
   * @throws NotAcceptableException -- if an exception is thrown by openfire during topic creation.
   */
  private LeafNode createLeafNode(String createUsername, String topicId,
                    CollectionNode parentNode, TopicCreateInfo topicInfo)
                        throws TopicExistsException, NotAcceptableException {

    String topicName = topicInfo.getTopicName();
    String topicDescription = topicInfo.getDescription();
    int maxItems = topicInfo.getMaxItems();

    Node result = mPubSubModule.getNode(topicId);
    if (result != null) {
      throw new TopicExistsException("Topic with id: " + topicId + " exists");
    }
    LeafNode createdNode = null;
    if (result == null) {
      // This config form should use the same values from setOptions().
      ConfigureForm form = new ConfigureForm(DataForm.Type.submit);
      form.setAccessModel(TopicHelper.isUserTopic(topicId) ?
          ConfigureForm.AccessModel.whitelist : ConfigureForm.AccessModel.open);
      form.setPersistentItems(maxItems != 0);
      form.setMaxItems(maxItems);
      form.setSendItemSubscribe(true);
      form.setMaxPayloadSize(Constants.MAX_PAYLOAD_SIZE);
      form.setDeliverPayloads(true);
      form.setNotifyRetract(false);
      form.setNotifyDelete(false);
      form.setNotifyConfig(false);
      form.setNodeType(ConfigureForm.NodeType.leaf);
      // TODO: default permission for topics created from console is subscribers
      TopicAction.PublisherType permission = topicInfo.getPublishPermission();
      if (permission == null) {
        permission = TopicAction.PublisherType.subscribers;
      }
      form.setPublishModel(ConfigureForm.convert(permission));
      form.setTitle(topicName);
      form.setSubscribe(topicInfo.isSubscriptionEnabled());
      if (topicDescription != null) {
        form.setDescription(topicDescription);
      }
//      LOGGER.trace("Leaf config form: "+form);

      JID jid = new JID(createUsername, mServer.getServerInfo().getXMPPDomain(), null);
      LeafNode node = new LeafNode(mPubSubModule, parentNode, topicId, jid);
      node.addOwner(jid);
      try {
        node.configure(form);
      } catch (NotAcceptableException e) {
        LOGGER.warn("NotAcceptableException", e);
        throw e;
      }

      if (topicInfo.isSubscribeOnCreate()) {
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
    return TopicHelper.TOPIC_DELIM + appId + TopicHelper.TOPIC_DELIM + "*" + TopicHelper.TOPIC_DELIM;
  }


  /**
   * Class that represents a result of a topic related action.
   */
  public static class TopicActionResult {
    boolean success;
    private TopicNode node;
    private String message;
    private TopicFailureCode code;

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

    public TopicFailureCode getCode() {
      return code;
    }

    public void setCode(TopicFailureCode code) {
      this.code = code;
    }
  }

  public static enum TopicFailureCode {
    DUPLICATE,
    NOTFOUND,
    UNKNOWN,
    INVALID_TOPIC_ID,
    INVALID_TOPIC_NAME
  }


  public static class TopicExistsException extends RuntimeException {
    public TopicExistsException(String message, Throwable cause) {
      super(message, cause);
    }

    public TopicExistsException(String message) {
      super(message);
    }
  }

  private void setOptions(String topicName, Node node, MMXTopicOptions options,
                          String appId) {
    ConfigureForm form = new ConfigureForm(DataForm.Type.submit);
    form.setSendItemSubscribe(true);
    form.setMaxPayloadSize(Constants.MAX_PAYLOAD_SIZE);
    form.setDeliverPayloads(true);
    form.setNotifyRetract(false);
    form.setNotifyDelete(false);
    form.setNotifyConfig(false);
    boolean isUserTopic = TopicHelper.isUserTopic(node.getNodeID());
    form.setAccessModel(isUserTopic ? ConfigureForm.AccessModel.whitelist :
        ConfigureForm.AccessModel.open);
    form.setNodeType(node.isCollectionNode() ?
        ConfigureForm.NodeType.collection : ConfigureForm.NodeType.leaf);
    if (options == null) {
      form.setPublishModel(ConfigureForm.PublishModel.open);
      form.setPersistentItems(true);
      form.setMaxItems(-1);
      form.setSubscribe(true);
      form.setTitle(topicName);
    } else {
      // Set the default values if not specified.
      options.fillWithDefaults();

      form.setPublishModel(ConfigureForm.convert(options.getPublisherType()));
      form.setPersistentItems(options.getMaxItems() != 0);
      form.setMaxItems(options.getMaxItems());
      form.setSubscribe(options.isSubscriptionEnabled());
      form.setTitle((options.getDisplayName() == null) ?
          topicName : options.getDisplayName());
      form.setDescription(options.getDescription());
    }
    try {
      node.configure(form);
      if (isUserTopic) {
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

  private JID getServerUser(String appId) {
    // Use LRU cache for server user JID.
    JID jid = mCachedServerUsers.get(appId);
    if (jid == null) {
      AppDAO appDAO = DBUtil.getAppDAO();
      String userId = appDAO.getServerUserForApp(appId);
      if (userId != null) {
        jid = JIDUtil.makeJID(userId, appId, null);
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
    String parentNodeId = TopicHelper.getParent(prefix, nodeId);
    CollectionNode parentNode;
    if (parentNodeId == null) {
      parentNode = getRootAppTopic(TopicHelper.getRootNodeId(nodeId));
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
   * Create an application-wide or personal topic and create all parent nodes
   * if needed.
   * @param from
   * @param appId
   * @param rqt
   * @return
   * @throws MMXException
   */
  public MMXStatus createTopic(JID from, String appId, TopicAction.CreateRequest rqt)
                          throws MMXException {
    String topic = rqt.getTopicName();
    try {
      // Note, TopicHelper.checkPathAllowed(topic) should not be called here;
      // it is a restriction in the client SDK.
      topic = TopicHelper.normalizePath(topic);
    } catch (IllegalArgumentException e) {
      throw new MMXException(e.getMessage(), StatusCode.BAD_REQUEST.getCode());
    }

    boolean isValid = TopicHelper.validateApplicationTopicName(topic);
    if (!isValid) {
      throw new MMXException(StatusCode.INVALID_TOPIC_NAME.getMessage(),
          StatusCode.INVALID_TOPIC_NAME.getCode());
    }
    String userId = JIDUtil.getUserId(from);
    JID owner = from.asBareJID();
    JID serverUser = getServerUser(appId);
    // Don't add server user if the creator is the server user already.
    JID[] owners = { owner, owner.equals(serverUser) ? null : serverUser };
    String topicId = TopicHelper.makeTopic(appId, rqt.isPersonal() ?
        userId : null, topic);
//    LOGGER.trace("createTopic realTopic="+realTopic+", topic="+topic);

    if (!mPubSubModule.canCreateNode(from)) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
          StatusCode.FORBIDDEN.getCode());
    }
    if (mPubSubModule.getNode(topicId) != null) {
      throw new MMXException(StatusCode.TOPIC_EXISTS.getMessage(topic),
          StatusCode.TOPIC_EXISTS.getCode());
    }
    if (!mPubSubModule.isInstantNodeSupported()) {
      throw new MMXException(StatusCode.NOT_ACCEPTABLE.getMessage(topic),
          StatusCode.NOT_ACCEPTABLE.getCode());
    }
    int prefix = TopicHelper.getPrefixLength(topicId);
    String parentId = TopicHelper.getParent(prefix, topicId);
    CollectionNode parent;
    if (parentId == null) {
      parent = getRootAppTopic(appId);
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
        throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
            StatusCode.FORBIDDEN.getCode());
      }
      if (parent.isMaxLeafNodeReached()) {
        throw new MMXException("Max nodes exceeded in the parent of "+topic,
            StatusCode.CONFLICT.getCode());
      }
    }

    synchronized(topicId.intern()) {
      if (mPubSubModule.getNode(topicId) != null) {
        throw new MMXException(StatusCode.TOPIC_EXISTS.getMessage(topic),
            StatusCode.TOPIC_EXISTS.getCode());
      }
//      LOGGER.trace("create node="+realTopic+", parent="+parent);
      Node node;
      if (rqt.isCollection()) {
        node = new CollectionNode(mPubSubModule, parent, topicId, from);
      } else {
        node = new LeafNode(mPubSubModule, parent, topicId, from);
      }
      // Add the creator as the owner.
      addOwnersToNode(node, owners);
      setOptions(topic, node, rqt.getOptions(), appId);

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
//    LOGGER.trace("create node="+realTopic+" success");

    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(StatusCode.SUCCESS.getMessage());
    return status;
  }

  // Owner is the requester and subscriber is where the item will be delivered
  // to.  They are the same user, but subscriber can be a full JID.
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

  /**
   * Delete a node recursively.  When an app is deleted, this method can be used
   * to remove the app node (i.e. appID) and its child nodes without specifying
   * the owner JID (use with care.)
   * @param node
   * @param owner
   * @return
   * @throws MMXException
   */
  public int deleteNode(Node node, JID owner) throws MMXException {
    if (owner != null && !node.getOwners().contains(owner)) {
      AppTopic topic = TopicHelper.parseTopic(node.getNodeID());
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic.getName()),
          StatusCode.FORBIDDEN.getCode());
    }
    int count = 0;
    if (node.isCollectionNode()) {
      for (Node child : node.getNodes()) {
        if (child.isCollectionNode()) {
          count += deleteNode(child, owner);
        } else {
          if (owner != null && !child.getOwners().contains(owner)) {
            AppTopic topic = TopicHelper.parseTopic(child.getNodeID());
            throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic.getName()),
                StatusCode.FORBIDDEN.getCode());
          }

//          LOGGER.trace("delete leaf node=" + node.getNodeID());
          // MAX-243: remove all privacy lists from the topic.
          deleteAllPrivacyListsForTopic(node);

          child.delete();
          // TODO: remove the tags

          ++count;
        }
      }
    }
    // MAX-243: remove all privacy lists from the topic.
    deleteAllPrivacyListsForTopic(node);

//    LOGGER.trace("delete node="+node.getNodeID());
    node.delete();
    // TODO: remove the tags

    ++count;
    return count;
  }

  // Get all subscribers and remove their privacy lists.
  private void deleteAllPrivacyListsForTopic(Node node) {
    // It may be expensive if there are many subscribers.  On the other
    // hand, if the topic is popular, it is unlikely to be deleted.
    PrivacyListManager plMgr = PrivacyListManager.getInstance();
    for (NodeSubscription sub : node.getAllSubscriptions()) {
      plMgr.deletePrivacyList(sub.getOwner().getNode(), node.getNodeID());
    }
  }

  public MMXStatus deleteTopic(JID from, String appId,
                          TopicAction.DeleteRequest rqt) throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopic());
    String userId = JIDUtil.getUserId(from);
    JID owner = from.asBareJID();
    String realTopic = TopicHelper.makeTopic(appId, rqt.isPersonal() ?
        userId : null, topic);
    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
    }
    int count = deleteNode(node, owner);
    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(count+" topic"+((count==1)?" is":"s are")+" deleted");
    return status;
  }

  public TopicInfo getTopic(JID from, String appId, MMXTopicId topic)
                          throws MMXException {
    String realTopic = TopicHelper.makeTopic(appId, topic.getEscUserId(),
        TopicHelper.normalizePath(topic.getName()));
    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic.getName()),
          StatusCode.TOPIC_NOT_FOUND.getCode());
    }
//    // A user can get the topic info if the topic is a global topic, or the owner
//    // of a user topic, or a subscriber to a user topic.
//    if (topic.isUserTopic() && !node.getOwners().contains(from.asBareJID()) &&
//        node.getSubscriptions(from.asBareJID()).size() > 0) {
//      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic.getName()),
//          StatusCode.FORBIDDEN.getCode());
//    }
    TopicInfo info = nodeToInfo(topic.getUserId(), topic.getName(), node);
    info.setPushMutedByUser(MMXPushConfigService.getInstance().isPushSuppressedByUser(JIDUtil.getUserId(from),
            TopicHelper.parseTopic(node.getNodeID()).getAppId(),
            TopicHelper.convertToId(node.getNodeID())));
    return info;
  }

  public List<TopicInfo> getTopics(JID from, String appId, List<MMXTopicId> topics)
                            throws MMXException {
    List<TopicInfo> infos = new ArrayList<TopicInfo>(topics.size());
    for (MMXTopicId topic : topics) {
      try {
        infos.add(getTopic(from, appId, topic));
      } catch (Throwable e) {
        infos.add(null);
      }
    }
    return infos;
  }

  public MMXStatus retractAllFromTopic(JID from, String appId,
      TopicAction.RetractAllRequest rqt) throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopic());
    String userId = JIDUtil.getUserId(from);
    JID owner = from.asBareJID();
    String realTopic = TopicHelper.makeTopic(appId, rqt.isPersonal() ?
        userId : null, topic);
    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
    }
    if (!node.getOwners().contains(owner)) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
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

  public Map<String, Integer> retractFromTopic(JID from, String appId,
      TopicAction.RetractRequest rqt) throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopic());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getUserId(), topic);
    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
    }
    if (node.isCollectionNode()) {
      throw new MMXException("Cannot retract items from a collection topic",
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
      throw new MMXException("Items required in this topic",
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
    if (!pubItems.isEmpty()) {
      leafNode.deleteItems(pubItems);
    }
    return results;
  }

  public TopicAction.SubscribeResponse subscribeTopic(JID from, String appId,
              TopicAction.SubscribeRequest rqt, List<String> userRoles) throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopic());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getUserId(), topic);
    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
    }

    JID owner = from.asBareJID();
    // The subscriber can specify a different resource or without resource.
    JID subscriber = new JID(from.getNode(), from.getDomain(), rqt.getDevId());

    AccessModel accessModel = node.getAccessModel();
    if (!accessModel.canSubscribe(node, owner, subscriber)) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
          StatusCode.FORBIDDEN.getCode());
    }

    // Check if the subscription owner is a user with outcast affiliation
    NodeAffiliate nodeAffiliate = node.getAffiliate(owner);
    if (nodeAffiliate != null &&
        nodeAffiliate.getAffiliation() == NodeAffiliate.Affiliation.outcast) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
          StatusCode.FORBIDDEN.getCode());
    }

    // Check that subscriptions to the node are enabled
    if (!node.isSubscriptionEnabled()) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
          StatusCode.FORBIDDEN.getCode());
    }
    /*
     * ensure user has the necessary role for subscribing to the topic.
     */
    boolean isSubScriptionAllowed = isAllowed(node.getNodeID(), userRoles);
    if (!isSubScriptionAllowed) {
      LOGGER.info("Subscription to Topic:{} not allowed for user with roles:{}", node.getNodeID(), userRoles);
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
          StatusCode.FORBIDDEN.getCode());
    }
    // Check for duplicated subscription; return error or existing subscription.
    NodeSubscription subscription;
    if ((subscription = node.getSubscription(subscriber)) != null) {
      if (rqt.isErrorOnDup()) {
        throw new MMXException(StatusCode.SUBSCRIPTION_EXISTS.getMessage(topic),
          StatusCode.SUBSCRIPTION_EXISTS.getCode());
      } else {
        TopicAction.SubscribeResponse resp = new TopicAction.SubscribeResponse(
            subscription.getID(), StatusCode.SUCCESS.getCode(),
            StatusCode.SUCCESS.getMessage());
        return resp;
      }
    }

    subscription = subscribeToNode(node, owner, subscriber);

    TopicAction.SubscribeResponse resp = new TopicAction.SubscribeResponse(
        subscription.getID(), StatusCode.SUCCESS.getCode(),
        StatusCode.SUCCESS.getMessage());
    return resp;
  }

  public MMXStatus unsubscribeTopic(JID from, String appId,
                  TopicAction.UnsubscribeRequest rqt) throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopic());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getUserId(), topic);
    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
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
    // MAX-243: remove the personal privacy list from the topic for all
    // subscriptions.  We should offer an option in the request instead of
    // relying on subscription ID.
    if (subId == null) {
      PrivacyListManager.getInstance().deletePrivacyList(from.getNode(), realTopic);
    }

    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(count+" subscription"+((count==1)?" is":"s are")+" cancelled");
    return status;
  }

  /**
   * Cancel all subscriptions to all topics for a device registered to a user.
   * This method is useful to cancel all subscriptions for a device before the
   * user unregisters the device.
   * @param from
   * @param appId
   * @param rqt
   * @return
   * @throws MMXException
   */
  public MMXStatus unsubscribeForDev(JID from, String appId,
                  TopicAction.UnsubscribeForDevRequest rqt) throws MMXException {
    final AtomicInteger count = new AtomicInteger(0);
    final JID owner = from.asBareJID();
    final String devId = rqt.getDevId();
    traverseNode(getRootAppTopic(appId), new NodeProcessor() {
      @Override
      public void process(Node node) {
        for (NodeSubscription subscription : node.getSubscriptions(owner)) {
          if (devId.equals(subscription.getJID().getResource())) {
            count.incrementAndGet();
            node.cancelSubscription(subscription);
          }
        }
      }
    });
    MMXStatus status = (new MMXStatus())
        .setCode(StatusCode.SUCCESS.getCode())
        .setMessage(count+" subscription"+((count.get()==1)?" is":"s are")+" cancelled");
    return status;
  }

  public TopicAction.ListResponse listTopics(JID from, String appId,
                  TopicAction.ListRequest rqt, List<String> userRoles) throws MMXException {
    TopicAction.ListResponse resp = new TopicAction.ListResponse();
    Integer maxLimit = rqt.getLimit();
    boolean recursive = rqt.isRecursive();
    String start = rqt.getStart();
    ListType type = rqt.getType();
    int limit = (maxLimit == null || maxLimit == -1) ? Integer.MAX_VALUE : maxLimit;
    String realTopic;
    if (start == null || start.isEmpty()) {
      // Get the global root node.
      Node node = mPubSubModule.getNode(appId);
      if (node == null) {
        throw new MMXException(StatusCode.APP_NODE_NOT_FOUND.getMessage(appId),
            StatusCode.APP_NODE_NOT_FOUND.getCode());
      }
      // Get its top level children nodes filtered by the search type.
      String userId = JIDUtil.getUserId(from);
      limit = getTopChildNodes(recursive, node, resp, limit, type, userId, userRoles);
    } else {
      // Filter by the global topics.
      if (type == ListType.global || type == ListType.both) {
        realTopic = TopicHelper.makeTopic(appId, null, start);
        // Get the global topic node first.
        Node node = mPubSubModule.getNode(realTopic);
        if (node == null) {
          throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(start),
                StatusCode.TOPIC_NOT_FOUND.getCode());
        }
        // Get its children nodes without search filter.
        limit = getChildNodes(recursive, node, resp, limit);
      }
      // Filter by the user topics.
      if (type == ListType.personal || type == ListType.both) {
        realTopic = TopicHelper.makeTopic(appId, JIDUtil.getUserId(from), start);
        // Get the personal topic node first.
        Node node = mPubSubModule.getNode(realTopic);
        if (node == null) {
          throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(start),
                StatusCode.TOPIC_NOT_FOUND.getCode());
        }
        // Get its children nodes without search filter.
        limit = getChildNodes(recursive, node, resp, limit);
      }
    }
    return resp;
  }

  private TopicInfo nodeToInfo(String userId, String topic, Node node) {
    TopicInfo info = new TopicInfo(userId,
        node.getName() != null ? node.getName() : topic, node.isCollectionNode())
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
          .setPublisherType(ConfigureForm.convert(leafNode.getPublisherModel()));
    } else {
      info.setMaxItems(0)
          .setMaxPayloadSize(0)
          .setPersistent(false)
          .setPublisherType(null);
    }
    return info;
  }

  // Get all top level children nodes which are either global topics or user
  // topics.  Filter out the nodes by the search type.
  private int getTopChildNodes(boolean recursive, Node root,
      TopicAction.ListResponse resp, int limit, ListType type, String userId, List<String> roles) {
    boolean globalOnly = (type == ListType.global);
    if (roles == null || roles.isEmpty()) {
      roles = Collections.singletonList(MMXServerConstants.TOPIC_ROLE_PUBLIC);
    }
    for (Node child : root.getNodes()) {
      AppTopic topic = TopicHelper.parseTopic(child.getNodeID());
      if (topic == null) {
        LOGGER.warn("Ignore malformed topic: " + child.getNodeID());
        continue;
      }
      // Brain teaser: the xor below is actually same as
      //    (type == ListType.global && !topic.isUserTopic()) ||
      //    (type == ListType.personal && topic.isUserTopic()))
      if ((type == ListType.both) || (globalOnly ^ topic.isUserTopic())) {
        if (topic.isUserTopic() && !topic.getEscUserId().equals(userId)) {
          continue;
        }
        if (--limit < 0) {
          return limit;
        }
        //resp.add(nodeToInfo(topic.getUserId(), topic.getName(), child));
        /**
         * Add a check to see if the topic role mapping allows current user's roles
         * to access this topic. The check should be done only for global topics
         */
        if (!topic.isUserTopic()) {
          boolean userHasRole = isAllowed(child.getNodeID(), roles);
          if (userHasRole) {
            resp.add(nodeToInfo(topic.getUserId(), topic.getName(), child));
          }
        } else {
          resp.add(nodeToInfo(topic.getUserId(), topic.getName(), child));
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
      TopicAction.ListResponse resp, int limit) {
    if (!node.isCollectionNode()) {
      AppTopic topic = TopicHelper.parseTopic(node.getNodeID());
      if (topic == null) {
        LOGGER.warn("Ignore malformed topic: " + node.getNodeID());
      } else {
        if (--limit < 0) {
          return limit;
        }
        resp.add(nodeToInfo(topic.getUserId(), topic.getName(), node));
      }
    } else {
      for (Node child : node.getNodes()) {
        AppTopic topic = TopicHelper.parseTopic(child.getNodeID());
        if (topic == null) {
          LOGGER.warn("Ignore malformed topic: " + child.getNodeID());
          continue;
        }
        if (--limit < 0) {
          return limit;
        }
        resp.add(nodeToInfo(topic.getUserId(), topic.getName(), child));
        if (recursive && child.isCollectionNode()) {
              limit = getChildNodes(recursive, child, resp, limit);
        }
      }
    }
    return limit;
  }

  private String[] getTopics(String appId, List<MMXTopicId> list,
                              int begin, int size) {
    if (list == null) {
      return null;
    }
    size = Math.min(size, list.size() - begin);
    String[] topics = new String[size];
    for (int i = 0, index = begin; --size >= 0; index++, i++) {
      MMXTopicId topicNode = list.get(index);
      topics[i] = TopicHelper.makeTopic(appId, topicNode.getEscUserId(),
                                        topicNode.getName());
    }
    return topics;
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

  public TopicAction.SummaryResponse getSummary(JID from, String appId,
          TopicAction.SummaryRequest rqt) throws MMXException {
    // Build a collection of topic ID's from the request; it contains topics
    // without any published items.
    HashSet<MMXTopicId> tpNoItems = new HashSet<MMXTopicId>(rqt.getTopicNodes().size());
    for (MMXTopicId topicId : rqt.getTopicNodes()) {
      tpNoItems.add(topicId);
    }
    TopicAction.SummaryResponse resp = new TopicAction.SummaryResponse(
        rqt.getTopicNodes().size());
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      con = DbConnectionManager.getConnection();
      int start = 0;
      int numOfTopics = rqt.getTopicNodes().size();
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
        // Limit to 128 topics per query because some DBMS cannot handle more
        // than 255 arguments in the IN clause.
        String[] topics = getTopics(appId, rqt.getTopicNodes(), start, 128);
        if (topics == null || topics.length == 0) {
          break;
        }
        String argList = SQLHelper.generateArgList(topics.length);
        String sql = "SELECT ofPubsubNode.maxItems,count(*),max(ofPubsubItem.creationDate),ofPubsubNode.nodeId, ofPubsubNode.name "+
                     "FROM ofPubsubItem, ofPubsubNode " +
                     "WHERE ofPubsubItem.serviceID=? AND " +
                     "      ofPubsubItem.nodeID = ofPubsubNode.nodeId AND " +
                     "      ofPubsubItem.nodeID IN ("+argList+") "+dateRange+
                     "GROUP BY ofPubsubItem.nodeID";
        pstmt = con.prepareStatement(sql);
        pstmt.setString(1, mPubSubModule.getServiceID());
        SQLHelper.bindArgList(pstmt, 2, topics);
        SQLHelper.bindArgList(pstmt, 2+topics.length, dates);
        rs = pstmt.executeQuery();
        while (rs.next()) {
          int maxItems = rs.getInt(1);
          int count = rs.getInt(2);
          Date creationDate = new Date(Long.parseLong(rs.getString(3).trim()));
          String nodeId = rs.getString(4);
          MMXTopicId topicId = TopicHelper.parseNode(nodeId);
          String topicName = rs.getString(5);
          MMXTopicId topicIdWithOriginalName = new MMXTopicId(topicId.getUserId(), topicName);
          resp.add(new TopicSummary(topicIdWithOriginalName)
            .setCount((maxItems < 0) ? count : Math.min(maxItems, count))
            .setLastPubTime(creationDate));
          // This topic has published items; remove it from the collection.
          tpNoItems.remove(topicId);
        }
        start += topics.length;
      } while (start < numOfTopics);
      // Fill the response with the topics having no published items.
      Iterator<MMXTopicId> it = tpNoItems.iterator();
      while (it.hasNext()) {
        resp.add(new TopicSummary(it.next()).setCount(0));
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

  public List<TopicInfo> searchByTags(JID from, String appId,
                      TagSearch rqt) throws MMXException {
    List<TopicEntity> entities;
    List<String> tags = rqt.getTags();
    TopicDAO topicDao = DBUtil.getTopicDAO();
    switch(rqt.getOperator()) {
    case AND:
      entities = topicDao.getTopicsForTagAND(tags, appId);
      break;
    case OR:
      entities = topicDao.getTopicsForTagOR(tags, appId);
      break;
    default:
      entities = new ArrayList<TopicEntity>(0);
      break;
    }
    // Convert the topic entity to topic info.
    List<TopicInfo> res = new ArrayList<TopicInfo>(
        entities.size());
    for (TopicEntity entity : entities) {
      MMXTopicId topic = TopicHelper.parseNode(entity.getNodeId());
      TopicInfo info =  new TopicInfo(topic.getUserId(), topic.getName(), !entity.isLeaf())
        .setId(TopicHelper.convertToId(entity.getNodeId()))
        .setDisplayName(entity.getName())
        .setDescription(entity.getDescription())
        .setCreationDate(entity.getCreationDate())
        .setMaxItems(entity.isPersistItems() ? entity.getMaxItems() : 0)
        .setMaxPayloadSize(entity.getMaxPayloadSize())
        .setModifiedDate(entity.getModificationDate())
        .setPublisherType(ConfigureForm.convert(
                PublisherModel.valueOf(entity.getPublisherModel())))
        .setPersistent(entity.isPersistItems())
        .setCreator(entity.getCreator())
        .setSubscriptionEnabled(entity.isSubscriptionEnabled());
      res.add(info);
    }
    return res;
  }

  public TopicAction.TopicTags getTags(JID from, String appId,
                      MMXTopicId rqt) throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getName());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getEscUserId(), topic);
    Node node = mPubSubModule.getNode(realTopic);
    // No need to check for permission; just check for existing.
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
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

    TopicAction.TopicTags topicTags = new TopicAction.TopicTags(rqt.getUserId(),
        rqt.getName(), tags, new Date());
    return topicTags;
  }

  public MMXStatus setTags(JID from, String appId, TopicAction.TopicTags rqt)
                      throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopicName());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getUserId(), topic);
    Node node = mPubSubModule.getNode(realTopic);
    // No need to check for permission; just check for existing.
    if (node == null) {
      LOGGER.trace("setTags : node not found appId={},  topic={}", appId, realTopic);
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
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

  public MMXStatus addTags(JID from, String appId, TopicAction.TopicTags rqt)
                      throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopicName());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getUserId(), topic);
    Node node = mPubSubModule.getNode(realTopic);
    // No need to check for permission; just check for existing.
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
    }
    List<String> tags = rqt.getTags();

    String serviceId = node.getService().getServiceID();
    String nodeId = node.getNodeID();

    if(!Utils.isNullOrEmpty(tags)) {
      TagDAO tagDao = DBUtil.getTagDAO();
      for(String tag : tags) {
        try {
          LOGGER.trace("addTags : creating topic setting tag={}, appId={}, serviceId={}, nodeId={}",
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

  public MMXStatus removeTags(JID from, String appId, TopicAction.TopicTags rqt)
                      throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopicName());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getUserId(), topic);
    Node node = mPubSubModule.getNode(realTopic);
    // No need to check for permission; just check for existing.
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
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

  @Deprecated
  public TopicAction.TopicQueryResponse queryTopic(JID from, String appId,
      TopicAction.TopicQueryRequest rqt) throws MMXException {
    String userId = JIDUtil.getUserId(from);
    int offset = rqt.getOffset();
    int maxItems = rqt.getLimit();
    List<MMXAttribute<TopicAction.TopicAttr>> criteria = rqt.getCriteria();
    TopicAction.TopicQueryResponse resp = PubSubPersistenceManagerExt.searchTopic(
            userId, appId, offset, maxItems, criteria);
    return resp;
  }

  public TopicAction.TopicQueryResponse searchTopic(JID from, String appId,
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

    List<TopicInfo> topicList = new ArrayList<TopicInfo>(results.getResults().size());

    for (TopicAction.TopicInfoWithSubscriptionCount ti : results.getResults()) {
      TopicInfo info = new TopicInfo(ti.getUserId(), ti.getName(), ti.isCollection())
        .setId(ti.getId())
        .setDisplayName(ti.getDisplayName())
        .setDescription(ti.getDescription())
        .setCreationDate(ti.getCreationDate())
        .setModifiedDate(ti.getModifiedDate())
        .setPublisherType(ti.getPublisherType())
        .setMaxPayloadSize(ti.getMaxPayloadSize())
        .setMaxItems(ti.isPersistent() ? ti.getMaxItems() : 0)
        .setPersistent(ti.isPersistent())
        .setCreator(ti.getCreator())
        .setSubscriptionEnabled(ti.isSubscriptionEnabled())
        .setPushMutedByUser(MMXPushConfigService.getInstance().isPushSuppressedByUser(JIDUtil.getUserId(from), appId, ti.getId()));

      topicList.add(info);
    }
    TopicAction.TopicQueryResponse resp = new TopicAction.TopicQueryResponse(results.getTotal(), topicList);
    return resp;
  }

  public MMXStatus processSendLastPublishedItems(JID from, String appId,
                          SendLastPublishedItems rqt) throws MMXException {
    Date since = rqt.getSince();
    if (since == null) {
      // Missing "since" here
      throw new MMXException(StatusCode.INVALID_DATE.getMessage(),
          StatusCode.INVALID_DATE.getCode());
    }

    MMXStatus status;
    if (rqt.getTopic() == null) {
      // Get the last published items from all subscribed nodes.
      status = sendLastPublishedItemsFromNodes(from, appId, since,
          rqt.getMaxItems());
    } else {
      // Get the last published items from a subscribed node.
      String topic = TopicHelper.normalizePath(rqt.getTopic().getName());
      String realTopic = TopicHelper.makeTopic(appId,
          rqt.getTopic().getEscUserId(), topic);
      status = sendLastPublishedItemsFromNode(from, realTopic, since,
          rqt.getMaxItems());
    }
    return status;
  }

  private MMXStatus sendLastPublishedItemsFromNode(JID from, String realTopic,
                                                    Date since, int maxItems) {
    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      MMXStatus status = new MMXStatus().setCode(StatusCode.TOPIC_NOT_FOUND.getCode())
          .setMessage(StatusCode.TOPIC_NOT_FOUND.getMessage());
      return status;
    }
    NodeSubscription sub = node.getSubscription(from.asBareJID());
    if (sub == null) {
      MMXStatus status = new MMXStatus().setCode(StatusCode.SUBSCRIPTION_NOT_FOUND.getCode())
          .setMessage(StatusCode.SUBSCRIPTION_NOT_FOUND.getMessage());
      return status;
    }
    if (node.isCollectionNode()) {
      MMXStatus status = new MMXStatus().setCode(StatusCode.BAD_REQUEST.getCode())
          .setMessage("Cannot get latest published items from a collection node");
      return status;
    }
    int numSent = 0;
    if (maxItems == 1) {
      PublishedItem item = node.getLastPublishedItem();
      if (item != null && item.getCreationDate().getTime() >= since.getTime()) {
        if (sendLastPublishedItem(item, sub, from)) {
          ++numSent;
        }
      }
    } else {
      List<PublishedItem> items = PubSubPersistenceManagerExt.getPublishedItems(from,
              (LeafNode) node, maxItems, since);
      if (items != null) {
        for (PublishedItem item : items) {
          if (sendLastPublishedItem(item, sub, from)) {
            ++numSent;
          }
        }
      }
    }
    MMXStatus status = (new MMXStatus())
        .setCode(Constants.STATUS_CODE_200)
        .setMessage("1 subscription; "+numSent+" published items sent");
    return status;
  }

  private interface NodeProcessor {
    public void process(Node node);
  }

  private void traverseNode(CollectionNode collectNode, NodeProcessor processor) {
    for (Node node : collectNode.getNodes()) {
      processor.process(node);
      if (node.isCollectionNode()) {
        traverseNode((CollectionNode) node, processor);
      }
    }
  }

  private MMXStatus sendLastPublishedItemsFromNodes(final JID from, String appId,
                                        final Date since, final int maxItems) {
    final JID subscriber = from.asBareJID();
    String prefix = TopicHelper.makePrefix(appId);

    // Don't query the DB directly because some items are cached in memory.
    // Besides, it is faster looping through all cached nodes in memory than
    // query the ofPubsubSubscription table because # of nodes should be many
    // less than # of subscriptions.
    // TODO: optimize it to traverse from the appID root node.

    // Find all collection nodes subscribed by the user.
    final TreeMap<String, Node> colNodes = new TreeMap<String, Node>();
    traverseNode(getRootAppTopic(appId), new NodeProcessor() {
      @Override
      public void process(Node node) {
        if (node.isCollectionNode()) {
          Collection<NodeSubscription> subs = node.getSubscriptions(subscriber);
          if (subs != null && subs.size() > 0) {
            colNodes.put(node.getNodeID(), node);
            LOGGER.trace("Collection node=" + node.getNodeID() + " is subscribed");
          }
        }
      }
    });

    final AtomicInteger numSent = new AtomicInteger(0), numSubs = new AtomicInteger(0);
    if (maxItems == 1) {
      traverseNode(getRootAppTopic(appId), new NodeProcessor() {
        @Override
        public void process(Node node) {
          // Check the leaf node if its last published item should be sent.
          PublishedItem item = node.getLastPublishedItem();
//          if (item == null) {
//            LOGGER.trace("No published items in subscribed node="+node.getNodeID());
//          } else {
//            LOGGER.trace("since="+since.getTime()+", creatDate="+item.getCreationDate().getTime()+
//                ", node="+node.getNodeID()+", last pub item="+item.getID());
//          }
          if (item == null || item.getCreationDate().getTime() < since.getTime()) {
            // Skip all last published items older than the last delivery time.
            return;
          }
          Collection<NodeSubscription> subs = node.getSubscriptions(subscriber);
          if (subs == null || subs.size() == 0) {
            // The leaf node has no subscriptions, check its ancestor for subscriptions.
            Node ancestor = findAncestor(colNodes, node);
            if (ancestor == null) {
              return;
            }
            subs = ancestor.getSubscriptions(subscriber);
          }
          // Either the leaf node or ancestor node has subscriptions with the
          // latest published item.
          numSubs.addAndGet(subs.size());
          for (NodeSubscription sub : subs) {
            if (sendLastPublishedItem(item, sub, from)) {
              numSent.incrementAndGet();
//              LOGGER.trace("Sent last published item="+item.getID()+
//                          ", sub ID="+sub.getID());
            } else {
//              LOGGER.trace("cannot send last published item="+item.getID()+
//                          ", sub ID="+sub.getID());
            }
          }
        }
      });
    }
    else {
      traverseNode(getRootAppTopic(appId), new NodeProcessor() {
        @Override
        public void process(Node node) {
          if (node.isCollectionNode()) {
            return;
          }
          Collection<NodeSubscription> subs = node.getSubscriptions(subscriber);
          if (subs == null || subs.size() == 0) {
            // The leaf node is not subscribed, check its ancestor for subscriptions.
            Node ancestor = findAncestor(colNodes, node);
            if (ancestor == null) {
//              LOGGER.trace("No ancestor node is subscribed, skip "+node.getNodeID());
              return;
            }
            // Get the subscriptions from the ancestor.
            subs = ancestor.getSubscriptions(subscriber);
            if (subs.size() == 0) {
//              LOGGER.trace("No subscriptions in ancestor node "+ancestor.getNodeID());
              return;
            }
          }

//          LOGGER.trace("Fetch published items from "+node.getNodeID()+", since="+since);
          List<PublishedItem> items = PubSubPersistenceManagerExt.getPublishedItems(from,
              (LeafNode) node, maxItems, since);
          if (items == null || items.size() == 0) {
//            LOGGER.trace("No published items in "+node.getNodeID()+", since="+since);
            return;
          }

          // Either the leaf node or ancestor node has subscriptions with the
          // latest published item.
          numSubs.addAndGet(subs.size());
          for (NodeSubscription sub : subs) {
            if (sendLastPublishedItems(items, sub, from)) {
              numSent.addAndGet(items.size());
//              LOGGER.trace("Sent last published #items="+items.size()+
//                          ", sub ID="+sub.getID());
            } else {
//              LOGGER.warn("cannot send last published items="+items.size()+
//                          ", sub ID="+sub.getID());
            }
          }
        }
      });
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
   * Create the <code>all</code> version topic of an OS.  All its parent topics
   * will be created if they do not exist.
   * @param creatorUserId The app server user ID.
   * @param appId The app ID.
   * @param osType The non-null OS type.
   * @param displayName The display name of the "all" versions.
   * @return
   */
  public MMXStatus createOSTopic(String creatorUserId, String appId,
                                  OSType osType, String displayName) {
    if (osType == null) {
      throw new NullPointerException("OS type is null");
    }
    String allTopic = TopicHelper.makeOSTopic(osType, TopicHelper.TOPIC_LEAF_ALL);
    MMXTopicOptions options = new MMXTopicOptions().setMaxItems(-1);
    TopicAction.CreateRequest rqt = new TopicAction.CreateRequest(allTopic, false,
                options);
    try {
      // create the OS and all version topics
      JID jid = new JID(creatorUserId, mServer.getServerInfo().getXMPPDomain(), null);
      MMXStatus status = createTopic(jid, appId, rqt);
      return status;
    } catch (MMXException e) {
      e.printStackTrace();
      return new MMXStatus().setCode(e.getCode()).setMessage(e.getMessage());
    }
  }

  /**
   * Delete the OS topic and its children, or a specific OS type topic.
   * @param creatorUserId The app server user ID.
   * @param appId The app ID.
   * @param osType null for all OS, or a specific OS type.
   * @return
   */
  public MMXStatus deleteOSTopic(String creatorUserId, String appId, OSType osType) {
    String topic = TopicHelper.makeOSTopic(osType, null);
    TopicAction.DeleteRequest rqt = new TopicAction.DeleteRequest(topic, false);
    try {
      // delete the OS topic and its children.
      JID jid = new JID(creatorUserId, mServer.getServerInfo().getXMPPDomain(), null);
      MMXStatus status = deleteTopic(jid, appId, rqt);
      return status;
    } catch (MMXException e) {
      e.printStackTrace();
      return new MMXStatus().setCode(e.getCode()).setMessage(e.getMessage());
    }
  }

  public TopicAction.FetchResponse fetchItems(JID from, String appId,
            TopicAction.FetchRequest rqt) throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopic());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getUserId(), topic);
    TopicAction.FetchOptions options = rqt.getOptions();
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

    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
    }
    if (node.isCollectionNode()) {
      throw new MMXException("Cannot fetch items from a collection topic",
          StatusCode.NOT_IMPLEMENTED.getCode());
    }
    // Check if sender and subscriber JIDs match or if a valid "trusted proxy" is being used
    // Assumed that the owner of the subscription is the bare JID of the subscription JID.
    JID owner = from.asBareJID();
    if (!node.getAccessModel().canAccessItems(node, owner, from)) {
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
          StatusCode.FORBIDDEN.getCode());
    }
    // Check that the requester is not an outcast
    NodeAffiliate affiliate = node.getAffiliate(owner);
    if (affiliate != null && affiliate.getAffiliation() == NodeAffiliate.Affiliation.outcast) {
        throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
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
        throw new MMXException(StatusCode.NOT_AUTHORIZED.getMessage("topic not suscribed"),
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
    TopicAction.FetchResponse resp = new TopicAction.FetchResponse(
        rqt.getUserId(), topic, total, mmxItems);
    return resp;
  }

  public TopicAction.FetchResponse getItems(JID from, String appId,
      TopicAction.ItemsByIdsRequest rqt) throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopic());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getUserId(), topic);
    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
    }
    if (node.isCollectionNode()) {
      throw new MMXException("Cannot get items from a collection topic",
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
      throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
          StatusCode.FORBIDDEN.getCode());
    }
    // Check that the requester is not an outcast
    NodeAffiliate affiliate = node.getAffiliate(owner);
    if (affiliate != null && affiliate.getAffiliation() == NodeAffiliate.Affiliation.outcast) {
        throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
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
    TopicAction.FetchResponse resp = new TopicAction.FetchResponse(
        rqt.getUserId(), topic, mmxItems.size(), mmxItems);
    return resp;
  }

  public TopicAction.SubscribersResponse getSubscribers(JID from, String appId,
      TopicAction.SubscribersRequest rqt) throws MMXException {
    String topic = TopicHelper.normalizePath(rqt.getTopic());
    String realTopic = TopicHelper.makeTopic(appId, rqt.getUserId(), topic);
    Node node = mPubSubModule.getNode(realTopic);
    if (node == null) {
      throw new MMXException(StatusCode.TOPIC_NOT_FOUND.getMessage(topic),
          StatusCode.TOPIC_NOT_FOUND.getCode());
    }

    if (rqt.getUserId() != null) {
      //do the affiliation check only for personal topics
      JID requester = from.asBareJID();
      // Check if the requester has any affiliations but not outcast affiliation.
      NodeAffiliate nodeAffiliate = node.getAffiliate(requester);
      if (nodeAffiliate == null ||
          nodeAffiliate.getAffiliation() == NodeAffiliate.Affiliation.outcast) {
        throw new MMXException(StatusCode.FORBIDDEN.getMessage(topic),
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
        if (userEntity == null) {
          // Skip any invalid users.
          --index;
          continue;
        }
        com.magnet.mmx.protocol.UserInfo userInfo = UserEntity.toUserInfo(userEntity);
        userInfoList.add(userInfo);
        addedCount++;
      }
    }

    TopicAction.SubscribersResponse resp = new TopicAction.SubscribersResponse()
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
   * Check if user with supplied roles has access to the topic identified by nodeId.
   * This API shouldn't be called for personal topic. It works with global topics only.
   * @param nodeId
   * @param userRoles
   * @return
   */
  private boolean isAllowed (String nodeId, List<String> userRoles) {
    TopicRoleDAO roleDAO = getTopicRoleDAO();
    List<String> topicRoles  = roleDAO.getTopicRoles("pubsub", nodeId);
    boolean userHasRole = false;
    Iterator<String> userRoleIterator = userRoles.iterator();

    while (userRoleIterator.hasNext() && !userHasRole) {
      String userRole = userRoleIterator.next();
      int index = Collections.binarySearch(topicRoles, userRole);
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
    TOPIC_EXISTS(409, "Topic already exists: "),
    TOPIC_NOT_FOUND(404, "Topic not found: "),
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
    NOT_ACCEPTABLE(406, "Instant topic creation is disabled"),
    SERVER_ERROR(500, "Server error; please check the server log"),
    NOT_IMPLEMENTED(501, "Feature not implemented: "),
    INVALID_TOPIC_NAME(400, "Channel name should be less than 50 characters long and can only have numbers, letters, hyphen, underscores, and dashes")
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

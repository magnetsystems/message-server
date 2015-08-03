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

import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.pubsub.models.PublisherModel;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;

import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicAction.PublisherType;

/**
 * @hide
 * Configuration form for PubSub node in MMX server.
 */
public class ConfigureForm extends DataForm {
  public static enum ConfigureNodeFields {
    /**
     * Determines who may subscribe and retrieve items
     * 
     * <p>
     * <b>Value: {@link AccessModel}</b>
     * </p>
     */
    access_model,

    /**
     * The URL of an XSL transformation which can be applied to payloads in
     * order to generate an appropriate message body element
     * 
     * <p>
     * <b>Value: {@link URL}</b>
     * </p>
     */
    body_xslt,

    /**
     * The collection with which a node is affiliated
     * 
     * <p>
     * <b>Value: String</b>
     * </p>
     */
    collection,

    /**
     * The URL of an XSL transformation which can be applied to payload format
     * in order to generate a valid Data Forms result that the client could
     * display using a generic Data Forms rendering engine body element.
     * 
     * <p>
     * <b>Value: {@link URL}</b>
     * </p>
     */
    dataform_xslt,

    /**
     * Whether to deliver payloads with event notifications
     * 
     * <p>
     * <b>Value: boolean</b>
     * </p>
     */
    deliver_payloads,

    /**
     * Whether owners or publisher should receive replies to items
     * 
     * <p>
     * <b>Value: {@link ItemReply}</b>
     * </p>
     */
    itemreply,

    /**
     * Who may associate leaf nodes with a collection
     * 
     * <p>
     * <b>Value: {@link ChildrenAssociationPolicy}</b>
     * </p>
     */
    children_association_policy,

    /**
     * The list of JIDs that may associate leaf nodes with a collection
     * 
     * <p>
     * <b>Value: List of JIDs as Strings</b>
     * </p>
     */
    children_association_whitelist,

    /**
     * The child nodes (leaf or collection) associated with a collection
     * 
     * <p>
     * <b>Value: List of Strings</b>
     * </p>
     */
    children,

    /**
     * The maximum number of child nodes that can be associated with a
     * collection
     * 
     * <p>
     * <b>Value: int</b>
     * </p>
     */
    children_max,

    /**
     * The description of this node.
     */
    description,
    
    /**
     * The maximum number of items to persist
     * 
     * <p>
     * <b>Value: int</b>
     * </p>
     */
    max_items,

    /**
     * The maximum payload size in bytes
     * 
     * <p>
     * <b>Value: int</b>
     * </p>
     */
    max_payload_size,

    /**
     * Whether the node is a leaf (default) or collection
     * 
     * <p>
     * <b>Value: {@link NodeType}</b>
     * </p>
     */
    node_type,

    /**
     * Whether to notify subscribers when the node configuration changes
     * 
     * <p>
     * <b>Value: boolean</b>
     * </p>
     */
    notify_config,

    /**
     * Whether to notify subscribers when the node is deleted
     * 
     * <p>
     * <b>Value: boolean</b>
     * </p>
     */
    notify_delete,

    /**
     * Whether to notify subscribers when items are removed from the node
     * 
     * <p>
     * <b>Value: boolean</b>
     * </p>
     */
    notify_retract,

    /**
     * Whether to persist items to storage. This is required to have multiple
     * items in the node.
     * 
     * <p>
     * <b>Value: boolean</b>
     * </p>
     */
    persist_items,

    /**
     * Whether to deliver notifications to available users only
     * 
     * <p>
     * <b>Value: boolean</b>
     * </p>
     */
    presence_based_delivery,

    /**
     * Defines who can publish to the node
     * 
     * <p>
     * <b>Value: {@link PublishModel}</b>
     * </p>
     */
    publish_model,

    /**
     * The specific multi-user chat rooms to specify for replyroom
     * 
     * <p>
     * <b>Value: List of JIDs as Strings</b>
     * </p>
     */
    replyroom,

    /**
     * The specific JID(s) to specify for replyto
     * 
     * <p>
     * <b>Value: List of JIDs as Strings</b>
     * </p>
     */
    replyto,

    /**
     * The roster group(s) allowed to subscribe and retrieve items
     * 
     * <p>
     * <b>Value: List of strings</b>
     * </p>
     */
    roster_groups_allowed,

    /**
     * <p>
     * <b>Value: boolean</b>
     * </p>
     */
    send_item_subscribe,

    /**
     * Whether to allow subscriptions
     * 
     * <p>
     * <b>Value: boolean</b>
     * </p>
     */
    subscribe,

    /**
     * A friendly name for the node
     * 
     * <p>
     * <b>Value: String</b>
     * </p>
     */
    title,

    /**
     * The type of node data, ussually specified by the namespace of the
     * payload(if any);MAY be a list-single rather than a text single
     * 
     * <p>
     * <b>Value: String</b>
     * </p>
     */
    type;

    public String getFieldName() {
      return "pubsub#" + toString();
    }
  }

  public static enum AccessModel {
    /** Anyone may subscribe and retrieve items */
    open,

    /**
     * Subscription request must be approved and only subscribers may retrieve
     * items
     */
    authorize,

    /**
     * Anyone with a presence subscription of both or from may subscribe and
     * retrieve items
     */
    presence,

    /** Anyone in the specified roster group(s) may subscribe and retrieve items */
    roster,

    /** Only those on a whitelist may subscribe and retrieve items */
    whitelist;
  }

  public static enum NodeType {
    leaf,
    collection;
  }

  public static enum PublishModel {
    /** Only publishers may publish */
    publishers,

    /** Only subscribers may publish */
    subscribers,

    /** Anyone may publish */
    open;
  }
  
  public static PublishModel convert(PublisherType type) {
    if (type == null) {
      return PublishModel.open;
    }
    switch(type) {
    case anyone:      return PublishModel.open;
    case owner:       return PublishModel.publishers;
    case subscribers: return PublishModel.subscribers;
    default:          return PublishModel.open;
    }
  }
  
  public static PublisherType convert(PublishModel model) {
    switch(model) {
    case open:        return PublisherType.anyone;
    case publishers:  return PublisherType.owner;
    case subscribers: return PublisherType.subscribers;
    default:          return PublisherType.anyone;
    }
  }
  
  public static PublisherType convert(PublisherModel model) {
    if (model == PublisherModel.open)
      return PublisherType.anyone;
    if (model == PublisherModel.publishers)
      return PublisherType.owner;
    if (model == PublisherModel.subscribers)
      return PublisherType.subscribers;
    return PublisherType.anyone;
  }
  
  /**
   * Create a decorator from an existing {@link DataForm} that has been
   * retrieved from parsing a node configuration request.
   * 
   * @param configDataForm
   */
//  public ConfigureForm(DataForm configDataForm) {
//    super(configDataForm);
//  }

  /**
   * Create a decorator from an existing {@link Form} for node configuration.
   * Typically, this can be used to create a decorator for an answer form by
   * using the result of {@link #createAnswerForm()} as the input parameter.
   * 
   * @param nodeConfigForm
   */
//  public ConfigureForm(Form nodeConfigForm) {
//    super(nodeConfigForm.getDataFormToSend());
//  }

  /**
   * Create a new form for configuring a node. This would typically only be used
   * when creating and configuring a node at the same time via
   * {@link PubSubManager#createNode(String, Form)}, since configuration of an
   * existing node is typically accomplished by calling
   * {@link LeafNode#getNodeConfiguration()} and using the resulting form to
   * create a answer form. See {@link #ConfigureForm(Form)}.
   * 
   * @param formType
   */
  public ConfigureForm(DataForm.Type formType) {
    super(formType);
  }

  /**
   * Get the currently configured {@link AccessModel}, null if it is not set.
   * 
   * @return The current {@link AccessModel}
   */
  public AccessModel getAccessModel() {
    String value = getFieldValue(ConfigureNodeFields.access_model);
    if (value == null)
      return null;
    else
      return AccessModel.valueOf(value);
  }

  /**
   * Sets the value of access model.
   * 
   * @param accessModel
   */
  public void setAccessModel(AccessModel accessModel) {
    FormField field = addField(ConfigureNodeFields.access_model,
        FormField.Type.list_single);
    field.addValue(accessModel.toString());
  }

  /**
   * Returns the URL of an XSL transformation which can be applied to payloads
   * in order to generate an appropriate message body element.
   * 
   * @return URL to an XSL
   */
  public String getBodyXSLT() {
    return getFieldValue(ConfigureNodeFields.body_xslt);
  }

  /**
   * Set the URL of an XSL transformation which can be applied to payloads in
   * order to generate an appropriate message body element.
   * 
   * @param bodyXslt
   *          The URL of an XSL
   */
  public void setBodyXSLT(String bodyXslt) {
    FormField field = addField(ConfigureNodeFields.body_xslt,
        FormField.Type.text_single);
    field.addValue(bodyXslt);
  }

  /**
   * The id's of the child nodes associated with a collection node (both leaf
   * and collection).
   * 
   * @return list of child nodes.
   */
  public List<String> getChildren() {
    return getFieldValues(ConfigureNodeFields.children);
  }

  /**
   * Set the list of child node ids that are associated with a collection node.
   * 
   * @param children
   */
  public void setChildren(List<String> children) {
    FormField field = addField(ConfigureNodeFields.children,
        FormField.Type.text_multi);
    field.addValue(children);
  }

  /**
   * Returns the policy that determines who may associate children with the
   * node.
   * 
   * @return The current policy
   */
//  public ChildrenAssociationPolicy getChildrenAssociationPolicy() {
//    String value = getFieldValue(ConfigureNodeFields.children_association_policy);
//    if (value == null)
//      return null;
//    else
//      return ChildrenAssociationPolicy.valueOf(value);
//  }

  /**
   * Sets the policy that determines who may associate children with the node.
   * 
   * @param policy
   *          The policy being set
   */
//  public void setChildrenAssociationPolicy(ChildrenAssociationPolicy policy) {
//    FormField field = addField(ConfigureNodeFields.children_association_policy,
//        FormField.Type.list_single);
//    field.addValue(getListSingle(policy.toString()));
//  }

  /**
   * List of JID's that are on the whitelist that determines who can associate
   * child nodes with the collection node. This is only relevant if
   * {@link #getChildrenAssociationPolicy()} is set to
   * {@link ChildrenAssociationPolicy#whitelist}.
   * 
   * @return List of the whitelist
   */
  public List<String> getChildrenAssociationWhitelist() {
    return getFieldValues(ConfigureNodeFields.children_association_whitelist);
  }

  /**
   * Set the JID's in the whitelist of users that can associate child nodes with
   * the collection node. This is only relevant if
   * {@link #getChildrenAssociationPolicy()} is set to
   * {@link ChildrenAssociationPolicy#whitelist}.
   * 
   * @param whitelist
   *          The list of JID's
   */
  public void setChildrenAssociationWhitelist(List<String> whitelist) {
    FormField field = addField(ConfigureNodeFields.children_association_whitelist,
        FormField.Type.jid_multi);
    field.addValue(whitelist);
  }

  /**
   * Gets the maximum number of child nodes that can be associated with the
   * collection node.
   * 
   * @return The maximum number of child nodes
   */
  public int getChildrenMax() {
    return Integer.parseInt(getFieldValue(ConfigureNodeFields.children_max));
  }

  /**
   * Set the maximum number of child nodes that can be associated with a
   * collection node.
   * 
   * @param max
   *          The maximum number of child nodes.
   */
  public void setChildrenMax(int max) {
    FormField field = addField(ConfigureNodeFields.children_max,
        FormField.Type.text_single);
    field.addValue(max);
  }

  /**
   * Gets the collection node which the node is affiliated with.
   * 
   * @return The collection node id
   */
  public String getCollection() {
    return getFieldValue(ConfigureNodeFields.collection);
  }

  /**
   * Sets the collection node which the node is affiliated with.
   * 
   * @param collection
   *          The node id of the collection node
   */
  public void setCollection(String collection) {
    FormField field = addField(ConfigureNodeFields.collection,
        FormField.Type.text_single);
    field.addValue(collection);
  }

  /**
   * Gets the URL of an XSL transformation which can be applied to the payload
   * format in order to generate a valid Data Forms result that the client could
   * display using a generic Data Forms rendering engine.
   * 
   * @return The URL of an XSL transformation
   */
  public String getDataformXSLT() {
    return getFieldValue(ConfigureNodeFields.dataform_xslt);
  }

  /**
   * Sets the URL of an XSL transformation which can be applied to the payload
   * format in order to generate a valid Data Forms result that the client could
   * display using a generic Data Forms rendering engine.
   * 
   * @param url
   *          The URL of an XSL transformation
   */
  public void setDataformXSLT(String url) {
    FormField field = addField(ConfigureNodeFields.dataform_xslt,
        FormField.Type.text_single);
    field.addValue(url);
  }

  /**
   * Does the node deliver payloads with event notifications.
   * 
   * @return true if it does, false otherwise
   */
  public boolean isDeliverPayloads() {
    try {
      return parseBoolean(getFieldValue(ConfigureNodeFields.deliver_payloads));
    } catch (ParseException e) {
      return false;
    }
  }

  /**
   * Sets whether the node will deliver payloads with event notifications.
   * 
   * @param deliver
   *          true if the payload will be delivered, false otherwise
   */
  public void setDeliverPayloads(boolean deliver) {
    FormField field = addField(ConfigureNodeFields.deliver_payloads,
        FormField.Type.boolean_type);
    field.addValue(deliver);
  }

  public String getDescription() {
    return getFieldValue(ConfigureNodeFields.description);
  }
  
  public void setDescription(String description) {
    FormField field = addField(ConfigureNodeFields.description, 
        FormField.Type.text_single);
    field.addValue(description);
  }
  
  /**
   * Determines who should get replies to items
   * 
   * @return Who should get the reply
   */
//  public ItemReply getItemReply() {
//    String value = getFieldValue(ConfigureNodeFields.itemreply);
//    if (value == null)
//      return null;
//    else
//      return ItemReply.valueOf(value);
//  }

  /**
   * Sets who should get the replies to items
   * 
   * @param reply
   *          Defines who should get the reply
   */
//  public void setItemReply(ItemReply reply) {
//    FormField field = addField(ConfigureNodeFields.itemreply,
//        FormField.Type.list_single);
//    field.addValue(getListSingle(reply.toString()));
//  }

  /**
   * Gets the maximum number of items to persisted to this node if
   * {@link #isPersistItems()} is true.
   * 
   * @return The maximum number of items to persist
   */
  public int getMaxItems() {
    return Integer.parseInt(getFieldValue(ConfigureNodeFields.max_items));
  }

  /**
   * Set the maximum number of items to persisted to this node if
   * {@link #isPersistItems()} is true.
   * 
   * @param max
   *          The maximum number of items to persist
   */
  public void setMaxItems(int max) {
    FormField field = addField(ConfigureNodeFields.max_items,
        FormField.Type.list_single);
    field.addValue(max);
  }

  /**
   * Gets the maximum payload size in bytes.
   * 
   * @return The maximum payload size
   */
  public int getMaxPayloadSize() {
    return Integer
        .parseInt(getFieldValue(ConfigureNodeFields.max_payload_size));
  }

  /**
   * Sets the maximum payload size in bytes
   * 
   * @param max
   *          The maximum payload size
   */
  public void setMaxPayloadSize(int max) {
    FormField field = addField(ConfigureNodeFields.max_payload_size,
        FormField.Type.text_single);
    field.addValue(max);
  }

  /**
   * Gets the node type
   * 
   * @return The node type
   */
  public NodeType getNodeType() {
    String value = getFieldValue(ConfigureNodeFields.node_type);
    if (value == null)
      return null;
    else
      return NodeType.valueOf(value);
  }

  /**
   * Sets the node type
   * 
   * @param type
   *          The node type
   */
  public void setNodeType(NodeType type) {
    FormField field = addField(ConfigureNodeFields.node_type,
        FormField.Type.list_single);
    field.addValue(type.toString());
  }

  /**
   * Determines if subscribers should be notified when the configuration
   * changes.
   * 
   * @return true if they should be notified, false otherwise
   */
  public boolean isNotifyConfig() {
    try {
      return parseBoolean(getFieldValue(ConfigureNodeFields.notify_config));
    } catch (ParseException e) {
      return false;
    }
  }

  /**
   * Sets whether subscribers should be notified when the configuration changes.
   * 
   * @param notify
   *          true if subscribers should be notified, false otherwise
   */
  public void setNotifyConfig(boolean notify) {
    FormField field = addField(ConfigureNodeFields.notify_config,
        FormField.Type.boolean_type);
    field.addValue(notify);
  }

  /**
   * Determines whether subscribers should be notified when the node is deleted.
   * 
   * @return true if subscribers should be notified, false otherwise
   */
  public boolean isNotifyDelete() {
    try {
      return parseBoolean(getFieldValue(ConfigureNodeFields.notify_delete));
    } catch (ParseException e) {
      return false;
    }
  }

  /**
   * Sets whether subscribers should be notified when the node is deleted.
   * 
   * @param notify
   *          true if subscribers should be notified, false otherwise
   */
  public void setNotifyDelete(boolean notify) {
    FormField field = addField(ConfigureNodeFields.notify_delete,
        FormField.Type.boolean_type);
    field.addValue(notify);
  }

  /**
   * Determines whether subscribers should be notified when items are deleted
   * from the node.
   * 
   * @return true if subscribers should be notified, false otherwise
   */
  public boolean isNotifyRetract() {
    try {
      return parseBoolean(getFieldValue(ConfigureNodeFields.notify_retract));
    } catch (ParseException e) {
      return false;
    }
  }

  /**
   * Sets whether subscribers should be notified when items are deleted from the
   * node.
   * 
   * @param notify
   *          true if subscribers should be notified, false otherwise
   */
  public void setNotifyRetract(boolean notify) {
    FormField field = addField(ConfigureNodeFields.notify_retract,
        FormField.Type.boolean_type);
    field.addValue(notify);
  }

  /**
   * Determines whether items should be persisted in the node.
   * 
   * @return true if items are persisted
   */
  public boolean isPersistItems() {
    try {
      return parseBoolean(getFieldValue(ConfigureNodeFields.persist_items));
    } catch (ParseException e) {
      return false;
    }
  }

  /**
   * Sets whether items should be persisted in the node.
   * 
   * @param persist
   *          true if items should be persisted, false otherwise
   */
  public void setPersistentItems(boolean persist) {
    FormField field = addField(ConfigureNodeFields.persist_items,
        FormField.Type.boolean_type);
    field.addValue(persist);
  }

  /**
   * Determines whether to deliver notifications to available users only.
   * 
   * @return true if users must be available
   */
  public boolean isPresenceBasedDelivery() {
    try {
      return parseBoolean(getFieldValue(ConfigureNodeFields.presence_based_delivery));
    } catch (ParseException e) {
      return false;
    }
  }

  /**
   * Sets whether to deliver notifications to available users only.
   * 
   * @param presenceBased
   *          true if user must be available, false otherwise
   */
  public void setPresenceBasedDelivery(boolean presenceBased) {
    FormField field = addField(ConfigureNodeFields.presence_based_delivery,
        FormField.Type.boolean_type);
    field.addValue(presenceBased);
  }

  /**
   * Gets the publishing model for the node, which determines who may publish to
   * it.
   * 
   * @return The publishing model
   */
  public PublishModel getPublishModel() {
    String value = getFieldValue(ConfigureNodeFields.publish_model);
    if (value == null)
      return null;
    else
      return PublishModel.valueOf(value);
  }

  /**
   * Sets the publishing model for the node, which determines who may publish to
   * it.
   * 
   * @param publish
   *          The enum representing the possible options for the publishing
   *          model
   */
  public void setPublishModel(PublishModel publish) {
    FormField field = addField(ConfigureNodeFields.publish_model,
        FormField.Type.list_single);
    field.addValue(publish.toString());
  }

  /**
   * List of the multi user chat rooms that are specified as reply rooms.
   * 
   * @return The reply room JID's
   */
  public List<String> getReplyRoom() {
    return getFieldValues(ConfigureNodeFields.replyroom);
  }

  /**
   * Sets the multi user chat rooms that are specified as reply rooms.
   * 
   * @param replyRooms
   *          The multi user chat room to use as reply rooms
   */
  public void setReplyRoom(List<String> replyRooms) {
    FormField field = addField(ConfigureNodeFields.replyroom,
        FormField.Type.list_multi);
    field.addValue(replyRooms);
  }

  /**
   * Gets the specific JID's for reply to.
   * 
   * @return The JID's
   */
  public List<String> getReplyTo() {
    return getFieldValues(ConfigureNodeFields.replyto);
  }

  /**
   * Sets the specific JID's for reply to.
   * 
   * @param replyTos
   *          The JID's to reply to
   */
  public void setReplyTo(List<String> replyTos) {
    FormField field = addField(ConfigureNodeFields.replyto,
        FormField.Type.list_multi);
    field.addValue(replyTos);
  }

  /**
   * Gets the roster groups that are allowed to subscribe and retrieve items.
   * 
   * @return The roster groups
   */
  public List<String> getRosterGroupsAllowed() {
    return getFieldValues(ConfigureNodeFields.roster_groups_allowed);
  }

  /**
   * Sets the roster groups that are allowed to subscribe and retrieve items.
   * 
   * @param groups
   *          The roster groups
   */
  public void setRosterGroupsAllowed(List<String> groups) {
    FormField field = addField(ConfigureNodeFields.roster_groups_allowed,
        FormField.Type.list_multi);
    field.addValue(groups);
  }

  /**
   * Determines if subscriptions are allowed.
   * 
   * @return true if subscriptions are allowed, false otherwise
   */
  public boolean isSubscribe() {
    try {
      return parseBoolean(getFieldValue(ConfigureNodeFields.subscribe));
    } catch (ParseException e) {
      return false;
    }
  }

  /**
   * Sets whether subscriptions are allowed.
   * 
   * @param subscribe
   *          true if they are, false otherwise
   */
  public void setSubscribe(boolean subscribe) {
    FormField field = addField(ConfigureNodeFields.subscribe,
        FormField.Type.boolean_type);
    field.addValue(subscribe);
  }

  public boolean isSendItemSubscribe() {
    try {
      return parseBoolean(getFieldValue(ConfigureNodeFields.send_item_subscribe));
    } catch (ParseException e) {
      return false;
    }
  }
  
  public void setSendItemSubscribe(boolean sendItem) {
    FormField field = addField(ConfigureNodeFields.send_item_subscribe, 
        FormField.Type.boolean_type);
    field.addValue(sendItem);
  }
  
  /**
   * Gets the human readable node title.
   * 
   * @return The node title
   */
  public String getTitle() {
    return getFieldValue(ConfigureNodeFields.title);
  }

  /**
   * Sets a human readable title for the node.
   * 
   * @param title
   *          The node title
   */
  public void setTitle(String title) {
    FormField field = addField(ConfigureNodeFields.title,
        FormField.Type.text_single);
    field.addValue(title);
  }

  /**
   * The type of node data, usually specified by the namespace of the payload
   * (if any).
   * 
   * @return The type of node data
   */
  public String getDataType() {
    return getFieldValue(ConfigureNodeFields.type);
  }

  /**
   * Sets the type of node data, usually specified by the namespace of the
   * payload (if any).
   * 
   * @param type
   *          The type of node data
   */
  public void setDataType(String type) {
    FormField field = addField(ConfigureNodeFields.type,
        FormField.Type.text_single);
    field.addValue(type);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(getClass().getName()
        + " Content [");

    for (FormField formField : getFields()) {
      result.append('(');
      result.append(formField.getVariable());
      result.append(':');

      StringBuilder valuesBuilder = new StringBuilder();

      for (String value : formField.getValues()) {
        if (valuesBuilder.length() > 0)
          result.append(',');
        valuesBuilder.append(value);
      }

      if (valuesBuilder.length() == 0)
        valuesBuilder.append("NOT SET");
      result.append(valuesBuilder);
      result.append(')');
    }
    result.append(']');
    return result.toString();
  }

  private String getFieldValue(ConfigureNodeFields field) {
    FormField formField = getField(field.getFieldName());
    return (formField.getValues().isEmpty()) ? null : formField.getValues()
        .get(0);
  }

  private List<String> getFieldValues(ConfigureNodeFields field) {
    FormField formField = getField(field.getFieldName());
    return formField.getValues();
  }

  private FormField addField(ConfigureNodeFields nodeField, FormField.Type type) {
    String fieldName = nodeField.getFieldName();
    FormField field = getField(fieldName);
    if (field == null) {
      field = addField(fieldName, null, type);
    }
    return field;
  }

  private List<String> getListSingle(String value) {
    List<String> list = new ArrayList<String>(1);
    list.add(value);
    return list;
  }
}

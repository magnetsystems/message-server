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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;

/**
 * @hide
 * Subscription form for PubSub node in MMX server.
 */
public class SubscribeForm extends DataForm {
  public static enum PresenceState {
    chat, online, away, xa, dnd
  }

  public static enum SubscribeOptionFields {
    deliver,           // boolean
    digest,             // boolean
    digest_frequency,   // int
    expire,             // Date
    include_body,       // boolean
    show_values,        // PresenceState
    subscription_type,  // list single of NodeSubscription.Type.items or nodes
    subscription_depth, // list single of "all" or "1"
    keywords;           // list multi of string

    public String getFieldName() {
      if (this == show_values)
        return "pubsub#" + toString().replace('_', '-');
      if (this == keywords)
        return "x-pubsub#" + toString();
      return "pubsub#" + toString();
    }

    static public SubscribeOptionFields valueOfFromElement(String elementName) {
      String portion = elementName.substring(elementName.lastIndexOf('#' + 1));
      if ("show-values".equals(portion))
        return show_values;
      else
        return valueOf(portion);
    }
  }

  private static XMPPDateTimeFormat sDateTimeFmtr = new XMPPDateTimeFormat();
  
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
  public SubscribeForm(DataForm.Type formType) {
    super(formType);
  }

  public boolean isDeliverOn() {
    try {
      return parseBoolean(getFieldValue(SubscribeOptionFields.deliver));
    } catch (ParseException e) {
      return false;
    }
  }
  
  public void setDeliverOn(boolean deliverNotification) {
    FormField field = addField(SubscribeOptionFields.deliver, 
        FormField.Type.boolean_type);
    field.addValue(deliverNotification);
  }
  
  public boolean isDigestOn() {
    try {
      return parseBoolean(getFieldValue(SubscribeOptionFields.digest));
    } catch (ParseException e) {
      return false;
    }
  }
  
  public int getDigestFrequency() {
    return Integer.parseInt(getFieldValue(SubscribeOptionFields.digest_frequency));
  }
  
  public void setDigestFrequency(int frequency)
  {
    FormField field = addField(SubscribeOptionFields.digest_frequency,
        FormField.Type.text_single);
    field.addValue(frequency);
  }

  
  public void setDigestOn(boolean digestOn) {
    FormField field = addField(SubscribeOptionFields.digest, 
        FormField.Type.boolean_type);
    field.addValue(digestOn);
  }
  
  public Date getExpiry() {
    String dateTime = getFieldValue(SubscribeOptionFields.expire);
    try {
      return sDateTimeFmtr.parseString(dateTime);
    } catch (ParseException e) {
      return null;
    }
  }
  
  public void setExpiry(Date expire) {
    FormField field = addField(SubscribeOptionFields.expire, 
        FormField.Type.text_single);
    field.addValue(XMPPDateTimeFormat.format(expire));
  }

  public boolean isIncludeBody() {
    try {
      return parseBoolean(getFieldValue(SubscribeOptionFields.include_body));
    } catch (ParseException e) {
      return false;
    }
  }

  public void setIncludeBody(boolean include) {
    FormField field = addField(SubscribeOptionFields.include_body,
        FormField.Type.boolean_type);
    field.addValue(include);
  }

  public List<PresenceState> getShowValues() {
    ArrayList<PresenceState> result = new ArrayList<PresenceState>(5);
    for (String state : getFieldValues(SubscribeOptionFields.show_values)) {
      result.add(PresenceState.valueOf(state));
    }
    return result;
  }

  public void setShowValues(Collection<PresenceState> stateValues) {
    ArrayList<String> values = new ArrayList<String>(stateValues.size());
    for (PresenceState state : stateValues) {
      values.add(state.toString());
    }
    FormField field = addField(SubscribeOptionFields.show_values,
        FormField.Type.list_multi);
    field.addValue(values);
  }
  
  public String getSubscriptionType() {
    return getFieldValue(SubscribeOptionFields.subscription_type);
  }
  
  public void setSubscriptionType(NodeSubscription.Type type) {
    FormField field = addField(SubscribeOptionFields.subscription_type,
        FormField.Type.list_single);
    field.addValue(type.toString());
  }

  public String getSubscriptionDepth() {
    return getFieldValue(SubscribeOptionFields.subscription_depth);
  }
  
  /**
   * 
   * @param depth "all" or "1"
   */
  public void setSubscriptionDepth(String depth) {
    FormField field = addField(SubscribeOptionFields.subscription_depth,
        FormField.Type.list_single);
    field.addValue(depth);
  }

  public List<String> getKeywords() {
    return getFieldValues(SubscribeOptionFields.keywords);
  }
  
  public void setKeywords(List<String> keywords) {
    FormField field = addField(SubscribeOptionFields.keywords, 
        FormField.Type.list_multi);
    field.addValue(keywords);
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

  private String getFieldValue(SubscribeOptionFields field) {
    FormField formField = getField(field.getFieldName());
    return (formField.getValues().isEmpty()) ? null : formField.getValues()
        .get(0);
  }

  private List<String> getFieldValues(SubscribeOptionFields field) {
    FormField formField = getField(field.getFieldName());
    return formField.getValues();
  }

  private FormField addField(SubscribeOptionFields nodeField, FormField.Type type) {
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

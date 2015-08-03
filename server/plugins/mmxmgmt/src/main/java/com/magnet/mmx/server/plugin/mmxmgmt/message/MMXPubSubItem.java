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
package com.magnet.mmx.server.plugin.mmxmgmt.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.magnet.mmx.protocol.Constants;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.openfire.pubsub.PublishedItem;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 */
@XmlRootElement
@XmlType(propOrder={"appId", "topicName", "itemId", "meta", "payload"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MMXPubSubItem {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXPubSubItem.class);
  private String itemId;
  private String topicName;
  private String appId;
  private MMXItemPublisher publisher;
  private Map<String, String> meta;
  private MMXPubSubPayload payload;

  public MMXPubSubItem(PublishedItem publishedItem, String appId, String topicName) {
    itemId = publishedItem.getID();
    this.appId = appId;
    this.topicName = topicName;
    parsePayload(publishedItem.getPayload());

  }

  public MMXPubSubItem(String appId, String topicName, String itemId, JID publisher, String payload) {
    this.appId = appId;
    this.topicName = topicName;
    this.itemId = itemId;
    this.publisher = new MMXItemPublisher(publisher);
    try {
      Document document = DocumentHelper.parseText(payload);
      parsePayload(document.getRootElement());
    } catch(Exception e) {

    }
  }

  private void parsePayload(Element payloadElement) {
    if(payloadElement != null) {
      Document d = payloadElement.getDocument();
      Node mmxPayloadNode = d.selectSingleNode(String.format("/*[name()='%s']/*[name()='%s']", Constants.MMX_ELEMENT, Constants.MMX_PAYLOAD));

      if(mmxPayloadNode != null) {
        String mtype = mmxPayloadNode.valueOf("@mtype");
        String stamp = DateFormatUtils.format(new DateTime(mmxPayloadNode.valueOf("@stamp")).toDate(), "yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"));
        String data = mmxPayloadNode.getText();
        payload = new MMXPubSubPayload(mtype, stamp, data);
      }

      Node mmxMetaNode = d.selectSingleNode(String.format("/*[name()='%s']/*[name()='%s']", Constants.MMX_ELEMENT, Constants.MMX_META));

      if(mmxMetaNode != null) {
        meta = getMapFromJsonString(mmxMetaNode.getText());
      }
    }
  }

  public MMXPubSubItem() {

  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public Map<String, String> getMeta() {
    return meta;
  }

  public void setMeta(Map<String, String> meta) {
    this.meta = meta;
  }

  public void setPayload(MMXPubSubPayload payload) {
    this.payload = payload;
  }

  public MMXPubSubPayload getPayload() {
    return this.payload;
  }

  public MMXItemPublisher getPublisher() {
    return publisher;
  }

  public void setPublisher(MMXItemPublisher publisher) {
    this.publisher = publisher;
  }

  private Map<String, String> getMapFromJsonString(String s) {
    ObjectMapper mapper = new ObjectMapper();
    HashMap<String, String> map = null;
    try {

      //convert JSON string to Map
      map = mapper.readValue(s,
              new TypeReference<HashMap<String,String>>(){});

      LOGGER.trace("getMapFromJsonString : {}",map);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return map;
  }

  @Override
  public String toString() {
    return "MMXPubSubItem{" +
            "itemId='" + itemId + '\'' +
            ", topicName='" + topicName + '\'' +
            ", appId='" + appId + '\'' +
            ", meta=" + meta +
            ", payload=" + payload +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MMXPubSubItem)) return false;

    MMXPubSubItem that = (MMXPubSubItem) o;

    if (!appId.equals(that.appId)) return false;
    if (!itemId.equals(that.itemId)) return false;
    if (meta != null ? !meta.equals(that.meta) : that.meta != null) return false;
    if (payload != null ? !payload.equals(that.payload) : that.payload != null) return false;
    if (!publisher.equals(that.publisher)) return false;
    if (!topicName.equals(that.topicName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = itemId.hashCode();
    result = 31 * result + topicName.hashCode();
    result = 31 * result + appId.hashCode();
    result = 31 * result + publisher.hashCode();
    result = 31 * result + (meta != null ? meta.hashCode() : 0);
    result = 31 * result + (payload != null ? payload.hashCode() : 0);
    return result;
  }
}

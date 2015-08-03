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
package com.magnet.mmx.client.app;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.PubSubManager;
import com.magnet.mmx.client.common.TopicExistsException;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.TopicAction.TopicTags;
import com.magnet.mmx.util.GsonData;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

/**
 * A facility to allow a topic to have a template for publishing and a template
 * for rendering a published item.  It also maintains the cached templates for
 * the topics.
 */
public class TopicTemplateManager {
  
  /**
   * Template content for topic.
   */
  public static class TopicTemplate {
    private String mContentType;
    private CharSequence mContent;
    
    private TopicTemplate() {
    }
    
    /**
     * Constructor with the content type and in-memory template.
     * @param contentType The content type of the template.
     * @param content The template content.
     */
    public TopicTemplate(String contentType, CharSequence content) {
      mContentType = contentType;
      mContent = content;
    }
    
    /**
     * Constructor with the content type and the template file.  The file
     * content will be read into memory.
     * @param contentType The content type of the template.
     * @param file A file containing the template.
     * @throws IOException
     */
    public TopicTemplate(String contentType, File file) throws IOException {
      mContentType = contentType;
      mContent = (file == null) ? null : readFileAsText(file);
    }
    
    /**
     * Get the MIME type of the template.
     * @return The MIME content type.
     */
    public String getContentType() {
      return mContentType;
    }
    
    /**
     * Get the content of the template.
     * @return The content in text.
     */
    public CharSequence getContent() {
      return mContent;
    }
    
    private CharSequence readFileAsText(File file) throws IOException {
      FileReader reader = null;
      try {
        reader = new FileReader(file);
        int len = (int) file.length();
        CharBuffer cb = CharBuffer.allocate(len);
        int n;
        while (len > 0 && (n = reader.read(cb)) > 0) {
          len -= n;
        }
        cb.rewind();
        return cb; 
      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    }
  }
  
  private static final String DELIMITER = ":";
  private static final String PREFIX_PUB_TEMPLATE = "p";
  private static final String PREFIX_SUB_TEMPLATE = "s";
  private static final TopicTemplate NO_TEMPLATE = new TopicTemplate();
  private MMXClient mClient;
  private MMXGlobalTopic mTemplateTopic;
  private Mustache.Compiler mCompiler;
  private HashMap<String, TopicTemplate> mPubTemplates = new HashMap<String, TopicTemplate>();
  private HashMap<String, TopicTemplate> mSubTemplates = new HashMap<String, TopicTemplate>();
  
  /**
   * Construct a template manager for a client.
   * @param client
   */
  public TopicTemplateManager(MMXClient client) {
    mClient = client;
    mTemplateTopic = new MMXGlobalTopic("com.magnet.templates");
    mCompiler = Mustache.compiler().emptyStringIsFalse(true);
  }
  
  /**
   * Set the templates for publishing and viewing item.
   * @param pubTempl The publishing template, or null.
   * @param itemTempl The viewing template with Mustache template engine.
   * @throws MMXException
   */
  public void setTemplates(MMXTopic topic, TopicTemplate pubTempl, 
                            TopicTemplate itemTempl) throws MMXException {
    PubSubManager mgr = mClient.getPubSubManager();
    try {
      MMXTopicOptions templateTopicOpts = new MMXTopicOptions()
        .setMaxItems(10000)
        .setSubscriptionEnabled(false)
        .setDescription("templates persistent storage; it is not for subscription");
      mgr.createTopic(mTemplateTopic, templateTopicOpts);
    } catch (TopicExistsException e) {
      // Ignored.
    }
    
    String pubTempId = null;
    if (pubTempl == null) {
      mPubTemplates.put(topic.toString(), NO_TEMPLATE);
    } else {
      pubTempId = mgr.publish(mTemplateTopic, new MMXPayload(
          pubTempl.getContent()).setContentType(pubTempl.getContentType()));
      mPubTemplates.put(topic.toString(), pubTempl);
    }
    String itemTempId = null;
    if (itemTempl == null) { 
      mSubTemplates.put(topic.toString(), NO_TEMPLATE);
    } else {
      itemTempId = mgr.publish(mTemplateTopic, new MMXPayload(
          itemTempl.getContent()).setContentType(itemTempl.getContentType()));
      mSubTemplates.put(topic.toString(), itemTempl);
    }
    
    // Save the template ID's using tags.
    ArrayList<String> tags = new ArrayList<String>();
    if (pubTempId != null) {
      str2Tags(tags, PREFIX_PUB_TEMPLATE, pubTempId);
    }
    if (itemTempId != null) {
      str2Tags(tags, PREFIX_SUB_TEMPLATE, itemTempId);
    }
    if (!tags.isEmpty()) {
      mgr.setAllTags(topic, tags);
    }
  }
  
  // Break a big string into multiple tags as: prefix:0:partial-ID, 
  // prefix:1:partial-ID...prefix:N:partial-ID
  private void str2Tags(List<String> tags, String prefix, String str) {
    int len;
    int start = 0;
    int index = 0;
    for (;;) {
      StringBuilder sb = new StringBuilder(Constants.MMX_MAX_TAG_LEN);
      sb.append(prefix).append(DELIMITER).append(index).append(DELIMITER);
      len = Math.min(str.length() - start, Constants.MMX_MAX_TAG_LEN - sb.length());
      if (len == 0) {
        return;
      }
      int end = start + len;
      sb.append(str.substring(start, end));
      tags.add(sb.toString());
      start = end;
      ++index;
    }
  }
  
  // Assemble tags into a big string.  Currently it only supports 5 parts.
  private String tags2Str(List<String> tags, String prefix) {
    String[] strs = new String[5];
    for (String tag : tags) {
      if (tag.startsWith(prefix)) {
        String[] tokens = tag.split(DELIMITER);
        int index = Integer.parseInt(tokens[1]);
        strs[index] = tokens[2];
      }
    }
    if (strs[0] == null) {
      return null;
    }
    StringBuilder str = new StringBuilder();
    for (String token : strs) {
      if (token != null) {
        str.append(token);
      }
    }
    return str.toString();
  }
  
  /**
   * Get the publishing view.  The view is a template.
   * @return A template view, or null.
   */
  public TopicTemplate getPublishView(MMXTopic topic) throws MMXException {
    TopicTemplate template = mPubTemplates.get(topic.toString());
    if (template == null) {
      List<String> list;
      PubSubManager mgr = mClient.getPubSubManager();
      TopicTags tags = mgr.getAllTags(topic);
      template = NO_TEMPLATE;
      if (tags != null && ((list = tags.getTags()) != null)) {
        String id = tags2Str(list, PREFIX_PUB_TEMPLATE);
        if (id != null) {
          Map<String, MMXMessage> map = mgr.getItemsByIds(mTemplateTopic,
              Arrays.asList(new String[] { id }));
          MMXMessage tempMsg = map.get(id);
          template = new TopicTemplate(tempMsg.getPayload().getContentType(),
                                        tempMsg.getPayload().getDataAsText());
        }
      }
      mPubTemplates.put(topic.toString(), template);
    }
    return (template == NO_TEMPLATE) ? null : template;
  }
  
  /**
   * Bind a published item with a Mustache template.
   * @param template
   * @param msg
   * @return
   */
  public CharSequence bindItemView(TopicTemplate template, MMXMessage msg) {
    if (template == null)
      return null;
    String json = msg.getPayload().getDataAsText().toString();
    Map<String, String> context = GsonData.getGson().fromJson(json, HashMap.class);
    Template mstTemplate = mCompiler.compile(template.getContent().toString());
    return mstTemplate.execute(context);
  }
  
  /**
   * Get the item view template from a topic.
   * @return A template view, or null.
   */
  public TopicTemplate getItemView(MMXTopic topic) throws MMXException {
    TopicTemplate template = mSubTemplates.get(topic.toString());
    if (template == null) {
      List<String> list;
      PubSubManager mgr = mClient.getPubSubManager();
      TopicTags tags = mgr.getAllTags(topic);
      template = NO_TEMPLATE;
      if (tags != null && ((list = tags.getTags()) != null)) {
        String id = tags2Str(list, PREFIX_SUB_TEMPLATE);
        if (id != null) {
          Map<String, MMXMessage> map = mgr.getItemsByIds(mTemplateTopic,
              Arrays.asList(new String[] { id }));
          MMXMessage tempMsg = map.get(id);
          template = new TopicTemplate(tempMsg.getPayload().getContentType(),
                                        tempMsg.getPayload().getDataAsText());
        }
      }
      mSubTemplates.put(topic.toString(), template);
    }
    return (template == NO_TEMPLATE) ? null : template;
  }
}

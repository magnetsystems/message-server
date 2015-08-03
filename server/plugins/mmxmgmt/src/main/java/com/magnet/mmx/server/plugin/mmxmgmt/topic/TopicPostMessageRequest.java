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
package com.magnet.mmx.server.plugin.mmxmgmt.topic;

/**
 * Object that represents a topic post message request.
 */
public class TopicPostMessageRequest {

  private String content;
  private String messageType;
  private String contentType;

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TopicPostMessageRequest{");
    sb.append("content='").append(content).append('\'');
    sb.append(", messageType='").append(messageType).append('\'');
    sb.append(", contentType='").append(contentType).append('\'');
    sb.append('}');
    return sb.toString();
  }
}

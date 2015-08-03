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
package com.magnet.mmx.server.plugin.mmxmgmt.api;

import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Count;
import com.magnet.mmx.server.plugin.mmxmgmt.message.UnsentMessage;

import java.util.List;

/**
 * Send message response
 */
public class SendMessageResponse {
  private Count count;
  private List<SentMessageId> sentList;
  private List<UnsentMessage> unsentList;

//  private String messageId;
//
//  public String getMessageId() {
//    return messageId;
//  }
//
//  public void setMessageId(String messageId) {
//    this.messageId = messageId;
//  }


  public Count getCount() {
    return count;
  }

  public void setCount(Count count) {
    this.count = count;
  }

  public List<SentMessageId> getSentList() {
    return sentList;
  }

  public void setSentList(List<SentMessageId> sentList) {
    this.sentList = sentList;
  }

  public List<UnsentMessage> getUnsentList() {
    return unsentList;
  }

  public void setUnsentList(List<UnsentMessage> unsentList) {
    this.unsentList = unsentList;
  }
}

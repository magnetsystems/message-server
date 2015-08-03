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

import com.magnet.mmx.server.plugin.mmxmgmt.api.SentMessageId;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Count;

import java.util.List;

/**
* Created by rphadnis on 4/23/15.
*/
public class SendMessageResult {
  private List<SentMessageId> sentList;
  private List<UnsentMessage> unsentList;
  private Count count;
  /**
   * Boolean flag indicating if the result is an error.
   * @return
   */
  private boolean error;

  /**
   * Get a descriptive error errorMessage if the result represents an error.
   * @return
   */
  private String errorMessage;

  private int errorCode;


  public void setSentList(List<SentMessageId> sentList) {
    this.sentList = sentList;
  }

  public void setCount(Count count) {
    this.count = count;
  }

  public List<SentMessageId> getSentList() {
    return sentList;
  }

  public Count getCount() {
    return count;
  }

  public List<UnsentMessage> getUnsentList() {
    return unsentList;
  }

  public void setUnsentList(List<UnsentMessage> unsentList) {
    this.unsentList = unsentList;
  }

  public boolean isError() {
    return error;
  }

  public void setError(boolean error) {
    this.error = error;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }
}

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
package com.magnet.mmx.server.plugin.mmxmgmt.api.push;

import java.util.List;

/**
 * SendPushMessageResponse
 */
public class SendPushMessageResponse {

  private Count count;

  private List<PushIdTuple> sentList;

  private List<Unsent> unsentList;

  public List<PushIdTuple> getSentList() {
    return sentList;
  }

  public void setSentList(List<PushIdTuple> sentList) {
    this.sentList = sentList;
  }

  public List<Unsent> getUnsentList() {
    return unsentList;
  }

  public void setUnsentList(List<Unsent> unsentList) {
    this.unsentList = unsentList;
  }

  public Count getCount() {
    return count;
  }

  public void setCount(Count count) {
    this.count = count;
  }

  public void setCount (int requested, int sent, int unsent) {
    Count count = new Count(requested, sent, unsent);
    setCount(count);
  }

  public void addUnsentList(List<Unsent> list) {
    if (unsentList == null) {
      unsentList = list;
    } else {
      unsentList.addAll(list);
    }
  }

  public void addSentList(List<PushIdTuple> list) {
    if (sentList == null) {
      sentList = list;
    } else {
      sentList.addAll(list);
    }
  }
}

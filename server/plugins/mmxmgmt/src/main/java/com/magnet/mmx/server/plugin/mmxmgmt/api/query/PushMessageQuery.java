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
package com.magnet.mmx.server.plugin.mmxmgmt.api.query;

import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;

import java.util.List;

/**
 */
public class PushMessageQuery {
  private Operator operator;

  private PushMessageEntity.PushMessageState state;
  private PushMessageEntity.PushMessageType type;
  private DateRange dateSent;
  private DateRange dateAcknowledged;

  private List<String> deviceIds;

  public PushMessageEntity.PushMessageState getState() {
    return state;
  }

  public void setState(PushMessageEntity.PushMessageState state) {
    this.state = state;
  }

  public PushMessageEntity.PushMessageType getType() {
    return type;
  }

  public void setType(PushMessageEntity.PushMessageType type) {
    this.type = type;
  }

  public DateRange getDateSent() {
    return dateSent;
  }

  public void setDateSent(DateRange dateSent) {
    this.dateSent = dateSent;
  }

  public DateRange getDateAcknowledged() {
    return dateAcknowledged;
  }

  public void setDateAcknowledged(DateRange dateAcknowledged) {
    this.dateAcknowledged = dateAcknowledged;
  }

  public List<String> getDeviceIds() {
    return deviceIds;
  }

  public void setDeviceIds(List<String> deviceIds) {
    this.deviceIds = deviceIds;
  }

  public void setState (String state) {
    this.state = Helper.enumeratePushMessageState(state);
  }

  public void setType (String type) {
    this.type = Helper.enumeratePushMessageType(type);
  }

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }
}

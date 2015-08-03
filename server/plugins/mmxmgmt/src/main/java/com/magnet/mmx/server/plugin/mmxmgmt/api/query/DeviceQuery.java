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

import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;

import java.util.List;

/**
 * Device query representation.
 */
public class DeviceQuery {

  private Operator operator;

  private List<String> tags;
  private OSType osType;
  private DateRange registrationDate;
  private String modelInfo;
  private String phoneNumber;
  private DeviceStatus status;

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }


  public OSType getOsType() {
    return osType;
  }

  public void setOsType(OSType osType) {
    this.osType = osType;
  }

  public DateRange getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(DateRange registrationDate) {
    this.registrationDate = registrationDate;
  }

  public String getModelInfo() {
    return modelInfo;
  }

  public void setModelInfo(String modelInfo) {
    this.modelInfo = modelInfo;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public DeviceStatus getStatus() {
    return status;
  }

  public void setStatus(DeviceStatus status) {
    this.status = status;
  }
}

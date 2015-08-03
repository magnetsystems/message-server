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

import javax.xml.bind.annotation.XmlRootElement;

/**
 */
@XmlRootElement
public class MMXPubSubPayload {
  String mtype;
  String creationDate;
  String data;

  public MMXPubSubPayload(String mtype, String creationDate, String data) {
    this.mtype = mtype;
    this.creationDate = creationDate;
    this.data = data;
  }

  public MMXPubSubPayload() {

  }

  public String getMtype() {
    return mtype;
  }

  public void setMtype(String mtype) {
    this.mtype = mtype;
  }

  public String getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(String creationDate) {
    this.creationDate = creationDate;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MMXPubSubPayload)) return false;

    MMXPubSubPayload that = (MMXPubSubPayload) o;

    if (!creationDate.equals(that.creationDate)) return false;
    if (mtype != null ? !mtype.equals(that.mtype) : that.mtype != null) return false;
    if (!data.equals(that.data)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mtype != null ? mtype.hashCode() : 0;
    result = 31 * result + creationDate.hashCode();
    result = 31 * result + data.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "MMXPubSubPayload{" +
            "mtype='" + mtype + '\'' +
            ", creationDate='" + creationDate + '\'' +
            ", data='" + data + '\'' +
            '}';
  }
}

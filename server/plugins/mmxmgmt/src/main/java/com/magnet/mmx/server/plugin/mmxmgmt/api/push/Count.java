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

/**
 * Count object to represent the results of the operation
 */
public class Count {

  private int requested;
  private int sent;
  private int unsent;

  public Count(int requested, int sent, int unsent) {
    this.requested = requested;
    this.sent = sent;
    this.unsent = unsent;
  }

  public Count() {
  }

  public int getRequested() {
    return requested;
  }

  public void setRequested(int requested) {
    this.requested = requested;
  }

  public int getSent() {
    return sent;
  }

  public void setSent(int sent) {
    this.sent = sent;
  }

  public int getUnsent() {
    return unsent;
  }

  public void setUnsent(int unsent) {
    this.unsent = unsent;
  }
}

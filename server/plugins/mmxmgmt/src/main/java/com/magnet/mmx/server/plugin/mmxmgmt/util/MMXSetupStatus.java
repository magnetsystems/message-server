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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import javax.xml.bind.annotation.XmlRootElement;

/**
 */
@XmlRootElement
public class MMXSetupStatus {
  private boolean setupComplete;

  public MMXSetupStatus() {
  }

  public MMXSetupStatus(boolean setupComplete) {
    this.setupComplete = setupComplete;
  }

  public boolean isSetupComplete() {
    return setupComplete;
  }

  public void setSetupComplete(boolean setupComplete) {
    this.setupComplete = setupComplete;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MMXSetupStatus)) return false;

    MMXSetupStatus that = (MMXSetupStatus) o;

    if (setupComplete != that.setupComplete) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return (setupComplete ? 1 : 0);
  }

  @Override
  public String toString() {
    return "MMXSetupStatus{" +
            "setupComplete=" + setupComplete +
            '}';
  }
}

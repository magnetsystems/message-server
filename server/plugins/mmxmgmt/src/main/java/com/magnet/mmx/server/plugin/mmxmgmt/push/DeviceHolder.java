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
package com.magnet.mmx.server.plugin.mmxmgmt.push;

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeviceHolder for building a hash of devices using the push type
 */
public class DeviceHolder {
  Map<PushType, List<DeviceEntity>> pushTypeHash;

  List<DeviceEntity> nullPushTypeList;

  private DeviceHolder() {
    pushTypeHash = new HashMap<PushType, List<DeviceEntity>>();
    nullPushTypeList = new ArrayList<DeviceEntity>(10);
  }

  private void add (DeviceEntity de) {
    PushType push = de.getTokenType();
    if (push == null) {
      nullPushTypeList.add(de);
    } else {
      if (pushTypeHash.containsKey(push)) {
        pushTypeHash.get(push).add(de);
      } else {
        List<DeviceEntity> list = new ArrayList<DeviceEntity>(10);
        list.add(de);
        pushTypeHash.put(push, list);
      }
    }
  }

  public static DeviceHolder build(List<DeviceEntity> deviceList) {
    DeviceHolder holder = new DeviceHolder();
    for (DeviceEntity  de : deviceList) {
      holder.add(de);
    }
    return holder;
  }

  /**
   * Check if we have devices of a particular type
   * @param type
   * @return
   */
  public boolean hasDevices (PushType type) {
    return pushTypeHash.containsKey(type);
  }


  /**
   * Get a list of devices for a type.
   * @param type
   * @return
   */
  public List<DeviceEntity> getDevices (PushType type) {
    List<DeviceEntity> list = pushTypeHash.get(type);
    if (list == null) {
      return Collections.emptyList();
    } else {
      return list;
    }
  }

}

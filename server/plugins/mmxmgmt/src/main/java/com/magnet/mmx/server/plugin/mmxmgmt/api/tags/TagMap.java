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
package com.magnet.mmx.server.plugin.mmxmgmt.api.tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class TagMap {
  protected Map<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();

  public Map<String, ArrayList<String>> getMap() {
    return map;
  }

  public void addTag(String resourceId, String tag) {
    ArrayList<String> list = map.get(resourceId);
    if(list == null) {
      list = new ArrayList<String>();
      map.put(resourceId, list);
    }
    list.add(tag);
  }
}

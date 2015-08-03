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
package com.magnet.mmx.server.plugin.mmxmgmt.db;

import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Target;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;

import java.util.List;

/**
 * Interface that defines API for resolving JSON Target definitions into
 * entity objects.
 */
public interface TargetResolver <E> {

  /**
   * Resolve the target and get a list of objects.
   *
   * @param appId appId for which we need entities for
   * @param target not null target
   * @return a list of objects
   * @throws com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException if an exception is encountered.
   */
  public List<E> resolve(String appId, Target target) throws ResolutionException;
}

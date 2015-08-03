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

import com.magnet.mmx.protocol.Constants;

/**
 */
public class MMXPushPayload extends MMXPayload {
  public MMXPushPayload(String type, String jsonPayload) {
    super(new MMXPushHeader(Constants.MMX, Constants.MMX_ACTION_CODE_PUSH, type), jsonPayload);
  }
  public MMXPushPayload(String jsonPayload) {
    super(new MMXPushHeader(Constants.MMX, Constants.MMX_ACTION_CODE_PUSH), jsonPayload);
  }
}

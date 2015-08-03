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
package com.magnet.mmx.server.plugin.mmxmgmt.search;

import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;

/**
 * Message entity post processor.
 */
public class MessageEntityPostProcessor implements PostProcessor<MessageEntity>{
  
  @Override
  public void postProcess(MessageEntity instance) {
    String from = instance.getFrom();
    String processed = JIDUtil.getReadableUserId(from);
    instance.setFrom(processed);
    String to = instance.getTo();
    String pto = JIDUtil.getReadableUserId(to);
    instance.setTo(pto);
  }
}

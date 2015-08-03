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
package com.magnet.mmx.server.plugin.mmxmgmt.bot;

public interface BotRegistration {

  /**
   * Register a bot with the given user name for the supplied appId.
   * @param appId -- mmx app id for which we need to create a bot
   * @param botUserName -- user name of the bot.
   * @param processor -- AutoResponse processor that processes message sent to the specified bot user.
   */
  void registerBot(String appId, String botUserName, AutoResponseProcessor processor);


}

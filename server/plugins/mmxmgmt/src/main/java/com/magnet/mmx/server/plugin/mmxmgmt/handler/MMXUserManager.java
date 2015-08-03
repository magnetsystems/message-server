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

package com.magnet.mmx.server.plugin.mmxmgmt.handler;

import org.jivesoftware.openfire.user.User;

import java.util.Map;

/**
 * Interface that defines the user management related API.
 */
public interface MMXUserManager {

  /**
   * Check if the passed in userId exists.
   * @param userId
   * @return true if the userId exists false otherwise
   */
  public boolean isUserIdTaken(String userId);

  /**
   * Create a user in the database
   * @param userId  userId for the user
   * @param password
   * @param name
   * @param email
   * @param metadata name value pairs for the user
   */
  public User createUser (String userId, String password, String name, String email, Map<String, String> metadata);

  /**
   * Delete user with userId
   * @param userId
   */
  public void deleteUser (String userId);

  /**
   * Mark the current user to be removed
   */
  public void markRemoveCurrentGuestUser(String bareJid);
  
//  /**
//   * Reset a user's password after verifying the question and answer.  The
//   * temporary password will be sent via email.  The <code>msg</code> is a text
//   * message with the macros: ${name}, ${password}, ${userid}
//   * @param userId A user name with %appID.
//   * @param question A question being asked.
//   * @param answer A case insensitive answer.
//   * @param msg A text message template as an email.
//   * @return
//   */
//  public String resetPassword(String userId, String question, String answer, String msg);
}

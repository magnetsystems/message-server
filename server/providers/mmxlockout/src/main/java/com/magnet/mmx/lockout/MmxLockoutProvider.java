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
package com.magnet.mmx.lockout;

import com.magnet.mmx.server.common.utils.MmxAppUtil;

import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.lockout.DefaultLockOutProvider;
import org.jivesoftware.openfire.lockout.LockOutFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An LockOut provider extending Openfire DefaultLockOutProvider to authenticate an MMX client connection.
 * This is called by SASLAuthentication to determine if use is locked out or not
 *
 * The username can be of the format:
 *
 * <user-name>[%<app-id>][@<domain>]
 *
 * If @ is found in the username, it will first check if it is a valid domain.
 * If the domain is valid, then set username to the substring before @.
 *
 * If % is found in the username, it will check if there is a valid app id following %.
 */
public class MmxLockoutProvider extends DefaultLockOutProvider {

  private static final Logger Log = LoggerFactory.getLogger(MmxLockoutProvider.class) ;

  public MmxLockoutProvider() {
    super();
    Log.info("MmxLockoutProvider: new instance ");
  }

  @Override
  public LockOutFlag getDisabledStatus(String username) {
    Log.info("MmxLockoutProvider: getDisabledStatus user "+username);

    LockOutFlag lockOutFlag = super.getDisabledStatus(username);
    if ( lockOutFlag != null) {
      return lockOutFlag;
    }

    String xid = null;
    try {
      xid = MmxAppUtil.extractUserName(username.trim().toLowerCase());
      MmxAppUtil.checkAppId(xid);
      return lockOutFlag; // fall back to default (null)

    } catch (UnauthorizedException e) {
      // this username is invalid wrt the app id; mark lockout for subsequent logins
      lockOutFlag = new LockOutFlag(xid, null, null);
      setDisabledStatus(lockOutFlag);
      return lockOutFlag;
    }
  }
}

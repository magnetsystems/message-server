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
package com.magnet.mmx.server.plugin.mmxmgmt.interceptor;

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import java.util.Collection;

/**
 */
public class MMXPresenceFinderImpl implements MMXPresenceFinder {
  private static Logger LOGGER = LoggerFactory.getLogger(MMXPresenceFinderImpl.class);

  @Override
  public boolean isOnline(JID user) {
    long start = System.nanoTime();
    SessionManager sessionManager = getSessionManager();
    Collection<ClientSession> sessions = sessionManager.getSessions(user.getNode());
    boolean activeSession = false;
    for (ClientSession clientSession : sessions) {
      JID clientSessionAddress = clientSession.getAddress();
      //compare the node and the resource
      if (clientSessionAddress.getNode().equals(user.getNode()) && clientSessionAddress.getResource().equals(user.getResource())) {
        //if (clientSession.getAddress().equals(user)) {
        if (clientSession.isInitialized()) {
          activeSession = true;
          break;
        }
      }
    }
    long end = System.nanoTime();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format ("For user:%s presence value:%s", user, activeSession));
      long delta = end - start;
      LOGGER.debug("Presence was checked in:" + (delta/1000) + " ms");
    }
    return activeSession;
  }

  /**
   * Get the session manager
   * @return
   */
  protected SessionManager getSessionManager() {
    return SessionManager.getInstance();
  }
}

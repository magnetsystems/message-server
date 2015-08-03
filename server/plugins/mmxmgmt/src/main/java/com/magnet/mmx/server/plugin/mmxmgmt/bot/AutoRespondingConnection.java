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

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.SessionPacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.AuthToken;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.net.VirtualConnection;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.LocalClientSession;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.StreamError;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A virtual connection for building auto responding users
 * Code in this class is inspired by:
 * https://community.igniterealtime.org/docs/DOC-1130
 */
public class AutoRespondingConnection extends VirtualConnection {
  private static final Logger LOGGER = LoggerFactory.getLogger(AutoRespondingConnection.class);

  /**
   * A processor that can handle the incoming messages and respond
   * using customized logic
   */
  private AutoResponseProcessor packetProcessor;

  /**
   * Holds the initialization state of the packet receiver.
   */
  private boolean initProcessor;

  /**
   * Pointer to the session for auto responding bot user.
   */
  private LocalClientSession localClientSession;


  /**
   * Creates a new instance of AutoResponseConnection with provided response processor.
   * <p/>
   * When login is attempted with an instance created with this constructor,
   * the packetReceiver traps incoming packets and texts as soon as the bot
   * logs on.
   *
   * @param processor AutoResponseProcessor packet processor
   */
  public AutoRespondingConnection(AutoResponseProcessor processor) {
    this.packetProcessor = processor;
  }

  /**
   * The method will be implicitly called by the server when the auto responding user's
   * connection is (virtually) closed. The method terminates the packet
   * processor.
   */
  @Override
  public void closeVirtualConnection() {
    if (packetProcessor != null && initProcessor) {
      packetProcessor.terminate();
      initProcessor = false;
    }
  }

  /**
   * Calls to this method is made by the server to deliver packets to the bot.
   * This method will in turn call
   * {@link AutoResponseProcessor#processIncoming(Packet)} of the packet receiver
   * associated with the bot.
   *
   * @param packet XMPP packet
   * @throws UnauthorizedException When packets could not be delivered due to authorization
   *                               problem.
   */
  public void deliver(Packet packet) throws UnauthorizedException {
    if (packetProcessor == null)
      return;
    packetProcessor.processIncoming(packet);
  }

  /**
   * Calls to this method is made by the server to deliver raw text to the
   * bot. This method will in turn call
   * {@link AutoResponseProcessor#processIncomingRaw(String)} of the packet
   * receiver associated with the bot.
   *
   * @param text The text string delivered to the bot.
   */
  public void deliverRawText(String text) {
    if (packetProcessor == null)
      return;
    packetProcessor.processIncomingRaw(text);
  }


  /**
   * Get the resource portion of the bot's JID.
   *
   * @return Resource portion of the bot's JID.
   */
  public String getResource() {
    if (localClientSession == null)
      return null;
    return localClientSession.getAddress().getResource();
  }

  /**
   * Get the node's portion of the bot's JID.
   *
   * @return Node portion of the bot's JID.
   */
  public String getUsername() {
    if (localClientSession == null)
      return null;
    return localClientSession.getAddress().getNode();
  }

  /**
   * Check whether the bot session is still active.
   *
   * @return <tt>true</tt> if the bot is still active, <tt>false</tt>
   * otherwise.
   */
  public boolean isLoggedOn() {
    return !isClosed();
  }

  /**
   * Mechanism to process login for the auto responding user. This
   * creates the specified user if it doesn't exist.
   *
   * @param username The username to login with.
   * @param resource The resource the user will bind to.
   * @throws SessionAlreadyExistsException If the bot's session already exists.
   * @throws UserNotFoundException         If it fails to create the user.
   * @see #login(String, String, boolean)
   */
  public void login(String username, String resource)
      throws SessionAlreadyExistsException, UserNotFoundException {
    login(username, resource, true);
  }

  /**
   * Login to the XMPP server and establish a non-anonymous user session using
   * the given username and resource. When <tt>createIfNotExist</tt> is
   * <tt>true</tt>, a new user with the username will be created and stored
   * in the database if it does not exist. When <tt>false</tt>, and the
   * user does not exist, the method will not attempt the login. Whenever
   * there's an error, the bot will not login.
   *
   * @param username         Username to login with.
   * @param resource         The resource the user will bind to.
   * @param createIfNotExist When specified as <tt>true</tt>, a new user will be created
   *                         and stored in the database if it does not exist.
   * @throws SessionAlreadyExistsException If the bot's session already exists.
   * @throws UserNotFoundException         If it fails to create the user.
   */
  public void login(String username, String resource, boolean createIfNotExist)
      throws SessionAlreadyExistsException, UserNotFoundException {
    LOGGER.debug("Bot login with username:{} with resource:{}", username, resource);

    if (isClosed())
      throw new SessionAlreadyExistsException();

    JID jid = new JID(username.toLowerCase(), XMPPServer.getInstance()
        .getServerInfo().getXMPPDomain(), resource);
    ClientSession oldSession = SessionManager.getInstance().getSession(jid);

    // Check for session conflict
    if (oldSession != null) {
      try {
        int count = oldSession.incrementConflictCount();
        int conflictLimit = SessionManager.getInstance()
            .getConflictKickLimit();
        if (conflictLimit != SessionManager.NEVER_KICK
            && count > conflictLimit) {
          // Kick out the old connection that is conflicting with the
          // new one
          StreamError error = new StreamError(
              StreamError.Condition.conflict);
          oldSession.deliverRawText(error.toXML());
          oldSession.close();
        } else
          throw new SessionAlreadyExistsException();
      } catch (Exception e) {
        LOGGER.error("Error during login", e);
      }
    }

    if (!XMPPServer.getInstance().getUserManager().isRegisteredUser(
        jid.getNode())) {
      if (createIfNotExist) {
        try {
          // Bot doesn't care of whatever password it is.
          XMPPServer.getInstance().getUserManager().createUser(
              jid.getNode(), StringUtils.randomString(15), null,
              null);
        } catch (UserAlreadyExistsException e) {
          // Ignore
        }
      } else {
        throw new UserNotFoundException();
      }
    }

    localClientSession = SessionManager.getInstance().createClientSession(this);

    localClientSession.setAuthToken(new AuthToken(jid.getNode()), jid.getResource());

    if (packetProcessor != null) {
      packetProcessor.initialize(this);
      initProcessor = true;
    }
  }

  /**
   * Logout the bot and destroy the active session. This method need not be
   * called explicitly unless, for example, when callers need to refresh the
   * assign a different username or resource (re-login).
   */
  public void logout() {
    close();
  }

  /**
   * Send a packet out to an XMPP entity. The packet must be one of
   * <message/>, <iq/> or <presence/>. Callers need not specify the
   * <tt>from</tt> attribute inside the packet because it will be
   * automatically inserted with/replaced by the bot's real JID.
   *
   * @param packet The packet to send.
   */
  public void sendPacket(Packet packet) {
    if (isClosed())
      throw new IllegalStateException("No valid session");
    SessionPacketRouter router = new SessionPacketRouter(localClientSession);
    router.route(packet);
  }


  @Override
  public String getHostAddress() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostAddress();
  }

  /**
   * Calls to this method is made by the server to notify about server
   * shutdown to the bot.
   */
  public void systemShutdown() {
    close();
  }


  @Override
  public byte[] getAddress() throws UnknownHostException {
    throw new UnsupportedOperationException("method not implemented");
  }

  @Override
  public String getHostName() throws UnknownHostException {
    throw new UnsupportedOperationException("method not implemented");
  }
}
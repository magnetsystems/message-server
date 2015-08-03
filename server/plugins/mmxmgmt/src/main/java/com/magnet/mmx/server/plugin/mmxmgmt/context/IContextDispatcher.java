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
package com.magnet.mmx.server.plugin.mmxmgmt.context;

import org.xmpp.packet.IQ;

public interface IContextDispatcher {

  /**
   * Return name of the context dispatcher registered
   * @return
   */
  public String getName();

  /**
   * Return type name of the context this Dispatcher can support
   * @return
   */
  public String getSupportedTypeName();

  /**
   * Return protocol names this Dispatcher can dispatch to
   * For example, "PROTOCOL_XMPP", "CONSOLE", "LOGGER", "FILE", etc
   */
  public String getSupportedProtocol();

  /**
   * Dispatch an event from IQ to an external listener
   * @param iq
   */
  public void dispatchToExternalService(IQ iq);

  /**
   * Shutdown the dispatcher and all its worker threads
   */
  public void shutdown();
}

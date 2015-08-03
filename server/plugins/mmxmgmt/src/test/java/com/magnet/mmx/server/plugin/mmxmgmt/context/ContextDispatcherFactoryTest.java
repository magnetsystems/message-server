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

import com.magnet.mmx.protocol.Constants;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class ContextDispatcherFactoryTest {

  @Before
  public void setUp() throws Exception {

  }

  @Test
  public void testGetInstance() throws Exception {
    assertTrue(ContextDispatcherFactory.getInstance() != null);
  }

  @Test
  public void testGetGeoDispatcher() throws Exception {
    Class.forName(GeoEventDispatcher.class.getName());  // load the class to initialize itself
    IContextDispatcher dispatcher = ContextDispatcherFactory.getInstance().getDispatcher((GeoEventDispatcher.class.getName()));
    assertNotNull(dispatcher);
    assertEquals(dispatcher.getSupportedTypeName(), Constants.MMX_MTYPE_GEOLOC);
    assertEquals(dispatcher.getSupportedProtocol(), GeoEventDispatcher.PROTOCOL_XMPP);
  }
}

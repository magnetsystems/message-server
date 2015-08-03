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
package com.magnet.mmx.server.plugin.mmxmgmt.message;

import com.magnet.mmx.protocol.Constants;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 */
public class MMXPubSubPayloadTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXPubSubPayloadTest.class);
  @Test
  public void parseTest() throws IOException, DocumentException {
    String s = "<mmx xmlns=\"com.magnet:msg:payload\"><meta>{\"key2\":\"value2\",\"key1\":\"value1\"}</meta><payload ctype=\"text/plain\" mtype=\"simpletext\" chunk=\"0/10/10\" stamp=\"2015-02-18T06:24:59.878Z\">Hi there 0</payload></mmx>";
//    String s = "<mmx xmlns=\"com.magnet:msg:payload\"><meta>"+"{\"key2\":\"value2\",\"key1\":\"value1\"}"+"</meta><payload ctype=\"text/plain\" mtype=\"simpletext\" chunk=\"0/10/10\" stamp=\"2015-02-18T06:24:59.878Z\">Hi there 0</payload></mmx>";
    SAXReader reader = new SAXReader();
    Document d = DocumentHelper.parseText(s);

    Node payloadElement = d.selectSingleNode(String.format("/*[name()='%s']/*[name()='%s']", Constants.MMX_ELEMENT, Constants.MMX_PAYLOAD));
    Node meta = d.selectSingleNode("/*[name()='mmx']/*[name()='meta']");

    MMXPubSubPayload payload;
    if(payloadElement != null) {
      String ctype = payloadElement.valueOf("@ctype");
      String stamp = payloadElement.valueOf("@stamp");
      String data = payloadElement.getText();
      payload = new MMXPubSubPayload(ctype, stamp, data);
      LOGGER.trace("parseTest : payload={}", payload);
    }

    if(meta != null) {
      String metaJson = meta.getText();
      LOGGER.trace("parseTest : meta value=\n{}", metaJson);
    }
  }
}

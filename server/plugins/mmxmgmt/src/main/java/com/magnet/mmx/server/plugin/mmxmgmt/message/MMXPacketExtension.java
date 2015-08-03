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

import java.util.Date;
import java.util.Map;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.PacketExtension;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Headers;
import com.magnet.mmx.protocol.Payload;
import com.magnet.mmx.server.plugin.mmxmgmt.interceptor.MMXMessageHandlingRule;
import com.magnet.mmx.util.DisposableBinFile;
import com.magnet.mmx.util.DisposableFile;
import com.magnet.mmx.util.FileUtil;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TimeUtil;
import com.magnet.mmx.util.Utils;

/**
 * MMX stanza encoder for MMX server.
 * <pre>
 * Message msg = new Message();
 * Headers headers = new Headers();
 * headers.put("name", "value");
 * Payload payload = new Payload("MyMsgType", content);
 * msg.addExtension(new MMXPacketExtension(headers, payload));
 * </pre>
 */
public class MMXPacketExtension extends PacketExtension {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXPacketExtension.class);
  private Headers mHeaders;
  private Payload mPayload;
  
  public MMXPacketExtension(Map<String, String> headers, Payload payload) {
    super(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    if (headers != null) {
      mHeaders = new Headers();
      mHeaders.putAll(headers);
    }
    mPayload = payload;
    DisposableFile file = mPayload.getFile();
    if (file != null && file.isBinary()) {
      if (mHeaders == null)
        mHeaders = new Headers();
      if (mHeaders.getContentEncoding(null) == null)
        mHeaders.setContentEncoding(Constants.BASE64);
    }
    fillElement();
  }
  
  public Map<String, String> getHeaders() {
    return mHeaders;
  }
  
  public Payload getPayload() {
    return mPayload;
  }
  
  private void fillElement() {
    // It seems that DOM4J does XML escape too, so don't escape again!
    final boolean xmlEsc = false;
    Date sentTime = new Date();
    mPayload.setSentTime(sentTime);
    
    if (mHeaders != null) {
      Element metaElement = this.element.addElement(Constants.MMX_META);
      // No need to do XML escape for the headers because GSON is XML safe.
      metaElement.setText(GsonData.getGson().toJson(mHeaders));
    }
    if (mPayload.getData() != null || mPayload.getFile() != null) {
      Element payloadElement = this.element.addElement(Constants.MMX_PAYLOAD);
      if (mPayload.getMsgType() != null) {
        payloadElement.addAttribute(Constants.MMX_ATTR_MTYPE, mPayload.getMsgType());
      }
      if (mPayload.getCid() != null) {
        payloadElement.addAttribute(Constants.MMX_ATTR_CID, mPayload.getCid());
      }
      payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, mPayload.formatChunk());
      payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, TimeUtil.toString(sentTime));
      CharSequence csq = null;
      if (mPayload.getData() != null) {
        if (!xmlEsc) {
          csq = mPayload.getData();
        } else {
          if (mPayload.getDataSize() >= Constants.PAYLOAD_THRESHOLD) {
            csq = FileUtil.encodeForXml(mPayload.getData());
          } else {
            csq = Utils.escapeForXML(mPayload.getData());
          }
        }
      } else if (mPayload.getFile() != null) {
        csq = FileUtil.encodeFile(mPayload.getFile(), xmlEsc);
        mPayload.getFile().finish();
      }
      if (csq != null) {
        payloadElement.setText(csq.toString());
      }
      LOGGER.warn("fillElement : payload={}", csq.toString());
    }
  }
}

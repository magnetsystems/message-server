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

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.PacketExtension;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Headers;
import com.magnet.mmx.protocol.MmxHeaders;
import com.magnet.mmx.protocol.Payload;
import com.magnet.mmx.util.DisposableFile;
import com.magnet.mmx.util.FileUtil;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TimeUtil;
import com.magnet.mmx.util.Utils;

/**
 * MMX stanza encoder and decoder in MMX server.  To encode to the MMX stanza,
 * <pre>
 * Message msg = new Message();
 * MmxHeaders mmxMeta = new MmxHeaders();
 * mmxMeta.put(...);
 * Headers headers = new Headers();
 * headers.put("name", "value");
 * Payload payload = new Payload("MyMsgType", content);
 * msg.addExtension(new MMXPacketExtension(mmxMeta, headers, payload));
 * </pre>
 * To decode MMX stanza, the starting element is &lt;mmx xmlns=...&gt;...&lt;/mmx&gt;
 * <pre>
 * MMXPacketExtension ext = new MMXPacketExtension(mmxElement);
 * MmxHeaders mmxMeta = ext.getMmxMeta();
 * Headers headers = ext.getHeaders();
 * Payload payload = ext.getPayload();
 * </pre>
 */
public class MMXPacketExtension extends PacketExtension {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXPacketExtension.class);
  private MmxHeaders mMmxMeta;
  private Headers mHeaders;
  private Payload mPayload;

  /**
   * Constructor to decode the MMX stanza.
   * @param mmxElement
   */
  public MMXPacketExtension(Element mmxElement) {
    super(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    parseElement(mmxElement);
  }

  /**
   * Constructor to create an MMX stanza without mmx meta headers.  Currently
   * it is used to construct custom error message.
   * @param headers
   * @param payload
   */
  public MMXPacketExtension(Map<String, String> headers, Payload payload) {
    this(null, headers, payload);
  }

  /**
   * Constructor to create an MMX stanza.
   * @param mmxMeta
   * @param headers
   * @param payload
   */
  public MMXPacketExtension(MmxHeaders mmxMeta, Map<String, String> headers, Payload payload) {
    super(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    if (mmxMeta != null ) {
      mMmxMeta = new MmxHeaders();
      mMmxMeta.putAll(mmxMeta);
    }
    if (headers != null) {
      mHeaders = new Headers();
      mHeaders.putAll(headers);
    }
    mPayload = payload;
    DisposableFile file = mPayload.getFile();
    if (file != null && file.isBinary()) {
      if (mHeaders == null) {
        mHeaders = new Headers();
      }
      if (mHeaders.getContentEncoding(null) == null) {
        mHeaders.setContentEncoding(Constants.BASE64);
      }
    }
    fillElement();
  }

  public Map<String, Object> getMmxMeta() {
    return mMmxMeta;
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

    if (mMmxMeta != null) {
      // No need to do XML escape for the MMX meta because GSON is XML safe.
      Element mmxMetaElement = this.element.addElement(Constants.MMX_MMXMETA);
      mmxMetaElement.setText(GsonData.getGson().toJson(mMmxMeta));
    }
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
    }
  }

  private void parseElement(Element mmxElement) {
    String text;
    Element mmxMetaElement = mmxElement.element(Constants.MMX_MMXMETA);
    mMmxMeta = (mmxMetaElement == null ||
        (text = mmxMetaElement.getText()) == null || text.isEmpty()) ?
        new MmxHeaders() : GsonData.getGson().fromJson(text, MmxHeaders.class);

    Element metaElement = mmxElement.element(Constants.MMX_META);
    mHeaders = (metaElement == null ||
        (text = metaElement.getText()) == null || text.isEmpty()) ?
        new Headers() : GsonData.getGson().fromJson(text, Headers.class);

    Element payloadElement = mmxElement.element(Constants.MMX_PAYLOAD);
    if (payloadElement == null) {
      mPayload = new Payload(null, (CharSequence) null);
    } else {
      String msgType, chunk, timestamp;
      msgType = getAttrValue(payloadElement, Constants.MMX_ATTR_MTYPE, null);
      chunk = getAttrValue(payloadElement, Constants.MMX_ATTR_CHUNK, null);
      timestamp = getAttrValue(payloadElement, Constants.MMX_ATTR_STAMP, null);
      text = payloadElement.getText();
      mPayload = new Payload(msgType, text);
      mPayload.parseChunk(chunk);
      mPayload.setSentTime(TimeUtil.toDate(timestamp));
    }
  }

  private String getAttrValue(Element element, String attrName, String def) {
    Attribute attr;
    if ((attr = element.attribute(attrName)) != null) {
      return attr.getStringValue();
    } else {
      return def;
    }
  }
}

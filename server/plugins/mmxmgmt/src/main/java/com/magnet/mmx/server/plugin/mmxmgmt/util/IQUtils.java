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

package com.magnet.mmx.server.plugin.mmxmgmt.util;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TopicHelper;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError.Condition;


public class IQUtils {


  public static IQ isValidGeoIQ(Packet packet) {
    if (packet instanceof IQ) {
      IQ iqPacket = (IQ)packet;
      if (IQ.Type.set == iqPacket.getType()) {
        JID to = iqPacket.getTo();
        if (to != null && to.toString().startsWith("pubsub")) {
          // find 'geoloc'
          Element element = iqPacket.getChildElement();
          Element action = element.element("publish");
          if (action != null) {
            String nodeID = action.attributeValue("node");
            if (nodeID != null && nodeID.endsWith(TopicHelper.TOPIC_GEOLOC)) {
              return iqPacket;
            }
          }
        }
      }
    }
    return null;
  }
  /**
   * Get the <code>command</code> attribute value from the IQ MMX element.
   * @param iq The request IQ.
   * @return The value of the <code>command</code> attribute.
   */
  public static String getCommand(IQ iq) {
    Element element = iq.getChildElement();
    return element.attributeValue(Constants.MMX_ATTR_COMMAND);
  }
  
  /**
   * Create an IQ MMX result from the request.  The element, namespace and
   * command are cloned.  The payload content type is set to JSON.
   * @param iq The request IQ.
   * @param payload The JSON payload.
   * @return An IQ MMX result.
   */
  public static IQ createResultIQ(IQ iq, String payload) {
    IQ result = IQ.createResultIQ(iq);
    Element rqtElt = iq.getChildElement();
    Element rstElt = result.setChildElement(rqtElt.getName(),
        rqtElt.getNamespace().getText());
    rstElt.addAttribute(Constants.MMX_ATTR_COMMAND, 
        rqtElt.attributeValue(Constants.MMX_ATTR_COMMAND));
    rstElt.addAttribute(Constants.MMX_ATTR_CTYPE, GsonData.CONTENT_TYPE_JSON);
    rstElt.setText(payload);
    return result;
  }
  
  /**
   * Create an error IQ from the request.  The element, namespace and command
   * are cloned.  The content type is set to JSON.
   * @param iq The request IQ.
   * @param msg  Optional message.
   * @param code
   * @return An IQ MMX error.
   */
  public static IQ createErrorIQ(IQ iq, String msg, int code) {
    IQ error = IQ.createResultIQ(iq);
    error.setType(IQ.Type.error);
    Element rqtElt = iq.getChildElement();
    Element errElt = error.setChildElement(rqtElt.getName(), 
        rqtElt.getNamespace().getText());
    errElt.addAttribute(Constants.MMX_ATTR_COMMAND, 
        rqtElt.attributeValue(Constants.MMX_ATTR_COMMAND));
    errElt.addAttribute(Constants.MMX_ATTR_CTYPE, GsonData.CONTENT_TYPE_JSON);
    MMXStatus status = new MMXStatus();
    status.setCode(code);
    status.setMessage(msg);
    errElt.setText(status.toJson());
    return error;
  }
  
  /**
   * Create an error IQ from the request using the XMPP error stanza.  The 
   * element, namespace and command are cloned.  The content type is XML.
   * @param iq
   * @param condition
   * @return An IQ MMX error with XMPP error stanza.
   */
  public static IQ createErrorIQ(IQ iq, Condition condition) {
    IQ error = IQ.createResultIQ(iq);
    error.setType(IQ.Type.error);
    Element rqtElt = iq.getChildElement();
    Element errElt = error.setChildElement(rqtElt.getName(), 
        rqtElt.getNamespace().getText());
    errElt.addAttribute(Constants.MMX_ATTR_COMMAND, 
        rqtElt.attributeValue(Constants.MMX_ATTR_COMMAND));
    errElt.addAttribute(Constants.MMX_ATTR_CTYPE, "application/xml");
    error.setError(condition);
    return error;
  }
  
  /**
   * Create an error IQ with proper from, to, and id.
   * @param iq
   * @param tag
   * @param namespace
   * @param msg
   * @param code
   * @return
   * @deprecated {@link #createErrorIQ(IQ, String, int)}
   */
  public static IQ createError(IQ iq, String tag, String namespace, String msg, int code) {
    IQ error = IQ.createResultIQ(iq);
    error.setType(IQ.Type.error);
    Element element = error.setChildElement(tag, namespace);
    MMXStatus iqErr = new MMXStatus();
    iqErr.setCode(code);
    iqErr.setMessage(msg);
    element.setText(iqErr.toJson());
    return error;
  }
  
  /**
   * Create an error IQ without specifying <code>from</code> and <code>id</code>.
   * @param to
   * @param tag
   * @param namespace
   * @param msg
   * @param code
   * @return
   * @deprecated {@link #createErrorIQ(IQ, String, int)}
   */
  public static IQ errorIQ(JID to, String tag, String namespace, String msg, int code) {
    IQ error = new IQ(IQ.Type.error);
    error.setTo(to);
    Element element = error.setChildElement(tag, namespace);
    MMXStatus iqErr = new MMXStatus();
    iqErr.setCode(code);
    iqErr.setMessage(msg);
    element.setText(iqErr.toJson());
    return error;
  }
}

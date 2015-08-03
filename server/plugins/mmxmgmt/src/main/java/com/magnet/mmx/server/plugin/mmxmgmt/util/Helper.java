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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.magnet.mmx.protocol.CarrierEnum;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.PushMessage;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.api.AbstractBaseResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TopicItemEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXPubSubItem;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.util.TopicHelper;
import org.jivesoftware.openfire.XMPPServer;
import org.xmpp.packet.JID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class that groups together utility functions.
 */
public class Helper {

  private static final String PHONE_NUMBER_VALIDATION_PATTERN_REGEX = "^\\d{10,}$";
  private static final Pattern PHONE_NUMBER_VALIDATION_PATTERN = Pattern.compile(PHONE_NUMBER_VALIDATION_PATTERN_REGEX);
  private static final String WAKEUP_MESSAGE = PushMessage.encode(PushMessage.Action.WAKEUP, Constants.PingPongCommand.retrieve.name(), null);
  private static final String APP_ID_DELIMITER = "%";

  private Helper() {
  }

  /**
   * Check if the <code>from</code> has admin capability.  Only the escaped
   * node part (userID) is validate.  TODO: it is not clear how the Openfire
   * stores the node as escaped or unescaped JID.
   *
   * @param from A full JID or bare JID.
   * @return
   */
  public static boolean isAdmin(JID from) {
    XMPPServer server = XMPPServer.getInstance();
    if (server.isLocal(from)) {
      String userId = from.getNode();
      for (JID admin : server.getAdmins()) {
//        if (JID.equals(admin.getNode(), userId)) {
//          return true;
//        }
        String adminNode = admin.getNode();
        if (adminNode.equals(userId)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isAppMgmtPermitted(JID from) {
    return isAdmin(from);
  }


  public static OSType enumerateOSType(String osType) {
    OSType rv = null;
    if (osType != null && !osType.isEmpty()) {
      try {
        rv = OSType.valueOf(osType);
      } catch (IllegalArgumentException e) {
        //bad value
      }
    }
    return rv;
  }

  public static PushType enumeratePushType (String pushType) {
    PushType rv = null;
    if (pushType != null && !pushType.isEmpty()) {
      try {
        rv = PushType.valueOf(pushType.toUpperCase());
      } catch (IllegalArgumentException e) {
        //bad value
      }
    }
    return rv;
  }

  public static PushMessageEntity.PushMessageType enumeratePushMessageType (String pushMessageType) {
    PushMessageEntity.PushMessageType rv = null;
    if (pushMessageType != null && !pushMessageType.isEmpty()) {
      try {
        rv = PushMessageEntity.PushMessageType.valueOf(pushMessageType);
      } catch (IllegalArgumentException e) {
        //bad value
      }
    }
    return rv;
  }

  public static PushMessageEntity.PushMessageState enumeratePushMessageState (String state) {
    PushMessageEntity.PushMessageState rv = null;
    if (state != null && !state.isEmpty()) {
      try {
        rv = PushMessageEntity.PushMessageState.valueOf(state);
      } catch (IllegalArgumentException e) {
        //bad value
      }
    }
    return rv;
  }

  public static CarrierEnum enumerateCarrierInfo (String carrierInfo) {
    CarrierEnum rv = null;
    if (carrierInfo != null && !carrierInfo.isEmpty()) {
      try {
        rv = CarrierEnum.valueOf(carrierInfo);
      } catch (IllegalArgumentException e) {
        //bad value
      }
    }
    return rv;
  }

  public static PushStatus enumeratePushStatus (String status) {
    PushStatus rv = null;
    if (status != null && !status.isEmpty()) {
      try {
        rv = PushStatus.valueOf(status);
      } catch (IllegalArgumentException e) {
        //bad value
      }
    }
    return rv;
  }

  public static DeviceStatus enumerateDeviceStatus (String status) {
    DeviceStatus rv = null;
    if (status != null && !status.isEmpty()) {
      try {
        rv = DeviceStatus.valueOf(status.toUpperCase());
      } catch (IllegalArgumentException e) {
        //bad value
      }
    }
    return rv;
  }

  /**
   * Check if the passed in phone number string is a valid phone number
   * @param phoneNumber
   * @return true if it is a valid phone number false other wise
   */
  public static boolean checkPhoneNumber (String phoneNumber) {
    if (phoneNumber == null) {
      return false;
    }
    Matcher matcher = PHONE_NUMBER_VALIDATION_PATTERN.matcher(phoneNumber);
    return matcher.matches();
  }

  /**
   * Reverse the input string
   * @param input
   * @return
   */
  public static String reverse (String input) {
    if (input == null) {
      return input;
    }
    String rv = null;
    int size = input.length();
    if (size == 1) {
      rv = input;
    } else {
      StringBuilder builder = new StringBuilder(size);

      for (int i = size - 1; i >= 0; i--) {
        char c = input.charAt(i);
        builder.append(c);
      }
      rv = builder.toString();
    }
    return rv;
  }

  /**
   * Remove the suffix after the delimiter. The delimiter is also removed.
   * @param input
   * @param delimiter
   * @return
   */
  public static String removeSuffix(String input, String delimiter) {
    int index = input.indexOf(delimiter);
    String stripped = input;
    if (index != -1) {
      stripped = input.substring(0, index);
    }
    return stripped;
  }

  public static String getStandardWakeupMessage() {
    return WAKEUP_MESSAGE;
  }

  /**
   * Build a simplified version of topicId. Simplified version doesn't
   * include the  "/<appId/{@literal *}/"
   * prefix or the "/i223hxed420/" prefix
   * @param appId
   * @param topicId
   * @return
   */
  public static String simplify(String appId, String topicId) {
    String appPrefix = "/" + appId; // should I cache these objects to prevent repeated construction ?
    String starPrefix = "/" + "*";
    String slash = "/";
    int removeIndex = 0;
    int appIndex = topicId.indexOf(appPrefix, removeIndex);
    if (appIndex > -1) {
      removeIndex = removeIndex + appPrefix.length();
    }
    int starIndex = topicId.indexOf(starPrefix, removeIndex);
    if (starIndex > -1) {
      removeIndex = removeIndex + starPrefix.length();
    }
    if (topicId.charAt(removeIndex) == '/') {
      removeIndex = removeIndex + slash.length();
    }
    String rv = topicId.substring(removeIndex);
    return rv;
  }

  /**
   * Get the XEP-0106 compliant username for MMX.  This username is typically
   * used by the services in Openfire.
   * @param username A human readable user ID.
   * @param appId An app ID.
   * @return An escaped MMX username.
   */
  public static String getMMXUsername(String username, String appId) {
    return JID.escapeNode(username) + Helper.APP_ID_DELIMITER + appId;
  }
  
  /**
   * Get the human readable username for MMX.  This username is typically used
   * by REST/Javascript API.
   * @param username A human readable user ID.
   * @param appId An app ID.
   * @return A non-escaped MMX username.
   */
  public static String getReadableMMXUsername(String username, String appId) {
    return username + Helper.APP_ID_DELIMITER + appId;
  }

  /**
   * Get a string like ?,?,? for count = 3
   * @param count
   * @return
   */
  public static String getSQLPlaceHolders(int count) {
    StringBuilder builder = new StringBuilder();
    for (int i=0; i<count; i++) {
      builder.append("?,");
    }
    builder.setLength(builder.length()-1);
    return builder.toString();
  }

  /**
   * THe method will return an empty list
   *
   * @param commaDelimitedStr Eg "aaaa,bbbb, cccc , dddd,,eeee"
   * @return List : {"aaaa", "bbbb", "cccc", "dddd", "eeee"}
   */
  public static ArrayList<String> getListFromCommaDelimitedString(String commaDelimitedStr) {
    return Lists.newArrayList(FluentIterable.from(Splitter.on(",").trimResults().split(Strings.nullToEmpty(commaDelimitedStr))).filter(new Predicate<String>() {
      @Override
      public boolean apply(final String input) {
        return !Strings.isNullOrEmpty(input);
      }
    }));
  }

  public static List<MMXPubSubItem> getPublishedItems(final String appId, List<TopicItemEntity> entityList) {
    Function<TopicItemEntity, MMXPubSubItem> entityToItem =
            new Function<TopicItemEntity, MMXPubSubItem>() {
              public MMXPubSubItem apply(TopicItemEntity i) {
                return new MMXPubSubItem(appId, TopicHelper.parseNode(i.getNodeId()).getName(), i.getId(), new JID(i.getJid()), i.getPayload());
              };
            };

    return Lists.transform(entityList, entityToItem);
  }

  public static boolean validateStringLen(List<String> strings, int length) {
    for(String s : strings) {
      if(Strings.isNullOrEmpty(s) || s.length() > length)
        return false;
    }
    return true;
  }

  public static boolean validateTag(String tag) {
    return validateStringLen(Arrays.asList(tag), MMXServerConstants.MAX_TAG_LENGTH);
  }

  public static boolean validateTags(List<String> tags) {
    return validateStringLen(tags, MMXServerConstants.MAX_TAG_LENGTH);
  }

  public static SortOrder validateSortOrder(String input) throws AbstractBaseResource.ValidationException {
    if (input == null || input.isEmpty()) {
      return null;
    }
    SortOrder sortOrder = SortOrder.from(input);
    if (sortOrder == null) {
      String message = String.format(ErrorMessages.ERROR_INVALID_SORT_ORDER_VALUE, input);
      throw new AbstractBaseResource.ValidationException(new ErrorResponse(ErrorCode.INVALID_SORT_ORDER_VALUE, message));
    }
    return sortOrder;
  }
}

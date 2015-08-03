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
package com.magnet.mmx.server.plugin.mmxmgmt.wakeup;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpRetryException;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of WakeupNotifier interface using GCM
 * GCM API is at http://developer.android.com/reference/com/google/android/gcm/server/package-summary.html
 */
public class GCMWakeupNotifierImpl implements WakeupNotifier {

  private static Logger LOGGER = LoggerFactory.getLogger(GCMWakeupNotifierImpl.class);
  private static final int RETRY_COUNT = 5;

  @Override
  public List<NotificationResult> sendNotification(List<String> deviceTokens, String payload, NotificationSystemContext context) {
      if (context instanceof GCMNotificationSystemContext) {
        return this.sendNotification(deviceTokens, payload, ((GCMNotificationSystemContext) context).getSenderIdentifier());
      } else {
        throw new IllegalArgumentException("Need an instance of GCMNotificationSystemContext");
      }
  }

  public List<NotificationResult> sendNotification(List<String> deviceTokens, String payload, String senderIdentifier) {
    String tokens = deviceTokens.toString();

    LOGGER.debug(String.format("Sending: %s to tokens:%s", payload, tokens));

    //Prepare a simple payload to push
    NotificationResult[] results = new NotificationResult[deviceTokens.size()];

    Sender sender = new Sender(senderIdentifier);
    Message.Builder mb = new Message.Builder().
        addData("msg", payload);


    MulticastResult mcResult = null;
    try {
      mcResult = sender.send(mb.build(), deviceTokens, RETRY_COUNT);
    } catch (IOException e) {
      LOGGER.error(String.format("Sending:%s to tokens:%s failed with an exception", payload, tokens), e);
      if (e instanceof HttpRetryException && ((HttpRetryException) e).responseCode() == 401) {
          LOGGER.error("Got status code:401 for Google API Key:{}", senderIdentifier);
          for (int i = 0; i < results.length; i++) {
            results[i] = NotificationResult.DELIVERY_FAILED_INVALID_API_KEY;
          }
      } else {
          for (int i = 0; i < results.length; i++) {
            results[i] = NotificationResult.DELIVERY_FAILED_PERMANENT;
          }
      }
    }
    if(null != mcResult) {
      for(int i = 0; i < mcResult.getResults().size(); i++) {
        Result r = mcResult.getResults().get(i);
        String error = r.getErrorCodeName();
        if (null != error) {
          if (error.equals(Constants.ERROR_NOT_REGISTERED) || error.equals(Constants.ERROR_INVALID_REGISTRATION) ||
              error.equals(Constants.ERROR_MISSING_REGISTRATION) || error.equals(Constants.ERROR_MISMATCH_SENDER_ID)) {
            results[i] = NotificationResult.DELIVERY_FAILED_INVALID_TOKEN;
          } else if (error.equals(Constants.ERROR_MESSAGE_TOO_BIG)) {
            //message is too big
            results[i] = NotificationResult.DELIVERY_FAILED_MESSAGE_TOO_BIG;
          } else {
            results[i] = NotificationResult.DELIVERY_FAILED_PERMANENT;
          }
          LOGGER.warn(String.format("Sending:%s to token:%s failed with error code:%s", payload, deviceTokens.get(i), error));
        } else {
          results[i] = NotificationResult.DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER;
        }
      }
    }
    return Arrays.asList(results);
  }


  public static class GCMNotificationSystemContext implements NotificationSystemContext {
    private String senderIdentifier;

    public GCMNotificationSystemContext(String senderIdentifier) {
      this.senderIdentifier = senderIdentifier;
    }

    public String getSenderIdentifier() {
      return this.senderIdentifier;
    }
  }


}

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
package com.magnet.mmx.server.plugin.mmxmgmt.gcm;

import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.util.JSONifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Validate the API Key
 */
public class GCMAPIKeyValidator {
  private static Logger LOGGER = LoggerFactory.getLogger(GCMAPIKeyValidator.class);

  private static final String GCM_ENDPOINT = "https://android.googleapis.com/gcm/send";
  private static final String BOGUS_KEY = "bad_reg_id";

  public static void validate (String googleApiKey) throws GCMAPIKeyValidationException {
    ValidationPayload payload = new ValidationPayload();
    payload.add(BOGUS_KEY);
    String json = payload.toJson();
    HttpURLConnection connection = null;
    try {
      connection = makePost(googleApiKey, json);
      int responseCode = connection.getResponseCode();
      if (responseCode == 200) {
        LOGGER.info("Supplied google API key is valid");
      } else if (responseCode == 401) {
        String message = String.format("Supplied google API key:%s is invalid", googleApiKey);
        LOGGER.info(message);
        throw new GCMAPIKeyValidationException(message);
      }
    } catch (IOException e) {
      LOGGER.warn("IO exception", e);
      throw new GCMAPIKeyValidationException(e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }


  private static HttpURLConnection makePost(String googleApiKey, String payload) throws IOException {
    if  (payload == null) {
      throw new IllegalArgumentException("arguments cannot be null");
    }
    if (!GCM_ENDPOINT.startsWith("https://")) {
      LOGGER.warn("URL does not use https: " + GCM_ENDPOINT);
    }
    LOGGER.debug("Sending POST to " + GCM_ENDPOINT);
    LOGGER.debug("POST body: " + payload);
    byte[] bytes = payload.getBytes(Constants.UTF8_CHARSET);
    HttpURLConnection conn = getConnection(GCM_ENDPOINT);
    conn.setDoOutput(true);
    conn.setUseCaches(false);
    conn.setFixedLengthStreamingMode(bytes.length);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_TYPE.toString());
    conn.setRequestProperty("Authorization", "key=" + googleApiKey);
    OutputStream out = conn.getOutputStream();
    try {
      out.write(bytes);
    } finally {
      try {
        if (out != null) {
          out.close();
          out = null;
        }
      } catch (IOException ioe) {
        //ignore
      }
    }
    return conn;
  }

  /**
   * Gets an {@link HttpURLConnection} given an URL.
   */
  protected static HttpURLConnection getConnection(String url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    return conn;
  }

  /**
   * Exception to indicate validation problems
   */
  public static class GCMAPIKeyValidationException extends  Exception {
    public GCMAPIKeyValidationException(String message, Throwable cause) {
      super(message, cause);
    }

    public GCMAPIKeyValidationException(Throwable cause) {
      super(cause);
    }
    public GCMAPIKeyValidationException(String message) {
      super(message);
    }
  }


  public static class ValidationPayload extends JSONifiable {

    @SerializedName("registration_ids")
    public List<String> registrationIds = new ArrayList<String>();

    public void add(String reqistrationId) {
      registrationIds.add(reqistrationId);
    }

  }

}

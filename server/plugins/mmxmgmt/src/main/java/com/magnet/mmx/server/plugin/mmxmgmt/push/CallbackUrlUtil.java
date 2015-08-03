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
package com.magnet.mmx.server.plugin.mmxmgmt.push;

import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.DefaultEncryptor;
import com.magnet.mmx.util.EncryptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.regex.Pattern;

/**
 */
public class CallbackUrlUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(CallbackUrlUtil.class);
  private static final String QUESTION = Character.toString('?');
  private static final String EQUAL = Character.toString('=');
  private static final String DELIMITER = "*";
  public static final String KEY_CALL_BACK_URL_TOKEN = "tk";
  private static final String TOKEN_ENCRYPTION_KEY ="895DCE68BDE82TRA";
  private static final DefaultEncryptor encryptor;

  static {
    try {
      EncryptorConfig config = new EncryptorConfig();
      config.setAlgo("AES");
      config.setHashAlgo("MD5");
      config.setMode("CBC");
      config.setPadding("PKCS5Padding");
      config.setKeySize(16);
      config.setRandomAlgo("SH1PRNG");
      encryptor = new DefaultEncryptor(config, TOKEN_ENCRYPTION_KEY.getBytes());
    } catch (Throwable t) {
      LOGGER.error("Exception in initializing the encryptor", t);
      throw new ExceptionInInitializerError(t);
    }
  }



  public static String buildCallBackURL(String pushMessageId) {
    String baseURL = CallbackURLProvider.getBaseCallbackURL();
    StringBuilder callBackBuilder = new StringBuilder(500);
    callBackBuilder.append(baseURL);
    callBackBuilder.append(QUESTION);
    String token = callBackURLToken(pushMessageId, new Date());
    callBackBuilder.append(KEY_CALL_BACK_URL_TOKEN).append(EQUAL);
    String urlEncoded = token;
    try {
      urlEncoded = URLEncoder.encode(token, "utf-8");
    } catch (UnsupportedEncodingException e) {
      LOGGER.warn("Exception in url encoding the token:" + token, e);
    }
    callBackBuilder.append(urlEncoded);
    return callBackBuilder.toString();
  }


  public static String callBackURLToken(String pushMessageId, Date currentTime) {
    long utcTime = currentTime.getTime();
    long utcSeconds = utcTime/1000;
    StringBuilder concat = new StringBuilder();
    concat.append(pushMessageId).append(DELIMITER).append(utcSeconds);
    String token = encryptor.encodeToString(concat.toString().getBytes());
    return token;
  }

  /**
   * Decode the token. If the token can't be decoded return a null.
   * @param token
   * @return
   */
  public static String[] decodeToken(String token) {
    String [] rv = null;
    try {
      byte[] decoded = null;
      decoded = encryptor.decodeFromString(token);
      String joined = new String(decoded);
      rv = joined.split(Pattern.quote(DELIMITER));
    } catch (Exception e) {
      LOGGER.error("Exception in decoding token:" + token, e);
    }
    return rv;
  }

  static class CallbackURLProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackURLProvider.class);

    /**
     * Get the call back url. This doesn't cache the config values since they can be changed using the rest API
     * @return
     */
    public static String getBaseCallbackURL() {
      MMXConfiguration configuration = MMXConfiguration.getConfiguration();
      StringBuilder builder = new StringBuilder(100);
      String protocol = configuration.getString(MMXConfigKeys.PUSH_CALLBACK_PROTOCOL, MMXServerConstants.DEFAULT_PUSH_CALLBACK_PROTOCOL);
      builder.append(protocol);
      builder.append("://");
      String host = configuration.getString(MMXConfigKeys.PUSH_CALLBACK_HOST);
      builder.append(host);
      builder.append(":");
      String port = configuration.getString(MMXConfigKeys.PUSH_CALLBACK_PORT);
      if (port == null) {
        if ("https".equals(protocol)) {
          port = configuration.getString(MMXConfigKeys.REST_HTTPS_PORT, Integer.toString(MMXServerConstants.DEFAULT_REST_HTTPS_PORT));
        } else {
          port = configuration.getString(MMXConfigKeys.REST_HTTP_PORT, Integer.toString(MMXServerConstants.DEFAULT_REST_HTTPS_PORT));
        }
      }
      if (port != null && !port.isEmpty()) {
        builder.append(port);
      }
      builder.append(MMXServerConstants.PUSH_CALLBACK_CONTEXT);
      builder.append(MMXServerConstants.PUSH_CALLBACK_ENDPOINT);
      String baseURL = builder.toString();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Base call back URL: {}", baseURL);
      }
      return baseURL;
    }
  }



}

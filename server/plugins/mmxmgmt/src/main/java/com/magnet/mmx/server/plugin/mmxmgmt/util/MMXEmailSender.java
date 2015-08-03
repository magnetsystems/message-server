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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;

/**
 */
public class MMXEmailSender {
  private static final String MMX_EMAIL_PERMIT = "mmxEmailPermit";

  private static final Logger LOGGER = LoggerFactory.getLogger(MMXEmailSender.class);

  public void send(EmailConfig emailConfig, String destination, String body) {

    try {
      Properties props = System.getProperties();
      props.put("mail.transport.protocol", "smtp");
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
      props.put("mail.smtp.port", emailConfig.getPort());

      Session session = Session.getDefaultInstance(props);

      Transport transport = null;
      List<String> bccList = MMXConfiguration.getConfiguration().getList(MMXConfigKeys.ALERT_EMAIL_BCC_LIST);
      try {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(MMXServerConstants.EMAIL_SENDER));

        if (destination != null)
          msg.setRecipient(Message.RecipientType.TO, new InternetAddress(destination));
        InternetAddress[] addressBcc = new InternetAddress[bccList.size()];
        int i = 0;
        for (String s : bccList) {
          LOGGER.trace("send : Adding bcc : {}", s);
          addressBcc[i++] = new InternetAddress(s);
        }
        msg.setRecipients(MimeMessage.RecipientType.BCC, addressBcc);
        msg.setSubject(MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_SUBJECT, MMXServerConstants.DEFAULT_ALERT_EMAIL_SUBJECT));
        msg.setContent(body, "text/plain");
        LOGGER.trace("send : Sending alert email to={}, body={}", destination, body);
        transport = session.getTransport();
        transport.connect(emailConfig.getHost(), emailConfig.getUsername(), emailConfig.getPassword());
        transport.sendMessage(msg, msg.getAllRecipients());
      } catch (MessagingException e) {
        LOGGER.error("send : could not send email to {}, bccList = {}", destination, bccList, e);
      } finally {
        if (transport != null) {
          try {
            transport.close();
          } catch (MessagingException e) {
            LOGGER.error("send : Exception closing transport", e);
            e.printStackTrace();
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("send : caught exception body={}", body, e);
    }
  }

  public void send(String destination, String body) {
    send(getEmailConfigFromMMXConfig(), destination, body);
  }

  public void sendToBccOnly(String body) {
    send(getEmailConfigFromMMXConfig(), null, body);
  }

  private EmailConfig getEmailConfigFromMMXConfig() {
    String host = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_HOST, MMXServerConstants.DEFAULT_EMAIL_HOST);
    String port = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_PORT, Integer.toString(MMXServerConstants.DEFAULT_SMTP_PORT));
    String user = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_USER, MMXServerConstants.DEFAULT_EMAIL_USER);
    String password = MMXConfiguration.getConfiguration().getString(MMXConfigKeys.ALERT_EMAIL_PASSWORD, MMXServerConstants.DEFAULT_EMAIL_PASSWORD);
    EmailConfig emailConfig = new EmailConfig(host, port, user, password);
    LOGGER.trace("getEmailConfigFromMMXConfig : created config : {}", emailConfig);
    return emailConfig;
  }
}

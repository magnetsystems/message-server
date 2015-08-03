package com.magnet.mmx.server.plugin.mmxmgmt.apns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Created by rphadnis on 4/23/15.
 */
public class APNSCertificateValidator {
  private static final Logger LOGGER = LoggerFactory.getLogger(APNSCertificateValidator.class);

  private static final String PCKSC12_KEY_STORE_TYPE =  "pkcs12";


  /**
   * Validate APNS certificate. This implementation attempts to open the certificate as a pkcs12 file.
   * @param certificationStream
   * @param password
   * @throws APNSCertificationValidationException indicates that the certificate couldn't be opened.
   */
  public static void validate(InputStream certificationStream, char[] password) throws APNSCertificationValidationException {
    try {
      KeyStore p12 = KeyStore.getInstance(PCKSC12_KEY_STORE_TYPE);
      p12.load(certificationStream, password);
      Enumeration aliases = p12.aliases();
      while (aliases.hasMoreElements()) {
        String alias = (String) aliases.nextElement();
        LOGGER.info("alias" + " - " + alias);
        X509Certificate c = (X509Certificate) p12.getCertificate(alias);
        Principal subject = c.getSubjectDN();
        String subjectArray[] = subject.toString().split(",");
        for (String s : subjectArray) {
          String[] str = s.trim().split("=");
          String key = str[0];
          String value = str[1];
          LOGGER.info(key + ":" + value);
        }
      }
    } catch (CertificateException e) {
      LOGGER.info("Certification exception", e);
      throw new APNSCertificationValidationException(e);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.info("NoSuchAlgorithmException", e);
      throw new APNSCertificationValidationException(e);
    } catch (KeyStoreException e) {
      LOGGER.info("KeyStoreException", e);
      throw new APNSCertificationValidationException(e);
    } catch (IOException e) {
      LOGGER.info("IOException", e);
      throw new APNSCertificationValidationException(e);
    }
  }


  /**
   * Exception to indicate validation problems
   */
  public static class APNSCertificationValidationException extends  Exception {
    public APNSCertificationValidationException(String message, Throwable cause) {
      super(message, cause);
    }

    public APNSCertificationValidationException(Throwable cause) {
      super(cause);
    }
  }
}

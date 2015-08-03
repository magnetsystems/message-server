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
package com.magnet.yak.util;

import java.security.SecureRandom;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SslUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(SslUtil.class);
    
	private static class NaiveTrustManager implements X509TrustManager {
	    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
	        throws CertificateException {
	    }

	    public void checkServerTrusted(X509Certificate[] arg0, String arg1)
	        throws CertificateException {
	    }

	    public X509Certificate[] getAcceptedIssuers() {
	      return null;
	    }
    }
	
	public synchronized static HostnameVerifier getNaiveHostnameVerifier() {
	   return new AllowAllHostnameVerifier(); 
	}
	
	public synchronized static SocketFactory getNaiveSocketFactory() {
	      try {
	        TrustManager[] tm = new TrustManager[] { new NaiveTrustManager() };
	        SSLContext context;
	        context = SSLContext.getInstance("TLS");
	        context.init( new KeyManager[0], tm, new SecureRandom());
	        return context.getSocketFactory();
	      } catch (Exception e) {
	        LOGGER.error("getNaiveSocketFactory : {}", e);
	        return null;
	      }
	}
}

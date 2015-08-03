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
package com.magnet.yak.config;

public class MMXConfig {
	private String host;
	private String port;
	private String apiKey;
	private String appId;
	private String guestSecret;
	private String guestUserId;
	private String appPath;
	private boolean enableSendLastPubItem;
	private boolean enableCompression;
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getApiKey() {
		return apiKey;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	public String getGuestSecret() {
		return guestSecret;
	}
	public void setGuestSecret(String guestSecret) {
		this.guestSecret = guestSecret;
	}
	public String getGuestUserId() {
		return guestUserId;
	}
	public void setGuestUserId(String guestUserId) {
		this.guestUserId = guestUserId;
	}
	public String getAppPath() {
		return appPath;
	}
	public void setAppPath(String appPath) {
		this.appPath = appPath;
	}
	
	public boolean isEnableSendLastPubItem() {
		return enableSendLastPubItem;
	}
	
	public void setEnableSendLastPubItem(boolean enableSendLastPubItem) {
		this.enableSendLastPubItem = enableSendLastPubItem;
	}
	
	public boolean isEnableCompression() {
		return enableCompression;
	}
	public void setEnableCompression(boolean enableCompression) {
		this.enableCompression = enableCompression;
	}
	
	
	@Override
	public String toString() {
		return "MMXConfig [host=" + host + ", port=" + port + ", apiKey=" + apiKey + ", appId=" + appId + ", guestSecret=" + guestSecret + ", guestUserId="
				+ guestUserId + ", appPath=" + appPath + ", enableSendLastPubItem=" + enableSendLastPubItem + ", enableCompression=" + enableCompression + "]";
	}
	
	
}

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
package com.magnet.yak;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.*;
import com.magnet.mmx.protocol.AuthData;
import com.magnet.mmx.protocol.MMXTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MMXMessageListenerImpl implements MMXMessageListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MMXMessageListenerImpl.class);
	private String username;
	private MMXClient mmxClient;

	public MMXMessageListenerImpl(String username, MMXClient mmxClient) {
		this.username = username;
		this.mmxClient = mmxClient;
	}

	public String getUsername() {
		return username;
	}
    
	public void onAuthReceived(AuthData arg0) {
		LOGGER.debug("onAuthReceived : {}");
	}

	public void onInvitationReceived(Invitation arg0) {
		LOGGER.debug("onInvitationReceived : {}");
	}

	public void onSendFailed(MMXMessage arg0, String[] arg1, MMXException arg2) {
		LOGGER.debug("onSendFailed : {}");
	}

	public void onMessageReceived(MMXMessage mmxMessage, String receiptId) {
		LOGGER.debug("onMessageReceived : mmxMessage={}, receiptId={}",mmxMessage, receiptId);	
	}
	
	public void onMessageFailed(String arg0) {
		LOGGER.debug("onMessageFailed : {}", arg0);	
	}

	public void onMessageSent(String msgId) {
		LOGGER.debug("onMessageSent : {}", msgId);
	}

	public void onErrorMessageReceived(MMXErrorMessage errorMessage) {
		String errorString = errorMessage.getPayload().getDataAsText().toString();
		LOGGER.debug("onErrorMessageReceived : msgId={}\n errorString={}\n msg={}", errorMessage.getId(), errorString, errorMessage);
		
	}

	public void onMessageDelivered(MMXid to, String msgId) {
		LOGGER.debug("onMessageDelivered : to={}, msgId={}", to, msgId);
	}

	public void onMessageSending(MMXMessage mmxMessage, MMXid[] toArr) {
		LOGGER.debug("onMessageReceived : mmxMessage={}, toArr={}",mmxMessage, toArr);
	}

	public void onItemReceived(MMXMessage mmxMessage, MMXTopic topic) {
		LOGGER.debug("onItemReceived : mmxMessage={}, topic={}", mmxMessage, topic);
	}

	public void onMessageReceiving(MMXMessage mmxMessage) {
		LOGGER.debug("onMessageReceiving : mmxMessage={}", mmxMessage);
	}
}

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
package com.magnet.yak.event;

import java.util.Arrays;

import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.MMXClient;

public class MessageSendFailedEvent extends MessageEvent {
	MMXMessage message;
	String[] recipients;
	MMXException cause;
	
	public MessageSendFailedEvent(MMXClient mmxClient, String username, MMXMessage message, String[] recipients, MMXException cause) {
		super(mmxClient, username);
		this.message = message;
		this.recipients = recipients;
		this.cause = cause;
	}

	public MMXMessage getMessage() {
		return message;
	}

	public String[] getRecipients() {
		return recipients;
	}

	public MMXException getCause() {
		return cause;
	}

	@Override
	public String toString() {
		return "MessageSendFailedEvent [message=" + message + ", recipients=" + Arrays.toString(recipients) + ", cause=" + cause + ", mmxClient=" + mmxClient
				+ ", username=" + username + "]";
	}
}

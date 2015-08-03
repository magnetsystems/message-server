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

import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.MMXClient;

public class MessageSentEvent extends MessageEvent {
	MMXMessage message;
	String[] recipients;

	public MessageSentEvent(MMXClient mmxClient, String username, MMXMessage message, String[] recipients) {
		super(mmxClient, username);
		this.message = message;
		this.recipients = recipients;
	}
	
	public MMXMessage getMessage() {
		return message;
	}
	
	public String[] getRecipients() {
		return recipients;
	}

	@Override
	public String toString() {
		return "MessageSentEvent [message=" + message + ", recipients=" + Arrays.toString(recipients) + ", mmxClient=" + mmxClient + ", username=" + username
				+ "]";
	}
	
}

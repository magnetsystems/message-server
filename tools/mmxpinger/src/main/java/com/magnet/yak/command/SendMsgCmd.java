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
package com.magnet.yak.command;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.protocol.Headers;
import com.magnet.mmx.protocol.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SendMsgCmd {
	private static final Logger LOGGER = LoggerFactory.getLogger(SendMsgCmd.class);
		
	public SendMsgCmdOutput exec(SendMsgCmdInput input) {
		MMXClient from = input.getFrom();
		MMXClient to = input.getTo();
		
		Map<String, String> meta = new HashMap<String, String>();
		meta.put("key1", "value1");
		try {
			for(int i=0; i < input.getNumMessages(); i++) {
				MMXPayload p = 
						new MMXPayload(new Headers(0), constructMessage(input, i));
				LOGGER.debug("exec : sending message from={} - to={}", from.getClientId(), to.getClientId());
				from.getMessageManager().sendPayload(new MMXid[]{to.getClientId()}, p, null);
			}
		} catch (MMXException e) {
			LOGGER.error("exec : {}");
			e.printStackTrace();
		}
		return new SendMsgCmdOutput();
	}
	
	private Payload constructMessage(SendMsgCmdInput input, int i) {
		return new Payload("simple-message", "from-"+ input.getFrom() + "_" + "to-"+ input.getTo() + "_"+"YAK_BENCH_MESSAGE"+"_" + i);
	}
}

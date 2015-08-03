/* Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.yak.util;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.protocol.Payload;
import com.magnet.yak.ExecUtil;
import com.magnet.yak.command.RegisterUserCmdInput;
import com.magnet.yak.command.RegisterUserCmdOutput;
import com.magnet.yak.config.Configuration;
import com.magnet.yak.functional.GlobalConfig;
import com.magnet.yak.functional.RegisterUsersTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class MultiUserMessageTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(MultiUserMessageTest.class);
	private static Configuration config = GlobalConfig.getConfiguration("localhost");
	
	public static void main(String[] args) throws Exception {
		new MultiUserMessageTest().run();
	}
	
	public void run() throws Exception {
		List<RegisterUserCmdInput> inputs = new ArrayList<RegisterUserCmdInput>();
		for(int i=1; i < 4; i++) {
			inputs.add(new RegisterUserCmdInput("multiuser" + i, "test"));
		}
		Future<List<RegisterUserCmdOutput>> future = ExecUtil.getService().submit(new RegisterUsersTask(inputs));
		List<RegisterUserCmdOutput> outputs = future.get();
		MMXClient sender = outputs.get(0).getClient();
		
		MMXid receiver1 = outputs.get(1).getClient().getClientId();
		MMXid receiver2 = outputs.get(2).getClient().getClientId();
		
		sender.getMessageManager().sendPayload(new MMXid[]{receiver1, receiver2},
                new MMXPayload(null, new Payload(null, "Hey there")), null);
	}
	
	
}

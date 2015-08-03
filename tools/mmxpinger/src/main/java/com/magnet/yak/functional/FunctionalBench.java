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
package com.magnet.yak.functional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.yak.ExecUtil;
import com.magnet.yak.command.RegisterUserCmdOutput;
import com.magnet.yak.command.SendMsgCmdInput;
import com.magnet.yak.command.SendMsgCmdOutput;
import com.magnet.yak.config.Configuration;

public class FunctionalBench {
	private static final Logger LOGGER = LoggerFactory.getLogger(FunctionalBench.class);
	AtomicBoolean started = new AtomicBoolean(false);
	public FunctionalBench() {
	}
	
	public void start() throws Exception {
		if(!started.get()) {		
			started.set(true);
		}
		ListenableFuture<List<RegisterUserCmdOutput>> registerFuture = ExecUtil.getService().submit(new RegisterUsersTask());
		try {
			List<RegisterUserCmdOutput> registerCmdOutputs = registerFuture.get();
			
			for(int i=0; i < registerCmdOutputs.size(); i++) {
				LOGGER.debug("start : User:{} has registered", registerCmdOutputs.get(i).getUsername());
			}
			
			int numMessages = GlobalConfig.getConfiguration(YakBench.ENV_NAME).getLoad().getMessagesPerUser();
			
			int size = registerCmdOutputs.size();
			
			if(size < 2)
				LOGGER.trace("start : not sending messages users should be greater than 2");
			
			if(size % 2 != 0)
				LOGGER.trace("start : odd users, messages are sent in pairs last user will be idle");
			
			int newSize = size - 1;
			
			List<SendMsgCmdInput> msgInputs = new ArrayList<SendMsgCmdInput>();
			
			for(int i=0; i < newSize; i=i+2) {
				RegisterUserCmdOutput from = registerCmdOutputs.get(i);
				RegisterUserCmdOutput to = registerCmdOutputs.get(i+1);
				SendMsgCmdInput sendMsgInput1 = new SendMsgCmdInput(from.getClient(), to.getClient(), numMessages);
				SendMsgCmdInput sendMsgInput2 = new SendMsgCmdInput(to.getClient(), from.getClient(), numMessages);
				msgInputs.add(sendMsgInput1);
				msgInputs.add(sendMsgInput2);
			}
			
			ListenableFuture<List<SendMsgCmdOutput>> sendMsgFuture = ExecUtil.getService().submit(new SendMsgTask(msgInputs));
			
			List<MMXClient> clients = new ArrayList<MMXClient>();
			
			for(int i=0; i < registerCmdOutputs.size(); i++) {
				clients.add(registerCmdOutputs.get(i).getClient());
			}
			
			new TopicSummaryTest().publishItems(clients);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}

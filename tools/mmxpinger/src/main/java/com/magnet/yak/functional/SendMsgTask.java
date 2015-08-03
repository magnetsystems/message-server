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

import com.magnet.yak.command.SendMsgCmd;
import com.magnet.yak.command.SendMsgCmdInput;
import com.magnet.yak.command.SendMsgCmdOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class SendMsgTask implements Callable<List<SendMsgCmdOutput>>{

	List<SendMsgCmdInput> msgInputs;
	
	public SendMsgTask(List<SendMsgCmdInput> msgInputs) {
		super();
		this.msgInputs = msgInputs;
	}

	public List<SendMsgCmdOutput> call() throws Exception {
		List<SendMsgCmdOutput> outputs = new ArrayList<SendMsgCmdOutput>();
		
		for(SendMsgCmdInput input : msgInputs) {
			SendMsgCmdOutput output = new SendMsgCmd().exec(input);
			outputs.add(output);
		}
		return outputs;
	}
}

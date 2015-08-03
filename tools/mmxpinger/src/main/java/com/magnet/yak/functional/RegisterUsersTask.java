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
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.magnet.yak.command.RegisterUserCmd;
import com.magnet.yak.command.RegisterUserCmdInput;
import com.magnet.yak.command.RegisterUserCmdOutput;

public class RegisterUsersTask implements Callable<List<RegisterUserCmdOutput>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RegisterUsersTask.class);

	private List<RegisterUserCmdInput> inputs = null;
	
	public RegisterUsersTask(List<RegisterUserCmdInput> inputs) {
		this.inputs = inputs;
	}
	
	public RegisterUsersTask() {
	}

	public List<RegisterUserCmdOutput> call() throws Exception {		
		List<RegisterUserCmdOutput> outputs = new ArrayList<RegisterUserCmdOutput>();

		if(inputs != null ) {
			for (int i = 0; i < inputs.size(); i++) {
				outputs.add(new RegisterUserCmd().exec(inputs.get(i)));
			}
			return outputs;
		} else {
			String prefix = GlobalConfig.getConfiguration(YakBench.ENV_NAME).getLoad().getPrefix();
			String password = GlobalConfig.getConfiguration(YakBench.ENV_NAME).getLoad().getPassword();
			for (int i = 0; i < GlobalConfig.getConfiguration(YakBench.ENV_NAME).getLoad().getNumUsers(); i++) {
				outputs.add(new RegisterUserCmd().exec(new RegisterUserCmdInput(prefix + i, password)));
			}
			return outputs;
		}
	}
	

}

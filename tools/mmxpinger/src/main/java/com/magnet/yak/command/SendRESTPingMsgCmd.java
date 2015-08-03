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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class SendRESTPingMsgCmd implements SyncCommand<SendRESTPingMsgInput, SendRESTPingMsgOutput>, AsyncCommand<SendRESTPingMsgInput, SendRESTPingMsgOutput> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SendRESTPingMsgCmd.class);
	
	public ListenableFuture<RegisterUserCmdOutput> execAsync(RegisterUserCmdInput input) {
		return null;
	}

	public ListenableFuture<SendRESTPingMsgOutput> execAsync(SendRESTPingMsgInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	public SendRESTPingMsgOutput exec(SendRESTPingMsgInput input) {
		try {
			Unirest.post(input.getBaseUri()).basicAuth("admin", "mmxtester")
			  .queryString("deviceid", input.getDeviceId())
			  .queryString("ping", "1").queryString("appId", input.getAppId()).asString();
		} catch (Exception e) {
			LOGGER.error("exec : caught exception", e);
		}
		return new SendRESTPingMsgOutput(input.getAppId(), input.getDeviceId());
	}

}

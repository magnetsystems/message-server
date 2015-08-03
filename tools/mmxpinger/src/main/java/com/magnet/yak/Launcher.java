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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXContext;
import com.magnet.mmx.client.MMXSettings;
import com.magnet.yak.command.RegisterUserCmd;
import com.magnet.yak.command.RegisterUserCmdInput;
import com.magnet.yak.command.RegisterUserCmdOutput;

public class Launcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
	public static void main(String args[]) {
		LOGGER.trace("main : {}");
		List<ListenableFuture<RegisterUserCmdOutput>> list = new ArrayList<ListenableFuture<RegisterUserCmdOutput>>();
		for(int i=1; i < 2; i++) {
			MMXContext context = new MMXContext("/Users/sdatar/work/git/mmxpinger/src/main/resources/","version-1","sujay-device" + i);
			
			MMXSettings mmxSettings = new MMXSettings(context, "aaaa");
			mmxSettings.setString(MMXSettings.PROP_APIKEY, "0dc6447b-5593-43b4-8437-20f33bc80d32");
			mmxSettings.setString(MMXSettings.PROP_APPID, "jcci3jaffqw");
			mmxSettings.setString(MMXSettings.PROP_GUESTSECRET, "1tqkjnfuk9g3w");
			mmxSettings.setString(MMXSettings.PROP_HOST, "localhost");
			mmxSettings.setString(MMXSettings.PROP_PORT, "5222");
			MMXClient mmxClient = new MMXClient(context, mmxSettings);

			RegisterUserCmdInput input = new RegisterUserCmdInput("zing"+i, "test");
			try {
				ListenableFuture<RegisterUserCmdOutput> future = new RegisterUserCmd().execAsync(input);
				list.add(future);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ListenableFuture<List<RegisterUserCmdOutput>> lists = Futures.successfulAsList(list);
		try {
			List<RegisterUserCmdOutput> successList = lists.get();
			for(RegisterUserCmdOutput output : successList) {
				LOGGER.trace("main : output={}", output);
			}
			try {
				//client.getPushManager().push("sujay2", "sujay-device2", "PlaySound", "{\r\n  \"text\": \"Current app is out of date.  Newer version is available.\",\r\n  \"date\": \"2014-10-9\",\r\n  \"sound\": \"base64-encoded-audio-content\"\r\n}\r\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

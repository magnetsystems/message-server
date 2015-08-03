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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.magnet.yak.config.Configuration;

import javassist.NotFoundException;

public class YakBench {
	public static final String ENV_NAME = "localhost";
	private static Env env = new Env(System.getProperty("env", ENV_NAME));
	
	public static void main(String[] args) throws Exception {
	    ScheduledExecutorService service = Executors.newScheduledThreadPool(10);
		try {
			new FunctionalBench().start();
			/*service.scheduleAtFixedRate(new Runnable() {
				public void run() {
					for (int i = 0; i < 5; i++) {
						SendRESTPingMsgInput input = new SendRESTPingMsgInput("http://207.135.69.242:9090/plugins/mmxmgmt/push", "5ifi5spfnmc",
								"11EC5FCD124F51410A5C536364CCBA6DE7E11BB7");
						new SendRESTPingMsgCmd().exec(input);
					}
				}
			}, 0L, 10, TimeUnit.SECONDS);*/
			
			
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
	}

	public static Env getEnv() {
		return env;
	}
	
	public static class Env {
		private Configuration config;
		
		public Env(String name) {
			super();
			this.config = GlobalConfig.getConfiguration(name);
		}
		public Configuration getConfig() {
			return config;
		}
	}
}

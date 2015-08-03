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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class AppCreate {
	private static final Logger LOGGER = LoggerFactory.getLogger(AppCreate.class);

	private long waitTime = -1;
	
	public AppCreate(int waitTime) {
		this.waitTime = waitTime;
	}

	public ListenableFuture<AppCreateOutput> apply(AppCreateInput input) throws Exception {
		return null;
	}

	public AppCreateOutput exec(AppCreateInput input) {
		LOGGER.trace("exec : input={}", input);
		try {
			ListenableFuture<AppCreateOutput> future = apply(input);
			return future.get(waitTime, TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new AppCreateOutput();
	}
}

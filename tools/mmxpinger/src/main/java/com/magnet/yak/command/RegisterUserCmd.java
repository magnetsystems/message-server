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

import java.util.concurrent.LinkedBlockingQueue;

import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXConnectionListener;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXMessageListener;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXContext;
import com.magnet.mmx.client.MMXSettings;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.yak.ExecUtil;
import com.magnet.yak.MMXConnectionListenerImpl;
import com.magnet.yak.MMXMessageListenerImpl;
import com.magnet.yak.MMXUtil;

public class RegisterUserCmd implements SyncCommand<RegisterUserCmdInput, RegisterUserCmdOutput>, AsyncCommand<RegisterUserCmdInput, RegisterUserCmdOutput> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegisterUserCmd.class);
	private MMXContext context;
	private MMXClient mmxClient;
	private MMXSettings mmxSettings;
	
	public ListenableFuture<RegisterUserCmdOutput> execAsync(final RegisterUserCmdInput input) {
		LOGGER.trace("apply : input={}", input);
		setup(input);
		final SettableFuture<RegisterUserCmdOutput> future = SettableFuture.create();
		final SettableFuture<Void> onAuth = SettableFuture.create();
		final MMXMessageListener messageListener = new MMXMessageListenerImpl(input.getUsername(), mmxClient);
		final MMXConnectionListener connectionListener = new MMXConnectionListenerImpl(input.getUsername(), onAuth);

		Futures.addCallback(onAuth, new FutureCallback<Void>() {
			public void onFailure(Throwable t) {
				LOGGER.trace("onFailure : input={}", input, t);
				future.setException(t);
			}

			public void onSuccess(Void v) {
				LOGGER.trace("onSuccess : input={}", input);
				future.set(new RegisterUserCmdOutput(mmxClient, input.getUsername(), input.getPassword(), connectionListener, messageListener));
			}
		});

		ExecUtil.getService().submit(new Runnable() {
			public void run() {
				try {
					Log.setLoggable("test", 6);
					mmxClient.connect(input.getUsername(), input.getPassword().getBytes(), connectionListener, messageListener, false);
				} catch (MMXException e) {
					e.printStackTrace();
				}
			}
		});

		return future;
	}

	public RegisterUserCmdOutput exec(RegisterUserCmdInput input) {
		LOGGER.trace("exec : input={}", input);
		setup(input);
		try {
			LinkedBlockingQueue<Boolean> notify = new LinkedBlockingQueue<Boolean>();
			MMXMessageListener messageListener = new MMXMessageListenerImpl(input.getUsername(), mmxClient);
			MMXConnectionListener connectionListener = new MMXConnectionListenerImpl(input.getUsername(), notify);
			mmxClient.connect(input.getUsername(), input.getPassword().getBytes(), connectionListener, messageListener, true);
			
			try {
				notify.take();
				return new RegisterUserCmdOutput(mmxClient, input.getUsername(), input.getPassword(), connectionListener, messageListener);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (MMXException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void setup(RegisterUserCmdInput input) {
		try {
			String version = "version-1";
			String deviceId = input.getUsername();
			context = MMXUtil.makeContext(version, deviceId);
			mmxSettings = MMXUtil.makeSettings(context);
			mmxClient = new MMXClient(context, mmxSettings);
			Log.setLoggable(null, Log.VERBOSE);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

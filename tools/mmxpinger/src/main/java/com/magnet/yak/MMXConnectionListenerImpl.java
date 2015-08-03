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

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;
import com.magnet.mmx.client.common.MMXConnectionListener;
import com.magnet.mmx.client.common.MMXException;

public class MMXConnectionListenerImpl implements MMXConnectionListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(MMXConnectionListenerImpl.class);
	private String username;
	private SettableFuture<Void> future;
	private LinkedBlockingQueue<Boolean> notify;
	
	public MMXConnectionListenerImpl(String username, SettableFuture<Void> future) {
		this.username = username;
		this.future = future;
	}
	
	public MMXConnectionListenerImpl(String username, LinkedBlockingQueue<Boolean> notify) {
		this.username = username;
		this.notify = notify;
	}

	public void onAccountCreated(final String arg0) {
		Runnable runnable = new Runnable() {
			public void run() {
				LOGGER.debug("onAccountCreated : username={}, arg={}",username, arg0);	
			}
		};
		runAsync(runnable);
	}

	public void onAuthFailed(final String arg0) {
		Runnable runnable = new Runnable() {
			public void run() {
				LOGGER.error("onAuthFailed : username={}, arg={}",username, arg0);
				if(future != null) future.setException(new MMXException(arg0));
				if(notify != null) notify.offer(new Boolean(false));
			}
		};
		runAsync(runnable);
		
	}

	public void onAuthenticated(final String arg0) {
		Runnable runnable = new Runnable() {
			public void run() {
				LOGGER.debug("onAuthenticated : username={}, arg={}",username, arg0);
				if(future != null) future.set(null);
				if(notify != null) notify.offer(new Boolean(true));
			}
		};
		runAsync(runnable);
	}

	public void onConnectionClosed() {
		Runnable runnable = new Runnable() {
			public void run() {
				LOGGER.debug("onConnectionClosed : username={}", username);
			}
		};
		runAsync(runnable);
	}

	public void onConnectionEstablished() {
		Runnable runnable = new Runnable() {
			public void run() {
				LOGGER.debug("onConnectionEstablished : username={}", username);
			}
		};
		runAsync(runnable);
	}

	public void onConnectionFailed(final Exception arg0) {
		Runnable runnable = new Runnable() {
			public void run() {
				LOGGER.error("onConnectionFailed : username={}", username, arg0);
				future.setException(new MMXException(arg0));
			}
		};
		runAsync(runnable);
	}
	
	private void runAsync(Runnable runnable) {
		ExecUtil.getService().submit(runnable);
	}
}

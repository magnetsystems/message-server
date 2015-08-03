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
package com.magnet.yak.config;

public class LoadConfig {
	private String prefix;
	private int messagesPerUser;
	private int numUsers;
	private boolean sendReceipt;
	private String password;
	private long interMessageDelay;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public int getMessagesPerUser() {
		return messagesPerUser;
	}

	public void setMessagesPerUser(int messagesPerUser) {
		this.messagesPerUser = messagesPerUser;
	}

	public int getNumUsers() {
		return numUsers;
	}

	public void setNumUsers(int numUsers) {
		this.numUsers = numUsers;
	}

	public boolean isSendReceipt() {
		return sendReceipt;
	}

	public void setSendReceipt(boolean sendReceipt) {
		this.sendReceipt = sendReceipt;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	
	public long getInterMessageDelay() {
		return interMessageDelay;
	}

	public void setInterMessageDelay(long interMessageDelay) {
		this.interMessageDelay = interMessageDelay;
	}

	@Override
	public String toString() {
		return "LoadConfig [prefix=" + prefix + ", messagesPerUser=" + messagesPerUser + ", numUsers=" + numUsers + ", sendReceipt=" + sendReceipt
				+ ", password=" + password + ", interMessageDelay=" + interMessageDelay + "]";
	}

}

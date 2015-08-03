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

public class Configuration {
	String name;
	MMXConfig mmx;
	LoadConfig load;

	public MMXConfig getMmx() {
		return mmx;
	}

	public void setMmx(MMXConfig mmx) {
		this.mmx = mmx;
	}

	public LoadConfig getLoad() {
		return load;
	}

	public void setLoad(LoadConfig load) {
		this.load = load;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Configuration [name=" + name + ", mmx=" + mmx + ", load=" + load + "]";
	}
}

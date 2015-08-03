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

import javassist.NotFoundException;

import com.magnet.mmx.client.MMXContext;
import com.magnet.mmx.client.MMXSettings;
import com.magnet.yak.functional.YakBench;
import com.magnet.yak.util.ConfigUtil;

public class MMXUtil {
	public static MMXContext makeContext(String version, String deviceId) throws NotFoundException {
		return ConfigUtil.getContextFromConfig(YakBench.getEnv().getConfig(), version, deviceId);
	}
	public static MMXSettings makeSettings(MMXContext context) throws NotFoundException {
		return ConfigUtil.getSettingsFromConfig(YakBench.getEnv().getConfig(), context);
	}
}
 
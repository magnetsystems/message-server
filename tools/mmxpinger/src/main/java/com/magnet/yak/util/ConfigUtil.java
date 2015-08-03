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
package com.magnet.yak.util;

import javassist.NotFoundException;

import com.magnet.mmx.client.MMXContext;
import com.magnet.mmx.client.MMXSettings;
import com.magnet.yak.config.Configuration;
import com.magnet.yak.config.MMXConfig;

public class ConfigUtil {
	public static MMXSettings getSettingsFromConfig(Configuration config, MMXContext context) throws NotFoundException {
		MMXConfig mmxConfig = config.getMmx();
		MMXSettings mmxSettings = new MMXSettings(context, "mmx.properties");
		mmxSettings.setString(MMXSettings.PROP_APIKEY, mmxConfig.getApiKey());
		mmxSettings.setString(MMXSettings.PROP_APPID, mmxConfig.getAppId());
		mmxSettings.setString(MMXSettings.PROP_GUESTSECRET, mmxConfig.getGuestSecret());
		mmxSettings.setString(MMXSettings.PROP_HOST, mmxConfig.getHost());
		mmxSettings.setInt(MMXSettings.PROP_PORT, Integer.parseInt(mmxConfig.getPort()));
		mmxSettings.setBoolean(MMXSettings.PROP_ENABLE_COMPRESSION, false);
		mmxSettings.setBoolean(MMXSettings.PROP_ENABLE_TLS, false);		
		return mmxSettings;	
	}
	
	public static MMXContext getContextFromConfig(Configuration config, String version, String deviceId) throws NotFoundException {
		MMXConfig mmxConfig = config.getMmx();
		String appPath = mmxConfig.getAppPath();
		return new MMXContext(appPath, version, deviceId);
	}
}

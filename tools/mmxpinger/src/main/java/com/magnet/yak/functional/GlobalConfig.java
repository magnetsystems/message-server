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

import static com.magnet.yak.util.Constants.YAK_CONFIG;
import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.magnet.yak.config.ConfigParser;
import com.magnet.yak.config.Configuration;

public class GlobalConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalConfig.class);
	private static Configuration configuration = null;
	
	public static synchronized void setConfiguration(Configuration c) {
		configuration = c;
	}
	
	public static synchronized Configuration getConfiguration(String name) {
		if(configuration == null) {
			String config = System.getProperty(YAK_CONFIG);
			if(Strings.isNullOrEmpty(config)) {	
				LOGGER.error("getConfiguration : Launching Yak : config file : {}", config);
				System.exit(0);
			}
			LOGGER.info("getConfiguration : Launching Yak : config file : {}", System.getProperty(YAK_CONFIG));
			configuration = ConfigParser.getByName(config, name);
			LOGGER.trace("getConfiguration : config={}", configuration);
		}
		return configuration;
	}
}

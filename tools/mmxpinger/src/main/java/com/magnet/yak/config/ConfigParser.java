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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ConfigParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigParser.class);
	public static Configurations getAll(String file) {
		return parse(file);
	}
	public static Configuration getByName(String file, String name) {
		return parse(file).getByName(name);
	}
	private static Configurations parse(String file) {
		Configurations config = null;
		try {
			InputStream input = new FileInputStream(new File(file));
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			config = mapper.readValue(input, Configurations.class);
			LOGGER.debug("load : parse config={}", config);
		} catch (Exception e) {
			LOGGER.error("parse : file={}", file, e);
		}
		return config;
	}
}

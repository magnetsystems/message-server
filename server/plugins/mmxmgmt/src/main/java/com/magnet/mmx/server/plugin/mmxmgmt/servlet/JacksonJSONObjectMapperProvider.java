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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * A provider implementation added for configuring the Jackson JSON Object Mapper.
 * This was added to fix an issue where Jackson would fail to de-serialize json containing
 * unmapped properties.
 *
 * Note this is added to the Jersey system by adding a reference to this in config. This is done
 * in @MMXJerseyServletWrapper.
 *
 *
 */
@Provider
public class JacksonJSONObjectMapperProvider implements ContextResolver<ObjectMapper> {
  private static final Logger LOGGER = LoggerFactory.getLogger(JacksonJSONObjectMapperProvider.class);

  final ObjectMapper customizedObjectMapper ;

  public JacksonJSONObjectMapperProvider() {
    customizedObjectMapper = new ObjectMapper();
    //don't fail on unmapped properties.
    customizedObjectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    customizedObjectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
    //customizedObjectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return customizedObjectMapper;
  }
}

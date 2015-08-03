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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.StringWriter;

/**
 */
public class BaseJerseyJacksonSerializationTest {

  public String toJsonString(Object objectToMarshall) throws Exception {
    StringWriter writer = new StringWriter();
    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(writer, objectToMarshall);
    return writer.toString();
  }

  public Object fromJson(String json, Class clazz) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(json, clazz);
  }

  public Object fromJson(String json, TypeReference<?> typeReference) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(json, typeReference);
  }
}
